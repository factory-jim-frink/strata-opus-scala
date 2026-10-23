/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

import java.util.Objects
import java.util.function.Function

/**
 * A function of two arguments - one object and one `int`.
 *
 * This takes two arguments and returns an object result.
 *
 * @tparam T the type of the object parameter
 * @tparam R the type of the result
 * @see BiFunction
 */
@FunctionalInterface
trait ObjIntFunction[T, R] {

  /**
   * Applies the function.
   *
   * @param obj  the first argument
   * @param value  the second argument
   * @return the result of the function
   */
  def apply(obj: T, value: Int): R

  /**
   * Returns a new function that composes this function and the specified function.
   *
   * This returns a composed function that applies the input to this function
   * and then converts the result using the specified function.
   *
   * @tparam V the result type of second function
   * @param other  the second function
   * @return the combined function, "this AND_THEN that"
   * @throws java.lang.NullPointerException if the other function is null
   */
  def andThen[V](other: Function[? >: R, ? <: V]): ObjIntFunction[T, V] = {
    Objects.requireNonNull(other)
    (obj, value) => other.apply(apply(obj, value))
  }

}
