/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `Predicate`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the object parameter
 */
@FunctionalInterface
trait CheckedPredicate[T] {

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param t  the input argument
   * @return true if the input argument matches the predicate
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def test(t: T): Boolean

}
