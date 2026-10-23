/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

import java.util.HashMap
import java.util.Map
import java.util.Optional

import scala.annotation.static

/**
 * Provides access to reference data, such as holiday calendars and securities.
 *
 * Reference data is looked up using implementations of `ReferenceDataId`.
 * The identifier is parameterized with the type of the reference data to be returned.
 *
 * The standard implementation is `ImmutableReferenceData`.
 */
trait ReferenceData {

  //-------------------------------------------------------------------------
  /**
   * Checks if this reference data contains a value for the specified identifier.
   *
   * @param id  the identifier to find
   * @return true if the reference data contains a value for the identifier
   */
  def containsValue(id: ReferenceDataId[?]): Boolean = {
    id.queryValueOrNull(this) != null
  }

  /**
   * Gets the reference data value associated with the specified identifier.
   *
   * If this reference data instance contains the identifier, the value will be returned.
   * Otherwise, an exception will be thrown.
   *
   * @tparam T  the type of the reference data value
   * @param id  the identifier to find
   * @return the reference data value
   * @throws com.opengamma.strata.basics.ReferenceDataNotFoundException if the identifier is not found
   */
  def getValue[T](id: ReferenceDataId[T]): T = {
    val value: T = id.queryValueOrNull(this)
    if (value == null) {
      throw new ReferenceDataNotFoundException(ImmutableReferenceData.msgValueNotFound(id))
    }
    value
  }

  /**
   * Finds the reference data value associated with the specified identifier.
   *
   * If this reference data instance contains the identifier, the value will be returned.
   * Otherwise, an empty optional will be returned.
   *
   * @tparam T  the type of the reference data value
   * @param id  the identifier to find
   * @return the reference data value, empty if not found
   */
  def findValue[T](id: ReferenceDataId[T]): Optional[T] = {
    val value: T = id.queryValueOrNull(this)
    Optional.ofNullable(value)
  }

  /**
   * Low-level method to query the reference data value associated with the specified identifier,
   * returning null if not found.
   *
   * This is a low-level method that obtains the reference data value, returning null instead of an error.
   * Applications should use `getValue(ReferenceDataId)` in preference to this method.
   *
   * @tparam T  the type of the reference data value
   * @param id  the identifier to find
   * @return the reference data value, null if not found
   */
  def queryValueOrNull[T](id: ReferenceDataId[T]): T

  //-------------------------------------------------------------------------
  /**
   * Combines this reference data with another.
   *
   * The result combines both sets of reference data.
   * Values are taken from this set of reference data if available, otherwise they are taken
   * from the other set.
   *
   * @param other  the other reference data
   * @return the combined reference data
   */
  def combinedWith(other: ReferenceData): ReferenceData = {
    new CombinedReferenceData(this, other)
  }

}

object ReferenceData {

  /**
   * Obtains an instance from a map of reference data.
   *
   * Each entry in the map is a single piece of reference data, keyed by the matching identifier.
   * For example, a `HolidayCalendar` can be looked up using a `HolidayCalendarId`.
   * The caller must ensure that the each entry in the map corresponds with the parameterized
   * type on the identifier.
   *
   * The resulting `ReferenceData` instance will include the minimal
   * set of reference data that includes non-controversial identifiers that are essential for pricing.
   * To exclude the minimal set of identifiers, use `ImmutableReferenceData#of(Map)`.
   *
   * @param values  the reference data values
   * @return the reference data instance containing the values in the map
   * @throws java.lang.ClassCastException if a value does not match the parameterized type associated with the identifier
   */
  @static def of(values: Map[? <: ReferenceDataId[?], ?]): ReferenceData = {
    // hash map so that keys can overlap, with this instance taking priority
    val combined: Map[ReferenceDataId[?], Any] = new HashMap[ReferenceDataId[?], Any]()
    combined.putAll(StandardReferenceData.MINIMAL.getValues())
    combined.putAll(values)
    ImmutableReferenceData.of(combined)
  }

  /**
   * Obtains an instance of standard reference data.
   *
   * Standard reference data is built into Strata and provides common holiday calendars and indices.
   * In most cases, production usage of Strata will not rely on this source of reference data.
   *
   * @return standard reference data
   */
  @static def standard(): ReferenceData = {
    StandardReferenceData.STANDARD
  }

  /**
   * Obtains the minimal set of reference data.
   *
   * The standard reference data contains common holiday calendars
   * and indices, but may not be suitable for production use. The minimal reference data contains
   * just those identifiers that are needed by Strata, and that are non-controversial.
   * These are `HolidayCalendars#NO_HOLIDAYS`, `HolidayCalendars#SAT_SUN`,
   * `HolidayCalendars#FRI_SAT` and `HolidayCalendars#THU_FRI`.
   *
   * @return minimal reference data
   */
  @static def minimal(): ReferenceData = {
    StandardReferenceData.MINIMAL
  }

  /**
   * Obtains an instance containing no reference data.
   *
   * @return empty reference data
   */
  @static def empty(): ReferenceData = {
    ImmutableReferenceData.empty()
  }

}
