/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard period addition conventions.
 *
 * The purpose of each convention is to define how to handle the addition of a period.
 * The default implementations include two different end-of-month rules.
 * The convention is generally only applicable for month-based periods.
 */
final class PeriodAdditionConventions private () {
}

object PeriodAdditionConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * No specific rule applies.
   *
   * Given a date, the specified period is added using standard date arithmetic.
   * The business day adjustment is applied to produce the final result.
   *
   * For example, adding a period of 1 month to June 30th will result in July 30th.
   */
  @static final val NONE: PeriodAdditionConvention =
    PeriodAdditionConvention.of(StandardPeriodAdditionConventions.NONE.getName())
  /**
   * Convention applying a last day of month rule, ''ignoring business days''.
   *
   * Given a date, the specified period is added using standard date arithmetic,
   * shifting to the end-of-month if the base date is the last day of the month.
   * The business day adjustment is applied to produce the final result.
   * Note that this rule is based on the last day of the month, not the last business day of the month.
   *
   * For example, adding a period of 1 month to June 30th will result in July 31st.
   */
  @static final val LAST_DAY: PeriodAdditionConvention =
    PeriodAdditionConvention.of(StandardPeriodAdditionConventions.LAST_DAY.getName())
  /**
   * Convention applying a last ''business'' day of month rule.
   *
   * Given a date, the specified period is added using standard date arithmetic,
   * shifting to the last business day of the month if the base date is the
   * last business day of the month.
   * The business day adjustment is applied to produce the final result.
   *
   * For example, adding a period of 1 month to June 29th will result in July 31st
   * assuming that June 30th is not a valid business day and July 31st is.
   */
  @static final val LAST_BUSINESS_DAY: PeriodAdditionConvention =
    PeriodAdditionConvention.of(StandardPeriodAdditionConventions.LAST_BUSINESS_DAY.getName())

  /**
   * The extended enum lookup from name to instance.
   */
  private val ENUM_LOOKUP: ExtendedEnum[PeriodAdditionConvention] =
    ExtendedEnum.of(classOf[PeriodAdditionConvention])

  // package-private in the Java API; held by the module and exposed through an accessor
  // because a @static field would be emitted as public static final
  private[date] def enumLookup(): ExtendedEnum[PeriodAdditionConvention] = ENUM_LOOKUP

}
