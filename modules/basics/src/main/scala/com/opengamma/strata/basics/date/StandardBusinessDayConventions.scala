/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

import com.opengamma.strata.collect.ArgChecker

/**
 * Standard business day convention implementations.
 *
 * See `BusinessDayConventions` for the description of each.
 */
private[date] enum StandardBusinessDayConventions(conventionName: String)
  extends java.lang.Enum[StandardBusinessDayConventions] with BusinessDayConvention {

  // make no adjustment
  case NO_ADJUST extends StandardBusinessDayConventions("NoAdjust")

  // next business day
  case FOLLOWING extends StandardBusinessDayConventions("Following")

  // next business day unless over a month end
  case MODIFIED_FOLLOWING extends StandardBusinessDayConventions("ModifiedFollowing")

  // next business day unless over a month end or mid
  case MODIFIED_FOLLOWING_BI_MONTHLY extends StandardBusinessDayConventions("ModifiedFollowingBiMonthly")

  // previous business day
  case PRECEDING extends StandardBusinessDayConventions("Preceding")

  // previous business day unless over a month end
  case MODIFIED_PRECEDING extends StandardBusinessDayConventions("ModifiedPreceding")

  // next business day if Sun/Mon, otherwise previous
  case NEAREST extends StandardBusinessDayConventions("Nearest")

  //-------------------------------------------------------------------------
  override def adjust(date: LocalDate, calendar: HolidayCalendar): LocalDate = {
    this match {
      case StandardBusinessDayConventions.NO_ADJUST =>
        ArgChecker.notNull(date, "date")

      case StandardBusinessDayConventions.FOLLOWING =>
        calendar.nextOrSame(date)

      case StandardBusinessDayConventions.MODIFIED_FOLLOWING =>
        calendar.nextSameOrLastInMonth(date)

      case StandardBusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY =>
        var adjusted = calendar.nextOrSame(date)
        if (adjusted.getMonthValue() != date.getMonthValue() ||
            (adjusted.getDayOfMonth() > 15 && date.getDayOfMonth() <= 15)) {
          adjusted = calendar.previous(date)
        }
        adjusted

      case StandardBusinessDayConventions.PRECEDING =>
        calendar.previousOrSame(date)

      case StandardBusinessDayConventions.MODIFIED_PRECEDING =>
        var adjusted = calendar.previousOrSame(date)
        if (adjusted.getMonth() ne date.getMonth()) {
          adjusted = calendar.next(date)
        }
        adjusted

      case StandardBusinessDayConventions.NEAREST =>
        if (calendar.isBusinessDay(date)) {
          date
        } else if ((date.getDayOfWeek() eq SUNDAY) || (date.getDayOfWeek() eq MONDAY)) {
          calendar.next(date)
        } else {
          calendar.previous(date)
        }
    }
  }

  override def getName(): String = {
    conventionName
  }

  override def toString(): String = {
    conventionName
  }

}
