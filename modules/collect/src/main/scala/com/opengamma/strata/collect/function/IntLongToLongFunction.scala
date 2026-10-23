/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A function of two arguments - `int` and `long`.
 *
 * This takes two arguments and returns a `long` result.
 */
@FunctionalInterface
trait IntLongToLongFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue  the first argument
   * @param longValue  the second argument
   * @return the result
   */
  def applyAsLong(intValue: Int, longValue: Long): Long

}
