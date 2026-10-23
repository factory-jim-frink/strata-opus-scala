/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.UncheckedIOException

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * Outputs an INI formatted file.
 *
 * Provides a simple tool for writing an INI file.
 *
 * Instances of this class contain mutable state.
 * A net instance must be created for each file to be output.
 */
final class IniFileOutput private (
    /**
     * The destination to write to.
     */
    private val underlying: Appendable,
    /**
     * The new line string.
     */
    private val newLine: String,
    /**
     * Whether to include an additional whitespace character either side of the '='
     * character between keys and values.
     *
     * Other parsers can be more strict and require that there is no whitespace
     * between the equals separator.
     */
    private val padSeparatorWithWhitespace: Boolean) {

  ArgChecker.notNull(underlying, "underlying")
  ArgChecker.notNull(newLine, "newLine")

  //------------------------------------------------------------------------
  /**
   * Writes an individual section of an INI file to the underlying.
   *
   * @param sectionName  the name of the section
   * @param section  the section whose contents to write
   */
  def writeSection(sectionName: String, section: PropertySet): Unit = {
    ArgChecker.notNull(sectionName, "sectionName")
    ArgChecker.notNull(section, "section")
    val assignment = if (padSeparatorWithWhitespace) " = " else "="

    try {
      underlying.append(IniFileOutput.formatSectionName(sectionName))
      underlying.append(newLine)
      val it = section.asMultimap().entries().iterator()
      while (it.hasNext()) {
        val entry = it.next()
        underlying.append(entry.getKey())
        underlying.append(assignment)
        underlying.append(entry.getValue())
        underlying.append(newLine)
      }
    } catch {
      case ex: IOException =>
        throw new UncheckedIOException(ex)
    }
  }

  /**
   * Writes the provided file to the underlying.
   *
   * @param file  the file whose contents to write
   */
  def writeIniFile(file: IniFile): Unit = {
    val it = file.asMap().entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      writeSection(entry.getKey(), entry.getValue())
      append(newLine)
    }
  }

  //-------------------------------------------------------------------------
  private def append(charSequence: CharSequence): Unit = {
    try {
      underlying.append(charSequence)
    } catch {
      case ex: IOException =>
        throw new UncheckedIOException(ex)
    }
  }

}

object IniFileOutput {

  private val NEW_LINE: String = System.lineSeparator()

  /**
   * Creates an instance, using the system default line separator and including
   * additional whitespace around the '=' separator.
   *
   * @param underlying  the destination to write to
   * @return the INI outputter
   */
  def standard(underlying: Appendable): IniFileOutput = {
    new IniFileOutput(underlying, NEW_LINE, true)
  }

  /**
   * Creates an instance, allowing the new line separator to be controlled and including
   * additional whitespace around the '=' separator.
   *
   * @param underlying  the destination to write to
   * @param newLine  the destination to write to
   * @return the INI outputter
   */
  def standard(underlying: Appendable, newLine: String): IniFileOutput = {
    new IniFileOutput(underlying, newLine, true)
  }

  /**
   * Creates an instance, using the system default line separator and allowing
   * the padding around the '=' separator to be controlled.
   *
   * @param underlying  the destination to write to
   * @param padSeparatorWithWhitespace  if true, the key-value separator will be padded with whitespace
   * @return the INI outputter
   */
  def standard(underlying: Appendable, padSeparatorWithWhitespace: Boolean): IniFileOutput = {
    new IniFileOutput(underlying, NEW_LINE, padSeparatorWithWhitespace)
  }

  /**
   * Creates an instance, allowing the new line separator and the padding around
   * the '=' separator to be controlled.
   *
   * @param underlying  the destination to write to
   * @param padSeparatorWithWhitespace  if true, the key-value separator will be padded with whitespace
   * @param newLine  the new line separator
   * @return the INI outputter
   */
  def standard(underlying: Appendable, padSeparatorWithWhitespace: Boolean, newLine: String): IniFileOutput = {
    new IniFileOutput(underlying, newLine, padSeparatorWithWhitespace)
  }

  //-------------------------------------------------------------------------
  /**
   * Formats the section name.
   *
   * @param name  section name
   * @return the formatted name
   */
  private def formatSectionName(name: String): String = {
    Messages.format("[{}]", name)
  }

}
