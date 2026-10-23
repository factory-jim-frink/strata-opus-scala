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
 * A percentage amount, with a maximum of 8 decimal places.
 *
 * A number has three standard representations in finance:
 *  - decimal form - the form needed for mathematical calculations, as used by most parts of Strata
 *  - percentage - where 1.2% is the same as the decimal 0.012
 *  - basis points - where 125bps is the same as the decimal 0.0125
 *
 * This class allows the use of basis points form to be explicit.
 */
final class BasisPoints private (basisPoints: Decimal) extends Comparable[BasisPoints] {

  ArgChecker.notNull(basisPoints, "basisPoints")
  /**
   * The basis points.
   */
  private val amount: Decimal = basisPoints.roundToScale(BasisPoints.MAX_SCALE, RoundingMode.HALF_UP)

  //-----------------------------------------------------------------------
  /**
   * Gets the value in basis points form as a `Decimal`.
   *
   * A value of 150bps will return 150.
   *
   * @return the value in basis points form, with a maximum of 8 decimal places
   */
  def valueBasisPoints(): Decimal = {
    amount
  }

  /**
   * Converts this basis points to mathematical decimal form.
   *
   * A value of 150bps will return 0.015.
   *
   * @return the amount in mathematical decimal form
   */
  def toDecimalForm(): Decimal = {
    amount.movePoint(-4)
  }

  /**
   * Converts this basis points to the equivalent percentage.
   *
   * A value of 150bps will return 1.5%.
   *
   * @return the amount in percentage form
   */
  def toPercentage(): Percentage = {
    Percentage.fromBasisPoints(this)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a basis points equal to the this basis points plus the other one.
   *
   * @param other the other basis points
   * @return the resulting basis points
   */
  def plus(other: BasisPoints): BasisPoints = {
    new BasisPoints(amount.plus(other.amount))
  }

  /**
   * Returns a basis points equal to the this basis points minus the other one.
   *
   * @param other the other basis points
   * @return the resulting basis points
   */
  def minus(other: BasisPoints): BasisPoints = {
    new BasisPoints(amount.minus(other.amount))
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
  def map(mapper: UnaryOperator[Decimal]): BasisPoints = {
    new BasisPoints(mapper.apply(amount))
  }

  //-----------------------------------------------------------------------
  /**
   * Compares this instance to another.
   *
   * @param other the other instance
   * @return the comparison result
   */
  override def compareTo(other: BasisPoints): Int = {
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
      case other: BasisPoints =>
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
   * Returns the formal string representation, '{value}bps'.
   *
   * @return the formal string
   */
  @ToString
  override def toString(): String = {
    amount.toString() + "bps"
  }

}

object BasisPoints {

  /** A basis points of zero. */
  @static final val ZERO: BasisPoints = new BasisPoints(Decimal.ZERO)

  private final val MAX_SCALE = 8

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a basis points value.
   *
   * @param basisPoints the basis points value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  def of(basisPoints: Double): BasisPoints = {
    of(Decimal.of(basisPoints))
  }

  /**
   * Obtains an instance from a basis points value.
   *
   * @param basisPoints the basis points value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  def of(basisPoints: Decimal): BasisPoints = {
    new BasisPoints(basisPoints)
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 70bps.
   *
   * @param decimal the mathematical decimal value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  def fromDecimalForm(decimal: Double): BasisPoints = {
    fromDecimalForm(Decimal.of(decimal))
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 70bps.
   *
   * @param decimal the mathematical decimal value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  def fromDecimalForm(decimal: Decimal): BasisPoints = {
    of(decimal.movePoint(4))
  }

  /**
   * Obtains an instance from a percentage, where 0.7% will create an instance representing 70bps.
   *
   * @param percentage the percentage
   * @return the basis points object
   */
  def fromPercentage(percentage: Percentage): BasisPoints = {
    of(percentage.valuePercent().movePoint(2))
  }

  /**
   * Parses a percentage.
   *
   * The percentage may be suffixed by 'bps'.
   *
   * @param str the basis points string
   * @return the basis points object, rounded to 8 decimal places
   */
  @FromString
  def parse(str: String): BasisPoints = {
    ArgChecker.notNull(str, "str")
    if (str.endsWith("bps")) {
      of(Decimal.of(str.substring(0, str.length() - 3).trim()))
    } else {
      of(Decimal.of(str.trim()))
    }
  }

}
