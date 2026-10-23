/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.util.function.Supplier

import com.google.common.base.Suppliers
import com.opengamma.strata.collect.ArgChecker

/**
 * An `FxRateProvider` that delays fetching its underlying provider
 * until actually necessary.
 *
 * This is typically useful where you ''may'' need a `MarketDataFxRateProvider`
 * but want to delay loading market data to construct the provider until you are sure you actually do need it.
 */
private[currency] class LazyFxRateProvider private[currency] (rawTarget: Supplier[FxRateProvider])
  extends FxRateProvider {

  ArgChecker.notNull(rawTarget, "target")
  /**
   * The supplier of the underlying provider.
   */
  private val target: Supplier[FxRateProvider] = Suppliers.memoize[FxRateProvider](() => rawTarget.get())

  override def convert(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double = {
    if (fromCurrency.equals(toCurrency)) {
      amount
    } else {
      target.get().convert(amount, fromCurrency, toCurrency)
    }
  }

  override def fxRate(baseCurrency: Currency, counterCurrency: Currency): Double = {
    fxRate(CurrencyPair.of(baseCurrency, counterCurrency))
  }

  override def fxRate(currencyPair: CurrencyPair): Double = {
    if (currencyPair.getBase().equals(currencyPair.getCounter())) {
      1d
    } else {
      target.get().fxRate(currencyPair)
    }
  }
}
