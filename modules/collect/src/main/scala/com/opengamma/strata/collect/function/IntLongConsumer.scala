/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * An operation consuming two arguments - `int` and `long`.
 *
 * Implementations of this interface will operate using side-effects.
 */
@FunctionalInterface
trait IntLongConsumer {

  /**
   * Consumes the values, performing an action.
   *
   * @param intValue  the first argument
   * @param longValue  the second argument
   */
  def accept(intValue: Int, longValue: Long): Unit

}
