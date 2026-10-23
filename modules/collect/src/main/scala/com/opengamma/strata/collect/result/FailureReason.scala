/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.EnumNames
import com.opengamma.strata.collect.named.NamedEnum

/**
 * Represents the reason why failure occurred.
 *
 * Each failure is categorized as one of the following reasons.
 */
enum FailureReason extends java.lang.Enum[FailureReason] with NamedEnum {

  /**
   * There were multiple failures of different types.
   *
   * An operation may produce zero to many errors.
   * If there is one error then that reason is used.
   * If there are many errors then the overall reason is "multiple".
   */
  case MULTIPLE
  /**
   * An error occurred.
   *
   * Where possible, a more specific reason code should be used.
   */
  case ERROR
  /**
   * The input was invalid.
   *
   * One or more input parameters was invalid.
   */
  case INVALID
  /**
   * A parsing error occurred.
   *
   * This is used when an error occurred during parsing.
   * Typically, this refers to parsing a file, such as CSV or XML.
   */
  case PARSING
  /**
   * The operation requested was not applicable.
   *
   * This is used when the particular combination of inputs is not applicable,
   * but given a different combination a result could have been calculated.
   * For example, this might occur in a grid of results where the calculation
   * requested for a column is not applicable for every row.
   */
  case NOT_APPLICABLE
  /**
   * The operation requested is unsupported.
   *
   * The operation failed because it is not supported.
   */
  case UNSUPPORTED
  /**
   * The operation failed because data was missing.
   *
   * One or more pieces of data that the operation required were missing.
   */
  case MISSING_DATA
  /**
   * Currency conversion failed.
   *
   * This is used to indicate that the operation failed during currency conversion, perhaps due to missing FX rates.
   */
  case CURRENCY_CONVERSION
  /**
   * The operation could not be performed.
   *
   * This is used to indicate that a calculation failed.
   */
  case CALCULATION_FAILED
  /**
   * Failure occurred for some other reason.
   *
   * This reason should only be used when no other type is applicable.
   * If using this reason, please consider raising an issue to get another
   * more descriptive reason added.
   */
  case OTHER

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  override def toString(): String = {
    name()
  }

}

object FailureReason {

  // helper for name conversions
  // this enum is unusual in that the names are just the standard enum names
  private lazy val NAMES: EnumNames[FailureReason] = EnumNames.ofManualToString(classOf[FailureReason])

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
  @static def of(name: String): FailureReason = {
    NAMES.parse(name)
  }

}
