/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.time.LocalDate
import java.util.Comparator
import java.util.Optional
import java.util.function.Function
import java.util.function.Supplier

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.currency.CurrencyPair
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * An index of foreign exchange rates.
 *
 * An FX rate is the conversion rate between two currencies.
 * An FX index is the rate as published by a specific organization, typically
 * at a well-known time-of-day.
 *
 * The index is defined by two dates.
 * The fixing date is the date on which the index is to be observed.
 * The maturity date is the date on which delivery of the implied exchange occurs.
 *
 * The most common implementations are provided in `FxIndices`.
 *
 * All implementations of this interface must be immutable and thread-safe.
 */
trait FxIndex
    extends Index with Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the currency pair of the index.
   *
   * @return the currency pair of the index
   */
  def getCurrencyPair(): CurrencyPair

  /**
   * Gets the adjustment applied to the maturity date to obtain the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two business days after the fixing date.
   *
   * @return the fixing date offset
   */
  def getFixingDateOffset(): DaysAdjustment

  /**
   * Gets the adjustment applied to the fixing date to obtain the maturity date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two business days after the fixing date.
   *
   * @return the maturity date offset
   */
  def getMaturityDateOffset(): DaysAdjustment

  /**
   * Gets the calendar that determines which dates are fixing dates.
   *
   * The rate will be fixed on each business day in this calendar.
   *
   * @return the calendar used to determine the fixing dates of the index
   */
  def getFixingCalendar(): HolidayCalendarId

  //-------------------------------------------------------------------------
  /**
   * Calculates the maturity date from the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   *
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   *
   * The maturity date is also known as the <i>value date</i>.
   *
   * @param fixingDate  the fixing date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the maturity date
   */
  def calculateMaturityFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate

  /**
   * Calculates the fixing date from the maturity date.
   *
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied amount is delivered/exchanged.
   * The maturity date is typically two days after the fixing date.
   *
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   *
   * The maturity date is also known as the <i>value date</i>.
   *
   * @param maturityDate  the maturity date
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return the fixing date
   */
  def calculateFixingFromMaturity(maturityDate: LocalDate, refData: ReferenceData): LocalDate

  //-------------------------------------------------------------------------
  /**
   * Resolves this index using the specified reference data, returning a function.
   *
   * This returns a `Function` that converts fixing dates to observations.
   * It binds the holiday calendar, looked up from the reference data, into the result.
   * As such, there is no need to pass the reference data in again.
   *
   * This method is intended for use when looping to create multiple instances
   * of `FxIndexObservation`. Implementations of the method are intended
   * to optimize, avoiding repeated calls to resolve the holiday calendar
   *
   * @param refData  the reference data, used to resolve the holiday calendar
   * @return a function that converts fixing date to observation
   */
  def resolve(refData: ReferenceData): Function[LocalDate, FxIndexObservation]

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

object FxIndex {

  /**
   * Obtains an instance from the specified unique name.
   *
   * If the unique name can be parsed as a currency pair, an FX index will be looked up on the pair.
   *
   * @param uniqueName  the unique name
   * @return the index
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): FxIndex = {
    ofName(uniqueName)
  }

  /**
   * Obtains an instance from the specified currency pair.
   *
   * If a currency pair does not have an implementation, an FX index will be created.
   *
   * @param currencyPair the currency pair
   * @return the index
   */
  @static def of(currencyPair: CurrencyPair): FxIndex = {
    ofPair(currencyPair)
  }

  /**
   * Creates a FX index for the provided currency pair.
   *
   * The FX index will be default to the combined holiday calendars for the currency pair.
   * The maturity day offset will default to 2 days.
   *
   * @param currencyPair the currency pair
   * @return the index
   */
  @static def createFxIndex(currencyPair: CurrencyPair): FxIndex = {
    val calendarId = HolidayCalendarId.defaultByCurrencyPair(currencyPair)
    ImmutableFxIndex.builder()
        .name(currencyPair.toString())
        .currencyPair(currencyPair)
        .fixingCalendar(calendarId)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, calendarId))
        .build()
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[FxIndex] = {
    FxIndices.enumLookup
  }

  // lambdas and exception handlers are kept outside the static interface methods
  // to avoid illegal interface helper methods
  private def ofName(uniqueName: String): FxIndex = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    val fxIndexOpt: Optional[FxIndex] = extendedEnum().find(uniqueName)
    if (fxIndexOpt.isPresent()) {
      return fxIndexOpt.get()
    }
    try {
      val currencyPair = CurrencyPair.parse(uniqueName)
      FxIndex.of(currencyPair)
    } catch {
      case ex: IllegalArgumentException =>
        throw new IllegalArgumentException("Unable to create FX index from " + uniqueName)
    }
  }

  private def ofPair(currencyPair: CurrencyPair): FxIndex = {
    ArgChecker.notNull(currencyPair, "currencyPair")
    val creator: Supplier[FxIndex] = () => createFxIndex(currencyPair)
    extendedEnum().lookupAll().values().stream()
        .filter(index => index.getCurrencyPair().equals(currencyPair))
        .min(Comparator.comparing[FxIndex, String]((index: FxIndex) => index.getName()))
        .orElseGet(creator)
  }

}
