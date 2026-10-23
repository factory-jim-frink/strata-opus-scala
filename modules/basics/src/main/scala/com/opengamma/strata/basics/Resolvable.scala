/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

/**
 * An object that can be resolved against reference data.
 *
 * Interface marking those objects that can be resolved using `ReferenceData`.
 * Implementations of this interface will use identifiers
 * to refer to key concepts, such as holiday calendars and securities.
 *
 * When the `resolve(ReferenceData)` method is called, the identifiers are resolved.
 * The resolving process will take each identifier, look it up using the `ReferenceData`,
 * and return a new "resolved" instance.
 * Typically the result is of a type that is optimized for pricing.
 *
 * Resolved objects may be bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 *
 * Implementations must be immutable and thread-safe beans.
 *
 * @tparam T  the type of the resolved result
 */
trait Resolvable[T] {

  /**
   * Resolves this object using the specified reference data.
   *
   * This converts the object implementing this interface to the equivalent resolved form.
   * All `ReferenceDataId` identifiers in this instance will be resolved.
   * The resolved form will typically be a type that is optimized for pricing.
   *
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   *
   * @param refData  the reference data to use when resolving
   * @return the resolved instance
   * @throws com.opengamma.strata.basics.ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws java.lang.RuntimeException if unable to resolve due to an invalid definition
   */
  def resolve(refData: ReferenceData): T

}
