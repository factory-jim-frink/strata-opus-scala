/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries

import java.time.LocalDate

import org.joda.beans.JodaBeanUtils

import com.google.common.collect.ComparisonChain
import com.opengamma.strata.collect.ArgChecker

/**
 * Immutable representation of a single point in a `LocalDateDoubleTimeSeries`.
 *
 * This implementation uses arrays internally.
 */
final class LocalDateDoublePoint private (rawDate: LocalDate, rawValue: Double)
  extends Comparable[LocalDateDoublePoint] {

  /**
   * The date.
   */
  private val date: LocalDate = ArgChecker.notNull(rawDate, "date")
  /**
   * The value.
   */
  private val value: Double = rawValue

  //-------------------------------------------------------------------------
  /**
   * Gets the date.
   *
   * @return the date
   */
  def getDate(): LocalDate = {
    date
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  def getValue(): Double = {
    value
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this point with another date.
   *
   * @param date  the date to change the point to
   * @return a point based on this point with the date changed
   */
  def withDate(date: LocalDate): LocalDateDoublePoint = {
    LocalDateDoublePoint.of(date, value)
  }

  /**
   * Returns a copy of this point with another value.
   *
   * @param value  the value to change the point to
   * @return a point based on this point with the value changed
   */
  def withValue(value: Double): LocalDateDoublePoint = {
    LocalDateDoublePoint.of(date, value)
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this point to another.
   *
   * The sort order is by date, then by double.
   * This is compatible with equals.
   *
   * @param other  the other point
   * @return negative if this is less than, zero if equal, positive if greater than
   */
  override def compareTo(other: LocalDateDoublePoint): Int = {
    ComparisonChain.start()
        .compare(date, other.date)
        .compare(value, other.value)
        .result()
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this point is equal to another point.
   *
   * @param obj  the object to check, null returns false
   * @return true if this is equal to the other point
   */
  override def equals(obj: Any): Boolean = {
    if (this eq obj.asInstanceOf[AnyRef]) {
      return true
    }
    obj match {
      case other: LocalDateDoublePoint =>
        date.equals(other.date) && JodaBeanUtils.equal(value, other.value)
      case _ =>
        false
    }
  }

  /**
   * A hash code for this point.
   *
   * @return a suitable hash code
   */
  override def hashCode(): Int = {
    date.hashCode() ^ JodaBeanUtils.hashCode(value)
  }

  /**
   * Returns a string representation of the point.
   *
   * @return the string
   */
  override def toString(): String = {
    new java.lang.StringBuilder(24)
        .append('(')
        .append(date)
        .append('=')
        .append(value)
        .append(')')
        .toString()
  }

}

object LocalDateDoublePoint {

  //-------------------------------------------------------------------------
  /**
   * Obtains a point from date and value.
   *
   * @param date  the date
   * @param value  the value
   * @return the point
   */
  def of(date: LocalDate, value: Double): LocalDateDoublePoint = {
    new LocalDateDoublePoint(date, value)
  }

}
