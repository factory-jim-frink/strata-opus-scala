/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import com.opengamma.strata.collect.named.CombinedExtendedEnum

/**
 * Helper for `Index`
 */
private[index] final class Indices private ()

private[index] object Indices {

  /**
   * The extended enum lookup from name to instance.
   */
  @static private[index] val INDEX_LOOKUP: CombinedExtendedEnum[Index] = CombinedExtendedEnum.of(classOf[Index])
  /**
   * The extended enum lookup from name to instance.
   */
  @static private[index] val RATE_INDEX_LOOKUP: CombinedExtendedEnum[RateIndex] =
    CombinedExtendedEnum.of(classOf[RateIndex])
  /**
   * The extended enum lookup from name to instance.
   */
  @static private[index] val FLOATING_RATE_INDEX_LOOKUP: CombinedExtendedEnum[FloatingRateIndex] =
    CombinedExtendedEnum.of(classOf[FloatingRateIndex])

}
