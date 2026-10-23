/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.Year

/**
 * Date adjusters that perform useful operations on `LocalDate`.
 *
 * This is a static utility class.
 * Returned objects are immutable and thread-safe.
 */
final class DateAdjusters private () {
}

object DateAdjusters {

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that finds the next leap day after the input date.
   *
   * The adjuster returns the next occurrence of February 29 after the input date.
   *
   * @return an adjuster that finds the next leap day
   */
  def nextLeapDay(): DateAdjuster = {
    (input: LocalDate) => nextLeapDay(input)
  }

  /**
   * Finds the next leap day after the input date.
   *
   * @param input  the input date
   * @return the next leap day date
   */
  private[date] def nextLeapDay(input: LocalDate): LocalDate = {
    // already a leap day, move forward either 4 or 8 years
    if (input.getMonthValue() == 2 && input.getDayOfMonth() == 29) {
      ensureLeapDay(input.getYear() + 4)
    } else if (input.isLeapYear() && input.getMonthValue() <= 2) {
      // handle if before February 29 in a leap year
      LocalDate.of(input.getYear(), 2, 29)
    } else {
      // handle any other date
      ensureLeapDay(((input.getYear() / 4) * 4) + 4)
    }
  }

  /**
   * Obtains a date adjuster that finds the next leap day on or after the input date.
   *
   * If the input date is February 29, the input date is returned unaltered.
   * Otherwise, the adjuster returns the next occurrence of February 29 after the input date.
   *
   * @return an adjuster that finds the next leap day
   */
  def nextOrSameLeapDay(): DateAdjuster = {
    (input: LocalDate) => nextOrSameLeapDay(input)
  }

  /**
   * Finds the next leap day on or after the input date.
   *
   * If the input date is February 29, the input date is returned unaltered.
   * Otherwise, the adjuster returns the next occurrence of February 29 after the input date.
   *
   * @param input  the input date
   * @return the next leap day date
   */
  private[date] def nextOrSameLeapDay(input: LocalDate): LocalDate = {
    // already a leap day, return it
    if (input.getMonthValue() == 2 && input.getDayOfMonth() == 29) {
      input
    } else if (input.isLeapYear() && input.getMonthValue() <= 2) {
      // handle if before February 29 in a leap year
      LocalDate.of(input.getYear(), 2, 29)
    } else {
      // handle any other date
      ensureLeapDay(((input.getYear() / 4) * 4) + 4)
    }
  }

  // handle 2100, which is not a leap year
  private def ensureLeapDay(possibleLeapYear: Int): LocalDate = {
    if (Year.isLeap(possibleLeapYear)) {
      LocalDate.of(possibleLeapYear, 2, 29)
    } else {
      LocalDate.of(possibleLeapYear + 4, 2, 29)
    }
  }

}
