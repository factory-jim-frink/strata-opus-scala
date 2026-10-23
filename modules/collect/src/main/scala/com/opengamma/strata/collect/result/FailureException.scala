/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import com.opengamma.strata.collect.ArgChecker

/**
 * An exception thrown when a failure `Result` is encountered and the failure can't be handled.
 *
 * @constructor Returns an exception wrapping a failure that couldn't be handled.
 * @param failure  a failure that couldn't be handled
 */
@SerialVersionUID(1L)
class FailureException(
    /** The details of the failure. */
    failure: Failure)
  extends RuntimeException(failure.getMessage()) {

  ArgChecker.notNull(failure, "failure")

  /**
   * Returns the details of the failure.
   *
   * @return the details of the failure
   */
  def getFailure(): Failure = {
    failure
  }

}
