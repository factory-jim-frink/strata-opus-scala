/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import java.io.ObjectStreamField
import java.io.Serializable
import java.util.Arrays
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Set
import java.util.SortedMap
import java.util.function.IntFunction
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

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
import org.joda.beans.gen.ImmutableConstructor
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.Sets
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.MapStream
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.array.DoubleArray

/**
 * An array of multi-currency amounts.
 *
 * This represents an array of `MultiCurrencyAmount`.
 * Internally, it stores the data using a map of `Currency` to `DoubleArray`,
 * which uses less memory than a `List<MultiCurrencyAmount>`.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class MultiCurrencyAmountArray @ImmutableConstructor private (
    // the Java field was named 'size', which clashes with size() in Scala;
    // Joda-Beans finds the '_size' field for the 'size' property, and the serialized form
    // is mapped explicitly by serialPersistentFields/writeObject/readObject
    /**
     * The size of this array.
     */
    @(PropertyDefinition @field)(validate = "notNegative")
    @transient private val _size: Int,
    valuesInput: Map[Currency, DoubleArray])
  extends FxConvertible[CurrencyAmountArray] with ImmutableBean with Serializable {

  /**
   * The currency values, keyed by currency.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  @transient private val values: ImmutableSortedMap[Currency, DoubleArray] = ImmutableSortedMap.copyOf(valuesInput)

  /** Holds the deserialized instance between readObject() and readResolve(). */
  @transient private var deserialized: MultiCurrencyAmountArray = uninitialized

  //-------------------------------------------------------------------------
  /**
   * Gets the set of currencies for which this object contains values.
   *
   * @return the set of currencies for which this object contains values
   */
  def getCurrencies(): Set[Currency] = {
    values.keySet()
  }

  /**
   * Gets the values for the specified currency, throws an exception if there are no values for the currency.
   *
   * @param currency  the currency for which values are required
   * @return the values for the specified currency, throws an exception if there are none
   * @throws java.lang.IllegalArgumentException if there are no values for the currency
   */
  def getValues(currency: Currency): DoubleArray = {
    val currencyValues = values.get(currency)
    if (currencyValues == null) {
      throw new IllegalArgumentException("No values available for " + currency)
    }
    currencyValues
  }

  /**
   * Gets the size of the array.
   *
   * @return the array size
   */
  def size(): Int = {
    _size
  }

  /**
   * Gets the amount at the specified index.
   *
   * @param index  the zero-based index to retrieve
   * @return the amount at the specified index
   */
  def get(index: Int): MultiCurrencyAmount = {
    val currencyAmounts: List[CurrencyAmount] = values.keySet().stream()
        .map[CurrencyAmount](ccy => CurrencyAmount.of(ccy, values.get(ccy).get(index)))
        .collect(Collectors.toList[CurrencyAmount]())
    MultiCurrencyAmount.of(currencyAmounts)
  }

  /**
   * Returns a stream of the amounts.
   *
   * @return a stream of the amounts
   */
  def stream(): Stream[MultiCurrencyAmount] = {
    IntStream.range(0, _size).mapToObj[MultiCurrencyAmount](i => get(i))
  }

  override def convertedTo(resultCurrency: Currency, fxRateProvider: FxRateProvider): CurrencyAmountArray = {
    val singleCurrencyValues = new Array[Double](_size)
    val it = values.entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      val currency = entry.getKey()
      val currencyValues = entry.getValue()
      var i = 0
      while (i < _size) {
        singleCurrencyValues(i) += currencyValues.get(i) * fxRateProvider.fxRate(currency, resultCurrency)
        i += 1
      }
    }
    CurrencyAmountArray.of(resultCurrency, DoubleArray.ofUnsafe(singleCurrencyValues))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new array containing the values from this array added to the values in the other array.
   *
   * The amounts are added to the matching element in this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added to the values in the other array
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def plus(other: MultiCurrencyAmountArray): MultiCurrencyAmountArray = {
    if (other.size() != _size) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", Integer.valueOf(_size), Integer.valueOf(other.size())))
    }
    val addedValues: Map[Currency, DoubleArray] =
      Stream.concat[Map.Entry[Currency, DoubleArray]](values.entrySet().stream(), other.values.entrySet().stream())
          .collect(Collectors.toMap[Map.Entry[Currency, DoubleArray], Currency, DoubleArray](
              e => e.getKey(), e => e.getValue(), (arr1, arr2) => arr1.plus(arr2)))
    MultiCurrencyAmountArray.of(addedValues)
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount added.
   *
   * The amount is added to each element in this array.
   *
   * @param amount  the amount to add
   * @return a new array containing the values from this array added to the values in the other array
   */
  def plus(amount: MultiCurrencyAmount): MultiCurrencyAmountArray = {
    val builder = ImmutableMap.builder[Currency, DoubleArray]()
    val it = Sets.union[Currency](values.keySet(), amount.getCurrencies()).iterator()
    while (it.hasNext()) {
      val currency = it.next()
      val array = values.get(currency)
      if (array == null) {
        builder.put(currency, DoubleArray.filled(_size, amount.getAmount(currency).getAmount()))
      } else if (!amount.contains(currency)) {
        builder.put(currency, array)
      } else {
        builder.put(currency, array.plus(amount.getAmount(currency).getAmount()))
      }
    }
    MultiCurrencyAmountArray.of(builder.build())
  }

  /**
   * Returns a new array containing the values from this array with the values from the other array subtracted.
   *
   * The amounts are subtracted from the matching element in this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added with the values from the other array subtracted
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def minus(other: MultiCurrencyAmountArray): MultiCurrencyAmountArray = {
    if (other.size() != _size) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", Integer.valueOf(_size), Integer.valueOf(other.size())))
    }
    val builder = ImmutableMap.builder[Currency, DoubleArray]()
    val it = Sets.union[Currency](values.keySet(), other.values.keySet()).iterator()
    while (it.hasNext()) {
      val currency = it.next()
      val array = values.get(currency)
      val otherArray = other.values.get(currency)
      if (otherArray == null) {
        builder.put(currency, array)
      } else if (array == null) {
        builder.put(currency, otherArray.multipliedBy(-1))
      } else {
        builder.put(currency, array.minus(otherArray))
      }
    }
    MultiCurrencyAmountArray.of(builder.build())
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount subtracted.
   *
   * The amount is subtracted from each element in this array.
   *
   * @param amount  the amount to subtract
   * @return a new array containing the values from this array with the values from the amount subtracted
   */
  def minus(amount: MultiCurrencyAmount): MultiCurrencyAmountArray = {
    val builder = ImmutableMap.builder[Currency, DoubleArray]()
    val it = Sets.union[Currency](values.keySet(), amount.getCurrencies()).iterator()
    while (it.hasNext()) {
      val currency = it.next()
      val array = values.get(currency)
      if (array == null) {
        builder.put(currency, DoubleArray.filled(_size, -amount.getAmount(currency).getAmount()))
      } else if (!amount.contains(currency)) {
        builder.put(currency, array)
      } else {
        builder.put(currency, array.minus(amount.getAmount(currency).getAmount()))
      }
    }
    MultiCurrencyAmountArray.of(builder.build())
  }

  //-------------------------------------------------------------------------
  // writes the baseline serialized form, fields 'size' and 'values'
  @throws[IOException]
  private def writeObject(out: ObjectOutputStream): Unit = {
    val fields = out.putFields()
    fields.put("size", _size)
    fields.put("values", values)
    out.writeFields()
  }

  // reads the baseline serialized form
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream): Unit = {
    val fields = in.readFields()
    deserialized = new MultiCurrencyAmountArray(
        fields.get("size", 0),
        fields.get("values", null).asInstanceOf[ImmutableSortedMap[Currency, DoubleArray]])
  }

  // validate when deserializing
  @throws[ObjectStreamException]
  private def readResolve(): Object = {
    deserialized.values.values().stream().reduce((a1, a2) => MultiCurrencyAmountArray.checkSize(a1, a2))
    deserialized
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): MultiCurrencyAmountArray.Meta = {
    MultiCurrencyAmountArray.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the size of this array.
   * @return the value of the property
   */
  def getSize(): Int = {
    _size
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency values, keyed by currency.
   * @return the value of the property, not null
   */
  def getValues(): ImmutableSortedMap[Currency, DoubleArray] = {
    values
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[MultiCurrencyAmountArray]
      (_size == other._size) &&
          JodaBeanUtils.equal(values, other.values)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(_size)
    hash = hash * 31 + JodaBeanUtils.hashCode(values)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(96)
    buf.append("MultiCurrencyAmountArray{")
    buf.append("size").append('=').append(JodaBeanUtils.toString(Integer.valueOf(_size))).append(',').append(' ')
    buf.append("values").append('=').append(JodaBeanUtils.toString(values))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object MultiCurrencyAmountArray {

  /** The serialized form, matching the original Java field names and types. */
  @static private val serialPersistentFields: Array[ObjectStreamField] = Array(
      new ObjectStreamField("size", Integer.TYPE),
      new ObjectStreamField("values", classOf[ImmutableSortedMap[?, ?]]))

  @static private val REGISTERED: Boolean = {
    MetaBean.register(MultiCurrencyAmountArray.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified multi-currency amounts.
   *
   * @param amounts  the amounts
   * @return an instance with the specified amounts
   */
  @varargs
  def of(amounts: MultiCurrencyAmount*): MultiCurrencyAmountArray = {
    of(Arrays.asList[MultiCurrencyAmount](amounts*))
  }

  /**
   * Obtains an instance from the specified multi-currency amounts.
   *
   * @param amounts  the amounts
   * @return an instance with the specified amounts
   */
  def of(amounts: List[MultiCurrencyAmount]): MultiCurrencyAmountArray = {
    val size = amounts.size()
    val valueMap = new HashMap[Currency, Array[Double]]()
    var i = 0
    while (i < size) {
      val multiCurrencyAmount = amounts.get(i)
      val it = multiCurrencyAmount.getAmounts().iterator()
      while (it.hasNext()) {
        val currencyAmount = it.next()
        val currencyValues = valueMap.computeIfAbsent(currencyAmount.getCurrency(), ccy => new Array[Double](size))
        currencyValues(i) = currencyAmount.getAmount()
      }
      i += 1
    }
    val doubleArrayMap: Map[Currency, DoubleArray] =
      MapStream.of(valueMap).mapValues[DoubleArray](v => DoubleArray.ofUnsafe(v)).toMap()
    new MultiCurrencyAmountArray(size, doubleArrayMap)
  }

  /**
   * Obtains an instance using a function to create the entries.
   *
   * The function is passed the index and returns the `MultiCurrencyAmount` for that index.
   *
   * @param size  the number of elements, at least size one
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws java.lang.IllegalArgumentException is size is zero or less
   */
  def of(size: Int, valueFunction: IntFunction[MultiCurrencyAmount]): MultiCurrencyAmountArray = {
    val map: Map[Currency, Array[Double]] = new HashMap[Currency, Array[Double]]()
    var i = 0
    while (i < size) {
      val mca = valueFunction.apply(i)
      val it = mca.getAmounts().iterator()
      while (it.hasNext()) {
        val ca = it.next()
        val array = map.computeIfAbsent(ca.getCurrency(), c => new Array[Double](size))
        array(i) = ca.getAmount()
      }
      i += 1
    }
    new MultiCurrencyAmountArray(size, MapStream.of(map).mapValues[DoubleArray](array => DoubleArray.ofUnsafe(array)).toMap())
  }

  /**
   * Obtains an instance from a map of amounts.
   *
   * Each currency is associated with an array of amounts.
   * All the arrays must have the same number of elements.
   *
   * If the map is empty the returned array will have a size of zero. To create an empty array
   * with a non-zero size use one of the other `of` methods.
   *
   * @param values  map of currencies to values
   * @return an instance containing the values from the map
   */
  def of(values: Map[Currency, DoubleArray]): MultiCurrencyAmountArray = {
    values.values().stream().reduce((a1, a2) => checkSize(a1, a2))
    // All of the values must have the same size so use the size of the first
    val size = if (values.isEmpty()) 0 else values.values().iterator().next().size()
    new MultiCurrencyAmountArray(size, values)
  }

  /**
   * Checks the size of the arrays are the same and throws an exception if not.
   *
   * @param array1  an array
   * @param array2  an array
   * @return array1
   * @throws java.lang.IllegalArgumentException if the array sizes are not equal
   */
  private def checkSize(array1: DoubleArray, array2: DoubleArray): DoubleArray = {
    if (array1.size() != array2.size()) {
      throw new IllegalArgumentException(
          Messages.format(
              "Arrays must have the same size but found sizes {} and {}",
              Integer.valueOf(array1.size()),
              Integer.valueOf(array2.size())))
    }
    array1
  }

  /**
   * Returns a multi currency amount array representing the total of the input arrays.
   *
   * If the input contains the same currency more than once, the amounts are added together.
   *
   * @param arrays  the amount arrays
   * @return the total amounts
   */
  def total(arrays: java.lang.Iterable[CurrencyAmountArray]): MultiCurrencyAmountArray = {
    Guavate.stream(arrays).collect(toMultiCurrencyAmountArray())
  }

  /**
   * Returns a collector which creates a multi currency amount array by combining a stream of
   * currency amount arrays.
   *
   * The arrays in the stream must all have the same length.
   *
   * @return the collector
   */
  def toMultiCurrencyAmountArray(): Collector[CurrencyAmountArray, ?, MultiCurrencyAmountArray] = {
    Collector.of[CurrencyAmountArray, Map[Currency, CurrencyAmountArray], MultiCurrencyAmountArray](
        // accumulate into a map
        () => new HashMap[Currency, CurrencyAmountArray](),
        (map, ca) => {
          map.merge(ca.getCurrency(), ca, (a, b) => a.plus(b))
          ()
        },
        // combine two maps
        (map1, map2) => {
          map2.values().forEach(ca2 => {
            map1.merge(ca2.getCurrency(), ca2, (a, b) => a.plus(b))
            ()
          })
          map1
        },
        // convert to MultiCurrencyAmountArray
        map => {
          val currencyArrayMap: Map[Currency, DoubleArray] =
            MapStream.of(map).mapValues[DoubleArray](caa => caa.getValues()).toMap()
          MultiCurrencyAmountArray.of(currencyArrayMap)
        },
        Collector.Characteristics.UNORDERED)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `MultiCurrencyAmountArray`.
   * @return the meta-bean, not null
   */
  def meta(): MultiCurrencyAmountArray.Meta = {
    MultiCurrencyAmountArray.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `MultiCurrencyAmountArray`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `size` property.
     */
    private val sizeProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "size", classOf[MultiCurrencyAmountArray], Integer.TYPE)
    /**
     * The meta-property for the `values` property.
     */
    private val valuesProperty: MetaProperty[ImmutableSortedMap[Currency, DoubleArray]] = DirectMetaProperty.ofImmutable(
        this, "values", classOf[MultiCurrencyAmountArray],
        classOf[ImmutableSortedMap[?, ?]].asInstanceOf[Class[ImmutableSortedMap[Currency, DoubleArray]]])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "size",
        "values")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 3530753 => // size
          sizeProperty
        case -823812830 => // values
          valuesProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: MultiCurrencyAmountArray] = {
      new MultiCurrencyAmountArray.Builder()
    }

    override def beanType(): Class[? <: MultiCurrencyAmountArray] = {
      classOf[MultiCurrencyAmountArray]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `size` property.
     * @return the meta-property, not null
     */
    def size(): MetaProperty[Integer] = {
      sizeProperty
    }

    /**
     * The meta-property for the `values` property.
     * @return the meta-property, not null
     */
    def values(): MetaProperty[ImmutableSortedMap[Currency, DoubleArray]] = {
      valuesProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 3530753 => // size
          Integer.valueOf(bean.asInstanceOf[MultiCurrencyAmountArray].getSize())
        case -823812830 => // values
          bean.asInstanceOf[MultiCurrencyAmountArray].getValues()
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
    private[currency] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `MultiCurrencyAmountArray`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[MultiCurrencyAmountArray] {

    private var size: Int = 0
    private var values: SortedMap[Currency, DoubleArray] = ImmutableSortedMap.of[Currency, DoubleArray]()

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 3530753 => // size
          Integer.valueOf(size)
        case -823812830 => // values
          values
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 3530753 => // size
          this.size = newValue.asInstanceOf[Integer].intValue()
        case -823812830 => // values
          this.values = newValue.asInstanceOf[SortedMap[Currency, DoubleArray]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): MultiCurrencyAmountArray = {
      new MultiCurrencyAmountArray(
          size,
          values)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("MultiCurrencyAmountArray.Builder{")
      buf.append("size").append('=').append(JodaBeanUtils.toString(Integer.valueOf(size))).append(',').append(' ')
      buf.append("values").append('=').append(JodaBeanUtils.toString(values))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
