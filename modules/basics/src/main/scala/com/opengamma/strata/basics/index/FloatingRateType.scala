/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.EnumNames
import com.opengamma.strata.collect.named.NamedEnum

/**
 * The type of a floating rate index.
 *
 * This provides a high-level categorization of the floating rate index.
 * This is used to classify the index and create the right kind of pricing
 * index, `IborIndex` or `OvernightIndex`.
 */
enum FloatingRateType extends java.lang.Enum[FloatingRateType] with NamedEnum {

  /**
   * A floating rate index that is based on an Ibor index.
   *
   * This kind of rate translates to an `IborIndex`.
   */
  case IBOR
  /**
   * A floating rate index that is based on an Overnight index with compounding.
   *
   * This kind of rate translates to an `OvernightIndex`.
   */
  case OVERNIGHT_COMPOUNDED
  /**
   * A floating rate index that is based on an Overnight index with averaging.
   *
   * This kind of rate translates to an `OvernightIndex`.
   * This is typically used only for US Fed Fund swaps.
   */
  case OVERNIGHT_AVERAGED
  /**
   * A floating rate index that is based on a price index.
   *
   * This kind of rate translates to an `PriceIndex`.
   */
  case PRICE
  /**
   * A floating rate index of another type.
   */
  case OTHER

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Ibor'.
   *
   * @return true if Ibor, false otherwise
   */
  def isIbor(): Boolean = {
    this eq FloatingRateType.IBOR
  }

  /**
   * Checks if the type is 'OvernightCompounded' or 'OvernightAveraged'.
   *
   * @return true if Overnight, false otherwise
   */
  def isOvernight(): Boolean = {
    (this eq FloatingRateType.OVERNIGHT_COMPOUNDED) || (this eq FloatingRateType.OVERNIGHT_AVERAGED)
  }

  /**
   * Checks if the type is 'Price'.
   *
   * @return true if Price, false otherwise
   */
  def isPrice(): Boolean = {
    this eq FloatingRateType.PRICE
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  override def toString(): String = {
    FloatingRateType.NAMES.format(this)
  }

}

object FloatingRateType {

  // helper for name conversions
  private lazy val NAMES: EnumNames[FloatingRateType] = EnumNames.of(classOf[FloatingRateType])

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
  @static def of(name: String): FloatingRateType = {
    NAMES.parse(name)
  }

}
