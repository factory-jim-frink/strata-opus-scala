/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries

import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.Collection
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.OptionalDouble
import java.util.SortedMap
import java.util.TreeMap
import java.util.function.BiFunction
import java.util.function.DoubleBinaryOperator
import java.util.stream.Stream

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation
import com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.INCLUDE_WEEKENDS
import com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS

/**
 * Builder to create the immutable `LocalDateDoubleTimeSeries`.
 *
 * This builder allows a time-series to be created.
 * Entries can be added to the builder in any order.
 * If a date is duplicated it will overwrite an earlier entry.
 *
 * Use `LocalDateDoubleTimeSeries#builder()` to create an instance.
 */
final class LocalDateDoubleTimeSeriesBuilder private[timeseries] () {

  /**
   * The entries for the time-series.
   */
  private val entries: SortedMap[LocalDate, java.lang.Double] = new TreeMap[LocalDate, java.lang.Double]()

  /**
   * Keep track of whether we have weekends in the data.
   */
  private var containsWeekends: Boolean = false

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   *
   * Use `LocalDateDoubleTimeSeries#toBuilder()`.
   *
   * @param dates  the dates to initialize with
   * @param values  the values to initialize with
   */
  private[timeseries] def this(dates: Array[LocalDate], values: Array[Double]) = {
    this()
    var i = 0
    while (i < dates.length) {
      put(dates(i), values(i))
      i += 1
    }
  }

  /**
   * Creates an instance.
   *
   * Use `DenseLocalDateDoubleTimeSeries#toBuilder()`.
   *
   * @param points  the stream of points to initialize with
   */
  private[timeseries] def this(points: Stream[LocalDateDoublePoint]) = {
    this()
    points.forEach(pt => put(pt.getDate(), pt.getValue()))
  }

  //-------------------------------------------------------------------------
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
  def get(date: LocalDate): OptionalDouble = {
    val value = entries.get(date)
    if (value != null) OptionalDouble.of(value.doubleValue()) else OptionalDouble.empty()
  }

  //-------------------------------------------------------------------------
  /**
   * Puts the specified date/value point into this builder.
   *
   * @param date  the date to be added
   * @param value  the value associated with the date
   * @return this builder
   */
  def put(date: LocalDate, value: Double): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(date, "date")
    ArgChecker.isFalse(java.lang.Double.isNaN(value), "NaN is not allowed as a value")
    entries.put(date, java.lang.Double.valueOf(value))
    updateWeekendFlag(date)
    this
  }

  /**
   * Puts the specified date/value point into this builder.
   *
   * @param point  the point to be added
   * @return this builder
   */
  def put(point: LocalDateDoublePoint): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(point, "point")
    put(point.getDate(), point.getValue())
    this
  }

  //-------------------------------------------------------------------------
  /**
   * Merges the specified date/value point into this builder.
   *
   * The operator is invoked if the date already exists.
   *
   * @param date  the date to be added
   * @param value  the value associated with the date
   * @param operator  the operator to use for merging
   * @return this builder
   */
  def merge(date: LocalDate, value: Double, operator: DoubleBinaryOperator): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(date, "date")
    ArgChecker.notNull(operator, "operator")
    val remapping: BiFunction[java.lang.Double, java.lang.Double, java.lang.Double] =
      (a, b) => java.lang.Double.valueOf(operator.applyAsDouble(a.doubleValue(), b.doubleValue()))
    entries.merge(date, java.lang.Double.valueOf(value), remapping)
    updateWeekendFlag(date)
    this
  }

  /**
   * Merges the specified date/value point into this builder.
   *
   * The operator is invoked if the date already exists.
   *
   * @param point  the point to be added
   * @param operator  the operator to use for merging
   * @return this builder
   */
  def merge(point: LocalDateDoublePoint, operator: DoubleBinaryOperator): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(point, "point")
    merge(point.getDate(), point.getValue(), operator)
  }

  // updates the weekend flag, if not updated exceptions will occur during build()
  private def updateWeekendFlag(date: LocalDate): Unit = {
    if (!containsWeekends && date.get(ChronoField.DAY_OF_WEEK) > 5) {
      containsWeekends = true
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Puts all the specified dates and values into this builder.
   *
   * The date and value collections must be the same size.
   *
   * The date-value pairs are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param dates  the dates to be added
   * @param values  the values to be added
   * @return this builder
   */
  def putAll(dates: Collection[LocalDate], values: Collection[java.lang.Double]): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.noNulls(dates, "dates")
    ArgChecker.noNulls(values, "values")
    ArgChecker.isTrue(dates.size() == values.size(),
        "Arrays are of different sizes - dates: {}, values: {}",
        Integer.valueOf(dates.size()), Integer.valueOf(values.size()))
    val itDate: Iterator[LocalDate] = dates.iterator()
    val itValue: Iterator[java.lang.Double] = values.iterator()
    var i = 0
    while (i < dates.size()) {
      put(itDate.next(), itValue.next().doubleValue())
      i += 1
    }
    this
  }

  /**
   * Puts all the specified dates and values into this builder.
   *
   * The date collection and value array must be the same size.
   *
   * The date-value pairs are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param dates  the dates to be added
   * @param values  the values to be added
   * @return this builder
   */
  def putAll(dates: Collection[LocalDate], values: Array[Double]): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.noNulls(dates, "dates")
    ArgChecker.notNull(values, "values")
    ArgChecker.isTrue(dates.size() == values.length,
        "Arrays are of different sizes - dates: {}, values: {}",
        Integer.valueOf(dates.size()), Integer.valueOf(values.length))
    val itDate: Iterator[LocalDate] = dates.iterator()
    var i = 0
    while (i < dates.size()) {
      put(itDate.next(), values(i))
      i += 1
    }
    this
  }

  /**
   * Puts all the specified points into this builder.
   *
   * The points are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param points  the points to be added
   * @return this builder
   */
  def putAll(points: Stream[LocalDateDoublePoint]): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(points, "points")
    points.forEach(pt => put(pt))
    this
  }

  /**
   * Puts all the specified points into this builder.
   *
   * The points are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param points  the points to be added
   * @return this builder
   */
  def putAll(points: List[LocalDateDoublePoint]): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(points, "points")
    putAll(points.stream())
  }

  /**
   * Puts the contents of the specified builder into this builder.
   *
   * The points are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param other  the other builder
   * @return this builder
   */
  def putAll(other: LocalDateDoubleTimeSeriesBuilder): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.notNull(other, "other")
    entries.putAll(other.entries)
    containsWeekends = containsWeekends || other.containsWeekends
    this
  }

  /**
   * Puts all the entries from the supplied map into this builder.
   *
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param map  the map of points to be added
   * @return this builder
   */
  def putAll(map: Map[LocalDate, java.lang.Double]): LocalDateDoubleTimeSeriesBuilder = {
    ArgChecker.noNulls(map, "map")
    map.entrySet().forEach(e => put(e.getKey(), e.getValue().doubleValue()))
    this
  }

  //-------------------------------------------------------------------------
  /**
   * Build the time-series from the builder.
   *
   * @return a time-series containing the entries from the builder
   */
  def build(): LocalDateDoubleTimeSeries = {
    if (entries.isEmpty()) {
      return LocalDateDoubleTimeSeries.empty()
    }

    // Depending on how dense the data is, judge which type of time series
    // is the best fit
    if (density() > LocalDateDoubleTimeSeriesBuilder.DENSITY_THRESHOLD) {
      createDenseSeries()
    } else {
      createSparseSeries()
    }
  }

  private def createDenseSeries(): LocalDateDoubleTimeSeries = {
    DenseLocalDateDoubleTimeSeries.of(
        entries.firstKey(),
        entries.lastKey(),
        streamEntries(),
        determineCalculation())
  }

  private def createSparseSeries(): SparseLocalDateDoubleTimeSeries = {
    SparseLocalDateDoubleTimeSeries.of(entries.keySet(), entries.values())
  }

  private def streamEntries(): Stream[LocalDateDoublePoint] = {
    entries.entrySet()
        .stream()
        .map[LocalDateDoublePoint](e => LocalDateDoublePoint.of(e.getKey(), e.getValue().doubleValue()))
  }

  private def determineCalculation(): DenseTimeSeriesCalculation = {
    if (containsWeekends) INCLUDE_WEEKENDS else SKIP_WEEKENDS
  }

  private def density(): Double = {
    // We can use the calculators to work out range size
    val rangeSize: Double = determineCalculation().calculatePosition(entries.firstKey(), entries.lastKey()) + 1
    entries.size() / rangeSize
  }

}

object LocalDateDoubleTimeSeriesBuilder {

  /**
   * Threshold for deciding whether we use the dense or sparse time-series implementation.
   */
  private final val DENSITY_THRESHOLD = 0.7

}
