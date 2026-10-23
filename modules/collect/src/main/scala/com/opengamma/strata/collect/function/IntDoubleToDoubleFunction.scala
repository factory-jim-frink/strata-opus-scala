/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A function of two arguments - `int` and `double`.
 *
 * This takes two arguments and returns a `double` result.
 */
@FunctionalInterface
trait IntDoubleToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   * @return the result
   */
  def applyAsDouble(intValue: Int, doubleValue: Double): Double

}
