/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

import java.util.Objects

/**
 * A predicate of two arguments - one object and one `long`.
 *
 * This takes two arguments and returns a `boolean` result.
 *
 * @tparam T the type of the object parameter
 * @see BiPredicate
 */
@FunctionalInterface
trait ObjLongPredicate[T] {

  /**
   * Evaluates the predicate.
   *
   * @param obj  the first argument
   * @param value  the second argument
   * @return true if the arguments match the predicate
   */
  def test(obj: T, value: Long): Boolean

  /**
   * Returns a new predicate that returns true if both predicates return true.
   *
   * The second predicate is only invoked if the first returns true.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this AND that"
   * @throws java.lang.NullPointerException if the other predicate is null
   */
  def and(other: ObjLongPredicate[? >: T]): ObjLongPredicate[T] = {
    Objects.requireNonNull(other)
    (obj, value) => test(obj, value) && other.test(obj, value)
  }

  /**
   * Returns a new predicate that returns true if either predicates returns true.
   *
   * The second predicate is only invoked if the first returns false.
   *
   * @param other  the second predicate
   * @return the combined predicate, "this OR that"
   * @throws java.lang.NullPointerException if the other predicate is null
   */
  def or(other: ObjLongPredicate[? >: T]): ObjLongPredicate[T] = {
    Objects.requireNonNull(other)
    (obj, value) => test(obj, value) || other.test(obj, value)
  }

  /**
   * Returns a new predicate that negates the result of this predicate.
   *
   * @return the predicate, "NOT this"
   */
  def negate(): ObjLongPredicate[T] = {
    (obj, value) => !test(obj, value)
  }

}
