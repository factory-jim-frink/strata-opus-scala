/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Standard roll convention implementations.
 *
 * See `RollConventions` for the description of each.
 */
// the constructor is package-scoped (public bytecode) because ExtendedEnum instantiates this public class reflectively
private[schedule] final class DayRollConventions private[schedule] () extends NamedLookup[RollConvention] {

  //-------------------------------------------------------------------------
  override def lookupAll(): ImmutableMap[String, RollConvention] = {
    DayRollConventions.MAP
  }

}

private[schedule] object DayRollConventions {

  // lookup of conventions
  private[schedule] val MAP: ImmutableMap[String, RollConvention] = {
    val mapBuilder = ImmutableMap.builder[String, RollConvention]()
    for (roll <- Dom.CONVENTIONS) {
      mapBuilder.put(roll.getName(), roll)
      mapBuilder.put(roll.getName().toUpperCase(Locale.ENGLISH), roll)
    }
    for (roll <- Dow.CONVENTIONS) {
      mapBuilder.put(roll.getName(), roll)
      mapBuilder.put(roll.getName().toUpperCase(Locale.ENGLISH), roll)
    }
    mapBuilder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-month roll convention.
   */
  @SerialVersionUID(1L)
  final class Dom private (
      // day-of-month
      private val day: Int)
    extends RollConvention with Serializable {
    // singleton, so no equals/hashCode

    // unique name
    private val name: String = "Day" + day

    private def readResolve(): Object = {
      Dom.of(day)
    }

    override def getDayOfMonth(): Int = {
      day
    }

    override def adjust(date: LocalDate): LocalDate = {
      ArgChecker.notNull(date, "date")
      if (day >= 29 && date.getMonthValue() == 2) {
        return date.withDayOfMonth(date.lengthOfMonth())
      }
      date.withDayOfMonth(day)
    }

    override def matches(date: LocalDate): Boolean = {
      ArgChecker.notNull(date, "date")
      date.getDayOfMonth() == day ||
          (date.getMonthValue() == 2 && day >= date.lengthOfMonth() && date.getDayOfMonth() == date.lengthOfMonth())
    }

    override def getName(): String = {
      name
    }

    override def toString(): String = {
      name
    }
  }

  object Dom {
    // cache of conventions
    private[schedule] val CONVENTIONS: Array[RollConvention] = {
      val array = new Array[RollConvention](30)
      var i = 0
      while (i < 30) {
        array(i) = new Dom(i + 1)
        i += 1
      }
      array
    }

    // obtains instance
    private[schedule] def of(day: Int): RollConvention = {
      if (day == 31) {
        return RollConventions.EOM
      } else if (day < 1 || day > 30) {
        throw new IllegalArgumentException("Invalid day-of-month: " + day)
      }
      CONVENTIONS(day - 1)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-week roll convention.
   */
  @SerialVersionUID(1L)
  final class Dow private (
      // day-of-week
      private val day: DayOfWeek,
      // unique name
      private val name: String)
    extends RollConvention with Serializable {
    // singleton, so no equals/hashCode

    private def readResolve(): Object = {
      Dow.of(day)
    }

    override def adjust(date: LocalDate): LocalDate = {
      ArgChecker.notNull(date, "date")
      date.`with`(TemporalAdjusters.nextOrSame(day))
    }

    override def matches(date: LocalDate): Boolean = {
      ArgChecker.notNull(date, "date")
      date.getDayOfWeek() eq day
    }

    override def next(date: LocalDate, periodicFrequency: Frequency): LocalDate = {
      ArgChecker.notNull(date, "date")
      ArgChecker.notNull(periodicFrequency, "periodicFrequency")
      val calculated = date.plus(periodicFrequency)
      calculated.`with`(TemporalAdjusters.nextOrSame(day))
    }

    override def previous(date: LocalDate, periodicFrequency: Frequency): LocalDate = {
      ArgChecker.notNull(date, "date")
      ArgChecker.notNull(periodicFrequency, "periodicFrequency")
      val calculated = date.minus(periodicFrequency)
      calculated.`with`(TemporalAdjusters.previousOrSame(day))
    }

    override def getName(): String = {
      name
    }

    override def toString(): String = {
      name
    }
  }

  object Dow {
    // convention names
    private val NAMES: String = "DayMonDayTueDayWedDayThuDayFriDaySatDaySun"
    // cache of conventions
    private[schedule] val CONVENTIONS: Array[RollConvention] = {
      val array = new Array[RollConvention](7)
      var i = 0
      while (i < 7) {
        val dow = DayOfWeek.of(i + 1)
        val name = NAMES.substring(i * 6, (i + 1) * 6)
        array(i) = new Dow(dow, name)
        i += 1
      }
      array
    }

    // obtains instance
    private[schedule] def of(dayOfWeek: DayOfWeek): RollConvention = {
      ArgChecker.notNull(dayOfWeek, "dayOfWeek")
      CONVENTIONS(dayOfWeek.getValue() - 1)
    }
  }

}
