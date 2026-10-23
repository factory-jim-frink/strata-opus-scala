/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.util.Iterator
import java.util.Spliterator
import java.util.Spliterators
import java.util.stream.Stream
import java.util.stream.StreamSupport

import scala.annotation.static

/**
 * Utilities for working with `LocalDate`.
 */
final class LocalDateUtils private ()

object LocalDateUtils {

  // First day-of-month minus one for a standard year
  // array length 13 with element zero ignored, so month 1 to 12 can be queried directly
  @static private val STANDARD: Array[Int] = Array[Int](0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
  // First day-of-month minus one for a leap year
  // array length 13 with element zero ignored, so month 1 to 12 can be queried directly
  @static private val LEAP: Array[Int] = Array[Int](0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)

  //-------------------------------------------------------------------------
  /**
   * Finds the day-of-year of the date.
   *
   * Faster than the JDK method.
   *
   * @param date  the date to query
   * @return the day-of-year
   */
  @static private[date] def doy(date: LocalDate): Int = {
    val lookup = (if (date.isLeapYear()) LEAP else STANDARD)
    lookup(date.getMonthValue()) + date.getDayOfMonth()
  }

  /**
   * Adds a number of days to the date.
   *
   * Faster than the JDK method.
   *
   * @param date  the date to add to
   * @param daysToAdd  the days to add
   * @return the new date
   */
  @static private[date] def plusDays(date: LocalDate, daysToAdd: Int): LocalDate = {
    if (daysToAdd == 0) {
      return date
    }
    // add the days to the current day-of-month
    // if it is guaranteed to be in this month or the next month then fast path it
    // (59th Jan is 28th Feb, 59th Feb is 31st Mar)
    // the int sum may overflow before it is widened to long
    val dom: Long = (date.getDayOfMonth() + daysToAdd).toLong
    if (dom > 0 && dom <= 59) {
      val monthLen = date.lengthOfMonth()
      val month = date.getMonthValue()
      val year = date.getYear()
      if (dom <= monthLen) {
        return LocalDate.of(year, month, dom.toInt)
      } else if (month < 12) {
        return LocalDate.of(year, month + 1, (dom - monthLen).toInt)
      } else {
        return LocalDate.of(year + 1, 1, (dom - monthLen).toInt)
      }
    }
    val mjDay = Math.addExact(date.toEpochDay(), daysToAdd.toLong)
    LocalDate.ofEpochDay(mjDay)
  }

  /**
   * Returns the number of days between two dates.
   *
   * Faster than the JDK method.
   *
   * @param firstDate  the first date
   * @param secondDate  the second date, after the first
   * @return the new date
   */
  @static private[date] def daysBetween(firstDate: LocalDate, secondDate: LocalDate): Long = {
    val firstYear = firstDate.getYear()
    val secondYear = secondDate.getYear()
    if (firstYear == secondYear) {
      return (doy(secondDate) - doy(firstDate)).toLong
    }
    if ((firstYear + 1) == secondYear) {
      return ((firstDate.lengthOfYear() - doy(firstDate)) + doy(secondDate)).toLong
    }
    secondDate.toEpochDay() - firstDate.toEpochDay()
  }

  //-------------------------------------------------------------------------
  /**
   * Streams the set of dates included in the range.
   *
   * This returns a stream consisting of each date in the range.
   * The stream is ordered.
   *
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the stream of dates from the start to the end
   */
  @static private[date] def stream(startInclusive: LocalDate, endExclusive: LocalDate): Stream[LocalDate] = {
    val it: Iterator[LocalDate] = new Iterator[LocalDate] {
      private var current: LocalDate = startInclusive

      override def next(): LocalDate = {
        val result = current
        current = plusDays(current, 1)
        result
      }

      override def hasNext(): Boolean = {
        current.isBefore(endExclusive)
      }
    }
    val count = endExclusive.toEpochDay() - startInclusive.toEpochDay() + 1
    val spliterator: Spliterator[LocalDate] = Spliterators.spliterator(it, count,
        Spliterator.IMMUTABLE | Spliterator.NONNULL |
            Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED |
            Spliterator.SIZED | Spliterator.SUBSIZED)
    StreamSupport.stream(spliterator, false)
  }

}
