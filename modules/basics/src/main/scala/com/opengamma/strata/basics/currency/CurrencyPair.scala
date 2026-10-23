/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.Locale
import java.util.Optional
import java.util.Set
import java.util.regex.Matcher
import java.util.regex.Pattern

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker

/**
 * An ordered pair of currencies, such as 'EUR/USD'.
 *
 * This could be used to identify a pair of currencies for quoting rates in FX deals.
 * See `FxRate` for the representation that contains a rate.
 *
 * It is recommended to define currencies in advance using the `CurrencyPair.ini` file.
 * Standard configuration includes many commonly used currency pairs.
 *
 * Only currencies listed in configuration will be returned by `getAvailablePairs()`.
 * If a pair is requested that is not defined in configuration, it will still be created,
 * however the market convention information will be generated.
 *
 * This class is immutable and thread-safe.
 */
@SerialVersionUID(1L)
final class CurrencyPair private (
    /**
     * The base currency of the pair.
     * In the pair 'AAA/BBB' the base is 'AAA'.
     */
    private val base: Currency,
    /**
     * The counter currency of the pair.
     * In the pair 'AAA/BBB' the counter is 'BBB'.
     */
    private val counter: Currency)
  extends Serializable {

  //-------------------------------------------------------------------------
  /**
   * Gets the inverse currency pair.
   *
   * The inverse pair has the same currencies but in reverse order.
   *
   * @return the inverse pair
   */
  def inverse(): CurrencyPair = {
    new CurrencyPair(counter, base)
  }

  /**
   * Checks if the currency pair contains the supplied currency as either its base or counter.
   *
   * @param currency  the currency to check against the pair
   * @return true if the currency is either the base or counter currency in the pair
   */
  def contains(currency: Currency): Boolean = {
    ArgChecker.notNull(currency, "currency")
    base.equals(currency) || counter.equals(currency)
  }

  /**
   * Finds the other currency in the pair.
   *
   * If the pair is AAA/BBB, then passing in AAA will return BBB, and passing in BBB will return AAA.
   * Passing in CCC will throw an exception.
   *
   * @param currency  the currency to check
   * @return the other currency in the pair
   * @throws java.lang.IllegalArgumentException if the specified currency is not one of those in the pair
   */
  def other(currency: Currency): Currency = {
    ArgChecker.notNull(currency, "currency")
    if (currency.equals(base)) {
      counter
    } else if (currency.equals(counter)) {
      base
    } else {
      throw new IllegalArgumentException(
          "Unable to find other currency, " + currency + " is not present in " + toString())
    }
  }

  /**
   * Checks if this currency pair is an identity pair.
   *
   * The identity pair is one where the base and counter currency are the same..
   *
   * @return true if this pair is an identity pair
   */
  def isIdentity(): Boolean = {
    base.equals(counter)
  }

  /**
   * Checks if this currency pair is the inverse of the specified pair.
   *
   * This could be used to check if an FX rate specified in one currency pair needs inverting.
   *
   * @param other  the other currency pair
   * @return true if the currency is the inverse of the specified pair
   */
  def isInverse(other: CurrencyPair): Boolean = {
    ArgChecker.notNull(other, "currencyPair")
    base.equals(other.counter) && counter.equals(other.base)
  }

  /**
   * Finds the currency pair that is a cross between this pair and the other pair.
   *
   * The cross is only returned if the two pairs contains three currencies in total,
   * such as AAA/BBB and BBB/CCC and neither pair is an identity such as AAA/AAA.
   *  - Given two pairs AAA/BBB and BBB/CCC the result will be AAA/CCC or CCC/AAA as per the market convention.
   *  - Given two pairs AAA/BBB and CCC/DDD the result will be empty.
   *  - Given two pairs AAA/AAA and AAA/BBB the result will be empty.
   *  - Given two pairs AAA/BBB and AAA/BBB the result will be empty.
   *  - Given two pairs AAA/AAA and AAA/AAA the result will be empty.
   *
   * @param other  the other currency pair
   * @return the cross currency pair, or empty if no cross currency pair can be created
   */
  def cross(other: CurrencyPair): Optional[CurrencyPair] = {
    ArgChecker.notNull(other, "other")
    if (isIdentity() || other.isIdentity() || this.equals(other) || this.equals(other.inverse())) {
      Optional.empty[CurrencyPair]()
    } else if (counter.equals(other.base)) {
      // AAA/BBB cross BBB/CCC
      Optional.of(CurrencyPair.of(base, other.counter).toConventional())
    } else if (counter.equals(other.counter)) {
      // AAA/BBB cross CCC/BBB
      Optional.of(CurrencyPair.of(base, other.base).toConventional())
    } else if (base.equals(other.base)) {
      // BBB/AAA cross BBB/CCC
      Optional.of(CurrencyPair.of(counter, other.counter).toConventional())
    } else if (base.equals(other.counter)) {
      // BBB/AAA cross CCC/BBB
      Optional.of(CurrencyPair.of(counter, other.base).toConventional())
    } else {
      Optional.empty[CurrencyPair]()
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this currency pair is a conventional currency pair.
   *
   * A market convention determines that 'EUR/USD' should be used and not 'USD/EUR'.
   * This knowledge is encoded in configuration for a standard set of pairs.
   *
   * It is possible to create two different currency pairs from any two currencies, and it is guaranteed that
   * exactly one of the pairs will be the market convention pair.
   *
   * If a currency pair is not explicitly listed in the configuration, a priority ordering of currencies
   * is used to choose base currency of the pair that is treated as conventional.
   *
   * If there is no configuration available to determine which pair is the market convention, a pair will
   * be chosen arbitrarily but deterministically. This ensures the same pair will be chosen for any two
   * currencies even if the `CurrencyPair` instances are created independently.
   *
   * @return true if the currency pair follows the market convention, false if it does not
   */
  def isConventional(): Boolean = {
    // If the pair is in the configuration file it is a market convention pair
    if (CurrencyPair.CONFIGURED.containsKey(this)) {
      return true
    }

    // If the inverse of the pair is in the configuration file then it is not a market convention pair
    if (CurrencyPair.CONFIGURED.containsKey(inverse())) {
      return false
    }

    // Get the priorities of the currencies to determine which should be the base
    val basePriority: Integer = CurrencyPair.CURRENCY_ORDERING.getOrDefault(base, Integer.valueOf(Integer.MAX_VALUE))
    val counterPriority: Integer =
      CurrencyPair.CURRENCY_ORDERING.getOrDefault(counter, Integer.valueOf(Integer.MAX_VALUE))

    // If a currency is earlier in the list it has a higher priority
    if (basePriority.intValue() < counterPriority.intValue()) {
      true
    } else if (basePriority.intValue() > counterPriority.intValue()) {
      false
    } else {
      // Neither currency is included in the list defining the ordering.
      // Use lexicographical ordering. It's arbitrary but consistent. This ensures two CurrencyPair instances
      // created independently for the same two currencies will always choose the same conventional pair.
      // The natural ordering of Currency is the same as the natural ordering of the currency code but
      // comparing the Currency instances is more efficient.
      // This is <= 0 so that a pair with two copies of the same currency is conventional
      base.compareTo(counter) <= 0
    }
  }

  /**
   * Returns the market convention currency pair for the currencies in the pair.
   *
   * If `isConventional()` is `true` this method returns `this`, otherwise
   * it returns the `inverse` pair.
   *
   * @return the market convention currency pair for the currencies in the pair
   */
  def toConventional(): CurrencyPair = {
    if (isConventional()) this else inverse()
  }

  /**
   * Returns the set of currencies contains in the pair.
   *
   * @return the set of currencies, with iteration in conventional order
   */
  def toSet(): ImmutableSet[Currency] = {
    if (isConventional()) {
      ImmutableSet.of(base, counter)
    } else {
      ImmutableSet.of(counter, base)
    }
  }

  /**
   * Gets the number of digits in the rate.
   *
   * If this rate is a conventional currency pair defined in configuration,
   * then the number of digits in a market FX rate quote is returned.
   *
   * If the currency pair is not defined in configuration the sum of the
   * minor unit digits from the two currencies is returned.
   *
   * @return the number of digits in the FX rate
   */
  def getRateDigits(): Int = {
    val digits: Integer = CurrencyPair.CONFIGURED.get(this)

    if (digits != null) {
      digits.intValue()
    } else {
      val inverseDigits: Integer = CurrencyPair.CONFIGURED.get(inverse())

      if (inverseDigits != null) {
        inverseDigits.intValue()
      } else {
        base.getMinorUnitDigits() + counter.getMinorUnitDigits()
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the base currency of the pair.
   *
   * In the pair 'AAA/BBB' the base is 'AAA'.
   *
   * @return the base currency
   */
  def getBase(): Currency = {
    base
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the counter currency of the pair.
   *
   * In the pair 'AAA/BBB' the counter is 'BBB'.
   *
   * The counter currency is also known as the ''quote currency'' or the ''variable currency''.
   *
   * @return the counter currency
   */
  def getCounter(): Currency = {
    counter
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this currency pair equals another.
   *
   * The comparison checks the two currencies.
   *
   * @param obj  the other currency pair, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[CurrencyPair]
      base.equals(other.base) &&
          counter.equals(other.counter)
    } else {
      false
    }
  }

  /**
   * Returns a suitable hash code for the currency.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    base.hashCode() ^ counter.hashCode()
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted string version of the currency pair.
   *
   * The format is '\${baseCurrency}/\${counterCurrency}'.
   *
   * @return the formatted string
   */
  @ToString
  override def toString(): String = {
    base.getCode() + "/" + counter.getCode()
  }

}

object CurrencyPair {

  /**
   * Regular expression to parse the textual format.
   * Three ASCII upper case letters, a slash, and another three ASCII upper case letters.
   */
  @static private[currency] val REGEX_FORMAT: Pattern = Pattern.compile("([A-Z]{3})/([A-Z]{3})")
  /**
   * The configured instances and associated rate digits.
   */
  @static private val CONFIGURED: ImmutableMap[CurrencyPair, Integer] = CurrencyDataLoader.loadPairs()
  /**
   * Ordering of each currency, used when choosing a market convention pair when there is no configuration.
   * The currency closer to the start of the list (with the lower ordering) is the base currency.
   */
  @static private val CURRENCY_ORDERING: ImmutableMap[Currency, Integer] = CurrencyDataLoader.loadOrdering()

  //-------------------------------------------------------------------------
  /**
   * Obtains the set of configured currency pairs.
   *
   * This contains all the currency pairs that have been defined in configuration.
   * Any currency pair instances that have been dynamically created are not included.
   *
   * @return an immutable set containing all registered currency pairs
   */
  def getAvailablePairs(): Set[CurrencyPair] = {
    CONFIGURED.keySet()
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from two currencies.
   *
   * The first currency is the base and the second is the counter.
   * The two currencies may be the same.
   *
   * @param base  the base currency
   * @param counter  the counter currency
   * @return the currency pair
   */
  @static def of(base: Currency, counter: Currency): CurrencyPair = {
    ArgChecker.notNull(base, "base")
    ArgChecker.notNull(counter, "counter")
    new CurrencyPair(base, counter)
  }

  /**
   * Parses a currency pair from a string with format AAA/BBB.
   *
   * The parsed format is '\${baseCurrency}/\${counterCurrency}'.
   * Currency parsing is case insensitive.
   *
   * @param pairStr  the currency pair as a string AAA/BBB
   * @return the currency pair
   * @throws java.lang.IllegalArgumentException if the pair cannot be parsed
   */
  @FromString
  def parse(pairStr: String): CurrencyPair = {
    ArgChecker.notNull(pairStr, "pairStr")
    val matcher: Matcher = REGEX_FORMAT.matcher(pairStr.toUpperCase(Locale.ENGLISH))
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid currency pair: " + pairStr)
    }
    val base = Currency.parse(matcher.group(1))
    val counter = Currency.parse(matcher.group(2))
    new CurrencyPair(base, counter)
  }

}
