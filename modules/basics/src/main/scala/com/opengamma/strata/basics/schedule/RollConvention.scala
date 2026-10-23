/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.time.DayOfWeek
import java.time.LocalDate

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.schedule.DayRollConventions.Dom
import com.opengamma.strata.basics.schedule.DayRollConventions.Dow
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A convention defining how to roll dates.
 *
 * A periodic schedule is determined using a periodic frequency.
 * When applying the frequency, the roll convention is used to fine tune the dates.
 * This might involve selecting the last day of the month, or the third Wednesday.
 *
 * To get the next date in the schedule, take the base date and the
 * periodic frequency. Once this date is calculated,
 * the roll convention is applied to produce the next schedule date.
 *
 * The most common implementations are provided as constants on `RollConventions`.
 * Additional implementations may be added by implementing this interface.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait RollConvention extends Named {

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date according to the rules of the roll convention.
   *
   * See the description of each roll convention to understand the rule applied.
   *
   * It is recommended to use `next()` and `previous()` rather than
   * directly using this method.
   * 
   * @param date  the date to adjust
   * @return the adjusted temporal
   */
  def adjust(date: LocalDate): LocalDate

  /**
   * Checks if the date matches the rules of the roll convention.
   *
   * See the description of each roll convention to understand the rule applied.
   * 
   * @param date  the date to check
   * @return true if the date matches this convention
   */
  def matches(date: LocalDate): Boolean = {
    ArgChecker.notNull(date, "date")
    date.equals(adjust(date))
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the next date in the sequence after the input date.
   *
   * This takes the input date, adds the periodic frequency and adjusts the date
   * as necessary to match the roll convention rules.
   * The result will always be after the input date.
   *
   * The default implementation is suitable for month-based conventions.
   * 
   * @param date  the date to adjust
   * @param periodicFrequency  the periodic frequency of the schedule
   * @return the adjusted date
   */
  def next(date: LocalDate, periodicFrequency: Frequency): LocalDate = {
    ArgChecker.notNull(date, "date")
    ArgChecker.notNull(periodicFrequency, "periodicFrequency")
    var calculated = adjust(date.plus(periodicFrequency))
    if (calculated.isAfter(date) == false) {
      calculated = adjust(date.plusMonths(1))
    }
    calculated
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the previous date in the sequence after the input date.
   *
   * This takes the input date, subtracts the periodic frequency and adjusts the date
   * as necessary to match the roll convention rules.
   * The result will always be before the input date.
   *
   * The default implementation is suitable for month-based conventions.
   * 
   * @param date  the date to adjust
   * @param periodicFrequency  the periodic frequency of the schedule
   * @return the adjusted date
   */
  def previous(date: LocalDate, periodicFrequency: Frequency): LocalDate = {
    ArgChecker.notNull(date, "date")
    ArgChecker.notNull(periodicFrequency, "periodicFrequency")
    var calculated = adjust(date.minus(periodicFrequency))
    if (calculated.isBefore(date) == false) {
      calculated = adjust(date.minusMonths(1))
    }
    calculated
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day-of-month that the roll convention implies.
   *
   * This extracts the day-of-month for simple roll conventions.
   * The numeric roll conventions will return their day-of-month.
   * The 'EOM' convention will return 31.
   * All other conventions will return zero.
   * 
   * @return the day-of-month that the roll convention implies, zero if not applicable
   */
  def getDayOfMonth(): Int = {
    0
  }

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

object RollConvention {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the roll convention
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): RollConvention = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Obtains an instance from the day-of-month.
   *
   * This convention will adjust the input date to the specified day-of-month.
   * The year and month of the result date will be the same as the input date.
   * It is intended for use with periods that are a multiple of months.
   *
   * If the month being adjusted has a length less than the requested day-of-month
   * then the last valid day-of-month will be chosen. As such, passing 31 to this
   * method is equivalent to selecting the end-of-month convention.
   * 
   * @param dayOfMonth  the day-of-month, from 1 to 31
   * @return the roll convention
   * @throws java.lang.IllegalArgumentException if the day-of-month is invalid
   */
  @static def ofDayOfMonth(dayOfMonth: Int): RollConvention = {
    Dom.of(dayOfMonth)
  }

  /**
   * Obtains an instance from the day-of-week.
   *
   * This convention will adjust the input date to the specified day-of-week.
   * It is intended for use with periods that are a multiple of weeks.
   *
   * In `adjust()`, if the input date is not the required day-of-week,
   * then the next occurrence of the day-of-week is selected, up to 6 days later.
   *
   * In `next()`, the day-of-week is selected after the frequency is added.
   * If the calculated date is not the required day-of-week, then the next occurrence
   * of the day-of-week is selected, up to 6 days later.
   *
   * In `previous()`, the day-of-week is selected after the frequency is subtracted.
   * If the calculated date is not the required day-of-week, then the previous occurrence
   * of the day-of-week is selected, up to 6 days earlier.
   * 
   * @param dayOfWeek  the day-of-week
   * @return the roll convention
   */
  @static def ofDayOfWeek(dayOfWeek: DayOfWeek): RollConvention = {
    Dow.of(dayOfWeek)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[RollConvention] = {
    RollConventions.ENUM_LOOKUP
  }

}
