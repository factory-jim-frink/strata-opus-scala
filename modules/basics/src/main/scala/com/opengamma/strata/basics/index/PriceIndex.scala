/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.DayCounts
import com.opengamma.strata.basics.location.Country
import com.opengamma.strata.basics.schedule.Frequency
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * An index of prices.
 *
 * A price index is a normalized average of the prices of goods and/or services.
 * Well-known price indices are published by Governments, such as the Consumer Price Index.
 * The annualized percentage change in the index is a measure of inflation.
 *
 * This interface represents a price index for a specific region.
 * The index is typically published monthly in arrears, however some regions
 * choose quarterly publication.
 *
 * The most common implementations are provided in `PriceIndices`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait PriceIndex
    extends FloatingRateIndex with Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the region that the index is defined for.
   *
   * @return the region of the index
   */
  def getRegion(): Country

  /**
   * Gets the day count convention of the index, which is '1/1'.
   *
   * @return the day count convention
   */
  override def getDayCount(): DayCount = {
    DayCounts.ONE_ONE
  }

  /**
   * Gets the frequency that the index is published.
   *
   * Most price indices are published monthly, but some are published quarterly.
   *
   * @return the frequency of publication of the index
   */
  def getPublicationFrequency(): Frequency

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

object PriceIndex {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): PriceIndex = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[PriceIndex] = {
    PriceIndices.enumLookup
  }

}
