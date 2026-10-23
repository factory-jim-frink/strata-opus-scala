/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

/**
 * Provides the ability to parse and format numbers.
 *
 * This exists as an alternative to `NumberFormat` and `DecimalFormat`
 * which are not thread-safe.
 *
 * Instances of this class are immutable and thread-safe.
 */
final class NumberFormatter private (numberFormat: NumberFormat) {

  ArgChecker.notNull(numberFormat, "format")
  numberFormat.setParseIntegerOnly(false)
  /**
   * The underlying format.
   */
  private val underlying: ThreadLocal[NumberFormat] =
    ThreadLocal.withInitial[NumberFormat](() => numberFormat.clone().asInstanceOf[NumberFormat])

  //-------------------------------------------------------------------------
  /**
   * Formats a `double` using this formatter.
   *
   * @param number  the number to format
   * @return the formatted string
   */
  def format(number: Double): String = {
    underlying.get().format(number)
  }

  /**
   * Formats a `long` using this formatter.
   *
   * @param number  the number to format
   * @return the formatted string
   */
  def format(number: Long): String = {
    underlying.get().format(number)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the specific string, returning a double.
   *
   * @param text  the string to parse
   * @return the parsed number
   * @throws java.lang.IllegalArgumentException if the text cannot be parsed
   */
  def parse(text: String): Double = {
    try {
      underlying.get().parse(text).doubleValue()
    } catch {
      case ex: ParseException =>
        throw new IllegalArgumentException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string representation of this formatter.
   *
   * @return the string
   */
  override def toString(): String = {
    underlying.toString()
  }

}

object NumberFormatter {

  //-------------------------------------------------------------------------
  /**
   * Obtains a standard formatter configured by grouping and decimal places.
   *
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * Numbers will be rounded using `RoundingMode#HALF_EVEN`
   *
   * @param grouped  true to group, false to not group
   * @param decimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws java.lang.IllegalArgumentException if the decimal places is invalid
   */
  def of(grouped: Boolean, decimalPlaces: Int): NumberFormatter = {
    of(grouped, decimalPlaces, decimalPlaces)
  }

  /**
   * Obtains a standard formatter configured by grouping and decimal places.
   *
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * Numbers will be rounded using `RoundingMode#HALF_EVEN`
   *
   * @param grouped  true to group, false to not group
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @param maxDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws java.lang.IllegalArgumentException if the decimal places is invalid
   */
  def of(grouped: Boolean, minDecimalPlaces: Int, maxDecimalPlaces: Int): NumberFormatter = {
    ArgChecker.inRangeInclusive(minDecimalPlaces, 0, 9, "minDecimalPlaces")
    ArgChecker.inRangeInclusive(maxDecimalPlaces, 0, 9, "maxDecimalPlaces")
    ArgChecker.isTrue(minDecimalPlaces <= maxDecimalPlaces, "Expected minDecimalPlaces <= maxDecimalPlaces")
    create(grouped, minDecimalPlaces, maxDecimalPlaces)
  }

  // creates an instance ignoring the cache
  private def create(grouped: Boolean, minDecimalPlaces: Int, maxDecimalPlaces: Int): NumberFormatter = {
    val format = NumberFormat.getNumberInstance(Locale.ENGLISH)
    format.setGroupingUsed(grouped)
    format.setMinimumIntegerDigits(1)
    format.setMinimumFractionDigits(minDecimalPlaces)
    format.setMaximumFractionDigits(maxDecimalPlaces)
    new NumberFormatter(format)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a formatter for decimal percentages configured by grouping and decimal places.
   *
   * The formatter will have the specified number of decimal places.
   * The integer part will be grouped if the flag is set.
   * The decimal part will never be grouped or truncated.
   * The implementation uses English locale data, which uses commas as a separator and a decimal point (dot).
   * The formatter will suffix the output with '%'.
   * Numbers will be rounded using `RoundingMode#HALF_EVEN`
   *
   * The number passed in must be the decimal representation of the percentage.
   * It will be multiplied by 100 before formatting.
   *
   * @param grouped  true to group, false to not group
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @param maxDecimalPlaces  the minimum number of decimal places, from 0 to 9
   * @return the formatter
   * @throws java.lang.IllegalArgumentException if the decimal places is invalid
   */
  def ofPercentage(grouped: Boolean, minDecimalPlaces: Int, maxDecimalPlaces: Int): NumberFormatter = {
    ArgChecker.inRangeInclusive(minDecimalPlaces, 0, 9, "minDecimalPlaces")
    ArgChecker.inRangeInclusive(maxDecimalPlaces, 0, 9, "maxDecimalPlaces")
    ArgChecker.isTrue(minDecimalPlaces <= maxDecimalPlaces, "Expected minDecimalPlaces <= maxDecimalPlaces")
    val format = NumberFormat.getPercentInstance(Locale.ENGLISH)
    format.setGroupingUsed(grouped)
    format.setMinimumIntegerDigits(1)
    format.setMinimumFractionDigits(minDecimalPlaces)
    format.setMaximumFractionDigits(maxDecimalPlaces)
    new NumberFormatter(format)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a formatter based on a pattern in the specified locale.
   *
   * @param pattern  the pattern string to use
   * @param locale  the locale to use
   * @return the formatter
   * @throws java.lang.IllegalArgumentException if the pattern is invalid
   * @see DecimalFormat
   */
  def ofPattern(pattern: String, locale: Locale): NumberFormatter = {
    ArgChecker.notNull(pattern, "pattern")
    ArgChecker.notNull(locale, "locale")
    new NumberFormatter(new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a number formatter for general-purpose use in the specified locale.
   *
   * @param locale  the locale to use
   * @return the formatter
   * @see NumberFormat#getNumberInstance(Locale)
   */
  def ofLocalizedNumber(locale: Locale): NumberFormatter = {
    ArgChecker.notNull(locale, "locale")
    new NumberFormatter(NumberFormat.getInstance(locale))
  }

}
