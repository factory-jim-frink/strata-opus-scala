/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.io.Serializable
import java.time.LocalDate
import java.util.Locale
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import scala.annotation.static

import com.opengamma.strata.basics.date.DayCount.ScheduleInfo
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Day count convention for 'Bus/252'.
 *
 * This day count is based on a holiday calendar, which is stored in the day count
 * and referenced in the name.
 */
private[date] final class Business252DayCount private () extends NamedLookup[DayCount] {

  // obtains the day count
  private[date] def of(calendar: HolidayCalendar): DayCount = {
    Business252DayCount.BY_CALENDAR.computeIfAbsent(calendar.getName(), calendarName => createByCalendarName(calendarName))
  }

  private def createByCalendarName(calendarName: String): DayCount = {
    lookup("Bus/252 " + calendarName)
  }

  //-------------------------------------------------------------------------
  override def lookup(name: String): DayCount = {
    var value = Business252DayCount.BY_NAME.get(name)
    if (value == null) {
      if (name.regionMatches(true, 0, "Bus/252 ", 0, 8)) {
        val cal = HolidayCalendars.of(name.substring(8))  // load from standard calendars
        val correctName = "Bus/252 " + cal.getName()
        val created: DayCount = new Business252DayCount.Bus252(correctName, cal)
        value = Business252DayCount.BY_NAME.computeIfAbsent(correctName, k => created)
        Business252DayCount.BY_NAME.putIfAbsent(correctName.toUpperCase(Locale.ENGLISH), created)
      }
    }
    value
  }

  override def lookupAll(): Map[String, DayCount] = {
    Business252DayCount.BY_NAME
  }

}

private[date] object Business252DayCount {

  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: Business252DayCount = new Business252DayCount()

  /**
   * The cache of day count by name.
   */
  @static private val BY_NAME: ConcurrentMap[String, DayCount] = new ConcurrentHashMap[String, DayCount]()
  /**
   * The cache of day count by calendar.
   */
  @static private val BY_CALENDAR: ConcurrentMap[String, DayCount] = new ConcurrentHashMap[String, DayCount]()

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-month roll convention.
   */
  @SerialVersionUID(1L)
  final class Bus252 private[date] (
      private val name: String,
      @transient private val calendar: HolidayCalendar)
    extends DayCount with Serializable {

    // resolve instance
    private def readResolve(): Object = {
      DayCount.of(name)
    }

    override def yearFraction(firstDate: LocalDate, secondDate: LocalDate, scheduleInfo: ScheduleInfo): Double = {
      calendar.daysBetween(firstDate, secondDate) / 252d
    }

    override def days(firstDate: LocalDate, secondDate: LocalDate): Int = {
      calendar.daysBetween(firstDate, secondDate)
    }

    //-------------------------------------------------------------------------
    override def getName(): String = {
      name
    }

    override def equals(obj: Any): Boolean = {
      if (obj.asInstanceOf[AnyRef] eq this) {
        true
      } else {
        obj match {
          case other: Bus252 => other.name.equals(name)
          case _ => false
        }
      }
    }

    override def hashCode(): Int = {
      name.hashCode()
    }

    override def toString(): String = {
      name
    }

  }

}
