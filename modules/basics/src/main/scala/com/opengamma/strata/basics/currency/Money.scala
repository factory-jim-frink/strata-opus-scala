/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.math.BigDecimal
import java.util.List
import java.util.function.UnaryOperator

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.base.Splitter
import com.google.common.collect.ComparisonChain
import com.google.common.math.DoubleMath
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Decimal
import com.opengamma.strata.collect.FixedScaleDecimal

/**
 * An amount of a currency, rounded to match the currency specifications.
 *
 * This class is similar to `CurrencyAmount`, but only exposes the rounded amounts.
 * The rounding is done using `BigDecimal`, as BigDecimal.ROUND_HALF_UP. Given this operation,
 * it should be assumed that the numbers are an approximation, and not an exact figure.
 *
 * This class is immutable and thread-safe.
 */
@SerialVersionUID(1L)
class Money private (rawCurrency: Currency, rawAmount: Decimal)
  extends FxConvertible[Money] with Comparable[Money] with Serializable {

  ArgChecker.notNull(rawCurrency, "currency")
  ArgChecker.notNull(rawAmount, "amount")
  /**
   * The currency.
   *
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   */
  private val currency: Currency = rawCurrency
  /**
   * The amount of the currency.
   *
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   */
  private val amount: Decimal = rawCurrency.roundMinorUnits(rawAmount)

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   *
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   *
   * @return the currency
   */
  def getCurrency(): Currency = {
    currency
  }

  /**
   * Gets the numeric amount of the money, as a `BigDecimal`.
   *
   * For example, in the amount 'GBP 12.34' the decimal value returned is 12.34,
   * and for 'GBP 13.40' the amount returned is '13.40'.
   *
   * @return the amount, with a scale equal to the currency minor digits
   * @deprecated Use `getValue()`
   */
  @Deprecated
  def getAmount(): BigDecimal = {
    amount.toBigDecimal().setScale(currency.getMinorUnitDigits())
  }

  /**
   * Gets the numeric amount of the money.
   *
   * For example, in the amount 'GBP 12.34' the decimal value returned is '12.34',
   * whereas for 'GBP 13.40' the amount returned is '13.40'.
   *
   * @return the amount, with a scale equal to the currency minor digits
   */
  def getValue(): FixedScaleDecimal = {
    amount.toFixedScale(currency.getMinorUnitDigits())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `Money` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add, in the same currency
   * @return an amount based on this with the specified amount added
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def plus(amountToAdd: Money): Money = {
    ArgChecker.notNull(amountToAdd, "amountToAdd")
    ArgChecker.isTrue(amountToAdd.getCurrency().equals(currency), "Unable to add amounts in different currencies")
    new Money(currency, amount.plus(amountToAdd.amount))
  }

  /**
   * Returns a copy of this `Money` with the specified amount subtracted.
   *
   * This subtracts the specified amount to this monetary amount, returning a new object.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract, in the same currency
   * @return an amount based on this with the specified amount subtracted
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def minus(amountToSubtract: Money): Money = {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract")
    ArgChecker.isTrue(
        amountToSubtract.getCurrency().equals(currency), "Unable to subtract amounts in different currencies")
    new Money(currency, amount.minus(amountToSubtract.amount))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `Money` with the amount multiplied.
   *
   * This takes this amount and multiplies it by the specified value.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param valueToMultiplyBy  the scalar amount to multiply by
   * @return an amount based on this with the amount multiplied
   */
  def multipliedBy(valueToMultiplyBy: Long): Money = {
    new Money(currency, amount.multipliedBy(valueToMultiplyBy))
  }

  /**
   * Applies an operation to the amount.
   *
   * This is generally used to apply a mathematical operation to the amount.
   * For example, the operator could multiply the amount by a constant, or take the inverse.
   * {{{
   *   abs = base.mapAmount(value -> value.abs());
   * }}}
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this amount with the mapping applied to the original amount
   */
  def map(mapper: UnaryOperator[Decimal]): Money = {
    ArgChecker.notNull(mapper, "mapper")
    new Money(currency, mapper.apply(amount))
  }

  /**
   * Applies an operation to the amount.
   *
   * This is generally used to apply a mathematical operation to the amount.
   * For example, the operator could multiply the amount by a constant, or take the inverse.
   * {{{
   *   abs = base.mapAmount(value -> value.abs());
   * }}}
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this amount with the mapping applied to the original amount
   * @deprecated Use `map(UnaryOperator)`, potentially using a lambda `decimal -> decimal.mapAsBigDecimal(mapper)`
   */
  @Deprecated
  def mapAmount(mapper: UnaryOperator[BigDecimal]): Money = {
    map((decimal: Decimal) => decimal.mapAsBigDecimal(mapper))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the amount is zero.
   *
   * @return true if zero
   */
  def isZero(): Boolean = {
    amount.isZero()
  }

  /**
   * Checks if the amount is positive.
   *
   * Zero and negative amounts return false.
   *
   * @return true if positive
   */
  def isPositive(): Boolean = {
    amount.unscaledValue() > 0
  }

  /**
   * Checks if the amount is negative.
   *
   * Zero and positive amounts return false.
   *
   * @return true if negative
   */
  def isNegative(): Boolean = {
    amount.unscaledValue() < 0
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `Money` with the amount negated.
   *
   * This takes this amount and negates it. If the amount is 0.0 or -0.0 the negated amount is 0.0.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this with the amount negated
   */
  def negated(): Money = {
    if (isZero()) {
      this
    } else {
      new Money(currency, amount.negated())
    }
  }

  /**
   * Returns a copy of this `Money` with a positive amount.
   *
   * The result of this method will always be positive, where the amount is equal to `abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this where the amount is positive
   */
  def positive(): Money = {
    if (isNegative()) negated() else this
  }

  /**
   * Returns a copy of this `Money` with a negative amount.
   *
   * The result of this method will always be negative, equal to `-.abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this where the amount is negative
   */
  def negative(): Money = {
    if (isPositive()) negated() else this
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this monetary amount to the equivalent `CurrencyAmount`.
   *
   * @return the equivalent `CurrencyAmount`
   */
  def toCurrencyAmount(): CurrencyAmount = {
    CurrencyAmount.of(currency, amount.doubleValue())
  }

  /**
   * Converts this monetary amount to the equivalent `BigMoney`.
   *
   * @return the equivalent `BigMoney`
   */
  def toBigMoney(): BigMoney = {
    BigMoney.of(this)
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this amount to an equivalent amount the specified currency.
   *
   * The result will be expressed in terms of the given currency, converting
   * using the specified FX rate.
   *
   * For example, if this represents 'GBP 100' and this method is called with
   * arguments `(USD, 1.6)` then the result will be 'USD 160'.
   *
   * @param resultCurrency  the currency of the result
   * @param fxRate  the FX rate from this currency to the result currency
   * @return the converted instance, which should be expressed in the specified currency
   * @throws java.lang.IllegalArgumentException if the FX is not 1 when no conversion is required
   */
  def convertedTo(resultCurrency: Currency, fxRate: BigDecimal): Money = {
    convertedTo(resultCurrency, Decimal.of(fxRate))
  }

  /**
   * Converts this amount to an equivalent amount the specified currency.
   *
   * The result will be expressed in terms of the given currency, converting
   * using the specified FX rate.
   *
   * For example, if this represents 'GBP 100' and this method is called with
   * arguments `(USD, 1.6)` then the result will be 'USD 160'.
   *
   * @param resultCurrency  the currency of the result
   * @param fxRate  the FX rate from this currency to the result currency
   * @return the converted instance, which should be expressed in the specified currency
   * @throws java.lang.IllegalArgumentException if the FX is not 1 when no conversion is required
   */
  def convertedTo(resultCurrency: Currency, fxRate: Decimal): Money = {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate.doubleValue(), 1d, 1e-8)) {
        this
      } else {
        throw new IllegalArgumentException("FX rate must be 1 when no conversion required")
      }
    } else {
      Money.of(resultCurrency, amount.multipliedBy(fxRate))
    }
  }

  /**
   * Converts this amount to an equivalent amount in the specified currency.
   *
   * The result will be expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   *
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, in the specified currency
   * @throws java.lang.RuntimeException if no FX rate could be found
   */
  override def convertedTo(resultCurrency: Currency, rateProvider: FxRateProvider): Money = {
    if (currency.equals(resultCurrency)) {
      this
    } else {
      val converted = rateProvider.convert(amount.doubleValue(), currency, resultCurrency)
      Money.of(resultCurrency, converted)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this money to another.
   *
   * This compares currencies alphabetically, then by amount.
   *
   * @param other  the other amount
   * @return negative if less, zero if equal, positive if greater
   */
  override def compareTo(other: Money): Int = {
    ComparisonChain.start()
        .compare(currency, other.currency)
        .compare(amount, other.amount)
        .result()
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this money equals another.
   *
   * @param obj  the other amount, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[Money]
      currency.equals(other.currency) && amount.equals(other.amount)
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
    currency.hashCode() * 31 + amount.hashCode()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the amount as a string.
   *
   * The format is the currency code, followed by a space, followed by the
   * amount: '\${currency} \${amount}'.
   *
   * @return the currency amount
   */
  @ToString
  override def toString(): String = {
    currency.toString() + " " + amount.formatAtLeast(currency.getMinorUnitDigits())
  }

}

object Money {

  //-------------------------------------------------------------------------
  /**
   * Obtains a zero amount instance of `Money` for the specified currency.
   *
   * @param currency  the currency the amount is in
   * @return the zero amount instance
   */
  def zero(currency: Currency): Money = {
    of(currency, Decimal.ZERO)
  }

  /**
   * Obtains an instance of `Money` for the specified `CurrencyAmount`.
   *
   * @param currencyAmount  the instance of `CurrencyAmount` wrapping the currency and amount.
   * @return the currency amount
   */
  def of(currencyAmount: CurrencyAmount): Money = {
    new Money(currencyAmount.getCurrency(), Decimal.of(currencyAmount.getAmount()))
  }

  /**
   * Obtains an instance of `Money` for the specified `BigMoney`.
   *
   * @param money  the instance of `BigMoney` wrapping the currency and amount.
   * @return the currency amount
   */
  def of(money: BigMoney): Money = {
    new Money(money.getCurrency(), money.getValue())
  }

  /**
   * Obtains an instance of `Money` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   */
  def of(currency: Currency, amount: Double): Money = {
    new Money(currency, Decimal.of(amount))
  }

  /**
   * Obtains an instance of `Money` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of `BigDecimal`
   * @return the currency amount
   */
  def of(currency: Currency, amount: BigDecimal): Money = {
    new Money(currency, Decimal.of(amount))
  }

  /**
   * Obtains an instance of `Money` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of `Decimal`
   * @return the currency amount
   */
  def of(currency: Currency, amount: Decimal): Money = {
    new Money(currency, amount)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a `Money`.
   *
   * This parses the `toString` format of '\${currency} \${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws java.lang.IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  def parse(amountStr: String): Money = {
    ArgChecker.notNull(amountStr, "amountStr")
    val split: List[String] = Splitter.on(' ').splitToList(amountStr)
    if (split.size() != 2) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr)
    }
    try {
      val cur = Currency.parse(split.get(0))
      new Money(cur, Decimal.of(split.get(1)))
    } catch {
      case ex: RuntimeException =>
        throw new IllegalArgumentException("Unable to parse amount: " + amountStr, ex)
    }
  }

}
