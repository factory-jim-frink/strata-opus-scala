/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for commonly used Ibor indices.
 *
 * Each constant returns a standard definition of the specified index.
 *
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
 */
final class IborIndices private () {
}

object IborIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  @static private[index] final val ENUM_LOOKUP: ExtendedEnum[IborIndex] = ExtendedEnum.of(classOf[IborIndex])

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_1W: IborIndex = IborIndex.of("GBP-LIBOR-1W")
  /**
   * The 1 month LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_1M: IborIndex = IborIndex.of("GBP-LIBOR-1M")
  /**
   * The 2 month LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_2M: IborIndex = IborIndex.of("GBP-LIBOR-2M")
  /**
   * The 3 month LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_3M: IborIndex = IborIndex.of("GBP-LIBOR-3M")
  /**
   * The 6 month LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_6M: IborIndex = IborIndex.of("GBP-LIBOR-6M")
  /**
   * The 12 month LIBOR index for GBP.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val GBP_LIBOR_12M: IborIndex = IborIndex.of("GBP-LIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_1W: IborIndex = IborIndex.of("CHF-LIBOR-1W")
  /**
   * The 1 month LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_1M: IborIndex = IborIndex.of("CHF-LIBOR-1M")
  /**
   * The 2 month LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_2M: IborIndex = IborIndex.of("CHF-LIBOR-2M")
  /**
   * The 3 month LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_3M: IborIndex = IborIndex.of("CHF-LIBOR-3M")
  /**
   * The 6 month LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_6M: IborIndex = IborIndex.of("CHF-LIBOR-6M")
  /**
   * The 12 month LIBOR index for CHF.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val CHF_LIBOR_12M: IborIndex = IborIndex.of("CHF-LIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_1W: IborIndex = IborIndex.of("EUR-LIBOR-1W")
  /**
   * The 1 month LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_1M: IborIndex = IborIndex.of("EUR-LIBOR-1M")
  /**
   * The 2 month LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_2M: IborIndex = IborIndex.of("EUR-LIBOR-2M")
  /**
   * The 3 month LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_3M: IborIndex = IborIndex.of("EUR-LIBOR-3M")
  /**
   * The 6 month LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_6M: IborIndex = IborIndex.of("EUR-LIBOR-6M")
  /**
   * The 12 month LIBOR index for EUR.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val EUR_LIBOR_12M: IborIndex = IborIndex.of("EUR-LIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_1W: IborIndex = IborIndex.of("JPY-LIBOR-1W")
  /**
   * The 1 month LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_1M: IborIndex = IborIndex.of("JPY-LIBOR-1M")
  /**
   * The 2 month LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_2M: IborIndex = IborIndex.of("JPY-LIBOR-2M")
  /**
   * The 3 month LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_3M: IborIndex = IborIndex.of("JPY-LIBOR-3M")
  /**
   * The 6 month LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_6M: IborIndex = IborIndex.of("JPY-LIBOR-6M")
  /**
   * The 12 month LIBOR index for JPY.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val JPY_LIBOR_12M: IborIndex = IborIndex.of("JPY-LIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_1W: IborIndex = IborIndex.of("USD-LIBOR-1W")
  /**
   * The 1 month LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_1M: IborIndex = IborIndex.of("USD-LIBOR-1M")
  /**
   * The 2 month LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_2M: IborIndex = IborIndex.of("USD-LIBOR-2M")
  /**
   * The 3 month LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_3M: IborIndex = IborIndex.of("USD-LIBOR-3M")
  /**
   * The 6 month LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_6M: IborIndex = IborIndex.of("USD-LIBOR-6M")
  /**
   * The 12 month LIBOR index for USD.
   *
   * The "London Interbank Offered Rate".
   */
  @static final val USD_LIBOR_12M: IborIndex = IborIndex.of("USD-LIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   */
  @static final val EUR_EURIBOR_1W: IborIndex = IborIndex.of("EUR-EURIBOR-1W")
  /**
   * The 2 week EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
  @static final val EUR_EURIBOR_2W: IborIndex = IborIndex.of("EUR-EURIBOR-2W")
  /**
   * The 1 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   */
  @static final val EUR_EURIBOR_1M: IborIndex = IborIndex.of("EUR-EURIBOR-1M")
  /**
   * The 2 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
  @static final val EUR_EURIBOR_2M: IborIndex = IborIndex.of("EUR-EURIBOR-2M")
  /**
   * The 3 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   */
  @static final val EUR_EURIBOR_3M: IborIndex = IborIndex.of("EUR-EURIBOR-3M")
  /**
   * The 6 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   */
  @static final val EUR_EURIBOR_6M: IborIndex = IborIndex.of("EUR-EURIBOR-6M")
  /**
   * The 9 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
  @static final val EUR_EURIBOR_9M: IborIndex = IborIndex.of("EUR-EURIBOR-9M")
  /**
   * The 12 month EURIBOR index.
   *
   * The "Euro Interbank Offered Rate".
   */
  @static final val EUR_EURIBOR_12M: IborIndex = IborIndex.of("EUR-EURIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  @static final val JPY_TIBOR_JAPAN_1W: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-1W")
  /**
   * The 1 month TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  @static final val JPY_TIBOR_JAPAN_1M: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-1M")
  /**
   * The 2 month TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   * 
   * @deprecated Not published as of 2019-04-01
   */
  @Deprecated
  @static final val JPY_TIBOR_JAPAN_2M: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-2M")
  /**
   * The 3 month TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  @static final val JPY_TIBOR_JAPAN_3M: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-3M")
  /**
   * The 6 month TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  @static final val JPY_TIBOR_JAPAN_6M: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-6M")
  /**
   * The 12 month TIBOR (Japan) index.
   *
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  @static final val JPY_TIBOR_JAPAN_12M: IborIndex = IborIndex.of("JPY-TIBOR-JAPAN-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  @static final val JPY_TIBOR_EUROYEN_1W: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-1W")
  /**
   * The 1 month TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  @static final val JPY_TIBOR_EUROYEN_1M: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-1M")
  /**
   * The 2 month TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   * 
   * @deprecated Not published as of 2019-04-01
   */
  @Deprecated
  @static final val JPY_TIBOR_EUROYEN_2M: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-2M")
  /**
   * The 3 month TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  @static final val JPY_TIBOR_EUROYEN_3M: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-3M")
  /**
   * The 6 month TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  @static final val JPY_TIBOR_EUROYEN_6M: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-6M")
  /**
   * The 12 month TIBOR (Euroyen) index.
   *
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  @static final val JPY_TIBOR_EUROYEN_12M: IborIndex = IborIndex.of("JPY-TIBOR-EUROYEN-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_1M: IborIndex = IborIndex.of("AUD-BBSW-1M")
  /**
   * The 2 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_2M: IborIndex = IborIndex.of("AUD-BBSW-2M")
  /**
   * The 3 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_3M: IborIndex = IborIndex.of("AUD-BBSW-3M")
  /**
   * The 4 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_4M: IborIndex = IborIndex.of("AUD-BBSW-4M")
  /**
   * The 5 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_5M: IborIndex = IborIndex.of("AUD-BBSW-5M")
  /**
   * The 6 month BBSW index.
   *
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  @static final val AUD_BBSW_6M: IborIndex = IborIndex.of("AUD-BBSW-6M")

  //-------------------------------------------------------------------------
  /**
   * The 1 month CDOR index.
   *
   * The "Canadian Dollar Offered Rate".
   */
  @static final val CAD_CDOR_1M: IborIndex = IborIndex.of("CAD-CDOR-1M")
  /**
   * The 2 month CDOR index.
   *
   * The "Canadian Dollar Offered Rate".
   */
  @static final val CAD_CDOR_2M: IborIndex = IborIndex.of("CAD-CDOR-2M")
  /**
   * The 3 month CDOR index.
   *
   * The "Canadian Dollar Offered Rate".
   */
  @static final val CAD_CDOR_3M: IborIndex = IborIndex.of("CAD-CDOR-3M")
  /**
   * The 6 month CDOR index.
   *
   * The "Canadian Dollar Offered Rate".
   * 
   * @deprecated Not published as of 2021-05-17
   */
  @Deprecated
  @static final val CAD_CDOR_6M: IborIndex = IborIndex.of("CAD-CDOR-6M")
  /**
   * The 12 month CDOR index.
   *
   * The "Canadian Dollar Offered Rate".
   * 
   * @deprecated Not published as of 2021-05-17
   */
  @Deprecated
  @static final val CAD_CDOR_12M: IborIndex = IborIndex.of("CAD-CDOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_1W: IborIndex = IborIndex.of("CZK-PRIBOR-1W")
  /**
   * The 2 week PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_2W: IborIndex = IborIndex.of("CZK-PRIBOR-2W")
  /**
   * The 1 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_1M: IborIndex = IborIndex.of("CZK-PRIBOR-1M")
  /**
   * The 2 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_2M: IborIndex = IborIndex.of("CZK-PRIBOR-2M")
  /**
   * The 3 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_3M: IborIndex = IborIndex.of("CZK-PRIBOR-3M")
  /**
   * The 6 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_6M: IborIndex = IborIndex.of("CZK-PRIBOR-6M")
  /**
   * The 9 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_9M: IborIndex = IborIndex.of("CZK-PRIBOR-9M")
  /**
   * The 12 month PRIBOR index.
   *
   * The "Prague Interbank Offered Rate".
   */
  @static final val CZK_PRIBOR_12M: IborIndex = IborIndex.of("CZK-PRIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_1W: IborIndex = IborIndex.of("DKK-CIBOR-1W")
  /**
   * The 2 week CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_2W: IborIndex = IborIndex.of("DKK-CIBOR-2W")
  /**
   * The 1 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_1M: IborIndex = IborIndex.of("DKK-CIBOR-1M")
  /**
   * The 2 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_2M: IborIndex = IborIndex.of("DKK-CIBOR-2M")
  /**
   * The 3 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_3M: IborIndex = IborIndex.of("DKK-CIBOR-3M")
  /**
   * The 6 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_6M: IborIndex = IborIndex.of("DKK-CIBOR-6M")
  /**
   * The 9 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_9M: IborIndex = IborIndex.of("DKK-CIBOR-9M")
  /**
   * The 12 month CIBOR index.
   *
   * The "Copenhagen Interbank Offered Rate".
   */
  @static final val DKK_CIBOR_12M: IborIndex = IborIndex.of("DKK-CIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_1W: IborIndex = IborIndex.of("HUF-BUBOR-1W")
  /**
   * The 2 week BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_2W: IborIndex = IborIndex.of("HUF-BUBOR-2W")
  /**
   * The 1 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_1M: IborIndex = IborIndex.of("HUF-BUBOR-1M")
  /**
   * The 2 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_2M: IborIndex = IborIndex.of("HUF-BUBOR-2M")
  /**
   * The 3 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_3M: IborIndex = IborIndex.of("HUF-BUBOR-3M")
  /**
   * The 6 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_6M: IborIndex = IborIndex.of("HUF-BUBOR-6M")
  /**
   * The 9 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_9M: IborIndex = IborIndex.of("HUF-BUBOR-9M")
  /**
   * The 12 month BUBOR index.
   *
   * The "Budapest Interbank Offered Rate".
   */
  @static final val HUF_BUBOR_12M: IborIndex = IborIndex.of("HUF-BUBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 4 week TIIE index.
   *
   * The "Interbank Equilibrium Interest Rate".
   */
  @static final val MXN_TIIE_4W: IborIndex = IborIndex.of("MXN-TIIE-4W")
  /**
   * The 13 week TIIE index.
   *
   * The "Interbank Equilibrium Interest Rate".
   */
  @static final val MXN_TIIE_13W: IborIndex = IborIndex.of("MXN-TIIE-13W")
  /**
   * The 26 week TIIE index.
   *
   * The "Interbank Equilibrium Interest Rate".
   */
  @static final val MXN_TIIE_26W: IborIndex = IborIndex.of("MXN-TIIE-26W")

  //-------------------------------------------------------------------------
  /**
   * The 1 week NIBOR index.
   *
   * The "Norwegian Interbank Offered Rate".
   */
  @static final val NOK_NIBOR_1W: IborIndex = IborIndex.of("NOK-NIBOR-1W")
  /**
   * The 1 month NIBOR index.
   *
   * The "Norwegian Interbank Offered Rate".
   */
  @static final val NOK_NIBOR_1M: IborIndex = IborIndex.of("NOK-NIBOR-1M")
  /**
   * The 2 month NIBOR index.
   *
   * The "Norwegian Interbank Offered Rate".
   */
  @static final val NOK_NIBOR_2M: IborIndex = IborIndex.of("NOK-NIBOR-2M")
  /**
   * The 3 month NIBOR index.
   *
   * The "Norwegian Interbank Offered Rate".
   */
  @static final val NOK_NIBOR_3M: IborIndex = IborIndex.of("NOK-NIBOR-3M")
  /**
   * The 6 month NIBOR index.
   *
   * The "Norwegian Interbank Offered Rate".
   */
  @static final val NOK_NIBOR_6M: IborIndex = IborIndex.of("NOK-NIBOR-6M")

  //-------------------------------------------------------------------------
  /**
   * The 1 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_1M: IborIndex = IborIndex.of("NZD-BKBM-1M")
  /**
   * The 2 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_2M: IborIndex = IborIndex.of("NZD-BKBM-2M")
  /**
   * The 3 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_3M: IborIndex = IborIndex.of("NZD-BKBM-3M")
  /**
   * The 4 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_4M: IborIndex = IborIndex.of("NZD-BKBM-4M")
  /**
   * The 5 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_5M: IborIndex = IborIndex.of("NZD-BKBM-5M")
  /**
   * The 6 month BKBM index.
   *
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  @static final val NZD_BKBM_6M: IborIndex = IborIndex.of("NZD-BKBM-6M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week WIBOR index.
   *
   * The "Polish Interbank Offered Rate".
   */
  @static final val PLN_WIBOR_1W: IborIndex = IborIndex.of("PLN-WIBOR-1W")
  /**
   * The 1 month WIBOR index.
   *
   * The "Polish Interbank Offered Rate".
   */
  @static final val PLN_WIBOR_1M: IborIndex = IborIndex.of("PLN-WIBOR-1M")
  /**
   * The 3 month WIBOR index.
   *
   * The "Polish Interbank Offered Rate".
   */
  @static final val PLN_WIBOR_3M: IborIndex = IborIndex.of("PLN-WIBOR-3M")
  /**
   * The 6 month WIBOR index.
   *
   * The "Polish Interbank Offered Rate".
   */
  @static final val PLN_WIBOR_6M: IborIndex = IborIndex.of("PLN-WIBOR-6M")
  /**
   * The 12 month WIBOR index.
   *
   * The "Polish Interbank Offered Rate".
   */
  @static final val PLN_WIBOR_12M: IborIndex = IborIndex.of("PLN-WIBOR-12M")

  //-------------------------------------------------------------------------
  /**
   * The 1 week STIBOR index.
   *
   * The "Swedish Interbank Offered Rate".
   */
  @static final val SEK_STIBOR_1W: IborIndex = IborIndex.of("SEK-STIBOR-1W")
  /**
   * The 1 month STIBOR index.
   *
   * The "Swedish Interbank Offered Rate".
   */
  @static final val SEK_STIBOR_1M: IborIndex = IborIndex.of("SEK-STIBOR-1M")
  /**
   * The 2 month STIBOR index.
   *
   * The "Swedish Interbank Offered Rate".
   */
  @static final val SEK_STIBOR_2M: IborIndex = IborIndex.of("SEK-STIBOR-2M")
  /**
   * The 3 month STIBOR index.
   *
   * The "Swedish Interbank Offered Rate".
   */
  @static final val SEK_STIBOR_3M: IborIndex = IborIndex.of("SEK-STIBOR-3M")
  /**
   * The 6 month STIBOR index.
   *
   * The "Swedish Interbank Offered Rate".
   */
  @static final val SEK_STIBOR_6M: IborIndex = IborIndex.of("SEK-STIBOR-6M")

  //-------------------------------------------------------------------------
  /**
   * The 1 month JIBAR index.
   *
   * The "Johannnesburg Interbank Average Rate".
   */
  @static final val ZAR_JIBAR_1M: IborIndex = IborIndex.of("ZAR-JIBAR-1M")
  /**
   * The 3 month JIBAR index.
   *
   * The "Johannnesburg Interbank Average Rate".
   */
  @static final val ZAR_JIBAR_3M: IborIndex = IborIndex.of("ZAR-JIBAR-3M")
  /**
   * The 6 month JIBAR index.
   *
   * The "Johannnesburg Interbank Average Rate".
   */
  @static final val ZAR_JIBAR_6M: IborIndex = IborIndex.of("ZAR-JIBAR-6M")
  /**
   * The 12 month JIBAR index.
   *
   * The "Johannnesburg Interbank Average Rate".
   */
  @static final val ZAR_JIBAR_12M: IborIndex = IborIndex.of("ZAR-JIBAR-12M")

}
