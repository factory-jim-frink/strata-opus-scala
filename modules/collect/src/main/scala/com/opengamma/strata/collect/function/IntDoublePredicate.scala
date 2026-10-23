/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A predicate of two arguments - `int` and `double`.
 *
 * This takes two arguments and returns a `boolean` result.
 */
@FunctionalInterface
trait IntDoublePredicate {

  /**
   * Evaluates the predicate.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   * @return true if the arguments match the predicate
   */
  def test(intValue: Int, doubleValue: Double): Boolean

}
