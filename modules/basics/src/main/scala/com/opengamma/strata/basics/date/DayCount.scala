/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.schedule.Frequency
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A convention defining how to calculate fractions of a year.
 *
 * The purpose of this convention is to define how to convert dates into numeric year fractions.
 * The is of use when calculating accrued interest over time.
 *
 * The most common implementations are provided in `DayCounts`.
 * Additional implementations may be added by implementing this interface.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait DayCount extends Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the year fraction between the specified dates.
   *
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention. The dates must be in order.
   *
   * This uses a simple `ScheduleInfo` which has the end-of-month convention
   * set to true, but throws an exception for other methods.
   * Certain implementations of `DayCount` need the missing information,
   * and thus will throw an exception.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date, on or after the first date
   * @return the year fraction
   * @throws java.lang.IllegalArgumentException if the dates are not in order
   * @throws java.lang.UnsupportedOperationException if the year fraction cannot be obtained
   */
  def yearFraction(firstDate: LocalDate, secondDate: LocalDate): Double = {
    yearFraction(firstDate, secondDate, DayCounts.simpleScheduleInfo())
  }

  /**
   * Gets the year fraction between the specified dates.
   *
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention. The dates must be in order.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date, on or after the first date
   * @param scheduleInfo  the schedule information
   * @return the year fraction, zero or greater
   * @throws java.lang.IllegalArgumentException if the dates are not in order
   * @throws java.lang.UnsupportedOperationException if the year fraction cannot be obtained
   */
  def yearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: DayCount.ScheduleInfo): Double

  /**
   * Gets the relative year fraction between the specified dates.
   *
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * The result of this method will be negative if the first date is after the second date.
   * The result is calculated using `yearFraction(LocalDate, LocalDate, ScheduleInfo)`.
   *
   * This uses a simple `ScheduleInfo` which has the end-of-month convention
   * set to true, but throws an exception for other methods.
   * Certain implementations of `DayCount` need the missing information,
   * and thus will throw an exception.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date, which may be before the first date
   * @return the year fraction, may be negative
   * @throws java.lang.UnsupportedOperationException if the year fraction cannot be obtained
   */
  def relativeYearFraction(firstDate: LocalDate, secondDate: LocalDate): Double = {
    relativeYearFraction(firstDate, secondDate, DayCounts.simpleScheduleInfo())
  }

  /**
   * Gets the relative year fraction between the specified dates.
   *
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * The result of this method will be negative if the first date is after the second date.
   * The result is calculated using `yearFraction(LocalDate, LocalDate, ScheduleInfo)`.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date, which may be before the first date
   * @param scheduleInfo  the schedule information
   * @return the year fraction, may be negative
   * @throws java.lang.UnsupportedOperationException if the year fraction cannot be obtained
   */
  def relativeYearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: DayCount.ScheduleInfo): Double = {
    if (secondDate.isBefore(firstDate)) {
      -yearFraction(secondDate, firstDate, scheduleInfo)
    } else {
      yearFraction(firstDate, secondDate, scheduleInfo)
    }
  }

  /**
   * Calculates the number of days between the specified dates using the rules of this day count.
   *
   * A day count is typically defines as a count of days divided by a year estimate.
   * This method returns the count of days, which is the numerator of the division.
   * For example, the 'Act/Act' day count will return the actual number of days between
   * the two dates, but the '30/360 ISDA' will return a value based on 30 day months.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date
   * @return the number of days, as determined by the day count
   */
  def days(firstDate: LocalDate, secondDate: LocalDate): Int

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

object DayCount {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the day count
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): DayCount = {
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Obtains an instance of the 'Bus/252' day count based on a specific calendar.
   *
   * The 'Bus/252' day count is unusual in that it relies on a specific holiday calendar.
   * The calendar is stored within the day count.
   *
   * To avoid widespread complexity in the system, the holiday calendar associated
   * with 'Bus/252' holiday calendars is looked up using the
   * standard reference data.
   *
   * This day count is typically used in Brazil.
   *
   * @param calendar  the holiday calendar
   * @return the day count
   */
  @static def ofBus252(calendar: HolidayCalendarId): DayCount = {
    Business252DayCount.INSTANCE.of(calendar.resolve(ReferenceData.standard()))
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the day count to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[DayCount] = {
    DayCounts.enumLookup()
  }

  //-------------------------------------------------------------------------
  /**
   * Information about the schedule necessary to calculate the day count.
   *
   * Some `DayCount` implementations require additional information about the schedule.
   * Implementations of this interface provide that information.
   */
  trait ScheduleInfo {

    /**
     * Gets the start date of the schedule.
     *
     * The first date in the schedule.
     * If the schedule adjusts for business days, then this is the adjusted date.
     *
     * This throws an exception by default.
     *
     * @return the schedule start date
     * @throws java.lang.UnsupportedOperationException if the date cannot be obtained
     */
    def getStartDate(): LocalDate = {
      throw new UnsupportedOperationException("The start date of the schedule is required")
    }

    /**
     * Gets the end date of the schedule.
     *
     * The last date in the schedule.
     * If the schedule adjusts for business days, then this is the adjusted date.
     *
     * This throws an exception by default.
     *
     * @return the schedule end date
     * @throws java.lang.UnsupportedOperationException if the date cannot be obtained
     */
    def getEndDate(): LocalDate = {
      throw new UnsupportedOperationException("The end date of the schedule is required")
    }

    /**
     * Gets the end date of the schedule period.
     *
     * This is called when a day count requires the end date of the schedule period.
     *
     * This throws an exception by default.
     *
     * @param date  the date to find the period end date for
     * @return the period end date
     * @throws java.lang.UnsupportedOperationException if the date cannot be obtained
     */
    def getPeriodEndDate(date: LocalDate): LocalDate = {
      throw new UnsupportedOperationException("The end date of the schedule period is required")
    }

    /**
     * Gets the periodic frequency of the schedule period.
     *
     * This is called when a day count requires the periodic frequency of the schedule.
     *
     * This throws an exception by default.
     *
     * @return the periodic frequency
     * @throws java.lang.UnsupportedOperationException if the frequency cannot be obtained
     */
    def getFrequency(): Frequency = {
      throw new UnsupportedOperationException("The frequency of the schedule is required")
    }

    /**
     * Checks if the end of month convention is in use.
     *
     * This is called when a day count needs to know whether the end-of-month convention is in use.
     *
     * This is true by default.
     *
     * @return true if the end of month convention is in use
     */
    def isEndOfMonthConvention(): Boolean = {
      true
    }
  }

}
