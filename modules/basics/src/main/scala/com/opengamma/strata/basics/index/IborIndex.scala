/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.function.Function

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.TenorAdjustment
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * An inter-bank lending rate index, such as Libor or Euribor.
 *
 * An index represented by this class relates to bank borrowing for periods
 * from one day to one year. They are typically calculated and published as the
 * trimmed arithmetic mean of estimated rates contributed by banks or based on 
 * traded volumes.
 *
 * With the Ibor transition in progress, the term rates have been incorporated
 * in this object as they have the same features, even if based on different financial realities.
 *
 * The index is defined by three dates.
 * The fixing date is the date on which the index is to be observed.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 *
 * The most common implementations are provided in `IborIndices`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait IborIndex
    extends RateIndex with Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * In most cases, the fixing date is 0 or 2 days before the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the fixing date offset
   */
  def getFixingDateOffset(): DaysAdjustment

  /**
   * Gets the adjustment applied to the fixing date to obtain the effective date.
   *
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the effective date offset
   */
  def getEffectiveDateOffset(): DaysAdjustment

  /**
   * Gets the adjustment applied to the effective date to obtain the maturity date.
   *
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the maturity date offset
   */
  def getMaturityDateOffset(): TenorAdjustment

  //-------------------------------------------------------------------------
  /**
   * Converts the fixing date-time from the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The fixing date-time is the specific date and time of the observation.
   *
   * No error is thrown if the input date is not a valid fixing date.
   * 
   * @param fixingDate  the fixing date
   * @return  the fixing date-time
   */
  def calculateFixingDateTime(fixingDate: LocalDate): ZonedDateTime

  /**
   * Calculates the effective date from the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   *
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the effective date
   */
  def calculateEffectiveFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate

  /**
   * Calculates the maturity date from the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied deposit ends.
   *
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the maturity date
   */
  def calculateMaturityFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate

  /**
   * Calculates the fixing date from the effective date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   *
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the fixing date
   */
  def calculateFixingFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate

  /**
   * Calculates the maturity date from the effective date.
   *
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   *
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the maturity date
   */
  def calculateMaturityFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate

  //-------------------------------------------------------------------------
  /**
   * Resolves this index using the specified reference data, returning a function.
   *
   * This returns a `Function` that converts fixing dates to observations.
   * It binds the holiday calendar, looked up from the reference data, into the result.
   * As such, there is no need to pass the reference data in again.
   *
   * This method is intended for use when looping to create multiple instances
   * of `IborIndexObservation`. Implementations of the method are intended
   * to optimize, avoiding repeated calls to resolve the holiday calendar
   * 
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return a function that converts fixing date to observation
   */
  def resolve(refData: ReferenceData): Function[LocalDate, IborIndexObservation]

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   * 
   * @return the unique name
   */
  @ToString
  override def getName(): String

}

object IborIndex {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): IborIndex = {
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
  @static def extendedEnum(): ExtendedEnum[IborIndex] = {
    IborIndices.ENUM_LOOKUP
  }

}
