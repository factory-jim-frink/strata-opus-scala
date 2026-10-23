/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import java.io.Serializable
import java.util.Collection
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Objects
import java.util.Set
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate

/**
 * A value with associated failures.
 *
 * This captures a common use case where an operation can tolerate some failure.
 * This is often referred to as partial success or partial failure.
 * The class stores the value, of any object type, and a list of failures that may be empty.
 *
 * The success value must be able to handle the case where everything fails.
 * In most cases, the success value will be a collection type, such as `List`
 * or `Map`, which can be empty if the operation failed completely.
 *
 * The classic example is loading rows from a file, when some rows are valid and some are invalid.
 * The partial result would contain the successful rows, with the list of failures containing an
 * entry for each row that failed to parse.
 *
 * @tparam T the type of the underlying success value, typically a collection type
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class ValueWithFailures[T] private (
    /**
     * The success value.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val value: T,
    rawFailures: List[FailureItem])
  extends ImmutableBean with Serializable {

  JodaBeanUtils.notNull(value, "value")
  JodaBeanUtils.notNull(rawFailures, "failures")
  /**
   * The failure items.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val failures: ImmutableList[FailureItem] = ImmutableList.copyOf(rawFailures)

  //-------------------------------------------------------------------------
  /**
   * Checks if there are any failures.
   *
   * @return true if there are any failures
   */
  def hasFailures(): Boolean = {
    !failures.isEmpty()
  }

  /**
   * Processes the value by applying a function that alters the value.
   *
   * This operation allows post-processing of a result value.
   * The specified function represents a conversion to be performed on the value.
   *
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @tparam R  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the transformed instance of value and failures
   */
  def map[R](function: Function[? >: T, ? <: R]): ValueWithFailures[R] = {
    val transformedValue: R = Objects.requireNonNull(function.apply(value))
    ValueWithFailures.of[R](transformedValue, this.failures: List[FailureItem])
  }

  /**
   * Processes the value by applying a function that alters the failures.
   *
   * This operation allows post-processing of a result failures.
   * The specified function represents a conversion to be performed on the failures.
   *
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @param function  the function to transform the failures with
   * @return the transformed instance of value and failures
   */
  def mapFailures(function: Function[FailureItem, FailureItem]): ValueWithFailures[T] = {
    if (failures.isEmpty()) {
      this
    } else {
      ValueWithFailures.of[T](
          value,
          failures.stream().map[FailureItem](function).collect(Guavate.toImmutableList[FailureItem]()): List[FailureItem])
    }
  }

  /**
   * Processes the value by applying a function that returns another result.
   *
   * This operation allows post-processing of a result value.
   * This is similar to `map(Function)` but the function returns a `ValueWithFailures`.
   * The result of this method consists of the transformed value, and the combined list of failures.
   *
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @tparam R  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the transformed instance of value and failures
   */
  def flatMap[R](function: Function[? >: T, ValueWithFailures[R]]): ValueWithFailures[R] = {
    val transformedValue: ValueWithFailures[R] = Objects.requireNonNull(function.apply(value))
    val combinedFailures: ImmutableList[FailureItem] =
      Guavate.concatToList[FailureItem](failures, transformedValue.failures)
    ValueWithFailures.of[R](transformedValue.value, combinedFailures: List[FailureItem])
  }

  /**
   * Combines this instance with another.
   *
   * If both instances contain lists of the same type, the combining function will
   * often be `Guavate::concatToList`.
   *
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @tparam U  the type of the value in the other instance
   * @tparam R  the type of the value in the returned result
   * @param other  the other instance
   * @param combiner  the function that combines the two values
   * @return the combined instance of value and failures
   */
  def combinedWith[U, R](other: ValueWithFailures[U], combiner: BiFunction[T, U, R]): ValueWithFailures[R] = {
    val combinedValue: R = Objects.requireNonNull(combiner.apply(value, other.value))
    val combinedFailures: ImmutableList[FailureItem] = Guavate.concatToList[FailureItem](failures, other.failures)
    ValueWithFailures.of[R](combinedValue, combinedFailures: List[FailureItem])
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with the specified value, retaining the current failures.
   *
   * This can be useful as an inline alternative to `map(Function)`.
   *
   * @tparam R  the type of the value in the returned result
   * @param value  the new value
   * @return the combined instance of value and failures
   */
  def withValue[R](value: R): ValueWithFailures[R] = {
    ValueWithFailures.of[R](value, this.failures: List[FailureItem])
  }

  /**
   * Returns a new instance with the specified value, combining the failures.
   *
   * This can be useful as an inline alternative to `flatMap(Function)`.
   *
   * @tparam R  the type of the value in the returned result
   * @param value  the new value
   * @param additionalFailures  the additional failures
   * @return the combined instance of value and failures
   */
  def withValue[R](value: R, additionalFailures: List[FailureItem]): ValueWithFailures[R] = {
    ValueWithFailures.of[R](
        value, Guavate.concatToList[FailureItem](this.failures, additionalFailures): List[FailureItem])
  }

  /**
   * Returns a new instance with the specified value, combining the failures.
   *
   * This can be useful as an inline alternative to `flatMap(Function)`.
   *
   * @tparam R  the type of the value in the returned result
   * @param valueWithFailures  the new value with failures
   * @return the combined instance of value and failures
   */
  def withValue[R](valueWithFailures: ValueWithFailures[R]): ValueWithFailures[R] = {
    ValueWithFailures.of[R](
        valueWithFailures.getValue(),
        Guavate.concatToList[FailureItem](this.failures, valueWithFailures.getFailures()): List[FailureItem])
  }

  /**
   * Returns a new instance with the specified failures, retaining the current value.
   *
   * @param additionalFailures  the additional failures
   * @return the combined instance of value and failures
   */
  def withAdditionalFailures(additionalFailures: List[FailureItem]): ValueWithFailures[T] = {
    ValueWithFailures.of[T](
        value, Guavate.concatToList[FailureItem](this.failures, additionalFailures): List[FailureItem])
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): ValueWithFailures.Meta[T] = {
    ValueWithFailures.Meta.INSTANCE.asInstanceOf[ValueWithFailures.Meta[T]]
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the success value.
   * @return the value of the property, not null
   */
  def getValue(): T = {
    value
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the failure items.
   * @return the value of the property, not null
   */
  def getFailures(): ImmutableList[FailureItem] = {
    failures
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[ValueWithFailures[?]]
      JodaBeanUtils.equal(value, other.value) &&
          JodaBeanUtils.equal(failures, other.failures)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(value)
    hash = hash * 31 + JodaBeanUtils.hashCode(failures)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(96)
    buf.append("ValueWithFailures{")
    buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ')
    buf.append("failures").append('=').append(JodaBeanUtils.toString(failures))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ValueWithFailures {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ValueWithFailures.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @tparam T  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  @varargs
  def of[T](successValue: T, failures: FailureItem*): ValueWithFailures[T] = {
    new ValueWithFailures[T](successValue, ImmutableList.copyOf(failures.toArray))
  }

  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @tparam T  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  def of[T](successValue: T, failures: List[FailureItem]): ValueWithFailures[T] = {
    // this method is retained to ensure binary compatibility
    new ValueWithFailures[T](successValue, failures)
  }

  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @tparam T  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  def of[T](successValue: T, failures: Collection[FailureItem]): ValueWithFailures[T] = {
    new ValueWithFailures[T](successValue, ImmutableList.copyOf(failures))
  }

  /**
   * Creates an instance using a supplier.
   *
   * If the supplier succeeds normally, the supplied value will be returned.
   * If the supplier fails, the empty value will be returned along with a failure.
   *
   * This recognizes and handles `FailureItemProvider` exceptions.
   * If the exception type is not recognized, the failure item will have a reason of `ERROR`.
   *
   * @tparam T the type of the value
   * @param emptyValue  the empty value
   * @param supplier  supplier of the result value
   * @return an instance containing the supplied value, or a failure if an exception is thrown
   */
  def of[T](emptyValue: T, supplier: Supplier[T]): ValueWithFailures[T] = {
    try {
      of[T](supplier.get())
    } catch {
      case ex: Exception =>
        ValueWithFailures.of[T](emptyValue, FailureItem.from(ex))
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a `BinaryOperator` that combines `ValueWithFailures` objects using the provided combiner
   * function.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.reduce(combiningValues(Guavate::concatToList));
   *
   *   stream.reduce(baseValueWithFailures, combiningValues(Guavate::concatToList));
   * }}}
   *
   * This replaces code of the form:
   * {{{
   *   stream.reduce((vwf1, vwf2) -> vwf1.combinedWith(vwf2, Guavate::concatToList));
   *
   *   stream.reduce(baseValueWithFailures, (vwf1, vwf2) -> vwf1.combinedWith(vwf2, Guavate::concatToList));
   * }}}
   *
   * @param combiner  the combiner of the values
   * @tparam T  the type of the values
   * @return the combining binary operator
   */
  def combiningValues[T](combiner: BinaryOperator[T]): BinaryOperator[ValueWithFailures[T]] = {
    ArgChecker.notNull(combiner, "combiner")
    (vwf1, vwf2) => vwf1.combinedWith(vwf2, combiner)
  }

  /**
   * Returns a collector that can be used to create a combined `ValueWithFailure`
   * from a stream of separate instances.
   *
   * The `Collector` returned performs a reduction of its `ValueWithFailures` input elements under a
   * specified `BinaryOperator` using the provided identity.
   *
   * This collects a `Stream<ValueWithFailures<T>>` to a `ValueWithFailures<T>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @param identityValue  the identity value
   * @param operator  the operator used for the reduction.
   * @return a `Collector`
   */
  def toValueWithFailures[T](
      identityValue: T,
      operator: BinaryOperator[T]): Collector[ValueWithFailures[T], ?, ValueWithFailures[T]] = {

    Collectors.reducing[ValueWithFailures[T]](ValueWithFailures.of[T](identityValue), combiningValues[T](operator))
  }

  /**
   * Returns a collector that creates a combined `ValueWithFailure` from a stream
   * of separate instances, combining into an immutable list.
   *
   * This collects a `Stream<ValueWithFailures<T>>` to a `ValueWithFailures<List<T>>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @return a `Collector`
   */
  def toCombinedValuesAsList[T](): Collector[ValueWithFailures[? <: T], ?, ValueWithFailures[List[T]]] = {
    val supplier: Supplier[StreamBuilder[T, ImmutableList.Builder[T]]] =
      () => new StreamBuilder[T, ImmutableList.Builder[T]](ImmutableList.builder[T]())
    val accumulator: BiConsumer[StreamBuilder[T, ImmutableList.Builder[T]], ValueWithFailures[? <: T]] =
      (builder, item) => builder.add(item)
    val combiner: BinaryOperator[StreamBuilder[T, ImmutableList.Builder[T]]] =
      (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[StreamBuilder[T, ImmutableList.Builder[T]], ValueWithFailures[List[T]]] =
      builder => builder.build[List[T]]()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  /**
   * Returns a collector that creates a combined `ValueWithFailure` from a stream
   * of separate instances, combining into an immutable set.
   *
   * This collects a `Stream<ValueWithFailures<T>>` to a `ValueWithFailures<Set<T>>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @return a `Collector`
   */
  def toCombinedValuesAsSet[T](): Collector[ValueWithFailures[? <: T], ?, ValueWithFailures[Set[T]]] = {
    val supplier: Supplier[StreamBuilder[T, ImmutableSet.Builder[T]]] =
      () => new StreamBuilder[T, ImmutableSet.Builder[T]](ImmutableSet.builder[T]())
    val accumulator: BiConsumer[StreamBuilder[T, ImmutableSet.Builder[T]], ValueWithFailures[? <: T]] =
      (builder, item) => builder.add(item)
    val combiner: BinaryOperator[StreamBuilder[T, ImmutableSet.Builder[T]]] =
      (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[StreamBuilder[T, ImmutableSet.Builder[T]], ValueWithFailures[Set[T]]] =
      builder => builder.build[Set[T]]()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  /**
   * Returns a collector that can be used to create a combined `ValueWithFailure`
   * from a stream of `Result` instances.
   *
   * This collects a `Stream<Result<T>>` to a `ValueWithFailures<List<T>>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @return a `Collector`
   */
  def toCombinedResultsAsList[T](): Collector[Result[? <: T], ?, ValueWithFailures[List[T]]] = {
    val supplier: Supplier[StreamBuilder[T, ImmutableList.Builder[T]]] =
      () => new StreamBuilder[T, ImmutableList.Builder[T]](ImmutableList.builder[T]())
    val accumulator: BiConsumer[StreamBuilder[T, ImmutableList.Builder[T]], Result[? <: T]] =
      (builder, item) => builder.addResult(item)
    val combiner: BinaryOperator[StreamBuilder[T, ImmutableList.Builder[T]]] =
      (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[StreamBuilder[T, ImmutableList.Builder[T]], ValueWithFailures[List[T]]] =
      builder => builder.build[List[T]]()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  /**
   * Returns a collector that can be used to create a combined `ValueWithFailure`
   * from a stream of entries where the values contain `Result` instances.
   *
   * This collects a `Stream<Map.Entry<K, Result<V>>>` to a `ValueWithFailures<Map<K, V>>`.
   *
   * @tparam K the type of the map key
   * @tparam V the type of the success map values
   * @throws java.lang.IllegalArgumentException if duplicate keys are collected
   * @return a `Collector`
   */
  def toCombinedResultsAsMap[K, V](): Collector[? >: Map.Entry[K, Result[V]], ?, ValueWithFailures[Map[K, V]]] = {
    val supplier: Supplier[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]]] =
      () => new MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]](ImmutableMap.builder[K, V]())
    val accumulator: BiConsumer[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]], Map.Entry[K, Result[V]]] =
      (builder, item) => builder.addResult(item)
    val combiner: BinaryOperator[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]]] =
      (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]], ValueWithFailures[Map[K, V]]] =
      builder => builder.build[Map[K, V]]()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  /**
   * Returns a collector that creates a combined `ValueWithFailure` from a stream
   * of entries where the values contain `ValueWithFailure` instances.
   *
   * This collects a `Stream<Map.Entry<K, ValueWithFailures<V>>` to a `ValueWithFailures<Map<K, V>>`.
   *
   * @tparam K the type of the map key
   * @tparam V the type of the entry success values in the `ValueWithFailures`
   * @throws java.lang.IllegalArgumentException if duplicate keys are collected
   * @return a `Collector`
   */
  def toCombinedValuesAsMap[K, V](): Collector[? >: Map.Entry[K, ValueWithFailures[V]], ?, ValueWithFailures[Map[K, V]]] = {
    val supplier: Supplier[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]]] =
      () => new MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]](ImmutableMap.builder[K, V]())
    val accumulator: BiConsumer[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]], Map.Entry[K, ValueWithFailures[V]]] =
      (builder, item) => builder.add(item)
    val combiner: BinaryOperator[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]]] =
      (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[MapStreamBuilder[K, V, ImmutableMap.Builder[K, V]], ValueWithFailures[Map[K, V]]] =
      builder => builder.build[Map[K, V]]()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  /**
   * Combines separate instances of `ValueWithFailure` into a single instance,
   * using a list to collect the values.
   *
   * This converts `Iterable<ValueWithFailures<T>>` to `ValueWithFailures<List<T>>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @param items  the items to combine
   * @return a new instance with a list of success values
   */
  def combineValuesAsList[T](
      items: java.lang.Iterable[? <: ValueWithFailures[? <: T]]): ValueWithFailures[List[T]] = {

    val values: ImmutableList.Builder[T] = ImmutableList.builder[T]()
    val failures: ImmutableList[FailureItem] = combine[T](items, values)
    ValueWithFailures.of[List[T]](values.build(), failures: List[FailureItem])
  }

  /**
   * Combines separate instances of `ValueWithFailure` into a single instance,
   * using a set to collect the values.
   *
   * This converts `Iterable<ValueWithFailures<T>>` to `ValueWithFailures<Set<T>>`.
   *
   * @tparam T  the type of the success value in the `ValueWithFailures`
   * @param items  the items to combine
   * @return a new instance with a set of success values
   */
  def combineValuesAsSet[T](
      items: java.lang.Iterable[? <: ValueWithFailures[? <: T]]): ValueWithFailures[Set[T]] = {

    val values: ImmutableSet.Builder[T] = ImmutableSet.builder[T]()
    val failures: ImmutableList[FailureItem] = combine[T](items, values)
    ValueWithFailures.of[Set[T]](values.build(), failures: List[FailureItem])
  }

  // combines the collection into the specified builder
  private def combine[T](
      items: java.lang.Iterable[? <: ValueWithFailures[? <: T]],
      values: ImmutableCollection.Builder[T]): ImmutableList[FailureItem] = {

    val failures: ImmutableList.Builder[FailureItem] = ImmutableList.builder[FailureItem]()
    val it = items.iterator()
    while (it.hasNext()) {
      val item: ValueWithFailures[? <: T] = it.next()
      values.add(item.getValue())
      failures.addAll(item.getFailures())
    }
    failures.build()
  }

  // mutable combined instance builder for use in stream collection
  // using a single dedicated collector is more efficient than a reduction with multiple calls to combinedWith()
  private final class StreamBuilder[T, B <: ImmutableCollection.Builder[T]](values: B) {

    private val failures: ImmutableList.Builder[FailureItem] = ImmutableList.builder[FailureItem]()

    def add(item: ValueWithFailures[? <: T]): Unit = {
      values.add(item.getValue())
      failures.addAll(item.getFailures())
    }

    def addResult(item: Result[? <: T]): Unit = {
      val typedItem = item.asInstanceOf[Result[T]]
      typedItem.ifSuccess(value => {
        values.add(value)
        ()
      })
      typedItem.ifFailure(failure => {
        failures.addAll(failure.getItems())
        ()
      })
    }

    def combine(builder: StreamBuilder[T, B]): StreamBuilder[T, B] = {
      values.addAll(builder.valuesBuilder().build())
      failures.addAll(builder.failures.build())
      this
    }

    private def valuesBuilder(): B = {
      values
    }

    // cast to the right collection type, can assume the methods in this class are using the correct types
    def build[C <: Collection[T]](): ValueWithFailures[C] = {
      ValueWithFailures.of[ImmutableCollection[T]](values.build(), failures.build(): List[FailureItem])
          .asInstanceOf[ValueWithFailures[C]]
    }
  }

  private final class MapStreamBuilder[K, V, B <: ImmutableMap.Builder[K, V]](values: B) {

    private val failures: ImmutableList.Builder[FailureItem] = ImmutableList.builder[FailureItem]()

    def add(item: Map.Entry[K, ValueWithFailures[V]]): Unit = {
      values.put(item.getKey(), item.getValue().getValue())
      failures.addAll(item.getValue().getFailures())
    }

    def addResult(item: Map.Entry[K, Result[V]]): Unit = {
      val itemValue: Result[V] = item.getValue()
      itemValue.ifSuccess(value => {
        values.put(item.getKey(), value)
        ()
      })
      itemValue.ifFailure(failure => {
        failures.addAll(failure.getItems())
        ()
      })
    }

    def combine(builder: MapStreamBuilder[K, V, B]): MapStreamBuilder[K, V, B] = {
      values.putAll(builder.valuesBuilder().build())
      failures.addAll(builder.failures.build())
      this
    }

    private def valuesBuilder(): B = {
      values
    }

    // cast to the right collection type, can assume the methods in this class are using the correct types
    def build[C <: Map[K, V]](): ValueWithFailures[C] = {
      ValueWithFailures.of[ImmutableMap[K, V]](values.buildOrThrow(), failures.build(): List[FailureItem])
          .asInstanceOf[ValueWithFailures[C]]
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ValueWithFailures`.
   * @return the meta-bean, not null
   */
  def meta(): ValueWithFailures.Meta[?] = {
    ValueWithFailures.Meta.INSTANCE
  }

  /**
   * The meta-bean for `ValueWithFailures`.
   * @tparam R  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  def metaValueWithFailures[R](cls: Class[R]): ValueWithFailures.Meta[R] = {
    ValueWithFailures.Meta.INSTANCE.asInstanceOf[ValueWithFailures.Meta[R]]
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ValueWithFailures`.
   * @tparam T  the type
   */
  final class Meta[T] private () extends DirectMetaBean {

    /**
     * The meta-property for the `value` property.
     */
    private val valueProperty: MetaProperty[T] = DirectMetaProperty.ofImmutable(
        this, "value", classOf[ValueWithFailures[?]], classOf[Object]).asInstanceOf[MetaProperty[T]]
    /**
     * The meta-property for the `failures` property.
     */
    private val failuresProperty: MetaProperty[ImmutableList[FailureItem]] = DirectMetaProperty.ofImmutable(
        this, "failures", classOf[ValueWithFailures[?]], classOf[ImmutableList[?]])
        .asInstanceOf[MetaProperty[ImmutableList[FailureItem]]]
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "value",
        "failures")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 111972721 => // value
          valueProperty
        case 675938345 => // failures
          failuresProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: ValueWithFailures[T]] = {
      new ValueWithFailures.Builder[T]()
    }

    override def beanType(): Class[? <: ValueWithFailures[T]] = {
      classOf[ValueWithFailures[?]].asInstanceOf[Class[? <: ValueWithFailures[T]]]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `value` property.
     * @return the meta-property, not null
     */
    def value(): MetaProperty[T] = {
      valueProperty
    }

    /**
     * The meta-property for the `failures` property.
     * @return the meta-property, not null
     */
    def failures(): MetaProperty[ImmutableList[FailureItem]] = {
      failuresProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 111972721 => // value
          bean.asInstanceOf[ValueWithFailures[?]].getValue().asInstanceOf[Object]
        case 675938345 => // failures
          bean.asInstanceOf[ValueWithFailures[?]].getFailures()
        case _ =>
          super.propertyGet(bean, propertyName, quiet)
      }
    }

    override protected def propertySet(bean: Bean, propertyName: String, newValue: Object, quiet: Boolean): Unit = {
      metaProperty(propertyName)
      if (!quiet) {
        throw new UnsupportedOperationException("Property cannot be written: " + propertyName)
      }
    }

  }

  object Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    private[result] val INSTANCE: Meta[?] = new Meta[Object]()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `ValueWithFailures`.
   * @tparam T  the type
   */
  private final class Builder[T] () extends DirectPrivateBeanBuilder[ValueWithFailures[T]] {

    private var value: T = uninitialized
    private var failures: List[FailureItem] = ImmutableList.of[FailureItem]()

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 111972721 => // value
          value.asInstanceOf[Object]
        case 675938345 => // failures
          failures
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder[T] = {
      propertyName.hashCode() match {
        case 111972721 => // value
          this.value = newValue.asInstanceOf[T]
        case 675938345 => // failures
          this.failures = newValue.asInstanceOf[List[FailureItem]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): ValueWithFailures[T] = {
      new ValueWithFailures[T](
          value,
          failures)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("ValueWithFailures.Builder{")
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ')
      buf.append("failures").append('=').append(JodaBeanUtils.toString(failures))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
