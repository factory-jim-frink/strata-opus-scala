/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.EnumNames
import com.opengamma.strata.collect.named.NamedEnum

/**
 * Alignment of the data within an ASCII table.
 *
 * See `AsciiTable` for more details.
 */
enum AsciiTableAlignment extends java.lang.Enum[AsciiTableAlignment] with NamedEnum {

  /**
   * Align left.
   */
  case LEFT
  /**
   * Align right.
   */
  case RIGHT

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  override def toString(): String = {
    AsciiTableAlignment.NAMES.format(this)
  }

}

object AsciiTableAlignment {

  // helper for name conversions
  private lazy val NAMES: EnumNames[AsciiTableAlignment] = EnumNames.of(classOf[AsciiTableAlignment])

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
  @FromString
  def of(name: String): AsciiTableAlignment = {
    NAMES.parse(name)
  }

}
