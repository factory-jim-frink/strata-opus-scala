/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.util.Collection
import java.util.Comparator
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Optional
import java.util.Spliterator
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.ToDoubleBiFunction
import java.util.function.ToDoubleFunction
import java.util.function.ToIntBiFunction
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Streams

/**
 * A stream implementation based on `Map.Entry`.
 *
 * This stream wraps a `Stream<Map.Entry>`, providing convenient methods for
 * manipulating the keys and values. Unlike a `Map`, the keys in a `MapStream`
 * do not have to be unique, although certain methods will fail if they are not unique.
 *
 * @tparam K the key type
 * @tparam V the value type
 */
final class MapStream[K, V] private (
    /** The stream of map entries. */
    private val underlying: Stream[Map.Entry[K, V]])
    extends Stream[Map.Entry[K, V]] {

  //-------------------------------------------------------------------------
  /**
   * Returns the keys as a stream, dropping the values.
   *
   * A `MapStream` may contain the same key more than once, so callers
   * may need to call `Stream.distinct()` on the result.
   *
   * @return a stream of the keys
   */
  def keys(): Stream[K] = {
    underlying.map[K]((e: Map.Entry[K, V]) => e.getKey())
  }

  /**
   * Returns the values as a stream, dropping the keys.
   *
   * @return a stream of the values
   */
  def values(): Stream[V] = {
    underlying.map[V]((e: Map.Entry[K, V]) => e.getValue())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a stream where the keys and values are swapped.
   *
   * @return a stream with swapped keys and values
   */
  def inverse(): MapStream[V, K] = {
    MapStream.wrap(underlying.map[Map.Entry[V, K]]((e: Map.Entry[K, V]) => Guavate.entry(e.getValue(), e.getKey())))
  }

  //-------------------------------------------------------------------------
  /**
   * Filters the stream by applying the predicate function to each key and value.
   *
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each key and value in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  def filter(predicate: BiPredicate[? >: K, ? >: V]): MapStream[K, V] = {
    MapStream.wrap(underlying.filter((e: Map.Entry[K, V]) => predicate.test(e.getKey(), e.getValue())))
  }

  /**
   * Filters the stream by applying the predicate function to each key.
   *
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each key in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  def filterKeys(predicate: Predicate[? >: K]): MapStream[K, V] = {
    MapStream.wrap(underlying.filter((e: Map.Entry[K, V]) => predicate.test(e.getKey())))
  }

  /**
   * Filters the stream checking the type of each key.
   *
   * Entries are included in the returned stream if the key is an instance of the specified type.
   *
   * @tparam R the type to filter to
   * @param castToClass  the class to filter the keys to
   * @return a stream including only those entries where the key is an instance of the specified type
   */
  def filterKeys[R](castToClass: Class[R]): MapStream[R, V] = {
    MapStream.wrap(underlying
        .filter((e: Map.Entry[K, V]) => castToClass.isInstance(e.getKey()))
        .map[Map.Entry[R, V]]((e: Map.Entry[K, V]) => Guavate.entry(castToClass.cast(e.getKey()), e.getValue())))
  }

  /**
   * Filters the stream by applying the predicate function to each value.
   *
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each value in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  def filterValues(predicate: Predicate[? >: V]): MapStream[K, V] = {
    MapStream.wrap(underlying.filter((e: Map.Entry[K, V]) => predicate.test(e.getValue())))
  }

  /**
   * Filters the stream checking the type of each value.
   *
   * Entries are included in the returned stream if the value is an instance of the specified type.
   *
   * @tparam R the type to filter to
   * @param castToClass  the class to filter the values to
   * @return a stream including only those entries where the value is an instance of the specified type
   */
  def filterValues[R](castToClass: Class[R]): MapStream[K, R] = {
    MapStream.wrap(underlying
        .filter((e: Map.Entry[K, V]) => castToClass.isInstance(e.getValue()))
        .map[Map.Entry[K, R]]((e: Map.Entry[K, V]) => Guavate.entry(e.getKey(), castToClass.cast(e.getValue()))))
  }

  //-------------------------------------------------------------------------
  /**
   * Transforms the keys in the stream by applying a mapper function to each key.
   *
   * The values are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new key
   * @tparam R the type of the new keys
   * @return a stream of entries with the keys transformed and the values unchanged
   */
  def mapKeys[R](mapper: Function[? >: K, ? <: R]): MapStream[R, V] = {
    MapStream.wrap(underlying.map[Map.Entry[R, V]](
        (e: Map.Entry[K, V]) => Guavate.entry[R, V](mapper.apply(e.getKey()), e.getValue())))
  }

  /**
   * Transforms the keys in the stream by applying a mapper function to each key and value.
   *
   * The values are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new key
   * @tparam R the type of the new keys
   * @return a stream of entries with the keys transformed and the values unchanged
   */
  def mapKeys[R](mapper: BiFunction[? >: K, ? >: V, ? <: R]): MapStream[R, V] = {
    MapStream.wrap(underlying.map[Map.Entry[R, V]](
        (e: Map.Entry[K, V]) => Guavate.entry[R, V](mapper.apply(e.getKey(), e.getValue()), e.getValue())))
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each value.
   *
   * The keys are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new value
   * @tparam R the type of the new values
   * @return a stream of entries with the values transformed and the keys unchanged
   */
  def mapValues[R](mapper: Function[? >: V, ? <: R]): MapStream[K, R] = {
    MapStream.wrap(underlying.map[Map.Entry[K, R]](
        (e: Map.Entry[K, V]) => Guavate.entry[K, R](e.getKey(), mapper.apply(e.getValue()))))
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each key and value.
   *
   * The keys are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new value
   * @tparam R the type of the new values
   * @return a stream of entries with the values transformed and the keys unchanged
   */
  def mapValues[R](mapper: BiFunction[? >: K, ? >: V, ? <: R]): MapStream[K, R] = {
    MapStream.wrap(underlying.map[Map.Entry[K, R]](
        (e: Map.Entry[K, V]) => Guavate.entry[K, R](e.getKey(), mapper.apply(e.getKey(), e.getValue()))))
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value.
   *
   * The result of this method is a `MapStream`, unlike `map(BiFunction)`.
   *
   * @param mapper  a mapper function whose return value is the new key-value entry
   * @tparam RK the type of the new keys
   * @tparam RV the type of the new values
   * @return a stream of entries with the keys and values transformed
   */
  def mapBoth[RK, RV](mapper: BiFunction[? >: K, ? >: V, Map.Entry[RK, RV]]): MapStream[RK, RV] = {
    MapStream.wrap(underlying.map[Map.Entry[RK, RV]]((e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue())))
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @tparam R the type of elements in the new stream
   * @return a stream containing the values returned from the mapper function
   */
  def map[R](mapper: BiFunction[? >: K, ? >: V, ? <: R]): Stream[R] = {
    underlying.map[R]((e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue()))
  }

  /**
   * Transforms the entries in the stream to doubles by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the double values returned from the mapper function
   */
  def mapToDouble(mapper: ToDoubleBiFunction[? >: K, ? >: V]): DoubleStream = {
    underlying.mapToDouble((e: Map.Entry[K, V]) => mapper.applyAsDouble(e.getKey(), e.getValue()))
  }

  /**
   * Transforms the entries in the stream to integers by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the integer values returned from the mapper function
   */
  def mapToInt(mapper: ToIntBiFunction[? >: K, ? >: V]): IntStream = {
    underlying.mapToInt((e: Map.Entry[K, V]) => mapper.applyAsInt(e.getKey(), e.getValue()))
  }

  //-------------------------------------------------------------------------
  /**
   * Transforms the keys in the stream by applying a mapper function to each key.
   *
   * The new keys produced will be associated with the original value.
   *
   * @param mapper  a mapper function whose return values are the keys in the new stream
   * @tparam R the type of the new keys
   * @return a stream of entries with new keys from the mapper function assigned to the values
   */
  def flatMapKeys[R](mapper: Function[? >: K, Stream[R]]): MapStream[R, V] = {
    MapStream.wrap(underlying.flatMap[Map.Entry[R, V]]((e: Map.Entry[K, V]) =>
        mapper.apply(e.getKey()).map[Map.Entry[R, V]]((newKey: R) => Guavate.entry(newKey, e.getValue()))))
  }

  /**
   * Transforms the keys in the stream by applying a mapper function to each key and value.
   *
   * The new keys produced will be associated with the original value.
   *
   * For example this could turn a `MapStream<List<String>, LocalDate>` into a
   * `MapStream<String, LocalDate>`
   *
   * @param mapper  a mapper function whose return values are the keys in the new stream
   * @tparam R the type of the new keys
   * @return a stream of entries with new keys from the mapper function assigned to the values
   */
  def flatMapKeys[R](mapper: BiFunction[? >: K, ? >: V, Stream[R]]): MapStream[R, V] = {
    MapStream.wrap(underlying.flatMap[Map.Entry[R, V]]((e: Map.Entry[K, V]) =>
        mapper.apply(e.getKey(), e.getValue())
            .map[Map.Entry[R, V]]((newKey: R) => Guavate.entry(newKey, e.getValue()))))
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each value.
   *
   * The new values produced will be associated with the original key.
   *
   * For example this could turn a `MapStream<LocalDate, List<String>>` into a
   * `MapStream<LocalDate, String>`
   *
   * @param mapper  a mapper function whose return values are the values in the new stream
   * @tparam R the type of the new values
   * @return a stream of entries with new values from the mapper function assigned to the keys
   */
  def flatMapValues[R](mapper: Function[? >: V, Stream[R]]): MapStream[K, R] = {
    MapStream.wrap(underlying.flatMap[Map.Entry[K, R]]((e: Map.Entry[K, V]) =>
        mapper.apply(e.getValue()).map[Map.Entry[K, R]]((newValue: R) => Guavate.entry(e.getKey(), newValue))))
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each key and value.
   *
   * The new values produced will be associated with the original key.
   *
   * For example this could turn a `MapStream<LocalDate, List<String>>` into a
   * `MapStream<LocalDate, String>`
   *
   * @param mapper  a mapper function whose return values are the values in the new stream
   * @tparam R the type of the new values
   * @return a stream of entries with new values from the mapper function assigned to the keys
   */
  def flatMapValues[R](mapper: BiFunction[? >: K, ? >: V, Stream[R]]): MapStream[K, R] = {
    MapStream.wrap(underlying.flatMap[Map.Entry[K, R]]((e: Map.Entry[K, V]) =>
        mapper.apply(e.getKey(), e.getValue())
            .map[Map.Entry[K, R]]((newValue: R) => Guavate.entry(e.getKey(), newValue))))
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value to produce a stream of
   * elements, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @tparam R the type of the elements in the new stream
   * @return a stream containing the values returned from the mapper function
   */
  def flatMap[R](mapper: BiFunction[? >: K, ? >: V, Stream[R]]): Stream[R] = {
    underlying.flatMap[R]((e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue()))
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value.
   *
   * The result of this method is a `MapStream`, unlike `flatMap(BiFunction)`.
   *
   * @param mapper  a mapper function whose return value is a stream of new key-value entries
   * @tparam RK the type of the new keys
   * @tparam RV the type of the new values
   * @return a stream of entries with the keys and values transformed
   */
  def flatMapBoth[RK, RV](mapper: BiFunction[? >: K, ? >: V, Stream[Map.Entry[RK, RV]]]): MapStream[RK, RV] = {
    MapStream.wrap(underlying.flatMap[Map.Entry[RK, RV]](
        (e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue())))
  }

  /**
   * Transforms the entries in the stream to doubles by applying a mapper function to each key and value to produce
   * a stream of doubles, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the double values returned from the mapper function
   */
  def flatMapToDouble(mapper: BiFunction[? >: K, ? >: V, ? <: DoubleStream]): DoubleStream = {
    underlying.flatMapToDouble((e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue()))
  }

  /**
   * Transforms the entries in the stream to integers by applying a mapper function to each key and value to produce
   * a stream of integers, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the integer values returned from the mapper function
   */
  def flatMapToInt(mapper: BiFunction[? >: K, ? >: V, ? <: IntStream]): IntStream = {
    underlying.flatMapToInt((e: Map.Entry[K, V]) => mapper.apply(e.getKey(), e.getValue()))
  }

  //-----------------------------------------------------------------------
  /**
   * Sorts the entries in the stream by comparing the keys using their natural ordering.
   *
   * If the keys in this map stream are not `Comparable` a `java.lang.ClassCastException` may be thrown.
   * In this case use `sortedKeys(Comparator)` instead.
   *
   * @return the sorted stream
   */
  def sortedKeys(): MapStream[K, V] = {
    val comparator = Comparator.naturalOrder[Comparable[Object]]().asInstanceOf[Comparator[K]]
    MapStream.wrap(underlying.sorted(
        (e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getKey(), e2.getKey())))
  }

  /**
   * Sorts the entries in the stream by comparing the keys using the supplied comparator.
   *
   * @param comparator  a comparator of keys
   * @return the sorted stream
   */
  def sortedKeys(comparator: Comparator[? >: K]): MapStream[K, V] = {
    MapStream.wrap(underlying.sorted(
        (e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getKey(), e2.getKey())))
  }

  /**
   * Sorts the entries in the stream by comparing the values using their natural ordering.
   *
   * If the values in this map stream are not `Comparable` a `java.lang.ClassCastException` may be thrown.
   * In this case use `sortedValues(Comparator)` instead.
   *
   * @return the sorted stream
   */
  def sortedValues(): MapStream[K, V] = {
    val comparator = Comparator.naturalOrder[Comparable[Object]]().asInstanceOf[Comparator[V]]
    MapStream.wrap(underlying.sorted(
        (e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getValue(), e2.getValue())))
  }

  /**
   * Sorts the entries in the stream by comparing the values using the supplied comparator.
   *
   * @param comparator  a comparator of values
   * @return the sorted stream
   */
  def sortedValues(comparator: Comparator[? >: V]): MapStream[K, V] = {
    MapStream.wrap(underlying.sorted(
        (e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getValue(), e2.getValue())))
  }

  //-----------------------------------------------------------------------
  /**
   * Finds the minimum entry in the stream by comparing the keys using the supplied comparator.
   *
   * This is a terminal operation.
   *
   * @param comparator  a comparator of keys
   * @return the minimum entry
   */
  def minKeys(comparator: Comparator[? >: K]): Optional[Map.Entry[K, V]] = {
    underlying.min((e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getKey(), e2.getKey()))
  }

  /**
   * Finds the minimum entry in the stream by comparing the values using the supplied comparator.
   *
   * This is a terminal operation.
   *
   * @param comparator  a comparator of values
   * @return the minimum entry
   */
  def minValues(comparator: Comparator[? >: V]): Optional[Map.Entry[K, V]] = {
    underlying.min((e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getValue(), e2.getValue()))
  }

  /**
   * Finds the maximum entry in the stream by comparing the keys using the supplied comparator.
   *
   * This is a terminal operation.
   *
   * @param comparator  a comparator of keys
   * @return the maximum entry
   */
  def maxKeys(comparator: Comparator[? >: K]): Optional[Map.Entry[K, V]] = {
    underlying.max((e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getKey(), e2.getKey()))
  }

  /**
   * Finds the maximum entry in the stream by comparing the values using the supplied comparator.
   *
   * This is a terminal operation.
   *
   * @param comparator  a comparator of values
   * @return the maximum entry
   */
  def maxValues(comparator: Comparator[? >: V]): Optional[Map.Entry[K, V]] = {
    underlying.max((e1: Map.Entry[K, V], e2: Map.Entry[K, V]) => comparator.compare(e1.getValue(), e2.getValue()))
  }

  //-----------------------------------------------------------------------
  /**
   * Returns whether any elements of this stream match the provided predicate.
   *
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether any of the entries matched the predicate
   */
  def anyMatch(predicate: BiPredicate[? >: K, ? >: V]): Boolean = {
    underlying.anyMatch((e: Map.Entry[K, V]) => predicate.test(e.getKey(), e.getValue()))
  }

  /**
   * Returns whether all elements of this stream match the provided predicate.
   *
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether all of the entries matched the predicate
   */
  def allMatch(predicate: BiPredicate[? >: K, ? >: V]): Boolean = {
    underlying.allMatch((e: Map.Entry[K, V]) => predicate.test(e.getKey(), e.getValue()))
  }

  /**
   * Returns whether no elements of this stream match the provided predicate.
   *
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether none of the entries matched the predicate
   */
  def noneMatch(predicate: BiPredicate[? >: K, ? >: V]): Boolean = {
    underlying.noneMatch((e: Map.Entry[K, V]) => predicate.test(e.getKey(), e.getValue()))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable map built from the entries in the stream.
   *
   * The keys must be unique or an exception will be thrown.
   * Duplicate keys can be handled using `toMap(BiFunction)`.
   *
   * This is a terminal operation.
   *
   * @return an immutable map built from the entries in the stream
   * @throws java.lang.IllegalArgumentException if the same key occurs more than once
   */
  def toMap(): ImmutableMap[K, V] = {
    val collector: Collector[Map.Entry[K, V], ?, ImmutableMap[K, V]] = Guavate.toImmutableMap[Map.Entry[K, V], K, V](
        (e: Map.Entry[K, V]) => e.getKey(),
        (e: Map.Entry[K, V]) => e.getValue())
    underlying.collect(collector)
  }

  /**
   * Returns an immutable map built from the entries in the stream.
   *
   * If the same key maps to multiple values the merge function is invoked with both values and the return
   * value is used in the map.
   *
   * Can be used with `concat(MapStream, MapStream)` to merge immutable
   * maps with duplicate keys.
   *
   * For example, to merge immutable maps with duplicate keys preferring values in the first map:
   * {{{
   *   MapStream.concat(mapStreamA, mapStreamB).toMap((a,b) -> a);
   * }}}
   *
   * This is a terminal operation.
   *
   * @param mergeFn  function used to merge values when the same key appears multiple times in the stream
   * @return an immutable map built from the entries in the stream
   */
  def toMap(mergeFn: BiFunction[? >: V, ? >: V, ? <: V]): ImmutableMap[K, V] = {
    underlying.collect(Guavate.toImmutableMap[Map.Entry[K, V], K, V](
        (e: Map.Entry[K, V]) => e.getKey(),
        (e: Map.Entry[K, V]) => e.getValue(),
        mergeFn))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable map built from the entries in the stream, grouping by key.
   *
   * Entries are grouped based on the equality of the key.
   *
   * This is a terminal operation.
   *
   * @return an immutable map built from the entries in the stream
   */
  def toMapGrouping(): ImmutableMap[K, List[V]] = {
    toMapGrouping(Collectors.toList[V]())
  }

  /**
   * Returns a stream built from a map of the entries in the stream, grouped by key.
   *
   * Entries are grouped based on the equality of the key.
   *
   * @return a stream where the values have been grouped
   */
  def groupingAndThen(): MapStream[K, List[V]] = {
    MapStream.of(toMapGrouping())
  }

  /**
   * Returns an immutable map built from the entries in the stream, grouping by key.
   *
   * Entries are grouped based on the equality of the key.
   * The collector allows the values to be flexibly combined.
   *
   * This is a terminal operation.
   *
   * @tparam A the internal collector type
   * @tparam R the type of the combined values
   * @param valueCollector  the collector used to combined the values
   * @return an immutable map built from the entries in the stream
   */
  def toMapGrouping[A, R](valueCollector: Collector[? >: V, A, R]): ImmutableMap[K, R] = {
    val downstream: Collector[Map.Entry[K, V], ?, R] =
      Collectors.mapping[Map.Entry[K, V], V, A, R]((e: Map.Entry[K, V]) => e.getValue(), valueCollector)
    underlying.collect(Collectors.collectingAndThen(
        Collectors.groupingBy(
            (e: Map.Entry[K, V]) => e.getKey(),
            () => new LinkedHashMap[K, R](),
            downstream),
        (m: LinkedHashMap[K, R]) => ImmutableMap.copyOf[K, R](m)))
  }

  /**
   * Returns a stream built from a map of the entries in the stream, grouped by key.
   *
   * Entries are grouped based on the equality of the key.
   * The collector allows the values to be flexibly combined.
   *
   * @tparam A the internal collector type
   * @tparam R the type of the combined values
   * @param valueCollector  the collector used to combined the values
   * @return a stream where the values have been grouped
   */
  def groupingAndThen[A, R](valueCollector: Collector[? >: V, A, R]): MapStream[K, R] = {
    MapStream.of(toMapGrouping(valueCollector))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable list multimap built from the entries in the stream.
   *
   * This is a terminal operation.
   *
   * @return an immutable list multimap built from the entries in the stream
   */
  def toListMultimap(): ImmutableListMultimap[K, V] = {
    underlying.collect(Guavate.toImmutableListMultimap[Map.Entry[K, V], K, V](
        (e: Map.Entry[K, V]) => e.getKey(),
        (e: Map.Entry[K, V]) => e.getValue()))
  }

  /**
   * Returns an immutable set multimap built from the entries in the stream.
   *
   * This is a terminal operation.
   *
   * @return an immutable set multimap built from the entries in the stream
   */
  def toSetMultimap(): ImmutableSetMultimap[K, V] = {
    underlying.collect(Guavate.toImmutableSetMultimap[Map.Entry[K, V], K, V](
        (e: Map.Entry[K, V]) => e.getKey(),
        (e: Map.Entry[K, V]) => e.getValue()))
  }

  /**
   * Performs an action for each entry in the stream, passing the key and value to the action.
   *
   * This is a terminal operation.
   *
   * @param action  an action performed for each entry in the stream
   */
  def forEach(action: BiConsumer[? >: K, ? >: V]): Unit = {
    underlying.forEach((e: Map.Entry[K, V]) => action.accept(e.getKey(), e.getValue()))
  }

  //-------------------------------------------------------------------------
  override def filter(predicate: Predicate[? >: Map.Entry[K, V]]): MapStream[K, V] = {
    MapStream.wrap(underlying.filter(predicate))
  }

  override def map[R](mapper: Function[? >: Map.Entry[K, V], ? <: R]): Stream[R] = {
    underlying.map(mapper)
  }

  override def mapToInt(mapper: ToIntFunction[? >: Map.Entry[K, V]]): IntStream = {
    underlying.mapToInt(mapper)
  }

  override def mapToLong(mapper: ToLongFunction[? >: Map.Entry[K, V]]): LongStream = {
    underlying.mapToLong(mapper)
  }

  override def mapToDouble(mapper: ToDoubleFunction[? >: Map.Entry[K, V]]): DoubleStream = {
    underlying.mapToDouble(mapper)
  }

  override def flatMap[R](mapper: Function[? >: Map.Entry[K, V], ? <: Stream[? <: R]]): Stream[R] = {
    underlying.flatMap(mapper)
  }

  override def flatMapToInt(mapper: Function[? >: Map.Entry[K, V], ? <: IntStream]): IntStream = {
    underlying.flatMapToInt(mapper)
  }

  override def flatMapToLong(mapper: Function[? >: Map.Entry[K, V], ? <: LongStream]): LongStream = {
    underlying.flatMapToLong(mapper)
  }

  override def flatMapToDouble(mapper: Function[? >: Map.Entry[K, V], ? <: DoubleStream]): DoubleStream = {
    underlying.flatMapToDouble(mapper)
  }

  override def distinct(): MapStream[K, V] = {
    MapStream.wrap(underlying.distinct())
  }

  override def sorted(): MapStream[K, V] = {
    MapStream.wrap(underlying.sorted())
  }

  override def sorted(comparator: Comparator[? >: Map.Entry[K, V]]): MapStream[K, V] = {
    MapStream.wrap(underlying.sorted(comparator))
  }

  override def peek(action: Consumer[? >: Map.Entry[K, V]]): MapStream[K, V] = {
    MapStream.wrap(underlying.peek(action))
  }

  override def limit(maxSize: Long): MapStream[K, V] = {
    MapStream.wrap(underlying.limit(maxSize))
  }

  override def skip(n: Long): MapStream[K, V] = {
    MapStream.wrap(underlying.skip(n))
  }

  override def forEach(action: Consumer[? >: Map.Entry[K, V]]): Unit = {
    underlying.forEach(action)
  }

  override def forEachOrdered(action: Consumer[? >: Map.Entry[K, V]]): Unit = {
    underlying.forEachOrdered(action)
  }

  override def toArray(): Array[Object] = {
    underlying.toArray()
  }

  override def toArray[A](generator: IntFunction[Array[A & Object]]): Array[A & Object] = {
    underlying.toArray(generator)
  }

  override def reduce(identity: Map.Entry[K, V], accumulator: BinaryOperator[Map.Entry[K, V]]): Map.Entry[K, V] = {
    underlying.reduce(identity, accumulator)
  }

  override def reduce(accumulator: BinaryOperator[Map.Entry[K, V]]): Optional[Map.Entry[K, V]] = {
    underlying.reduce(accumulator)
  }

  override def reduce[U](
      identity: U,
      accumulator: BiFunction[U, ? >: Map.Entry[K, V], U],
      combiner: BinaryOperator[U]): U = {

    underlying.reduce(identity, accumulator, combiner)
  }

  override def collect[R](
      supplier: Supplier[R],
      accumulator: BiConsumer[R, ? >: Map.Entry[K, V]],
      combiner: BiConsumer[R, R]): R = {

    underlying.collect(supplier, accumulator, combiner)
  }

  override def collect[R, A](collector: Collector[? >: Map.Entry[K, V], A, R]): R = {
    underlying.collect(collector)
  }

  override def min(comparator: Comparator[? >: Map.Entry[K, V]]): Optional[Map.Entry[K, V]] = {
    underlying.min(comparator)
  }

  override def max(comparator: Comparator[? >: Map.Entry[K, V]]): Optional[Map.Entry[K, V]] = {
    underlying.max(comparator)
  }

  override def count(): Long = {
    underlying.count()
  }

  override def anyMatch(predicate: Predicate[? >: Map.Entry[K, V]]): Boolean = {
    underlying.anyMatch(predicate)
  }

  override def allMatch(predicate: Predicate[? >: Map.Entry[K, V]]): Boolean = {
    underlying.allMatch(predicate)
  }

  override def noneMatch(predicate: Predicate[? >: Map.Entry[K, V]]): Boolean = {
    underlying.noneMatch(predicate)
  }

  override def findFirst(): Optional[Map.Entry[K, V]] = {
    underlying.findFirst()
  }

  override def findAny(): Optional[Map.Entry[K, V]] = {
    underlying.findAny()
  }

  override def iterator(): Iterator[Map.Entry[K, V]] = {
    underlying.iterator()
  }

  override def spliterator(): Spliterator[Map.Entry[K, V]] = {
    underlying.spliterator()
  }

  override def isParallel(): Boolean = {
    underlying.isParallel()
  }

  override def sequential(): MapStream[K, V] = {
    MapStream.wrap(underlying.sequential())
  }

  override def parallel(): MapStream[K, V] = {
    MapStream.wrap(underlying.parallel())
  }

  override def unordered(): MapStream[K, V] = {
    MapStream.wrap(underlying.unordered())
  }

  override def onClose(closeHandler: Runnable): MapStream[K, V] = {
    MapStream.wrap(underlying.onClose(closeHandler))
  }

  override def close(): Unit = {
    underlying.close()
  }

}

object MapStream {

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the entries in the map.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @param map  the map to wrap
   * @return a stream over the entries in the map
   */
  def of[K, V](map: Map[K, V]): MapStream[K, V] = {
    new MapStream[K, V](map.entrySet().stream())
  }

  /**
   * Returns a stream over all the entries in the multimap.
   *
   * This will typically create a stream with duplicate keys.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @param multimap  the multimap to wrap
   * @return a stream over the entries in the multimap
   */
  def of[K, V](multimap: Multimap[K, V]): MapStream[K, V] = {
    new MapStream[K, V](multimap.entries().stream())
  }

  /**
   * Returns a stream of map entries where the keys and values are taken from a collection.
   *
   * @tparam V the key and value type
   * @param collection  the collection
   * @return a stream of map entries derived from the values in the collection
   */
  def of[V](collection: Collection[V]): MapStream[V, V] = {
    of(collection.stream())
  }

  /**
   * Returns a stream of map entries where the keys and values are taken from a stream.
   *
   * @tparam V the key and value type
   * @param stream  the stream
   * @return a stream of map entries derived from the values in the stream
   */
  def of[V](stream: Stream[V]): MapStream[V, V] = {
    of(stream, (key: V) => key)
  }

  /**
   * Returns a stream of map entries where the values are taken from a collection
   * and the keys are created by applying a function to each value.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @param collection  the collection of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the collection
   */
  def of[K, V](collection: Collection[V], keyFunction: Function[? >: V, ? <: K]): MapStream[K, V] = {
    of(collection.stream(), keyFunction)
  }

  /**
   * Returns a stream of map entries where the keys and values are extracted from a
   * collection by applying a function to each item in the collection.
   *
   * @tparam T the collection type
   * @tparam K the key type
   * @tparam V the value type
   * @param collection  the collection of values
   * @param keyFunction  a function which returns the key
   * @param valueFunction  a function which returns the value
   * @return a stream of map entries derived from the collection
   */
  def of[T, K, V](
      collection: Collection[T],
      keyFunction: Function[? >: T, ? <: K],
      valueFunction: Function[? >: T, ? <: V]): MapStream[K, V] = {

    of(collection.stream(), keyFunction, valueFunction)
  }

  /**
   * Returns a stream of map entries where the values are taken from a stream
   * and the keys are created by applying a function to each value.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @param stream  the stream of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the stream
   */
  def of[K, V](stream: Stream[V], keyFunction: Function[? >: V, ? <: K]): MapStream[K, V] = {
    new MapStream[K, V](stream.map[Map.Entry[K, V]]((v: V) => Guavate.entry[K, V](keyFunction.apply(v), v)))
  }

  /**
   * Returns a stream of map entries where the keys and values are extracted from a
   * stream by applying a function to each item in the stream.
   *
   * @tparam T the collection type
   * @tparam K the key type
   * @tparam V the value type
   * @param stream  the stream of values
   * @param keyFunction  a function which returns the key for a value
   * @param valueFunction  a function which returns the value
   * @return a stream of map entries derived from the stream
   */
  def of[T, K, V](
      stream: Stream[T],
      keyFunction: Function[? >: T, ? <: K],
      valueFunction: Function[? >: T, ? <: V]): MapStream[K, V] = {

    new MapStream[K, V](stream.map[Map.Entry[K, V]](
        (item: T) => Guavate.entry[K, V](keyFunction.apply(item), valueFunction.apply(item))))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a map stream that combines two other streams, continuing until either stream ends.
   *
   * Note that this can produce a stream with non-unique keys.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @param keyStream  the stream of keys
   * @param valueStream  the stream of values
   * @return a stream of map entries derived from the stream
   */
  def zip[K, V](keyStream: Stream[K], valueStream: Stream[V]): MapStream[K, V] = {
    new MapStream[K, V](Guavate.zip[K, V, Map.Entry[K, V]](keyStream, valueStream, (k: K, v: V) => Guavate.entry(k, v)))
  }

  /**
   * Returns a stream of map entries where each key is the index of the value in the original stream.
   *
   * @tparam V the value type
   * @param stream  the stream of values
   * @return a stream of map entries derived from the stream
   */
  def zipWithIndex[V](stream: Stream[V]): MapStream[Integer, V] = {
    val zipped: Stream[Map.Entry[Integer, V]] =
      Streams.mapWithIndex[V, Map.Entry[Integer, V]](
          stream,
          (value: V, index: Long) => Guavate.entry[Integer, V](Integer.valueOf(Math.toIntExact(index)), value))
    new MapStream[Integer, V](zipped)
  }

  /**
   * Returns an empty map stream.
   *
   * @tparam K the key type
   * @tparam V the value type
   * @return an empty map stream
   */
  def empty[K, V](): MapStream[K, V] = {
    new MapStream[K, V](Stream.empty[Map.Entry[K, V]]())
  }

  /**
   * Creates a stream of map entries whose elements are those of the first stream followed by those of the second
   * stream.
   *
   * @param a  the first stream of entries
   * @param b  the second stream of entries
   * @tparam K the key type
   * @tparam V the value type
   * @return the concatenation of the two input streams
   */
  def concat[K, V](
      a: MapStream[? <: K, ? <: V],
      b: MapStream[? <: K, ? <: V]): MapStream[K, V] = {

    val kvMapStream = new MapStream[K, V](Streams.concat[Map.Entry[K, V]](
        a.asInstanceOf[Stream[? <: Map.Entry[K, V]]],
        b.asInstanceOf[Stream[? <: Map.Entry[K, V]]]))
    kvMapStream
  }

  //-------------------------------------------------------------------------
  private def wrap[K, V](underlying: Stream[Map.Entry[K, V]]): MapStream[K, V] = {
    new MapStream[K, V](underlying)
  }

}
