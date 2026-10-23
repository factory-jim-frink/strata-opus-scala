/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

import java.util.Objects
import java.util.function.Function

/**
 * A function that takes three arguments.
 *
 * @tparam T the type of the first object parameter
 * @tparam U the type of the second object parameter
 * @tparam V the type of the third object parameter
 * @tparam R the type of the result
 */
@FunctionalInterface
trait TriFunction[T, U, V, R] {

  /**
   * Applies this function to the given arguments.
   *
   * @param t  the first function argument
   * @param u  the second function argument
   * @param v  the third function argument
   * @return the function result
   */
  def apply(t: T, u: U, v: V): R

  /**
   * Returns a new function that composes this function and the specified function.
   *
   * This returns a composed function that applies the input to this function
   * and then converts the result using the specified function.
   *
   * @tparam S the type of the resulting function
   * @param after  the function to combine with
   * @return the combined function, "this AND_THEN that"
   */
  def andThen[S](after: Function[? >: R, ? <: S]): TriFunction[T, U, V, S] = {
    Objects.requireNonNull(after)
    (t: T, u: U, v: V) => after.apply(apply(t, u, v))
  }

}
