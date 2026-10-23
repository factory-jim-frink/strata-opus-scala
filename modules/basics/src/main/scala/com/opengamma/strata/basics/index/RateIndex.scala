/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.collect.ArgChecker

/**
 * A index of interest rates, such as an Overnight or Inter-Bank rate.
 *
 * Many financial products require knowledge of interest rate indices, such as Libor.
 * Implementations of this interface define these indices.
 * See `IborIndex` and `OvernightIndex`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait RateIndex
    extends FloatingRateIndex {

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  def getName(): String

  /**
   * Gets the calendar that determines which dates are fixing dates.
   *
   * The rate will be fixed on each business day in this calendar.
   *
   * @return the calendar used to determine the fixing dates of the index
   */
  def getFixingCalendar(): HolidayCalendarId

  /**
   * Gets the tenor of the index.
   *
   * @return the tenor
   */
  def getTenor(): Tenor

}

object RateIndex {

  /**
   * Obtains an instance from the specified unique name.
   *
   * This parses names from `IborIndex` and `OvernightIndex`.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): RateIndex = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    Indices.RATE_INDEX_LOOKUP.lookup(uniqueName)
  }

}
