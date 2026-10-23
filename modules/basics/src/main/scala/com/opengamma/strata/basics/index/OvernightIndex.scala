/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.time.LocalDate

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * An Overnight index, such as Sonia or Eonia.
 *
 * An index represented by this class relates to lending over one night.
 * The rate typically refers to "Today/Tomorrow" but might refer to "Tomorrow/Next".
 *
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 *
 * The most common implementations are provided in `OvernightIndices`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait OvernightIndex
    extends RateIndex with Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the publication date.
   *
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   * 
   * @return the publication date offset
   */
  def getPublicationDateOffset(): Int

  /**
   * Gets the number of days to add to the fixing date to obtain the effective date.
   *
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   * 
   * @return the effective date offset
   */
  def getEffectiveDateOffset(): Int

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   *
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the publication date
   */
  def calculatePublicationFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate

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
   * Gets the name that uniquely identifies this index.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   * 
   * @return the unique name
   */
  @ToString
  override def getName(): String

}

object OvernightIndex {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): OvernightIndex = {
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
  @static def extendedEnum(): ExtendedEnum[OvernightIndex] = {
    OvernightIndices.ENUM_LOOKUP
  }

}
