/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

import java.util.Objects

/**
 * A predicate that takes three arguments.
 *
 * @tparam T the type of the first object parameter
 * @tparam U the type of the second object parameter
 * @tparam V the type of the third object parameter
 */
@FunctionalInterface
trait TriPredicate[T, U, V] {

  /**
   * Applies this predicate to the given arguments.
   *
   * @param t  the first predicate argument
   * @param u  the second predicate argument
   * @param v  the third predicate argument
   * @return whether the predicate matches
   */
  def test(t: T, u: U, v: V): Boolean

  /**
   * Returns a new predicate that returns true if both predicates return true.
   *
   * The second predicate is only invoked if the first returns true.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this AND that"
   */
  def and(other: TriPredicate[? >: T, ? >: U, ? >: V]): TriPredicate[T, U, V] = {
    Objects.requireNonNull(other)
    (t, u, v) => test(t, u, v) && other.test(t, u, v)
  }

  /**
   * Returns a new predicate that returns true if either predicates returns true.
   *
   * The second predicate is only invoked if the first returns false.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this OR that"
   */
  def or(other: TriPredicate[? >: T, ? >: U, ? >: V]): TriPredicate[T, U, V] = {
    Objects.requireNonNull(other)
    (t, u, v) => test(t, u, v) || other.test(t, u, v)
  }

  /**
   * Returns a new predicate that negates the result of this predicate.
   *
   * @return the predicate, "NOT this"
   */
  def negate(): TriPredicate[T, U, V] = {
    (t, u, v) => !test(t, u, v)
  }

}
