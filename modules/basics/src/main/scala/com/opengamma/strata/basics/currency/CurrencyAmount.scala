/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.function.DoubleUnaryOperator

import org.joda.beans.JodaBeanUtils
import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.collect.ComparisonChain
import com.google.common.math.DoubleMath
import com.opengamma.strata.collect.ArgChecker

/**
 * An amount of a currency.
 *
 * This class represents a `double` amount associated with a currency.
 * It is specifically named "CurrencyAmount" and not "Money" to indicate that
 * it simply holds a currency and an amount. By contrast, naming it "Money"
 * would imply it was a suitable choice for accounting purposes, which it is not.
 * This design approach has been chosen primarily for performance reasons.
 * Using a `BigDecimal` is markedly slower.
 * See the `Money` class for the alternative that uses `BigDecimal`.
 *
 * A `double` is a 64 bit floating point value suitable for most calculations.
 * Floating point maths is
 * [[http://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html inexact]]
 * due to the conflict between binary and decimal arithmetic.
 * As such, there is the potential for data loss at the margins.
 * For example, adding the `double` values `0.1d` and `0.2d`
 * results in `0.30000000000000004` rather than `0.3`.
 * As can be seen, the level of error is small, hence providing this class is
 * used appropriately, the use of `double` is acceptable.
 * For example, using this class to provide a meaningful result type after
 * calculations have completed would be an appropriate use.
 *
 * This class is immutable and thread-safe.
 */
@SerialVersionUID(1L)
final class CurrencyAmount private (rawCurrency: Currency, rawAmount: Double)
  extends FxConvertible[CurrencyAmount] with Comparable[CurrencyAmount] with Serializable {

  /**
   * The currency.
   *
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   */
  private val currency: Currency = ArgChecker.notNull(rawCurrency, "currency")
  /**
   * The amount of the currency.
   *
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   */
  private val amount: Double = ArgChecker.notNaN(rawAmount + 0d, "amount")  // this weird addition removes negative zero

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
   * Gets the amount of the currency.
   *
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   *
   * @return the amount
   */
  def getAmount(): Double = {
    amount
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `CurrencyAmount` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add, in the same currency
   * @return an amount based on this with the specified amount added
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def plus(amountToAdd: CurrencyAmount): CurrencyAmount = {
    ArgChecker.notNull(amountToAdd, "amountToAdd")
    ArgChecker.isTrue(amountToAdd.getCurrency().equals(currency), "Unable to add amounts in different currencies")
    plus(amountToAdd.getAmount())
  }

  /**
   * Returns a copy of this `CurrencyAmount` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add
   * @return an amount based on this with the specified amount added
   */
  def plus(amountToAdd: Double): CurrencyAmount = {
    new CurrencyAmount(currency, amount + amountToAdd)
  }

  /**
   * Returns a copy of this `CurrencyAmount` with the specified amount subtracted.
   *
   * This subtracts the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract, in the same currency
   * @return an amount based on this with the specified amount subtracted
   * @throws java.lang.IllegalArgumentException if the currencies are not equal
   */
  def minus(amountToSubtract: CurrencyAmount): CurrencyAmount = {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract")
    ArgChecker.isTrue(
        amountToSubtract.getCurrency().equals(currency), "Unable to subtract amounts in different currencies")
    minus(amountToSubtract.getAmount())
  }

  /**
   * Returns a copy of this `CurrencyAmount` with the specified amount subtracted.
   *
   * This subtracts the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract
   * @return an amount based on this with the specified amount subtracted
   */
  def minus(amountToSubtract: Double): CurrencyAmount = {
    new CurrencyAmount(currency, amount - amountToSubtract)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `CurrencyAmount` with the amount multiplied.
   *
   * This takes this amount and multiplies it by the specified value.
   * The multiplication simply uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param valueToMultiplyBy  the scalar amount to multiply by
   * @return an amount based on this with the amount multiplied
   */
  def multipliedBy(valueToMultiplyBy: Double): CurrencyAmount = {
    new CurrencyAmount(currency, amount * valueToMultiplyBy)
  }

  /**
   * Applies an operation to the amount.
   *
   * This is generally used to apply a mathematical operation to the amount.
   * For example, the operator could multiply the amount by a constant, or take the inverse.
   * {{{
   *   multiplied = base.mapAmount(value -> (value < 0 ? 0 : value * 3));
   * }}}
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this amount with the mapping applied to the original amount
   */
  def mapAmount(mapper: DoubleUnaryOperator): CurrencyAmount = {
    ArgChecker.notNull(mapper, "mapper")
    new CurrencyAmount(currency, mapper.applyAsDouble(amount))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the amount is zero.
   *
   * @return true if zero
   */
  def isZero(): Boolean = {
    amount == 0
  }

  /**
   * Checks if the amount is positive.
   *
   * Zero and negative amounts return false.
   *
   * @return true if positive
   */
  def isPositive(): Boolean = {
    amount > 0
  }

  /**
   * Checks if the amount is negative.
   *
   * Zero and positive amounts return false.
   *
   * @return true if negative
   */
  def isNegative(): Boolean = {
    amount < 0
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `CurrencyAmount` with the amount negated.
   *
   * This takes this amount and negates it. If the amount is 0.0 or -0.0 the negated amount is 0.0.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this with the amount negated
   */
  def negated(): CurrencyAmount = {
    new CurrencyAmount(currency, -amount)
  }

  /**
   * Returns a copy of this `CurrencyAmount` with a positive amount.
   *
   * The result of this method will always be positive, where the amount is equal to `Math.abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return a currency amount based on this where the amount is positive
   */
  def positive(): CurrencyAmount = {
    if (amount < 0) negated() else this
  }

  /**
   * Returns a copy of this `CurrencyAmount` with a negative amount.
   *
   * The result of this method will always be negative, equal to `-Math.abs(amount)`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return a currency amount based on this where the amount is negative
   */
  def negative(): CurrencyAmount = {
    if (amount > 0) negated() else this
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this monetary amount to the equivalent `Money`.
   *
   * An instance of `Money` only stores the amount to the number of decimal places of the currency.
   * As such, this method may result in a loss of precision.
   *
   * @return the equivalent `Money`
   */
  def toMoney(): Money = {
    Money.of(currency, amount)
  }

  /**
   * Converts this monetary amount to the equivalent `BigMoney`.
   *
   * @return the equivalent `BigMoney`
   */
  def toBigMoney(): BigMoney = {
    BigMoney.of(currency, amount)
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
  def convertedTo(resultCurrency: Currency, fxRate: Double): CurrencyAmount = {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate, 1d, 1e-8)) {
        this
      } else {
        throw new IllegalArgumentException("FX rate must be 1 when no conversion required")
      }
    } else {
      CurrencyAmount.of(resultCurrency, amount * fxRate)
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
  override def convertedTo(resultCurrency: Currency, rateProvider: FxRateProvider): CurrencyAmount = {
    if (currency.equals(resultCurrency)) {
      this
    } else {
      val converted = rateProvider.convert(amount, currency, resultCurrency)
      CurrencyAmount.of(resultCurrency, converted)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this currency amount to another.
   *
   * This compares currencies alphabetically, then by amount.
   *
   * @param other  the other amount
   * @return negative if less, zero if equal, positive if greater
   */
  override def compareTo(other: CurrencyAmount): Int = {
    ComparisonChain.start()
        .compare(currency, other.currency)
        .compare(amount, other.amount)
        .result()
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this currency amount equals another.
   *
   * @param obj  the other amount, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[CurrencyAmount]
      currency.equals(other.currency) &&
          JodaBeanUtils.equal(amount, other.amount)
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
    currency.hashCode() * 31 + JodaBeanUtils.hashCode(amount)
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
    currency.toString() + " " +
        (if (DoubleMath.isMathematicalInteger(amount)) java.lang.Long.toString(amount.toLong)
        else java.lang.Double.toString(amount))
  }

}

object CurrencyAmount {

  //-------------------------------------------------------------------------
  /**
   * Obtains a zero amount instance of `CurrencyAmount` for the specified currency.
   *
   * @param currency  the currency the amount is in
   * @return the zero amount instance
   */
  def zero(currency: Currency): CurrencyAmount = {
    of(currency, 0d)
  }

  /**
   * Obtains an instance of `CurrencyAmount` for the specified currency and amount.
   *
   * If the negative form of zero is passed in, it will be converted to positive zero.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   * @throws java.lang.IllegalArgumentException if the amount is `NaN`
   */
  def of(currency: Currency, amount: Double): CurrencyAmount = {
    new CurrencyAmount(currency, amount)
  }

  /**
   * Obtains an instance of `CurrencyAmount` for the specified ISO-4217
   * three letter currency code and amount.
   *
   * A currency is uniquely identified by ISO-4217 three letter code.
   * This method creates the currency if it is not known.
   *
   * If the negative form of zero is passed in, it will be converted to positive zero.
   *
   * @param currencyCode  the three letter currency code, ASCII and upper case
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   * @throws java.lang.IllegalArgumentException if the currency code is invalid, or if the amount is `NaN`
   */
  def of(currencyCode: String, amount: Double): CurrencyAmount = {
    of(Currency.of(currencyCode), amount)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a `CurrencyAmount`.
   *
   * This parses the `toString` format of '\${currency} \${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws java.lang.IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  def parse(amountStr: String): CurrencyAmount = {
    // this method has had some performance optimizations applied
    if (amountStr == null || amountStr.length() <= 4 || amountStr.charAt(3) != ' ') {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr)
    }

    val currencyCode = amountStr.substring(0, 3)
    val doubleString = amountStr.substring(4)
    if (doubleString.indexOf(' ') != -1) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr)
    }

    try {
      val cur = Currency.parse(currencyCode)
      val amount = java.lang.Double.parseDouble(doubleString)
      new CurrencyAmount(cur, amount)
    } catch {
      case ex: RuntimeException =>
        throw new IllegalArgumentException("Unable to parse amount: " + amountStr, ex)
    }
  }

}
