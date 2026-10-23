/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.logging.Level
import java.util.logging.Logger

import scala.annotation.static

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.BusinessDayAdjustment
import com.opengamma.strata.basics.date.BusinessDayConvention
import com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING
import com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING
import com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.PeriodAdditionConvention
import com.opengamma.strata.basics.date.PeriodAdditionConventions
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.basics.date.TenorAdjustment
import com.opengamma.strata.collect.io.CsvFile
import com.opengamma.strata.collect.io.CsvRow
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads standard Ibor Index implementations from CSV.
 *
 * See `IborIndices` for the description of each.
 */
private[index] final class IborIndexCsvLookup private ()
    extends NamedLookup[IborIndex] {

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, IborIndex] = {
    IborIndexCsvLookup.BY_NAME
  }

}

private[index] object IborIndexCsvLookup {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
  // LIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // different rules for overnight
  // EURIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // EURIBOR - http://www.emmi-benchmarks.eu/assets/files/Euribor_code_conduct.pdf
  // TIBOR - http://www.jbatibor.or.jp/english/public/

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[IborIndexCsvLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: IborIndexCsvLookup = new IborIndexCsvLookup()

  // CSV column headers
  @static private final val NAME_FIELD = "Name"
  @static private final val CURRENCY_FIELD = "Currency"
  @static private final val ACTIVE_FIELD = "Active"
  @static private final val DAY_COUNT_FIELD = "Day Count"
  @static private final val FIXING_CALENDAR_FIELD = "Fixing Calendar"
  @static private final val OFFSET_DAYS_FIELD = "Offset Days"
  @static private final val OFFSET_CALENDAR_FIELD = "Offset Calendar"
  @static private final val EFFECTIVE_DATE_CALENDAR_FIELD = "Effective Date Calendar"
  @static private final val TENOR_FIELD = "Tenor"
  @static private final val TENOR_CONVENTION_FIELD = "Tenor Convention"
  @static private final val FIXING_TIME_FIELD = "FixingTime"
  @static private final val FIXING_ZONE_FIELD = "FixingZone"
  @static private final val FIXED_LEG_DAY_COUNT = "Fixed Leg Day Count"

  /**
   * The time formatter.
   */
  @static private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH[:mm]", Locale.ENGLISH)
  /**
   * The cache by name.
   */
  @static private val BY_NAME: ImmutableMap[String, IborIndex] = loadFromCsv()

  @static private def loadFromCsv(): ImmutableMap[String, IborIndex] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources("IborIndexData.csv")
    val map: Map[String, IborIndex] = new HashMap[String, IborIndex]()
    // files are ordered lowest priority to highest, thus Map::put is used
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val csv = CsvFile.of(resource.getCharSource(), true)
        val rowIt = csv.rows().iterator()
        while (rowIt.hasNext()) {
          val row: CsvRow = rowIt.next()
          val parsed = parseIborIndex(row)
          map.put(parsed.getName(), parsed)
          map.put(parsed.getName().toUpperCase(Locale.ENGLISH), parsed)
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as Ibor Index CSV file: " + resource, ex)
          return ImmutableMap.of[String, IborIndex]()
      }
    }
    ImmutableMap.copyOf[String, IborIndex](map)
  }

  @static private def parseIborIndex(row: CsvRow): IborIndex = {
    val name = row.getValue(NAME_FIELD)
    val currency = Currency.parse(row.getValue(CURRENCY_FIELD))
    val active = java.lang.Boolean.parseBoolean(row.getValue(ACTIVE_FIELD))
    val dayCount = DayCount.of(row.getValue(DAY_COUNT_FIELD))
    val fixingCal = HolidayCalendarId.of(row.getValue(FIXING_CALENDAR_FIELD))
    val offsetDays = Integer.parseInt(row.getValue(OFFSET_DAYS_FIELD))
    val offsetCal = HolidayCalendarId.of(row.getValue(OFFSET_CALENDAR_FIELD))
    val effectiveCal = HolidayCalendarId.of(row.getValue(EFFECTIVE_DATE_CALENDAR_FIELD))
    val tenor = Tenor.parse(row.getValue(TENOR_FIELD))
    val time = LocalTime.parse(row.getValue(FIXING_TIME_FIELD), TIME_FORMAT)
    val zoneId = ZoneId.of(row.getValue(FIXING_ZONE_FIELD))
    val fixedLegDayCount = DayCount.of(row.getValue(FIXED_LEG_DAY_COUNT))

    // interpret CSV
    val fixingOffset: DaysAdjustment = DaysAdjustment.ofBusinessDays(
        -offsetDays, offsetCal, BusinessDayAdjustment.of(PRECEDING, fixingCal)).normalized()
    val effectiveOffset: DaysAdjustment = DaysAdjustment.ofBusinessDays(
        offsetDays, offsetCal, BusinessDayAdjustment.of(FOLLOWING, effectiveCal)).normalized()

    // convention can be two different things
    val periodAdditionConvention: PeriodAdditionConvention =
      PeriodAdditionConvention.extendedEnum().find(row.getField(TENOR_CONVENTION_FIELD))
          .orElse(PeriodAdditionConventions.NONE)
    val tenorBusinessConvention: BusinessDayConvention =
      BusinessDayConvention.extendedEnum().find(row.getField(TENOR_CONVENTION_FIELD))
          .orElse(if (isEndOfMonth(periodAdditionConvention)) MODIFIED_FOLLOWING else FOLLOWING)
    val adj = BusinessDayAdjustment.of(tenorBusinessConvention, effectiveCal)
    val tenorAdjustment = TenorAdjustment.of(tenor, periodAdditionConvention, adj)

    // build result
    ImmutableIborIndex.builder()
        .name(name)
        .currency(currency)
        .active(active)
        .dayCount(dayCount)
        .fixingCalendar(fixingCal)
        .fixingDateOffset(fixingOffset)
        .effectiveDateOffset(effectiveOffset)
        .maturityDateOffset(tenorAdjustment)
        .fixingTime(time)
        .fixingZone(zoneId)
        .defaultFixedLegDayCount(fixedLegDayCount)
        .build()
  }

  @static private def isEndOfMonth(tenorConvention: PeriodAdditionConvention): Boolean = {
    tenorConvention.equals(PeriodAdditionConventions.LAST_BUSINESS_DAY) ||
        tenorConvention.equals(PeriodAdditionConventions.LAST_DAY)
  }

}
