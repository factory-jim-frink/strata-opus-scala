/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `BiFunction`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the first object parameter
 * @tparam U the type of the second object parameter
 * @tparam R the type of the result
 */
@FunctionalInterface
trait CheckedBiFunction[T, U, R] {

  /**
   * Applies this function to the given arguments.
   *
   * @param t  the first function argument
   * @param u  the second function argument
   * @return the function result
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def apply(t: T, u: U): R

}
