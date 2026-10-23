/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.time.LocalDate

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A convention defining how to adjust a date if it falls on a day other than a business day.
 *
 * The purpose of this convention is to define how to handle non-business days.
 * When processing dates in finance, it is typically intended that non-business days,
 * such as weekends and holidays, are converted to a nearby valid business day.
 * The convention, in conjunction with a holiday calendar,
 * defines exactly how the adjustment should be made.
 *
 * The most common implementations are provided in `BusinessDayConventions`.
 * Additional implementations may be added by implementing this interface.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait BusinessDayConvention extends Named {

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date as necessary if it is not a business day.
   *
   * If the date is a business day it will be returned unaltered.
   * If the date is not a business day, the convention will be applied.
   *
   * @param date  the date to adjust
   * @param calendar  the calendar that defines holidays and business days
   * @return the adjusted date
   */
  def adjust(date: LocalDate, calendar: HolidayCalendar): LocalDate

  /**
   * Gets the name that uniquely identifies this convention.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  override def getName(): String

}

object BusinessDayConvention {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the business convention
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): BusinessDayConvention = {
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[BusinessDayConvention] = {
    BusinessDayConventions.enumLookup()
  }

}
