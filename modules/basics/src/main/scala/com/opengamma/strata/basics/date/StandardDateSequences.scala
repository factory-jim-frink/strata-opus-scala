/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

import com.opengamma.strata.collect.ArgChecker

/**
 * Standard date sequence implementations.
 *
 * See `DateSequences` for the description of each.
 */
private[date] enum StandardDateSequences(conventionName: String)
  extends java.lang.Enum[StandardDateSequences] with DateSequence {

  // IMM in Mar/Jun/Sep/Dec
  case QUARTERLY_IMM extends StandardDateSequences("Quarterly-IMM")

  // IMM in Mar/Jun/Sep/Dec plus first 4 non quarterly serial months making 6 serial months in total
  case QUARTERLY_IMM_6_SERIAL extends StandardDateSequences("Quarterly-IMM-6-Serial")

  // IMM in Mar/Jun/Sep/Dec plus first 2 non quarterly serial months making 3 serial months in total
  case QUARTERLY_IMM_3_SERIAL extends StandardDateSequences("Quarterly-IMM-3-Serial")

  // Third Wednesday
  case MONTHLY_IMM extends StandardDateSequences("Monthly-IMM")

  // 10th in Mar/Jun/Sep/Dec
  case QUARTERLY_10TH extends StandardDateSequences("Quarterly-10th")

  // 1st of the month
  case MONTHLY_1ST extends StandardDateSequences("Monthly-1st")

  //-------------------------------------------------------------------------
  override def baseSequence(): DateSequence = {
    this match {
      case StandardDateSequences.QUARTERLY_IMM_6_SERIAL => StandardDateSequences.QUARTERLY_IMM
      case StandardDateSequences.QUARTERLY_IMM_3_SERIAL => StandardDateSequences.QUARTERLY_IMM
      case _ => this
    }
  }

  override def next(date: LocalDate): LocalDate = {
    nth(date, 1)
  }

  override def nextOrSame(date: LocalDate): LocalDate = {
    nthOrSame(date, 1)
  }

  override def nth(date: LocalDate, sequenceNumber: Int): LocalDate = {
    val thirdWednesday = StandardDateSequences.THIRD_WEDNESDAY
    this match {
      case StandardDateSequences.QUARTERLY_IMM =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.`with`(thirdWednesday)
        if (!base.isAfter(date)) {
          base = base.plusMonths(1)
        }
        shiftQuarterlyImm(base, sequenceNumber)

      case StandardDateSequences.QUARTERLY_IMM_6_SERIAL =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var baseImm = date.`with`(thirdWednesday)
        if (!baseImm.isAfter(date)) {
          baseImm = baseImm.plusMonths(1)
        }
        shiftQuarterlyImm6Serial(baseImm, sequenceNumber)

      case StandardDateSequences.QUARTERLY_IMM_3_SERIAL =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var baseImm = date.`with`(thirdWednesday)
        if (!baseImm.isAfter(date)) {
          baseImm = baseImm.plusMonths(1)
        }
        shiftQuarterlyImm3Serial(baseImm, sequenceNumber)

      case StandardDateSequences.MONTHLY_IMM =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        val base = date.`with`(thirdWednesday)
        if (!base.isAfter(date)) {
          base.plusMonths(sequenceNumber).`with`(thirdWednesday)
        } else {
          base.plusMonths(sequenceNumber - 1).`with`(thirdWednesday)
        }

      case StandardDateSequences.QUARTERLY_10TH =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.withDayOfMonth(10)
        if (!base.isAfter(date)) {
          base = base.plusMonths(1)
        }
        shiftQuarterly10th(base, sequenceNumber)

      case StandardDateSequences.MONTHLY_1ST =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.withDayOfMonth(1)
        if (!base.isAfter(date)) {
          base = base.plusMonths(1)
        }
        base.plusMonths(sequenceNumber - 1)
    }
  }

  override def nthOrSame(date: LocalDate, sequenceNumber: Int): LocalDate = {
    val thirdWednesday = StandardDateSequences.THIRD_WEDNESDAY
    this match {
      case StandardDateSequences.QUARTERLY_IMM =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.`with`(thirdWednesday)
        if (base.isBefore(date)) {
          base = base.plusMonths(1)
        }
        shiftQuarterlyImm(base, sequenceNumber)

      case StandardDateSequences.QUARTERLY_IMM_6_SERIAL =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var baseImm = date.`with`(thirdWednesday)
        if (baseImm.isBefore(date)) {
          baseImm = baseImm.plusMonths(1)
        }
        shiftQuarterlyImm6Serial(baseImm, sequenceNumber)

      case StandardDateSequences.QUARTERLY_IMM_3_SERIAL =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var baseImm = date.`with`(thirdWednesday)
        if (baseImm.isBefore(date)) {
          baseImm = baseImm.plusMonths(1)
        }
        shiftQuarterlyImm3Serial(baseImm, sequenceNumber)

      case StandardDateSequences.MONTHLY_IMM =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        val base = date.`with`(thirdWednesday)
        if (base.isBefore(date)) {
          base.plusMonths(sequenceNumber).`with`(thirdWednesday)
        } else {
          base.plusMonths(sequenceNumber - 1).`with`(thirdWednesday)
        }

      case StandardDateSequences.QUARTERLY_10TH =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.withDayOfMonth(10)
        if (base.isBefore(date)) {
          base = base.plusMonths(1)
        }
        shiftQuarterly10th(base, sequenceNumber)

      case StandardDateSequences.MONTHLY_1ST =>
        ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
        var base = date.withDayOfMonth(1)
        if (base.isBefore(date)) {
          base = base.plusMonths(1)
        }
        base.plusMonths(sequenceNumber - 1)
    }
  }

  override def dateMatching(yearMonth: YearMonth): LocalDate = {
    this match {
      case StandardDateSequences.MONTHLY_IMM =>
        yearMonth.atDay(1).`with`(StandardDateSequences.THIRD_WEDNESDAY)
      case _ =>
        nextOrSame(yearMonth.atDay(1))
    }
  }

  //-------------------------------------------------------------------------
  // QUARTERLY_IMM shift
  private def shiftQuarterlyImm(base: LocalDate, sequenceNumber: Int): LocalDate = {
    val monthsToAdd = StandardDateSequences.monthsToAdd(base.getMonthValue(), sequenceNumber)
    base.plusMonths(monthsToAdd).`with`(StandardDateSequences.THIRD_WEDNESDAY)
  }

  // QUARTERLY_IMM_6_SERIAL shift
  private def shiftQuarterlyImm6Serial(base: LocalDate, sequenceNumber: Int): LocalDate = {
    // first 4 serial can be expanded to first 6 serial by subsuming first two quarterly
    if (sequenceNumber <= 6) {
      base.plusMonths(sequenceNumber - 1).`with`(StandardDateSequences.THIRD_WEDNESDAY)
    } else {
      val effectiveQuarterlySequenceNumber = sequenceNumber - 4
      val monthsToAdd = StandardDateSequences.monthsToAdd(base.getMonthValue(), effectiveQuarterlySequenceNumber)
      base.plusMonths(monthsToAdd).`with`(StandardDateSequences.THIRD_WEDNESDAY)
    }
  }

  // QUARTERLY_IMM_3_SERIAL shift
  private def shiftQuarterlyImm3Serial(base: LocalDate, sequenceNumber: Int): LocalDate = {
    // first 2 serial can be expanded to first 3 serial by subsuming first quarterly
    if (sequenceNumber <= 3) {
      base.plusMonths(sequenceNumber - 1).`with`(StandardDateSequences.THIRD_WEDNESDAY)
    } else {
      val effectiveQuarterlySequenceNumber = sequenceNumber - 2
      val monthsToAdd = StandardDateSequences.monthsToAdd(base.getMonthValue(), effectiveQuarterlySequenceNumber)
      base.plusMonths(monthsToAdd).`with`(StandardDateSequences.THIRD_WEDNESDAY)
    }
  }

  // QUARTERLY_10TH shift
  private def shiftQuarterly10th(base: LocalDate, sequenceNumber: Int): LocalDate = {
    val monthsToAdd = StandardDateSequences.monthsToAdd(base.getMonthValue(), sequenceNumber)
    base.plusMonths(monthsToAdd).withDayOfMonth(10)
  }

  //-------------------------------------------------------------------------
  override def getName(): String = {
    conventionName
  }

  override def toString(): String = {
    conventionName
  }

}

private[date] object StandardDateSequences {

  //-------------------------------------------------------------------------
  // Third Wednesday
  private lazy val THIRD_WEDNESDAY: TemporalAdjuster = TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY)

  // calculates the number of months to add to the base date for a quarterly sequence (Mar/Jun/Sep/Dec)
  private def monthsToAdd(baseMonth: Int, sequenceNumber: Int): Int = {
    val monthInQuarter = (baseMonth + 2) % 3  // Jan/Apr/Jul/Oct is 0, Feb/May/Aug/Nov is 1, Mar/Jun/Sep/Dec is 2
    val monthsUntilNextQuarter = 2 - monthInQuarter
    monthsUntilNextQuarter + (sequenceNumber - 1) * 3
  }

}
