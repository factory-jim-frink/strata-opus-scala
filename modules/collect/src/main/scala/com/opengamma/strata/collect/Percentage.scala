/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.math.RoundingMode
import java.util.function.UnaryOperator

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

/**
 * A percentage amount, with a maximum of 10 decimal places.
 *
 * A number has three standard representations in finance:
 *  - decimal form - the form needed for mathematical calculations, as used by most parts of Strata
 *  - percentage - where 1.2% is the same as the decimal 0.012
 *  - basis points - where 125bps is the same as the decimal 0.0125
 *
 * This class allows the use of percentage form to be explicit.
 */
final class Percentage private (percentage: Decimal) extends Comparable[Percentage] {

  ArgChecker.notNull(percentage, "percentage")
  /**
   * The percentage.
   */
  private val amount: Decimal = percentage.roundToScale(Percentage.MAX_SCALE, RoundingMode.HALF_UP)

  //-----------------------------------------------------------------------
  /**
   * Gets the value in percentage form as a `Decimal`.
   *
   * A value of 1.5% will return 1.5.
   *
   * @return the value in percentage form, with a maximum of 10 decimal places
   */
  def valuePercent(): Decimal = {
    amount
  }

  /**
   * Converts this percentage to mathematical decimal form.
   *
   * A value of 1.5% will return 0.015.
   *
   * @return the amount in mathematical decimal form
   */
  def toDecimalForm(): Decimal = {
    amount.movePoint(-2)
  }

  /**
   * Converts this percentage to the equivalent basis points.
   *
   * A value of 1.5% will return 150bps.
   *
   * @return the amount in basis points form
   */
  def toBasisPoints(): BasisPoints = {
    BasisPoints.fromPercentage(this)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a percentage equal to the this percentage plus the other one.
   *
   * @param other the other percentage
   * @return the resulting percentage
   */
  def plus(other: Percentage): Percentage = {
    new Percentage(amount.plus(other.amount))
  }

  /**
   * Returns a percentage equal to the this percentage minus the other one.
   *
   * @param other the other percentage
   * @return the resulting percentage
   */
  def minus(other: Percentage): Percentage = {
    new Percentage(amount.minus(other.amount))
  }

  /**
   * Applies an operation to the value.
   *
   * This is generally used to apply a mathematical operation to the value.
   * For example, the operator could multiply the value by a constant, or take the inverse.
   * {{{
   *   abs = base.map(value -> value.abs());
   * }}}
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this value with the mapping applied to the original amount
   */
  def map(mapper: UnaryOperator[Decimal]): Percentage = {
    new Percentage(mapper.apply(amount))
  }

  //-----------------------------------------------------------------------
  /**
   * Compares this instance to another.
   *
   * @param other the other instance
   * @return the comparison result
   */
  override def compareTo(other: Percentage): Int = {
    amount.compareTo(other.amount)
  }

  /**
   * Checks if this instance equals another.
   *
   * @param obj the other instance
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    obj match {
      case other: Percentage =>
        amount.equals(other.amount)
      case _ =>
        false
    }
  }

  /**
   * Returns a suitable hash code.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    amount.hashCode()
  }

  /**
   * Returns the formal string representation, '{value}%'.
   *
   * @return the formal string
   */
  @ToString
  override def toString(): String = {
    amount.toString() + '%'
  }

}

object Percentage {

  /** A percentage of zero. */
  @static final val ZERO: Percentage = new Percentage(Decimal.ZERO)

  private final val MAX_SCALE = 10

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a percentage value.
   *
   * @param percentage the percentage value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  def of(percentage: Double): Percentage = {
    of(Decimal.of(percentage))
  }

  /**
   * Obtains an instance from a percentage value.
   *
   * @param percentage the percentage value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  def of(percentage: Decimal): Percentage = {
    new Percentage(percentage)
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 0.7%.
   *
   * @param decimal the mathematical decimal value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  def fromDecimalForm(decimal: Double): Percentage = {
    fromDecimalForm(Decimal.of(decimal))
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 0.7%.
   *
   * @param decimal the mathematical decimal value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  def fromDecimalForm(decimal: Decimal): Percentage = {
    of(decimal.movePoint(2))
  }

  /**
   * Obtains an instance from a basis points value, where 70bps will create an instance representing 0.7%.
   *
   * @param basisPoints the basis points value
   * @return the percentage object
   */
  def fromBasisPoints(basisPoints: BasisPoints): Percentage = {
    of(basisPoints.valueBasisPoints().movePoint(-2))
  }

  /**
   * Parses a percentage.
   *
   * The percentage may be suffixed by '%' or 'pct'.
   *
   * @param str the percentage string
   * @return the percentage object, rounded to 10 decimal places
   */
  @FromString
  def parse(str: String): Percentage = {
    ArgChecker.notNull(str, "str")
    if (str.endsWith("%")) {
      of(Decimal.of(str.substring(0, str.length() - 1).trim()))
    } else if (str.endsWith("pct")) {
      of(Decimal.of(str.substring(0, str.length() - 3).trim()))
    } else {
      of(Decimal.of(str.trim()))
    }
  }

}
