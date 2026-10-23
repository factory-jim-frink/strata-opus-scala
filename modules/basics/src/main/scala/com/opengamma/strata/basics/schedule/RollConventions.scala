/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.time.DayOfWeek

import scala.annotation.static

import com.opengamma.strata.basics.schedule.DayRollConventions.Dom
import com.opengamma.strata.basics.schedule.DayRollConventions.Dow
import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard roll conventions.
 *
 * The purpose of this convention is to define how to roll dates when building a schedule.
 * The standard approach to building a schedule is based on unadjusted dates, which do not
 * have a business day convention applied.
 * To get the next date in the schedule, take the base date and the
 * periodic frequency. Once this date is calculated,
 * the roll convention is applied to produce the next schedule date.
 *
 * In most cases the specific values for day-of-month and day-of-week are not needed.
 * A one month periodic frequency will naturally select the same day-of-month as the
 * input date, thus the day-of-month does not need to be additionally specified.
 */
final class RollConventions private () {
}

object RollConventions {
  /**
   * The 'None' roll convention.
   *
   * The input date will not be adjusted.
   *
   * When calculating a schedule, there will be no further adjustment after the
   * periodic frequency is added or subtracted.
   */
  @static final val NONE: RollConvention =
    RollConvention.of(StandardRollConventions.NONE.getName())
  /**
   * The 'EOM' roll convention which adjusts the date to the end of the month.
   *
   * The input date will be adjusted ensure it is the last valid day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val EOM: RollConvention =
    RollConvention.of(StandardRollConventions.EOM.getName())
  /**
   * The 'IMM' roll convention which adjusts the date to the third Wednesday.
   *
   * The input date will be adjusted ensure it is the third Wednesday of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val IMM: RollConvention =
    RollConvention.of(StandardRollConventions.IMM.getName())
  /**
   * The 'IMMCAD' roll convention which adjusts the date two days before the third Wednesday.
   *
   * The input date will be adjusted ensure it is two GBLO business days before the third Wednesday of the month.
   * The date is further adjusted earlier by a combination of the CATO and CAMO calendars.
   * Standard reference data is used to resolve the holiday calendars.
   * (Note that all current GBLO, CATO and CAMO holiday dates will not impact the result.)
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val IMMCAD: RollConvention =
    RollConvention.of(StandardRollConventions.IMMCAD.getName())
  /**
   * The 'IMMAUD' roll convention which adjusts the date to the Thursday before the second Friday.
   *
   * The input date will be adjusted ensure it is the Thursday before the second Friday of the month.
   * Standard reference data is used to resolve the AUSY holiday calendar to subtract the day.
   * (Note that all current AUSY holiday dates will not impact the result.)
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val IMMAUD: RollConvention =
    RollConvention.of(StandardRollConventions.IMMAUD.getName())
  /**
   * The 'IMMNZD' roll convention which adjusts the date to the first Wednesday
   * on or after the ninth day of the month.
   *
   * The input date will be adjusted to the ninth day of the month, and then it will
   * be adjusted to be a Wednesday. If the ninth is a Wednesday, then that is returned.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val IMMNZD: RollConvention =
    RollConvention.of(StandardRollConventions.IMMNZD.getName())
  /**
   * The 'SFE' roll convention which adjusts the date to the second Friday.
   *
   * The input date will be adjusted ensure it is the second Friday of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val SFE: RollConvention =
    RollConvention.of(StandardRollConventions.SFE.getName())
  /**
   * The 'TBILL' roll convention which adjusts the date to next Monday.
   *
   * The input date will be adjusted ensure it is the next Monday.
   * The USNY holiday calendar is used in case the Monday is a holiday.
   * Standard reference data is used to resolve the holiday calendar.
   */
  @static final val TBILL: RollConvention =
    RollConvention.of(StandardRollConventions.TBILL.getName())
  /**
   * The 'Day1' roll convention which adjusts the date to day-of-month 1.
   *
   * The input date will be adjusted ensure it is the 1st day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_1: RollConvention =
    Dom.of(1)
  /**
   * The 'Day2' roll convention which adjusts the date to day-of-month 2.
   *
   * The input date will be adjusted ensure it is the 2nd day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_2: RollConvention =
    Dom.of(2)
  /**
   * The 'Day3' roll convention which adjusts the date to day-of-month 3.
   *
   * The input date will be adjusted ensure it is the 3rd day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_3: RollConvention =
    Dom.of(3)
  /**
   * The 'Day4' roll convention which adjusts the date to day-of-month 4.
   *
   * The input date will be adjusted ensure it is the 4th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_4: RollConvention =
    Dom.of(4)
  /**
   * The 'Day5' roll convention which adjusts the date to day-of-month 5.
   *
   * The input date will be adjusted ensure it is the 5th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_5: RollConvention =
    Dom.of(5)
  /**
   * The 'Day6' roll convention which adjusts the date to day-of-month 6.
   *
   * The input date will be adjusted ensure it is the 6th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_6: RollConvention =
    Dom.of(6)
  /**
   * The 'Day7' roll convention which adjusts the date to day-of-month 7.
   *
   * The input date will be adjusted ensure it is the 7th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_7: RollConvention =
    Dom.of(7)
  /**
   * The 'Day8' roll convention which adjusts the date to day-of-month 8.
   *
   * The input date will be adjusted ensure it is the 8th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_8: RollConvention =
    Dom.of(8)
  /**
   * The 'Day9' roll convention which adjusts the date to day-of-month 9.
   *
   * The input date will be adjusted ensure it is the 9th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_9: RollConvention =
    Dom.of(9)
  /**
   * The 'Day10' roll convention which adjusts the date to day-of-month 10.
   *
   * The input date will be adjusted ensure it is the 10th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_10: RollConvention =
    Dom.of(10)
  /**
   * The 'Day11' roll convention which adjusts the date to day-of-month 11.
   *
   * The input date will be adjusted ensure it is the 11th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_11: RollConvention =
    Dom.of(11)
  /**
   * The 'Day12' roll convention which adjusts the date to day-of-month 12.
   *
   * The input date will be adjusted ensure it is the 12th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_12: RollConvention =
    Dom.of(12)
  /**
   * The 'Day13' roll convention which adjusts the date to day-of-month 13
   *
   * The input date will be adjusted ensure it is the 13th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_13: RollConvention =
    Dom.of(13)
  /**
   * The 'Day14' roll convention which adjusts the date to day-of-month 14.
   *
   * The input date will be adjusted ensure it is the 14th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_14: RollConvention =
    Dom.of(14)
  /**
   * The 'Day15' roll convention which adjusts the date to day-of-month 15.
   *
   * The input date will be adjusted ensure it is the 15th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_15: RollConvention =
    Dom.of(15)
  /**
   * The 'Day16' roll convention which adjusts the date to day-of-month 16.
   *
   * The input date will be adjusted ensure it is the 16th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_16: RollConvention =
    Dom.of(16)
  /**
   * The 'Day17' roll convention which adjusts the date to day-of-month 17.
   *
   * The input date will be adjusted ensure it is the 17th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_17: RollConvention =
    Dom.of(17)
  /**
   * The 'Day18' roll convention which adjusts the date to day-of-month 18.
   *
   * The input date will be adjusted ensure it is the 18th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_18: RollConvention =
    Dom.of(18)
  /**
   * The 'Day19' roll convention which adjusts the date to day-of-month 19.
   *
   * The input date will be adjusted ensure it is the 19th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_19: RollConvention =
    Dom.of(19)
  /**
   * The 'Day20' roll convention which adjusts the date to day-of-month 20.
   *
   * The input date will be adjusted ensure it is the 20th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_20: RollConvention =
    Dom.of(20)
  /**
   * The 'Day21' roll convention which adjusts the date to day-of-month 21.
   *
   * The input date will be adjusted ensure it is the 21st day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_21: RollConvention =
    Dom.of(21)
  /**
   * The 'Day22' roll convention which adjusts the date to day-of-month 22.
   *
   * The input date will be adjusted ensure it is the 22nd day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_22: RollConvention =
    Dom.of(22)
  /**
   * The 'Day23' roll convention which adjusts the date to day-of-month 23.
   *
   * The input date will be adjusted ensure it is the 23rd day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_23: RollConvention =
    Dom.of(23)
  /**
   * The 'Day24' roll convention which adjusts the date to day-of-month 24.
   *
   * The input date will be adjusted ensure it is the 24th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_24: RollConvention =
    Dom.of(24)
  /**
   * The 'Day25' roll convention which adjusts the date to day-of-month 25.
   *
   * The input date will be adjusted ensure it is the 25th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_25: RollConvention =
    Dom.of(25)
  /**
   * The 'Day26' roll convention which adjusts the date to day-of-month 26.
   *
   * The input date will be adjusted ensure it is the 26th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_26: RollConvention =
    Dom.of(26)
  /**
   * The 'Day27' roll convention which adjusts the date to day-of-month 27.
   *
   * The input date will be adjusted ensure it is the 27th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_27: RollConvention =
    Dom.of(27)
  /**
   * The 'Day28' roll convention which adjusts the date to day-of-month 28.
   *
   * The input date will be adjusted ensure it is the 28th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_28: RollConvention =
    Dom.of(28)
  /**
   * The 'Day29' roll convention which adjusts the date to day-of-month 29.
   *
   * The input date will be adjusted ensure it is the 29th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_29: RollConvention =
    Dom.of(29)
  /**
   * The 'Day30' roll convention which adjusts the date to day-of-month 30.
   *
   * The input date will be adjusted ensure it is the 30th day of the month.
   * The year and month of the result date will be the same as the input date.
   *
   * This convention is intended for use with periods that are a multiple of months.
   */
  @static final val DAY_30: RollConvention =
    Dom.of(30)
  /**
   * The 'DayMon' roll convention which adjusts the date to be Monday.
   *
   * The input date will be adjusted ensure it is a Monday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_MON: RollConvention =
    Dow.of(DayOfWeek.MONDAY)
  /**
   * The 'DayTue' roll convention which adjusts the date to be Tuesday.
   *
   * The input date will be adjusted ensure it is a Tuesday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_TUE: RollConvention =
    Dow.of(DayOfWeek.TUESDAY)
  /**
   * The 'DayWed' roll convention which adjusts the date to be Wednesday.
   *
   * The input date will be adjusted ensure it is a Wednesday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_WED: RollConvention =
    Dow.of(DayOfWeek.WEDNESDAY)
  /**
   * The 'DayThu' roll convention which adjusts the date to be Thursday.
   *
   * The input date will be adjusted ensure it is a Thursday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_THU: RollConvention =
    Dow.of(DayOfWeek.THURSDAY)
  /**
   * The 'DayFri' roll convention which adjusts the date to be Friday.
   *
   * The input date will be adjusted ensure it is a Friday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_FRI: RollConvention =
    Dow.of(DayOfWeek.FRIDAY)
  /**
   * The 'DaySat' roll convention which adjusts the date to be Saturday.
   *
   * The input date will be adjusted ensure it is a Saturday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_SAT: RollConvention =
    Dow.of(DayOfWeek.SATURDAY)
  /**
   * The 'DaySun' roll convention which adjusts the date to be Sunday.
   *
   * The input date will be adjusted ensure it is a Sunday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  @static final val DAY_SUN: RollConvention =
    Dow.of(DayOfWeek.SUNDAY)

  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config
  // ENUM_LOOKUP lives on the module instance (not @static) so that it is not a public static final field
  // of RollConventions, and Scala requires non-static fields to follow all @static fields
  /**
   * The extended enum lookup from name to instance.
   */
  private[schedule] val ENUM_LOOKUP: ExtendedEnum[RollConvention] =
    ExtendedEnum.of(classOf[RollConvention])

}
