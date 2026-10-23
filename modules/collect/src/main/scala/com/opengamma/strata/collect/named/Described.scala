/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

/**
 * A described instance.
 *
 * This simple interface is used to define objects that have a human-readable description.
 * This can be used alongside `Named` to provide a way to decode names that are codified.
 */
trait Described {

  /**
   * Gets the human-readable described of the instance.
   *
   * @return the description
   */
  def getDescription(): String

}
