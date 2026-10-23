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
import com.opengamma.strata.basics.currency.CurrencyPair
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.collect.io.CsvFile
import com.opengamma.strata.collect.io.CsvRow
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads standard FX Index implementations from CSV.
 *
 * See `FxIndices` for the description of each.
 */
private[index] final class FxIndexCsvLookup private ()
    extends NamedLookup[FxIndex] {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, FxIndex] = {
    FxIndexCsvLookup.BY_NAME
  }

}

private[index] object FxIndexCsvLookup {

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[FxIndexCsvLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: FxIndexCsvLookup = new FxIndexCsvLookup()

  // CSV column headers
  @static private final val NAME_FIELD = "Name"
  @static private final val BASE_CURRENCY_FIELD = "Base Currency"
  @static private final val COUNTER_CURRENCY_FIELD = "Counter Currency"
  @static private final val FIXING_CALENDAR_FIELD = "Fixing Calendar"
  @static private final val MATURITY_DAYS_FIELD = "Maturity Days"
  @static private final val MATURITY_CALENDAR_FIELD = "Maturity Calendar"

  /**
   * The cache by name.
   */
  @static private val BY_NAME: ImmutableMap[String, FxIndex] = loadFromCsv()

  //-------------------------------------------------------------------------
  private def loadFromCsv(): ImmutableMap[String, FxIndex] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources("FxIndexData.csv")
    val map: Map[String, FxIndex] = new HashMap[String, FxIndex]()
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val csv = CsvFile.of(resource.getCharSource(), true)
        val rowIt = csv.rows().iterator()
        while (rowIt.hasNext()) {
          val row = rowIt.next()
          val parsed = parseFxIndex(row)
          map.put(parsed.getName(), parsed)
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed)
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as FX Index CSV file: " + resource, ex)
          return ImmutableMap.of[String, FxIndex]()
      }
    }
    ImmutableMap.copyOf(map)
  }

  private def parseFxIndex(row: CsvRow): FxIndex = {
    val name = row.getField(NAME_FIELD)
    val baseCurrency = Currency.parse(row.getField(BASE_CURRENCY_FIELD))
    val counterCurrency = Currency.parse(row.getField(COUNTER_CURRENCY_FIELD))
    val fixingCal = HolidayCalendarId.of(row.getField(FIXING_CALENDAR_FIELD))
    val maturityDays = Integer.parseInt(row.getField(MATURITY_DAYS_FIELD))
    val maturityCal = HolidayCalendarId.of(row.getField(MATURITY_CALENDAR_FIELD))
    // build result
    ImmutableFxIndex.builder()
        .name(name)
        .currencyPair(CurrencyPair.of(baseCurrency, counterCurrency))
        .fixingCalendar(fixingCal)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(maturityDays, maturityCal))
        .build()
  }

}
