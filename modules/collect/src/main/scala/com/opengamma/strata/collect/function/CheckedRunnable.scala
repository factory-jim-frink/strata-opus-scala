/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `Runnable`.
 *
 * This is intended to be used with `Unchecked`.
 */
@FunctionalInterface
trait CheckedRunnable {

  /**
   * Performs an action.
   *
   * @throws java.lang.Throwable if an error occurs
   */
  @throws[Throwable]
  def run(): Unit

}
