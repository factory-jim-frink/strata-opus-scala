/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value

import java.math.BigDecimal

import scala.annotation.static

import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.collect.Decimal

/**
 * A convention defining how to round a number.
 *
 * This defines a standard mechanism for rounding a `double`, `BigDecimal` or  `Decimal`.
 * Since financial instruments have different and complex conventions, rounding is extensible.
 *
 * Note that rounding a `double` is not straightforward as floating point
 * numbers are based on a binary representation, not a decimal one.
 * For example, the value 0.1 cannot be exactly represented in a `double`.
 *
 * The standard implementation is `HalfUpRounding`.
 * Additional implementations may be added by implementing this interface.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait Rounding {

  //-------------------------------------------------------------------------
  /**
   * Rounds the specified value according to the rules of the convention.
   *
   * @param value  the value to be rounded
   * @return the rounded value
   */
  def round(value: Double): Double = {
    round(BigDecimal.valueOf(value)).doubleValue()
  }

  /**
   * Rounds the specified value according to the rules of the convention.
   *
   * @param value  the value to be rounded
   * @return the rounded value
   */
  def round(value: BigDecimal): BigDecimal

  /**
   * Rounds the specified value according to the rules of the convention.
   *
   * @param value  the value to be rounded
   * @return the rounded value
   */
  def round(value: Decimal): Decimal = {
    Decimal.of(round(value.toBigDecimal()))
  }

}

object Rounding {

  /**
   * Obtains an instance that performs no rounding.
   *
   * @return the rounding convention
   */
  @static def none(): Rounding = {
    NoRounding.INSTANCE
  }

  /**
   * Obtains an instance that rounds to the number of minor units in the currency.
   *
   * This returns a convention that rounds for the specified currency.
   * Rounding follows the normal `RoundingMode#HALF_UP` convention.
   *
   * @param currency  the currency
   * @return the rounding convention
   */
  @static def of(currency: Currency): Rounding = {
    HalfUpRounding.ofDecimalPlaces(currency.getMinorUnitDigits())
  }

  /**
   * Obtains an instance that rounds to the specified number of decimal places.
   *
   * This returns a convention that rounds to the specified number of decimal places.
   * Rounding follows the normal `RoundingMode#HALF_UP` convention.
   *
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @return the rounding convention
   * @throws java.lang.IllegalArgumentException if the decimal places is invalid
   */
  @static def ofDecimalPlaces(decimalPlaces: Int): Rounding = {
    HalfUpRounding.ofDecimalPlaces(decimalPlaces)
  }

  /**
   * Obtains an instance from the number of decimal places and fraction.
   *
   * This returns a convention that rounds to a fraction of the specified number of decimal places.
   * Rounding follows the normal `RoundingMode#HALF_UP` convention.
   *
   * For example, to round to the nearest 1/32nd of the 4th decimal place, call
   * this method with the arguments 4 and 32.
   *
   * @param decimalPlaces  the number of decimal places to round to, from 0 to 255 inclusive
   * @param fraction  the fraction of the last decimal place, such as 32 for 1/32, from 0 to 255 inclusive
   * @return the rounding convention
   * @throws java.lang.IllegalArgumentException if the decimal places or fraction is invalid
   */
  @static def ofFractionalDecimalPlaces(decimalPlaces: Int, fraction: Int): Rounding = {
    HalfUpRounding.ofFractionalDecimalPlaces(decimalPlaces, fraction)
  }

}
