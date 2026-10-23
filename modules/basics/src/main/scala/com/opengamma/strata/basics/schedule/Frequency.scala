/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.io.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.MONTHS
import java.time.temporal.ChronoUnit.YEARS
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.List
import java.util.Locale

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * A periodic frequency used by financial products that have a specific event every so often.
 *
 * Frequency is primarily intended to be used to subdivide events within a year.
 *
 * A frequency is allowed to be any non-negative period of days, weeks, month or years.
 * This class provides constants for common frequencies which are best used by static import.
 *
 * A special value, 'Term', is provided for when there are no subdivisions of the entire term.
 * This is also know as 'zero-coupon' or 'once'. It is represented using the period 10,000 years,
 * which allows addition/subtraction to work, producing a date after the end of the term.
 *
 * Each frequency is based on a `Period`. The months and years of the period are not normalized,
 * thus it is possible to have a frequency of 12 months and a different one of 1 year.
 * When used, standard date addition rules apply, thus there is no difference between them.
 * Call `normalized()` to apply normalization.
 *
 * The periodic frequency is often expressed as a number of events per year.
 * The `eventsPerYear()` method can be used to obtain this for common frequencies.
 * 
 * <h4>Usage</h4>
 * `Frequency` implements `TemporalAmount` allowing it to be directly added to a date:
 * {{{
 *  LocalDate later = baseDate.plus(frequency);
 * }}}
 */
@SerialVersionUID(1L)
final class Frequency private (
    /**
     * The period of the frequency.
     */
    private val period: Period,
    /**
     * The name of the frequency.
     */
    private val name: String)
  extends TemporalAmount with Serializable {

  ArgChecker.notNull(period, "period")
  ArgChecker.isFalse(period.isZero(), "Frequency period must not be zero")
  ArgChecker.isFalse(period.isNegative(), "Frequency period must not be negative")

  // the Java field names cannot be reused as they match the accessor methods; both fields are transient
  /**
   * The number of events per year.
   */
  @transient private val eventsPerYearValue: Int = Frequency.calculateEventsPerYear(period)
  /**
   * The number of events per year.
   */
  @transient private val eventsPerYearEstimateValue: Double = Frequency.calculateEventsPerYearEstimate(period)

  /**
   * Creates a periodic frequency.
   *
   * @param period  the period to represent
   */
  private def this(period: Period) = {
    this(period, period.toString())
  }

  // safe deserialization
  private def readResolve(): Object = {
    if (this.equals(Frequency.TERM)) {
      return Frequency.TERM
    }
    Frequency.of(period)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying period of the frequency.
   *
   * @return the period
   */
  def getPeriod(): Period = {
    period
  }

  /**
   * Checks if the periodic frequency is the 'Term' instance.
   *
   * The term instance corresponds to there being no subdivisions of the entire term.
   *
   * @return true if this is the 'Term' instance
   */
  def isTerm(): Boolean = {
    this eq Frequency.TERM
  }

  //-------------------------------------------------------------------------
  /**
   * Normalizes the months and years of this tenor.
   *
   * This method returns a tenor of an equivalent length but with any number
   * of months greater than 12 normalized into a combination of months and years.
   *
   * @return the normalized tenor
   */
  def normalized(): Frequency = {
    val norm = period.normalized()
    if (norm ne period) Frequency.of(norm) else this
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the periodic frequency is week-based.
   *
   * A week-based frequency consists of an integral number of weeks.
   * There must be no day, month or year element.
   *
   * @return true if this is week-based
   */
  def isWeekBased(): Boolean = {
    period.toTotalMonths() == 0 && period.getDays() % 7 == 0
  }

  /**
   * Checks if the periodic frequency is month-based.
   *
   * A month-based frequency consists of an integral number of months.
   * Any year-based frequency is also counted as month-based.
   * There must be no day or week element.
   *
   * @return true if this is month-based
   */
  def isMonthBased(): Boolean = {
    period.toTotalMonths() > 0 && period.getDays() == 0 && isTerm() == false
  }

  /**
   * Checks if the periodic frequency is annual.
   *
   * An annual frequency consists of 12 months.
   * There must be no day or week element.
   *
   * @return true if this is annual
   */
  def isAnnual(): Boolean = {
    period.toTotalMonths() == 12 && period.getDays() == 0
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number of events that occur in a year.
   *
   * The number of events per year is the number of times that the period occurs per year.
   * Not all periodic frequency instances can be converted to an integer events per year.
   * All constants declared on this class will return a result.
   *
   * Month-based and year-based periodic frequencies are converted by dividing 12 by the number of months.
   * Only the following periodic frequencies return a value - P1M, P2M, P3M, P4M, P6M, P1Y.
   *
   * Day-based and week-based periodic frequencies are converted by dividing 364 by the number of days.
   * Only the following periodic frequencies return a value - P1D, P2D, P4D, P1W, P2W, P4W, P13W, P26W, P52W.
   *
   * The 'Term' periodic frequency returns zero.
   *
   * @return the number of events per year
   * @throws java.lang.IllegalArgumentException if unable to calculate the number of events per year
   */
  def eventsPerYear(): Int = {
    if (eventsPerYearValue == -1) {
      throw new IllegalArgumentException("Unable to calculate events per year: " + this)
    }
    eventsPerYearValue
  }

  /**
   * Estimates the number of events that occur in a year.
   *
   * The number of events per year is the number of times that the period occurs per year.
   * This method returns an estimate without throwing an exception.
   * The exact number of events is returned by `eventsPerYear()`.
   *
   * The 'Term' periodic frequency returns zero.
   * Month-based and year-based periodic frequencies return 12 divided by the number of months.
   * Day-based and week-based periodic frequencies return 364 divided by the number of days.
   * Other frequencies are calculated using estimated durations, dividing the year by the period.
   *
   * @return the estimated number of events per year
   */
  def eventsPerYearEstimate(): Double = {
    eventsPerYearEstimateValue
  }

  //-------------------------------------------------------------------------
  /**
   * Exactly divides this frequency by another.
   *
   * This calculates the integer division of this frequency by the specified frequency.
   * If the result is not an integer, an exception is thrown.
   *
   * Month-based and year-based periodic frequencies are calculated by dividing the total number of months.
   * For example, P6M divided by P3M results in 2, and P2Y divided by P6M returns 4.
   *
   * Day-based and week-based periodic frequencies are calculated by dividing the total number of days.
   * For example, P26W divided by P13W results in 2, and P2W divided by P1D returns 14.
   *
   * The 'Term' frequency throws an exception.
   *
   * @param other  the other frequency to divide into this one
   * @return this frequency divided by the other frequency
   * @throws java.lang.IllegalArgumentException if the frequency does not exactly divide into this one
   */
  def exactDivide(other: Frequency): Int = {
    ArgChecker.notNull(other, "other")
    if (isMonthBased() && other.isMonthBased()) {
      val paymentMonths = getPeriod().toTotalMonths()
      val accrualMonths = other.getPeriod().toTotalMonths()
      if ((paymentMonths % accrualMonths) == 0) {
        return Math.toIntExact(paymentMonths / accrualMonths)
      }
    } else if (period.toTotalMonths() == 0 && other.period.toTotalMonths() == 0) {
      val paymentDays: Long = getPeriod().getDays().toLong
      val accrualDays: Long = other.getPeriod().getDays().toLong
      if ((paymentDays % accrualDays) == 0) {
        return Math.toIntExact(paymentDays / accrualDays)
      }
    }
    throw new IllegalArgumentException(Messages.format(
        "Frequency '{}' is not a multiple of '{}'", this, other))
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value of the specified unit.
   *
   * This will return a value for the years, months and days units.
   * Note that weeks are not included.
   * All other units throw an exception.
   *
   * The 'Term' period is returned as a period of 10,000 years.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   *
   * @param unit  the unit to query
   * @return the value of the unit
   * @throws java.time.temporal.UnsupportedTemporalTypeException if the unit is not supported
   */
  override def get(unit: TemporalUnit): Long = {
    period.get(unit)
  }

  /**
   * Gets the unit of this periodic frequency.
   *
   * This returns a list containing years, months and days.
   * Note that weeks are not included.
   *
   * The 'Term' period is returned as a period of 10,000 years.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   *
   * @return a list containing the years, months and days units
   */
  override def getUnits(): List[TemporalUnit] = {
    period.getUnits()
  }

  /**
   * Adds the period of this frequency to the specified date.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   * Use `LocalDate.plus(TemporalAmount)` instead.
   *
   * @param temporal  the temporal object to add to
   * @return the result with this frequency added
   * @throws java.time.DateTimeException if unable to add
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  override def addTo(temporal: Temporal): Temporal = {
    // special case for performance
    temporal match {
      case date: LocalDate =>
        date.plusMonths(period.toTotalMonths()).plusDays(period.getDays().toLong)
      case _ =>
        period.addTo(temporal)
    }
  }

  /**
   * Subtracts the period of this frequency from the specified date.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   * Use `LocalDate.minus(TemporalAmount)` instead.
   *
   * @param temporal  the temporal object to subtract from
   * @return the result with this frequency subtracted
   * @throws java.time.DateTimeException if unable to subtract
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  override def subtractFrom(temporal: Temporal): Temporal = {
    // special case for performance
    temporal match {
      case date: LocalDate =>
        date.minusMonths(period.toTotalMonths()).minusDays(period.getDays().toLong)
      case _ =>
        period.subtractFrom(temporal)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this periodic frequency equals another periodic frequency.
   *
   * The comparison checks the frequency period.
   * 
   * @param obj  the other frequency, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    if (obj == null || (getClass() ne obj.getClass())) {
      return false
    }
    val other = obj.asInstanceOf[Frequency]
    period.equals(other.period)
  }

  /**
   * Returns a suitable hash code for the periodic frequency.
   * 
   * @return the hash code
   */
  override def hashCode(): Int = {
    period.hashCode()
  }

  /**
   * Returns a formatted string representing the periodic frequency.
   *
   * The format is a combination of the quantity and unit, such as P1D, P2W, P3M, P4Y.
   * The 'Term' amount is returned as 'Term'.
   *
   * @return the formatted frequency
   */
  @ToString
  override def toString(): String = {
    name
  }

}

object Frequency {

  /**
   * A periodic frequency of one day.
   * Also known as daily.
   * There are considered to be 364 events per year with this frequency.
   */
  @static final val P1D: Frequency = ofDays(1)
  /**
   * A periodic frequency of 1 week (7 days).
   * Also known as weekly.
   * There are considered to be 52 events per year with this frequency.
   */
  @static final val P1W: Frequency = new Frequency(Period.ofWeeks(1), "P1W")
  /**
   * A periodic frequency of 2 weeks (14 days).
   * Also known as bi-weekly.
   * There are considered to be 26 events per year with this frequency.
   */
  @static final val P2W: Frequency = new Frequency(Period.ofWeeks(2), "P2W")
  /**
   * A periodic frequency of 4 weeks (28 days).
   * Also known as lunar.
   * There are considered to be 13 events per year with this frequency.
   */
  @static final val P4W: Frequency = new Frequency(Period.ofWeeks(4), "P4W")
  /**
   * A periodic frequency of 13 weeks (91 days).
   * There are considered to be 4 events per year with this frequency.
   */
  @static final val P13W: Frequency = new Frequency(Period.ofWeeks(13), "P13W")
  /**
   * A periodic frequency of 26 weeks (182 days).
   * There are considered to be 2 events per year with this frequency.
   */
  @static final val P26W: Frequency = new Frequency(Period.ofWeeks(26), "P26W")
  /**
   * A periodic frequency of 52 weeks (364 days).
   * There is considered to be 1 event per year with this frequency.
   */
  @static final val P52W: Frequency = new Frequency(Period.ofWeeks(52), "P52W")
  /**
   * A periodic frequency of 1 month.
   * Also known as monthly.
   * There are 12 events per year with this frequency.
   */
  @static final val P1M: Frequency = new Frequency(Period.ofMonths(1))
  /**
   * A periodic frequency of 2 months.
   * Also known as bi-monthly.
   * There are 6 events per year with this frequency.
   */
  @static final val P2M: Frequency = new Frequency(Period.ofMonths(2))
  /**
   * A periodic frequency of 3 months.
   * Also known as quarterly.
   * There are 4 events per year with this frequency.
   */
  @static final val P3M: Frequency = new Frequency(Period.ofMonths(3))
  /**
   * A periodic frequency of 4 months.
   * There are 3 events per year with this frequency.
   */
  @static final val P4M: Frequency = new Frequency(Period.ofMonths(4))
  /**
   * A periodic frequency of 6 months.
   * Also known as semi-annual.
   * There are 2 events per year with this frequency.
   */
  @static final val P6M: Frequency = new Frequency(Period.ofMonths(6))
  /**
   * A periodic frequency of 12 months (1 year).
   * Also known as annual.
   * There is 1 event per year with this frequency.
   */
  @static final val P12M: Frequency = new Frequency(Period.ofMonths(12))
  /**
   * A periodic frequency matching the term.
   * Also known as zero-coupon.
   * This is represented using the period 10,000 years.
   * There are no events per year with this frequency.
   */
  @static final val TERM: Frequency = new Frequency(Period.ofYears(TERM_YEARS), "Term")

  /**
   * The artificial maximum length of a normal tenor in years.
   */
  private final val MAX_YEARS = 1000
  /**
   * The artificial maximum length of a normal tenor in months.
   */
  private final val MAX_MONTHS = MAX_YEARS * 12
  /**
   * The artificial length in years of the 'Term' frequency.
   */
  private final val TERM_YEARS = 10000

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a `Period`.
   *
   * The period normally consists of either days and weeks, or months and years.
   * It must also be positive and non-zero.
   *
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   * Months are not normalized into years.
   *
   * The maximum tenor length is 1,000 years.
   *
   * @param period  the period to convert to a periodic frequency
   * @return the periodic frequency
   * @throws java.lang.IllegalArgumentException if the period is negative, zero or too large
   */
  def of(period: Period): Frequency = {
    ArgChecker.notNull(period, "period")
    val days = period.getDays()
    val months = period.toTotalMonths()
    if (months == 0 && days != 0) {
      return ofDays(days)
    }
    if (months > MAX_MONTHS) {
      throw new IllegalArgumentException("Period must not exceed 1000 years")
    }
    new Frequency(period)
  }

  /**
   * Obtains an instance backed by a period of days.
   *
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   *
   * @param days  the number of days
   * @return the periodic frequency
   * @throws java.lang.IllegalArgumentException if days is negative or zero
   */
  def ofDays(days: Int): Frequency = {
    if (days % 7 == 0) {
      return ofWeeks(days / 7)
    }
    new Frequency(Period.ofDays(days))
  }

  /**
   * Obtains an instance backed by a period of weeks.
   *
   * @param weeks  the number of weeks
   * @return the periodic frequency
   * @throws java.lang.IllegalArgumentException if weeks is negative or zero
   */
  def ofWeeks(weeks: Int): Frequency = {
    weeks match {
      case 1 => P1W
      case 2 => P2W
      case 4 => P4W
      case 13 => P13W
      case 26 => P26W
      case 52 => P52W
      case _ => new Frequency(Period.ofWeeks(weeks), "P" + weeks + "W")
    }
  }

  /**
   * Obtains an instance backed by a period of months.
   *
   * Months are not normalized into years.
   *
   * @param months  the number of months
   * @return the periodic frequency
   * @throws java.lang.IllegalArgumentException if months is negative, zero or over 12,000
   */
  def ofMonths(months: Int): Frequency = {
    months match {
      case 1 => P1M
      case 2 => P2M
      case 3 => P3M
      case 4 => P4M
      case 6 => P6M
      case 12 => P12M
      case _ =>
        if (months > MAX_MONTHS) {
          throw new IllegalArgumentException(maxMonthMsg())
        }
        new Frequency(Period.ofMonths(months))
    }
  }

  // extracted to aid inlining
  private def maxMonthMsg(): String = {
    val formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.ENGLISH))
    "Months must not exceed " + formatter.format(MAX_MONTHS.toLong)
  }

  /**
   * Obtains an instance backed by a period of years.
   *
   * @param years  the number of years
   * @return the periodic frequency
   * @throws java.lang.IllegalArgumentException if years is negative, zero or over 1,000
   */
  def ofYears(years: Int): Frequency = {
    if (years > MAX_YEARS) {
      throw new IllegalArgumentException(maxYearMsg())
    }
    new Frequency(Period.ofYears(years))
  }

  // extracted to aid inlining
  private def maxYearMsg(): String = {
    val formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.ENGLISH))
    "Years must not exceed " + formatter.format(MAX_YEARS.toLong)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the frequency.
   *
   * The format can either be based on ISO-8601, such as 'P3M'
   * or without the 'P' prefix e.g. '2W'.
   *
   * The period must be positive and non-zero.
   *
   * @param toParse  the string representing the frequency
   * @return the frequency
   * @throws java.lang.IllegalArgumentException if the frequency cannot be parsed
   */
  @FromString
  def parse(toParse: String): Frequency = {
    ArgChecker.notNull(toParse, "toParse")
    if (toParse.equalsIgnoreCase("Term") ||
        toParse.equalsIgnoreCase("T") ||
        toParse.equalsIgnoreCase("0T") ||
        toParse.equalsIgnoreCase("1T")) {
      return TERM
    }
    val prefixed = if (toParse.startsWith("P")) toParse else "P" + toParse
    try {
      Frequency.of(Period.parse(prefixed))
    } catch {
      case ex: DateTimeParseException =>
        throw new IllegalArgumentException(ex)
    }
  }

  //-------------------------------------------------------------------------
  // calculate events per year
  private def calculateEventsPerYear(period: Period): Int = {
    val monthsLong = period.toTotalMonths()
    if (monthsLong > MAX_MONTHS) {
      0
    } else {
      val months = monthsLong.toInt
      val days = period.getDays()
      if (months > 0 && days == 0) {
        if (12 % months == 0) 12 / months else -1
      } else if (days > 0 && months == 0) {
        if (364 % days == 0) 364 / days else -1
      } else {
        -1
      }
    }
  }

  // calculate the estimated events per year
  private def calculateEventsPerYearEstimate(period: Period): Double = {
    val monthsLong = period.toTotalMonths()
    if (monthsLong > MAX_MONTHS) {
      0d
    } else {
      val months = monthsLong.toInt
      val days = period.getDays()
      if (months > 0 && days == 0) {
        12d / months
      } else if (days > 0 && months == 0) {
        364d / days
      } else {
        val estimatedSecs: Double =
          (months * MONTHS.getDuration().getSeconds() + days * DAYS.getDuration().getSeconds()).toDouble
        YEARS.getDuration().getSeconds() / estimatedSecs
      }
    }
  }

}
