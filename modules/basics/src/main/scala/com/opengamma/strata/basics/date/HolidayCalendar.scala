/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.stream.Stream

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.Named

/**
 * A holiday calendar, classifying dates as holidays or business days.
 *
 * Many calculations in finance require knowledge of whether a date is a business day or not.
 * This class encapsulates that knowledge, with each day treated as a holiday or a business day.
 * Weekends are effectively treated as a special kind of holiday.
 *
 * Applications should refer to holidays using `HolidayCalendarId`.
 * The identifier must be resolved
 * to a `HolidayCalendar` before the holiday data methods can be accessed.
 * See `HolidayCalendarIds` for a standard set of identifiers available in `ReferenceData#standard()`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 *
 * @see ImmutableHolidayCalendar
 */
trait HolidayCalendar
    extends Named {

  /**
   * Checks if the specified date is a holiday.
   *
   * This is the opposite of `isBusinessDay(LocalDate)`.
   * A weekend is treated as a holiday.
   *
   * @param date  the date to check
   * @return true if the specified date is a holiday
   * @throws java.lang.IllegalArgumentException if the date is outside the supported range
   */
  def isHoliday(date: LocalDate): Boolean

  /**
   * Checks if the specified date is a business day.
   *
   * This is the opposite of `isHoliday(LocalDate)`.
   * A weekend is treated as a holiday.
   *
   * @param date  the date to check
   * @return true if the specified date is a business day
   * @throws java.lang.IllegalArgumentException if the date is outside the supported range
   */
  def isBusinessDay(date: LocalDate): Boolean = {
    !isHoliday(date)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an adjuster that changes the date.
   *
   * The adjuster is intended to be used with the method `Temporal#with(TemporalAdjuster)`.
   * For example:
   * {{{
   * threeDaysLater = date.with(businessDays.adjustBy(3));
   * twoDaysEarlier = date.with(businessDays.adjustBy(-2));
   * }}}
   *
   * @param amount  the number of business days to adjust by
   * @return the first business day after this one
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def adjustBy(amount: Int): TemporalAdjuster = {
    TemporalAdjusters.ofDateAdjuster(date => shift(date, amount))
  }

  //-------------------------------------------------------------------------
  /**
   * Shifts the date by the specified number of business days.
   *
   * If the amount is zero, the input date is returned.
   * If the amount is positive, later business days are chosen.
   * If the amount is negative, earlier business days are chosen.
   *
   * @param date  the date to adjust
   * @param amount  the number of business days to adjust by
   * @return the shifted date
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def shift(date: LocalDate, amount: Int): LocalDate = {
    var adjusted = date
    if (amount > 0) {
      var i = 0
      while (i < amount) {
        adjusted = next(adjusted)
        i += 1
      }
    } else if (amount < 0) {
      var i = 0
      while (i > amount) {
        adjusted = previous(adjusted)
        i -= 1
      }
    }
    adjusted
  }

  /**
   * Finds the next business day, always returning a later date.
   *
   * Given a date, this method returns the next business day.
   *
   * @param date  the date to adjust
   * @return the first business day after the input date
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def next(date: LocalDate): LocalDate = {
    val next = LocalDateUtils.plusDays(date, 1)
    if (isHoliday(next)) this.next(next) else next
  }

  /**
   * Finds the next business day, returning the input date if it is a business day.
   *
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * Otherwise, the next business day is returned.
   *
   * @param date  the date to adjust
   * @return the input date if it is a business day, or the next business day
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def nextOrSame(date: LocalDate): LocalDate = {
    if (isHoliday(date)) next(date) else date
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the previous business day, always returning an earlier date.
   *
   * Given a date, this method returns the previous business day.
   *
   * @param date  the date to adjust
   * @return the first business day before the input date
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def previous(date: LocalDate): LocalDate = {
    val previous = LocalDateUtils.plusDays(date, -1)
    if (isHoliday(previous)) this.previous(previous) else previous
  }

  /**
   * Finds the previous business day, returning the input date if it is a business day.
   *
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * Otherwise, the previous business day is returned.
   *
   * @param date  the date to adjust
   * @return the input date if it is a business day, or the previous business day
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def previousOrSame(date: LocalDate): LocalDate = {
    if (isHoliday(date)) previous(date) else date
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next business day within the month, returning the input date if it is a business day,
   * or the last business day of the month if the next business day is in a different month.
   *
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * If the next business day is within the same month, it is returned.
   * Otherwise, the last business day of the month is returned.
   *
   * Note that the result of this method may be earlier than the input date.
   *
   * This corresponds to the modified following
   * business day convention.
   *
   * @param date  the date to adjust
   * @return the input date if it is a business day, the next business day if within the same month
   *   or the last business day of the month
   * @throws java.lang.IllegalArgumentException if the calculation is outside the supported range
   */
  def nextSameOrLastInMonth(date: LocalDate): LocalDate = {
    val nextOrSame = this.nextOrSame(date)
    (if (nextOrSame.getMonthValue() != date.getMonthValue()) previous(date) else nextOrSame)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is the last business day of the month.
   *
   * This returns true if the date specified is the last valid business day of the month.
   *
   * @param date  the date to check
   * @return true if the specified date is the last business day of the month
   * @throws java.lang.IllegalArgumentException if the date is outside the supported range
   */
  def isLastBusinessDayOfMonth(date: LocalDate): Boolean = {
    isBusinessDay(date) && next(date).getMonthValue() != date.getMonthValue()
  }

  /**
   * Calculates the last business day of the month.
   *
   * Given a date, this method returns the date of the last business day of the month.
   *
   * @param date  the date to check
   * @return the date of the last business day of the month
   * @throws java.lang.IllegalArgumentException if the date is outside the supported range
   */
  def lastBusinessDayOfMonth(date: LocalDate): LocalDate = {
    previousOrSame(date.withDayOfMonth(date.lengthOfMonth()))
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number of business days between two dates.
   *
   * This calculates the number of business days within the range.
   * If the dates are equal, zero is returned.
   * If the end is before the start, an exception is thrown.
   *
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the total number of business days between the start and end date
   * @throws java.lang.IllegalArgumentException if either date is outside the supported range
   */
  def daysBetween(startInclusive: LocalDate, endExclusive: LocalDate): Int = {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive")
    Math.toIntExact(LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(d => isBusinessDay(d))
        .count())
  }

  /**
   * Gets the stream of business days between the two dates.
   *
   * This method will treat weekends as holidays.
   * If the dates are equal, an empty stream is returned.
   * If the end is before the start, an exception is thrown.
   *
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the stream of business days
   * @throws java.lang.IllegalArgumentException if either date is outside the supported range
   */
  def businessDays(startInclusive: LocalDate, endExclusive: LocalDate): Stream[LocalDate] = {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive")
    LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(d => isBusinessDay(d))
  }

  /**
   * Gets the stream of holidays between the two dates.
   *
   * This method will treat weekends as holidays.
   * If the dates are equal, an empty stream is returned.
   * If the end is before the start, an exception is thrown.
   *
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the stream of holidays
   * @throws java.lang.IllegalArgumentException if either date is outside the supported range
   */
  def holidays(startInclusive: LocalDate, endExclusive: LocalDate): Stream[LocalDate] = {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive")
    LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(d => isHoliday(d))
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this holiday calendar with another.
   *
   * The resulting calendar will declare a day as a business day if it is a
   * business day in both source calendars.
   *
   * @param other  the other holiday calendar
   * @return the combined calendar
   * @throws java.lang.IllegalArgumentException if unable to combine the calendars
   */
  def combinedWith(other: HolidayCalendar): HolidayCalendar = {
    if (this.equals(other)) {
      this
    } else if (other eq HolidayCalendars.NO_HOLIDAYS) {
      this
    } else {
      new CombinedHolidayCalendar(this, other)
    }
  }

  /**
   * Combines this holiday calendar with another.
   *
   * The resulting calendar will declare a day as a business day if it is a
   * business day in either source calendar.
   *
   * @param other  the other holiday calendar
   * @return the combined calendar
   */
  def linkedWith(other: HolidayCalendar): HolidayCalendar = {
    if (this.equals(other)) {
      this
    } else if ((this eq HolidayCalendars.NO_HOLIDAYS) || (other eq HolidayCalendars.NO_HOLIDAYS)) {
      HolidayCalendars.NO_HOLIDAYS
    } else {
      new LinkedHolidayCalendar(this, other)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the identifier for the calendar.
   *
   * This identifier is used to locate the index in `ReferenceData`.
   *
   * @return the identifier
   */
  def getId(): HolidayCalendarId

  //-------------------------------------------------------------------------
  /**
   * Gets the name that identifies this calendar.
   *
   * This is the name associated with the identifier.
   *
   * @return the name
   */
  override def getName(): String = {
    getId().getName()
  }

}
