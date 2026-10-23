/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.util.LinkedHashMap
import java.util.Map
import java.util.Optional
import java.util.function.BiFunction
import java.util.function.Function

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.MapStream
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * An INI file.
 *
 * Represents an INI file together with the ability to parse it from a `CharSource`.
 *
 * The INI file format used here is deliberately simple.
 * There are two elements - key-value pairs and sections.
 *
 * The basic element is a key-value pair.
 * The key is separated from the value using the '=' symbol.
 * The string ' = ' is searched for before '=' to allow an equals sign to be present
 * in the key, which implies that this string cannot be in either the key or the value.
 * Duplicate keys are allowed.
 * For example 'key = value'.
 * The equals sign and value may be omitted, in which case the value is an empty string.
 *
 * All properties are grouped into named sections.
 * The section name occurs on a line by itself surrounded by square brackets.
 * Duplicate section names are not allowed.
 * For example '[section]'.
 *
 * Keys, values and section names are trimmed.
 * Blank lines are ignored.
 * Whole line comments begin with hash '#' or semicolon ';'.
 * No escape format is available.
 * Lookup is case sensitive.
 *
 * This example explains the format:
 * {{{
 *  # line comment
 *  [foo]
 *  key = value
 *
 *  [bar]
 *  key = value
 *  month = January
 * }}}
 *
 * The aim of this class is to parse the basic format.
 * Interpolation of variables is not supported.
 */
final class IniFile private (
    /**
     * The INI sections.
     */
    private val sectionMap: ImmutableMap[String, PropertySet]) {

  //-------------------------------------------------------------------------
  /**
   * Returns the set of sections of this INI file.
   *
   * @return the set of sections
   */
  def sections(): ImmutableSet[String] = {
    sectionMap.keySet()
  }

  /**
   * Returns the INI file as a map.
   *
   * The iteration order of the map matches that of the original file.
   *
   * @return the INI file sections
   */
  def asMap(): ImmutableMap[String, PropertySet] = {
    sectionMap
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file contains the specified section.
   *
   * @param name  the section name
   * @return true if the section exists
   */
  def contains(name: String): Boolean = {
    ArgChecker.notNull(name, "name")
    sectionMap.containsKey(name)
  }

  /**
   * Gets a single section of this INI file.
   *
   * This returns the section associated with the specified name.
   * If the section does not exist an exception is thrown.
   *
   * @param name  the section name
   * @return the INI file section
   * @throws java.lang.IllegalArgumentException if the section does not exist
   */
  def section(name: String): PropertySet = {
    ArgChecker.notNull(name, "name")
    if (contains(name) == false) {
      throw new IllegalArgumentException("Unknown INI file section: " + name)
    }
    sectionMap.get(name)
  }

  /**
   * Finds a single section in this INI file.
   *
   * This returns the section associated with the specified name, empty if not present.
   *
   * @param name  the section name
   * @return the INI file section, empty if not found
   */
  def findSection(name: String): Optional[PropertySet] = {
    ArgChecker.notNull(name, "name")
    Optional.ofNullable(sectionMap.get(name))
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this file with another.
   *
   * This file takes precedence. Where a key exists in both files the values in the other file
   * will be discarded.
   * Any order of any additional keys will be retained, with those keys located after the base set of keys.
   *
   * @param other  the other INI file
   * @return the combined INI file
   */
  def combinedWith(other: IniFile): IniFile = {
    ArgChecker.notNull(other, "other")

    val combiner: BiFunction[PropertySet, PropertySet, PropertySet] = (first, second) => first.combinedWith(second)
    IniFile.of(MapStream.concat[String, PropertySet](
        MapStream.of(this.sectionMap),
        MapStream.of(other.sectionMap))
        .toMap(combiner))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this INI file equals another.
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
        case other: IniFile =>
          sectionMap.equals(other.sectionMap)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the INI file.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    sectionMap.hashCode()
  }

  /**
   * Returns a string describing the INI file.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    sectionMap.toString()
  }

}

object IniFile {

  //-------------------------------------------------------------------------
  /**
   * Parses the specified source as an INI file.
   *
   * This parses the specified character source expecting an INI file format.
   * The resulting instance can be queried for each section in the file.
   *
   * INI files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using `UnicodeBom`.
   *
   * @param source  the INI file resource
   * @return the INI file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: CharSource): IniFile = {
    ArgChecker.notNull(source, "source")
    val reader: CheckedSupplier[ImmutableList[String]] = () => source.readLines()
    val lines = Unchecked.wrap(reader)
    val parsedIni = parse(lines)
    val builder = ImmutableMap.builder[String, PropertySet]()
    parsedIni.forEach((sectionName: String, sectionData: ImmutableListMultimap[String, String]) => {
      builder.put(sectionName, PropertySet.of(sectionData))
      ()
    })
    new IniFile(builder.build())
  }

  //-------------------------------------------------------------------------
  // parses the INI file format
  private def parse(lines: ImmutableList[String]): ImmutableMap[String, ImmutableListMultimap[String, String]] = {
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    val ini: Map[String, ImmutableListMultimap.Builder[String, String]] =
      new LinkedHashMap[String, ImmutableListMultimap.Builder[String, String]]()
    var currentSection: ImmutableListMultimap.Builder[String, String] = null
    var lineNum = 0
    val it = lines.iterator()
    while (it.hasNext()) {
      lineNum += 1
      val line = it.next().trim()
      if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
        // continue
      } else if (line.startsWith("[") && line.endsWith("]")) {
        val sectionName = line.substring(1, line.length() - 1).trim()
        if (ini.containsKey(sectionName)) {
          throw new IllegalArgumentException("Invalid INI file, duplicate section not allowed, line " + lineNum)
        }
        currentSection = ImmutableListMultimap.builder[String, String]()
        ini.put(sectionName, currentSection)

      } else if (currentSection == null) {
        throw new IllegalArgumentException("Invalid INI file, properties must be within a [section], line " + lineNum)

      } else {
        var equalsPosition = line.indexOf(" = ")
        equalsPosition = if (equalsPosition < 0) line.indexOf('=') else equalsPosition + 1
        val key = if (equalsPosition < 0) line.trim() else line.substring(0, equalsPosition).trim()
        val value = if (equalsPosition < 0) "" else line.substring(equalsPosition + 1).trim()
        if (key.length() == 0) {
          throw new IllegalArgumentException("Invalid INI file, empty key, line " + lineNum)
        }
        currentSection.put(key, value)
      }
    }
    val buildFn: Function[ImmutableListMultimap.Builder[String, String], ImmutableListMultimap[String, String]] =
      b => b.build()
    MapStream.of(ini).mapValues(buildFn).toMap()
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance, specifying the map of section to properties.
   *
   * @param sectionMap  the map of sections
   * @return the INI file
   */
  def of(sectionMap: Map[String, PropertySet]): IniFile = {
    new IniFile(ImmutableMap.copyOf(sectionMap))
  }

}
