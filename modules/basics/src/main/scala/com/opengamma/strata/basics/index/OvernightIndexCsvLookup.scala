/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.logging.Level
import java.util.logging.Logger

import scala.annotation.static

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.collect.io.CsvFile
import com.opengamma.strata.collect.io.CsvRow
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads standard Overnight Index implementations from CSV.
 *
 * See `OvernightIndices` for the description of each.
 */
private[index] final class OvernightIndexCsvLookup private ()
    extends NamedLookup[OvernightIndex] {

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, OvernightIndex] = {
    OvernightIndexCsvLookup.BY_NAME
  }

}

private[index] object OvernightIndexCsvLookup {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[OvernightIndexCsvLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: OvernightIndexCsvLookup = new OvernightIndexCsvLookup()

  // CSV column headers
  @static private final val NAME_FIELD = "Name"
  @static private final val CURRENCY_FIELD = "Currency"
  @static private final val ACTIVE_FIELD = "Active"
  @static private final val DAY_COUNT_FIELD = "Day Count"
  @static private final val FIXING_CALENDAR_FIELD = "Fixing Calendar"
  @static private final val PUBLICATION_DAYS_FIELD = "Publication Offset Days"
  @static private final val EFFECTIVE_DAYS_FIELD = "Effective Offset Days"
  @static private final val FIXED_LEG_DAY_COUNT = "Fixed Leg Day Count"

  /**
   * The cache by name.
   */
  @static private val BY_NAME: ImmutableMap[String, OvernightIndex] = loadFromCsv()

  @static private def loadFromCsv(): ImmutableMap[String, OvernightIndex] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources("OvernightIndexData.csv")
    val map: Map[String, OvernightIndex] = new HashMap[String, OvernightIndex]()
    // files are ordered lowest priority to highest, thus Map::put is used
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val csv = CsvFile.of(resource.getCharSource(), true)
        val rowIt = csv.rows().iterator()
        while (rowIt.hasNext()) {
          val row: CsvRow = rowIt.next()
          val parsed = parseOvernightIndex(row)
          map.put(parsed.getName(), parsed)
          map.put(parsed.getName().toUpperCase(Locale.ENGLISH), parsed)
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as Overnight Index CSV file: " + resource, ex)
          return ImmutableMap.of[String, OvernightIndex]()
      }
    }
    ImmutableMap.copyOf[String, OvernightIndex](map)
  }

  @static private def parseOvernightIndex(row: CsvRow): OvernightIndex = {
    val name = row.getValue(NAME_FIELD)
    val currency = Currency.parse(row.getValue(CURRENCY_FIELD))
    val active = java.lang.Boolean.parseBoolean(row.getValue(ACTIVE_FIELD))
    val dayCount = DayCount.of(row.getValue(DAY_COUNT_FIELD))
    val fixingCal = HolidayCalendarId.of(row.getValue(FIXING_CALENDAR_FIELD))
    val publicationDays = Integer.parseInt(row.getValue(PUBLICATION_DAYS_FIELD))
    val effectiveDays = Integer.parseInt(row.getValue(EFFECTIVE_DAYS_FIELD))
    val fixedLegDayCount = DayCount.of(row.getValue(FIXED_LEG_DAY_COUNT))
    // build result
    ImmutableOvernightIndex.builder()
        .name(name)
        .currency(currency)
        .active(active)
        .dayCount(dayCount)
        .fixingCalendar(fixingCal)
        .publicationDateOffset(publicationDays)
        .effectiveDateOffset(effectiveDays)
        .defaultFixedLegDayCount(fixedLegDayCount)
        .build()
  }

}
