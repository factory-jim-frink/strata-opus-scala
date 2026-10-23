/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A function of three arguments - `int`, `int` and `double`.
 *
 * This takes three arguments and returns a `double` result.
 */
@FunctionalInterface
trait IntIntDoubleToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   * @return the result
   */
  def applyAsDouble(intValue1: Int, intValue2: Int, doubleValue: Double): Double

}
