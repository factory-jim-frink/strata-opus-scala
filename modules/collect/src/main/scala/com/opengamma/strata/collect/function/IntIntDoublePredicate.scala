/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A predicate of three arguments - `int`, `int` and `double`.
 *
 * This takes three arguments and returns a `boolean` result.
 */
@FunctionalInterface
trait IntIntDoublePredicate {

  /**
   * Evaluates the predicate.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   * @return true if the arguments match the predicate
   */
  def test(intValue1: Int, intValue2: Int, doubleValue: Double): Boolean

}
