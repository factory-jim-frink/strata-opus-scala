/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard Overnight rate indices.
 *
 * Each constant returns a standard definition of the specified index.
 *
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
 */
final class OvernightIndices private () {
}

object OvernightIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  @static private[index] final val ENUM_LOOKUP: ExtendedEnum[OvernightIndex] = ExtendedEnum.of(classOf[OvernightIndex])

  //-------------------------------------------------------------------------
  /**
   * The SONIA index for GBP.
   *
   * The Sterling Overnight Index Average (SONIA) index.
   */
  @static final val GBP_SONIA: OvernightIndex = OvernightIndex.of("GBP-SONIA")
  /**
   * The SARON index for CHF.
   *
   * The Swiss Average Overnight Rate (SARON) index.
   */
  @static final val CHF_SARON: OvernightIndex = OvernightIndex.of("CHF-SARON")
  /**
   * The TOIS index for CHF.
   *
   * The Tomorrow/Next Overnight Indexed Swaps (TOIS) index, which is a "Tomorrow/Next" index.
   * 
   * @deprecated Not published as of 2017-12-29
   */
  @Deprecated
  @static final val CHF_TOIS: OvernightIndex = OvernightIndex.of("CHF-TOIS")
  /**
   * The EONIA index for EUR.
   *
   * The Euro OverNight Index Average (EONIA) index.
   */
  @static final val EUR_EONIA: OvernightIndex = OvernightIndex.of("EUR-EONIA")
  /**
   * The ESTR index for EUR.
   *
   * The Euro Short-Term Rate (€STR) index.
   * This was first published on 2019-10-02.
   */
  @static final val EUR_ESTR: OvernightIndex = OvernightIndex.of("EUR-ESTR")
  /**
   * The ESTR index for EUR, under the old ESTER name.
   *
   * The index was renamed.
   * 
   * @deprecated Use EUR_ESTR instead
   */
  @Deprecated
  @static final val EUR_ESTER: OvernightIndex = EUR_ESTR
  /**
   * The TONAR index for JPY.
   *
   * The Tokyo Overnight Average Rate (TONAR) index.
   */
  @static final val JPY_TONAR: OvernightIndex = OvernightIndex.of("JPY-TONAR")
  /**
   * The Fed Fund index for USD.
   *
   * The Federal Funds Rate index.
   */
  @static final val USD_FED_FUND: OvernightIndex = OvernightIndex.of("USD-FED-FUND")
  /**
   * The SOFR index for USD.
   *
   * The Secured Overnight Financing Rate (SOFR) index.
   */
  @static final val USD_SOFR: OvernightIndex = OvernightIndex.of("USD-SOFR")
  /**
   * The AMERIBOR index for USD.
   *
   * The AMERIBOR index.
   */
  @static final val USD_AMERIBOR: OvernightIndex = OvernightIndex.of("USD-AMERIBOR")
  /**
   * The AONIA index for AUD.
   *
   * AONIA is an "Overnight" index.
   */
  @static final val AUD_AONIA: OvernightIndex = OvernightIndex.of("AUD-AONIA")
  /**
   * The CDI index for BRL.
   *
   * The "Brazil Certificates of Interbank Deposit" index.
   */
  @static final val BRL_CDI: OvernightIndex = OvernightIndex.of("BRL-CDI")
  /**
   * The CORRA index for CAD.
   *
   * The "Canadian Overnight Repo Rate Average" index.
   */
  @static final val CAD_CORRA: OvernightIndex = OvernightIndex.of("CAD-CORRA")
  /**
   * The TN index for DKK.
   *
   * The "Tomorrow/Next-renten" index.
   */
  @static final val DKK_TNR: OvernightIndex = OvernightIndex.of("DKK-TNR")
  /**
   * The NOWA index for NOK.
   *
   * The "Norwegian Overnight Weighted Average" index.
   */
  @static final val NOK_NOWA: OvernightIndex = OvernightIndex.of("NOK-NOWA")
  /**
   * The NZIONA index for NZD.
   *
   * The "New Zealand Overnight" index.
   */
  @static final val NZD_NZIONA: OvernightIndex = OvernightIndex.of("NZD-NZIONA")
  /**
   * The PLONIA index for PLN.
   *
   * The "Polish Overnight" index.
   */
  @static final val PLN_POLONIA: OvernightIndex = OvernightIndex.of("PLN-POLONIA")
  /**
   * The POLSTR index for PLN.
   *
   * The "Polish Short Term Rate".
   */
  @static final val PLN_POLSTR: OvernightIndex = OvernightIndex.of("PLN-POLSTR")
  /**
   * The SIOR index for SEK.
   *
   * The "STIBOR T/N" index.
   */
  @static final val SEK_SIOR: OvernightIndex = OvernightIndex.of("SEK-SIOR")
  /**
   * The THOR index for THB.
   *
   * The "Thai Overnight Repurchase Rate" index.
   */
  @static final val THB_THOR: OvernightIndex = OvernightIndex.of("THB-THOR")
  /**
   * The SABOR index for ZAR.
   *
   * The "South African Benchmark Overnight Rate" index.
   */
  @static final val ZAR_SABOR: OvernightIndex = OvernightIndex.of("ZAR-SABOR")

}
