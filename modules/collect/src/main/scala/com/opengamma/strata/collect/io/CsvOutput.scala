/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.UncheckedIOException
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Map
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collector

import org.joda.beans.JodaBeanUtils

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Decimal
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.tuple.ObjIntPair

/**
 * Outputs a CSV formatted file.
 *
 * Provides a simple tool for writing a CSV file.
 *
 * Each line in the CSV file will consist of comma separated entries.
 * Each entry may be quoted using a double quote.
 * If an entry contains a double quote, comma or trimmable whitespace, it will be quoted.
 * If an entry starts with '=' or '@', it will be quoted.
 * Two double quotes will be used to escape a double quote.
 *
 * There are two modes of output.
 * Standard mode provides the encoding described above which is accepted by most CSV parsers.
 * Safe mode provides extra encoding to protect unsafe content from being run as a script in tools like Excel.
 *
 * Instances of this class contain mutable state.
 * A new instance must be created for each file to be output.
 */
final class CsvOutput private (
    /**
     * The header row, ordered as the headers appear in the file.
     */
    private val underlying: Appendable,
    /**
     * The new line string.
     */
    private val newLine: String,
    /**
     * The line item separator.
     */
    private val separator: String,
    /**
     * Whether expressions should be safely encoded.
     */
    private val safeExpressions: Boolean) {

  ArgChecker.notNull(underlying, "underlying")

  /**
   * Whether the writer is currently at the start of a line.
   */
  private var lineStarted: Boolean = false

  //-------------------------------------------------------------------------
  /**
   * Write a header line to the underlying, returning an instance that allows
   * cells to be written by header name.
   *
   * This writes the header line to the underlying.
   * This is normally called once when the `CsvOutput` instance is created
   * with only the returned instance used to write additional rows.
   * While it is possible to write rows through both instances, this is likely to get confusing.
   *
   * @param headers  the list of headers
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @return the writer to use from now on
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def withHeaders(headers: List[String], alwaysQuote: Boolean): CsvOutput#CsvRowOutputWithHeaders = {
    val keyFn: Function[ObjIntPair[String], String] = pair => pair.getFirst()
    val valueFn: Function[ObjIntPair[String], Integer] = pair => Integer.valueOf(pair.getSecond())
    val collector: Collector[ObjIntPair[String], ?, ImmutableMap[String, Integer]] =
      Guavate.toImmutableMap[ObjIntPair[String], String, Integer](keyFn, valueFn)
    val headerIndices: ImmutableMap[String, Integer] = Guavate.zipWithIndex(headers.stream()).collect(collector)
    writeLine(headers, alwaysQuote)
    new CsvRowOutputWithHeaders(ImmutableList.copyOf[String](headers), headerIndices, alwaysQuote)
  }

  //------------------------------------------------------------------------
  /**
   * Writes multiple CSV lines to the underlying.
   *
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * @param lines  the lines to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeLines(lines: java.lang.Iterable[? <: List[String]], alwaysQuote: Boolean): Unit = {
    ArgChecker.notNull(lines, "lines")
    val it = lines.iterator()
    while (it.hasNext()) {
      writeLine(it.next(), alwaysQuote)
    }
  }

  /**
   * Writes a single CSV line to the underlying, only quoting if needed.
   *
   * This can be used as a method reference from a `Stream` pipeline from
   * `Stream.forEachOrdered(Consumer)`.
   *
   * This method writes each cell in the specified list to the underlying, followed by
   * a new line character.
   *
   * @param line  the line to write
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeLine(line: List[String]): Unit = {
    writeLine(line, false)
  }

  /**
   * Writes a single CSV line to the underlying.
   *
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * This method writes each cell in the specified list to the underlying, followed by
   * a new line character.
   *
   * @param line  the line to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeLine(line: List[String], alwaysQuote: Boolean): Unit = {
    ArgChecker.notNull(line, "line")
    val it = line.iterator()
    while (it.hasNext()) {
      writeCell(it.next(), alwaysQuote)
    }
    writeNewLine()
  }

  /**
   * Writes multiple `CsvRow`s to the underlying.
   *
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * @param rows  the rows to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeRows(rows: java.lang.Iterable[CsvRow], alwaysQuote: Boolean): Unit = {
    ArgChecker.notNull(rows, "rows")
    val it = rows.iterator()
    while (it.hasNext()) {
      writeRow(it.next(), alwaysQuote)
    }
  }

  /**
   * Writes a single `CsvRow` to the underlying, only quoting if needed.
   *
   * This can be used as a method reference from a `Stream` pipeline from
   * `Stream.forEachOrdered(Consumer)`.
   *
   * This method writes each field in the specified row to the underlying, followed by
   * a new line character.
   *
   * @param row  the row to write
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeRow(row: CsvRow): Unit = {
    writeRow(row, false)
  }

  /**
   * Writes a single `CsvRow` to the underlying.
   *
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * This method writes each field in the specified row to the underlying, followed by
   * a new line character.
   *
   * @param row  the row to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeRow(row: CsvRow, alwaysQuote: Boolean): Unit = {
    ArgChecker.notNull(row, "row")
    val it = row.fields().iterator()
    while (it.hasNext()) {
      writeCell(it.next(), alwaysQuote)
    }
    writeNewLine()
  }

  /**
   * Writes the provided `CsvFile` to the underlying.
   *
   * @param file  the file whose contents to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeCsvFile(file: CsvFile, alwaysQuote: Boolean): Unit = {
    this.writeLine(file.headers(), alwaysQuote)
    this.writeRows(file.rows(), alwaysQuote)
  }

  /**
   * Writes the output of the provided `CsvIterator` to the underlying.
   *
   * @param iterator  the iterator whose output to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeCsvIterator(iterator: CsvIterator, alwaysQuote: Boolean): Unit = {
    this.writeLine(iterator.headers(), alwaysQuote)
    iterator.asStream().forEachOrdered((row: CsvRow) => writeRow(row, alwaysQuote))
  }

  //-------------------------------------------------------------------------
  /**
   * Writes a single cell to the current line, only quoting if needed.
   *
   * When using this method, either `writeNewLine()` or one of the `writeLine`
   * methods must be called at the end of the line.
   *
   * @param cell  the cell to write
   * @return this, for method chaining
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeCell(cell: String): CsvOutput = {
    writeCell(cell, false)
    this
  }

  /**
   * Writes a single cell to the current line.
   *
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * When using this method, either `writeNewLine()` or one of the `writeLine`
   * methods must be called at the end of the line.
   *
   * @param cell  the cell to write
   * @param alwaysQuote  when true, the cell will be quoted, when false, quoting is selective
   * @return this, for method chaining
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeCell(cell: String, alwaysQuote: Boolean): CsvOutput = {
    try {
      if (lineStarted) {
        underlying.append(separator)
      }
      if (alwaysQuote || isQuotingRequired(cell)) {
        outputQuotedCell(cell)
      } else {
        underlying.append(cell)
      }
      lineStarted = true
    } catch {
      case ex: IOException =>
        throw new UncheckedIOException(ex)
    }
    this
  }

  /**
   * Writes a new line character.
   *
   * @return this, for method chaining
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  def writeNewLine(): CsvOutput = {
    try {
      underlying.append(newLine)
    } catch {
      case ex: IOException =>
        throw new UncheckedIOException(ex)
    }
    lineStarted = false
    this
  }

  //-------------------------------------------------------------------------
  // quoting is required if entry contains quote, comma, trimmable whitespace, or starts with an expression character
  private def isQuotingRequired(cell: String): Boolean = {
    cell.indexOf('"') >= 0 ||
        cell.indexOf(',') >= 0 ||
        cell.trim().length() != cell.length() ||
        isExpressionPrefix(cell)
  }

  // checks if quoting should be applied
  private def isExpressionPrefix(cell: String): Boolean = {
    if (cell.isEmpty()) {
      return false
    }
    val first = cell.charAt(0)
    if (first == '=' || first == '@') {
      return true
    }
    if (safeExpressions && (first == '+' || first == '-')) {
      return !CsvOutput.FP_REGEX.matcher(cell.substring(1)).matches()
    }
    false
  }

  // quotes the entry
  @throws[IOException]
  private def outputQuotedCell(cell: String): Unit = {
    if (safeExpressions && isExpressionPrefix(cell)) {
      underlying.append('=')
    }
    underlying.append('"')
    underlying.append(cell.replace("\"", "\"\""))
    underlying.append('"')
  }

  //-------------------------------------------------------------------------
  /**
   * Class used when outputting CSV with headers.
   */
  class CsvRowOutputWithHeaders private[CsvOutput] (
      private val _headers: ImmutableList[String],
      private val headerIndices: ImmutableMap[String, Integer],
      private val alwaysQuote: Boolean) {
    // the headers field is prefixed to avoid clashing with the accessor method

    private val mutableValueList: List[String] = new ArrayList[String](Collections.nCopies(headerIndices.size(), ""))

    /**
     * Gets the list of headers that are in use.
     *
     * This is the same list that was passed into `CsvOutput.withHeaders(List, boolean)`.
     *
     * @return the list of headers
     */
    def headers(): ImmutableList[String] = {
      _headers
    }

    /**
     * Writes a row to the output, specifying each value by the header.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * This method is equivalent to multiple calls to `writeCell(String, String)`
     * followed by one call to `writeNewLine()`.
     *
     * @param valueMap  the map of values to write keyed by header
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeLine(valueMap: Map[String, String]): CsvRowOutputWithHeaders = {
      writeCells(valueMap).writeNewLine()
    }

    /**
     * Writes a map of cells to the output, with the cell only being output when `writeNewLine()` is called.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * This method is equivalent to multiple calls to `writeCell(String, String)`.
     *
     * Note that if `writeNewLine()` is not called, the cell will never be output.
     *
     * @param valueMap  the map of values to write keyed by header
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeCells(valueMap: Map[String, String]): CsvRowOutputWithHeaders = {
      valueMap.forEach((header: String, value: String) => {
        writeCell(header, value)
        ()
      })
      this
    }

    /**
     * Writes a single cell by header, with the cell only being output when `writeNewLine()` is called.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * Note that if `writeNewLine()` is not called, the cell will never be output.
     *
     * @param header  the header to write
     * @param value  the value to write
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeCell(header: String, value: String): CsvRowOutputWithHeaders = {
      val index = headerIndices.get(header)
      if (index == null) {
        throw new IllegalArgumentException("Unknown header: " + header)
      }
      mutableValueList.set(index.intValue(), value)
      this
    }

    /**
     * Writes a single cell by header, with the cell only being output when `writeNewLine()` is called.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * Note that if `writeNewLine()` is not called, the cell will never be output.
     *
     * @param header  the header to write
     * @param value  the value to write
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeCell(header: String, value: Object): CsvRowOutputWithHeaders = {
      value match {
        case doubleValue: java.lang.Double =>
          writeCell(header, doubleValue.doubleValue())
        case floatValue: java.lang.Float =>
          writeCell(header, floatValue.doubleValue())
        case _ =>
          writeCell(header, JodaBeanUtils.stringConverter().convertToString(value))
      }
    }

    /**
     * Writes a single cell by header, with the cell only being output when `writeNewLine()` is called.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * Note that if `writeNewLine()` is not called, the cell will never be output.
     *
     * @param header  the header to write
     * @param value  the value to write
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeCell(header: String, value: Double): CsvRowOutputWithHeaders = {
      writeCell(header, Decimal.of(value).toString())
    }

    /**
     * Writes a single cell by header, with the cell only being output when `writeNewLine()` is called.
     *
     * The header must exactly match the header passed into the constructor of this instance.
     * An exception is thrown if the header is not known.
     *
     * Note that if `writeNewLine()` is not called, the cell will never be output.
     *
     * @param header  the header to write
     * @param value  the value to write
     * @return this, for method chaining
     * @throws java.lang.IllegalArgumentException if one of the headers does not match
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeCell(header: String, value: Long): CsvRowOutputWithHeaders = {
      writeCell(header, java.lang.Long.valueOf(value): Object)
    }

    /**
     * Writes a new line character.
     *
     * @return this, for method chaining
     * @throws java.io.UncheckedIOException if an IO exception occurs
     */
    def writeNewLine(): CsvRowOutputWithHeaders = {
      CsvOutput.this.writeLine(mutableValueList, alwaysQuote)
      mutableValueList.replaceAll((current: String) => "")
      this
    }
  }

}

object CsvOutput {

  // regex for a floating point decimal number, using possessive to prevent backtracking
  private val DIGITS: String = "(?:[0-9]++(?:[.][0-9]*+)?+|[.][0-9]++)"
  private val EXPONENT: String = "(?:[eE][+-]?+[0-9]++)?+"
  private val FP_REGEX: Pattern = Pattern.compile(DIGITS + EXPONENT)
  private val COMMA: String = ","
  private val NEW_LINE: String = System.lineSeparator()

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, using the system default line separator and using a comma separator.
   *
   * See the standard quoting rules in the class-level documentation.
   *
   * @param underlying  the destination to write to
   * @return the CSV outputter
   */
  def standard(underlying: Appendable): CsvOutput = {
    new CsvOutput(underlying, NEW_LINE, COMMA, false)
  }

  /**
   * Creates an instance, allowing the new line character to be controlled and using a comma separator.
   *
   * See the standard quoting rules in the class-level documentation.
   *
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @return the CSV outputter
   */
  def standard(underlying: Appendable, newLine: String): CsvOutput = {
    new CsvOutput(underlying, newLine, COMMA, false)
  }

  /**
   * Creates an instance, allowing the new line character to be controlled, specifying the separator.
   *
   * See the standard quoting rules in the class-level documentation.
   *
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV outputter
   */
  def standard(underlying: Appendable, newLine: String, separator: String): CsvOutput = {
    new CsvOutput(underlying, newLine, separator, false)
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, using the system default line separator and using a comma separator.
   *
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   *
   * @param underlying  the destination to write to
   * @return the CSV outputter
   */
  def safe(underlying: Appendable): CsvOutput = {
    new CsvOutput(underlying, NEW_LINE, COMMA, true)
  }

  /**
   * Creates an instance, allowing the new line character to be controlled and using a comma separator.
   *
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   *
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @return the CSV outputter
   */
  def safe(underlying: Appendable, newLine: String): CsvOutput = {
    new CsvOutput(underlying, newLine, COMMA, true)
  }

  /**
   * Creates an instance, allowing the new line character to be controlled, specifying the separator.
   *
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   *
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV outputter
   */
  def safe(underlying: Appendable, newLine: String, separator: String): CsvOutput = {
    new CsvOutput(underlying, newLine, separator, true)
  }

}
