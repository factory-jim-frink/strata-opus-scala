/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

/**
 * A named enum instance.
 *
 * This extends `Named` for implementations of `Enum`.
 * The name is provided by the `Enum#toString()` method of the enum, typically
 * using the `EnumNames` helper class.
 *
 * Implementations must provide a static method `of(String)` that allows the
 * instance to be created from the name, see `Named#of(Class, String)`.
 */
trait NamedEnum extends Named {

  /**
   * Gets the unique name of the instance.
   *
   * The name contains enough information to be able to recreate the instance.
   *
   * @return the unique name
   */
  override def getName(): String = {
    toString()
  }

}
