/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `BiPredicate`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the first object parameter
 * @tparam U the type of the second object parameter
 */
@FunctionalInterface
trait CheckedBiPredicate[T, U] {

  /**
   * Evaluates this predicate on the given arguments.
   *
   * @param t  the first input argument
   * @param u  the second input argument
   * @return true if the input arguments match the predicate
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def test(t: T, u: U): Boolean

}
