/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.util.Optional
import java.util.function.Supplier

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.collect.ArgChecker

/**
 * An index used to provide floating rates, typically in interest rate swaps.
 *
 * See `IborIndex`, `OvernightIndex` and  `PriceIndex` for more details.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait FloatingRateIndex
    extends Index with FloatingRate {

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  def getName(): String

  /**
   * Gets the currency of the index.
   *
   * @return the currency of the index
   */
  def getCurrency(): Currency

  /**
   * Gets whether the index is active.
   *
   * Over time some indices become inactive and are no longer produced.
   * If this occurs, this method will return false.
   *
   * @return true if the index is active, false if inactive
   */
  def isActive(): Boolean

  /**
   * Gets the day count convention of the index.
   *
   * @return the day count convention
   */
  def getDayCount(): DayCount

  /**
   * Gets the floating rate name for this index.
   *
   * For an Ibor index, the `FloatingRateName` does not include the tenor.
   * It can be used to find the other tenors available for this index.
   *
   * @return the floating rate name
   */
  def getFloatingRateName(): FloatingRateName

  /**
   * Gets the default day count convention for the associated fixed leg.
   *
   * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
   * The day count convention of the fixed leg often differs from that of the index,
   * and the default is value is available here.
   *
   * @return the day count convention
   */
  def getDefaultFixedLegDayCount(): DayCount = {
    getDayCount()
  }

}

object FloatingRateIndex {

  /**
   * Parses a string, handling different types of index.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * If `FloatingRateName` is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor from `FloatingRateName#getDefaultTenor()` will be used.
   *
   * @param indexStr  the index string to parse
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @static def parse(indexStr: String): FloatingRateIndex = {
    parseImpl(indexStr, null)
  }

  /**
   * Parses a string, handling different types of index, optionally specifying a tenor for Ibor.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * If `FloatingRateName` is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor can optionally be supplied. If needed and missing,
   * the result of `FloatingRateName#getDefaultTenor()` will be used.
   *
   * @param indexStr  the index string to parse
   * @param defaultIborTenor  the tenor to use for Ibor if matched as a `FloatingRateName`, may be null
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @static def parse(indexStr: String, defaultIborTenor: Tenor): FloatingRateIndex = {
    parseImpl(indexStr, defaultIborTenor)
  }

  /**
   * Parses a string, handling different types of index.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * If `FloatingRateName` is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor from `FloatingRateName#getDefaultTenor()` will be used.
   *
   * @param indexStr  the index string to parse
   * @return the floating rate index, empty if not found
   */
  @static def tryParse(indexStr: String): Optional[FloatingRateIndex] = {
    tryParseImpl(indexStr, null)
  }

  /**
   * Parses a string, handling different types of index, optionally specifying a tenor for Ibor.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * If `FloatingRateName` is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor can optionally be supplied. If needed and missing,
   * the result of `FloatingRateName#getDefaultTenor()` will be used.
   *
   * @param indexStr  the index string to parse
   * @param defaultIborTenor  the tenor to use for Ibor if matched as a `FloatingRateName`, may be null
   * @return the floating rate index, empty if not found
   */
  @static def tryParse(indexStr: String, defaultIborTenor: Tenor): Optional[FloatingRateIndex] = {
    tryParseImpl(indexStr, defaultIborTenor)
  }

  /**
   * Obtains an instance from the specified unique name.
   *
   * This parses names from `IborIndex`, `OvernightIndex` and `PriceIndex`.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): FloatingRateIndex = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    Indices.FLOATING_RATE_INDEX_LOOKUP.lookup(uniqueName)
  }

  // lambdas are created outside the static interface methods to avoid illegal interface lambda helpers
  private def parseImpl(indexStr: String, defaultIborTenor: Tenor): FloatingRateIndex = {
    ArgChecker.notNull(indexStr, "indexStr")
    val failure: Supplier[IllegalArgumentException] =
      () => new IllegalArgumentException("Floating rate index not known: " + indexStr)
    tryParseImpl(indexStr, defaultIborTenor).orElseThrow(failure)
  }

  private def tryParseImpl(indexStr: String, defaultIborTenor: Tenor): Optional[FloatingRateIndex] = {
    FloatingRate.tryParse(indexStr)
        .map[FloatingRateIndex](fr => {
          fr match {
            case frName: FloatingRateName =>
              frName.toFloatingRateIndex(if (defaultIborTenor != null) defaultIborTenor else frName.getDefaultTenor())
            case _ =>
              fr.asInstanceOf[FloatingRateIndex]
          }
        })
  }

}
