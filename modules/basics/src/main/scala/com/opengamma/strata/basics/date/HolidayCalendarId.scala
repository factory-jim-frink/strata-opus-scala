/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.io.Serializable
import java.util.Comparator
import java.util.List
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.stream.Collectors

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.ReferenceDataId
import com.opengamma.strata.basics.ReferenceDataNotFoundException
import com.opengamma.strata.basics.Resolvable
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.currency.CurrencyPair
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.named.Named

/**
 * An identifier for a holiday calendar.
 *
 * This identifier is used to obtain a `HolidayCalendar` from `ReferenceData`.
 * The holiday calendar itself is used to determine whether a day is a business day or not.
 *
 * Identifiers for common holiday calendars are provided in `HolidayCalendarIds`.
 */
@SerialVersionUID(1L)
final class HolidayCalendarId private (
    /**
     * The identifier, expressed as a normalized unique name.
     */
    private val name: String,
    rawResolver: BiFunction[HolidayCalendarId, ReferenceData, HolidayCalendar])
  extends ReferenceDataId[HolidayCalendar] with Resolvable[HolidayCalendar] with Named with Serializable {

  /**
   * The hash code.
   */
  @transient private val cachedHashCode: Int = name.hashCode()
  /**
   * The resolver function.
   * Implementations of this function must only call `ReferenceData#queryValueOrNull(ReferenceDataId)`.
   */
  @transient private val resolver: BiFunction[HolidayCalendarId, ReferenceData, HolidayCalendar] =
    if (rawResolver != null) rawResolver else (id, refData) => refData.queryValueOrNull(this)

  //-------------------------------------------------------------------------
  // creates an identifier for a single calendar
  private def this(normalizedName: String) = {
    this(normalizedName, null)
  }

  // resolve after deserialization
  private def readResolve(): Object = {
    HolidayCalendarId.of(name)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this calendar.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  override def getName(): String = {
    name
  }

  /**
   * Gets the type of data this identifier refers to.
   *
   * A `HolidayCalendarId` refers to a `HolidayCalendar`.
   *
   * @return the type of the reference data this identifier refers to
   */
  override def getReferenceDataType(): Class[HolidayCalendar] = {
    classOf[HolidayCalendar]
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves this identifier to a holiday calendar using the specified reference data.
   *
   * This returns an instance of `HolidayCalendar` that can perform calculations.
   *
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   *
   * @param refData  the reference data, used to resolve the reference
   * @return the resolved holiday calendar
   * @throws com.opengamma.strata.basics.ReferenceDataNotFoundException if the identifier is not found
   */
  override def resolve(refData: ReferenceData): HolidayCalendar = {
    refData.getValue(this)
  }

  override def queryValueOrNull(refData: ReferenceData): HolidayCalendar = {
    resolver.apply(this, refData)
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this holiday calendar identifier with another.
   *
   * The resulting calendar will declare a day as a business day if it is a
   * business day in both source calendars.
   *
   * @param other  the other holiday calendar identifier
   * @return the combined holiday calendar identifier
   */
  def combinedWith(other: HolidayCalendarId): HolidayCalendarId = {
    if (this eq other) {
      this
    } else if (this eq HolidayCalendarIds.NO_HOLIDAYS) {
      ArgChecker.notNull(other, "other")
    } else if (other eq HolidayCalendarIds.NO_HOLIDAYS) {
      this
    } else {
      HolidayCalendarId.of(name + '+' + other.name)
    }
  }

  /**
   * Combines this holiday calendar identifier with another.
   *
   * The resulting calendar will declare a day as a business day if it is a
   * business day in either source calendar.
   *
   * @param other  the other holiday calendar identifier
   * @return the combined holiday calendar identifier
   */
  def linkedWith(other: HolidayCalendarId): HolidayCalendarId = {
    if (this eq other) {
      this
    } else if ((this eq HolidayCalendarIds.NO_HOLIDAYS) || (other eq HolidayCalendarIds.NO_HOLIDAYS)) {
      HolidayCalendarIds.NO_HOLIDAYS
    } else {
      HolidayCalendarId.of(name + '~' + other.name)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   *
   * The comparison checks the name.
   *
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    // could use (obj == this), but this code seems to be a little faster
    if (this eq obj.asInstanceOf[AnyRef]) {
      true
    } else if (obj == null || (getClass() ne obj.getClass())) {
      false
    } else {
      val that = obj.asInstanceOf[HolidayCalendarId]
      name.equals(that.name)
    }
  }

  /**
   * Returns a suitable hash code for the identifier.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    cachedHashCode
  }

  /**
   * Returns the name of the identifier.
   *
   * @return the name
   */
  override def toString(): String = {
    name
  }

}

object HolidayCalendarId {

  /** Name splitter. */
  private val SPLITTER_PLUS: Splitter = Splitter.on('+')
  /** Name joiner. */
  private val JOINER_PLUS: Joiner = Joiner.on('+')
  /** Name splitter. */
  private val SPLITTER_WIGGLE: Splitter = Splitter.on('~')
  /** Name joiner. */
  private val JOINER_WIGGLE: Joiner = Joiner.on('~')
  /** Instance cache. */
  private val CACHE: ConcurrentHashMap[String, HolidayCalendarId] = new ConcurrentHashMap[String, HolidayCalendarId]()

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   *
   * The name uniquely identifies the calendar.
   * The `HolidayCalendar` is resolved from `ReferenceData` when required.
   *
   * It is possible to combine two or more calendars using the '+' symbol.
   * For example, 'GBLO+USNY' will combine the separate 'GBLO' and 'USNY' calendars.
   * The resulting identifier will have the individual identifiers normalized into alphabetical order.
   *
   * @param uniqueName  the unique name
   * @return the identifier
   */
  @FromString
  @static def of(uniqueName: String): HolidayCalendarId = {
    val id = CACHE.get(uniqueName)
    if (id != null) id else create(uniqueName)
  }

  // create a new instance atomically, broken out to aid inlining
  private def create(name: String): HolidayCalendarId = {
    // parse names once and build resolver function to aid performance
    // name BBB+CCC+AAA changed to sorted form of AAA+BBB+CCC
    // dedicated resolver function created
    if (name.indexOf('~') >= 0) {
      val ids: List[HolidayCalendarId] = SPLITTER_WIGGLE.splitToList(name).stream()
          .map[HolidayCalendarId](n => HolidayCalendarId.of(n))
          .distinct()
          .sorted(Comparator.comparing[HolidayCalendarId, String]((id: HolidayCalendarId) => id.getName()))
          .collect(Collectors.toList[HolidayCalendarId]())
      if (ids.contains(HolidayCalendarIds.NO_HOLIDAYS)) {
        HolidayCalendarIds.NO_HOLIDAYS
      } else {
        val normalizedName = JOINER_WIGGLE.join(ids)
        val fn: BinaryOperator[HolidayCalendar] = (cal1, cal2) => cal1.linkedWith(cal2)
        create(name, ids, normalizedName, fn)
      }
    } else if (name.indexOf('+') >= 0) {
      val ids: List[HolidayCalendarId] = SPLITTER_PLUS.splitToList(name).stream()
          .filter(n => !n.equals(HolidayCalendarIds.NO_HOLIDAYS.getName()))
          .map[HolidayCalendarId](n => HolidayCalendarId.of(n))
          .distinct()
          .sorted(Comparator.comparing[HolidayCalendarId, String]((id: HolidayCalendarId) => id.getName()))
          .collect(Collectors.toList[HolidayCalendarId]())
      val normalizedName = JOINER_PLUS.join(ids)
      val fn: BinaryOperator[HolidayCalendar] = (cal1, cal2) => cal1.combinedWith(cal2)
      create(name, ids, normalizedName, fn)
    } else {
      val creator: Function[String, HolidayCalendarId] = n => new HolidayCalendarId(name)
      CACHE.computeIfAbsent(name, creator)
    }
  }

  // creates a new complex id
  private def create(
      name: String,
      ids: List[HolidayCalendarId],
      normalizedName: String,
      fn: BinaryOperator[HolidayCalendar]): HolidayCalendarId = {

    val resolver: BiFunction[HolidayCalendarId, ReferenceData, HolidayCalendar] = (id, refData) => {
      var cal: HolidayCalendar = refData.queryValueOrNull(id)
      if (cal != null) {
        cal
      } else {
        val it = ids.iterator()
        while (it.hasNext()) {
          val splitId = it.next()
          val splitCal: HolidayCalendar = refData.queryValueOrNull(splitId)
          if (splitCal == null) {
            throw new ReferenceDataNotFoundException(Messages.format(
                "Reference data not found for '{}' of type 'HolidayCalendarId' when finding '{}'", splitId, id))
          }
          cal = if (cal != null) fn.apply(cal, splitCal) else splitCal
        }
        cal
      }
    }
    // cache under the normalized and non-normalized names
    val creator: Function[String, HolidayCalendarId] = n => new HolidayCalendarId(normalizedName, resolver)
    val id = CACHE.computeIfAbsent(normalizedName, creator)
    CACHE.putIfAbsent(name, id)
    id
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the default calendar for a currency.
   *
   * This uses data from `HolidayCalendarDefaultData.ini` to provide a default.
   *
   * @param currency  the currency to find the default for
   * @return the holiday calendar
   * @throws java.lang.IllegalArgumentException if there is no default for the currency
   */
  def defaultByCurrency(currency: Currency): HolidayCalendarId = {
    HolidayCalendarIniLookup.INSTANCE.defaultByCurrency(currency)
  }

  /**
   * Tries to find a default calendar for a currency.
   *
   * This uses data from `HolidayCalendarDefaultData.ini` to provide a default.
   *
   * @param currency  the currency to find the default for
   * @return the holiday calendar, empty if no calendar found
   */
  def findDefaultByCurrency(currency: Currency): Optional[HolidayCalendarId] = {
    HolidayCalendarIniLookup.INSTANCE.findDefaultByCurrency(currency)
  }

  /**
   * Gets the default calendar for a pair of currencies.
   *
   * This uses data from `HolidayCalendarDefaultData.ini` to provide a default.
   *
   * If no calendar is found, the 'NoHolidays' calendar is used as the default.
   *
   * @param currencyPair the currency pair to find the defaults for
   * @return the holiday calendar
   */
  def defaultByCurrencyPair(currencyPair: CurrencyPair): HolidayCalendarId = {
    val combiner: BinaryOperator[HolidayCalendarId] = (id1, id2) => id1.combinedWith(id2)
    currencyPair.toSet().stream()
        .map[Optional[HolidayCalendarId]](ccy => findDefaultByCurrency(ccy))
        .flatMap(Guavate.filteringOptional[HolidayCalendarId]())
        .reduce(HolidayCalendarIds.NO_HOLIDAYS, combiner)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the holiday calendar is a combined or linked calendar.
   *
   * @param id the holiday calendar id
   * @return if the holiday calendar is a combined or linked calendar
   */
  def isCompositeCalendar(id: HolidayCalendarId): Boolean = {
    id.getName().indexOf('~') >= 0 || id.getName().indexOf('+') >= 0
  }

}
