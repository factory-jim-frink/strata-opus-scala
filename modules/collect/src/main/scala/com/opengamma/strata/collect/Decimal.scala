/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import java.io.ObjectStreamField
import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Arrays
import java.util.function.DoubleUnaryOperator
import java.util.function.UnaryOperator

import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.convert.FromString
import org.joda.convert.ToString

/**
 * A decimal number, similar to `BigDecimal`, but optimized for the needs of finance.
 *
 * This class represents a decimal number using a `long` unscaled value and an `int` scale.
 * The scale is constrained to be from 0 to 18. The unscaled value limits the precision to 18 digits.
 * Given this, the class supports 18 decimal places for values between -1 and 1,
 * 17 decimal places for values between -10 and 10, and so on.
 * Fractional values never have trailing zeros, thus the comparator is compatible with equals.
 */
@SerialVersionUID(1L)
final class Decimal private (
    /** The unscaled value. */
    @transient private val unscaled: Long,
    // the Java field was named 'scale', which clashes with scale() in Scala;
    // the serialized form is mapped explicitly by serialPersistentFields/writeObject/readObject
    /** The scale. */
    @transient private val _scale: Int)
  extends Serializable with Comparable[Decimal] {

  /** Holds the deserialized instance between readObject() and readResolve(). */
  @transient private var deserialized: Decimal = uninitialized

  //-------------------------------------------------------------------------
  /**
   * Returns the unscaled part of the value.
   *
   * @return the unscaled value
   */
  def unscaledValue(): Long = {
    unscaled
  }

  /**
   * Returns the scale.
   *
   * @return the scale, from 0 to 18
   */
  def scale(): Int = {
    _scale
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other decimal
   * @return the result of the addition
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def plus(other: Decimal): Decimal = {
    if (this.unscaled == 0) {
      return other
    }
    if (other.unscaled == 0) {
      return this
    }
    plus0(this.unscaled, this._scale, other.unscaled, other._scale)
  }

  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other decimal
   * @return the result of the addition
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def plus(other: Long): Decimal = {
    if (other == 0) {
      return this
    }
    if (other < -Decimal.MAX_UNSCALED || other > Decimal.MAX_UNSCALED) {
      try {
        return Decimal.of(Math.addExact(longValue(), other))
      } catch {
        case ex: ArithmeticException =>
          throw new IllegalArgumentException(
              "Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + " * " + other)
      }
    }
    plus0(unscaled, _scale, other, 0)
  }

  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   *
   * The `double` is converted to a `Decimal` before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other decimal
   * @return the result of the addition
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def plus(other: Double): Decimal = {
    if (other == 0) {
      return this
    }
    plus(Decimal.of(other))
  }

  // sum
  private def plus0(unscaled1: Long, scale1: Int, unscaled2: Long, scale2: Int): Decimal = {
    if (scale1 == scale2) {
      val sum = unscaled1 + unscaled2  // safe, as MAX_UNSCALED + MAX_UNSCALED fits in a long
      Decimal.ofScaled(sum, scale1)
    } else if (scale1 > scale2) {
      plus0Sorted(unscaled1, scale1, unscaled2, scale2)
    } else {
      plus0Sorted(unscaled2, scale2, unscaled1, scale1)
    }
  }

  // sum where scale1 > scale2
  private def plus0Sorted(unscaled1: Long, scale1: Int, unscaled2: Long, scale2: Int): Decimal = {
    val scaleDiff = scale1 - scale2
    if (scaleDiff < Decimal.MAX_SCALE && Math.abs(unscaled2) < Decimal.POWERS(Decimal.MAX_SCALE - scaleDiff - 1)) {
      val rescaled2 = unscaled2 * Decimal.POWERS(scaleDiff)
      return Decimal.ofScaled(unscaled1 + rescaled2, scale1)  // simple add is safe by analysis above
    }
    Decimal.of(BigDecimal.valueOf(unscaled1, scale1).add(BigDecimal.valueOf(unscaled2, scale2)))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other decimal
   * @return the result of the subtraction
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def minus(other: Decimal): Decimal = {
    if (other.unscaled == 0) {
      return this
    }
    plus(other.negated())
  }

  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the result of the subtraction
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def minus(other: Long): Decimal = {
    if (other == 0) {
      return this
    }
    if (other < -Decimal.MAX_UNSCALED || other > Decimal.MAX_UNSCALED) {
      try {
        return Decimal.of(Math.subtractExact(longValue(), other))
      } catch {
        case ex: ArithmeticException =>
          throw new IllegalArgumentException(
              "Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + " * " + other)
      }
    }
    plus0(unscaled, _scale, -other, 0)
  }

  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   *
   * The `double` is converted to a `Decimal` before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the result of the subtraction
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def minus(other: Double): Decimal = {
    if (other == 0) {
      return this
    }
    minus(Decimal.of(other))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   *
   * The result will have a scale in the range 0-18.
   *
   * @param other  the other value
   * @return the result of the multiplication
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def multipliedBy(other: Decimal): Decimal = {
    if (other._scale == 0) {
      return multipliedBy(other.unscaled)
    }
    Decimal.of(toBigDecimal().multiply(other.toBigDecimal()))
  }

  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   *
   * The result will have a scale in the range 0-18.
   *
   * @param other  the other value
   * @return the result of the multiplication
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def multipliedBy(other: Long): Decimal = {
    if (other == 0) {
      return Decimal.ZERO
    }
    try {
      Decimal.ofScaled(Math.multiplyExact(unscaled, other), _scale)
    } catch {
      case ex: ArithmeticException =>
        Decimal.of(toBigDecimal().multiply(BigDecimal.valueOf(other)))
    }
  }

  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   *
   * The `double` is converted to a `Decimal` before the calculation.
   * The result will have a scale in the range 0-18.
   *
   * @param other  the other value
   * @return the result of the multiplication
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def multipliedBy(other: Double): Decimal = {
    if (other == 0) {
      return Decimal.ZERO
    }
    multipliedBy(Decimal.of(other))
  }

  /**
   * Returns a decimal value with the decimal point moved.
   *
   * This can be used to multiply or divide by powers of ten.
   * Positive values move right (multiply), negative values move left (divide)
   *
   * `Decimal.of(1.235d).movePoint(2)` returns a decimal with the value '123.5'.
   *
   * `Decimal.of(1.235d).movePoint(-2)` returns a decimal with the value '0.01235'.
   *
   * @param movement  the amount to move by, positive to move right (multiply), negative to move left (divide)
   * @return the result of the movement
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def movePoint(movement: Int): Decimal = {
    if (movement == 0) {
      return this
    }
    try {
      Decimal.ofScaled0(unscaled, Math.subtractExact(_scale, movement))
    } catch {
      case ex: ArithmeticException =>
        Decimal.ZERO
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the result of the division
   * @throws java.lang.ArithmeticException if dividing by zero
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def dividedBy(other: Decimal): Decimal = {
    Decimal.ofRounded(toBigDecimal().divide(other.toBigDecimal(), Decimal.MATH_CONTEXT))
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value, with a rounding mode.
   *
   * The result will have a scale in the range 0-18.
   *
   * @param other  the other value
   * @param roundingMode  the rounding mode
   * @return the result of the division
   * @throws java.lang.ArithmeticException if dividing by zero
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def dividedBy(other: Decimal, roundingMode: RoundingMode): Decimal = {
    Decimal.ofRounded(
        toBigDecimal().divide(other.toBigDecimal(), new MathContext(Decimal.MAX_PRECISION, roundingMode)))
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the result of the division
   * @throws java.lang.ArithmeticException if dividing by zero
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def dividedBy(other: Long): Decimal = {
    if (other == 1) {
      return this
    }
    val pos = Arrays.binarySearch(Decimal.POWERS, other)
    if (pos > 0) {
      return movePoint(-pos)
    }
    Decimal.ofRounded(toBigDecimal().divide(BigDecimal.valueOf(other), Decimal.MATH_CONTEXT))
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   *
   * The `double` is converted to a `Decimal` before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the result of the multiplication
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def dividedBy(other: Double): Decimal = {
    dividedBy(Decimal.of(other))
  }

  /**
   * Returns the remainder when dividing this value by the specified value.
   *
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   *
   * @param other  the other value
   * @return the remainder of the division
   * @throws java.lang.ArithmeticException if dividing by zero
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def remainder(other: Decimal): Decimal = {
    Decimal.ofRounded(toBigDecimal().remainder(other.toBigDecimal(), Decimal.MATH_CONTEXT))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value rounded to the specified scale.
   *
   * This ensures that the result has the specified scale or less, because trailing zeroes are removed.
   * Specifying a scale of 18 or greater will have no effect.
   * The result will have a scale in the range 0-18.
   *
   * `Decimal.of(1.235d).roundToScale(2, HALF_UP)` returns a decimal with the value '1.24'.
   *
   * `Decimal.of(1.201d).roundToScale(2, HALF_UP)` returns a decimal with the value '1.2'.
   *
   * `Decimal.of(1235).roundToScale(-1, HALF_UP)` returns a decimal with the value '1240'.
   *
   * @param desiredScale  the scale, positive for decimal places, negative to round the integer-part
   * @param roundingMode  the rounding mode
   * @return the result of the round
   * @throws java.lang.IllegalArgumentException if the scale is -18 or less
   */
  def roundToScale(desiredScale: Int, roundingMode: RoundingMode): Decimal = {
    if (desiredScale >= _scale) {
      return this
    }
    // the scale -18 has tricky edge cases, avoid those problems via a blanket ban
    if (desiredScale <= -Decimal.MAX_SCALE) {
      throw new IllegalArgumentException("Rounding scale must not be -18 or less: " + desiredScale)
    }
    // scaling zero always ends up with ZERO
    if (unscaled == 0) {
      return Decimal.ZERO
    }
    // scales above 18 will have no effect
    val adjScale = Math.min(desiredScale, 18)
    // optimize common rounding modes
    if (roundingMode == null) {
      // a Java switch on a null enum throws NullPointerException
      throw new NullPointerException()
    }
    roundingMode match {
      case RoundingMode.DOWN =>
        roundDownToScale(adjScale)
      case RoundingMode.HALF_UP =>
        roundHalfUpToScale(adjScale)
      case RoundingMode.UP =>
        roundUpToScale(adjScale)
      case RoundingMode.FLOOR =>
        if (unscaled > 0) roundDownToScale(adjScale) else roundUpToScale(adjScale)
      case RoundingMode.CEILING =>
        if (unscaled > 0) roundUpToScale(adjScale) else roundDownToScale(adjScale)
      case _ =>
        Decimal.of(toBigDecimal().setScale(adjScale, roundingMode))
    }
  }

  // round down is simple truncation
  private def roundDownToScale(adjScale: Int): Decimal = {
    val scaleDiff = _scale - adjScale
    if (scaleDiff <= Decimal.MAX_SCALE) {
      val rescaled = unscaled / Decimal.POWERS(scaleDiff)
      return Decimal.ofScaled(rescaled, adjScale)
    }
    Decimal.ZERO
  }

  // round half-up only requires looking at the next digit
  private def roundHalfUpToScale(adjScale: Int): Decimal = {
    val scaleDiff = _scale - adjScale
    if (scaleDiff < Decimal.MAX_SCALE) {
      val rescaledPlusNext = unscaled / Decimal.POWERS(scaleDiff - 1)
      val rescaled = rescaledPlusNext / 10
      val nextDigit = rescaledPlusNext % 10
      val bump: Int = if (nextDigit >= 5) 1 else (if (nextDigit <= -5) -1 else 0)
      return Decimal.ofScaled(rescaled + bump, adjScale)
    }
    Decimal.of(toBigDecimal().setScale(adjScale, RoundingMode.HALF_DOWN))
  }

  // round up using round down
  private def roundUpToScale(adjScale: Int): Decimal = {
    val scaleDiff = _scale - adjScale
    if (scaleDiff <= Decimal.MAX_SCALE) {
      val rescaled = unscaled / Decimal.POWERS(scaleDiff)
      if (unscaled == rescaled) {
        return this
      }
      return Decimal.ofScaled(rescaled + java.lang.Long.signum(unscaled), adjScale)
    }
    Decimal.of(toBigDecimal().setScale(adjScale, RoundingMode.UP))
  }

  /**
   * Returns a decimal value rounded to the specified precision.
   *
   * This ensures that the result has no more than the specified precision.
   * Specifying a precision of 18 or greater will have no effect.
   * The result will have a scale in the range 0-18.
   *
   * Note that the decimal 12,000 is considered to have a precision of 2 and scale -3 for the purpose of rounding.
   * In the result it will however be stored with a scale of 0, which could be viewed as a precision of 5.
   *
   * @param precision  the precision, not negative
   * @param roundingMode  the rounding mode
   * @return the result of the round
   */
  def roundToPrecision(precision: Int, roundingMode: RoundingMode): Decimal = {
    ArgChecker.notNegative(precision, "precision")
    if (precision >= 18) {
      return this
    }
    Decimal.ofRounded(toBigDecimal().round(new MathContext(precision, roundingMode)))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the decimal is zero.
   *
   * @return true if zero
   */
  def isZero(): Boolean = {
    unscaled == 0
  }

  /**
   * Returns a decimal value that is positive.
   *
   * @return the positive absolute value
   */
  def abs(): Decimal = {
    new Decimal(if (unscaled < 0) -unscaled else unscaled, _scale)
  }

  /**
   * Returns a decimal value that is negated.
   *
   * @return the negated value
   */
  def negated(): Decimal = {
    new Decimal(-unscaled, _scale)
  }

  /**
   * Returns the sign, -1 for negative, 0 for zero and 1 for positive.
   *
   * @return -1 for negative, 0 for zero and 1 for positive
   */
  def signum(): Int = {
    java.lang.Long.signum(unscaled)
  }

  //-------------------------------------------------------------------------
  /**
   * Maps this decimal value using the maths operations of `double`.
   *
   * Note that `double` maths operations can be imprecise.
   *
   * @param fn  the function to apply
   * @return the result of the function
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def mapAsDouble(fn: DoubleUnaryOperator): Decimal = {
    Decimal.of(fn.applyAsDouble(doubleValue()))
  }

  /**
   * Maps this decimal value using the maths operations of `BigDecimal`.
   *
   * @param fn  the function to apply
   * @return the result of the function
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def mapAsBigDecimal(fn: UnaryOperator[BigDecimal]): Decimal = {
    Decimal.of(fn.apply(toBigDecimal()))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the equivalent `double`.
   *
   * @return the equivalent value
   */
  def doubleValue(): Double = {
    if (_scale == 0) {
      return unscaled.toDouble
    }
    if (Math.abs(unscaled) < (1L << 52)) {
      return unscaled.toDouble / Decimal.POWERS(_scale)
    }
    java.lang.Double.parseDouble(toString())
  }

  /**
   * Returns the equivalent `long`.
   *
   * This truncates any fractional part.
   *
   * @return the equivalent value
   */
  def longValue(): Long = {
    unscaled / Decimal.POWERS(_scale)
  }

  /**
   * Returns the equivalent `BigDecimal`.
   *
   * @return the equivalent value
   */
  def toBigDecimal(): BigDecimal = {
    BigDecimal.valueOf(unscaled, _scale)
  }

  /**
   * Returns the equivalent `FixedScaleDecimal`.
   *
   * Callers should call `roundToScale(int, RoundingMode)` first if scale is unknown.
   *
   * @param fixedScale  the fixed scale
   * @return the fixed scale decimal
   * @throws java.lang.IllegalArgumentException if the fixed scale is less than the scale of this decimal
   */
  def toFixedScale(fixedScale: Int): FixedScaleDecimal = {
    FixedScaleDecimal.of(this, fixedScale)
  }

  //-------------------------------------------------------------------------
  /**
   * Formats the decimal to at least the specified number of decimal places.
   *
   * With a minimum decimal places of 2,
   * the decimal '12.1' will be formatted as '12.10', and
   * the decimal '12.123' will be formatted as '12.123'
   *
   * Calling this method with '0' as the minimum decimal places is equivalent to using `toString()`..
   *
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 18 inclusive
   * @return the formatted string
   */
  def formatAtLeast(minDecimalPlaces: Int): String = {
    if (minDecimalPlaces < 0 || minDecimalPlaces > 18) {
      throw new IllegalArgumentException("Format requires decimal places between 0 and 18 inclusive")
    }
    format0(Math.max(minDecimalPlaces, _scale))
  }

  /**
   * Formats the decimal to exactly the specified number of decimal places, specifying the rounding mode.
   *
   * With 2 decimal places and rounding mode HALF_UP,
   * the decimal '12.1' will be formatted as '12.10', and
   * the decimal '12.125' will be formatted as '12.13'
   *
   * Use `RoundingMode#DOWN` to truncate at the specified number of decimal places.
   *
   * @param decimalPlaces  the number of decimal places, from 0 to 18 inclusive
   * @param roundingMode  the rounding mode to use
   * @return the formatted string
   */
  def format(decimalPlaces: Int, roundingMode: RoundingMode): String = {
    if (decimalPlaces < 0 || decimalPlaces > 18) {
      throw new IllegalArgumentException("Format requires decimal places between 0 and 18 inclusive")
    }
    roundToScale(decimalPlaces, roundingMode).format0(decimalPlaces)
  }

  /**
   * States if this decimal is greater than the other decimal.
   *
   * @param other the other decimal
   * @return true if this is greater than the other
   */
  def isGreaterThan(other: Decimal): Boolean = {
    compareTo(other) > 0
  }

  /**
   * States if this decimal is greater than or equal to the other decimal.
   *
   * @param other the other decimal
   * @return true if this is greater than or equal to the other
   */
  def isGreaterThanEqualTo(other: Decimal): Boolean = {
    compareTo(other) >= 0
  }

  /**
   * States if this decimal is less than the other decimal.
   *
   * @param other the other decimal
   * @return true if this is less than the other
   */
  def isLessThan(other: Decimal): Boolean = {
    compareTo(other) < 0
  }

  /**
   * States if this decimal is less than or equal to the other decimal.
   *
   * @param other the other decimal
   * @return true if this is less than or equal to the other
   */
  def isLessThanEqualTo(other: Decimal): Boolean = {
    compareTo(other) <= 0
  }

  // formats the string
  private def format0(decimalPlaces: Int): String = {
    // split 78345 into whole and fraction: 78 and 345
    // prefix the fraction 345, so there is an additional digit, thus 1345, dropping the sign
    // this ensures that leading zeroes in the fraction are retained
    val abs = Math.abs(unscaled)
    val power = Decimal.POWERS(_scale)
    val whole = abs / power
    val prefixedFraction = (abs % power) + power
    var paddedFraction = prefixedFraction * Decimal.POWERS(decimalPlaces - _scale)
    val buf = new java.lang.StringBuilder(40)
    if (unscaled < 0) {
      buf.append('-')
    }
    buf.append(whole)
    if (decimalPlaces > 0) {
      buf.append('.')
      // use knowledge of fixed length of long to output each character directly
      val pos = buf.length()
      buf.setLength(pos + decimalPlaces)
      var i = decimalPlaces + pos - 1
      while (i >= pos) {
        val value = (paddedFraction % 10).toInt
        buf.setCharAt(i, (value + 48).toChar)
        paddedFraction = paddedFraction / 10
        i -= 1
      }
    }
    buf.toString()
  }

  //-----------------------------------------------------------------------
  override def compareTo(other: Decimal): Int = {
    if (this._scale == other._scale) {
      return java.lang.Long.compare(this.unscaled, other.unscaled)
    }
    val power1 = Decimal.POWERS(this._scale)
    val power2 = Decimal.POWERS(other._scale)
    val whole1 = this.unscaled / power1
    val whole2 = other.unscaled / power2
    if (whole1 < whole2) {
      return -1
    } else if (whole1 > whole2) {
      return 1
    }
    val fraction1 = (this.unscaled % power1) * Decimal.POWERS(18 - this._scale)
    val fraction2 = (other.unscaled % power2) * Decimal.POWERS(18 - other._scale)
    java.lang.Long.compare(fraction1, fraction2)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: Decimal =>
        this.unscaled == other.unscaled && this._scale == other._scale
      case _ =>
        false
    }
  }

  override def hashCode(): Int = {
    java.lang.Long.hashCode(unscaled) ^ _scale
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formal string representation of the decimal.
   *
   * This is equivalent to the "plain string" of `BigDecimal`.
   *
   * @return the plain string
   */
  @ToString
  override def toString(): String = {
    format0(_scale)
  }

  //-------------------------------------------------------------------------
  // writes the baseline serialized form, fields 'unscaled' and 'scale'
  @throws[IOException]
  private def writeObject(out: ObjectOutputStream): Unit = {
    val fields = out.putFields()
    fields.put("unscaled", unscaled)
    fields.put("scale", _scale)
    out.writeFields()
  }

  // reads the baseline serialized form, without validation as per default Java deserialization
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream): Unit = {
    val fields = in.readFields()
    deserialized = new Decimal(fields.get("unscaled", 0L), fields.get("scale", 0))
  }

  @throws[ObjectStreamException]
  private def readResolve(): Object = {
    deserialized
  }

}

object Decimal {

  /** Powers of ten. */
  @static private val POWERS: Array[Long] = {
    val powers = new Array[Long](MAX_SCALE + 1)
    powers(0) = 1
    var i = 1
    while (i < powers.length) {
      powers(i) = powers(i - 1) * 10
      i += 1
    }
    powers
  }
  /** Math context that truncates. */
  @static private val MATH_CONTEXT: MathContext = new MathContext(MAX_PRECISION, RoundingMode.DOWN)

  /**
   * A decimal value representing zero.
   */
  @static final val ZERO: Decimal = new Decimal(0, 0)
  /**
   * A decimal value representing the largest supported value.
   */
  @static final val MAX_VALUE: Decimal = new Decimal(MAX_UNSCALED, 0)
  /**
   * A decimal value representing the smallest supported value.
   */
  @static final val MIN_VALUE: Decimal = new Decimal(-MAX_UNSCALED, 0)

  /** The serialized form, matching the original Java field names and types. */
  @static private val serialPersistentFields: Array[ObjectStreamField] = Array(
      new ObjectStreamField("unscaled", java.lang.Long.TYPE),
      new ObjectStreamField("scale", java.lang.Integer.TYPE))

  // compile-time constants are inlined, so declaring them after the @static values is safe
  /** Max precision. */
  private final val MAX_PRECISION = 18
  /** Max scale. */
  private[collect] final val MAX_SCALE = 18
  /** Max unscaled value. */
  private final val MAX_UNSCALED = 999_999_999_999_999_999L

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a `long`.
   *
   * @param value  the value
   * @return the equivalent decimal
   */
  def of(value: Long): Decimal = {
    if (value > MAX_UNSCALED || value < -MAX_UNSCALED) {
      throw new IllegalArgumentException(
          "Decimal value must not exceed 18 digits of precision at scale 0: " + value)
    }
    new Decimal(value, 0)
  }

  /**
   * Obtains an instance from a `double`.
   *
   * This operates as though the `double` is converted to a `String` and then parsed using `of(String)`.
   *
   * @param value  the value
   * @return the equivalent decimal
   * @throws java.lang.IllegalArgumentException if the value is not finite or is too large
   */
  def of(value: Double): Decimal = {
    if (!java.lang.Double.isFinite(value)) {
      throw new IllegalArgumentException("Decimal value must be finite: " + value)
    }
    val longValue = value.toLong
    if (value == longValue.toDouble) {
      return of(longValue)
    }
    of(java.lang.Double.toString(value))
  }

  /**
   * Obtains an instance from a `String`.
   *
   * This uses a parser with the same semantics as constructing a `BigDecimal` with the string
   * and then converting using `of(BigDecimal)`.
   *
   * @param str  the string
   * @return the equivalent decimal
   * @throws java.lang.NumberFormatException if the string cannot be parsed
   * @throws java.lang.IllegalArgumentException if the value is too large
   */
  def of(str: String): Decimal = {
    // pre-filter algorithm, based on performance testing
    val len = str.length()
    if (len == 0) {
      throw new NumberFormatException("Decimal string must not be empty")
    } else if (len == 1) {
      parseShortString(str)
    } else {
      parseString(str)
    }
  }

  private def parseString(str: String): Decimal = {
    val chs = str.toCharArray()
    if (chs.length > 256) {
      throw new NumberFormatException("Decimal string must not exceed 256 characters")
    }
    // extract sign
    var sign = 1
    var startPos = 0
    val first = chs(0)
    if (first == '-') {
      sign = -1
      startPos = 1
    } else if (first == '+') {
      startPos = 1
    }
    // extract number
    var unscaled = 0L
    var precision = 0
    var scale = 0
    var afterDecimalPoint = false
    var i = startPos
    while (i < chs.length) {
      val ch = chs(i)
      ch match {
        case '+' | '-' =>
          throw new NumberFormatException("Decimal string must not have sign in the middle: " + str)
        case '.' =>
          if (afterDecimalPoint) {
            throw new NumberFormatException("Decimal string must not have two decimal points: " + str)
          }
          afterDecimalPoint = true
        case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          if (precision >= MAX_PRECISION) {
            // max precision has been reached, stop parsing but still look for errors
            if (!afterDecimalPoint) {
              throw new NumberFormatException(
                  "Decimal string must not exceed 18 digits before decimal point: " + str)
            }
          } else if (scale >= MAX_SCALE) {
            // max scale has been reached, stop parsing but still look for errors
          } else {
            val chValue = ch - 48
            // increase scale if after decimal point
            if (afterDecimalPoint) {
              scale += 1
            }
            // parse unscaled value, skip leading zeroes before decimal point
            if (precision > 0 || chValue > 0) {
              unscaled = unscaled * 10 + chValue
              precision += 1
            }
          }
        case _ =>
          return of(new BigDecimal(str))  // handles exponents and unicode digits
      }
      i += 1
    }
    ofScaled(unscaled * sign, scale)
  }

  private def parseShortString(str: String): Decimal = {
    val ch = str.charAt(0)
    if (ch >= '0' && ch <= '9') {
      return new Decimal(ch - 48, 0)
    }
    throw new NumberFormatException("Decimal string is invalid: " + ch)
  }

  /**
   * Obtains an instance from a `BigDecimal`.
   *
   * The scale is adjusted to be in the range 0-18, with any smaller fractional part dropped by truncation.
   *
   * @param value  the value
   * @return the equivalent decimal
   * @throws java.lang.IllegalArgumentException if the value is too large
   */
  def of(value: BigDecimal): Decimal = {
    ofRounded(value.round(MATH_CONTEXT))
  }

  // creates from a pre-rounded value
  private def ofRounded(value: BigDecimal): Decimal = {
    var adjusted = value.stripTrailingZeros()
    adjusted = if (adjusted.scale() < 0) adjusted.setScale(0) else adjusted
    if (adjusted.precision() > MAX_PRECISION) {
      throw new IllegalArgumentException(
          "Decimal value must not exceed 18 digits of precision at scale 0: " + value)
    }
    // longValueExact() used for extra safety, it should never actually throw
    ofScaled(adjusted.unscaledValue().longValueExact(), adjusted.scale())
  }

  /**
   * Obtains an instance from an unscaled value and a scale.
   *
   * The scale is adjusted to be in the range 0-18, with any smaller fractional part dropped by truncation.
   * The result is normalized to have no fractional trailing zeroes.
   *
   * For example, `Decimal.ofScaled(1230, 2)` returns a decimal with the value '12.3'.
   *
   * @param unscaled  the unscaled value
   * @param scale  the scale
   * @return the equivalent decimal
   * @throws java.lang.IllegalArgumentException if the value is too large
   */
  def ofScaled(unscaled: Long, scale: Int): Decimal = {
    if (unscaled == 0) {
      return ZERO
    }
    if (scale < 0 || scale > MAX_SCALE || unscaled > MAX_UNSCALED || unscaled < -MAX_UNSCALED) {
      return ofScaled0(unscaled, scale)
    }
    create(unscaled, scale)
  }

  // create, removing any trailing zeroes
  private def create(unscaled: Long, scale: Int): Decimal = {
    if (scale > 0 && ((unscaled % 10) == 0)) {
      return create(unscaled / 10, scale - 1)
    }
    new Decimal(unscaled, scale)
  }

  // special cases, broken out to aid JVM inlining
  private def ofScaled0(unscaled: Long, scale: Int): Decimal = {
    // any scale allowed for ZERO, max precision will truncate to zero at this scale
    if (scale >= 2 * MAX_SCALE) {
      return ZERO
    }
    if (scale <= -MAX_SCALE) {
      throw new IllegalArgumentException(
          "Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + "E" + -scale)
    }
    // reduce the precision by truncation
    if (unscaled > MAX_UNSCALED || unscaled < -MAX_UNSCALED) {
      if (scale == 0) {
        throw new IllegalArgumentException(
            "Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + "E" + -scale)
      } else {
        // recurse to check scale (only needs to happen once as max precision of a long is 19)
        return ofScaled0(unscaled / 10, scale - 1)
      }
    }
    // set scale to zero if possible
    if (scale < 0) {
      try {
        val adjUnscaled = Math.multiplyExact(unscaled, POWERS(-scale))
        return ofScaled0(adjUnscaled, 0)  // recurse to check if between MAX_UNSCALED and Long.MAX_VALUE
      } catch {
        case ex: ArithmeticException =>
          throw new IllegalArgumentException(
              "Decimal value must not exceed 18 digits of precision: " + unscaled + "E" + -scale)
      }
    }
    // reduce the scale by truncation
    if (scale > MAX_SCALE) {
      val adjUnscaled = unscaled / POWERS(scale - MAX_SCALE)
      if (adjUnscaled == 0) {
        return ZERO
      }
      return create(adjUnscaled, MAX_SCALE)
    }
    // scale >= 0, thus OK to call create()
    create(unscaled, scale)
  }

  /**
   * Parses an instance from a `String`.
   *
   * This uses a parser with the same semantics as constructing a `BigDecimal` with the string
   * and then converting using `of(BigDecimal)`.
   *
   * @param str  the string
   * @return the equivalent decimal
   * @throws java.lang.NumberFormatException if the string cannot be parsed
   * @throws java.lang.IllegalArgumentException if the value is too large
   */
  @FromString
  def parse(str: String): Decimal = {
    // this method exists to provide a unique name for method references
    // and to allow parse() to diverge from of() in the future if desired
    of(str)
  }

}
