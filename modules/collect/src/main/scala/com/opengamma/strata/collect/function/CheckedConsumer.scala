/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `Consumer`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the object parameter
 */
@FunctionalInterface
trait CheckedConsumer[T] {

  /**
   * Performs this operation on the given argument.
   *
   * @param t  the input argument
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def accept(t: T): Unit

}
