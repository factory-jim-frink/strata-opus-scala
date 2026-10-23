/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A properties file.
 *
 * Represents a properties file together with the ability to parse it from a `CharSource`.
 * This is similar to `Properties` but with a simpler format and without extending `Map`.
 * Duplicate keys are allowed and handled.
 *
 * The properties file format used here is deliberately simple.
 * There is only one element - key-value pairs.
 *
 * The key is separated from the value using the '=' symbol.
 * The string ' = ' is searched for before '=' to allow an equals sign to be present
 * in the key, which implies that this string cannot be in either the key or the value.
 * Duplicate keys are allowed.
 * For example 'key = value'.
 * The equals sign and value may be omitted, in which case the value is an empty string.
 *
 * Keys and values are trimmed.
 * Blank lines are ignored.
 * Whole line comments begin with hash '#' or semicolon ';'.
 * No escape format is available.
 * Lookup is case sensitive.
 *
 * This example explains the format:
 * {{{
 *  # line comment
 *  key = value
 *  month = January
 * }}}
 *
 * The aim of this class is to parse the basic format.
 * Interpolation of variables is not supported.
 */
final class PropertiesFile private (
    /**
     * The key-value pairs.
     */
    private val keyValueMap: PropertySet) {

  //-------------------------------------------------------------------------
  /**
   * Gets all the key-value properties of this file.
   *
   * The map of key-value properties is exposed by this method.
   *
   * @return the key-value properties
   */
  def getProperties(): PropertySet = {
    keyValueMap
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this file equals another.
   *
   * The comparison checks the content.
   *
   * @param obj  the other section, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: PropertiesFile =>
          keyValueMap.equals(other.keyValueMap)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the file.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    keyValueMap.hashCode()
  }

  /**
   * Returns a string describing the file.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    keyValueMap.toString()
  }

}

object PropertiesFile {

  //-------------------------------------------------------------------------
  /**
   * Parses the specified source as a properties file.
   *
   * This parses the specified character source expecting a properties file format.
   * The resulting instance can be queried for each key and value.
   *
   * Properties files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param source  the properties file resource
   * @return the properties file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource): PropertiesFile = {
    ArgChecker.notNull(source, "source")
    val reader: CheckedSupplier[ImmutableList[String]] = () => source.readLines()
    val lines = Unchecked.wrap(reader)
    val keyValues = parse(lines)
    new PropertiesFile(keyValues)
  }

  // parses the properties file format
  private def parse(lines: ImmutableList[String]): PropertySet = {
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    val parsed = ImmutableListMultimap.builder[String, String]()
    var lineNum = 0
    val it = lines.iterator()
    while (it.hasNext()) {
      lineNum += 1
      val line = it.next().trim()
      if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
        // continue
      } else {
        var equalsPosition = line.indexOf(" = ")
        equalsPosition = if (equalsPosition < 0) line.indexOf('=') else equalsPosition + 1
        val key = if (equalsPosition < 0) line.trim() else line.substring(0, equalsPosition).trim()
        val value = if (equalsPosition < 0) "" else line.substring(equalsPosition + 1).trim()
        if (key.length() == 0) {
          throw new IllegalArgumentException("Invalid properties file, empty key, line " + lineNum)
        }
        parsed.put(key, value)
      }
    }
    PropertySet.of(parsed.build())
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a key-value property set.
   *
   * @param keyValueMap  the key-value property set
   * @return the properties file
   */
  def of(keyValueMap: PropertySet): PropertiesFile = {
    ArgChecker.notNull(keyValueMap, "keyValueMap")
    new PropertiesFile(keyValueMap)
  }

}
