/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `Function`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the object parameter
 * @tparam R the type of the result
 */
@FunctionalInterface
trait CheckedFunction[T, R] {

  /**
   * Applies this function to the given argument.
   *
   * @param t  the function argument
   * @return the function result
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def apply(t: T): R

}
