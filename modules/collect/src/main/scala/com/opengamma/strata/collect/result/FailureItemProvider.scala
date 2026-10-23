/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

/**
 * Provides access to a `FailureItem`.
 *
 * This is intended to be implemented by exception classes
 */
trait FailureItemProvider {

  /**
   * Gets the failure item.
   *
   * @return the failure item
   */
  def getFailureItem(): FailureItem

}
