/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.util.Arrays
import java.util.List
import java.util.Map.Entry
import java.util.logging.Logger

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.io.IniFile
import com.opengamma.strata.collect.io.PropertySet
import com.opengamma.strata.collect.io.ResourceConfig

/**
 * Internal loader of currency and currency pair data.
 *
 * This loads configuration files for `Currency` and `CurrencyPair`.
 */
private[currency] final class CurrencyDataLoader private ()

private[currency] object CurrencyDataLoader {

  /**
   * The logger.
   */
  private val log: Logger = Logger.getLogger(classOf[CurrencyDataLoader].getName())
  /**
   * INI file for currency data.
   */
  private final val CURRENCY_INI = "Currency.ini"
  /**
   * INI file for currency pair data.
   */
  private final val PAIR_INI = "CurrencyPair.ini"
  /**
   * INI file containing a list of general currency data.
   * This in includes a list of currencies in priority order used to choose the base currency of the market
   * convention pair for pairs that aren't configured in currency-pair.ini.
   */
  private final val CURRENCY_DATA_INI = "CurrencyData.ini"

  //-------------------------------------------------------------------------
  /**
   * Loads the available currencies.
   *
   * @param loadHistoric  whether to load the historic or active currencies
   * @return the map of known currencies
   */
  def loadCurrencies(loadHistoric: Boolean): ImmutableMap[String, Currency] = {
    try {
      val ini = ResourceConfig.combinedIniFile(ResourceConfig.orderedResources(CURRENCY_INI))
      parseCurrencies(ini, loadHistoric)

    } catch {
      case ex: RuntimeException =>
        // logging used because this is loaded in a static variable
        log.severe(Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
        ImmutableMap.of[String, Currency]()
    }
  }

  // parse currency info
  private def parseCurrencies(ini: IniFile, loadHistoric: Boolean): ImmutableMap[String, Currency] = {
    val builder = ImmutableMap.builder[String, Currency]()
    val it = ini.asMap().entrySet().iterator()
    while (it.hasNext()) {
      val entry: Entry[String, PropertySet] = it.next()
      val currencyCode = entry.getKey()
      if (currencyCode.length() == 3 && Currency.CODE_MATCHER.matchesAllOf(currencyCode)) {
        val properties = entry.getValue()
        val isHistoric =
          (properties.keys().contains("historic") && java.lang.Boolean.parseBoolean(properties.value("historic")))
        if (isHistoric == loadHistoric) {
          val minorUnits = Integer.parseInt(properties.value("minorUnitDigits"))
          val triangulationCurrency = properties.value("triangulationCurrency")
          builder.put(currencyCode, new Currency(currencyCode, minorUnits, triangulationCurrency))
        }
      }
    }
    builder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available currency pairs.
   *
   * @return the map of known currency pairs, where the value is the number of digits in the rate
   */
  def loadPairs(): ImmutableMap[CurrencyPair, Integer] = {
    try {
      val ini = ResourceConfig.combinedIniFile(ResourceConfig.orderedResources(PAIR_INI))
      parsePairs(ini)

    } catch {
      case ex: RuntimeException =>
        // logging used because this is loaded in a static variable
        log.severe(Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
        ImmutableMap.of[CurrencyPair, Integer]()
    }
  }

  // parse pair info
  private def parsePairs(ini: IniFile): ImmutableMap[CurrencyPair, Integer] = {
    val builder = ImmutableMap.builder[CurrencyPair, Integer]()
    val it = ini.asMap().entrySet().iterator()
    while (it.hasNext()) {
      val entry: Entry[String, PropertySet] = it.next()
      val pairStr = entry.getKey()
      if (CurrencyPair.REGEX_FORMAT.matcher(pairStr).matches()) {
        val pair = CurrencyPair.parse(pairStr)
        val properties = entry.getValue()
        val rateDigits: Integer = Integer.valueOf(Integer.parseInt(properties.value("rateDigits")))
        builder.put(pair, rateDigits)
      }
    }
    builder.build()
  }

  /**
   * Loads the priority order of currencies, used to determine the base currency of the market convention pair
   * for pairs that aren't explicitly configured.
   *
   * @return a map of currency to order
   */
  def loadOrdering(): ImmutableMap[Currency, Integer] = {
    try {
      val ini = ResourceConfig.combinedIniFile(ResourceConfig.orderedResources(CURRENCY_DATA_INI))
      val section = ini.section("marketConventionPriority")
      val list = section.value("ordering")
      // The currency ordering is defined as a comma-separated list
      val currencies: List[Currency] = Arrays.stream(list.split(","))
          .map[String](str => str.trim())
          .map[Currency](str => Currency.of(str))
          .collect(Guavate.toImmutableList[Currency]())

      val orderBuilder = ImmutableMap.builder[Currency, Integer]()

      var i = 0
      while (i < currencies.size()) {
        orderBuilder.put(currencies.get(i), Integer.valueOf(i + 1))
        i += 1
      }
      orderBuilder.build()
    } catch {
      case ex: Exception =>
        // logging used because this is loaded in a static variable
        log.severe(Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
        ImmutableMap.of[Currency, Integer]()
    }
  }
}
