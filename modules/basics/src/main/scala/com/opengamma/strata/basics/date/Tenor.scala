/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.io.Serializable
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.List

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.date.LocalDateUtils.plusDays
import com.opengamma.strata.collect.ArgChecker

/**
 * A tenor indicating how long it will take for a financial instrument to reach maturity.
 *
 * A tenor is allowed to be any non-negative non-zero period of days, weeks, month or years.
 * This class provides constants for common tenors which are best used by static import.
 *
 * Each tenor is based on a `Period`. The months and years of the period are not normalized,
 * thus it is possible to have a tenor of 12 months and a different one of 1 year.
 * When used, standard date addition rules apply, thus there is no difference between them.
 * Call `normalized()` to apply normalization.
 *
 * ==Usage==
 * `Tenor` implements `TemporalAmount` allowing it to be directly added to a date:
 * {{{
 *  LocalDate later = baseDate.plus(tenor);
 * }}}
 */
@SerialVersionUID(1L)
final class Tenor private (
    /**
     * The period of the tenor.
     */
    private val period: Period,
    /**
     * The name of the tenor.
     */
    private val name: String)
  extends Comparable[Tenor] with TemporalAmount with Serializable {

  ArgChecker.notNull(period, "period")
  ArgChecker.isFalse(period.isZero(), "Tenor period must not be zero")
  ArgChecker.isFalse(period.isNegative(), "Tenor period must not be negative")

  // safe deserialization
  private def readResolve(): Object = {
    new Tenor(period, name)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying period of the tenor.
   *
   * @return the period
   */
  def getPeriod(): Period = {
    period
  }

  //-------------------------------------------------------------------------
  /**
   * Normalizes the months and years of this tenor.
   *
   * This method returns a normalized tenor of an equivalent length.
   * If the period is exactly 1 year then the result will be expressed as 12 months.
   * Otherwise, the result will be expressed using `Period#normalized()`.
   *
   * @return the normalized tenor
   */
  def normalized(): Tenor = {
    if (period.getDays() == 0 && period.toTotalMonths() == 12) {
      Tenor.TENOR_12M
    } else {
      val norm = period.normalized()
      if (norm ne period) Tenor.of(norm) else this
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the tenor is week-based.
   *
   * A week-based tenor consists of an integral number of weeks.
   * There must be no day, month or year element.
   *
   * @return true if this is week-based
   */
  def isWeekBased(): Boolean = {
    period.toTotalMonths() == 0 && period.getDays() % 7 == 0
  }

  /**
   * Checks if the tenor is month-based.
   *
   * A month-based tenor consists of an integral number of months.
   * Any year-based tenor is also counted as month-based.
   * There must be no day or week element.
   *
   * @return true if this is month-based
   */
  def isMonthBased(): Boolean = {
    period.toTotalMonths() > 0 && period.getDays() == 0
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value of the specified unit.
   *
   * This will return a value for the years, months and days units.
   * Note that weeks are not included.
   * All other units throw an exception.
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
   * Gets the units supported by a tenor.
   *
   * This returns a list containing years, months and days.
   * Note that weeks are not included.
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
   * Adds this tenor to the specified date.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   * Use `LocalDate#plus(TemporalAmount)` instead.
   *
   * @param temporal  the temporal object to add to
   * @return the result with this tenor added
   * @throws java.time.DateTimeException if unable to add
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  override def addTo(temporal: Temporal): Temporal = {
    // special case for performance
    temporal match {
      case date: LocalDate =>
        plusDays(date.plusMonths(period.toTotalMonths()), period.getDays())
      case _ =>
        period.addTo(temporal)
    }
  }

  /**
   * Subtracts this tenor from the specified date.
   *
   * This method implements `TemporalAmount`.
   * It is not intended to be called directly.
   * Use `LocalDate#minus(TemporalAmount)` instead.
   *
   * @param temporal  the temporal object to subtract from
   * @return the result with this tenor subtracted
   * @throws java.time.DateTimeException if unable to subtract
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  override def subtractFrom(temporal: Temporal): Temporal = {
    // special case for performance
    temporal match {
      case date: LocalDate =>
        plusDays(date.minusMonths(period.toTotalMonths()), -period.getDays())
      case _ =>
        period.subtractFrom(temporal)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this tenor to another tenor.
   *
   * Comparing tenors is a hard problem in general, but for commonly used tenors the outcome is as expected.
   * If the two tenors are both based on days, then comparison is easy.
   * If the two tenors are both based on months/years, then comparison is easy.
   * Otherwise, months are converted to days to form an estimated length in days which is compared.
   * The conversion from months to days divides by 12 and then multiplies by 365.2425.
   * As a result, this method is compatible with `equals(Object)` for tenors less than 400 years.
   *
   * The resulting order places:
   *  - a 1 month tenor between 30 and 31 days
   *  - a 2 month tenor between 60 and 61 days
   *  - a 3 month tenor between 91 and 92 days
   *  - a 6 month tenor between 182 and 183 days
   *  - a 1 year tenor between 365 and 366 days
   *  - a 4 year tenor between 1460 and 1461 days
   *
   * @param other  the other tenor
   * @return negative if this is less than the other, zero if equal and positive if greater
   */
  override def compareTo(other: Tenor): Int = {
    val thisDays = this.getPeriod().getDays()
    val thisMonths = this.getPeriod().toTotalMonths()
    val otherDays = other.getPeriod().getDays()
    val otherMonths = other.getPeriod().toTotalMonths()
    if (thisMonths == 0 && otherMonths == 0) {
      // both day-only
      Integer.compare(thisDays, otherDays)
    } else if (thisDays == 0 && otherDays == 0) {
      // both month-only
      java.lang.Long.compare(thisMonths, otherMonths)
    } else {
      // complex
      val thisMonthsInDays = (thisMonths / 12d) * 365.2425d
      val otherMonthsInDays = (otherMonths / 12d) * 365.2425d
      java.lang.Double.compare(thisDays + thisMonthsInDays, otherDays + otherMonthsInDays)
    }
  }

  /**
   * Checks if this tenor equals another tenor.
   *
   * The comparison checks the tenor period.
   *
   * @param obj  the other tenor, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj == null || (getClass() ne obj.getClass())) {
      false
    } else {
      val other = obj.asInstanceOf[Tenor]
      period.equals(other.period)
    }
  }

  /**
   * Returns a suitable hash code for the tenor.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    period.hashCode()
  }

  /**
   * Returns a formatted string representing the tenor.
   *
   * The format is a combination of the quantity and unit, such as 1D, 2W, 3M, 4Y.
   *
   * @return the formatted tenor
   */
  @ToString
  override def toString(): String = {
    name
  }

}

object Tenor {
  /**
   * A tenor of one day.
   */
  @static final val TENOR_1D: Tenor = ofDays(1)
  /**
   * A tenor of two days.
   */
  @static final val TENOR_2D: Tenor = ofDays(2)
  /**
   * A tenor of three days.
   */
  @static final val TENOR_3D: Tenor = ofDays(3)
  /**
   * A tenor of 1 week.
   */
  @static final val TENOR_1W: Tenor = ofWeeks(1)
  /**
   * A tenor of 2 weeks.
   */
  @static final val TENOR_2W: Tenor = ofWeeks(2)
  /**
   * A tenor of 3 weeks.
   */
  @static final val TENOR_3W: Tenor = ofWeeks(3)
  /**
   * A tenor of 4 weeks.
   */
  @static final val TENOR_4W: Tenor = ofWeeks(4)
  /**
   * A tenor of 6 weeks.
   */
  @static final val TENOR_6W: Tenor = ofWeeks(6)
  /**
   * A tenor of 13 weeks.
   */
  @static final val TENOR_13W: Tenor = ofWeeks(13)
  /**
   * A tenor of 26 weeks.
   */
  @static final val TENOR_26W: Tenor = ofWeeks(26)
  /**
   * A tenor of 52 weeks.
   */
  @static final val TENOR_52W: Tenor = ofWeeks(52)
  /**
   * A tenor of 1 month.
   */
  @static final val TENOR_1M: Tenor = ofMonths(1)
  /**
   * A tenor of 2 months.
   */
  @static final val TENOR_2M: Tenor = ofMonths(2)
  /**
   * A tenor of 3 months.
   */
  @static final val TENOR_3M: Tenor = ofMonths(3)
  /**
   * A tenor of 4 months.
   */
  @static final val TENOR_4M: Tenor = ofMonths(4)
  /**
   * A tenor of 5 months.
   */
  @static final val TENOR_5M: Tenor = ofMonths(5)
  /**
   * A tenor of 6 months.
   */
  @static final val TENOR_6M: Tenor = ofMonths(6)
  /**
   * A tenor of 7 months.
   */
  @static final val TENOR_7M: Tenor = ofMonths(7)
  /**
   * A tenor of 8 months.
   */
  @static final val TENOR_8M: Tenor = ofMonths(8)
  /**
   * A tenor of 9 months.
   */
  @static final val TENOR_9M: Tenor = ofMonths(9)
  /**
   * A tenor of 10 months.
   */
  @static final val TENOR_10M: Tenor = ofMonths(10)
  /**
   * A tenor of 11 months.
   */
  @static final val TENOR_11M: Tenor = ofMonths(11)
  /**
   * A tenor of 12 months.
   */
  @static final val TENOR_12M: Tenor = ofMonths(12)
  /**
   * A tenor of 15 months.
   */
  @static final val TENOR_15M: Tenor = ofMonths(15)
  /**
   * A tenor of 18 months.
   */
  @static final val TENOR_18M: Tenor = ofMonths(18)
  /**
   * A tenor of 21 months.
   */
  @static final val TENOR_21M: Tenor = ofMonths(21)
  /**
   * A tenor of 1 year.
   */
  @static final val TENOR_1Y: Tenor = ofYears(1)
  /**
   * A tenor of 2 years.
   */
  @static final val TENOR_2Y: Tenor = ofYears(2)
  /**
   * A tenor of 3 years.
   */
  @static final val TENOR_3Y: Tenor = ofYears(3)
  /**
   * A tenor of 4 years.
   */
  @static final val TENOR_4Y: Tenor = ofYears(4)
  /**
   * A tenor of 5 years.
   */
  @static final val TENOR_5Y: Tenor = ofYears(5)
  /**
   * A tenor of 6 years.
   */
  @static final val TENOR_6Y: Tenor = ofYears(6)
  /**
   * A tenor of 7 years.
   */
  @static final val TENOR_7Y: Tenor = ofYears(7)
  /**
   * A tenor of 8 years.
   */
  @static final val TENOR_8Y: Tenor = ofYears(8)
  /**
   * A tenor of 9 years.
   */
  @static final val TENOR_9Y: Tenor = ofYears(9)
  /**
   * A tenor of 10 years.
   */
  @static final val TENOR_10Y: Tenor = ofYears(10)
  /**
   * A tenor of 11 years.
   */
  @static final val TENOR_11Y: Tenor = ofYears(11)
  /**
   * A tenor of 12 years.
   */
  @static final val TENOR_12Y: Tenor = ofYears(12)
  /**
   * A tenor of 13 years.
   */
  @static final val TENOR_13Y: Tenor = ofYears(13)
  /**
   * A tenor of 14 years.
   */
  @static final val TENOR_14Y: Tenor = ofYears(14)
  /**
   * A tenor of 15 years.
   */
  @static final val TENOR_15Y: Tenor = ofYears(15)
  /**
   * A tenor of 20 years.
   */
  @static final val TENOR_20Y: Tenor = ofYears(20)
  /**
   * A tenor of 25 years.
   */
  @static final val TENOR_25Y: Tenor = ofYears(25)
  /**
   * A tenor of 30 years.
   */
  @static final val TENOR_30Y: Tenor = ofYears(30)
  /**
   * A tenor of 35 years.
   */
  @static final val TENOR_35Y: Tenor = ofYears(35)
  /**
   * A tenor of 40 years.
   */
  @static final val TENOR_40Y: Tenor = ofYears(40)
  /**
   * A tenor of 45 years.
   */
  @static final val TENOR_45Y: Tenor = ofYears(45)
  /**
   * A tenor of 50 years.
   */
  @static final val TENOR_50Y: Tenor = ofYears(50)

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
   * @param period  the period to convert to a tenor
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if the period is negative or zero
   */
  def of(period: Period): Tenor = {
    val days = period.getDays()
    val months = period.toTotalMonths()
    if (months == 0 && days != 0) {
      ofDays(days)
    } else {
      new Tenor(period, period.toString().substring(1))
    }
  }

  /**
   * Obtains an instance backed by a period of days.
   *
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   *
   * @param days  the number of days
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if days is negative or zero
   */
  def ofDays(days: Int): Tenor = {
    if (days % 7 == 0) {
      ofWeeks(days / 7)
    } else {
      new Tenor(Period.ofDays(days), Integer.toString(days) + "D")
    }
  }

  /**
   * Obtains an instance backed by a period of weeks.
   *
   * @param weeks  the number of weeks
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if weeks is negative or zero
   */
  def ofWeeks(weeks: Int): Tenor = {
    new Tenor(Period.ofWeeks(weeks), Integer.toString(weeks) + "W")
  }

  /**
   * Obtains an instance backed by a period of months.
   *
   * Months are not normalized into years.
   *
   * @param months  the number of months
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if months is negative or zero
   */
  def ofMonths(months: Int): Tenor = {
    new Tenor(Period.ofMonths(months), Integer.toString(months) + "M")
  }

  /**
   * Obtains an instance backed by a period of years.
   *
   * @param years  the number of years
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if years is negative or zero
   */
  def ofYears(years: Int): Tenor = {
    new Tenor(Period.ofYears(years), Integer.toString(years) + "Y")
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the tenor.
   *
   * The format can either be based on ISO-8601, such as 'P3M'
   * or without the 'P' prefix e.g. '2W'.
   *
   * @param toParse  the string representing the tenor
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if the tenor cannot be parsed
   */
  @FromString
  def parse(toParse: String): Tenor = {
    val prefixed = if (toParse.startsWith("P")) toParse else "P" + toParse
    try {
      Tenor.of(Period.parse(prefixed))
    } catch {
      case ex: DateTimeParseException => throw new IllegalArgumentException(ex)
    }
  }

}
