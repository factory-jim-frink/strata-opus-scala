/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.util.function.BinaryOperator

import scala.annotation.static

import com.google.common.base.Splitter
import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard holiday calendars.
 *
 * The purpose of each holiday calendar is to define whether a date is a holiday or a business day.
 * The standard holiday calendar data is provided by direct research and is not derived
 * from a vendor of holiday calendar data. The implementation is defined by `HolidayCalendar.ini`,
 * The data may or may not be sufficient for your production needs.
 *
 * Applications should refer to holidays using `HolidayCalendarId`.
 * The identifier must be resolved
 * to a `HolidayCalendar` before holidays can be accessed.
 */
final class HolidayCalendars private ()

object HolidayCalendars {

  //-------------------------------------------------------------------------
  /**
   * An instance declaring no holidays and no weekends.
   *
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  @static final val NO_HOLIDAYS: HolidayCalendar = NoHolidaysCalendar.INSTANCE
  /**
   * An instance declaring all days as business days except Saturday/Sunday weekends.
   *
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  @static final val SAT_SUN: HolidayCalendar = WeekendHolidayCalendar.SAT_SUN
  /**
   * An instance declaring all days as business days except Friday/Saturday weekends.
   *
   * This calendar is mostly useful in testing scenarios.
   */
  @static final val FRI_SAT: HolidayCalendar = WeekendHolidayCalendar.FRI_SAT
  /**
   * An instance declaring all days as business days except Thursday/Friday weekends.
   *
   * This calendar is mostly useful in testing scenarios.
   */
  @static final val THU_FRI: HolidayCalendar = WeekendHolidayCalendar.THU_FRI

  // This constant must be after the constants above in the source file.
  /**
   * The extended enum lookup from name to instance.
   */
  @static private val ENUM_LOOKUP: ExtendedEnum[HolidayCalendar] = ExtendedEnum.of(classOf[HolidayCalendar])

  /**
   * Decorates a `ReferenceData` instance such that all requests for
   * a `HolidayCalendarId` will return a value.
   *
   * If the `HolidayCalendarId` is not found in the underlying reference data,
   * an instance with Saturday/Sunday holidays will be returned.
   *
   * @param underlying  the underlying instance
   * @return the holiday safe reference data
   */
  def defaultingReferenceData(underlying: ReferenceData): ReferenceData = {
    new HolidaySafeReferenceData(underlying)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the set of standard holiday calendars.
   *
   * The unique name identifies the calendar in the ''standard'' source of calendars.
   * The standard source is loaded at startup based on the `HolidayCalendar.ini` file.
   *
   * Applications should generally avoid using this method.
   * Instead, applications should refer to holidays using `HolidayCalendarId`,
   * resolving them using a `ReferenceData`.
   *
   * It is possible to combine two or more calendars using the '+' symbol.
   * For example, 'GBLO+USNY' will combine the separate 'GBLO' and 'USNY' calendars.
   *
   * @param uniqueName  the unique name of the calendar
   * @return the holiday calendar
   */
  def of(uniqueName: String): HolidayCalendar = {
    if (uniqueName.contains("+")) {
      val combiner: BinaryOperator[HolidayCalendar] = (cal1, cal2) => cal1.combinedWith(cal2)
      Splitter.on('+').splitToList(uniqueName).stream()
          .map[HolidayCalendar](name => HolidayCalendars.of(name))
          .reduce(NO_HOLIDAYS, combiner)
    } else {
      ENUM_LOOKUP.lookup(uniqueName)
    }
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the calendar to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  def extendedEnum(): ExtendedEnum[HolidayCalendar] = {
    HolidayCalendars.ENUM_LOOKUP
  }

}
