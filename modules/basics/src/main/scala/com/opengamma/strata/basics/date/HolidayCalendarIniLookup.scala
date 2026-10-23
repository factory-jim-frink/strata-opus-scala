/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField.DAY_OF_MONTH
import java.time.temporal.ChronoField.DAY_OF_WEEK
import java.time.temporal.ChronoField.MONTH_OF_YEAR
import java.time.temporal.TemporalQuery
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Optional
import java.util.Set
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger

import scala.annotation.static

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Splitter
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.io.IniFile
import com.opengamma.strata.collect.io.PropertySet
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads holiday calendar implementations from INI files.
 *
 * These will form the standard holiday calendars available in `ReferenceData#standard()`.
 */
final class HolidayCalendarIniLookup private ()
  extends NamedLookup[HolidayCalendar] {

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, HolidayCalendar] = {
    HolidayCalendarIniLookup.BY_NAME
  }

  // finds a default
  private[date] def defaultByCurrency(currency: Currency): HolidayCalendarId = {
    val calId = findDefaultByCurrency(currency)
    val failure: Supplier[IllegalArgumentException] = () => new IllegalArgumentException(
        "No default Holiday Calendar for currency " + currency)
    calId.orElseThrow(failure)
  }

  // try to find a default
  private[date] def findDefaultByCurrency(currency: Currency): Optional[HolidayCalendarId] = {
    Optional.ofNullable(HolidayCalendarIniLookup.BY_CURRENCY.get(currency))
  }

}

object HolidayCalendarIniLookup {

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[HolidayCalendarIniLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: HolidayCalendarIniLookup = new HolidayCalendarIniLookup()

  /**
   * The Weekend key name.
   */
  @static private final val WEEKEND_KEY = "Weekend"
  /**
   * The WorkingDays key name.
   */
  @static private final val WORKING_DAYS_KEY = "WorkingDays"
  /**
   * The lenient day-of-week parser.
   */
  @static private val DOW_PARSER: DateTimeFormatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .appendText(DAY_OF_WEEK)
      .toFormatter(Locale.ENGLISH)
  /**
   * The lenient month-day parser.
   */
  @static private val DAY_MONTH_PARSER: DateTimeFormatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .appendText(MONTH_OF_YEAR)
      .appendOptional(new DateTimeFormatterBuilder().appendLiteral('-').toFormatter(Locale.ENGLISH))
      .appendValue(DAY_OF_MONTH)
      .toFormatter(Locale.ENGLISH)

  /**
   * The holiday calendars by name.
   */
  @static private val BY_NAME: ImmutableMap[String, HolidayCalendar] = loadFromIni("HolidayCalendarData.ini")
  /**
   * The default holiday calendars by currency.
   */
  @static private val BY_CURRENCY: ImmutableMap[Currency, HolidayCalendarId] =
    loadDefaultsFromIni("HolidayCalendarDefaultData.ini")

  //-------------------------------------------------------------------------
  @VisibleForTesting
  private[date] def loadFromIni(filename: String): ImmutableMap[String, HolidayCalendar] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources(filename)
    val map: Map[String, HolidayCalendar] = new HashMap[String, HolidayCalendar]()
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val ini = IniFile.of(resource.getCharSource())
        val sectionIt = ini.sections().iterator()
        while (sectionIt.hasNext()) {
          val sectionName = sectionIt.next()
          val section = ini.section(sectionName)
          val parsed = parseHolidayCalendar(sectionName, section)
          map.put(parsed.getName(), parsed)
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed)
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as Holiday Calendar INI file: " + resource, ex)
          return ImmutableMap.of[String, HolidayCalendar]()
      }
    }
    ImmutableMap.copyOf(map)
  }

  // parses the holiday calendar
  private def parseHolidayCalendar(calendarName: String, section: PropertySet): HolidayCalendar = {
    val weekendStr = section.value(WEEKEND_KEY)
    val weekends = parseWeekends(weekendStr)
    val holidays: List[LocalDate] = new ArrayList[LocalDate]()
    val workingDays: Set[LocalDate] = new HashSet[LocalDate]()
    val keyIt = section.keys().iterator()
    while (keyIt.hasNext()) {
      val key = keyIt.next()
      if (!key.equals(WEEKEND_KEY)) {
        val value = section.value(key)
        if (key.length() == 4) {
          val year = Integer.parseInt(key)
          holidays.addAll(parseYearDates(year, value))
        } else if (WORKING_DAYS_KEY.equals(key)) {
          workingDays.addAll(parseDates(value))
        } else {
          holidays.add(LocalDate.parse(key))
        }
      }
    }
    // build result
    ImmutableHolidayCalendar.of(HolidayCalendarId.of(calendarName), holidays, weekends, workingDays)
  }

  // parse weekend format, such as 'Sat,Sun'
  private def parseWeekends(str: String): Set[DayOfWeek] = {
    val split: List[String] = Splitter.on(',').splitToList(str)
    val query: TemporalQuery[DayOfWeek] = temporal => DayOfWeek.from(temporal)
    split.stream()
        .map[DayOfWeek](v => DOW_PARSER.parse(v, query))
        .collect(Guavate.toImmutableSet[DayOfWeek]())
  }

  // parse year format, such as 'Jan1,Mar12,Dec25' or '2015-01-01,2015-03-12,2015-12-25'
  private def parseYearDates(year: Int, str: String): List[LocalDate] = {
    val split: List[String] = Splitter.on(',').splitToList(str)
    split.stream()
        .map[LocalDate](v => parseDate(year, v))
        .collect(Guavate.toImmutableList[LocalDate]())
  }

  // parse comma separated date format such as "2015-01-01,2015-03-12"
  private def parseDates(str: String): List[LocalDate] = {
    val split: List[String] = Splitter.on(',').splitToList(str)
    split.stream()
        .map[LocalDate](v => LocalDate.parse(v))
        .collect(Guavate.toImmutableList[LocalDate]())
  }

  private def parseDate(year: Int, str: String): LocalDate = {
    try {
      MonthDay.parse(str, DAY_MONTH_PARSER).atYear(year)
    } catch {
      case ex: DateTimeParseException =>
        val date = LocalDate.parse(str)
        if (date.getYear() != year) {
          throw new IllegalArgumentException("Parsed date had incorrect year: " + str + ", but expected: " + year)
        }
        date
    }
  }

  //-------------------------------------------------------------------------
  @VisibleForTesting
  private[date] def loadDefaultsFromIni(filename: String): ImmutableMap[Currency, HolidayCalendarId] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources(filename)
    val map: Map[Currency, HolidayCalendarId] = new HashMap[Currency, HolidayCalendarId]()
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val ini = IniFile.of(resource.getCharSource())
        val section = ini.section("defaultByCurrency")
        val keyIt = section.keys().iterator()
        while (keyIt.hasNext()) {
          val currencyCode = keyIt.next()
          map.put(Currency.of(currencyCode), HolidayCalendarId.of(section.value(currencyCode)))
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as Holiday Calendar Defaults INI file: " + resource, ex)
          return ImmutableMap.of[Currency, HolidayCalendarId]()
      }
    }
    ImmutableMap.copyOf(map)
  }

}
