/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries

import java.time.LocalDate
import java.util.Map
import java.util.NoSuchElementException
import java.util.OptionalDouble
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.DoubleBinaryOperator
import java.util.function.DoublePredicate
import java.util.function.DoubleUnaryOperator
import java.util.function.Function
import java.util.function.ObjDoubleConsumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import java.util.stream.Stream

import scala.annotation.static

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.function.ObjDoublePredicate
import com.opengamma.strata.collect.tuple.Pair

/**
 * Interface for all local date time-series types containing
 * `double` values.
 *
 * A time-series is similar to both a `SortedMap` of value keyed
 * by date-time and a `List` of date-time to value pairs. As such,
 * the date/times do not have to be evenly spread over time within the series.
 *
 * The distribution of the data will influence which implementation
 * is most appropriate.
 *
 * Implementations must be immutable and thread-safe beans.
 *
 * Note that `Double#NaN` is used internally as a sentinel
 * value and is therefore not allowed as a value.
 */
trait LocalDateDoubleTimeSeries {

  //-------------------------------------------------------------------------
  /**
   * Return the size of this time-series.
   *
   * @return the size of the time-series
   */
  def size(): Int

  /**
   * Indicates if this time-series is empty.
   *
   * @return true if the time-series contains no entries
   */
  def isEmpty(): Boolean

  /**
   * Checks if this time-series contains a value for the specified date.
   *
   * @param date  the date to check for
   * @return true if there is a value associated with the date
   */
  def containsDate(date: LocalDate): Boolean

  /**
   * Gets the value associated with the specified date.
   *
   * The result is an `OptionalDouble` which avoids the need to handle null
   * or exceptions. Use `isPresent()` to check whether the value is present.
   * Use `orElse(double)` to default a missing value.
   *
   * @param date  the date to get the value for
   * @return the value associated with the date, optional empty if the date is not present
   */
  def get(date: LocalDate): OptionalDouble

  //-------------------------------------------------------------------------
  /**
   * Get the earliest date contained in this time-series.
   *
   * If the time-series is empty then `NoSuchElementException` will be thrown.
   *
   * @return the earliest date contained in this time-series
   * @throws java.util.NoSuchElementException if the time-series is empty
   */
  def getEarliestDate(): LocalDate = {
    val exceptionSupplier: Supplier[NoSuchElementException] =
      () => new NoSuchElementException("Unable to return earliest date, time-series is empty")
    dates().findFirst().orElseThrow(exceptionSupplier)
  }

  /**
   * Get the value held for the earliest date contained in this time-series.
   *
   * If the time-series is empty then `NoSuchElementException` will be thrown.
   *
   * @return the value held for the earliest date contained in this time-series
   * @throws java.util.NoSuchElementException if the time-series is empty
   */
  def getEarliestValue(): Double = {
    val exceptionSupplier: Supplier[NoSuchElementException] =
      () => new NoSuchElementException("Unable to return earliest value, time-series is empty")
    values().findFirst().orElseThrow(exceptionSupplier)
  }

  /**
   * Get the latest date contained in this time-series.
   *
   * If the time-series is empty then `NoSuchElementException` will be thrown.
   *
   * @return the latest date contained in this time-series
   * @throws java.util.NoSuchElementException if the time-series is empty
   */
  def getLatestDate(): LocalDate

  /**
   * Get the value held for the latest date contained in this time-series.
   *
   * If the time-series is empty then `NoSuchElementException` will be thrown.
   *
   * @return the value held for the latest date contained in this time-series
   * @throws java.util.NoSuchElementException if the time-series is empty
   */
  def getLatestValue(): Double

  //-------------------------------------------------------------------------
  /**
   * Gets part of this series as a sub-series between two dates.
   *
   * The date-times do not have to match exactly.
   * The sub-series contains all entries between the two dates using a half-open interval.
   * The start date is included, the end date is excluded.
   * The dates may be before or after the end of the time-series.
   *
   * To obtain the series before a specific date, used `LocalDate.MIN` as the first argument.
   * To obtain the series from a specific date, used `LocalDate.MAX` as the second argument.
   *
   * @param startInclusive  the start date, inclusive
   * @param endExclusive  the end date, exclusive
   * @return the sub-series between the dates
   * @throws java.lang.IllegalArgumentException if the end is before the start
   */
  def subSeries(startInclusive: LocalDate, endExclusive: LocalDate): LocalDateDoubleTimeSeries

  /**
   * Gets part of this series as a sub-series, choosing the earliest entries.
   *
   * The sub-series contains the earliest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size starting with the earliest entry
   * @throws java.lang.IllegalArgumentException if the number of items is less than zero
   */
  def headSeries(numPoints: Int): LocalDateDoubleTimeSeries

  /**
   * Gets part of this series as a sub-series, choosing the latest entries.
   *
   * The sub-series contains the latest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size ending with the latest entry
   * @throws java.lang.IllegalArgumentException if the number of items is less than zero
   */
  def tailSeries(numPoints: Int): LocalDateDoubleTimeSeries

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the points of this time-series.
   *
   * This provides access to the entire time-series.
   *
   * @return a stream over the points of this time-series
   */
  def stream(): Stream[LocalDateDoublePoint]

  /**
   * Returns a stream over the dates of this time-series.
   *
   * This is most useful to summarize the dates in the stream, such as calculating
   * the maximum or minimum date, or searching for a specific value.
   *
   * @return a stream over the dates of this time-series
   */
  def dates(): Stream[LocalDate]

  /**
   * Returns a stream over the values of this time-series.
   *
   * This is most useful to summarize the values in the stream, such as calculating
   * the maximum, minimum or average value, or searching for a specific value.
   *
   * @return a stream over the values of this time-series
   */
  def values(): DoubleStream

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each pair in the time series.
   *
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * {{{
   *   base.forEach((date, value) -> System.out.println(date + "=" + value));
   * }}}
   *
   * @param action  the action to be applied to each pair
   */
  def forEach(action: ObjDoubleConsumer[LocalDate]): Unit

  /**
   * Applies an operation to each date in the time series which creates a new date, returning a new time series
   * with the new dates and the points from this time series.
   *
   * This operation creates a new time series with the same data but the dates moved.
   *
   * The operation must not change the dates in a way that reorders them. The mapped dates must be in ascending
   * order or an exception is thrown.
   * {{{
   *   updatedSeries = timeSeries.mapDates(date -> date.plusYears(1));
   * }}}
   *
   * @param mapper  the operation applied to each date in the time series
   * @return a copy of this time series with new dates
   */
  def mapDates(mapper: Function[? >: LocalDate, ? <: LocalDate]): LocalDateDoubleTimeSeries

  /**
   * Applies an operation to each value in the time series.
   *
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * {{{
   *   multiplied = base.mapValues(value -> value * 3);
   * }}}
   *
   * @param mapper  the operator to be applied to the values
   * @return a copy of this series with the mapping applied to the original values
   */
  def mapValues(mapper: DoubleUnaryOperator): LocalDateDoubleTimeSeries

  /**
   * Create a new time-series by filtering this one.
   *
   * The time-series can be filtered by both date and value.
   * Note that if filtering by date range is required, it is likely to be more efficient to
   * use `subSeries(LocalDate, LocalDate)` as that avoids traversal of the whole series.
   *
   * @param predicate  the predicate to use to the filter the elements of the series
   * @return a filtered version of the series
   */
  def filter(predicate: ObjDoublePredicate[LocalDate]): LocalDateDoubleTimeSeries

  //-------------------------------------------------------------------------
  /**
   * Obtains the intersection of a pair of time series.
   *
   * This returns a time-series with the intersection of the dates of the two inputs.
   * The operator is invoked to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *   input series with their values combined together using the function
   */
  def intersection(other: LocalDateDoubleTimeSeries, mapper: DoubleBinaryOperator): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(other, "other")
    ArgChecker.notNull(mapper, "mapper")
    val inOther: Predicate[LocalDateDoublePoint] = pt => other.containsDate(pt.getDate())
    val combine: Function[LocalDateDoublePoint, LocalDateDoublePoint] = pt => LocalDateDoublePoint.of(
        pt.getDate(), mapper.applyAsDouble(pt.getValue(), other.get(pt.getDate()).getAsDouble()))
    new LocalDateDoubleTimeSeriesBuilder()
        .putAll(stream()
            .filter(inOther)
            .map[LocalDateDoublePoint](combine))
        .build()
  }

  /**
   * Obtains the union of a pair of time series.
   *
   * This returns a time-series with the union of the dates of the two inputs.
   * When the same date occurs in both time-series, the operator is invoked to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *   input series with their values combined together using the function
   */
  def union(other: LocalDateDoubleTimeSeries, mapper: DoubleBinaryOperator): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(other, "other")
    ArgChecker.notNull(mapper, "mapper")
    val builder = new LocalDateDoubleTimeSeriesBuilder(stream())
    other.stream().forEach(pt => builder.merge(pt, mapper))
    builder.build()
  }

  /**
   * Partition the time-series into a pair of distinct series using a predicate.
   *
   * Points in the time-series which match the predicate will be put into the first series,
   * whilst those points which do not match will be put into the second.
   *
   * @param predicate  predicate used to test the points in the time-series
   * @return a `Pair` containing two time-series. The first is a series
   *   made of all the points in this series which match the predicate. The
   *   second is a series made of the points which do not match.
   */
  def partition(predicate: ObjDoublePredicate[LocalDate]): Pair[LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries] = {
    val test: Predicate[LocalDateDoublePoint] = pt => predicate.test(pt.getDate(), pt.getValue())
    val partitioned: Map[java.lang.Boolean, LocalDateDoubleTimeSeries] = stream()
        .collect(
            Collectors.partitioningBy(
                test,
                LocalDateDoubleTimeSeries.collector()))

    Pair.of(partitioned.get(java.lang.Boolean.TRUE), partitioned.get(java.lang.Boolean.FALSE))
  }

  /**
   * Partition the time-series into a pair of distinct series using a predicate.
   *
   * Points in the time-series whose values match the predicate will be put into the first series,
   * whilst those points whose values do not match will be put into the second.
   *
   * @param predicate  predicate used to test the points in the time-series
   * @return a `Pair` containing two time-series. The first is a series
   *   made of all the points in this series which match the predicate. The
   *   second is a series made of the points which do not match.
   */
  def partitionByValue(predicate: DoublePredicate): Pair[LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries] = {
    partition((obj, value) => predicate.test(value))
  }

  /**
   * Return a builder populated with the values from this series.
   *
   * This can be used to mutate the time-series.
   *
   * @return a builder containing the point from this time-series
   */
  def toBuilder(): LocalDateDoubleTimeSeriesBuilder

}

object LocalDateDoubleTimeSeries {

  /**
   * Returns an empty time-series. Generally a singleton instance
   * is returned.
   *
   * @return an empty time-series
   */
  @static def empty(): LocalDateDoubleTimeSeries = {
    SparseLocalDateDoubleTimeSeries.EMPTY
  }

  /**
   * Obtains a time-series containing a single date and value.
   *
   * @param date  the singleton date
   * @param value  the singleton value
   * @return the time-series
   */
  @static def of(date: LocalDate, value: Double): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(date, "date")
    builder().put(date, value).build()
  }

  /**
   * Creates an empty builder, used to create time-series.
   *
   * The builder has methods to create and modify a time-series.
   *
   * @return the time-series builder
   */
  @static def builder(): LocalDateDoubleTimeSeriesBuilder = {
    new LocalDateDoubleTimeSeriesBuilder()
  }

  /**
   * Returns a collector that can be used to create a time-series from a stream of points.
   *
   * @return the time-series collector
   */
  @static def collector(): Collector[LocalDateDoublePoint, LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries] = {
    createCollector()
  }

  // lambdas are created outside the static interface method to avoid illegal interface lambda helpers
  private def createCollector()
      : Collector[LocalDateDoublePoint, LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries] = {

    val supplier: Supplier[LocalDateDoubleTimeSeriesBuilder] = () => new LocalDateDoubleTimeSeriesBuilder()
    val accumulator: BiConsumer[LocalDateDoubleTimeSeriesBuilder, LocalDateDoublePoint] =
      (builder, point) => builder.put(point)
    val combiner: BinaryOperator[LocalDateDoubleTimeSeriesBuilder] = (builder, other) => builder.putAll(other)
    val finisher: Function[LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries] = builder => builder.build()
    Collector.of[LocalDateDoublePoint, LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries](
        supplier,
        accumulator,
        combiner,
        finisher)
  }

}
