/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.Period

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A convention defining how a period is added to a date.
 *
 * The purpose of this convention is to define how to handle the addition of a period.
 * The default implementations include two different end-of-month rules.
 * The convention is generally only applicable for month-based periods.
 *
 * The most common implementations are provided in `PeriodAdditionConventions`.
 * Additional implementations may be added by implementing this interface.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait PeriodAdditionConvention extends Named {

  //-------------------------------------------------------------------------
  /**
   * Adjusts the base date, adding the period and applying the convention rule.
   *
   * The adjustment occurs in two steps.
   * First, the period is added to the based date to create the end date.
   * Second, the end date is adjusted by the convention rules.
   *
   * @param baseDate  the base date to add to
   * @param period  the period to add
   * @param calendar  the holiday calendar to use
   * @return the adjusted date
   */
  def adjust(baseDate: LocalDate, period: Period, calendar: HolidayCalendar): LocalDate

  /**
   * Checks whether the convention requires a month-based period.
   *
   * A month-based period contains only months and/or years, and not days.
   *
   * @return true if the convention requires a month-based period
   */
  def isMonthBased(): Boolean

  /**
   * Gets the name that uniquely identifies this convention.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  override def getName(): String

}

object PeriodAdditionConvention {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the addition convention
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): PeriodAdditionConvention = {
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[PeriodAdditionConvention] = {
    PeriodAdditionConventions.enumLookup()
  }

}
