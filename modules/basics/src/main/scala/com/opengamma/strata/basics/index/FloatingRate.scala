/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.util.Optional
import java.util.function.Supplier

import scala.annotation.static

import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.Named

/**
 * An index or group of indices used to provide floating rates, typically in interest rate swaps.
 *
 * This provides an abstraction above `FloatingRateName` and `FloatingRateIndex`.
 * This allows code to work with a specific index, `IborIndex`, `OvernightIndex` and `PriceIndex`,
 * or with the index group, represented by `FloatingRateName`, using `instanceof`
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait FloatingRate
    extends Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the associated currency.
   *
   * @return the currency
   */
  def getCurrency(): Currency

  /**
   * Gets the associated floating rate name.
   *
   * @return the floating rate name
   */
  def getFloatingRateName(): FloatingRateName

}

object FloatingRate {

  /**
   * Parses a string, handling various different formats.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * @param indexStr  the index string to parse
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @static def parse(indexStr: String): FloatingRate = {
    parseImpl(indexStr)
  }

  /**
   * Parses a string, handling various different formats.
   *
   * This tries a number of ways to parse the input:
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *  - `FloatingRateName#of(String)`
   *
   * @param indexStr  the index string to parse
   * @return the floating rate index, empty if not found
   */
  @static def tryParse(indexStr: String): Optional[FloatingRate] = {
    tryParseImpl(indexStr)
  }

  // lambdas are created outside the static interface methods to avoid illegal interface lambda helpers
  private def parseImpl(indexStr: String): FloatingRate = {
    ArgChecker.notNull(indexStr, "indexStr")
    val failure: Supplier[IllegalArgumentException] =
      () => new IllegalArgumentException("Floating rate index not known: " + indexStr)
    tryParseImpl(indexStr).orElseThrow(failure)
  }

  private def tryParseImpl(indexStr: String): Optional[FloatingRate] = {
    val iborOpt: Optional[IborIndex] = IborIndex.extendedEnum().find(indexStr)
    if (iborOpt.isPresent()) {
      return iborOpt.map[FloatingRate](t => t)
    }
    val overnightOpt: Optional[OvernightIndex] = OvernightIndex.extendedEnum().find(indexStr)
    if (overnightOpt.isPresent()) {
      return overnightOpt.map[FloatingRate](t => t)
    }
    val priceOpt: Optional[PriceIndex] = PriceIndex.extendedEnum().find(indexStr)
    if (priceOpt.isPresent()) {
      return priceOpt.map[FloatingRate](t => t)
    }
    val frnOpt: Optional[FloatingRateName] = FloatingRateName.extendedEnum().find(indexStr)
    if (frnOpt.isPresent()) {
      return frnOpt.map[FloatingRate](t => t)
    }
    Optional.empty[FloatingRate]()
  }

}
