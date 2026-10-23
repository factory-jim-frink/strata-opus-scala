/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `Supplier`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam R the type of the result
 */
@FunctionalInterface
trait CheckedSupplier[R] {

  /**
   * Gets a result.
   *
   * @return a result
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def get(): R

}
