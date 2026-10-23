/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.UncheckedIOException
import java.util.Collection
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.regex.Pattern

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.CharSource
import com.google.common.io.CharStreams
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A CSV file.
 *
 * Represents a CSV file together with the ability to parse it from a `CharSource`.
 * The separator may be specified, allowing TSV files (tab-separated) and other similar formats to be parsed.
 *
 * This class loads the entire CSV file into memory.
 * To process the CSV file row-by-row, use `CsvIterator`.
 *
 * The CSV file format is a general-purpose comma-separated value format.
 * The format is parsed line-by-line, with lines separated by CR, LF or CRLF.
 * Each line can contain one or more fields.
 * Each field is separated by a comma character (,) or tab.
 * Any field may be quoted using a double quote at the start and end.
 * A quoted field may additionally be prefixed by an equals sign.
 * The content of a quoted field may include commas and additional double quotes.
 * Two adjacent double quotes in a quoted field will be replaced by a single double quote.
 * Quoted fields are not trimmed. Non-quoted fields are trimmed.
 *
 * The first line may be treated as a header row.
 * The header row is accessed separately from the data rows.
 *
 * Blank lines are ignored.
 * Lines may be commented with has '#' or semicolon ';'.
 */
final class CsvFile private (
    /**
     * The header row, ordered as the headers appear in the file.
     */
    private val _headers: ImmutableList[String],
    /**
     * The header map, transformed for case-insensitive searching.
     */
    private val searchHeaders: ImmutableMap[String, Integer],
    /**
     * The data rows in the CSV file.
     */
    private val _rows: ImmutableList[CsvRow]) {
  // the backing fields are prefixed to avoid clashing with the accessor methods

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
   * Gets all data rows in the file.
   *
   * @return the data rows
   */
  def rows(): ImmutableList[CsvRow] = {
    _rows
  }

  /**
   * Gets the number of data rows.
   *
   * @return the number of data rows
   */
  def rowCount(): Int = {
    _rows.size()
  }

  /**
   * Gets a single row.
   *
   * @param index  the row index, zero-based
   * @return the row
   */
  def row(index: Int): CsvRow = {
    _rows.get(index)
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
   * Returns an instance with the specified headers.
   *
   * @param headers  the new headers
   * @return the instance with the specified headers
   */
  def withHeaders(headers: List[String]): CsvFile = {
    CsvFile.of(
        headers,
        _rows.stream()
            .map[ImmutableList[String]]((row: CsvRow) => row.fields())
            .collect(Guavate.toImmutableList[ImmutableList[String]]()))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this CSV file equals another.
   *
   * The comparison checks the content.
   *
   * @param obj  the other file, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: CsvFile =>
          _headers.equals(other._headers) && _rows.equals(other._rows)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the CSV file.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    _headers.hashCode() ^ _rows.hashCode()
  }

  /**
   * Returns a string describing the CSV file.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    "CsvFile" + _headers.toString()
  }

}

object CsvFile {

  //------------------------------------------------------------------------
  /**
   * Parses the specified source as a CSV file, using a comma as the separator.
   *
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param source  the CSV file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource, headerRow: Boolean): CsvFile = {
    of(source, headerRow, ',')
  }

  /**
   * Parses the specified source as a CSV file where the separator is specified and might not be a comma.
   *
   * This overload allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   *
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param source  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource, headerRow: Boolean, separator: Char): CsvFile = {
    ArgChecker.notNull(source, "source")
    val reader: CheckedSupplier[List[String]] = () => source.readLines()
    val lines = Unchecked.wrap(reader)
    create(lines, headerRow, separator)
  }

  /**
   * Parses the specified reader as a CSV file, using a comma as the separator.
   *
   * This factory method takes a `Reader`.
   * Callers are encouraged to use `CharSource` instead of `Reader`
   * as it allows the resource to be safely managed.
   *
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   *
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param reader  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(reader: Reader, headerRow: Boolean): CsvFile = {
    of(reader, headerRow, ',')
  }

  /**
   * Parses the specified reader as a CSV file where the separator is specified and might not be a comma.
   *
   * This factory method takes a `Reader`.
   * Callers are encouraged to use `CharSource` instead of `Reader`
   * as it allows the resource to be safely managed.
   *
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   *
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param reader  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(reader: Reader, headerRow: Boolean, separator: Char): CsvFile = {
    ArgChecker.notNull(reader, "source")
    val lineReader: CheckedSupplier[List[String]] = () => CharStreams.readLines(reader)
    val lines = Unchecked.wrap(lineReader)
    create(lines, headerRow, separator)
  }

  // creates the file
  private def create(lines: List[String], headerRow: Boolean, separator: Char): CsvFile = {
    if (headerRow) {
      var i = 0
      while (i < lines.size()) {
        val headers = parseLine(lines.get(i), separator)
        if (!headers.isEmpty()) {
          val searchHeaders = buildSearchHeaders(headers)
          return parseAll(lines, i + 1, separator, headers, searchHeaders)
        }
        i += 1
      }
      throw new IllegalArgumentException("Could not read header row from empty CSV file")
    }
    parseAll(lines, 0, separator, ImmutableList.of[String](), ImmutableMap.of[String, Integer]())
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the separator used by the specified CSV file.
   *
   * The search includes comma, semicolon, colon, tab and pipe (in that order of priority).
   *
   * The algorithm operates in a number of steps.
   * Firstly, it looks for occurrences where a separator is followed by valid quoted text.
   * If this matches, the separator is assumed to be correct.
   * Secondly, it looks for lines that only consist of a separator.
   * If this matches, the separator is assumed to be correct.
   * Thirdly, it looks to see which separator is the most common on the line.
   * If that separator is also the most common on the next line, and the number of columns matches,
   * the separator is assumed to be correct. Otherwise another line is processed.
   * Thus to match a separator, there must be two lines with the same number of columns.
   * At most, 100 content lines are read from the file.
   * The default is comma if the file is empty.
   *
   * @param source  the source to read as CSV
   * @return the CSV file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def findSeparator(source: CharSource): Char = {
    val possibleSeparators = ",;\t:|"
    try {
      // equivalent of try-with-resources
      val breader: BufferedReader = source.openBufferedStream()
      var primary: Throwable = null
      try {
        readSeparator(breader, possibleSeparators)
      } catch {
        case ex: Throwable =>
          primary = ex
          throw ex
      } finally {
        if (breader != null) {
          if (primary != null) {
            try {
              breader.close()
            } catch {
              case closeEx: Throwable => primary.addSuppressed(closeEx)
            }
          } else {
            breader.close()
          }
        }
      }
    } catch {
      case ex: IOException =>
        throw new UncheckedIOException(ex)
    }
  }

  // finds the separator from the open reader
  @throws[IOException]
  private def readSeparator(breader: BufferedReader, possibleSeparators: String): Char = {
    var bestCount = 0
    var bestSeparator = ','
    var line = breader.readLine()
    var contentLines = 0
    var matched = false
    while (!matched && line != null && contentLines <= 100) {
      if (line.length() == 0 || line.startsWith("#")) {
        // comment
      } else if (line.startsWith(";") && bestCount == 0 && bestSeparator == ',') {
        // if we see semicolon it could be a start of comment or a semicolon separator
        bestSeparator = ';'
      } else {
        line = simplifyLine(line)
        var lineBestCount = 0
        var lineBestSeparator = ','
        val separators = possibleSeparators.toCharArray()
        var index = 0
        while (index < separators.length) {
          val separator = separators(index)
          index += 1
          // a quote following a separator is a strong marker for the separator
          if (line.contains(String.valueOf(separator) + "\"\"")) {
            return separator
          }
          // a line only formed of separators is a strong marker for the separator
          if (line.length() > 1 && line.equals(Strings.repeat(Character.toString(separator), line.length()))) {
            return separator
          }
          // a minimal line of a separator is a weak marker for the separator
          if (line.length() == 1 && line.charAt(0) == separator && bestCount == 0 && bestSeparator == ',') {
            bestCount = 1
            bestSeparator = separator
          }
          // parse the row and see if it is the best match
          if (line.length() > 1) {
            val count = CsvFile.parseLine(line, separator).size()
            if (count > lineBestCount) {
              lineBestCount = count
              lineBestSeparator = separator
            }
          }
        }
        if (lineBestCount > 0) {
          contentLines += 1
          if (bestCount > 0 && bestCount == lineBestCount && bestSeparator == lineBestSeparator) {
            matched = true
          } else {
            bestCount = lineBestCount
            bestSeparator = lineBestSeparator
          }
        }
      }
      if (!matched) {
        line = breader.readLine()
      }
    }
    bestSeparator
  }

  // simplifies a line, removing quoted sections (which assumes the input is validly quoted)
  private def simplifyLine(line: String): String = {
    var pos = 0
    var quoteMode = false
    val buf = new java.lang.StringBuilder(line.length())
    while (pos < line.length()) {
      val ch = line.charAt(pos)
      pos += 1
      if (quoteMode) {
        // currently in quote mode
        if (ch == '"' && pos < line.length() - 1 && line.charAt(pos) == '"') {
          // two double quotes treated as a normal character, as thus skipped
          pos += 1
        } else if (ch == '"' || pos == line.length()) {
          // end of quoted section, or end of string with quote not terminated properly
          buf.append('"').append('"')
          quoteMode = false
        } else {
          // skip characters in quotes
        }
      } else if (ch == '"') {
        // quoted mode
        quoteMode = true
      } else if (ch == ' ' || ch == '=') {
        // ignore awkward characters
      } else {
        // append all other characters
        buf.append(ch)
      }
    }
    buf.toString()
  }

  //------------------------------------------------------------------------
  /**
   * Obtains an instance from a list of headers and rows.
   *
   * The headers may be an empty list.
   * All the rows must contain a list of the same size, matching the header if present.
   *
   * @param headers  the headers, empty if no headers
   * @param rows  the data rows
   * @return the CSV file
   * @throws java.lang.IllegalArgumentException if the rows do not match the headers
   */
  def of(headers: List[String], rows: List[? <: List[String]]): CsvFile = {
    ArgChecker.notNull(headers, "headers")
    ArgChecker.notNull(rows, "rows")
    val size = if (headers.size() == 0 && rows.size() > 0) rows.get(0).size() else headers.size()
    if (rows.stream().filter((row: List[String]) => row.size() != size).findAny().isPresent()) {
      throw new IllegalArgumentException("Invalid data rows, each row must have same columns as header row")
    }
    val copiedHeaders = ImmutableList.copyOf[String](headers)
    val searchHeaders = buildSearchHeaders(copiedHeaders)
    val csvRows = ImmutableList.builder[CsvRow]()
    val firstLine = if (copiedHeaders.isEmpty()) 1 else 2
    var i = 0
    while (i < rows.size()) {
      csvRows.add(new CsvRow(copiedHeaders, searchHeaders, i + firstLine, ImmutableList.copyOf[String](rows.get(i))))
      i += 1
    }
    new CsvFile(copiedHeaders, searchHeaders, csvRows.build())
  }

  //------------------------------------------------------------------------
  // parses the CSV file format
  private def parseAll(
      lines: List[String],
      lineIndex: Int,
      separator: Char,
      headers: ImmutableList[String],
      searchHeaders: ImmutableMap[String, Integer]): CsvFile = {

    val rows = ImmutableList.builder[CsvRow]()
    var i = lineIndex
    while (i < lines.size()) {
      val fields = parseLine(lines.get(i), separator)
      if (!fields.isEmpty()) {
        rows.add(new CsvRow(headers, searchHeaders, i + 1, fields))
      }
      i += 1
    }
    new CsvFile(headers, searchHeaders, rows.build())
  }

  // parse a single line
  private[io] def parseLine(line: String, separator: Char): ImmutableList[String] = {
    if (line.length() == 0 || line.startsWith("#") || (line.startsWith(";") && separator != ';')) {
      return ImmutableList.of[String]()
    }
    val builder = ImmutableList.builder[String]()
    val terminated = line + separator
    // three modes of parsing - base, value and quote
    // to match other lenient parsers, when quote mode finishes, the mode switches to value with the result combined
    var pos = 0
    var startPos = 0
    var value = ""
    var valueMode = false
    var quoteMode = false
    while (pos < terminated.length()) {
      val ch = terminated.charAt(pos)
      pos += 1
      if (quoteMode) {
        // currently in quote mode
        if (ch == '"' && pos < terminated.length() - 1 && terminated.charAt(pos) == '"') {
          // two double quotes will become one
          pos += 1
        } else if (ch == '"') {
          // end of quoted section
          value = terminated.substring(startPos, pos - 1).replace("\"\"", "\"")
          startPos = pos
          quoteMode = false
        } else if (pos == terminated.length()) {
          // end of string with quote not terminated properly
          builder.add(terminated.substring(startPos, pos - 1).replace("\"\"", "\""))
        }
      } else if (valueMode) {
        // currently in value mode
        if (ch == separator) {
          builder.add(value + terminated.substring(startPos, pos - 1).trim())
          valueMode = false
          value = ""
        }
      } else if (ch == separator) {
        // handle empty value
        builder.add("")
      } else if (ch == ' ') {
        // ignore spaces after separators
      } else if (ch == '=' && pos < terminated.length() - 1 && terminated.charAt(pos) == '"') {
        // handle convention where ="xxx" means xxx by simply ignoring the equals
      } else if (ch == '"') {
        // quoted mode
        startPos = pos
        quoteMode = true
        valueMode = true
      } else {
        // non-quoted mode
        startPos = pos - 1
        valueMode = true
      }
    }
    // check line has content
    val fields = builder.build()
    if (!hasContent(fields)) {
      return ImmutableList.of[String]()
    }
    fields
  }

  // determines whether there is any content on a line
  // this handles lines that contain separators but nothing else
  private def hasContent(fields: ImmutableList[String]): Boolean = {
    val it = fields.iterator()
    while (it.hasNext()) {
      if (!it.next().isEmpty()) {
        return true
      }
    }
    false
  }

  // build the search headers
  private[io] def buildSearchHeaders(headers: ImmutableList[String]): ImmutableMap[String, Integer] = {
    // need to allow duplicate headers and only store the first instance
    val searchHeaders: Map[String, Integer] = new HashMap[String, Integer]()
    var i = 0
    while (i < headers.size()) {
      val searchHeader = headers.get(i).toLowerCase(Locale.ENGLISH)
      searchHeaders.putIfAbsent(searchHeader, Integer.valueOf(i))
      i += 1
    }
    ImmutableMap.copyOf(searchHeaders)
  }

}
