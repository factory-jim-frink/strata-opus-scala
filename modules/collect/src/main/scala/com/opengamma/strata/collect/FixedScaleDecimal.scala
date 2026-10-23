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
import java.util.function.UnaryOperator

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.gen.PropertyDefinition
import org.joda.convert.FromString
import org.joda.convert.ToString

/**
 * A decimal number based on `Decimal` with a fixed scale.
 *
 * This is a lightweight wrapper for `Decimal` that ensures the scale is fixed, between 0 and 18.
 * It is most useful for monetary values.
 */
@SerialVersionUID(1L)
final class FixedScaleDecimal private (
    // the Java fields were named 'decimal' and 'fixedScale', which clash with the accessor methods in Scala;
    // the serialized form is mapped explicitly by serialPersistentFields/writeObject/readObject
    /** The unscaled value. */
    @(PropertyDefinition @field)
    @transient private val _decimal: Decimal,
    /** The fixed scale. */
    @(PropertyDefinition @field)
    @transient private val _fixedScale: Int)
  extends Serializable with Comparable[FixedScaleDecimal] {

  /** Holds the deserialized instance between readObject() and readResolve(). */
  @transient private var deserialized: FixedScaleDecimal = uninitialized

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying decimal.
   *
   * The decimal may have a smaller scale, but it will not have a larger scale.
   *
   * @return the decimal
   */
  def decimal(): Decimal = {
    _decimal
  }

  /**
   * Gets the fixed scale.
   *
   * @return the fixed scale, from 0 to 18
   */
  def fixedScale(): Int = {
    _fixedScale
  }

  //-------------------------------------------------------------------------
  /**
   * Maps this value using the maths operations of `Decimal`.
   *
   * The result must have a scale equal or less than the fixed scale.
   *
   * @param fn  the function to apply
   * @return the result of the function
   * @throws java.lang.IllegalArgumentException if the result is too large
   */
  def map(fn: UnaryOperator[Decimal]): FixedScaleDecimal = {
    FixedScaleDecimal.of(fn.apply(_decimal), _fixedScale)
  }

  /**
   * Gets the value as a `BigDecimal` with the fixed scale.
   *
   * @return the decimal, with a scale equal to the fixed scale
   */
  def toBigDecimal(): BigDecimal = {
    _decimal.toBigDecimal().setScale(_fixedScale)
  }

  //-----------------------------------------------------------------------
  override def compareTo(other: FixedScaleDecimal): Int = {
    _decimal.compareTo(other._decimal)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: FixedScaleDecimal =>
        this._decimal.equals(other._decimal) && this._fixedScale == other._fixedScale
      case _ =>
        false
    }
  }

  override def hashCode(): Int = {
    _decimal.hashCode() ^ _fixedScale
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formal string representation of the fixed scale decimal.
   *
   * @return the plain string
   */
  @ToString
  override def toString(): String = {
    _decimal.formatAtLeast(_fixedScale)
  }

  //-------------------------------------------------------------------------
  // writes the baseline serialized form, fields 'decimal' and 'fixedScale'
  @throws[IOException]
  private def writeObject(out: ObjectOutputStream): Unit = {
    val fields = out.putFields()
    fields.put("decimal", _decimal)
    fields.put("fixedScale", _fixedScale)
    out.writeFields()
  }

  // reads the baseline serialized form, without validation as per default Java deserialization
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream): Unit = {
    val fields = in.readFields()
    deserialized = new FixedScaleDecimal(fields.get("decimal", null).asInstanceOf[Decimal], fields.get("fixedScale", 0))
  }

  @throws[ObjectStreamException]
  private def readResolve(): Object = {
    deserialized
  }

}

object FixedScaleDecimal {

  /** The serialized form, matching the original Java field names and types. */
  @static private val serialPersistentFields: Array[ObjectStreamField] = Array(
      new ObjectStreamField("decimal", classOf[Decimal]),
      new ObjectStreamField("fixedScale", java.lang.Integer.TYPE))

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a decimal and scale.
   *
   * @param decimal  the underlying decimal
   * @param fixedScale  the fixed scale, equal or greater than the scale of the decimal
   * @return the equivalent decimal
   */
  def of(decimal: Decimal, fixedScale: Int): FixedScaleDecimal = {
    create(decimal, fixedScale)
  }

  /**
   * Parses an instance from a `String`.
   *
   * @param str  the string
   * @return the equivalent decimal
   * @throws java.lang.NumberFormatException if the string cannot be parsed
   * @throws java.lang.IllegalArgumentException if the value is too large
   */
  @FromString
  def parse(str: String): FixedScaleDecimal = {
    val decimal = Decimal.parse(str)
    val decimalPointPos = str.lastIndexOf('.')
    create(decimal, if (decimalPointPos < 0) 0 else str.length() - decimalPointPos - 1)
  }

  // create an instance, validating as the Java constructor did
  private def create(decimal: Decimal, fixedScale: Int): FixedScaleDecimal = {
    ArgChecker.notNull(decimal, "decimal")
    if (fixedScale < decimal.scale()) {
      throw new IllegalArgumentException("Scale must be equal or greater than the scale of the decimal: " + fixedScale)
    }
    if (fixedScale > Decimal.MAX_SCALE) {
      throw new IllegalArgumentException("Scale must be 18 or less: " + fixedScale)
    }
    new FixedScaleDecimal(decimal, fixedScale)
  }

}
