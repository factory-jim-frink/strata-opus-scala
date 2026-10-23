/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

import java.util.Objects

/**
 * A consumer that takes three arguments.
 *
 * @tparam T the type of the first object parameter
 * @tparam U the type of the second object parameter
 * @tparam V the type of the third object parameter
 */
@FunctionalInterface
trait TriConsumer[T, U, V] {

  /**
   * Applies this consumer to the given arguments.
   *
   * @param t  the first consumer argument
   * @param u  the second consumer argument
   * @param v  the third consumer argument
   */
  def accept(t: T, u: U, v: V): Unit

  /**
   * Returns a new consumer that composes this consumer and the specified consumer.
   *
   * This returns a composed consumer that first calls this consumer and then calls
   * the specified consumer.
   *
   * @param after  the consumer to combine with
   * @return the combined consumer, "this AND_THEN that"
   */
  def andThen(after: TriConsumer[? >: T, ? >: U, ? >: V]): TriConsumer[T, U, V] = {
    Objects.requireNonNull(after)
    (t: T, u: U, v: V) => {
      accept(t, u, v)
      after.accept(t, u, v)
    }
  }

}
