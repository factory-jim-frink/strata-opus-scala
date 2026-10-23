/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.util.function.Supplier

import scala.annotation.static

import com.opengamma.strata.collect.Decimal
import com.opengamma.strata.collect.Messages

/**
 * A provider of FX rates.
 *
 * This provides the ability to obtain an FX rate. The interface does not mandate when the
 * rate applies, however it typically represents the current rate.
 *
 * One possible implementation is `FxMatrix`.
 *
 * Implementations do not have to be immutable, but calls to the methods must be thread-safe.
 */
trait FxRateProvider {

  //-------------------------------------------------------------------------
  /**
   * Converts an amount in a currency to an amount in a different currency using this rate.
   *
   * The currencies must both be included in the currency pair of this rate.
   *
   * @param amount  an amount in `fromCurrency`
   * @param fromCurrency  the currency of the amount
   * @param toCurrency  the currency into which the amount should be converted
   * @return the amount converted into `toCurrency`
   * @throws java.lang.IllegalArgumentException if either of the currencies aren't included in the currency pair of this rate
   */
  def convert(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double = {
    amount * fxRate(fromCurrency, toCurrency)
  }

  /**
   * Converts an amount in a currency to an amount in a different currency using this rate.
   *
   * The currencies must both be included in the currency pair of this rate.
   *
   * @param amount  an amount in `fromCurrency`
   * @param fromCurrency  the currency of the amount
   * @param toCurrency  the currency into which the amount should be converted
   * @return the amount converted into `toCurrency`
   * @throws java.lang.IllegalArgumentException if either of the currencies aren't included in the currency pair of this rate
   */
  def convert(amount: Decimal, fromCurrency: Currency, toCurrency: Currency): Decimal = {
    amount.multipliedBy(fxRate(fromCurrency, toCurrency))
  }

  /**
   * Gets the FX rate for the specified currency pair.
   *
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: `(1 * baseCurrency = fxRate * counterCurrency)`.
   *
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws java.lang.RuntimeException if no FX rate could be found
   */
  def fxRate(baseCurrency: Currency, counterCurrency: Currency): Double

  /**
   * Gets the FX rate for the specified currency pair.
   *
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: `(1 * baseCurrency = fxRate * counterCurrency)`.
   *
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the FX rate for the currency pair
   * @throws java.lang.RuntimeException if no FX rate could be found
   */
  def fxRate(currencyPair: CurrencyPair): Double = {
    fxRate(currencyPair.getBase(), currencyPair.getCounter())
  }

}

object FxRateProvider {

  /**
   * Returns an `FxRateProvider` that delays fetching its underlying provider
   * until actually necessary.
   *
   * This is typically useful where you ''may'' need a `MarketDataFxRateProvider`
   * but want to delay loading market data to construct the provider until you are sure you actually do need it.
   *
   * @param target  the supplier of the underlying provider
   * @return the provider
   */
  @static def `lazy`(target: Supplier[FxRateProvider]): FxRateProvider = {
    new LazyFxRateProvider(target)
  }

  /**
   * Returns a provider that always throws an exception.
   *
   * The provider will always throw an exception, even if the two currencies are the same.
   *
   * @return the provider
   */
  @static def noConversion(): FxRateProvider = {
    noConversionProvider()
  }

  /**
   * Returns a provider that provides minimal behavior.
   *
   * This provider returns a rate or 1 when the two currencies are the same.
   * When the two currencies differ an exception will be thrown.
   *
   * @return the provider
   */
  @static def minimal(): FxRateProvider = {
    minimalProvider()
  }

  // lambdas are created outside the static interface methods to avoid illegal interface lambda helpers
  private def noConversionProvider(): FxRateProvider = {
    (baseCurrency: Currency, counterCurrency: Currency) => {
      throw new IllegalArgumentException(Messages.format(
          "FX rate conversion is not supported, requested for {}/{}", baseCurrency, counterCurrency))
    }
  }

  private def minimalProvider(): FxRateProvider = {
    (baseCurrency: Currency, counterCurrency: Currency) => {
      if (baseCurrency.equals(counterCurrency)) {
        1d
      } else {
        throw new IllegalArgumentException(Messages.format(
            "FX rate conversion is not supported for {}/{}", baseCurrency, counterCurrency))
      }
    }
  }

}
