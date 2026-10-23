/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.time.Duration
import java.util.AbstractMap
import java.util.ArrayList
import java.util.Comparator
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import java.util.Set
import java.util.Spliterator
import java.util.Spliterators
import java.util.TreeMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

import scala.annotation.static
import scala.annotation.targetName
import scala.annotation.varargs
import scala.compiletime.uninitialized

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMultiset
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Iterables
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.MoreCollectors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.opengamma.strata.collect.tuple.ObjIntPair
import com.opengamma.strata.collect.tuple.Pair

/**
 * Utilities that help bridge the gap between Java 8 and Google Guava.
 *
 * Guava has the `FluentIterable` concept which is similar to streams.
 * In many ways, fluent iterable is nicer, because it directly binds to the
 * immutable collection classes. However, on balance it seems wise to use
 * the stream API rather than `FluentIterable` in Java 8.
 */
final class Guavate private ()

object Guavate {

  //-------------------------------------------------------------------------
  /**
   * Concatenates a number of iterables into a single list.
   *
   * This is harder than it should be, a method `Stream.of(Iterable)`
   * would have been appropriate, but cannot be added now.
   *
   * @tparam T the type of element in the iterable
   * @param iterables  the iterables to combine
   * @return the list that combines the inputs
   */
  @varargs
  def concatToList[T](iterables: java.lang.Iterable[? <: T]*): ImmutableList[T] = {
    ImmutableList.copyOf[T](Iterables.concat[T](iterables*))
  }

  /**
   * Concatenates a number of items onto a single base list.
   *
   * This returns a new list, the input is unaltered.
   *
   * @tparam T the type of element in the iterable
   * @param baseList  the base list
   * @param additionalItems  the additional items
   * @return the list that combines the inputs
   */
  @varargs
  def concatItemsToList[T](baseList: java.lang.Iterable[? <: T], additionalItems: T*): ImmutableList[T] = {
    // this cannot be named concatToList() as it would be ambiguous for callers
    ImmutableList.builder[T]()
        .addAll(baseList)
        .addAll(ImmutableList.copyOf[T](java.util.Arrays.asList[T](additionalItems*)))
        .build()
  }

  /**
   * Concatenates a number of iterables into a single set.
   *
   * This is harder than it should be, a method `Stream.of(Iterable)`
   * would have been appropriate, but cannot be added now.
   *
   * @tparam T the type of element in the iterable
   * @param iterables  the iterables to combine
   * @return the set that combines the inputs
   */
  @varargs
  def concatToSet[T](iterables: java.lang.Iterable[? <: T]*): ImmutableSet[T] = {
    ImmutableSet.copyOf[T](Iterables.concat[T](iterables*))
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two distinct maps into a single map, throwing an exception for duplicate keys.
   *
   * @param first  the first map
   * @param second  the second map
   * @tparam K the type of the keys
   * @tparam V the type of the values
   * @return a combined map
   * @throws java.lang.IllegalArgumentException if the same key is encountered in both maps
   */
  def combineMaps[K, V](
      first: Map[? <: K, ? <: V],
      second: Map[? <: K, ? <: V]): ImmutableMap[K, V] = {

    Stream.concat[Map.Entry[? <: K, ? <: V]](first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap[K, V]())
  }

  /**
   * Combines two distinct maps into a single map, choosing the key from the second map in case of duplicates.
   *
   * @param first  the first map
   * @param second  the second map
   * @tparam K the type of the keys
   * @tparam V the type of the values
   * @return a combined map
   */
  def combineMapsOverwriting[K, V](
      first: Map[? <: K, ? <: V],
      second: Map[? <: K, ? <: V]): ImmutableMap[K, V] = {

    Stream.concat[Map.Entry[? <: K, ? <: V]](first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap[K, V]((a: V, b: V) => b))
  }

  /**
   * Combines two maps into a single map.
   *
   * If the maps have shared keys then the merge function is used on the two values
   * and the result is placed in the resulting map.
   *
   * @param first  the first map
   * @param second  the second map
   * @param mergeFn  the function used to merge values
   * @tparam K the type of the keys
   * @tparam V the type of the values
   * @return a combined map
   */
  def combineMaps[K, V](
      first: Map[? <: K, ? <: V],
      second: Map[? <: K, ? <: V],
      mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): ImmutableMap[K, V] = {

    Stream.concat[Map.Entry[? <: K, ? <: V]](first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap[K, V](mergeFn))
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two list multimaps into a single list multimap.
   *
   * @param first  the first list multimap
   * @param second  the second list multimap
   * @tparam K the type of the keys
   * @tparam V the type of the values
   * @return a combined list multi map
   */
  def combineListMultimaps[K, V](
      first: ListMultimap[? <: K, ? <: V],
      second: ListMultimap[? <: K, ? <: V]): ImmutableListMultimap[K, V] = {

    Stream.concat[Map.Entry[? <: K, ? <: V]](first.entries().stream(), second.entries().stream())
        .collect(toImmutableListMultimap[Map.Entry[? <: K, ? <: V], K, V](
            (e: Map.Entry[? <: K, ? <: V]) => e.getKey(),
            (e: Map.Entry[? <: K, ? <: V]) => e.getValue()))
  }

  //-------------------------------------------------------------------------
  /**
   * Combines a map with new entries, choosing the last entry if there is a duplicate key.
   *
   * @param baseMap  the base map
   * @param additionalEntries  the additional entries
   * @tparam K the type of the keys
   * @tparam V the type of the values
   * @return the combined map
   */
  @varargs
  def combineMapsOverwriting[K, V](
      baseMap: Map[? <: K, ? <: V],
      additionalEntries: Map.Entry[? <: K, ? <: V]*): ImmutableMap[K, V] = {

    Stream.concat[Map.Entry[? <: K, ? <: V]](
        baseMap.entrySet().stream(),
        Stream.of[Map.Entry[? <: K, ? <: V]](additionalEntries*))
        .collect(entriesToImmutableMap[K, V]((a: V, b: V) => b))
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a try-catch block around an expression, avoiding exceptions.
   *
   * This converts an exception throwing method into a method that returns an optional by discarding the exception.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   *
   * To handle checked exceptions, use `Unchecked.supplier(CheckedSupplier)`.
   *
   * @tparam T the type of the result in the optional
   * @param resultSupplier  the supplier that might throw an exception
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  def tryCatchToOptional[T](resultSupplier: Supplier[T]): Optional[T] = {
    try {
      Optional.ofNullable(resultSupplier.get())
    } catch {
      case ex: RuntimeException =>
        Optional.empty()
    }
  }

  /**
   * Wraps a try-catch block around an expression, avoiding exceptions.
   *
   * This variant allows the exception to be observed, such as for logging.
   *
   * This converts an exception throwing method into a method that returns an optional by discarding the exception.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   *
   * To handle checked exceptions, use `Unchecked.supplier(CheckedSupplier)`.
   *
   * @tparam T the type of the result in the optional
   * @param resultSupplier  the supplier that might throw an exception
   * @param exceptionHandler  the exception handler
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  def tryCatchToOptional[T](resultSupplier: Supplier[T], exceptionHandler: Consumer[RuntimeException]): Optional[T] = {
    try {
      Optional.ofNullable(resultSupplier.get())
    } catch {
      case ex: RuntimeException =>
        exceptionHandler.accept(ex)
        Optional.empty()
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Uses a number of suppliers to create a single optional result.
   *
   * This invokes each supplier in turn until a non empty optional is returned.
   * As such, not all suppliers are necessarily invoked.
   *
   * The Java 8 `Optional` class does not have an `or` method,
   * so this provides an alternative.
   *
   * @tparam T the type of element in the optional
   * @param suppliers  the suppliers to combine
   * @return the first non empty optional
   */
  @varargs
  def firstNonEmpty[T](suppliers: Supplier[Optional[? <: T]]*): Optional[T] = {
    val it = suppliers.iterator
    while (it.hasNext) {
      val result = it.next().get()
      if (result.isPresent()) {
        // safe, because Optional is read-only
        return result.asInstanceOf[Optional[T]]
      }
    }
    Optional.empty()
  }

  // the Scala Seq overload needs a distinct JVM name, the Java varargs method keeps the name firstNonEmpty
  /**
   * Chooses the first optional that is not empty.
   *
   * The Java 8 `Optional` class does not have an `or` method,
   * so this provides an alternative.
   *
   * @tparam T the type of element in the optional
   * @param optionals  the optionals to combine
   * @return the first non empty optional
   */
  @varargs
  @targetName("firstNonEmptyOptionals")
  def firstNonEmpty[T](optionals: Optional[? <: T]*): Optional[T] = {
    val it = optionals.iterator
    while (it.hasNext) {
      val optional = it.next()
      if (optional.isPresent()) {
        // safe, because Optional is read-only
        return optional.asInstanceOf[Optional[T]]
      }
    }
    Optional.empty()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the first value from the iterable, returning empty if the iterable is empty.
   *
   * @tparam T the type of element in the optional
   * @param iterable  the iterable to query
   * @return the first value, empty if empty
   */
  def first[T](iterable: java.lang.Iterable[T]): Optional[T] = {
    val it = iterable.iterator()
    if (it.hasNext()) Optional.of(it.next()) else Optional.empty()
  }

  /**
   * Gets the only element from the collection, returning empty if the collection is empty or has more than one element.
   *
   * @tparam T the type of element in the optional
   * @param iterable  the iterable to query
   * @return the first value, empty if empty or more than one element
   */
  def only[T](iterable: java.lang.Iterable[T]): Optional[T] = {
    val it = iterable.iterator()
    if (it.hasNext()) Optional.of(it.next()).filter((element: T) => !it.hasNext()) else Optional.empty()
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a single `Map.Entry`.
   *
   * The entry is immutable.
   *
   * @tparam K the type of the key
   * @tparam V the type of the value
   * @param key  the key
   * @param value  the value
   * @return the map entry
   */
  def entry[K, V](key: K, value: V): Map.Entry[K, V] = {
    new AbstractMap.SimpleImmutableEntry[K, V](key, value)
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a list from the first element and remaining varargs.
   *
   * This can be used to create a list of at least size one.
   *
   * @tparam T the type of the elements
   * @param first  the first element
   * @param remaining  the remaining elements
   * @return a list formed from the first and remaining elements
   */
  @varargs
  def list[T](first: T, remaining: T*): ImmutableList[T] = {
    val builder = ImmutableList.builderWithExpectedSize[T](remaining.length + 1)
    builder.add(first)
    val it = remaining.iterator
    while (it.hasNext) {
      builder.add(it.next())
    }
    builder.build()
  }

  /**
   * Converts a set from the first element and remaining varargs.
   *
   * This can be used to create a set of at least size one.
   * The input may contain duplicates, which will be combined.
   *
   * @tparam T the type of the elements
   * @param first  the first element
   * @param remaining  the remaining elements
   * @return a set formed from the first and remaining elements
   */
  @varargs
  def set[T](first: T, remaining: T*): ImmutableSet[T] = {
    val builder = ImmutableSet.builderWithExpectedSize[T](remaining.length + 1)
    builder.add(first)
    val it = remaining.iterator
    while (it.hasNext) {
      builder.add(it.next())
    }
    builder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Boxes an `OptionalInt`.
   *
   * `OptionalInt` has almost no useful methods and no easy way to convert it to an `Optional`.
   * This method provides the conversion to `Optional<Integer>`.
   *
   * @param optional the `OptionalInt`
   * @return an equivalent optional
   */
  def boxed(optional: OptionalInt): Optional[Integer] = {
    if (optional.isPresent()) Optional.of(Integer.valueOf(optional.getAsInt())) else Optional.empty()
  }

  /**
   * Boxes an `OptionalLong`.
   *
   * `OptionalLong` has almost no useful methods and no easy way to convert it to an `Optional`.
   * This method provides the conversion to `Optional<Long>`.
   *
   * @param optional the `OptionalLong`
   * @return an equivalent optional
   */
  def boxed(optional: OptionalLong): Optional[java.lang.Long] = {
    if (optional.isPresent()) Optional.of(java.lang.Long.valueOf(optional.getAsLong())) else Optional.empty()
  }

  /**
   * Boxes an `OptionalDouble`.
   *
   * `OptionalDouble` has almost no useful methods and no easy way to convert it to an `Optional`.
   * This method provides the conversion to `Optional<Double>`.
   *
   * @param optional the `OptionalDouble`
   * @return an equivalent optional
   */
  def boxed(optional: OptionalDouble): Optional[java.lang.Double] = {
    if (optional.isPresent()) Optional.of(java.lang.Double.valueOf(optional.getAsDouble())) else Optional.empty()
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an iterable to a serial stream.
   *
   * This is harder than it should be, a method `Stream.of(Iterable)`
   * would have been appropriate, but cannot be added now.
   *
   * @tparam T the type of element in the iterable
   * @param iterable  the iterable to convert
   * @return a stream of the elements in the iterable
   */
  def stream[T](iterable: java.lang.Iterable[T]): Stream[T] = {
    StreamSupport.stream(iterable.spliterator(), false)
  }

  /**
   * Converts an optional to a stream with zero or one elements.
   *
   * @tparam T the type of optional element
   * @param optional  the optional
   * @return a stream containing a single value if the optional has a value, else a stream with no values.
   */
  def stream[T](optional: Optional[T]): Stream[T] = {
    if (optional.isPresent()) Stream.of[T](optional.get()) else Stream.empty[T]()
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a stream to an iterable for use in the for-each statement.
   *
   * For some use cases this approach is nicer than `Stream.forEach(Consumer)`.
   * Notably code that mutates a local variable or has to handle checked exceptions will benefit.
   *
   * {{{
   *  for (Item item : in(stream)) {
   *    // lazily use each item in the stream
   * }}}
   *
   * NOTE: The result of this method can only be iterated once, which does not
   * meet the expected specification of `Iterable`.
   * Use in the for-each statement is safe as it will only be called once.
   *
   * @tparam T the type of stream element
   * @param stream  the stream
   * @return an iterable representation of the stream that can only be invoked once
   */
  def in[T](stream: Stream[T]): java.lang.Iterable[T] = {
    val iterable: java.lang.Iterable[T] = () => stream.iterator()
    iterable
  }

  /**
   * Converts an optional to an iterable for use in the for-each statement.
   *
   * For some use cases this approach is nicer than `Optional.isPresent()`
   * followed by `Optional.get()`.
   *
   * {{{
   *  for (Item item : inOptional(optional)) {
   *    // use the optional value, code not called if the optional is empty
   * }}}
   *
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose `Iterable`, but the method name
   * is focussed on the for-each use case.
   *
   * @tparam T the type of optional element
   * @param optional  the optional
   * @return an iterable representation of the optional
   */
  def inOptional[T](optional: Optional[T]): java.lang.Iterable[T] = {
    if (optional.isPresent()) {
      ImmutableList.of[T](optional.get())
    } else {
      ImmutableList.of[T]()
    }
  }

  /**
   * Converts a nullable value to an iterable for use in the for-each statement.
   *
   * In most cases `if (nullable != null)` would be preferred.
   *
   * {{{
   *  for (Item item : inNullable(nullable)) {
   *    // use the nullable value, code not called if the value is null
   * }}}
   *
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose `Iterable`, but the method name
   * is focussed on the for-each use case.
   *
   * @tparam T the type of the nullable value
   * @param nullable  the nullable value
   * @return an iterable representation of the nullable
   */
  def inNullable[T](nullable: T): java.lang.Iterable[T] = {
    if (nullable != null) {
      ImmutableList.of[T](nullable)
    } else {
      ImmutableList.of[T]()
    }
  }

  /**
   * Converts an expression for use in the for-each statement wrapping it with a try-catch block to avoid exceptions.
   *
   * This allows a method that throws an exception to be used with the for-each statement.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   * The typical use case is for parsing, where the parser throws an exception if invalid.
   *
   * This method is intended for use with the for-each statement:
   *
   * {{{
   *  for (Item item : inTryCatchIgnore(() -> parse(str))) {
   *    // use the value, code not called if parse(str) threw an exception
   * }}}
   *
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose `Iterable`, but the method name
   * is focussed on the for-each use case.
   *
   * @tparam T the type of the result in the optional
   * @param supplier  the supplier that might throw an exception
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  def inTryCatchIgnore[T](supplier: Supplier[T]): java.lang.Iterable[T] = {
    try {
      inNullable(supplier.get())
    } catch {
      case ex: RuntimeException =>
        ImmutableList.of[T]()
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a stream that wraps a stream with the index.
   *
   * Each input object is decorated with an `ObjIntPair`.
   * The `int` is the index of the element in the stream.
   *
   * See also `MapStream.zipWithIndex(Stream)`.
   *
   * @tparam T the type of the stream
   * @param stream  the stream to index
   * @return a stream of pairs, containing the element and index
   */
  def zipWithIndex[T](stream: Stream[T]): Stream[ObjIntPair[T]] = {
    val split1 = stream.spliterator()
    val it1 = Spliterators.iterator(split1)
    val it: Iterator[ObjIntPair[T]] = new Iterator[ObjIntPair[T]]() {
      private var index: Int = 0

      override def hasNext(): Boolean = {
        it1.hasNext()
      }

      override def next(): ObjIntPair[T] = {
        val value = it1.next()
        val current = index
        index += 1
        ObjIntPair.of(value, current)
      }
    }
    val split: Spliterator[ObjIntPair[T]] =
      Spliterators.spliterator[ObjIntPair[T]](it, split1.getExactSizeIfKnown(), split1.characteristics())
    StreamSupport.stream(split, false)
  }

  /**
   * Creates a stream that combines two other streams, continuing until either stream ends.
   *
   * Each pair of input objects is combined into a `Pair`.
   *
   * See also `MapStream.zip(Stream, Stream)`.
   *
   * @tparam A the type of the first stream
   * @tparam B the type of the second stream
   * @param stream1  the first stream
   * @param stream2  the first stream
   * @return a stream of pairs, one from each stream
   */
  def zip[A, B](stream1: Stream[A], stream2: Stream[B]): Stream[Pair[A, B]] = {
    zip[A, B, Pair[A, B]](stream1, stream2, (a: A, b: B) => Pair.of(a, b))
  }

  /**
   * Creates a stream that combines two other streams, continuing until either stream ends.
   *
   * The combiner function is called once for each pair of objects found in the input streams.
   *
   * @tparam A the type of the first stream
   * @tparam B the type of the second stream
   * @tparam R the type of the resulting stream
   * @param stream1  the first stream
   * @param stream2  the first stream
   * @param zipper  the function used to combine the pair of objects
   * @return a stream of pairs, one from each stream
   */
  private[collect] def zip[A, B, R](stream1: Stream[A], stream2: Stream[B], zipper: BiFunction[A, B, R]): Stream[R] = {
    // this is private for now, to see if it is really needed on the API
    // it suffers from generics problems at the call site with common zipper functions
    // as such, it is less useful than it might seem
    val split1 = stream1.spliterator()
    val split2 = stream2.spliterator()
    // merged stream lacks some characteristics
    val characteristics = split1.characteristics() & split2.characteristics() &
        ~(Spliterator.DISTINCT | Spliterator.SORTED)
    val size = Math.min(split1.getExactSizeIfKnown(), split2.getExactSizeIfKnown())

    val it1 = Spliterators.iterator(split1)
    val it2 = Spliterators.iterator(split2)
    val it: Iterator[R] = new Iterator[R]() {
      override def hasNext(): Boolean = {
        it1.hasNext() && it2.hasNext()
      }

      override def next(): R = {
        zipper.apply(it1.next(), it2.next())
      }
    }
    val split: Spliterator[R] = Spliterators.spliterator[R](it, size, characteristics)
    StreamSupport.stream(split, false)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a predicate that negates the original.
   *
   * The JDK provides `Predicate.negate()` however this requires a predicate.
   * Sometimes, it can be useful to have a static method to achieve this.
   * {{{
   *  stream.filter(not(String::isEmpty))
   * }}}
   *
   * @tparam R the type of the object the predicate works on
   * @param predicate  the predicate to negate
   * @return the negated predicate
   */
  def not[R](predicate: Predicate[R]): Predicate[R] = {
    predicate.negate()
  }

  //-------------------------------------------------------------------------
  /**
   * Reducer used in a stream to ensure there is no more than one matching element.
   *
   * This method returns an operator that can be used with `Stream.reduce(BinaryOperator)`
   * that returns either zero or one elements from the stream. Unlike `Stream.findFirst()`
   * or `Stream.findAny()`, this approach ensures an exception is thrown if there
   * is more than one element in the stream.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.filter(...).reduce(ensureOnlyOne()).get();
   * }}}
   *
   * @tparam T the type of element in the stream
   * @return the operator
   * @throws java.lang.IllegalArgumentException if more than one element is present
   */
  def ensureOnlyOne[T](): BinaryOperator[T] = {
    (a: T, b: T) => {
      throw new IllegalArgumentException(Messages.format(
          "Multiple values found where only one was expected: {} and {}",
          a.asInstanceOf[Object], b.asInstanceOf[Object]))
    }
  }

  /**
   * Reducer used in a stream to ensure there is no more than one matching element.
   *
   * This method returns an operator that can be used with `Stream.reduce(BinaryOperator)`
   * that returns either zero or one elements from the stream. Unlike `Stream.findFirst()`
   * or `Stream.findAny()`, this approach ensures an exception is thrown if there
   * is more than one element in the stream.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.filter(...).reduce(ensureOnlyOne()).get();
   * }}}
   *
   * @tparam T the type of element in the stream
   * @param message the message template for the `IllegalArgumentException` with "{}" placeholders
   * @param args the arguments for the message
   * @return the operator
   * @throws java.lang.IllegalArgumentException if more than one element is present
   */
  @varargs
  def ensureOnlyOne[T](message: String, args: Object*): BinaryOperator[T] = {
    (a: T, b: T) => {
      throw new IllegalArgumentException(Messages.format(message, args*))
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Function used in a stream to cast instances to a particular type without filtering.
   *
   * This method returns a function that can be used with `Stream.map(Function)`
   * to cast elements in a stream to a particular type, throwing an exception if any
   * element is not of the specified type.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.map(casting(Foo.class));
   * }}}
   *
   * This replaces code of the form:
   * {{{
   *   stream.map(Foo.class::cast);
   * }}}
   *
   * @tparam T the type of element in the input stream
   * @tparam R the type of element in the output stream
   * @param cls  the type of element in the output stream
   * @return the function
   */
  def casting[T, R <: T](cls: Class[R]): Function[T, R] = {
    (input: T) => cls.cast(input)
  }

  /**
   * Function used in a stream to filter instances to a particular type.
   *
   * This method returns a function that can be used with `Stream.flatMap(Function)`
   * to filter elements in a stream to a particular type.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.flatMap(filtering(Foo.class));
   * }}}
   *
   * This replaces code of the form:
   * {{{
   *   stream.filter(Foo.class::isInstance).map(Foo.class::cast);
   * }}}
   *
   * @tparam T the type of element in the input stream
   * @tparam R the type of element in the output stream
   * @param cls  the type of element in the output stream
   * @return the function
   */
  def filtering[T, R <: T](cls: Class[R]): Function[T, Stream[R]] = {
    (input: T) => if (cls.isInstance(input)) Stream.of[R](cls.cast(input)) else Stream.empty[R]()
  }

  /**
   * Function used in a stream to filter optionals.
   *
   * This method returns a function that can be used with `Stream.flatMap(Function)`
   * to filter optional elements in a stream.
   * The resulting stream only contains the optional elements that are present.
   *
   * This would be used as follows (with a static import):
   * {{{
   *   stream.flatMap(filteringOptional());
   * }}}
   *
   * This replaces code of the form:
   * {{{
   *   stream.filter(Optional::isPresent).map(Optional::get);
   * }}}
   *
   * @tparam T the type of element in the output stream
   * @return the function
   */
  def filteringOptional[T](): Function[Optional[T], Stream[T]] = {
    (opt: Optional[T]) => if (opt.isPresent()) Stream.of[T](opt.get()) else Stream.empty[T]()
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to extract either zero or one elements.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into an `Optional`.
   * The collector throws `IllegalArgumentException` if the stream consists of two or more elements.
   * The collector throws `NullPointerException` if the stream consists of a null element.
   *
   * @tparam T the type of element in the list
   * @return the optional collector
   */
  def toOptional[T](): Collector[T, ?, Optional[T]] = {
    MoreCollectors.toOptional[T]()
  }

  /**
   * Collector used at the end of a stream to extract one element.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into an `Optional`.
   * The collector throws `NullPointerException` if the stream consists of a null element.
   * The collector returns empty if the stream consists of zero, two or more elements.
   *
   * @tparam T the type of element in the list
   * @return the collector
   */
  def toOnly[T](): Collector[T, ?, Optional[T]] = {
    new OnlyCollector[T]()
  }

  private[collect] class OnlyCollector[T] extends Collector[T, OnlyCollector[T], Optional[T]] {
    private var value: T = uninitialized
    private var count: Int = 0

    override def supplier(): Supplier[OnlyCollector[T]] = {
      () => new OnlyCollector[T]()
    }

    override def accumulator(): BiConsumer[OnlyCollector[T], T] = {
      (state: OnlyCollector[T], value: T) => {
        state.value = value
        state.count += 1
      }
    }

    override def combiner(): BinaryOperator[OnlyCollector[T]] = {
      (a: OnlyCollector[T], b: OnlyCollector[T]) => {
        throw new UnsupportedOperationException("OnlyCollector does not support parallel processing")
      }
    }

    override def finisher(): Function[OnlyCollector[T], Optional[T]] = {
      (state: OnlyCollector[T]) => if (state.count == 1) Optional.of(state.value) else Optional.empty[T]()
    }

    override def characteristics(): Set[Collector.Characteristics] = {
      ImmutableSet.of[Collector.Characteristics]()
    }
  }

  /**
   * Collector used at the end of a stream to build an immutable list.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableList`.
   *
   * @tparam T the type of element in the list
   * @return the immutable list collector
   */
  def toImmutableList[T](): Collector[T, ImmutableList.Builder[T], ImmutableList[T]] = {
    Collector.of[T, ImmutableList.Builder[T], ImmutableList[T]](
        () => new ImmutableList.Builder[T](),
        (builder: ImmutableList.Builder[T], value: T) => { builder.add(value); () },
        (l: ImmutableList.Builder[T], r: ImmutableList.Builder[T]) => l.addAll(r.build()),
        (builder: ImmutableList.Builder[T]) => builder.build())
  }

  /**
   * Collector used at the end of a stream to build an immutable list of
   * immutable lists of size equal to or less than size.
   * For example, the following list [a, b, c, d, e] with a partition
   * size of 2 will give [[a, b], [c, d], [e]].
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableList` of `ImmutableList`.
   *
   * @param size  the size of the partitions of the original list
   * @tparam T the type of element in the list
   * @return the immutable list of lists collector
   */
  def splittingBySize[T](size: Int): Collector[T, ?, ImmutableList[ImmutableList[T]]] = {
    Collectors.collectingAndThen(
        Collectors.collectingAndThen(
            Guavate.toImmutableList[T](),
            (objects: ImmutableList[T]) => Lists.partition(objects, size)),
        (lists: List[List[T]]) => Guavate.toImmutables(lists))
  }

  /**
   * Helper method to transform a list of lists into their immutable counterparts.
   *
   * @param lists  the list of lists
   * @tparam T the type of element in the list
   * @return the immutable lists
   */
  private def toImmutables[T](lists: List[List[T]]): ImmutableList[ImmutableList[T]] = {
    lists.stream()
        .map[ImmutableList[T]]((list: List[T]) => ImmutableList.copyOf[T](list))
        .collect(Guavate.toImmutableList[ImmutableList[T]]())
  }

  /**
   * Collector used at the end of a stream to build an immutable set.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSet`.
   *
   * @tparam T the type of element in the set
   * @return the immutable set collector
   */
  def toImmutableSet[T](): Collector[T, ImmutableSet.Builder[T], ImmutableSet[T]] = {
    Collector.of[T, ImmutableSet.Builder[T], ImmutableSet[T]](
        () => new ImmutableSet.Builder[T](),
        (builder: ImmutableSet.Builder[T], value: T) => { builder.add(value); () },
        (l: ImmutableSet.Builder[T], r: ImmutableSet.Builder[T]) => l.addAll(r.build()),
        (builder: ImmutableSet.Builder[T]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted set.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSet`.
   *
   * @tparam T the type of element in the sorted set
   * @return the immutable sorted set collector
   */
  def toImmutableSortedSet[T <: Comparable[?]](): Collector[T, ImmutableSortedSet.Builder[T], ImmutableSortedSet[T]] = {
    Collector.of[T, ImmutableSortedSet.Builder[T], ImmutableSortedSet[T]](
        () => ImmutableSortedSet.naturalOrder[T](),
        (builder: ImmutableSortedSet.Builder[T], value: T) => { builder.add(value); () },
        (l: ImmutableSortedSet.Builder[T], r: ImmutableSortedSet.Builder[T]) => l.addAll(r.build()),
        (builder: ImmutableSortedSet.Builder[T]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted set.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSet`.
   *
   * @tparam T the type of element in the sorted set
   * @param comparator  the comparator
   * @return the immutable sorted set collector
   */
  def toImmutableSortedSet[T](
      comparator: Comparator[? >: T]): Collector[T, ImmutableSortedSet.Builder[T], ImmutableSortedSet[T]] = {
    Collector.of[T, ImmutableSortedSet.Builder[T], ImmutableSortedSet[T]](
        () => new ImmutableSortedSet.Builder[T](comparator),
        (builder: ImmutableSortedSet.Builder[T], value: T) => { builder.add(value); () },
        (l: ImmutableSortedSet.Builder[T], r: ImmutableSortedSet.Builder[T]) => l.addAll(r.build()),
        (builder: ImmutableSortedSet.Builder[T]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable multiset.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMultiset`.
   *
   * @tparam T the type of element in the multiset
   * @return the immutable multiset collector
   */
  def toImmutableMultiset[T](): Collector[T, ImmutableMultiset.Builder[T], ImmutableMultiset[T]] = {
    Collector.of[T, ImmutableMultiset.Builder[T], ImmutableMultiset[T]](
        () => new ImmutableMultiset.Builder[T](),
        (builder: ImmutableMultiset.Builder[T], value: T) => { builder.add(value); () },
        (l: ImmutableMultiset.Builder[T], r: ImmutableMultiset.Builder[T]) => l.addAll(r.build()),
        (builder: ImmutableMultiset.Builder[T]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`, retaining insertion order.
   *
   * This returns a map by extracting a key from each element.
   * The input stream must resolve to unique keys.
   * The value associated with each key is the stream element.
   * See `Collectors.toMap(Function, Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def toImmutableMap[T, K](
      keyExtractor: Function[? >: T, ? <: K]): Collector[T, ?, ImmutableMap[K, T]] = {

    toImmutableMap[T, K, T](keyExtractor, Function.identity[T]())
  }

  /**
   * Collector used at the end of a stream to build an immutable map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`, retaining insertion order.
   *
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   * See `Collectors.toMap(Function, Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def toImmutableMap[T, K, V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V]): Collector[T, ?, ImmutableMap[K, V]] = {

    Collector.of[T, ImmutableMap.Builder[K, V], ImmutableMap[K, V]](
        () => new ImmutableMap.Builder[K, V](),
        (builder: ImmutableMap.Builder[K, V], value: T) => {
          builder.put(keyExtractor.apply(value), valueExtractor.apply(value))
          ()
        },
        (l: ImmutableMap.Builder[K, V], r: ImmutableMap.Builder[K, V]) => l.putAll(r.build()),
        (builder: ImmutableMap.Builder[K, V]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`, retaining insertion order.
   *
   * This returns a map by converting each stream element to a key and value.
   * If the same key is generated more than once the merge function is applied to the
   * values and the return value of the function is used as the value in the map.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @param mergeFn  function to merge values with the same key
   * @return the immutable map collector
   */
  def toImmutableMap[T, K, V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V],
      mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): Collector[T, Map[K, V], ImmutableMap[K, V]] = {

    Collector.of[T, Map[K, V], ImmutableMap[K, V]](
        () => new LinkedHashMap[K, V](),
        (map: Map[K, V], value: T) => {
          map.merge(keyExtractor.apply(value), valueExtractor.apply(value), mergeFn)
          ()
        },
        (m1: Map[K, V], m2: Map[K, V]) => mergeMaps(m1, m2, mergeFn),
        (map: Map[K, V]) => ImmutableMap.copyOf[K, V](map),
        Collector.Characteristics.UNORDERED)
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSortedMap`.
   *
   * This returns a map by extracting a key from each element.
   * The input stream must resolve to unique keys.
   * The value associated with each key is the stream element.
   * See `Collectors.toMap(Function, Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable sorted map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def toImmutableSortedMap[T, K <: Comparable[?]](
      keyExtractor: Function[? >: T, ? <: K]): Collector[T, ?, ImmutableSortedMap[K, T]] = {

    toImmutableSortedMap[T, K, T](keyExtractor, Function.identity[T]())
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSortedMap`.
   *
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   * See `Collectors.toMap(Function, Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable sorted map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def toImmutableSortedMap[T, K <: Comparable[?], V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V]): Collector[T, ?, ImmutableSortedMap[K, V]] = {

    Collector.of[T, ImmutableSortedMap.Builder[K, V], ImmutableSortedMap[K, V]](
        () => ImmutableSortedMap.naturalOrder[K, V](),
        (builder: ImmutableSortedMap.Builder[K, V], value: T) => {
          builder.put(keyExtractor.apply(value), valueExtractor.apply(value))
          ()
        },
        (l: ImmutableSortedMap.Builder[K, V], r: ImmutableSortedMap.Builder[K, V]) => l.putAll(r.build()),
        (builder: ImmutableSortedMap.Builder[K, V]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSortedMap`.
   *
   * This returns a map by converting each stream element to a key and value.
   * See `Collectors.toMap(Function, Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @param mergeFn  function to merge values with the same key
   * @return the immutable sorted map collector
   */
  def toImmutableSortedMap[T, K <: Comparable[?], V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V],
      mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): Collector[T, ?, ImmutableSortedMap[K, V]] = {

    Collector.of[T, TreeMap[K, V], ImmutableSortedMap[K, V]](
        () => new TreeMap[K, V](),
        (map: TreeMap[K, V], value: T) => {
          map.merge(keyExtractor.apply(value), valueExtractor.apply(value), mergeFn)
          ()
        },
        (m1: TreeMap[K, V], m2: TreeMap[K, V]) => mergeMaps(m1, m2, mergeFn),
        (map: TreeMap[K, V]) => ImmutableSortedMap.copyOfSorted[K, V](map),
        Collector.Characteristics.UNORDERED)
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable multimap.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableListMultimap`.
   *
   * This returns a multimap by extracting a key from each element.
   * The value associated with each key is the stream element.
   * Stream elements may be converted to the same key, with the values forming a multimap list.
   * See `Collectors.groupingBy(Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable multimap collector
   */
  def toImmutableListMultimap[T, K](
      keyExtractor: Function[? >: T, ? <: K]): Collector[T, ?, ImmutableListMultimap[K, T]] = {

    toImmutableListMultimap[T, K, T](keyExtractor, Function.identity[T]())
  }

  /**
   * Collector used at the end of a stream to build an immutable multimap.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableListMultimap`.
   *
   * This returns a multimap by converting each stream element to a key and value.
   * Stream elements may be converted to the same key, with the values forming a multimap list.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result multimap
   * @tparam V the type of the values in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable multimap collector
   */
  def toImmutableListMultimap[T, K, V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V]): Collector[T, ?, ImmutableListMultimap[K, V]] = {

    Collector.of[T, ImmutableListMultimap.Builder[K, V], ImmutableListMultimap[K, V]](
        () => new ImmutableListMultimap.Builder[K, V](),
        (builder: ImmutableListMultimap.Builder[K, V], value: T) => {
          builder.put(keyExtractor.apply(value), valueExtractor.apply(value))
          ()
        },
        (l: ImmutableListMultimap.Builder[K, V], r: ImmutableListMultimap.Builder[K, V]) => l.putAll(r.build()),
        (builder: ImmutableListMultimap.Builder[K, V]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable multimap.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSetMultimap`.
   *
   * This returns a multimap by extracting a key from each element.
   * The value associated with each key is the stream element.
   * Stream elements may be converted to the same key, with the values forming a multimap set.
   * See `Collectors.groupingBy(Function)` for more details.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable multimap collector
   */
  def toImmutableSetMultimap[T, K](
      keyExtractor: Function[? >: T, ? <: K]): Collector[T, ?, ImmutableSetMultimap[K, T]] = {

    toImmutableSetMultimap[T, K, T](keyExtractor, Function.identity[T]())
  }

  /**
   * Collector used at the end of a stream to build an immutable multimap.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableSetMultimap`.
   *
   * This returns a multimap by converting each stream element to a key and value.
   * Stream elements may be converted to the same key, with the values forming a multimap set.
   *
   * @tparam T the type of the stream elements
   * @tparam K the type of the keys in the result multimap
   * @tparam V the type of the values in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable multimap collector
   */
  def toImmutableSetMultimap[T, K, V](
      keyExtractor: Function[? >: T, ? <: K],
      valueExtractor: Function[? >: T, ? <: V]): Collector[T, ?, ImmutableSetMultimap[K, V]] = {

    Collector.of[T, ImmutableSetMultimap.Builder[K, V], ImmutableSetMultimap[K, V]](
        () => new ImmutableSetMultimap.Builder[K, V](),
        (builder: ImmutableSetMultimap.Builder[K, V], value: T) => {
          builder.put(keyExtractor.apply(value), valueExtractor.apply(value))
          ()
        },
        (l: ImmutableSetMultimap.Builder[K, V], r: ImmutableSetMultimap.Builder[K, V]) => l.putAll(r.build()),
        (builder: ImmutableSetMultimap.Builder[K, V]) => builder.build(),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing map entries. This is a common case if a map's
   * `entrySet` has undergone a `filter` operation. For example:
   * {{{
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
   *       ImmutableMap<String, Integer> output =
   *         input.entrySet()
   *           .stream()
   *           .filter(e -> e.getValue() % 2 == 1)
   *           .collect(entriesToImmutableMap());
   *
   *       // Produces map with "a" -> 1, "c" -> 3, "e" -> 5
   * }}}
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`.
   *
   * This returns a map by converting each `Map.Entry` to a key and value.
   * The input stream must resolve to unique keys.
   *
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @return the immutable map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def entriesToImmutableMap[K, V](): Collector[Map.Entry[? <: K, ? <: V], ?, ImmutableMap[K, V]] = {
    toImmutableMap[Map.Entry[? <: K, ? <: V], K, V](
        (e: Map.Entry[? <: K, ? <: V]) => e.getKey(),
        (e: Map.Entry[? <: K, ? <: V]) => e.getValue())
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing map entries. This is a common case if a map's
   * `entrySet` has undergone a `filter` operation. For example:
   * {{{
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
   *       ImmutableMap<String, Integer> output =
   *         input.entrySet()
   *           .stream()
   *           .filter(e -> e.getValue() % 2 == 1)
   *           .collect(entriesToImmutableMap());
   *
   *       // Produces map with "a" -> 1, "c" -> 3, "e" -> 5
   * }}}
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`.
   *
   * This returns a map by converting each `Map.Entry` to a key and value.
   * The input stream must resolve to unique keys.
   *
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @return the immutable map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def entriesToImmutableMap[K, V](
      mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): Collector[Map.Entry[? <: K, ? <: V], ?, ImmutableMap[K, V]] = {

    toImmutableMap[Map.Entry[? <: K, ? <: V], K, V](
        (e: Map.Entry[? <: K, ? <: V]) => e.getKey(),
        (e: Map.Entry[? <: K, ? <: V]) => e.getValue(),
        mergeFn)
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing pairs. This is a common case if a map's
   * `entrySet` has undergone a `map` operation with the
   * `Map.Entry` converted to a `Pair`. For example:
   * {{{
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
   *       ImmutableMap<String, Double> output =
   *         input.entrySet()
   *           .stream()
   *           .map(e -> Pair.of(e.getKey().toUpperCase(), Math.pow(e.getValue(), 2)))
   *           .collect(pairsToImmutableMap());
   *
   *       // Produces map with "A" -> 1.0, "B" -> 4.0, "C" -> 9.0, "D" -> 16.0
   * }}}
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an `ImmutableMap`.
   *
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   *
   * @tparam K the type of the keys in the result map
   * @tparam V the type of the values in the result map
   * @return the immutable map collector
   * @throws java.lang.IllegalArgumentException if the same key is generated twice
   */
  def pairsToImmutableMap[K, V](): Collector[Pair[? <: K, ? <: V], ?, ImmutableMap[K, V]] = {
    toImmutableMap[Pair[? <: K, ? <: V], K, V](
        (p: Pair[? <: K, ? <: V]) => p.getFirst(),
        (p: Pair[? <: K, ? <: V]) => p.getSecond())
  }

  //-------------------------------------------------------------------------
  /**
   * Helper method to merge two mutable maps by inserting all values from `map2` into `map1`.
   *
   * If `map1` already contains a mapping for a key the merge function is applied to the existing value and
   * the new value, and the return value is inserted.
   *
   * @param map1  the map into which values are copied
   * @param map2  the map from which values are copied
   * @param mergeFn  function applied to the existing and new values if the map contains the key
   * @tparam K the key type
   * @tparam V the value type
   * @tparam M the type of the first map
   * @return `map1` with the values from `map2` inserted
   */
  private def mergeMaps[K, V, M <: Map[K, V]](
      map1: M,
      map2: Map[K, V],
      mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): M = {

    val it = map2.entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      val existingValue = map1.get(entry.getKey())

      if (existingValue == null) {
        map1.put(entry.getKey(), entry.getValue())
      } else {
        map1.put(entry.getKey(), mergeFn.apply(existingValue, entry.getValue()))
      }
    }
    map1
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a list of futures to a single future, combining the values into a list.
   *
   * The `CompletableFuture.allOf(CompletableFuture...)` method is useful
   * but it returns `Void`. This method combines the futures but also
   * returns the resulting value as a list.
   * Effectively, this converts `List<CompletableFuture<T>>` to `CompletableFuture<List<T>>`.
   *
   * If any input future completes exceptionally, the result will also complete exceptionally.
   *
   * @tparam T the type of the values in the list
   * @param futures the futures to convert, may be empty
   * @return a future that combines the input futures as a list
   */
  def combineFuturesAsList[T](
      futures: List[? <: CompletableFuture[? <: T]]): CompletableFuture[List[T]] = {

    val size = futures.size()
    val futuresArray: Array[CompletableFuture[? <: T]] =
      futures.toArray(new Array[CompletableFuture[? <: T]](size))
    CompletableFuture.allOf(futuresArray*)
        .thenApply[List[T]]((unused: Void) => {
          val builder: List[T] = new ArrayList[T](size)
          var i = 0
          while (i < size) {
            builder.add(futuresArray(i).join())
            i += 1
          }
          builder
        })
  }

  /**
   * Collector used at the end of a stream to convert a list of futures to a single future,
   * combining the values into a list.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing a stream of futures to be combined into a single future.
   * This converts `List<CompletableFuture<T>>` to `CompletableFuture<List<T>>`.
   *
   * @tparam S the type of the input futures
   * @tparam T the type of the values
   * @return a collector that combines the input futures as a list
   */
  def toCombinedFuture[T, S <: CompletableFuture[? <: T]](): Collector[S, ?, CompletableFuture[List[T]]] = {
    Collectors.collectingAndThen(
        toImmutableList[S](),
        (list: ImmutableList[S]) => Guavate.combineFuturesAsList[T](list))
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a map of futures to a single future.
   *
   * This is similar to `combineFuturesAsList(List)` but for maps.
   * Effectively, this converts `Map<K, CompletableFuture<V>>` to `CompletableFuture<Map<K, V>>`.
   *
   * If any input future completes exceptionally, the result will also complete exceptionally.
   * The results must be non-null.
   *
   * @tparam K the type of the keys in the map
   * @tparam V the type of the values in the map
   * @tparam F the type of the futures, must not be Void
   * @param futures the futures to convert, may be empty
   * @return a future that combines the input futures as a map
   */
  def combineFuturesAsMap[K, V, F <: CompletableFuture[? <: V]](
      futures: Map[? <: K, ? <: F]): CompletableFuture[Map[K, V]] = {

    val size = futures.size()
    val keyArray = new Array[Object](size)
    val futuresArray = new Array[CompletableFuture[? <: V]](size)
    var index = 0
    val it = futures.entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      keyArray(index) = entry.getKey().asInstanceOf[Object]
      futuresArray(index) = entry.getValue()
      index += 1
    }
    CompletableFuture.allOf(futuresArray*)
        .thenApply[Map[K, V]]((unused: Void) => {
          val builder = ImmutableMap.builderWithExpectedSize[K, V](size)
          var i = 0
          while (i < size) {
            builder.put(keyArray(i).asInstanceOf[K], futuresArray(i).join())
            i += 1
          }
          builder.build()
        })
  }

  /**
   * Collector used at the end of a stream to convert a map of futures to a single future,
   * combining the values into a map.
   *
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing a stream of futures to be combined into a single future.
   * This converts `Map<K, CompletableFuture<V>>` to `CompletableFuture<Map<K, V>>`.
   *
   * @tparam K the type of the keys in the map
   * @tparam V the type of the values in the map
   * @tparam F the type of the input futures
   * @return a collector that combines the input futures as a map
   */
  def toCombinedFutureMap[K, V, F <: CompletableFuture[? <: V]]()
      : Collector[Map.Entry[? <: K, ? <: F], ?, CompletableFuture[Map[K, V]]] = {

    Collectors.collectingAndThen(
        entriesToImmutableMap[K, F](),
        (map: ImmutableMap[K, F]) => Guavate.combineFuturesAsMap[K, V, F](map))
  }

  //-------------------------------------------------------------------------
  /**
   * Polls on a regular frequency until a result is found.
   *
   * Polling is performed via the specified supplier, which must return null until the result is available.
   * If the supplier throws an exception, polling will stop and the future will complete exceptionally.
   *
   * If the future is cancelled, the polling will also be cancelled.
   * It is advisable to consider using a timeout when querying the future.
   *
   * In most cases, there needs to be an initial request, which might return an identifier to query.
   * This pattern may be useful for that case:
   * {{{
   *  return CompletableFuture.supplyAsync(initPollingReturningId(), executorService)
   *      .thenCompose(id -> poll(executorService, delay, freq, performPolling(id)));
   *  });
   * }}}
   *
   * @tparam T the result type
   * @param executorService  the executor service to use for polling
   * @param initialDelay  the initial delay before starting to poll
   * @param frequency  the frequency to poll at
   * @param pollingTask  the task used to poll, returning null when not yet complete
   * @return the future representing the asynchronous operation
   */
  def poll[T](
      executorService: ScheduledExecutorService,
      initialDelay: Duration,
      frequency: Duration,
      pollingTask: Supplier[T]): CompletableFuture[T] = {

    val result = new CompletableFuture[T]()
    val decoratedPollingTask: Runnable = () => pollTask(pollingTask, result)
    val scheduledTask = executorService.scheduleAtFixedRate(
        decoratedPollingTask, initialDelay.toMillis(), frequency.toMillis(), TimeUnit.MILLISECONDS)
    result.whenComplete((r: T, ex: Throwable) => {
      scheduledTask.cancel(true)
      ()
    })
  }

  // the task the executor calls
  private def pollTask[T](
      pollingTask: Supplier[T],
      resultFuture: CompletableFuture[T]): Unit = {

    try {
      val result = pollingTask.get()
      if (result != null) {
        resultFuture.complete(result)
      }
    } catch {
      case ex: RuntimeException =>
        resultFuture.completeExceptionally(ex)
    }
  }

  //-------------------------------------------------------------------------
  // namedThreadFactory() and callerClass() inspect the call stack, so they are true static methods of
  // Guavate, giving the same stack depths and frame classes as the Java implementation
  /**
   * Creates a ThreadFactoryBuilder which names new threads with the name of the calling class plus a unique integer.
   *
   * @return the thread factory builder
   */
  @static def namedThreadFactory(): ThreadFactoryBuilder = {
    namedThreadFactory(callerClass(3).getSimpleName())
  }

  /**
   * Creates a ThreadFactoryBuilder which names new threads with the given name prefix plus a unique integer.
   *
   * @param threadNamePrefix  the name which new thread names should be prefixed by
   * @return the thread factory builder
   */
  @static def namedThreadFactory(threadNamePrefix: String): ThreadFactoryBuilder = {
    new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d")
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a generified `Class` instance.
   *
   * It is not possible in Java generics to get a `Class` object with the desired generic
   * signature, such as `Class<List<String>>`. This method provides a way to get such a value.
   * The method returns the input parameter, but the compiler sees the result as being a different type.
   *
   * Note that the generic part of the resulting type is not checked and can be unsound.
   * The safest choice is to explicitly specify the type you want, by assigning to a variable or constant:
   * {{{
   *   Class<List<String>> cls = genericClass(List.class);
   * }}}
   *
   * @tparam T the partially specified generic type, such as `List` from a constant such as `List.class`
   * @tparam S the fully specified generic type, such as `List<String>`
   * @param cls  the class instance to base the result in, such as `List.class`
   * @return the class instance from the input, with whatever generic parameter is desired
   */
  def genericClass[T, S <: T](cls: Class[T]): Class[S] = {
    cls.asInstanceOf[Class[S]]
  }

  /**
   * Finds the caller class.
   *
   * This takes an argument which is the number of stack levels to look back.
   * This will be 2 to return the caller of this method, 3 to return the caller of the caller, and so on.
   *
   * @param callStackDepth  the depth of the stack to look back
   * @return the caller class
   */
  @static def callerClass(callStackDepth: Int): Class[?] = {
    CallerClassSecurityManager.INSTANCE.callerClass(callStackDepth)
  }

  // on Java 9 or later could use StackWalker, but this is a good choice for Java 8
  private[collect] class CallerClassSecurityManager extends SecurityManager {
    private[collect] def callerClass(callStackDepth: Int): Class[?] = {
      getClassContext()(callStackDepth)
    }
  }

  private[collect] object CallerClassSecurityManager {
    private[collect] val INSTANCE: CallerClassSecurityManager = new CallerClassSecurityManager()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the substring before the first occurrence of the separator.
   *
   * This returns the string before the first occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  def substringBeforeFirst(str: String, separator: String): String = {
    val pos = str.indexOf(separator)
    if (pos < 0) {
      return str
    }
    str.substring(0, pos)
  }

  /**
   * Gets the substring after the first occurrence of the separator.
   *
   * This returns the string after the first occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  def substringAfterFirst(str: String, separator: String): String = {
    val pos = str.indexOf(separator)
    if (pos < 0) {
      return str
    }
    str.substring(pos + separator.length())
  }

  /**
   * Gets the substring before the last occurrence of the separator.
   *
   * This returns the string before the last occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  def substringBeforeLast(str: String, separator: String): String = {
    val pos = str.lastIndexOf(separator)
    if (pos < 0) {
      return str
    }
    str.substring(0, pos)
  }

  /**
   * Gets the substring after the last occurrence of the separator.
   *
   * This returns the string after the last occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  def substringAfterLast(str: String, separator: String): String = {
    val pos = str.lastIndexOf(separator)
    if (pos < 0) {
      return str
    }
    str.substring(pos + separator.length())
  }

}
