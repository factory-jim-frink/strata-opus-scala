/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for commonly used Floating rate names.
 *
 * Each constant refers to a standard definition of the specified index.
 *
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
 */
final class FloatingRateNames private ()

object FloatingRateNames {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  //-------------------------------------------------------------------------
  /**
   * Constant for GBP-LIBOR.
   */
  @static final val GBP_LIBOR: FloatingRateName = FloatingRateName.of("GBP-LIBOR")
  /**
   * Constant for USD-LIBOR.
   */
  @static final val USD_LIBOR: FloatingRateName = FloatingRateName.of("USD-LIBOR")
  /**
   * Constant for USD-BSBY.
   */
  @static final val USD_BSBY: FloatingRateName = FloatingRateName.of("USD-BSBY")
  /**
   * Constant for CHF-LIBOR.
   */
  @static final val CHF_LIBOR: FloatingRateName = FloatingRateName.of("CHF-LIBOR")
  /**
   * Constant for EUR-LIBOR.
   */
  @static final val EUR_LIBOR: FloatingRateName = FloatingRateName.of("EUR-LIBOR")
  /**
   * Constant for JPY-LIBOR.
   */
  @static final val JPY_LIBOR: FloatingRateName = FloatingRateName.of("JPY-LIBOR")
  /**
   * Constant for EUR-EURIBOR.
   */
  @static final val EUR_EURIBOR: FloatingRateName = FloatingRateName.of("EUR-EURIBOR")
  /**
   * Constant for AUD-BBSW.
   */
  @static final val AUD_BBSW: FloatingRateName = FloatingRateName.of("AUD-BBSW")
  /**
   * Constant for CAD-CDOR.
   */
  @static final val CAD_CDOR: FloatingRateName = FloatingRateName.of("CAD-CDOR")
  /**
   * Constant for CZK-PRIBOR.
   */
  @static final val CZK_PRIBOR: FloatingRateName = FloatingRateName.of("CZK-PRIBOR")
  /**
   * Constant for DKK-CIBOR.
   */
  @static final val DKK_CIBOR: FloatingRateName = FloatingRateName.of("DKK-CIBOR")
  /**
   * Constant for HUF-BUBOR.
   */
  @static final val HUF_BUBOR: FloatingRateName = FloatingRateName.of("HUF-BUBOR")
  /**
   * Constant for MXN-TIIE.
   */
  @static final val MXN_TIIE: FloatingRateName = FloatingRateName.of("MXN-TIIE")
  /**
   * Constant for NOK-NIBOR.
   */
  @static final val NOK_NIBOR: FloatingRateName = FloatingRateName.of("NOK-NIBOR")
  /**
   * Constant for NZD-BKBM.
   */
  @static final val NZD_BKBM: FloatingRateName = FloatingRateName.of("NZD-BKBM")
  /**
   * Constant for PLN-WIBOR.
   */
  @static final val PLN_WIBOR: FloatingRateName = FloatingRateName.of("PLN-WIBOR")
  /**
   * Constant for SEK-STIBOR.
   */
  @static final val SEK_STIBOR: FloatingRateName = FloatingRateName.of("SEK-STIBOR")
  /**
   * Constant for ZAR-JIBAR.
   */
  @static final val ZAR_JIBAR: FloatingRateName = FloatingRateName.of("ZAR-JIBAR")

  /**
   * Constant for GBP-SONIA Overnight index.
   */
  @static final val GBP_SONIA: FloatingRateName = FloatingRateName.of("GBP-SONIA")
  /**
   * Constant for USD-FED-FUND Overnight index.
   */
  @static final val USD_FED_FUND: FloatingRateName = FloatingRateName.of("USD-FED-FUND")
  /**
   * Constant for USD-SOFR Overnight index.
   */
  @static final val USD_SOFR: FloatingRateName = FloatingRateName.of("USD-SOFR")
  /**
   * Constant for CHF-SARON Overnight index.
   */
  @static final val CHF_SARON: FloatingRateName = FloatingRateName.of("CHF-SARON")
  /**
   * Constant for CHF-TOIS Overnight index.
   *
   * @deprecated Not published as of 2017-12-29
   */
  @Deprecated
  @static final val CHF_TOIS: FloatingRateName = FloatingRateName.of("CHF-TOIS")
  /**
   * Constant for EUR-EONIA Overnight index.
   */
  @static final val EUR_EONIA: FloatingRateName = FloatingRateName.of("EUR-EONIA")
  /**
   * Constant for EUR-ESTR Overnight index.
   */
  @static final val EUR_ESTR: FloatingRateName = FloatingRateName.of("EUR-ESTR")
  /**
   * Constant for EUR-ESTER Overnight index.
   *
   * The index was renamed.
   *
   * @deprecated Use EUR_ESTR instead
   */
  @Deprecated
  @static final val EUR_ESTER: FloatingRateName = FloatingRateName.of("EUR-ESTER")
  /**
   * Constant for JPY-TONAR Overnight index.
   */
  @static final val JPY_TONAR: FloatingRateName = FloatingRateName.of("JPY-TONAR")
  /**
   * Constant for AUD-AONIA Overnight index.
   */
  @static final val AUD_AONIA: FloatingRateName = FloatingRateName.of("AUD-AONIA")
  /**
   * Constant for BRL-CDI Overnight index.
   */
  @static final val BRL_CDI: FloatingRateName = FloatingRateName.of("BRL-CDI")
  /**
   * Constant for CAD-CORRA Overnight index.
   */
  @static final val CAD_CORRA: FloatingRateName = FloatingRateName.of("CAD-CORRA")
  /**
   * Constant for DKK-TNR Overnight index.
   */
  @static final val DKK_TNR: FloatingRateName = FloatingRateName.of("DKK-TNR")
  /**
   * Constant for NOK-NOWA Overnight index.
   */
  @static final val NOK_NOWA: FloatingRateName = FloatingRateName.of("NOK-NOWA")
  /**
   * Constant for PLN-POLONIA Overnight index.
   */
  @static final val PLN_POLONIA: FloatingRateName = FloatingRateName.of("PLN-POLONIA")
  /**
   * Constant for PLN-POLSTR Overnight index.
   */
  @static final val PLN_POLSTR: FloatingRateName = FloatingRateName.of("PLN-POLSTR")
  /**
   * Constant for SEK-SIOR Overnight index.
   */
  @static final val SEK_SIOR: FloatingRateName = FloatingRateName.of("SEK-SIOR")
  /**
   * Constant for THB-THOR Overnight index.
   */
  @static final val THB_THOR: FloatingRateName = FloatingRateName.of("THB-THOR")

  /**
   * Constant for USD-FED-FUND Overnight index using averaging.
   */
  @static final val USD_FED_FUND_AVG: FloatingRateName = FloatingRateName.of("USD-FED-FUND-AVG")

  /**
   * Constant for GB-RPI Price index.
   */
  @static final val GB_RPI: FloatingRateName = FloatingRateName.of("GB-RPI")
  /**
   * Constant for EU-EXT-CPI Price index.
   */
  @static final val EU_EXT_CPI: FloatingRateName = FloatingRateName.of("EU-EXT-CPI")
  /**
   * Constant for US-CPI-U Price index.
   */
  @static final val US_CPI_U: FloatingRateName = FloatingRateName.of("US-CPI-U")
  /**
   * Constant for FR-EXT-CPI Price index.
   */
  @static final val FR_EXT_CPI: FloatingRateName = FloatingRateName.of("FR-EXT-CPI")

  //-------------------------------------------------------------------------
  // The extended enum lookup is held in a lazily initialized nested object, not a @static field:
  // Scala emits an accessed @static field as public, which would break reflective scans of the
  // public constants, and non-static members may not precede the @static constants.
  private object Lookup {
    /**
     * The extended enum lookup from name to instance.
     */
    val ENUM_LOOKUP: ExtendedEnum[FloatingRateName] = ExtendedEnum.of(classOf[FloatingRateName])
  }

  private[index] def enumLookup: ExtendedEnum[FloatingRateName] = Lookup.ENUM_LOOKUP

}
