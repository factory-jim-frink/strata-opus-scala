/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * An operation consuming two arguments - `int` and `double`.
 *
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
trait IntDoubleConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   */
  def accept(intValue: Int, doubleValue: Double): Unit

}
