/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard business day conventions.
 *
 * The purpose of each convention is to define how to handle non-business days.
 * When processing dates in finance, it is typically intended that non-business days,
 * such as weekends and holidays, are converted to a nearby valid business day.
 * The convention, in conjunction with a holiday calendar,
 * defines exactly how the adjustment should be made.
 */
final class BusinessDayConventions private () {
}

object BusinessDayConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The 'NoAdjust' convention which makes no adjustment.
   *
   * The input date will not be adjusted even if it is not a business day.
   */
  @static final val NO_ADJUST: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.NO_ADJUST.getName())
  /**
   * The 'Following' convention which adjusts to the next business day.
   *
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day.
   */
  @static final val FOLLOWING: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.FOLLOWING.getName())
  /**
   * The 'ModifiedFollowing' convention which adjusts to the next business day without crossing month end.
   *
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day unless that day is in a different
   * calendar month, in which case the previous business day is returned.
   */
  @static final val MODIFIED_FOLLOWING: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_FOLLOWING.getName())
  /**
   * The 'ModifiedFollowingBiMonthly' convention which adjusts to the next business day without
   * crossing mid-month or month end.
   *
   * If the input date is not a business day then the date is adjusted.
   * The month is divided into two parts, the first half, the 1st to 15th and the 16th onwards.
   * The adjusted date is the next business day unless that day is in a different half-month,
   * in which case the previous business day is returned.
   */
  @static final val MODIFIED_FOLLOWING_BI_MONTHLY: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY.getName())
  /**
   * The 'Preceding' convention which adjusts to the previous business day.
   *
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day.
   */
  @static final val PRECEDING: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.PRECEDING.getName())
  /**
   * The 'ModifiedPreceding' convention which adjusts to the previous business day without crossing month start.
   *
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day unless that day is in a different
   * calendar month, in which case the next business day is returned.
   */
  @static final val MODIFIED_PRECEDING: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_PRECEDING.getName())
  /**
   * The 'Nearest' convention which adjusts Sunday and Monday forward, and other days backward.
   *
   * If the input date is not a business day then the date is adjusted.
   * If the input is Sunday or Monday then the next business day is returned.
   * Otherwise the previous business day is returned.
   *
   * Note that despite the name, the algorithm may not return the business day that is actually nearest.
   */
  @static final val NEAREST: BusinessDayConvention =
    BusinessDayConvention.of(StandardBusinessDayConventions.NEAREST.getName())

  /**
   * The extended enum lookup from name to instance.
   */
  private val ENUM_LOOKUP: ExtendedEnum[BusinessDayConvention] =
    ExtendedEnum.of(classOf[BusinessDayConvention])

  // package-private in the Java API; held by the module and exposed through an accessor
  // because a @static field would be emitted as public static final
  private[date] def enumLookup(): ExtendedEnum[BusinessDayConvention] = ENUM_LOOKUP

}
