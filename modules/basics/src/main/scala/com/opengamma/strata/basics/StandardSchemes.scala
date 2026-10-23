/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

import scala.annotation.static

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.tuple.Pair

/**
 * A set of schemes that can be used with `StandardId`.
 *
 * The scheme part of a `StandardId` is freeform, but it obviously works best where
 * the scheme name is widely agreed. This class contains a set of useful constants.
 */
final class StandardSchemes private () {

}

object StandardSchemes {

  /**
   * The OpenGamma scheme used to identify values in market data.
   */
  @static final val OG_TICKER_SCHEME: String = "OG-Ticker"
  /**
   * The OpenGamma scheme used to identify ETDs in market data.
   */
  @static final val OG_ETD_SCHEME: String = "OG-ETD"
  /**
   * The OpenGamma scheme used for trade identifiers.
   */
  @static final val OG_TRADE_SCHEME: String = "OG-Trade"
  /**
   * The OpenGamma scheme used for position identifiers.
   */
  @static final val OG_POSITION_SCHEME: String = "OG-Position"
  /**
   * The OpenGamma scheme used for portfolio sensitivity identifiers.
   */
  @static final val OG_SENSITIVITY_SCHEME: String = "OG-Sensitivity"
  /**
   * The OpenGamma scheme used for securities.
   */
  @static final val OG_SECURITY_SCHEME: String = "OG-Security"
  /**
   * The OpenGamma scheme used for counterparties.
   */
  @static final val OG_COUNTERPARTY: String = "OG-Counterparty"
  /**
   * The OpenGamma scheme used for portfolios.
   */
  @static final val OG_PORTFOLIO: String = "OG-Portfolio"

  /**
   * The scheme for exchange Tickers.
   *
   * A ticker is the human-readable identifier used by the exchange for a security.
   * It is not a stable identifier, and each exchange defines their own tickers.
   * A company can change ticker over time, such as if the company merges or changes it's name.
   * If a company ceases to use a particular ticker, the ticker can be reused for an entirely different company.
   * Tickers are typically reused over time as companies change.
   *
   * A ticker is unlikely to be useful as an identifier without an exchange, see `TICMIC_SCHEME`.
   */
  @static final val TICKER_SCHEME: String = "TICKER"
  /**
   * The scheme for TICMICs combining the exchange Ticker with the exchange MIC.
   *
   * A TICMIC is an identifier that combines the Ticker, as defined by the exchange,
   * with the MIC (Market Identifier Code) that identifies the exchange.
   * The format is `<ticker>@<exchangeMic>`.
   * For example, ULVR@XLON represents Unilever on the London Stock Exchange.
   */
  @static final val TICMIC_SCHEME: String = "TICMIC"
  /**
   * The scheme for ISINs.
   *
   * https://en.wikipedia.org/wiki/International_Securities_Identification_Number
   */
  @static final val ISIN_SCHEME: String = "ISIN"
  /**
   * The scheme for CUSIPs, the North American numbering system.
   *
   * https://en.wikipedia.org/wiki/CUSIP
   */
  @static final val CUSIP_SCHEME: String = "CUSIP"
  /**
   * The scheme for SEDOLs, the United Kingdom numbering system.
   *
   * https://en.wikipedia.org/wiki/SEDOL
   */
  @static final val SEDOL_SCHEME: String = "SEDOL"
  /**
   * The scheme for Wertpapierkennnummer, the German numbering system.
   *
   * https://en.wikipedia.org/wiki/Wertpapierkennnummer
   */
  @static final val WKN_SCHEME: String = "WKN"
  /**
   * The scheme for VALOR numbers, the Swiss numbering system.
   *
   * https://en.wikipedia.org/wiki/Valoren_number
   */
  @static final val VALOR_SCHEME: String = "VALOR"

  /**
   * The scheme for RICs, the Reuters Instrument Code.
   *
   * https://en.wikipedia.org/wiki/Reuters_Instrument_Code
   */
  @static final val RIC_SCHEME: String = "RIC"
  /**
   * The scheme for Chain RICs, which identifies a set of linked RICs.
   */
  @static final val CHAIN_RIC_SCHEME: String = "CHAINRIC"
  /**
   * The scheme for Bloomberg Tickers.
   */
  @static final val BBG_SCHEME: String = "BBG"
  /**
   * The scheme for FIGIs, the Financial Instrument Global Identifier.
   *
   * https://en.wikipedia.org/wiki/Financial_Instrument_Global_Identifier
   */
  @static final val FIGI_SCHEME: String = "FIGI"
  /**
   * The scheme for LEIs, the Legal Entity Identifier.
   *
   * https://en.wikipedia.org/wiki/Legal_Entity_Identifier
   */
  @static final val LEI_SCHEME: String = "LEI"
  /**
   * The scheme for 6 character RED codes, the Reference Entity Data code.
   *
   * https://ihsmarkit.com/products/red-cds.html
   */
  @static final val RED6_SCHEME: String = "RED6"
  /**
   * The scheme for 9 character RED codes, the Reference Entity Data code.
   *
   * https://ihsmarkit.com/products/red-cds.html
   */
  @static final val RED9_SCHEME: String = "RED9"
  /**
   * The scheme for OPRA option codes.
   *
   * These codes have:
   *  - 1 to 5 characters for the underlying root symbol
   *  - 1 letter representing the month and put/call
   *  - 2 digits for the day-of-month
   *  - 2 digits for the year
   *  - 1 character flag indicating the scale of the strike
   *  - 6 digits for the strike
   * https://customers.refinitiv.com/wetfetch/index.aspx?CID=27348&doc=OSI_FAQ_18th_Feb_2010.pdf&base=/support/datasupport/option_symbology_change.aspx
   */
  @static final val OPRA_SCHEME: String = "OPRA"
  /**
   * The scheme for OCC option codes.
   *
   * These codes have:
   *  - 1 to 6 characters for the underlying root symbol
   *  - 2 digits for the year
   *  - 2 digits for the month
   *  - 2 digits for the day-of-month
   *  - 1 letter representing put/call
   *  - 8 digits for the strike multiplied by 1000
   * https://customers.refinitiv.com/wetfetch/index.aspx?CID=27348&doc=OSI_FAQ_18th_Feb_2010.pdf&base=/support/datasupport/option_symbology_change.aspx
   * https://ibkr.info/node/972
   */
  @static final val OCC_SCHEME: String = "OCC"

  //-------------------------------------------------------------------------
  /**
   * Creates a TICMIC identifier.
   *
   * A TICMIC is an identifier that combines the Ticker, as defined by the exchange,
   * with the MIC (Market Identifier Code) that defines the exchange.
   *
   * @param ticker the ticker, as defined by the exchange
   * @param exchangeMic the MIC code of the exchange, four characters
   * @return the TICMIC identifier
   */
  def createTicMic(ticker: String, exchangeMic: String): StandardId = {
    ArgChecker.notNull(ticker, "ticker")
    ArgChecker.notNull(exchangeMic, "exchangeMic")
    ArgChecker.isTrue(exchangeMic.length() == 4, "MIC must have 4 characters, but was {}", exchangeMic)
    StandardId.of(TICMIC_SCHEME, ticker + '@' + exchangeMic)
  }

  /**
   * Splits a TICMIC identifier.
   *
   * This method extracts the Ticker and exchange MIC from the identifier.
   *
   * @param ticMic the TICMIC identifier
   * @return the pair, holding the Ticker and MIC
   * @throws java.lang.IllegalArgumentException if unable to split the identifier
   */
  def splitTicMic(ticMic: StandardId): Pair[String, String] = {
    ArgChecker.notNull(ticMic, "ticMic")
    val splitPos = ticMic.getValue().lastIndexOf('@')
    if (splitPos < 0 || ticMic.getValue().length() < 6 || splitPos != ticMic.getValue().length() - 5) {
      throw new IllegalArgumentException("Invalid TICMIC identifier: " + ticMic)
    }
    val ticker = ticMic.getValue().substring(0, splitPos)
    val mic = ticMic.getValue().substring(splitPos + 1)
    Pair.of(ticker, mic)
  }

}
