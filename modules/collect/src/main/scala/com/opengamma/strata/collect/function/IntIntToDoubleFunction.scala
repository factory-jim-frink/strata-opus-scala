/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A function of two arguments - `int` and `int`.
 *
 * This takes two arguments and returns a `double` result.
 */
@FunctionalInterface
trait IntIntToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @return the result
   */
  def applyAsDouble(intValue1: Int, intValue2: Int): Double

}
