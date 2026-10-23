/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.UncheckedIOException
import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Locale
import java.util.NoSuchElementException
import java.util.Spliterator
import java.util.Spliterators
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Stream
import java.util.stream.StreamSupport

import scala.compiletime.uninitialized

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedRunnable
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * Iterator over the rows of a CSV file.
 *
 * Provides the ability to iterate over a CSV file together with the ability to parse it from a `CharSource`.
 * The separator may be specified, allowing TSV files (tab-separated) and other similar formats to be parsed.
 * See `CsvFile` for more details of the CSV format.
 *
 * This class processes the CSV file row-by-row.
 * To load the entire CSV file into memory, use `CsvFile`.
 *
 * This class must be used in a try-with-resources block to ensure that the underlying CSV file is closed:
 * {{{
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    // use the CsvIterator
 *  }
 * }}}
 * One way to use the iterable is with the for-each loop, using `asIterable()`:
 * {{{
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    for (CsvRow row : csvIterator.asIterable()) {
 *      // process the row
 *    }
 *  }
 * }}}
 * This class also allows the headers to be obtained without reading the whole CSV file:
 * {{{
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    ImmutableList<String> headers = csvIterator.headers();
 *  }
 * }}}
 */
final class CsvIterator private (
    /**
     * The buffered reader.
     */
    private val reader: BufferedReader,
    /**
     * The separator
     */
    private val separator: Char,
    /**
     * The header row, ordered as the headers appear in the file.
     */
    private val _headers: ImmutableList[String],
    /**
     * The header map, transformed for case-insensitive searching.
     */
    private val searchHeaders: ImmutableMap[String, Integer],
    /**
     * The current line number in the source file.
     */
    private var currentLineNumber: Int)
  extends AutoCloseable with PeekingIterator[CsvRow] {
  // the headers field is prefixed to avoid clashing with the accessor method

  /**
   * The next row.
   */
  private var nextRow: CsvRow = uninitialized

  //------------------------------------------------------------------------
  /**
   * Gets the header row.
   *
   * If there is no header row, an empty list is returned.
   *
   * @return the header row
   */
  def headers(): ImmutableList[String] = {
    _headers
  }

  /**
   * Checks if the header is present in the file.
   *
   * Matching is case insensitive.
   *
   * @param header  the column header to match
   * @return true if the header is present
   */
  def containsHeader(header: String): Boolean = {
    searchHeaders.containsKey(header.toLowerCase(Locale.ENGLISH))
  }

  /**
   * Checks if the headers are present in the file.
   *
   * Matching is case insensitive.
   *
   * @param headers  the column headers to match
   * @return true if all the headers are present
   */
  def containsHeaders(headers: Collection[String]): Boolean = {
    headers.stream().allMatch((header: String) => containsHeader(header))
  }

  /**
   * Checks if the header pattern is present in the file.
   *
   * Matching is case insensitive.
   *
   * @param headerPattern  the header pattern to match
   * @return true if the header is present
   */
  def containsHeader(headerPattern: Pattern): Boolean = {
    var i = 0
    while (i < _headers.size()) {
      if (headerPattern.matcher(_headers.get(i)).matches()) {
        return true
      }
      i += 1
    }
    false
  }

  /**
   * Returns an `Iterable` that wraps this iterator.
   *
   * Unlike most `Iterable` implementations, the method `Iterable.iterator()`
   * can only be called once. This is intended for use with the for-each loop.
   * {{{
   *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
   *    for (CsvRow row : csvIterator.asIterable()) {
   *      // process the row
   *    }
   *  }
   * }}}
   *
   * @return this iterator as an `Iterable`
   */
  def asIterable(): java.lang.Iterable[CsvRow] = {
    () => this
  }

  /**
   * Returns a stream that wraps this iterator.
   *
   * The stream will process any remaining rows in the CSV file.
   * As such, it is recommended that callers should use this method or the iterator methods and not both.
   *
   * @return the stream wrapping this iterator
   */
  def asStream(): Stream[CsvRow] = {
    val spliterator: Spliterator[CsvRow] =
      Spliterators.spliteratorUnknownSize[CsvRow](this, Spliterator.ORDERED | Spliterator.NONNULL)
    StreamSupport.stream(spliterator, false)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks whether there is another row in the CSV file.
   *
   * @return true if there is another row, false if not
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  override def hasNext(): Boolean = {
    if (nextRow != null) {
      true
    } else {
      val lineReader: CheckedSupplier[String] = () => reader.readLine()
      var line = Unchecked.wrap(lineReader)
      while (line != null) {
        currentLineNumber += 1
        val fields = CsvFile.parseLine(line, separator)
        if (!fields.isEmpty()) {
          nextRow = new CsvRow(_headers, searchHeaders, currentLineNumber, fields)
          return true
        }
        line = Unchecked.wrap(lineReader)
      }
      false
    }
  }

  /**
   * Peeks the next row from the CSV file without changing the iteration position.
   *
   * @return the peeked row
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   * @throws java.util.NoSuchElementException if the end of file has been reached
   */
  override def peek(): CsvRow = {
    if (nextRow != null || hasNext()) {
      nextRow
    } else {
      throw new NoSuchElementException("CsvIterator has reached the end of the file")
    }
  }

  /**
   * Returns the next row from the CSV file.
   *
   * @return the next row
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   * @throws java.util.NoSuchElementException if the end of file has been reached
   */
  override def next(): CsvRow = {
    if (nextRow != null || hasNext()) {
      val row = nextRow
      nextRow = null
      row
    } else {
      throw new NoSuchElementException("CsvIterator has reached the end of the file")
    }
  }

  /**
   * Returns the next batch of rows from the CSV file.
   *
   * This will return up to the specified number of rows from the file at the current iteration point.
   * An empty list is returned if there are no more rows.
   *
   * @param count  the number of rows to try and get, negative returns an empty list
   * @return the next batch of rows, up to the number requested
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def nextBatch(count: Int): List[CsvRow] = {
    val rows: List[CsvRow] = new ArrayList[CsvRow]()
    var i = 0
    while (i < count) {
      if (hasNext()) {
        rows.add(next())
      }
      i += 1
    }
    rows
  }

  /**
   * Returns the next batch of rows from the CSV file using a predicate to determine the rows.
   *
   * This is useful for CSV files where information is grouped with an identifier or key.
   * For example, a variable notional trade file might have one row for the trade followed by
   * multiple rows for the variable aspects, all grouped by a common trade identifier.
   * In general, callers should peek or read the first row and use information within it to
   * create the selector:
   * {{{
   *  while (it.hasNext()) {
   *    CsvRow first = it.peek();
   *    String id = first.getValue("ID");
   *    List<CsvRow> batch = it.nextBatch(row -> row.getValue("ID").equals(id));
   *    // process batch
   *  }
   * }}}
   * This will return a batch of rows where the selector returns true for the row.
   * An empty list is returned if the selector returns false for the first row.
   *
   * @param selector  selects whether a row is part of the batch or part of the next batch
   * @return the next batch of rows, as determined by the selector
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def nextBatch(selector: Predicate[CsvRow]): List[CsvRow] = {
    val rows: List[CsvRow] = new ArrayList[CsvRow]()
    while (hasNext() && selector.test(peek())) {
      rows.add(next())
    }
    rows
  }

  /**
   * Throws an exception as remove is not supported.
   *
   * @throws java.lang.UnsupportedOperationException always
   */
  override def remove(): Unit = {
    throw new UnsupportedOperationException("CsvIterator does not support remove()")
  }

  /**
   * Closes the underlying reader.
   *
   * @throws java.io.UncheckedIOException if an IO exception occurs
   */
  override def close(): Unit = {
    val closer: CheckedRunnable = () => reader.close()
    Unchecked.wrap(closer)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the CSV iterator.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    "CsvIterator" + _headers.toString()
  }

}

object CsvIterator {

  //------------------------------------------------------------------------
  /**
   * Parses the specified source as a CSV file, using a comma as the separator.
   *
   * @param source  the source to read as CSV
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource, headerRow: Boolean): CsvIterator = {
    of(source, headerRow, ',')
  }

  /**
   * Parses the specified source as a CSV file where the separator is specified and might not be a comma.
   *
   * This overload allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   *
   * @param source  the source to read as CSV
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource, headerRow: Boolean, separator: Char): CsvIterator = {
    ArgChecker.notNull(source, "source")
    val opener: CheckedSupplier[BufferedReader] = () => source.openBufferedStream()
    val reader = Unchecked.wrap(opener)
    create(reader, headerRow, separator)
  }

  /**
   * Parses the specified reader as a CSV file, using a comma as the separator.
   *
   * The caller is responsible for closing the reader, such as by calling `close()`.
   *
   * @param reader  the file reader
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(reader: Reader, headerRow: Boolean): CsvIterator = {
    of(reader, headerRow, ',')
  }

  /**
   * Parses the specified reader as a CSV file where the separator is specified and might not be a comma.
   *
   * This overload allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   *
   * The caller is responsible for closing the reader, such as by calling `close()`.
   *
   * @param reader  the file reader
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(reader: Reader, headerRow: Boolean, separator: Char): CsvIterator = {
    ArgChecker.notNull(reader, "reader")
    val breader = reader match {
      case buffered: BufferedReader => buffered
      case _ => new BufferedReader(reader)
    }
    create(breader, headerRow, separator)
  }

  // create the iterator
  private def create(breader: BufferedReader, headerRow: Boolean, separator: Char): CsvIterator = {
    try {
      if (!headerRow) {
        return new CsvIterator(breader, separator, ImmutableList.of[String](), ImmutableMap.of[String, Integer](), 0)
      }
      var line = breader.readLine()
      var lineNumber = 1
      while (line != null) {
        val headers = CsvFile.parseLine(line, separator)
        if (!headers.isEmpty()) {
          return new CsvIterator(breader, separator, headers, CsvFile.buildSearchHeaders(headers), lineNumber)
        }
        line = breader.readLine()
        lineNumber += 1
      }
      throw new IllegalArgumentException("Could not read header row from empty CSV file")

    } catch {
      case ex: RuntimeException =>
        try {
          breader.close()
        } catch {
          case ex2: IOException => ex.addSuppressed(ex2)
        }
        throw ex

      case ex: IOException =>
        try {
          breader.close()
        } catch {
          case ex2: IOException => ex.addSuppressed(ex2)
        }
        throw new UncheckedIOException(ex)
    }
  }

}
