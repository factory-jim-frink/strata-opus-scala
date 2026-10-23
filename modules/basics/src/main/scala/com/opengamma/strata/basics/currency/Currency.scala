/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.Set
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.base.CharMatcher
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Decimal

/**
 * A unit of currency.
 *
 * This class represents a unit of currency such as the British Pound, Euro or US Dollar.
 * The currency is represented by a three letter code, intended to be ISO-4217.
 *
 * It is recommended to define currencies in advance using the `Currency.ini` file.
 * Standard configuration includes many commonly used currencies.
 *
 * Only currencies listed in configuration will be returned by `getAvailableCurrencies()`.
 * If a currency is requested that is not defined in configuration, it will still be created,
 * however it will have the default value of zero for the minor units and 'USD' for
 * the triangulation currency.
 *
 * This class is immutable and thread-safe.
 */
@SerialVersionUID(1L)
final class Currency private[currency] (
    /**
     * The currency code.
     */
    private val code: String,
    fractionDigits: Int,
    /**
     * The triangulation currency.
     * Due to initialization ordering, cannot guarantee that USD/EUR is loaded first, so this must be a string.
     */
    @transient private val triangulationCurrency: String)
  extends Comparable[Currency] with Serializable {

  /**
   * The number of fraction digits, such as 2 for cents in the dollar.
   */
  @transient private val minorUnitDigits: Int = fractionDigits
  /**
   * The cached hash code.
   */
  // total universe is (26 * 26 * 26) codes, which can provide a unique hash code
  @transient private val cachedHashCode: Int =
    ((code.charAt(0) - 64) << 16) + ((code.charAt(1) - 64) << 8) + (code.charAt(2) - 64)

  /**
   * Ensure singleton on deserialization.
   *
   * @return the singleton
   */
  private def readResolve(): Object = {
    Currency.of(code)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the three letter ISO code.
   *
   * @return the three letter ISO code
   */
  def getCode(): String = {
    code
  }

  /**
   * Gets the number of digits in the minor unit.
   *
   * For example, 'USD' will return 2, indicating that there are two digits,
   * corresponding to cents in the dollar.
   *
   * @return the number of fraction digits
   */
  def getMinorUnitDigits(): Int = {
    minorUnitDigits
  }

  /**
   * Gets the preferred triangulation currency.
   *
   * When obtaining a market quote for a currency, the triangulation currency
   * is used if no direct rate can be found.
   * For example, there is no direct rate for 'CZK/SGD'. Instead 'CZK' might be defined to
   * triangulate via 'EUR' and 'SGD' with 'USD'. Since the three rates, 'CZK/EUR', 'EUR/USD'
   * and 'USD/SGD' can be obtained, a rate can be determined for 'CZK/SGD'.
   * Note that most currencies triangulate via 'USD'.
   *
   * @return the triangulation currency
   */
  def getTriangulationCurrency(): Currency = {
    Currency.of(triangulationCurrency)
  }

  //-------------------------------------------------------------------------
  /**
   * Rounds the specified amount according to the minor units.
   *
   * For example, 'USD' has 2 minor digits, so 63.347 will be rounded to 63.35.
   *
   * @param amount  the amount to round
   * @return the rounded amount
   */
  def roundMinorUnits(amount: Double): Double = {
    roundMinorUnits(BigDecimal.valueOf(amount)).doubleValue()
  }

  /**
   * Rounds the specified amount according to the minor units.
   *
   * For example, 'USD' has 2 minor digits, so 63.347 will be rounded to 63.35.
   *
   * @param amount  the amount to round
   * @return the rounded amount
   */
  def roundMinorUnits(amount: BigDecimal): BigDecimal = {
    amount.setScale(minorUnitDigits, RoundingMode.HALF_UP)
  }

  /**
   * Rounds the specified amount according to the minor units.
   *
   * For example, 'USD' has 2 minor digits, so 63.347 will be rounded to 63.35.
   *
   * @param amount  the amount to round
   * @return the rounded amount
   */
  def roundMinorUnits(amount: Decimal): Decimal = {
    amount.roundToScale(minorUnitDigits, RoundingMode.HALF_UP)
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this currency to another.
   *
   * The comparison sorts alphabetically by the three letter currency code.
   *
   * @param other  the other currency
   * @return negative if less, zero if equal, positive if greater
   */
  override def compareTo(other: Currency): Int = {
    // hash code is unique and ordered so can be used for compareTo
    cachedHashCode - other.cachedHashCode
  }

  /**
   * Checks if this currency equals another currency.
   *
   * The comparison checks the three letter currency code.
   *
   * @param obj  the other currency, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: Currency => equals(other)
        case _ => false
      }
    }
  }

  // called by CurrencyAmount
  private[currency] def equals(other: Currency): Boolean = {
    // hash code is unique so can be used for equals
    other.cachedHashCode == cachedHashCode
  }

  /**
   * Returns a suitable hash code for the currency.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    cachedHashCode
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string representation of the currency, which is the three letter code.
   *
   * @return the three letter currency code
   */
  @ToString
  override def toString(): String = {
    code
  }

}

object Currency {

  /**
   * The matcher for the code.
   */
  @static private[currency] val CODE_MATCHER: CharMatcher = CharMatcher.inRange('A', 'Z')
  /**
   * The configured instances.
   */
  @static private val CONFIGURED: ImmutableMap[String, Currency] =
    CurrencyDataLoader.loadCurrencies(false)
  /**
   * A cache of dynamically created instances, initialized with some historic currencies.
   */
  @static private val DYNAMIC: ConcurrentMap[String, Currency] =
    new ConcurrentHashMap[String, Currency](CurrencyDataLoader.loadCurrencies(true))

  // a selection of commonly traded, stable currencies
  /**
   * The currency 'USD' - United States Dollar.
   */
  @static final val USD: Currency = of("USD")
  /**
   * The currency 'EUR' - Euro.
   */
  @static final val EUR: Currency = of("EUR")
  /**
   * The currency 'JPY' - Japanese Yen.
   */
  @static final val JPY: Currency = of("JPY")
  /**
   * The currency 'GBP' - British pound.
   */
  @static final val GBP: Currency = of("GBP")
  /**
   * The currency 'CHF' - Swiss Franc.
   */
  @static final val CHF: Currency = of("CHF")
  /**
   * The currency 'AUD' - Australian Dollar.
   */
  @static final val AUD: Currency = of("AUD")
  /**
   * The currency 'CAD' - Canadian Dollar.
   */
  @static final val CAD: Currency = of("CAD")
  /**
   * The currency 'NZD' - New Zealand Dollar.
   */
  @static final val NZD: Currency = of("NZD")

  // a selection of other currencies
  /**
   * The currency 'AED' - UAE Dirham.
   */
  @static final val AED: Currency = of("AED")
  /**
   * The currency 'ARS' - Argentine Peso.
   */
  @static final val ARS: Currency = of("ARS")
  /**
   * The currency 'BGN' - Bulgarian Lev.
   */
  @static final val BGN: Currency = of("BGN")
  /**
   * The currency 'BHD' - Bahraini Dinar.
   */
  @static final val BHD: Currency = of("BHD")
  /**
   * The currency 'BRL' - Brazilian Real.
   */
  @static final val BRL: Currency = of("BRL")
  /**
   * The currency 'CLP' - Chilean Peso.
   */
  @static final val CLP: Currency = of("CLP")
  /**
   * The currency 'CNH' - Chinese Offshore Yuan.
   */
  @static final val CNH: Currency = of("CNH")
  /**
   * The currency 'CNY' - Chinese Onshore Yuan.
   */
  @static final val CNY: Currency = of("CNY")
  /**
   * The currency 'COP' - Colombian Peso.
   */
  @static final val COP: Currency = of("COP")
  /**
   * The currency 'CZK' - Czeck Krona.
   */
  @static final val CZK: Currency = of("CZK")
  /**
   * The currency 'DKK' - Danish Krone.
   */
  @static final val DKK: Currency = of("DKK")
  /**
   * The currency 'EGP' - Egyptian Pound.
   */
  @static final val EGP: Currency = of("EGP")
  /**
   * The currency 'HKD' - Hong Kong Dollar.
   */
  @static final val HKD: Currency = of("HKD")
  /**
   * The currency 'HRK' - Croatian Kuna.
   */
  @static final val HRK: Currency = of("HRK")
  /**
   * The currency 'HUF' = Hugarian Forint.
   */
  @static final val HUF: Currency = of("HUF")
  /**
   * The currency 'IDR' = Indonesian Rupiah.
   */
  @static final val IDR: Currency = of("IDR")
  /**
   * The currency 'ILS' = Israeli Shekel.
   */
  @static final val ILS: Currency = of("ILS")
  /**
   * The currency 'INR' = Indian Rupee.
   */
  @static final val INR: Currency = of("INR")
  /**
   * The currency 'ISK' = Icelandic Krone.
   */
  @static final val ISK: Currency = of("ISK")
  /**
   * The currency 'KRW' = South Korean Won.
   */
  @static final val KRW: Currency = of("KRW")
  /**
   * The currency 'KZT' = Kazakhstani Tenge.
   */
  @static final val KZT: Currency = of("KZT")
  /**
   * The currency 'MAD' - Moroccan Dirham.
   */
  @static final val MAD: Currency = of("MAD")
  /**
   * The currency 'MXN' - Mexican Peso.
   */
  @static final val MXN: Currency = of("MXN")
  /**
   * The currency 'MYR' - Malaysian Ringgit.
   */
  @static final val MYR: Currency = of("MYR")
  /**
   * The currency 'NOK' - Norwegian Krone.
   */
  @static final val NOK: Currency = of("NOK")
  /**
   * The currency 'OMR' - Omani Rial.
   */
  @static final val OMR: Currency = of("OMR")
  /**
   * The currency 'PEN' - Peruvian Nuevo Sol.
   */
  @static final val PEN: Currency = of("PEN")
  /**
   * The currency 'PHP' - Philippine Peso.
   */
  @static final val PHP: Currency = of("PHP")
  /**
   * The currency 'PKR' - Pakistani Rupee.
   */
  @static final val PKR: Currency = of("PKR")
  /**
   * The currency 'PLN' - Polish Zloty.
   */
  @static final val PLN: Currency = of("PLN")
  /**
   * The currency 'QAR' - Qatari Riyal.
   */
  @static final val QAR: Currency = of("QAR")
  /**
   * The currency 'RON' - Romanian New Leu.
   */
  @static final val RON: Currency = of("RON")
  /**
   * The currency 'RUB' - Russian Ruble.
   */
  @static final val RUB: Currency = of("RUB")
  /**
   * The currency 'SAR' - Saudi Riyal.
   */
  @static final val SAR: Currency = of("SAR")
  /**
   * The currency 'SEK' - Swedish Krona.
   */
  @static final val SEK: Currency = of("SEK")
  /**
   * The currency 'SGD' - Singapore Dollar.
   */
  @static final val SGD: Currency = of("SGD")
  /**
   * The currency 'THB' - Thai Baht.
   */
  @static final val THB: Currency = of("THB")
  /**
   * The currency 'TRY' - Turkish Lira.
   */
  @static final val TRY: Currency = of("TRY")
  /**
   * The currency 'TWD' - New Taiwan Dollar.
   */
  @static final val TWD: Currency = of("TWD")
  /**
   * The currency 'UAH' - Ukrainian Hryvnia.
   */
  @static final val UAH: Currency = of("UAH")
  /**
   * The currency 'VND' - Vietnamese Dong.
   */
  @static final val VND: Currency = of("VND")
  /**
   * The currency 'ZAR' - South African Rand.
   */
  @static final val ZAR: Currency = of("ZAR")

  // special cases
  /**
   * The currency 'XXX' - No applicable currency.
   */
  @static final val XXX: Currency = of("XXX")
  /**
   * The currency 'XAG' - Silver (troy ounce).
   */
  @static final val XAG: Currency = of("XAG")
  /**
   * The currency 'XAU' - Gold (troy ounce).
   */
  @static final val XAU: Currency = of("XAU")
  /**
   * The currency 'XPD' - Paladium (troy ounce).
   */
  @static final val XPD: Currency = of("XPD")
  /**
   * The currency 'XPT' - Platinum (troy ounce).
   */
  @static final val XPT: Currency = of("XPT")

  //-------------------------------------------------------------------------
  /**
   * Obtains the set of configured currencies.
   *
   * This contains all the currencies that have been defined in configuration.
   * Any currency instances that have been dynamically created are not included.
   *
   * @return an immutable set containing all registered currencies
   */
  def getAvailableCurrencies(): Set[Currency] = {
    ImmutableSet.copyOf[Currency](CONFIGURED.values())
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance for the specified ISO-4217 three letter currency code.
   *
   * A currency is uniquely identified by ISO-4217 three letter code.
   * Currencies should be defined in configuration before they can be used.
   * If the requested currency is not defined in configuration, it will still be created,
   * however it will have the default value of zero for the minor units and 'USD' for
   * the triangulation currency.
   *
   * @param currencyCode  the three letter currency code, ASCII and upper case
   * @return the singleton instance
   * @throws java.lang.IllegalArgumentException if the currency code is invalid
   */
  @FromString
  @static def of(currencyCode: String): Currency = {
    ArgChecker.notNull(currencyCode, "currencyCode")
    val currency = CONFIGURED.get(currencyCode)
    if (currency == null) {
      addCode(currencyCode)
    } else {
      currency
    }
  }

  // add code
  private def addCode(currencyCode: String): Currency = {
    ArgChecker.matches(CODE_MATCHER, 3, 3, currencyCode, "currencyCode", "[A-Z][A-Z][A-Z]")
    DYNAMIC.computeIfAbsent(currencyCode, (code: String) => new Currency(code, 0, "USD"))
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a string to obtain a `Currency`.
   *
   * The parse is identical to `of(String)` except that it will convert
   * letters to upper case first.
   * If the requested currency is not defined in configuration, it will still be created,
   * however it will have the default value of zero for the minor units and 'USD' for
   * the triangulation currency.
   *
   * @param currencyCode  the three letter currency code, ASCII
   * @return the singleton instance
   * @throws java.lang.IllegalArgumentException if the currency code is invalid
   */
  def parse(currencyCode: String): Currency = {
    ArgChecker.notNull(currencyCode, "currencyCode")
    of(currencyCode.toUpperCase(Locale.ENGLISH))
  }

}
