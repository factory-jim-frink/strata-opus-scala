/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard price indices.
 *
 * Each constant returns a standard definition of the specified index.
 */
final class PriceIndices private ()

object PriceIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The harmonized consumer price index for the United Kingdom,
   * "Non-revised Harmonised Index of Consumer Prices".
   */
  @static final val GB_HICP: PriceIndex = PriceIndex.of("GB-HICP")
  /**
   * The retail price index for the United Kingdom,
   * "Non-revised Retail Price Index All Items in the United Kingdom".
   */
  @static final val GB_RPI: PriceIndex = PriceIndex.of("GB-RPI")
  /**
   * The retail price index for the United Kingdom excluding mortgage interest payments,
   * "Non-revised Retail Price Index Excluding Mortgage Interest Payments in the United Kingdom".
   */
  @static final val GB_RPIX: PriceIndex = PriceIndex.of("GB-RPIX")
  /**
   * The consumer price index for Switzerland,
   * "Non-revised Consumer Price Index".
   */
  @static final val CH_CPI: PriceIndex = PriceIndex.of("CH-CPI")
  /**
   * The consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices All Items".
   */
  @static final val EU_AI_CPI: PriceIndex = PriceIndex.of("EU-AI-CPI")
  /**
   * The consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  @static final val EU_EXT_CPI: PriceIndex = PriceIndex.of("EU-EXT-CPI")
  /**
   * The consumer price index for Japan excluding fresh food,
   * "Non-revised Consumer Price Index Nationwide General Excluding Fresh Food".
   */
  @static final val JP_CPI_EXF: PriceIndex = PriceIndex.of("JP-CPI-EXF")
  /**
   * The consumer price index for US Urban consumers,
   * "Non-revised index of Consumer Prices for All Urban Consumers (CPI-U) before seasonal adjustment".
   */
  @static final val US_CPI_U: PriceIndex = PriceIndex.of("US-CPI-U")
  /**
   * The consumer price index for France,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  @static final val FR_EXT_CPI: PriceIndex = PriceIndex.of("FR-EXT-CPI")

  //-------------------------------------------------------------------------
  // The extended enum lookup is held in a lazily initialized nested object, not a @static field:
  // Scala emits an accessed @static field as public, which would break reflective scans of the
  // public constants, and non-static members may not precede the @static constants.
  private object Lookup {
    /**
     * The extended enum lookup from name to instance.
     */
    val ENUM_LOOKUP: ExtendedEnum[PriceIndex] = ExtendedEnum.of(classOf[PriceIndex])
  }

  private[index] def enumLookup: ExtendedEnum[PriceIndex] = Lookup.ENUM_LOOKUP

}
