/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.EnumNames
import com.opengamma.strata.collect.named.NamedEnum

/**
 * The type of value adjustment.
 *
 * A `double` value can be transformed into another value in various different ways.
 * Each type is a function of two values, the base value and the modifying value.
 *
 * Each type represents a different way to express the same concept.
 * For example, here is how an increase from 200 to 220 could be represented:
 *
 * <table class="border 1px solid black;border-collapse:collapse">
 * <tr>
 * <th>Type</th><th>baseValue</th><th>modifyingValue</th><th>Calculation</th>
 * </tr><tr>
 * <td>Replace</td><td>200</td><td>220</td><td>`result = modifyingValue = 220`</td>
 * </tr><tr>
 * <td>DeltaAmount</td><td>200</td><td>20</td><td>`result = baseValue + modifyingValue = (200 + 20) = 220`</td>
 * </tr><tr>
 * <td>DeltaMultiplier</td><td>200</td><td>0.1</td>
 * <td>`result = baseValue + baseValue * modifyingValue = (200 + 200 * 0.1) = 220`</td>
 * </tr><tr>
 * <td>Multiplier</td><td>200</td><td>1.1</td><td>`result = baseValue * modifyingValue = (200 * 1.1) = 220`</td>
 * </tr>
 * </table>
 */
enum ValueAdjustmentType extends java.lang.Enum[ValueAdjustmentType] with NamedEnum {

  /**
   * The modifying value replaces the base value.
   * The input base value is ignored.
   *
   * The result is `modifyingValue`.
   */
  case REPLACE
  /**
   * Calculates the result by treating the modifying value as a delta, adding it to the base value.
   *
   * The result is `(baseValue + modifyingValue)`.
   *
   * This adjustment type can be referred to as an <i>absolute shift</i>.
   */
  case DELTA_AMOUNT
  /**
   * Calculates the result by treating the modifying value as a multiplication factor, adding it to the base value.
   *
   * The result is `(baseValue + baseValue * modifyingValue)`.
   *
   * This adjustment type can be referred to as a <i>relative shift</i>.
   */
  case DELTA_MULTIPLIER
  /**
   * Calculates the result by treating the modifying value as a multiplication factor to apply to the base value.
   *
   * The result is `(baseValue * modifyingValue)`.
   */
  case MULTIPLIER

  //-------------------------------------------------------------------------
  /**
   * Adjusts the base value based on the type and the modifying value.
   *
   * @param baseValue  the base, or previous, value to be adjusted
   * @param modifyingValue  the value that the type uses to modify the base value
   * @return the calculated result
   */
  def adjust(baseValue: Double, modifyingValue: Double): Double = {
    this match {
      case ValueAdjustmentType.REPLACE =>
        modifyingValue
      case ValueAdjustmentType.DELTA_AMOUNT =>
        (baseValue + modifyingValue)
      case ValueAdjustmentType.DELTA_MULTIPLIER =>
        (baseValue + baseValue * modifyingValue)
      case ValueAdjustmentType.MULTIPLIER =>
        (baseValue * modifyingValue)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  override def toString(): String = {
    ValueAdjustmentType.NAMES.format(this)
  }

}

object ValueAdjustmentType {

  // helper for name conversions
  private lazy val NAMES: EnumNames[ValueAdjustmentType] = EnumNames.of(classOf[ValueAdjustmentType])

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   *
   * Parsing handles the mixed case form produced by `toString()` and
   * the upper and lower case variants of the enum constant name.
   *
   * @param name  the name to parse
   * @return the type
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  // @static because the inherited static Named.of suppresses the static forwarder
  @FromString
  @static def of(name: String): ValueAdjustmentType = {
    NAMES.parse(name)
  }

}
