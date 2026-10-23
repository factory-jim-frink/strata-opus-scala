/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import scala.annotation.static

/**
 * Identifiers for common holiday calendars.
 *
 * The constants defined here are identifiers, used to locate instances of
 * `HolidayCalendar` from `ReferenceData`.
 *
 * All the constants defined here will be available from `ReferenceData#standard()`.
 * The associated holiday data may or may not be sufficient for your production needs.
 *
 * The standard holiday data was obtained by direct research - it was not derived from a vendor
 * of holiday calendar data. Two approaches are available to add or change the data.
 * Firstly, applications can provide their own `ReferenceData` implementation, mapping
 * the identifier to any data desired. Secondly, the standard data can be amended by following
 * the instructions in `HolidayCalendar.ini`.
 */
final class HolidayCalendarIds private ()

object HolidayCalendarIds {

  /**
   * An identifier for a calendar declaring no holidays and no weekends, with code 'NoHolidays'.
   *
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  @static final val NO_HOLIDAYS: HolidayCalendarId = HolidayCalendarId.of("NoHolidays")
  /**
   * An identifier for a calendar declaring all days as business days
   * except Saturday/Sunday weekends, with code 'SatSun'.
   *
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  @static final val SAT_SUN: HolidayCalendarId = HolidayCalendarId.of("Sat/Sun")
  /**
   * An identifier for a calendar declaring all days as business days
   * except Friday/Saturday weekends, with code 'FriSat'.
   *
   * This calendar is mostly useful in testing scenarios.
   */
  @static final val FRI_SAT: HolidayCalendarId = HolidayCalendarId.of("Fri/Sat")
  /**
   * An identifier for a calendar declaring all days as business days
   * except Thursday/Friday weekends, with code 'ThuFri'.
   *
   * This calendar is mostly useful in testing scenarios.
   */
  @static final val THU_FRI: HolidayCalendarId = HolidayCalendarId.of("Thu/Fri")

  /**
   * An identifier for the holiday calendar of London, United Kingdom, with code 'GBLO'.
   *
   * This constant references the calendar for London bank holidays.
   */
  @static final val GBLO: HolidayCalendarId = HolidayCalendarId.of("GBLO")
  /**
   * An identifier for the holiday calendar of Paris, France, with code 'FRPA'.
   *
   * This constant references the calendar for Paris public holidays.
   */
  @static final val FRPA: HolidayCalendarId = HolidayCalendarId.of("FRPA")
  /**
   * An identifier for the holiday calendar of Frankfurt, Germany, with code 'DEFR'.
   *
   * This constant references the calendar for Frankfurt public holidays.
   */
  @static final val DEFR: HolidayCalendarId = HolidayCalendarId.of("DEFR")
  /**
   * An identifier for the holiday calendar of Zurich, Switzerland, with code 'EUTA'.
   *
   * This constant references the calendar for Zurich public holidays.
   */
  @static final val CHZU: HolidayCalendarId = HolidayCalendarId.of("CHZU")
  /**
   * An identifier for the holiday calendar of the European Union TARGET system, with code 'EUTA'.
   *
   * This constant references the calendar for the TARGET interbank payment system holidays.
   *
   * Referenced by the 2006 ISDA definitions 1.8.
   */
  @static final val EUTA: HolidayCalendarId = HolidayCalendarId.of("EUTA")
  /**
   * An identifier for the holiday calendar of United States Government Securities, with code 'USGS'.
   *
   * This constant references the calendar for United States Government Securities as per SIFMA.
   *
   * Referenced by the 2006 ISDA definitions 1.11.
   */
  @static final val USGS: HolidayCalendarId = HolidayCalendarId.of("USGS")
  /**
   * An identifier for the holiday calendar of New York, United States, with code 'USNY'.
   *
   * This constant references the calendar for New York holidays.
   */
  @static final val USNY: HolidayCalendarId = HolidayCalendarId.of("USNY")
  /**
   * An identifier for the holiday calendar of the Federal Reserve Bank of New York, with code 'NYFD'.
   *
   * This constant references the calendar for the Federal Reserve Bank of New York holidays.
   *
   * Referenced by the 2006 ISDA definitions 1.9.
   */
  @static final val NYFD: HolidayCalendarId = HolidayCalendarId.of("NYFD")
  /**
   * An identifier for the holiday calendar of the New York Stock Exchange, with code 'NYSE'.
   *
   * This constant references the calendar for the New York Stock Exchange.
   *
   * Referenced by the 2006 ISDA definitions 1.10.
   */
  @static final val NYSE: HolidayCalendarId = HolidayCalendarId.of("NYSE")
  /**
   * An identifier for the holiday calendar of Tokyo, Japan, with code 'JPTO'.
   *
   * This constant references the calendar for Tokyo bank holidays.
   */
  @static final val JPTO: HolidayCalendarId = HolidayCalendarId.of("JPTO")

  /**
   * An identifier for the holiday calendar of Sydney, Australia, with code 'AUSY'.
   *
   * This constant references the calendar for Sydney bank holidays.
   */
  @static final val AUSY: HolidayCalendarId = HolidayCalendarId.of("AUSY")
  /**
   * An identifier for the holiday calendar of Brazil, with code 'BRBD'.
   *
   * This constant references the combined calendar for Brazil bank holidays.
   * This unites city-level calendars.
   */
  @static final val BRBD: HolidayCalendarId = HolidayCalendarId.of("BRBD")
  /**
   * An identifier for the holiday calendar of Montreal, Canada, with code 'CAMO'.
   *
   * This constant references the calendar for Montreal bank holidays.
   */
  @static final val CAMO: HolidayCalendarId = HolidayCalendarId.of("CAMO")
  /**
   * An identifier for the holiday calendar of Toronto, Canada, with code 'CATO'.
   *
   * This constant references the calendar for Toronto bank holidays.
   */
  @static final val CATO: HolidayCalendarId = HolidayCalendarId.of("CATO")
  /**
   * An identifier for the holiday calendar of Prague, Czech Republic, with code 'CZPR'.
   *
   * This constant references the calendar for Prague bank holidays.
   */
  @static final val CZPR: HolidayCalendarId = HolidayCalendarId.of("CZPR")
  /**
   * An identifier for the holiday calendar of Copenhagen, Denmark, with code 'DKCO'.
   *
   * This constant references the calendar for Copenhagen bank holidays.
   */
  @static final val DKCO: HolidayCalendarId = HolidayCalendarId.of("DKCO")
  /**
   * An identifier for the holiday calendar of Budapest, Hungary, with code 'HUBU'.
   *
   * This constant references the calendar for Budapest bank holidays.
   */
  @static final val HUBU: HolidayCalendarId = HolidayCalendarId.of("HUBU")
  /**
   * An identifier for the holiday calendar of Mexico City, Mexico, with code 'MXMC'.
   *
   * This constant references the calendar for Mexico City bank holidays.
   */
  @static final val MXMC: HolidayCalendarId = HolidayCalendarId.of("MXMC")
  /**
   * An identifier for the holiday calendar of Oslo, Norway, with code 'NOOS'.
   *
   * This constant references the calendar for Oslo bank holidays.
   */
  @static final val NOOS: HolidayCalendarId = HolidayCalendarId.of("NOOS")
  /**
   * An identifier for the holiday calendar of Auckland, New Zealand, with code 'NZAU'.
   *
   * This constant references the calendar for Auckland bank holidays.
   */
  @static final val NZAU: HolidayCalendarId = HolidayCalendarId.of("NZAU")
  /**
   * An identifier for the holiday calendar of Wellington, New Zealand, with code 'NZWE'.
   *
   * This constant references the calendar for Wellington bank holidays.
   */
  @static final val NZWE: HolidayCalendarId = HolidayCalendarId.of("NZWE")
  /**
   * An identifier for the holiday calendar of Warsaw, Poland, with code 'PLWA'.
   *
   * This constant references the calendar for Warsaw bank holidays.
   */
  @static final val PLWA: HolidayCalendarId = HolidayCalendarId.of("PLWA")
  /**
   * An identifier for the holiday calendar of Stockholm, Sweden, with code 'SEST'.
   *
   * This constant references the calendar for Stockholm bank holidays.
   */
  @static final val SEST: HolidayCalendarId = HolidayCalendarId.of("SEST")
  /**
   * An identifier for the holiday calendar of Bangkok, Thailand., with code 'THBA'.
   *
   * This constant references the calendar for Bangkok bank holidays.
   */
  @static final val THBA: HolidayCalendarId = HolidayCalendarId.of("THBA")
  /**
   * An identifier for the holiday calendar of Johannesburg, South Africa, with code 'ZAJO'.
   *
   * This constant references the calendar for Johannesburg bank holidays.
   */
  @static final val ZAJO: HolidayCalendarId = HolidayCalendarId.of("ZAJO")

}
