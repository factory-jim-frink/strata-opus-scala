/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.List
import java.util.function.UnaryOperator

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.base.Splitter
import com.google.common.collect.ComparisonChain
import com.google.common.math.DoubleMath
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Decimal

/**
 * A monetary amount, held to a maximum of 12 decimal places.
 *
 * This class is similar to `Money`, but permits any number of decimal places.
 * The amount will always have at least the number of decimal places of the currency.
 * Trailing zeroes are stripped.
 *
 * This class is immutable and thread-safe.
 */
@SerialVersionUID(1L)
class BigMoney private (rawCurrency: Currency, rawAmount: Decimal)
  extends FxConvertible[BigMoney] with Comparable[BigMoney] with Serializable {

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
  private val amount: Decimal = rawAmount.roundToScale(12, RoundingMode.HALF_UP)

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
   * For example, in the amount 'GBP 12.345' the decimal value returned is '12.345'.
   *
   * @return the amount, with a scale from 0 to 12
   * @deprecated Use `getValue()`
   */
  @Deprecated
  def getAmount(): BigDecimal = {
    val decimal = amount.toBigDecimal()
    val digits = currency.getMinorUnitDigits()
    if (decimal.scale() < digits) {
      decimal.setScale(digits)
    } else {
      decimal
    }
  }

  /**
   * Gets the numeric amount of the money.
   *
   * For example, in the amount 'GBP 12.345' the decimal value returned is '12.345'.
   *
   * @return the amount, with a scale from 0 to 12
   */
  def getValue(): Decimal = {
    amount
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `BigMoney` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add, in the same currency
   * @return an amount based on this with the specified amount added
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def plus(amountToAdd: BigMoney): BigMoney = {
    ArgChecker.notNull(amountToAdd, "amountToAdd")
    ArgChecker.isTrue(amountToAdd.getCurrency().equals(currency), "Unable to add amounts in different currencies")
    new BigMoney(currency, amount.plus(amountToAdd.amount))
  }

  /**
   * Returns a copy of this `BigMoney` with the specified amount subtracted.
   *
   * This subtracts the specified amount to this monetary amount, returning a new object.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract, in the same currency
   * @return an amount based on this with the specified amount subtracted
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def minus(amountToSubtract: BigMoney): BigMoney = {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract")
    ArgChecker.isTrue(
        amountToSubtract.getCurrency().equals(currency), "Unable to subtract amounts in different currencies")
    new BigMoney(currency, amount.minus(amountToSubtract.amount))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `BigMoney` with the amount multiplied.
   *
   * This takes this amount and multiplies it by the specified value.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param valueToMultiplyBy  the scalar amount to multiply by
   * @return an amount based on this with the amount multiplied
   */
  def multipliedBy(valueToMultiplyBy: Long): BigMoney = {
    new BigMoney(currency, amount.multipliedBy(valueToMultiplyBy))
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
  def map(mapper: UnaryOperator[Decimal]): BigMoney = {
    ArgChecker.notNull(mapper, "mapper")
    new BigMoney(currency, mapper.apply(amount))
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
  def mapAmount(mapper: UnaryOperator[BigDecimal]): BigMoney = {
    map((decimal: Decimal) => decimal.mapAsBigDecimal(mapper))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this amount is greater than the other amount.
   *
   * @param otherAmount the other amount
   * @return true if this amount is greater than the other amount
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def isGreaterThan(otherAmount: BigMoney): Boolean = {
    ArgChecker.notNull(otherAmount, "other")
    ArgChecker.isTrue(otherAmount.getCurrency().equals(currency), "Unable to compare amounts in different currencies")
    amount.isGreaterThan(otherAmount.getValue())
  }

  /**
   * Checks if this amount is greater than or equal to the other amount.
   *
   * @param otherAmount the other amount
   * @return true if this amount is greater than or equal to the other amount
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def isGreaterThanEqualTo(otherAmount: BigMoney): Boolean = {
    ArgChecker.notNull(otherAmount, "otherAmount")
    ArgChecker.isTrue(otherAmount.getCurrency().equals(currency), "Unable to compare amounts in different currencies")
    amount.isGreaterThanEqualTo(otherAmount.getValue())
  }

  /**
   * Checks if this amount is less than the other amount.
   *
   * @param otherAmount the other amount
   * @return true if this amount is less than the other amount
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def isLessThan(otherAmount: BigMoney): Boolean = {
    ArgChecker.notNull(otherAmount, "otherAmount")
    ArgChecker.isTrue(otherAmount.getCurrency().equals(currency), "Unable to compare amounts in different currencies")
    amount.isLessThan(otherAmount.getValue())
  }

  /**
   * Checks if this amount is less than or equal to the other amount.
   *
   * @param otherAmount the other amount
   * @return true if this amount is less than or equal to the other amount
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def isLessThanEqualTo(otherAmount: BigMoney): Boolean = {
    ArgChecker.notNull(otherAmount, "otherAmount")
    ArgChecker.isTrue(otherAmount.getCurrency().equals(currency), "Unable to compare amounts in different currencies")
    amount.isLessThanEqualTo(otherAmount.getValue())
  }

  /**
   * Returns a copy of this `BigMoney` with the amount rounded to the specified scale.
   *
   * The underlying amount will be rounded using `Decimal#roundToScale(int, RoundingMode)`
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param desiredScale  the scale, positive for decimal places, negative to round the integer-part
   * @param roundingMode  the rounding mode
   * @return the rounded amount
   * @throws java.lang.IllegalArgumentException if the scale is -18 or less
   */
  def roundToScale(desiredScale: Int, roundingMode: RoundingMode): BigMoney = {
    new BigMoney(currency, amount.roundToScale(desiredScale, roundingMode))
  }

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
   * Returns a copy of this `BigMoney` with the amount negated.
   *
   * This takes this amount and negates it.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this with the amount negated
   */
  def negated(): BigMoney = {
    if (isZero()) {
      this
    } else {
      new BigMoney(currency, amount.negated())
    }
  }

  /**
   * Returns a copy of this `BigMoney` with a positive amount.
   *
   * The result of this method will always be positive, where the amount is equal to `abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this where the amount is positive
   */
  def positive(): BigMoney = {
    if (isNegative()) negated() else this
  }

  /**
   * Returns a copy of this `BigMoney` with a negative amount.
   *
   * The result of this method will always be negative, equal to `-.abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this where the amount is negative
   */
  def negative(): BigMoney = {
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
   * Converts this monetary amount to the equivalent `Money`.
   *
   * @return the equivalent `Money`
   */
  def toMoney(): Money = {
    Money.of(this)
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
  def convertedTo(resultCurrency: Currency, fxRate: BigDecimal): BigMoney = {
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
  def convertedTo(resultCurrency: Currency, fxRate: Decimal): BigMoney = {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate.doubleValue(), 1d, 1e-8)) {
        this
      } else {
        throw new IllegalArgumentException("FX rate must be 1 when no conversion required")
      }
    } else {
      BigMoney.of(resultCurrency, amount.multipliedBy(fxRate))
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
  override def convertedTo(resultCurrency: Currency, rateProvider: FxRateProvider): BigMoney = {
    if (currency.equals(resultCurrency)) {
      this
    } else {
      val converted = rateProvider.convert(amount.doubleValue(), currency, resultCurrency)
      BigMoney.of(resultCurrency, converted)
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
  override def compareTo(other: BigMoney): Int = {
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
      val other = obj.asInstanceOf[BigMoney]
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

object BigMoney {

  //-------------------------------------------------------------------------
  /**
   * Obtains a zero amount instance of `BigMoney` for the specified currency.
   *
   * @param currency  the currency the amount is in
   * @return the zero amount instance
   */
  def zero(currency: Currency): BigMoney = {
    of(currency, Decimal.ZERO)
  }

  /**
   * Obtains an instance of `BigMoney` for the specified `CurrencyAmount`.
   *
   * @param currencyAmount  the instance of `CurrencyAmount` wrapping the currency and amount.
   * @return the currency amount
   */
  def of(currencyAmount: CurrencyAmount): BigMoney = {
    new BigMoney(currencyAmount.getCurrency(), Decimal.of(currencyAmount.getAmount()))
  }

  /**
   * Obtains an instance of `BigMoney` for the specified `Money`.
   *
   * @param money  the instance of `Money` wrapping the currency and amount.
   * @return the currency amount
   */
  def of(money: Money): BigMoney = {
    new BigMoney(money.getCurrency(), money.getValue().decimal())
  }

  /**
   * Obtains an instance of `BigMoney` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   */
  def of(currency: Currency, amount: Double): BigMoney = {
    new BigMoney(currency, Decimal.of(amount))
  }

  /**
   * Obtains an instance of `BigMoney` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of `BigDecimal`
   * @return the currency amount
   */
  def of(currency: Currency, amount: BigDecimal): BigMoney = {
    new BigMoney(currency, Decimal.of(amount))
  }

  /**
   * Obtains an instance of `BigMoney` for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of `Decimal`
   * @return the currency amount
   */
  def of(currency: Currency, amount: Decimal): BigMoney = {
    new BigMoney(currency, amount)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a `BigMoney`.
   *
   * This parses the `toString` format of '\${currency} \${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws java.lang.IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  def parse(amountStr: String): BigMoney = {
    ArgChecker.notNull(amountStr, "amountStr")
    val split: List[String] = Splitter.on(' ').splitToList(amountStr)
    if (split.size() != 2) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr)
    }
    try {
      val cur = Currency.parse(split.get(0))
      new BigMoney(cur, Decimal.of(split.get(1)))
    } catch {
      case ex: RuntimeException =>
        throw new IllegalArgumentException("Unable to parse amount: " + amountStr, ex)
    }
  }

}
