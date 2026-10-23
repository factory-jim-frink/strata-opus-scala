/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.util.HashMap
import java.util.Locale
import java.util.Map
import java.util.Optional
import java.util.function.Function
import java.util.regex.Pattern

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.result.ParseFailureException

/**
 * A row in a CSV file.
 *
 * Represents a single row in a CSV file, accessed via `CsvFile`.
 * Each row object provides access to the data in the row by field index.
 * If the CSV file has headers, the headers can also be used to lookup the fields.
 */
final class CsvRow private[io] (
    /**
     * The header row, ordered as the headers appear in the file.
     */
    private val _headers: ImmutableList[String],
    /**
     * The header map, transformed for case-insensitive searching.
     */
    private val searchHeaders: ImmutableMap[String, Integer],
    /**
     * The line number in the source file.
     */
    private val _lineNumber: Int,
    /**
     * The fields in the row.
     */
    private val _fields: ImmutableList[String]) {
  // some methods have been inlined/simplified for startup/performance reasons
  // the backing fields are prefixed to avoid clashing with the accessor methods

  //------------------------------------------------------------------------
  /**
   * Creates an instance, specifying the headers and row.
   *
   * See `CsvFile`.
   *
   * @param headers  the headers
   * @param fields  the fields
   */
  private def this(headers: ImmutableList[String], lineNumber: Int, fields: ImmutableList[String]) = {
    this(headers, CsvRow.buildSearchHeaders(headers), lineNumber, fields)
  }

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
   * Gets the line number in the source file.
   *
   * @return the line number
   */
  def lineNumber(): Int = {
    _lineNumber
  }

  /**
   * Gets all fields in the row.
   *
   * @return the fields
   */
  def fields(): ImmutableList[String] = {
    _fields
  }

  /**
   * Gets the number of fields.
   *
   * This will never be less than the number of headers.
   *
   * @return the number of fields
   */
  def fieldCount(): Int = {
    Math.max(_fields.size(), _headers.size())
  }

  /**
   * Gets the specified field.
   *
   * @param index  the field index
   * @return the field
   * @throws java.lang.IndexOutOfBoundsException if the field index is invalid
   */
  def field(index: Int): String = {
    if (index >= _fields.size() && index < _headers.size()) {
      ""
    } else {
      _fields.get(index)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a single field value from the row by header.
   *
   * This returns the value of the first column where the header matches the specified header.
   * Matching is case insensitive.
   *
   * @param header  the column header
   * @return the field value, trimmed unless surrounded by quotes
   * @throws com.opengamma.strata.collect.result.ParseFailureException if the header is not found
   */
  def getField(header: String): String = {
    val index = findIndex(header)
    if (index == null) {
      throw new ParseFailureException("Header not found: '{header}'", header)
    }
    field(index.intValue())
  }

  /**
   * Gets a single field value from the row by header, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header.
   * Matching is case insensitive.
   *
   * The value is post processed via the specified function.
   *
   * @tparam T  the post process result type
   * @param header  the column header
   * @param postProcessor  the post processor
   * @return the post processed field value
   * @throws com.opengamma.strata.collect.result.ParseFailureException if the header is not found
   */
  def getField[T](header: String, postProcessor: Function[String, T]): T = {
    val value = getField(header)
    postProcessor.apply(value)
  }

  /**
   * Gets a single field value from the row by header.
   *
   * This returns the value of the first column where the header matches the specified header.
   * Matching is case insensitive.
   *
   * If the value needs post processing, use `Optional.map(Function)`.
   *
   * @param header  the column header
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findField(header: String): Optional[String] = {
    val index = findIndex(header)
    if (index == null) Optional.empty() else Optional.of(field(index.intValue()))
  }

  // finds the index of the specified header
  private def findIndex(header: String): Integer = {
    searchHeaders.get(header.toLowerCase(Locale.ENGLISH))
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a single field value from the row by header pattern.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   *
   * @param headerPattern  the header pattern to match
   * @return the field value, trimmed unless surrounded by quotes
   * @throws java.lang.IllegalArgumentException if the header is not found
   */
  def getField(headerPattern: Pattern): String = {
    var i = 0
    while (i < _headers.size()) {
      if (headerPattern.matcher(_headers.get(i)).matches()) {
        return field(i)
      }
      i += 1
    }
    throw new IllegalArgumentException("Header pattern not found: '" + headerPattern + "'")
  }

  /**
   * Gets a single field value from the row by header pattern, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   *
   * The value is post processed via the specified function.
   *
   * @tparam T  the post process result type
   * @param headerPattern  the header pattern to match
   * @param postProcessor  the post processor
   * @return the post processed field value
   * @throws java.lang.IllegalArgumentException if the header is not found
   */
  def getField[T](headerPattern: Pattern, postProcessor: Function[String, T]): T = {
    val value = getField(headerPattern)
    postProcessor.apply(value)
  }

  /**
   * Gets a single field value from the row by header pattern.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   *
   * If the value needs post processing, use `Optional.map(Function)`.
   *
   * @param headerPattern  the header pattern to match
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findField(headerPattern: Pattern): Optional[String] = {
    var i = 0
    while (i < _headers.size()) {
      if (headerPattern.matcher(_headers.get(i)).matches()) {
        return Optional.of(field(i))
      }
      i += 1
    }
    Optional.empty()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a single field value from the row by header.
   *
   * This returns the value of the first column where the header matches the specified header.
   * If the header is not found or the value found is an empty string, then an IllegalArgumentException is thrown.
   *
   * @param header  the column header
   * @return the field value, trimmed unless surrounded by quotes
   * @throws com.opengamma.strata.collect.result.ParseFailureException if the header is not found or if the value in the field is empty.
   */
  def getValue(header: String): String = {
    val value = getField(header)
    if (value.isEmpty()) {
      throw new ParseFailureException("No value was found for '{header}'", header)
    }
    value
  }

  /**
   * Gets a single field value from the row by header, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header.
   * If the header is not found or the value found is an empty string, then an IllegalArgumentException is thrown.
   *
   * The value is post processed via the specified function.
   *
   * @tparam T  the post process result type
   * @param header  the column header
   * @param postProcessor  the post processor
   * @return the post processed field value
   * @throws com.opengamma.strata.collect.result.ParseFailureException if the header is not found or if the value in the field is empty.
   */
  def getValue[T](header: String, postProcessor: Function[String, T]): T = {
    val value = getValue(header)
    postProcessor.apply(value)
  }

  /**
   * Gets a single value from the row by header.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   * If the value is an empty string, then an empty optional is returned.
   *
   * If the value needs post processing, use `CsvRow.findValue(String, Function)`.
   *
   * @param header  the column header
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findValue(header: String): Optional[String] = {
    findField(header).filter(str => !str.isEmpty())
  }

  /**
   * Gets a single value from the row by header pattern, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header pattern. If the value is
   * an empty string, then an empty optional is returned.
   *
   * @tparam T the post process result type
   * @param header the column header
   * @param postProcessor the post processor
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findValue[T](header: String, postProcessor: Function[String, T]): Optional[T] = {
    findValue(header).map(postProcessor)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a single field value from the row by header pattern.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   * If the header is not found or the value found is an empty string, then an IllegalArgumentException is thrown.
   *
   * @param headerPattern  the header pattern to match
   * @return the field value, trimmed unless surrounded by quotes
   * @throws java.lang.IllegalArgumentException if the header is not found or if the value in the field is empty.
   */
  def getValue(headerPattern: Pattern): String = {
    val value = getField(headerPattern)
    if (value.isEmpty()) {
      throw new IllegalArgumentException("No value was found for header pattern: '" + headerPattern + "'")
    }
    value
  }

  /**
   * Gets a single field value from the row by header pattern, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   * If the header is not found or the value found is an empty string, then an IllegalArgumentException is thrown.
   *
   * The value is post processed via the specified function.
   *
   * @tparam T  the post process result type
   * @param headerPattern  the header pattern to match
   * @param postProcessor  the post processor
   * @return the post processed field value
   * @throws java.lang.IllegalArgumentException if the header is not found or if the value in the field is empty.
   */
  def getValue[T](headerPattern: Pattern, postProcessor: Function[String, T]): T = {
    val value = getValue(headerPattern)
    postProcessor.apply(value)
  }

  /**
   * Gets a single value from the row by header pattern.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   * If the value is an empty string, then an empty optional is returned.
   *
   * If the value needs post processing, use `CsvRow.findValue(Pattern, Function)`.
   *
   * @param headerPattern  the header pattern to match
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findValue(headerPattern: Pattern): Optional[String] = {
    findField(headerPattern).filter(str => !str.isEmpty())
  }

  /**
   * Gets a single value from the row by header pattern, post processing the result.
   *
   * This returns the value of the first column where the header matches the specified header pattern.
   * If the value is an empty string, then an empty optional is returned.
   *
   * @tparam T the post process result type
   * @param headerPattern the header pattern to match
   * @param postProcessor the post processor
   * @return the field value, trimmed unless surrounded by quotes, empty if not found
   */
  def findValue[T](headerPattern: Pattern, postProcessor: Function[String, T]): Optional[T] = {
    findValue(headerPattern).map(postProcessor)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a sub-row, containing a selection of fields by index.
   *
   * All fields after the specified index are included.
   *
   * @param startInclusive  the start index, zero-based, inclusive
   * @return the sub row
   */
  def subRow(startInclusive: Int): CsvRow = {
    subRow(startInclusive, _fields.size())
  }

  /**
   * Obtains a sub-row, containing a selection of fields by index.
   *
   * @param startInclusive  the start index, zero-based, inclusive
   * @param endExclusive  the end index, zero-based, exclusive
   * @return the sub row
   */
  def subRow(startInclusive: Int, endExclusive: Int): CsvRow = {
    new CsvRow(
        _headers.subList(Math.min(startInclusive, _headers.size()), Math.min(endExclusive, _headers.size())),
        _lineNumber,
        _fields.subList(startInclusive, endExclusive))
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
        case other: CsvRow =>
          _headers.equals(other._headers) && _fields.equals(other._fields)
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
    _headers.hashCode() ^ _fields.hashCode()
  }

  /**
   * Returns a string describing the CSV file.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    "CsvRow" + _fields.toString()
  }

}

object CsvRow {

  // builds the search headers
  private def buildSearchHeaders(headers: ImmutableList[String]): ImmutableMap[String, Integer] = {
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
