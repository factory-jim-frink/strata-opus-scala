/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.YearMonth

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.date.LocalDateUtils.plusDays
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A series of dates identified by name.
 *
 * This interface encapsulates a sequence of dates as used in standard financial instruments.
 * The most common are the quarterly IMM dates, which are on the third Wednesday of March,
 * June, September and December.
 *
 * The most common implementations are provided in `DateSequences`.
 *
 * Note that the dates produced by the sequence may not be business days.
 * The application of a holiday calendar is typically the responsibility of the caller.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait DateSequence extends Named {

  //-------------------------------------------------------------------------
  /**
   * Returns the simpler "base" sequence underlying this one.
   *
   * Many date sequences have two interlinked sequences.
   * One is considered to be the base sequence, the other is considered to be the full sequence.
   *
   * For example, the "base sequence" of a future is often March, June, September and December.
   * But additionally, the nearest two "serial" months are also listed.
   * Together these make the "full sequence".
   *
   * If this instance represents the "full sequence", this method returns the "base sequence".
   * If this instance represents the "base sequence", or there is no "base sequence", this method returns `this`.
   *
   * @return the base sequence
   */
  def baseSequence(): DateSequence = {
    this
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next date in the sequence, always returning a date later than the input date.
   *
   * Given an input date, this method returns the next date after it from the sequence.
   *
   * @param date  the input date
   * @return the next sequence date after the input date
   * @throws java.lang.IllegalArgumentException if there are no more sequence dates
   */
  def next(date: LocalDate): LocalDate = {
    val next = plusDays(date, 1)
    nextOrSame(next)
  }

  /**
   * Finds the next date in the sequence, returning the input date if it is a date in the sequence.
   *
   * Given an input date, this method returns a date from the sequence.
   * If the input date is in the sequence, it is returned.
   * Otherwise, the next date in the sequence after the input date is returned.
   *
   * @param date  the input date
   * @return the input date if it is a sequence date, otherwise the next sequence date
   * @throws java.lang.IllegalArgumentException if there are no more sequence dates
   */
  def nextOrSame(date: LocalDate): LocalDate

  //-------------------------------------------------------------------------
  /**
   * Finds the nth date in the sequence after the input date,
   * always returning a date later than the input date.
   *
   * Given an input date, this method returns a date from the sequence.
   * If the sequence number is 1, then the first date in the sequence after the input date is returned.
   *
   * If the sequence number is 2 or larger, then the date referred to by sequence number 1
   * is calculated, and the nth matching sequence date after that date returned.
   *
   * @param date  the input date
   * @param sequenceNumber  the 1-based index of the date to find
   * @return the nth sequence date after the input date
   * @throws java.lang.IllegalArgumentException if the sequence number is zero or negative or if there are no more sequence dates
   */
  def nth(date: LocalDate, sequenceNumber: Int): LocalDate = {
    ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
    if (sequenceNumber == 1) {
      next(date)
    } else {
      nth(next(date), sequenceNumber - 1)
    }
  }

  /**
   * Finds the nth date in the sequence on or after the input date,
   * returning the input date if it is a date in the sequence.
   *
   * Given an input date, this method returns a date from the sequence.
   * If the sequence number is 1, then either the input date or the first date
   * in the sequence after the input date is returned.
   *
   * If the sequence number is 2 or larger, then the date referred to by sequence number 1
   * is calculated, and the nth matching sequence date after that date returned.
   *
   * @param date  the input date
   * @param sequenceNumber  the 1-based index of the date to find
   * @return the nth sequence date on or after the input date
   * @throws java.lang.IllegalArgumentException if the sequence number is zero or negative or if there are no more sequence dates
   */
  def nthOrSame(date: LocalDate, sequenceNumber: Int): LocalDate = {
    ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber")
    if (sequenceNumber == 1) {
      nextOrSame(date)
    } else {
      nth(nextOrSame(date), sequenceNumber - 1)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the date in the sequence that corresponds to the specified year-month.
   *
   * Given an input month, this method returns the date from the sequence that is associated with the year-month.
   * In most cases, the returned date will be in the same month as the input month,
   * but this is not guaranteed.
   *
   * @param yearMonth  the input year-month
   * @return the next sequence date after the input date
   * @throws java.lang.IllegalArgumentException if there are no more sequence dates
   */
  def dateMatching(yearMonth: YearMonth): LocalDate

  //-------------------------------------------------------------------------
  /**
   * Selects a date from the sequence.
   *
   * Given an input date, this uses the `SequenceDate` to select a single matching date from the sequence.
   * If the `SequenceDate` specifies a year-month, the match starts from the first date of the specified month.
   * Otherwise, the match starts from the day after the input date.
   *
   * @param inputDate  the input date
   * @param sequenceDate  the instructions specifying which date to select
   * @return the next sequence date after the input date
   * @throws java.lang.IllegalArgumentException if there are no more sequence dates
   */
  def selectDate(inputDate: LocalDate, sequenceDate: SequenceDate): LocalDate = {
    sequenceDate.selectDate(inputDate, this, false)
  }

  /**
   * Selects a date from the sequence.
   *
   * Given an input date, this uses the `SequenceDate` to select a single matching date from the sequence.
   * If the `SequenceDate` specifies a year-month, the match starts from the first date of the specified month.
   * Otherwise, the match starts from the input date.
   *
   * @param inputDate  the input date
   * @param sequenceDate  the instructions specifying which date to select
   * @return the next sequence date after the input date
   * @throws java.lang.IllegalArgumentException if there are no more sequence dates
   */
  def selectDateOrSame(inputDate: LocalDate, sequenceDate: SequenceDate): LocalDate = {
    sequenceDate.selectDate(inputDate, this, true)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this sequence.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  override def getName(): String

}

object DateSequence {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the date sequence
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): DateSequence = {
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the sequence to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[DateSequence] = {
    DateSequences.enumLookup()
  }

}
