/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.Period

import com.opengamma.strata.collect.ArgChecker

/**
 * Standard period addition implementations.
 *
 * See `PeriodAdditionConventions` for the description of each.
 */
private[date] enum StandardPeriodAdditionConventions(conventionName: String)
  extends java.lang.Enum[StandardPeriodAdditionConventions] with PeriodAdditionConvention {

  // no specific addition rule
  case NONE extends StandardPeriodAdditionConventions("None")

  // last day of month
  case LAST_DAY extends StandardPeriodAdditionConventions("LastDay")

  // last business day of month
  case LAST_BUSINESS_DAY extends StandardPeriodAdditionConventions("LastBusinessDay")

  //-------------------------------------------------------------------------
  override def adjust(baseDate: LocalDate, period: Period, calendar: HolidayCalendar): LocalDate = {
    ArgChecker.notNull(baseDate, "baseDate")
    ArgChecker.notNull(period, "period")
    ArgChecker.notNull(calendar, "calendar")
    this match {
      case StandardPeriodAdditionConventions.NONE =>
        baseDate.plus(period)

      case StandardPeriodAdditionConventions.LAST_DAY =>
        val endDate = baseDate.plus(period)
        if (baseDate.getDayOfMonth() == baseDate.lengthOfMonth()) {
          endDate.withDayOfMonth(endDate.lengthOfMonth())
        } else {
          endDate
        }

      case StandardPeriodAdditionConventions.LAST_BUSINESS_DAY =>
        val endDate = baseDate.plus(period)
        if (calendar.isLastBusinessDayOfMonth(baseDate)) {
          calendar.lastBusinessDayOfMonth(endDate)
        } else {
          endDate
        }
    }
  }

  override def isMonthBased(): Boolean = {
    this match {
      case StandardPeriodAdditionConventions.NONE => false
      case StandardPeriodAdditionConventions.LAST_DAY => true
      case StandardPeriodAdditionConventions.LAST_BUSINESS_DAY => true
    }
  }

  override def getName(): String = {
    conventionName
  }

  override def toString(): String = {
    conventionName
  }

}
