/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.util.HashMap
import java.util.Locale
import java.util.Map
import java.util.logging.Logger

import scala.annotation.static

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.collect.io.IniFile
import com.opengamma.strata.collect.io.PropertySet
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Loads standard floating rate names from INI.
 */
private[index] final class FloatingRateNameIniLookup private ()
    extends NamedLookup[FloatingRateName] {

  //-------------------------------------------------------------------------
  override def lookupAll(): Map[String, FloatingRateName] = {
    FloatingRateNameIniLookup.Loader.INSTANCE.names
  }

  // finds a default
  private[index] def defaultIborIndex(currency: Currency): FloatingRateName = {
    val frname = FloatingRateNameIniLookup.Loader.INSTANCE.iborDefaults.get(currency)
    if (frname == null) {
      throw new IllegalArgumentException("No default Ibor index for currency " + currency)
    }
    frname
  }

  // finds a default
  private[index] def defaultOvernightIndex(currency: Currency): FloatingRateName = {
    val frname = FloatingRateNameIniLookup.Loader.INSTANCE.overnightDefaults.get(currency)
    if (frname == null) {
      throw new IllegalArgumentException("No default Overnight index for currency " + currency)
    }
    frname
  }

}

private[index] object FloatingRateNameIniLookup {

  /**
   * The logger.
   */
  @static private val log: Logger = Logger.getLogger(classOf[FloatingRateNameIniLookup].getName())
  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: FloatingRateNameIniLookup = new FloatingRateNameIniLookup()

  /**
   * INI file for floating rate names.
   */
  @static private final val FLOATING_RATE_NAME_INI = "FloatingRateNameData.ini"

  //-------------------------------------------------------------------------
  private[index] class Loader private[index] () {

    /** The cache by name. */
    private[index] var names: ImmutableMap[String, FloatingRateName] = ImmutableMap.of[String, FloatingRateName]()
    /** The Ibor defaults by currency. */
    private[index] var iborDefaults: ImmutableMap[Currency, FloatingRateName] =
      ImmutableMap.of[Currency, FloatingRateName]()
    /** The Overnight defaults by currency. */
    private[index] var overnightDefaults: ImmutableMap[Currency, FloatingRateName] =
      ImmutableMap.of[Currency, FloatingRateName]()

    // fields are assigned in order, so a failure part way through keeps the values parsed so far
    try {
      val ini = ResourceConfig.combinedIniFile(FLOATING_RATE_NAME_INI)
      names = Loader.parseIndices(ini)
      iborDefaults = Loader.parseIborDefaults(ini, names)
      overnightDefaults = Loader.parseOvernightDefaults(ini, names)

    } catch {
      case ex: RuntimeException =>
        // logging used because this is loaded in a static variable
        log.severe(Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
    }

  }

  private[index] object Loader {
    /** Instance. */
    private[index] val INSTANCE: Loader = new Loader()

    // parse the config file FloatingRateName.ini
    private def parseIndices(ini: IniFile): ImmutableMap[String, FloatingRateName] = {
      val map = new HashMap[String, ImmutableFloatingRateName]()
      parseSection(ini.section("ibor"), "-", FloatingRateType.IBOR, map)
      parseFixingDateOffset(ini.section("iborFixingDateOffset"), map)
      parseSection(ini.section("overnightCompounded"), "", FloatingRateType.OVERNIGHT_COMPOUNDED, map)
      parseSection(ini.section("overnightAveraged"), "", FloatingRateType.OVERNIGHT_AVERAGED, map)
      parseSection(ini.section("price"), "", FloatingRateType.PRICE, map)
      ImmutableMap.copyOf[String, FloatingRateName](map)
    }

    // parse a single section
    private def parseSection(
        section: PropertySet,
        indexNameSuffix: String,
        `type`: FloatingRateType,
        mutableMap: HashMap[String, ImmutableFloatingRateName]): Unit = {

      // find our names from the RHS of the key/value pairs
      val keyIt = section.keys().iterator()
      while (keyIt.hasNext()) {
        val key = keyIt.next()
        val name = ImmutableFloatingRateName.of(key, section.value(key) + indexNameSuffix, `type`)
        mutableMap.put(key, name)
        mutableMap.putIfAbsent(key.toUpperCase(Locale.ENGLISH), name)
      }
    }

    // parse the fixing date offset section
    private def parseFixingDateOffset(section: PropertySet, mutableMap: HashMap[String, ImmutableFloatingRateName]): Unit = {
      // find our names from the RHS of the key/value pairs
      val keyIt = section.keys().iterator()
      while (keyIt.hasNext()) {
        val key = keyIt.next()
        val days: Integer = Integer.valueOf(Integer.parseInt(section.value(key)))
        val name = mutableMap.get(key.toUpperCase(Locale.ENGLISH))
        val updated = name.toBuilder().fixingDateOffsetDays(days).build()
        mutableMap.put(key.toUpperCase(Locale.ENGLISH), updated)
      }
    }

    //-------------------------------------------------------------------------
    // load currency defaults
    private def parseIborDefaults(
        ini: IniFile,
        names: ImmutableMap[String, FloatingRateName]): ImmutableMap[Currency, FloatingRateName] = {

      val map = ImmutableMap.builder[Currency, FloatingRateName]()
      val section = ini.section("currencyDefaultIbor")
      val keyIt = section.keys().iterator()
      while (keyIt.hasNext()) {
        val key = keyIt.next()
        val frname = names.get(section.value(key))
        if (frname == null) {
          throw new IllegalArgumentException("Invalid default Ibor index for currency " + key)
        }
        map.put(Currency.of(key), frname)
      }
      map.build()
    }

    //-------------------------------------------------------------------------
    // load currency defaults
    private def parseOvernightDefaults(
        ini: IniFile,
        names: ImmutableMap[String, FloatingRateName]): ImmutableMap[Currency, FloatingRateName] = {

      val map = ImmutableMap.builder[Currency, FloatingRateName]()
      val section = ini.section("currencyDefaultOvernight")
      val keyIt = section.keys().iterator()
      while (keyIt.hasNext()) {
        val key = keyIt.next()
        val frname = names.get(section.value(key))
        if (frname == null) {
          throw new IllegalArgumentException("Invalid default Overnight index for currency " + key)
        }
        map.put(Currency.of(key), frname)
      }
      map.build()
    }
  }

}
