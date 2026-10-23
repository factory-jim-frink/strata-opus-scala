/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

/**
 * An identifier for a unique item of reference data.
 *
 * Reference data is obtained from an instance of `ReferenceData` using this identifier.
 *
 * @tparam T  the type of the reference data this identifier refers to
 */
trait ReferenceDataId[T] {

  /**
   * Gets the type of data this identifier refers to.
   *
   * @return the type of the reference data this identifier refers to
   */
  def getReferenceDataType(): Class[T]

  /**
   * Low-level method to query the reference data value associated with this identifier,
   * returning null if not found.
   *
   * This is a low-level method that obtains the reference data value, returning null instead of an error.
   * Applications should use `ReferenceData#getValue(ReferenceDataId)` in preference to this method.
   *
   * @param refData  the reference data to lookup the value in
   * @return the reference data value, null if not found
   */
  def queryValueOrNull(refData: ReferenceData): T = {
    refData.queryValueOrNull(this)
  }

}
