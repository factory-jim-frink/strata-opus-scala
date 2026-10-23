/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate

import com.opengamma.strata.basics.date.DayCount.ScheduleInfo
import com.opengamma.strata.basics.date.LocalDateUtils.daysBetween
import com.opengamma.strata.basics.date.LocalDateUtils.doy
import com.opengamma.strata.basics.schedule.Frequency

/**
 * Standard day count convention implementations.
 *
 * See `DayCounts` for the description of each.
 */
private[date] enum StandardDayCounts(conventionName: String)
  extends java.lang.Enum[StandardDayCounts] with DayCount {

  // always one
  case ONE_ONE extends StandardDayCounts("1/1")

  // actual days / actual days in year
  case ACT_ACT_ISDA extends StandardDayCounts("Act/Act ISDA")

  // complex ICMA calculation
  case ACT_ACT_ICMA extends StandardDayCounts("Act/Act ICMA")

  // AFB year-based calculation
  case ACT_ACT_AFB extends StandardDayCounts("Act/Act AFB")

  // actual days / actual days in year from start date
  case ACT_ACT_YEAR extends StandardDayCounts("Act/Act Year")

  // actual days / 365 or 366
  case ACT_365_ACTUAL extends StandardDayCounts("Act/365 Actual")

  // actual days / 365 or 366
  case ACT_365L extends StandardDayCounts("Act/365L")

  // simple actual days / 360
  case ACT_360 extends StandardDayCounts("Act/360")

  // simple actual days / 364
  case ACT_364 extends StandardDayCounts("Act/364")

  // simple actual days / 365
  case ACT_365F extends StandardDayCounts("Act/365F")

  // simple actual days / 365.25
  case ACT_365_25 extends StandardDayCounts("Act/365.25")

  // no leaps / 360
  case NL_360 extends StandardDayCounts("NL/360")

  // no leaps / 365
  case NL_365 extends StandardDayCounts("NL/365")

  // ISDA thirty day months / 360
  case THIRTY_360_ISDA extends StandardDayCounts("30/360 ISDA")

  // US thirty day months / 360 with dynamic EOM rule
  case THIRTY_U_360 extends StandardDayCounts("30U/360")

  // US thirty day months / 360 with fixed EOM rule
  case THIRTY_U_360_EOM extends StandardDayCounts("30U/360 EOM")

  case THIRTY_360_PSA extends StandardDayCounts("30/360 PSA")

  // ISDA EU thirty day months / 360
  case THIRTY_E_360_ISDA extends StandardDayCounts("30E/360 ISDA")

  // E thirty day months / 360
  case THIRTY_E_360 extends StandardDayCounts("30E/360")

  // E+ thirty day months / 360
  case THIRTY_EPLUS_360 extends StandardDayCounts("30E+/360")

  // thirty day months / 365 (German)
  case THIRTY_E_365 extends StandardDayCounts("30E/365")

  //-------------------------------------------------------------------------
  // calculate using the 30/360 function as above but does not divide by 360, as the number of days is needed, not the fraction
  private def thirty360Days(y1: Int, m1: Int, d1: Int, y2: Int, m2: Int, d2: Int): Int = {
    360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1)
  }

  // determine if the date is the last day of february
  private def lastDayOfFebruary(date: LocalDate): Boolean = {
    date.getMonthValue() == 2 && date.getDayOfMonth() == date.lengthOfMonth()
  }

  // calculate the number of leap days between two dates
  private def numberOfLeapDays(firstDate: LocalDate, secondDate: LocalDate): Int = {
    var numberOfLeapDays = 0
    var temp = DateAdjusters.nextLeapDay(firstDate)
    while (temp.isAfter(secondDate) == false) {
      numberOfLeapDays += 1
      temp = DateAdjusters.nextLeapDay(temp)
    }
    numberOfLeapDays
  }

  override def yearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: ScheduleInfo): Double = {
    if (secondDate.isBefore(firstDate)) {
      throw new IllegalArgumentException("Dates must be in time-line order")
    }
    calculateYearFraction(firstDate, secondDate, scheduleInfo)
  }

  override def days(firstDate: LocalDate, secondDate: LocalDate): Int = {
    if (secondDate.isBefore(firstDate)) {
      throw new IllegalArgumentException("Dates must be in time-line order")
    }
    calculateDays(firstDate, secondDate)
  }

  override def relativeYearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: ScheduleInfo): Double = {
    // override to avoid duplicate null checks
    if (secondDate.isBefore(firstDate)) {
      -calculateYearFraction(secondDate, firstDate, scheduleInfo)
    } else {
      calculateYearFraction(firstDate, secondDate, scheduleInfo)
    }
  }

  // calculate the year fraction, using validated inputs
  private[date] def calculateYearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: ScheduleInfo): Double = {
    this match {
      case StandardDayCounts.ONE_ONE =>
        1

      case StandardDayCounts.ACT_ACT_ISDA =>
        val y1 = firstDate.getYear()
        val y2 = secondDate.getYear()
        val firstYearLength: Double = firstDate.lengthOfYear()
        if (y1 == y2) {
          val actualDays: Double = doy(secondDate) - doy(firstDate)
          actualDays / firstYearLength
        } else {
          val firstRemainderOfYear: Double = firstYearLength - doy(firstDate) + 1
          val secondRemainderOfYear: Double = doy(secondDate) - 1
          val secondYearLength: Double = secondDate.lengthOfYear()
          firstRemainderOfYear / firstYearLength +
              secondRemainderOfYear / secondYearLength +
              (y2 - y1 - 1)
        }

      case StandardDayCounts.ACT_ACT_ICMA =>
        // avoid using ScheduleInfo in this case
        if (firstDate.equals(secondDate)) {
          0d
        } else {
          // calculation is based on the schedule period, firstDate assumed to be the start of the period
          val scheduleEndDate = scheduleInfo.getEndDate()
          val nextCouponDate = scheduleInfo.getPeriodEndDate(firstDate)
          val freq = scheduleInfo.getFrequency()
          val eom = scheduleInfo.isEndOfMonthConvention()
          // final period, also handling single period schedules
          if (nextCouponDate.equals(scheduleEndDate)) {
            icmaFinalPeriod(firstDate, secondDate, freq, eom)
          } else {
            // handle initial period, and all other cases where need to determine the previous coupon date
            // whether that is real or nominal
            icmaInitPeriod(firstDate, secondDate, nextCouponDate, freq, eom)
          }
        }

      case StandardDayCounts.ACT_ACT_AFB =>
        // tests show that there is no need to perform an initial check of period less than
        // or equal one year when using the OpenGamma interpretation of end-of-February rule
        // calculate the number of whole years back from the end
        // OpenGamma interpretation: reject ISDA end-of-Feb if 28th Feb, apply simple subtraction from secondDate
        var end = secondDate
        var start = secondDate.minusYears(1)
        var years = 0
        while (!start.isBefore(firstDate)) {
          years += 1
          end = start
          start = secondDate.minusYears(years + 1)
        }
        // calculate the remaining fraction, including start, excluding end
        val actualDays = daysBetween(firstDate, end)
        val nextLeap = DateAdjusters.nextOrSameLeapDay(firstDate)
        years + (actualDays / (if (nextLeap.isBefore(end)) 366d else 365d))

      case StandardDayCounts.ACT_ACT_YEAR =>
        var startDate = firstDate
        var yearsAdded = 0
        while (secondDate.compareTo(startDate.plusYears(1)) > 0) {
          yearsAdded += 1
          startDate = firstDate.plusYears(yearsAdded)
        }
        val actualDays: Double = daysBetween(startDate, secondDate).toDouble
        val actualDaysInYear: Double = daysBetween(startDate, startDate.plusYears(1)).toDouble
        yearsAdded + (actualDays / actualDaysInYear)

      case StandardDayCounts.ACT_365_ACTUAL =>
        val actualDays = daysBetween(firstDate, secondDate)
        val nextLeap = DateAdjusters.nextLeapDay(firstDate)
        actualDays / (if (nextLeap.isAfter(secondDate)) 365d else 366d)

      case StandardDayCounts.ACT_365L =>
        val actualDays = daysBetween(firstDate, secondDate)
        // avoid using ScheduleInfo in this case
        if (firstDate.equals(secondDate)) {
          0d
        } else {
          // calculation is based on the end of the schedule period (next coupon date) and annual/non-annual frequency
          val nextCouponDate = scheduleInfo.getPeriodEndDate(firstDate)
          if (scheduleInfo.getFrequency().isAnnual()) {
            val nextLeap = DateAdjusters.nextLeapDay(firstDate)
            actualDays / (if (nextLeap.isAfter(nextCouponDate)) 365d else 366d)
          } else {
            actualDays / (if (nextCouponDate.isLeapYear()) 366d else 365d)
          }
        }

      case StandardDayCounts.ACT_360 =>
        daysBetween(firstDate, secondDate) / 360d

      case StandardDayCounts.ACT_364 =>
        daysBetween(firstDate, secondDate) / 364d

      case StandardDayCounts.ACT_365F =>
        daysBetween(firstDate, secondDate) / 365d

      case StandardDayCounts.ACT_365_25 =>
        daysBetween(firstDate, secondDate) / 365.25d

      case StandardDayCounts.NL_360 =>
        val actualDays = daysBetween(firstDate, secondDate)
        val numberOfLeapDays = this.numberOfLeapDays(firstDate, secondDate)
        (actualDays - numberOfLeapDays) / 360d

      case StandardDayCounts.NL_365 =>
        val actualDays = daysBetween(firstDate, secondDate)
        val numberOfLeapDays = this.numberOfLeapDays(firstDate, secondDate)
        (actualDays - numberOfLeapDays) / 365d

      case StandardDayCounts.THIRTY_360_ISDA =>
        calculateDays(firstDate, secondDate) / 360d

      case StandardDayCounts.THIRTY_U_360 =>
        if (scheduleInfo.isEndOfMonthConvention()) {
          StandardDayCounts.THIRTY_U_360_EOM.calculateYearFraction(firstDate, secondDate, scheduleInfo)
        } else {
          StandardDayCounts.THIRTY_360_ISDA.calculateYearFraction(firstDate, secondDate, scheduleInfo)
        }

      case StandardDayCounts.THIRTY_U_360_EOM =>
        calculateDays(firstDate, secondDate) / 360d

      case StandardDayCounts.THIRTY_360_PSA =>
        calculateDays(firstDate, secondDate) / 360d

      case StandardDayCounts.THIRTY_E_360_ISDA =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == 31 || lastDayOfFebruary(firstDate)) {
          d1 = 30
        }
        if (d2 == 31 || (lastDayOfFebruary(secondDate) && !secondDate.equals(scheduleInfo.getEndDate()))) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2) / 360d

      case StandardDayCounts.THIRTY_E_360 =>
        calculateDays(firstDate, secondDate) / 360d

      case StandardDayCounts.THIRTY_EPLUS_360 =>
        calculateDays(firstDate, secondDate) / 360d

      case StandardDayCounts.THIRTY_E_365 =>
        calculateDays(firstDate, secondDate) / 365d
    }
  }

  //calculate the number of days between the specified dates, using validated inputs
  private[date] def calculateDays(firstDate: LocalDate, secondDate: LocalDate): Int = {
    this match {
      case StandardDayCounts.ONE_ONE =>
        1

      case StandardDayCounts.NL_360 | StandardDayCounts.NL_365 =>
        val actualDays = daysBetween(firstDate, secondDate)
        val numberOfLeapDays = this.numberOfLeapDays(firstDate, secondDate)
        Math.toIntExact(actualDays) - numberOfLeapDays

      case StandardDayCounts.THIRTY_360_ISDA =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == 31) {
          d1 = 30
        }
        if (d2 == 31 && d1 == 30) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.THIRTY_U_360 =>
        StandardDayCounts.THIRTY_360_ISDA.days(firstDate, secondDate)

      case StandardDayCounts.THIRTY_U_360_EOM =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (lastDayOfFebruary(firstDate)) {
          if (lastDayOfFebruary(secondDate)) {
            d2 = 30
          }
          d1 = 30
        }
        // now apply '30/360 ISDA' rules
        if (d1 == 31) {
          d1 = 30
        }
        if (d2 == 31 && d1 == 30) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.THIRTY_360_PSA =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == firstDate.lengthOfMonth()) {
          d1 = 30
        }
        if (d2 == 31 && d1 == 30) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.THIRTY_E_360_ISDA =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == 31 || lastDayOfFebruary(firstDate)) {
          d1 = 30
        }
        if (d2 == 31 || (lastDayOfFebruary(secondDate))) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.THIRTY_E_360 =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == 31) {
          d1 = 30
        }
        if (d2 == 31) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.THIRTY_EPLUS_360 =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        val m1 = firstDate.getMonthValue()
        var m2 = secondDate.getMonthValue()
        if (d1 == 31) {
          d1 = 30
        }
        if (d2 == 31) {
          d2 = 1
          m2 = m2 + 1  // nature of calculation means no need to adjust Dec to Jan
        }
        thirty360Days(
            firstDate.getYear(), m1, d1,
            secondDate.getYear(), m2, d2)

      case StandardDayCounts.THIRTY_E_365 =>
        var d1 = firstDate.getDayOfMonth()
        var d2 = secondDate.getDayOfMonth()
        if (d1 == firstDate.lengthOfMonth()) {
          d1 = 30
        }
        if (d2 == secondDate.lengthOfMonth()) {
          d2 = 30
        }
        thirty360Days(
            firstDate.getYear(), firstDate.getMonthValue(), d1,
            secondDate.getYear(), secondDate.getMonthValue(), d2)

      case StandardDayCounts.ACT_ACT_ISDA | StandardDayCounts.ACT_ACT_ICMA | StandardDayCounts.ACT_ACT_AFB |
          StandardDayCounts.ACT_ACT_YEAR | StandardDayCounts.ACT_365_ACTUAL | StandardDayCounts.ACT_365L |
          StandardDayCounts.ACT_360 | StandardDayCounts.ACT_364 | StandardDayCounts.ACT_365F |
          StandardDayCounts.ACT_365_25 =>
        val actualDays = daysBetween(firstDate, secondDate)
        Math.toIntExact(actualDays)
    }
  }

  //-------------------------------------------------------------------------
  // Act/Act ICMA: calculate nominal periods backwards from couponDate
  private def icmaInitPeriod(
      startDate: LocalDate,
      endDate: LocalDate,
      couponDate: LocalDate,
      freq: Frequency,
      eom: Boolean): Double = {

    var currentNominal = couponDate
    var prevNominal = icmaEom(couponDate, currentNominal.minus(freq), eom)
    var result = 0d
    while (prevNominal.isAfter(startDate)) {
      result += icmaCalc(prevNominal, currentNominal, startDate, endDate, freq)
      currentNominal = prevNominal
      prevNominal = icmaEom(couponDate, currentNominal.minus(freq), eom)
    }
    result + icmaCalc(prevNominal, currentNominal, startDate, endDate, freq)
  }

  // Act/Act ICMA: calculate nominal periods forwards from couponDate
  private def icmaFinalPeriod(couponDate: LocalDate, endDate: LocalDate, freq: Frequency, eom: Boolean): Double = {
    var curNominal = couponDate
    var nextNominal = icmaEom(couponDate, curNominal.plus(freq), eom)
    var result = 0d
    while (nextNominal.isBefore(endDate)) {
      result += icmaCalc(curNominal, nextNominal, curNominal, endDate, freq)
      curNominal = nextNominal
      nextNominal = icmaEom(couponDate, curNominal.plus(freq), eom)
    }
    result + icmaCalc(curNominal, nextNominal, curNominal, endDate, freq)
  }

  // Act/Act ICMA: apply eom convention
  private def icmaEom(base: LocalDate, calc: LocalDate, eom: Boolean): LocalDate = {
    if (eom && base.getDayOfMonth() == base.lengthOfMonth()) calc.withDayOfMonth(calc.lengthOfMonth()) else calc
  }

  // Act/Act ICMA: calculate the result
  private def icmaCalc(
      prevNominal: LocalDate,
      curNominal: LocalDate,
      start: LocalDate,
      end: LocalDate,
      freq: Frequency): Double = {

    if (end.isAfter(prevNominal)) {
      val curNominalEpochDay = curNominal.toEpochDay()
      val prevNominalEpochDay = prevNominal.toEpochDay()
      val startEpochDay = start.toEpochDay()
      val endEpochDay = end.toEpochDay()
      val periodDays: Double = (curNominalEpochDay - prevNominalEpochDay).toDouble
      val actualDays: Double =
        (Math.min(endEpochDay, curNominalEpochDay) - Math.max(startEpochDay, prevNominalEpochDay)).toDouble
      actualDays / (freq.eventsPerYear() * periodDays)
    } else {
      0
    }
  }

  //-------------------------------------------------------------------------
  override def getName(): String = {
    conventionName
  }

  override def toString(): String = {
    conventionName
  }

}
