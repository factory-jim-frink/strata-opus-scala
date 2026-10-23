/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.Named

/**
 * An index of values, such as LIBOR, FED FUND or daily exchange rates.
 *
 * An index is an agreed mechanism for determining certain financial indicators,
 * such as exchange rate or interest rates. Most common indices are daily.
 *
 * This is a marker interface, see `FxIndex`, `IborIndex`
 * and `OvernightIndex` for more information.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait Index
    extends Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the unique name
   */
  @ToString
  def getName(): String

}

object Index {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): Index = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    Indices.INDEX_LOOKUP.lookup(uniqueName)
  }

}
