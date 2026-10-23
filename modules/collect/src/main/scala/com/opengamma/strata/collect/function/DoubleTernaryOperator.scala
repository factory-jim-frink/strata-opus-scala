/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A function of three arguments that returns a value.
 *
 * All the inputs and outputs are of type `double`.
 */
@FunctionalInterface
trait DoubleTernaryOperator {

  /**
   * Applies the function.
   *
   * @param a  the first argument
   * @param b  the second argument
   * @param c  the third argument
   * @return the result of the function
   */
  def applyAsDouble(a: Double, b: Double, c: Double): Double

}
