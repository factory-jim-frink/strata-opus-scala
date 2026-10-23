/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.date.HolidayCalendar
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.HolidayCalendarIds
import com.opengamma.strata.basics.date.HolidayCalendars
import com.opengamma.strata.collect.ArgChecker

/**
 * Standard roll convention implementations.
 *
 * See `RollConventions` for the description of each.
 */
private[schedule] enum StandardRollConventions(conventionName: String)
  extends java.lang.Enum[StandardRollConventions] with RollConvention {

  // no adjustment
  case NONE extends StandardRollConventions("None")

  // last day of month
  case EOM extends StandardRollConventions("EOM")

  // 3rd Wednesday
  case IMM extends StandardRollConventions("IMM")

  // 2nd London banking day before 3rd Wednesday, adjusted by Montreal & Toronto holiday
  case IMMCAD extends StandardRollConventions("IMMCAD")

  // 1 Sydney business day before 2nd Friday
  case IMMAUD extends StandardRollConventions("IMMAUD")

  // Wednesday on or after 9th
  case IMMNZD extends StandardRollConventions("IMMNZD")

  // 2nd Friday
  case SFE extends StandardRollConventions("SFE")

  // Each Monday, or Tuesday if USNY holiday
  case TBILL extends StandardRollConventions("TBILL")

  //-------------------------------------------------------------------------
  override def adjust(date: LocalDate): LocalDate = {
    this match {
      case StandardRollConventions.NONE =>
        ArgChecker.notNull(date, "date")

      case StandardRollConventions.EOM =>
        ArgChecker.notNull(date, "date")
        date.withDayOfMonth(date.lengthOfMonth())

      case StandardRollConventions.IMM =>
        ArgChecker.notNull(date, "date")
        date.`with`(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY))

      case StandardRollConventions.IMMCAD =>
        ArgChecker.notNull(date, "date")
        val wed3 = date.`with`(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY))
        StandardRollConventions.IMMCAD_CANADA.previousOrSame(StandardRollConventions.IMMCAD_GBLO.shift(wed3, -2))

      case StandardRollConventions.IMMAUD =>
        ArgChecker.notNull(date, "date")
        StandardRollConventions.IMMAUD_AUSY.previous(
            date.`with`(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY)))

      case StandardRollConventions.IMMNZD =>
        ArgChecker.notNull(date, "date")
        date.withDayOfMonth(9).`with`(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))

      case StandardRollConventions.SFE =>
        ArgChecker.notNull(date, "date")
        date.`with`(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY))

      case StandardRollConventions.TBILL =>
        ArgChecker.notNull(date, "date")
        StandardRollConventions.TBILL_USNY.nextOrSame(date.`with`(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)))
    }
  }

  override def getDayOfMonth(): Int = {
    this match {
      case StandardRollConventions.EOM =>
        // EOM is equivalent to 31 in FpML in most cases
        // because roll conventions 30 and 29 also have to adjust to end of February
        31
      case _ =>
        0
    }
  }

  override def getName(): String = {
    conventionName
  }

  override def toString(): String = {
    conventionName
  }

}

private[schedule] object StandardRollConventions {

  // the holiday calendars of the constants, initialized eagerly with the constants
  private val IMMCAD_GBLO: HolidayCalendar = holidayCalendar(HolidayCalendarIds.GBLO)
  private val IMMCAD_CANADA: HolidayCalendar =
    holidayCalendar(HolidayCalendarIds.CATO).combinedWith(holidayCalendar(HolidayCalendarIds.CAMO))
  private val IMMAUD_AUSY: HolidayCalendar = holidayCalendar(HolidayCalendarIds.AUSY)
  private val TBILL_USNY: HolidayCalendar = holidayCalendar(HolidayCalendarIds.USNY)

  // safely find a holiday calendar
  // this exists because using a RollConvention should not result in a reference data exception
  private def holidayCalendar(id: HolidayCalendarId): HolidayCalendar = {
    ReferenceData.standard().findValue(id).orElse(HolidayCalendars.SAT_SUN)
  }

}
