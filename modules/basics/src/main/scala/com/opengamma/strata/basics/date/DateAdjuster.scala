/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster

/**
 * Functional interface that can adjust a date.
 *
 * This extends `TemporalAdjuster` for those cases where the temporal to
 * be adjusted is an ISO-8601 date.
 */
@FunctionalInterface
trait DateAdjuster extends TemporalAdjuster {

  /**
   * Adjusts the date according to the rules of the implementation.
   *
   * Implementations must specify how the date is adjusted.
   *
   * @param date  the date to adjust
   * @return the adjusted date
   * @throws java.time.DateTimeException if unable to make the adjustment
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  def adjust(date: LocalDate): LocalDate

  /**
   * Adjusts the temporal according to the rules of the implementation.
   *
   * This method implements `TemporalAdjuster` by calling `adjust(LocalDate)`.
   * Note that conversion to `LocalDate` ignores the calendar system
   * of the input, which is the desired behaviour in this case.
   *
   * @param temporal  the temporal to adjust
   * @return the adjusted temporal
   * @throws java.time.DateTimeException if unable to make the adjustment
   * @throws java.lang.ArithmeticException if numeric overflow occurs
   */
  override def adjustInto(temporal: Temporal): Temporal = {
    // conversion to LocalDate ensures that other calendar systems are ignored
    temporal.`with`(adjust(LocalDate.from(temporal)))
  }

}
