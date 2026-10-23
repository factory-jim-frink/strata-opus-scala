/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * An operation consuming two arguments - `int` and `int`.
 *
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
trait IntIntConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   */
  def accept(intValue1: Int, intValue2: Int): Unit

}
