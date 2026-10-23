/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * An operation consuming three arguments - `int`, `int` and `double`.
 *
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
trait IntIntDoubleConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   */
  def accept(intValue1: Int, intValue2: Int, doubleValue: Double): Unit

}
