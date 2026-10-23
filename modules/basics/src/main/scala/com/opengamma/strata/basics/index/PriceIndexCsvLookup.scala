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
import com.opengamma.strata.basics.location.Country
import com.opengamma.strata.basics.schedule.Frequency
import com.opengamma.strata.collect.io.CsvFile
import com.opengamma.strata.collect.io.CsvRow
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads standard Price Index implementations from CSV.
 *
 * See `PriceIndices` for the description of each.
 */
private[index] final class PriceIndexCsvLookup private ()
    extends NamedLookup[PriceIndex] {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, PriceIndex] = {
    PriceIndexCsvLookup.BY_NAME
  }

}

private[index] object PriceIndexCsvLookup {

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[PriceIndexCsvLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: PriceIndexCsvLookup = new PriceIndexCsvLookup()

  // CSV column headers
  @static private final val NAME_FIELD = "Name"
  @static private final val CURRENCY_FIELD = "Currency"
  @static private final val COUNTRY_FIELD = "Country"
  @static private final val ACTIVE_FIELD = "Active"
  @static private final val PUBLICATION_FREQUENCY_FIELD = "Publication Frequency"

  /**
   * The cache by name.
   */
  @static private val BY_NAME: ImmutableMap[String, PriceIndex] = loadFromCsv()

  //-------------------------------------------------------------------------
  private def loadFromCsv(): ImmutableMap[String, PriceIndex] = {
    val resources: List[ResourceLocator] = ResourceConfig.orderedResources("PriceIndexData.csv")
    val map: Map[String, PriceIndex] = new HashMap[String, PriceIndex]()
    // files are ordered lowest priority to highest, thus Map::put is used
    val resourceIt = resources.iterator()
    while (resourceIt.hasNext()) {
      val resource = resourceIt.next()
      try {
        val csv = CsvFile.of(resource.getCharSource(), true)
        val rowIt = csv.rows().iterator()
        while (rowIt.hasNext()) {
          val row = rowIt.next()
          val parsed = parsePriceIndex(row)
          map.put(parsed.getName(), parsed)
          map.put(parsed.getName().toUpperCase(Locale.ENGLISH), parsed)
        }
      } catch {
        case ex: RuntimeException =>
          log.log(Level.SEVERE, "Error processing resource as Price Index CSV file: " + resource, ex)
          return ImmutableMap.of[String, PriceIndex]()
      }
    }
    ImmutableMap.copyOf(map)
  }

  private def parsePriceIndex(row: CsvRow): PriceIndex = {
    val name = row.getField(NAME_FIELD)
    val currency = Currency.parse(row.getField(CURRENCY_FIELD))
    val region = Country.of(row.getField(COUNTRY_FIELD))
    val active = java.lang.Boolean.parseBoolean(row.getField(ACTIVE_FIELD))
    val frequency = Frequency.parse(row.getField(PUBLICATION_FREQUENCY_FIELD))
    // build result
    ImmutablePriceIndex.builder()
        .name(name)
        .currency(currency)
        .region(region)
        .active(active)
        .publicationFrequency(frequency)
        .build()
  }

}
