/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard foreign exchange indices.
 *
 * Each constant returns a standard definition of the specified index.
 */
final class FxIndices private ()

object FxIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The FX index for conversion from EUR to CHF, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  @static final val EUR_CHF_ECB: FxIndex = FxIndex.of("EUR/CHF-ECB")
  /**
   * The FX index for conversion from EUR to GBP, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  @static final val EUR_GBP_ECB: FxIndex = FxIndex.of("EUR/GBP-ECB")
  /**
   * The FX index for conversion from EUR to JPY, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  @static final val EUR_JPY_ECB: FxIndex = FxIndex.of("EUR/JPY-ECB")
  /**
   * The FX index for conversion from EUR to USD, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  @static final val EUR_USD_ECB: FxIndex = FxIndex.of("EUR/USD-ECB")

  /**
   * The FX index for conversion from USD to CHF, as defined by the WM company
   * "Closing Spot rates".
   */
  @static final val USD_CHF_WM: FxIndex = FxIndex.of("USD/CHF-WM")
  /**
   * The FX index for conversion from GBP to USD, as defined by the WM company
   * "Closing Spot rates".
   */
  @static final val GBP_USD_WM: FxIndex = FxIndex.of("GBP/USD-WM")
  /**
   * The FX index for conversion from EUR to GBP, as defined by the WM company
   * "Closing Spot rates".
   */
  @static final val EUR_USD_WM: FxIndex = FxIndex.of("EUR/USD-WM")
  /**
   * The FX index for conversion from USD to JPY, as defined by the WM company
   * "Closing Spot rates".
   */
  @static final val USD_JPY_WM: FxIndex = FxIndex.of("USD/JPY-WM")

  //-------------------------------------------------------------------------
  // The extended enum lookup is held in a lazily initialized nested object, not a @static field:
  // Scala emits an accessed @static field as public, which would break reflective scans of the
  // public constants, and non-static members may not precede the @static constants.
  private object Lookup {
    /**
     * The extended enum lookup from name to instance.
     */
    val ENUM_LOOKUP: ExtendedEnum[FxIndex] = ExtendedEnum.of(classOf[FxIndex])
  }

  private[index] def enumLookup: ExtendedEnum[FxIndex] = Lookup.ENUM_LOOKUP

}
