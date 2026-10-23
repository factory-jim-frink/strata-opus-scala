/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import scala.annotation.static

import com.opengamma.strata.collect.named.ExtendedEnum

/**
 * Constants and implementations for standard date sequences.
 *
 * This class provides instances of `DateSequence` representing standard financial industry
 * sequences of dates. The most common are the quarterly IMM dates, which are on the third
 * Wednesday of March, June, September and December.
 *
 * Additional date sequences may be registered by name using `DateSequence.ini`.
 */
final class DateSequences private () {
}

object DateSequences {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The 'Quarterly-IMM' date sequence.
   *
   * An instance defining the sequence of quarterly IMM dates.
   * The quarterly IMM dates are the third Wednesday of March, June, September and December.
   */
  @static final val QUARTERLY_IMM: DateSequence = DateSequence.of(StandardDateSequences.QUARTERLY_IMM.getName())
  /**
   * The 'Quarterly-IMM-6-Serial' date sequence.
   *
   * An instance defining the sequence of 6 serial monthly IMM dates followed by the standard quarterly IMM dates.
   * The IMM dates are the third Wednesday of each month.
   * The quarterly months are March, June, September and December.
   * The base sequence consists of just the quarterly IMM dates.
   * The full sequence consists of the six serial months before joining the base quarterly sequence.
   */
  @static final val QUARTERLY_IMM_6_SERIAL: DateSequence =
    DateSequence.of(StandardDateSequences.QUARTERLY_IMM_6_SERIAL.getName())
  /**
   * The 'Quarterly-IMM-3-Serial' date sequence.
   *
   * An instance defining the sequence of 3 serial monthly IMM dates followed by the standard quarterly IMM dates.
   * The IMM dates are the third Wednesday of each month.
   * The quarterly months are March, June, September and December.
   * The base sequence consists of just the quarterly IMM dates.
   * The full sequence consists of the three serial months before joining the base quarterly sequence.
   */
  @static final val QUARTERLY_IMM_3_SERIAL: DateSequence =
    DateSequence.of(StandardDateSequences.QUARTERLY_IMM_3_SERIAL.getName())
  /**
   * The 'Monthly-IMM' date sequence.
   *
   * An instance defining the sequence of monthly IMM dates.
   * The monthly IMM dates are the third Wednesday of each month.
   */
  @static final val MONTHLY_IMM: DateSequence = DateSequence.of(StandardDateSequences.MONTHLY_IMM.getName())
  /**
   * The 'Quarterly-10th' date sequence.
   *
   * An instance defining the sequence of quarterly dates on the 10th of each month.
   * The quarterly months are March, June, September and December.
   */
  @static final val QUARTERLY_10TH: DateSequence = DateSequence.of(StandardDateSequences.QUARTERLY_10TH.getName())
  /**
   * The 'Monthly-1st' date sequence, equivalent to a sequence of calendar months.
   *
   * An instance defining the sequence of monthly dates on the 1st of each month.
   */
  @static final val MONTHLY_1ST: DateSequence = DateSequence.of(StandardDateSequences.MONTHLY_1ST.getName())

  /**
   * The extended enum lookup from name to instance.
   */
  private val ENUM_LOOKUP: ExtendedEnum[DateSequence] = ExtendedEnum.of(classOf[DateSequence])

  // package-private in the Java API; held by the module and exposed through an accessor
  // because a @static field would be emitted as public static final
  private[date] def enumLookup(): ExtendedEnum[DateSequence] = ENUM_LOOKUP

}
