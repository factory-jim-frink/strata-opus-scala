/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

import java.util.EnumMap
import java.util.Locale
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet

import com.google.common.base.CaseFormat
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * Helper that allows enum names to be created and parsed.
 *
 * @tparam T  the type of the enum
 */
final class EnumNames[T <: java.lang.Enum[T] & NamedEnum] private (
    /**
     * Class of the enum.
     */
    private val enumType: Class[T],
    rawParseMap: SortedMap[String, T],
    rawFormattedSet: SortedSet[String],
    /**
     * Format map (mutable, but treated as immutable).
     */
    private val formatMap: EnumMap[T, String]) {

  /**
   * Parsing map.
   */
  private val parseMap: ImmutableSortedMap[String, T] = ImmutableSortedMap.copyOf(rawParseMap)
  /**
   * Formatted forms.
   */
  private val formattedSet: ImmutableSortedSet[String] = ImmutableSortedSet.copyOf(rawFormattedSet)

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with an additional alias added.
   *
   * @param alias  the string of the alias
   * @param value  the enum value that the alias represents
   * @return the converted name
   */
  def withParseAlias(alias: String, value: T): EnumNames[T] = {
    val parseMap: SortedMap[String, T] = new TreeMap[String, T](this.parseMap)
    val formattedSet: SortedSet[String] = new TreeSet[String](this.formattedSet)
    parseMap.put(alias, value)
    parseMap.put(alias.toUpperCase(Locale.ENGLISH), value)
    parseMap.put(alias.toLowerCase(Locale.ENGLISH), value)
    formattedSet.add(alias)
    new EnumNames[T](enumType, parseMap, formattedSet, formatMap)
  }

  /**
   * Creates a standard Strata mixed case name from an enum-style constant.
   *
   * @param value  the enum value to convert
   * @return the converted name
   */
  def format(value: T): String = {
    // this should never return null
    formatMap.get(value)
  }

  /**
   * Parses the standard external name for an enum.
   *
   * @param name  the external name
   * @return the enum value
   */
  def parse(name: String): T = {
    ArgChecker.notNull(name, "name")
    val value = parseMap.get(name)
    if (value == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unknown enum name '{}' for type {}, valid values are {}", name, enumType.getName(), formattedSet))
    }
    value
  }

}

object EnumNames {

  /**
   * Creates an instance deriving the formatted string from the enum constant name.
   *
   * @tparam T  the type of the enum
   * @param enumType  the type of the enum
   * @return the names instance
   */
  def of[T <: java.lang.Enum[T] & NamedEnum](enumType: Class[T]): EnumNames[T] = {
    create(enumType, false)
  }

  /**
   * Creates an instance where the `toString` method is written manually.
   *
   * The `toString` method is called to extract the correct formatted string.
   *
   * @tparam T  the type of the enum
   * @param enumType  the type of the enum
   * @return the names instance
   */
  def ofManualToString[T <: java.lang.Enum[T] & NamedEnum](enumType: Class[T]): EnumNames[T] = {
    create(enumType, true)
  }

  // restricted constructor
  private def create[T <: java.lang.Enum[T] & NamedEnum](enumType: Class[T], manualToString: Boolean): EnumNames[T] = {
    ArgChecker.notNull(enumType, "enumType")
    val parseMap: SortedMap[String, T] = new TreeMap[String, T]()
    val formattedSet: SortedSet[String] = new TreeSet[String]()
    val formatMap: EnumMap[T, String] = new EnumMap[T, String](enumType)
    for (value <- enumType.getEnumConstants()) {
      val formatted =
        if (manualToString) value.toString() else CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value.name())
      parseMap.put(value.name(), value)
      parseMap.put(value.name().toUpperCase(Locale.ENGLISH), value)
      parseMap.put(value.name().toLowerCase(Locale.ENGLISH), value)
      parseMap.put(formatted, value)
      parseMap.put(formatted.toUpperCase(Locale.ENGLISH), value)
      parseMap.put(formatted.toLowerCase(Locale.ENGLISH), value)
      formattedSet.add(formatted)
      formatMap.put(value, formatted)
    }
    new EnumNames[T](enumType, parseMap, formattedSet, formatMap)
  }

}
