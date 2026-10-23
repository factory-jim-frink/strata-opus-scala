// CSOFF: ALL (class over 2000 lines)
/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.io.Serializable
import java.time.LocalDate
import java.util.AbstractList
import java.util.ArrayList
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional
import java.util.function.Supplier

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutableValidator
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.date.AdjustableDate
import com.opengamma.strata.basics.date.BusinessDayAdjustment
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate

/**
 * Definition of a periodic schedule.
 *
 * A periodic schedule is determined using a "periodic frequency".
 * This splits the schedule into "regular" periods of a fixed length, such as every 3 months.
 * Any remaining days are allocated to irregular "stubs" at the start and/or end.
 *
 * For example, a 24 month (2 year) swap might be divided into 3 month periods.
 * The 24 month period is the overall schedule and the 3 month period is the periodic frequency.
 *
 * Note that a 23 month swap cannot be split into even 3 month periods.
 * Instead, there will be a 2 month "initial" stub at the start, a 2 month "final" stub at the end
 * or both an initial and final stub with a combined length of 2 months.
 * 
 * <h4>Example</h4>
 *
 * This example creates a schedule for a 13 month swap cannot be split into 3 month periods
 * with a long initial stub rolling at end-of-month:
 * {{{
 *  // example swap using builder
 *  BusinessDayAdjustment businessDayAdj =
 *    BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GlobalHolidayCalendars.EUTA);
 *  PeriodicSchedule definition = PeriodicSchedule.builder()
 *      .startDate(LocalDate.of(2014, 2, 12))
 *      .endDate(LocalDate.of(2015, 3, 31))
 *      .businessDayAdjustment(businessDayAdj)
 *      .frequency(Frequency.P3M)
 *      .stubConvention(StubConvention.LONG_INITIAL)
 *      .rollConvention(RollConventions.EOM)
 *      .build();
 *  Schedule schedule = definition.createSchedule();
 * 
 *  // result
 *  period 1: 2014-02-12 to 2014-06-30
 *  period 2: 2014-06-30 to 2014-09-30
 *  period 3: 2014-09-30 to 2014-12-31
 *  period 4: 2014-12-31 to 2015-03-31
 * }}}
 * 
 * <h4>Details about stubs and date rolling</h4>
 *
 * The stubs are specified using a combination of the `StubConvention`, `RollConvention` and dates.
 *
 * The explicit stub dates are checked first. An explicit stub occurs if 'firstRegularStartDate' or
 * 'lastRegularEndDate' is present and they differ from 'startDate' and 'endDate'.
 *
 * If explicit stub dates are specified then they are used to lock the initial or final stub.
 * If the stub convention is present, it is matched and validated against the locked stub.
 * For example, if an initial stub is specified by dates and the stub convention is 'ShortInitial',
 * 'LongInitial' or 'SmartInitial' then the convention is considered to be matched, thus the periodic
 * frequency is applied using the implicit stub convention 'None'.
 * If the stub convention does not match the dates, then an exception will be thrown during schedule creation.
 * If the stub convention is not present, then the periodic frequency is applied
 * using the implicit stub convention 'None'.
 *
 * If explicit stub dates are not specified then the stub convention is used.
 * The convention selects whether to use the start date or the end date as the beginning of the schedule calculation.
 * The beginning of the calculation must match the roll convention, unless the convention is 'EOM',
 * in which case 'EOM' is only applied if the calculation starts at the end of the month.
 *
 * In all cases, the roll convention is used to fine-tune the dates.
 * If not present or 'None', the convention is effectively implied from the first date of the calculation.
 * All calculated dates will match the roll convention.
 * If this is not possible due to the dates specified then an exception will be thrown during schedule creation.
 *
 * It is permitted to have 'firstRegularStartDate' equal to 'endDate', or 'lastRegularEndDate' equal to 'startDate'.
 * In both cases, the effect is to define a schedule that is entirely "stub" and has no regular periods.
 * The resulting schedule will retain the frequency specified here, even though it is not used.
 *
 * The schedule operates primarily on "unadjusted" dates.
 * An unadjusted date can be any day, including non-business days.
 * When the unadjusted schedule has been determined, the appropriate business day adjustment
 * is applied to create a parallel schedule of "adjusted" dates.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class PeriodicSchedule private (
    /**
     * The start date, which is the start of the first schedule period.
     *
     * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted effective date.
     *
     * In most cases, the start date of a financial instrument is just after the trade date,
     * such as two business days later. However, the start date of a schedule is permitted
     * to be any date, which includes dates before or after the trade date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val startDate: LocalDate,
    /**
     * The end date, which is the end of the last schedule period.
     *
     * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted maturity date or unadjusted termination date.
     * This date must be after the start date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val endDate: LocalDate,
    /**
     * The regular periodic frequency to use.
     *
     * Most dates are calculated using a regular periodic frequency, such as every 3 months.
     * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val frequency: Frequency,
    /**
     * The business day adjustment to apply.
     *
     * Each date in the calculated schedule is determined without taking into account weekends and holidays.
     * The adjustment specified here is used to convert those dates to valid business days.
     *
     * The start date and end date may have their own business day adjustment rules.
     * If those are not present, then this adjustment is used instead.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val businessDayAdjustment: BusinessDayAdjustment,
    /**
     * The optional business day adjustment to apply to the start date.
     *
     * The start date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the start date to a valid business day.
     *
     * If this property is not present, the standard `businessDayAdjustment` property is used instead.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val startDateBusinessDayAdjustment: BusinessDayAdjustment,
    /**
     * The optional business day adjustment to apply to the end date.
     *
     * The end date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the end date to a valid business day.
     *
     * If this property is not present, the standard `businessDayAdjustment` property is used instead.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val endDateBusinessDayAdjustment: BusinessDayAdjustment,
    /**
     * The optional convention defining how to handle stubs.
     *
     * The stub convention is used during schedule construction to determine whether the irregular
     * remaining period occurs at the start or end of the schedule.
     * It also determines whether the irregular period is shorter or longer than the regular period.
     * This property interacts with the "explicit dates" of `PeriodicSchedule.getFirstRegularStartDate()`
     * and `PeriodicSchedule.getLastRegularEndDate()`.
     *
     * The convention 'None' may be used to explicitly indicate there are no stubs.
     * There must be no explicit dates.
     * This will be validated during schedule construction.
     *
     * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
     * The stubs themselves must be specified using explicit dates.
     * This will be validated during schedule construction.
     *
     * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
     * and 'SmartFinal' are used to indicate the type of stub to be generated.
     * The exact behavior varies depending on whether there are explicit dates or not:
     *
     * If explicit dates are specified, then the combination of stub convention an explicit date
     * will be validated during schedule construction. For example, the combination of an explicit dated
     * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
     * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
     *
     * If explicit dates are not specified, then it is not required that a stub is generated.
     * The convention determines whether to generate dates from the start date forward, or the
     * end date backwards. Date generation may or may not result in a stub, but if it does then
     * the stub will be of the correct type.
     *
     * When the stub convention is not present, the generation of stubs is based on the presence or absence
     * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
     * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val stubConvention: StubConvention,
    /**
     * The optional convention defining how to roll dates.
     *
     * The schedule periods are determined at the high level by repeatedly adding
     * the frequency to the start date, or subtracting it from the end date.
     * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the roll convention will be implied.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val rollConvention: RollConvention,
    /**
     * The optional start date of the first regular schedule period, which is the end date of the initial stub.
     *
     * This is used to identify the boundary date between the initial stub and the first regular schedule period.
     *
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be on or after 'startDate' and on or before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val firstRegularStartDate: LocalDate,
    /**
     * The optional end date of the last regular schedule period, which is the start date of the final stub.
     *
     * This is used to identify the boundary date between the last regular schedule period and the final stub.
     *
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val lastRegularEndDate: LocalDate,
    /**
     * The optional start date of the first schedule period, overriding normal schedule generation.
     *
     * This property is rarely used, and is generally needed when accrual starts before the effective date.
     * If specified, it overrides the start date of the first period once schedule generation has been completed.
     * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
     * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
     *
     * If a roll convention is explicitly specified and the regular start date does not match it,
     * then the override will be used when generating regular periods.
     *
     * If set, it should be different to the start date, although this is not validated.
     * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
     * and before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to override the start date
     * of the first generated schedule period.
     * If not present, then the start of the first period will be the normal start date.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val overrideStartDate: AdjustableDate)
  extends ImmutableBean with Serializable {

  JodaBeanUtils.notNull(startDate, "startDate")
  JodaBeanUtils.notNull(endDate, "endDate")
  JodaBeanUtils.notNull(frequency, "frequency")
  JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment")
  validate()

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    // startDate < endDate
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate")
    if (overrideStartDate != null) {
      ArgChecker.inOrderNotEqual(overrideStartDate.getUnadjusted(), endDate, "overrideStartDate", "endDate")
    }
    if (firstRegularStartDate != null) {
      // firstRegularStartDate <= endDate
      ArgChecker.inOrderOrEqual(firstRegularStartDate, endDate, "firstRegularStartDate", "endDate")
      // firstRegularStartDate <= lastRegularEndDate
      if (lastRegularEndDate != null) {
        ArgChecker.inOrderOrEqual(
            firstRegularStartDate, lastRegularEndDate, "firstRegularStartDate", "lastRegularEndDate")
      }
      // startDate (or overrideStartDate) <= firstRegularStartDate
      if (overrideStartDate != null) {
        ArgChecker.inOrderOrEqual(
            overrideStartDate.getUnadjusted(), firstRegularStartDate, "overrideStartDate", "firstRegularStartDate")
      } else {
        ArgChecker.inOrderOrEqual(startDate, firstRegularStartDate, "unadjusted", "firstRegularStartDate")
      }
    }
    if (lastRegularEndDate != null) {
      // startDate (or overrideStartDate) < lastRegularEndDate
      if (overrideStartDate != null) {
        ArgChecker.inOrderOrEqual(
            overrideStartDate.getUnadjusted(), lastRegularEndDate, "overrideStartDate", "lastRegularEndDate")
      } else {
        ArgChecker.inOrderOrEqual(startDate, lastRegularEndDate, "unadjusted", "lastRegularEndDate")
      }
      // lastRegularEndDate <= endDate
      ArgChecker.inOrderOrEqual(lastRegularEndDate, endDate, "lastRegularEndDate", "endDate")
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the schedule from the definition, see `createSchedule(ReferenceData, boolean)`.
   *
   * @return the schedule
   * @param refData  the reference data, used to find the holiday calendars
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the definition is invalid
   */
  def createSchedule(refData: ReferenceData): Schedule = {
    createSchedule(refData, false)
  }

  /**
   * Creates the schedule from the definition.
   *
   * The schedule consists of an optional initial stub, a number of regular periods and an optional final stub.
   *
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   *
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the `startDateBusinessDayAdjustment` property equals `BusinessDayAdjustment.NONE`
   *   or the roll convention is 'EOM'
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the specified start date
   *
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   *
   * @return the schedule
   * @param refData  the reference data, used to find the holiday calendars
   * @param combinePeriodsIfNecessary  determines whether periods should be combined if necessary
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the definition is invalid
   */
  def createSchedule(refData: ReferenceData, combinePeriodsIfNecessary: Boolean): Schedule = {
    val unadjStart = calculatedUnadjustedStartDate(refData)
    val unadjEnd = calculatedUnadjustedEndDate(refData)
    val regularStart = calculatedFirstRegularStartDate(unadjStart, refData)
    val regularEnd = calculatedLastRegularEndDate(unadjEnd, refData)
    val rollConv = calculatedRollConvention(regularStart, regularEnd)
    var unadj: List[LocalDate] = generateUnadjustedDates(unadjStart, regularStart, regularEnd, unadjEnd, rollConv)
    var adj: List[LocalDate] = applyBusinessDayAdjustment(unadj, refData)
    val periods: List[SchedulePeriod] = new ArrayList[SchedulePeriod]()
    try {
      // Remove any duplicate adjusted dates if requested
      if (combinePeriodsIfNecessary) {
        adj = new ArrayList[LocalDate](adj)
        unadj = new ArrayList[LocalDate](unadj)
        var i = 0
        while (i < adj.size() - 1) {
          while (i < adj.size() - 1 && adj.get(i).equals(adj.get(i + 1))) {
            adj.remove(i)
            unadj.remove(i)
          }
          i += 1
        }
      }
      // for performance, handle silly errors using exceptions
      var i = 0
      while (i < unadj.size() - 1) {
        periods.add(SchedulePeriod.of(adj.get(i), adj.get(i + 1), unadj.get(i), unadj.get(i + 1)))
        i += 1
      }
    } catch {
      case ex: IllegalArgumentException =>
        // check dates to throw a better exception for duplicate dates in schedule
        createUnadjustedDates()
        createAdjustedDates(refData)
        // unknown exception
        val se = new ScheduleException(this, "Schedule calculation resulted in invalid period")
        se.initCause(ex)
        throw se
    }
    Schedule.builder()
        .periods(periods)
        .frequency(frequency)
        .rollConvention(rollConv)
        .build()
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the list of unadjusted dates in the schedule.
   *
   * The unadjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   *
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * If the frequency is 'Term' explicit stub dates are disallowed, and the roll and stub convention are ignored.
   *
   * The special handling for last business day of month seen in
   * `createUnadjustedDates(ReferenceData)` is not applied.
   * 
   * @return the schedule of unadjusted dates
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the definition is invalid
   */
  def createUnadjustedDates(): ImmutableList[LocalDate] = {
    val regularStart = calculatedFirstRegularStartDate()
    val regularEnd = calculatedLastRegularEndDate()
    val rollConv = calculatedRollConvention(regularStart, regularEnd)
    val unadj = generateUnadjustedDates(startDate, regularStart, regularEnd, endDate, rollConv)
    // ensure schedule is valid with no duplicated dates
    val deduplicated = ImmutableSet.copyOf[LocalDate](unadj).asList()
    if (deduplicated.size() < unadj.size()) {
      throw new ScheduleException(this, "Schedule calculation resulted in duplicate unadjusted dates {}", unadj)
    }
    deduplicated
  }

  /**
   * Creates the list of unadjusted dates in the schedule.
   *
   * The unadjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   *
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * If the frequency is 'Term' explicit stub dates are disallowed, and the roll and stub convention are ignored.
   *
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the `startDateBusinessDayAdjustment` property equals `BusinessDayAdjustment.NONE`
   *   or the roll convention is 'EOM'
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the specified start date
   *
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   * 
   * @param refData  the reference data, used to find the holiday calendars
   * @return the schedule of unadjusted dates
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the definition is invalid
   */
  def createUnadjustedDates(refData: ReferenceData): ImmutableList[LocalDate] = {
    val unadj = unadjustedDates(refData)
    // ensure schedule is valid with no duplicated dates
    val deduplicated = ImmutableSet.copyOf[LocalDate](unadj).asList()
    if (deduplicated.size() < unadj.size()) {
      throw new ScheduleException(this, "Schedule calculation resulted in duplicate unadjusted dates {}", unadj)
    }
    deduplicated
  }

  // using the provided reference data create the unadjusted dates
  private def unadjustedDates(refData: ReferenceData): List[LocalDate] = {
    val unadjStart = calculatedUnadjustedStartDate(refData)
    val unadjEnd = calculatedUnadjustedEndDate(refData)
    val regularStart = calculatedFirstRegularStartDate(unadjStart, refData)
    val regularEnd = calculatedLastRegularEndDate(unadjEnd, refData)
    val rollConv = calculatedRollConvention(regularStart, regularEnd)
    generateUnadjustedDates(unadjStart, regularStart, regularEnd, unadjEnd, rollConv)
  }

  // creates the unadjusted dates, returning the mutable list
  private def generateUnadjustedDates(
      start: LocalDate,
      regStart: LocalDate,
      regEnd: LocalDate,
      end: LocalDate,
      rollConv: RollConvention): List[LocalDate] = {

    val overrideStart = if (overrideStartDate != null) overrideStartDate.getUnadjusted() else start
    val explicitInitStub = !start.equals(regStart)
    val explicitFinalStub = !end.equals(regEnd)
    // handle case where whole period is stub
    if (regStart.equals(end) || regEnd.equals(start)) {
      return ImmutableList.of(overrideStart, end)
    }
    // handle TERM frequency
    if (frequency eq Frequency.TERM) {
      if (explicitInitStub || explicitFinalStub) {
        throw new ScheduleException(this, "Explicit stubs must not be specified when using 'Term' frequency")
      }
      return ImmutableList.of(overrideStart, end)
    }
    // calculate base schedule excluding explicit stubs
    val stubConv = generateImplicitStubConvention(explicitInitStub, explicitFinalStub, regStart, regEnd)
    // special fallback if there is an override start date with a specified roll convention
    if (overrideStartDate != null &&
        rollConvention != null &&
        firstRegularStartDate == null &&
        !rollConv.matches(regStart) &&
        rollConv.matches(overrideStart)) {
      generateUnadjustedDates(
          overrideStart, regEnd, rollConv, stubConv, explicitInitStub, overrideStart, explicitFinalStub, end)
    } else {
      generateUnadjustedDates(
          regStart, regEnd, rollConv, stubConv, explicitInitStub, overrideStart, explicitFinalStub, end)
    }
  }

  // using knowledge of the explicit stubs, generate the correct convention for implicit stubs
  private def generateImplicitStubConvention(
      explicitInitialStub: Boolean,
      explicitFinalStub: Boolean,
      regStart: LocalDate,
      regEnd: LocalDate): StubConvention = {

    // null is not same as NONE; NONE validates that there are no explicit stubs whereas
    // null ensures that remainder after explicit stubs are removed has no stubs
    if (stubConvention != null) {
      return stubConvention.toImplicit(this, explicitInitialStub, explicitFinalStub)
    }
    // if stub convention is missing, but roll convention is present and matches start/end date, then set roll
    if (rollConvention != null && !explicitInitialStub && !explicitFinalStub) {
      if (rollConvention.getDayOfMonth() == regEnd.getDayOfMonth()) {
        return StubConvention.SMART_INITIAL
      }
      if (rollConvention.getDayOfMonth() == regStart.getDayOfMonth()) {
        return StubConvention.SMART_FINAL
      }
    }
    StubConvention.NONE
  }

  // generate dates, forwards or backwards
  private def generateUnadjustedDates(
      regStart: LocalDate,
      regEnd: LocalDate,
      rollCnv: RollConvention,
      stubCnv: StubConvention,
      explicitInitStub: Boolean,
      overrideStart: LocalDate,
      explicitFinalStub: Boolean,
      end: LocalDate): List[LocalDate] = {

    if (stubCnv.isCalculateBackwards()) {
      PeriodicSchedule.generateBackwards(
          this, regStart, regEnd, frequency, rollCnv, stubCnv, overrideStart, explicitFinalStub, end)
    } else {
      PeriodicSchedule.generateForwards(
          this, regStart, regEnd, frequency, rollCnv, stubCnv, explicitInitStub, overrideStart, explicitFinalStub, end)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the list of adjusted dates in the schedule.
   *
   * The adjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   * Each date will be a valid business day as per the appropriate business day adjustment.
   *
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   *
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the `startDateBusinessDayAdjustment` property equals `BusinessDayAdjustment.NONE`
   *   or the roll convention is 'EOM'
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the specified start date
   *
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * - the roll convention is numeric or 'EOM'
   * - applying `businessDayAdjustment` to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   * 
   * @return the schedule of dates adjusted to valid business days
   * @param refData  the reference data, used to find the holiday calendar
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the definition is invalid
   */
  def createAdjustedDates(refData: ReferenceData): ImmutableList[LocalDate] = {
    val unadj = unadjustedDates(refData)
    val adj = applyBusinessDayAdjustment(unadj, refData)
    // ensure schedule is valid with no duplicated dates
    val deduplicated = ImmutableSet.copyOf[LocalDate](adj).asList()
    if (deduplicated.size() < adj.size()) {
      throw new ScheduleException(
          this,
          "Schedule calculation resulted in duplicate adjusted dates {} from unadjusted dates {} using adjustment '{}'",
          adj,
          unadj,
          businessDayAdjustment)
    }
    deduplicated
  }

  // applies the appropriate business day adjustment to each date
  private def applyBusinessDayAdjustment(unadj: List[LocalDate], refData: ReferenceData): List[LocalDate] = {
    val adj: List[LocalDate] = new ArrayList[LocalDate](unadj.size())
    adj.add(calculatedStartDate().adjusted(refData))
    var i = 1
    while (i < unadj.size() - 1) {
      adj.add(businessDayAdjustment.adjust(unadj.get(i), refData))
      i += 1
    }
    adj.add(calculatedEndDate().adjusted(refData))
    adj
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the applicable roll convention defining how to roll dates.
   *
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   *
   * The applicable roll convention is a non-null value.
   * If the roll convention property is not present, this is determined from the
   * stub convention, dates and frequency, defaulting to 'None' if necessary.
   * 
   * @return the non-null roll convention
   */
  def calculatedRollConvention(): RollConvention = {
    calculatedRollConvention(calculatedFirstRegularStartDate(), calculatedLastRegularEndDate())
  }

  // calculates the applicable roll convention
  // the calculated start date parameter allows for influence by calculatedUnadjustedStartDate()
  private def calculatedRollConvention(
      calculatedFirstRegStartDate: LocalDate,
      calculatedLastRegEndDate: LocalDate): RollConvention = {

    // determine roll convention from stub convention
    val stubConv = MoreObjects.firstNonNull(stubConvention, StubConvention.NONE)
    // special handling for EOM as it is advisory rather than mandatory
    if (rollConvention eq RollConventions.EOM) {
      val derived =
        stubConv.toRollConvention(calculatedFirstRegStartDate, calculatedLastRegEndDate, frequency, true)
      return if (derived eq RollConventions.NONE) RollConventions.EOM else derived
    }
    // avoid RollConventions.NONE if possible
    if (rollConvention == null || (rollConvention eq RollConventions.NONE)) {
      return stubConv.toRollConvention(calculatedFirstRegStartDate, calculatedLastRegEndDate, frequency, false)
    }
    // use RollConventions.NONE if nothing else applies
    MoreObjects.firstNonNull(rollConvention, RollConventions.NONE)
  }

  //-------------------------------------------------------------------------
  // calculates the applicable start date
  // applies de facto rule where EOM means last business day for startDate
  // and similar rule for numeric roll conventions
  // http://www.fpml.org/forums/topic/can-a-roll-convention-imply-a-stub/#post-7659
  // For 'StandardRollConventions', such as IMM, adjusted date is identified by finding the closest valid roll date
  // and applying the trade level business day adjustment
  private def calculatedUnadjustedStartDate(refData: ReferenceData): LocalDate = {
    // change date if reference data is available and explicit start adjustment must be NONE or roll convention
    // is EOM and either numeric roll convention and day-of-month actually differs or StandardDayConvention is used
    // and the day is not a valid roll date
    if (refData != null &&
        rollConvention != null &&
        (BusinessDayAdjustment.NONE.equals(startDateBusinessDayAdjustment) || (rollConvention eq RollConventions.EOM))) {
      return PeriodicSchedule.calculatedUnadjustedDateFromAdjusted(
          startDate, rollConvention, businessDayAdjustment, refData)
    }
    startDate
  }

  // calculates the applicable end date
  private def calculatedUnadjustedEndDate(refData: ReferenceData): LocalDate = {
    if (refData != null && rollConvention != null) {
      return PeriodicSchedule.calculatedUnadjustedDateFromAdjusted(
          endDate, rollConvention, calculatedEndDateBusinessDayAdjustment(), refData)
    }
    endDate
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the applicable first regular start date.
   *
   * This will be either 'firstRegularStartDate' or 'startDate'.
   * 
   * @return the non-null start date of the first regular period
   */
  def calculatedFirstRegularStartDate(): LocalDate = {
    MoreObjects.firstNonNull(firstRegularStartDate, startDate)
  }

  // calculates the first regular start date, adjust when numeric roll convention present
  private def calculatedFirstRegularStartDate(unadjStart: LocalDate, refData: ReferenceData): LocalDate = {
    if (firstRegularStartDate == null) {
      return unadjStart
    }
    if (refData != null && rollConvention != null) {
      return PeriodicSchedule.calculatedUnadjustedDateFromAdjusted(
          firstRegularStartDate, rollConvention, businessDayAdjustment, refData)
    }
    firstRegularStartDate
  }

  /**
   * Calculates the applicable last regular end date.
   *
   * This will be either 'lastRegularEndDate' or 'endDate'.
   * 
   * @return the non-null end date of the last regular period
   */
  def calculatedLastRegularEndDate(): LocalDate = {
    MoreObjects.firstNonNull(lastRegularEndDate, endDate)
  }

  // calculates the last regular end date, adjust when numeric roll convention present
  private def calculatedLastRegularEndDate(unadjEnd: LocalDate, refData: ReferenceData): LocalDate = {
    if (lastRegularEndDate == null) {
      return unadjEnd
    }
    if (refData != null && rollConvention != null) {
      return PeriodicSchedule.calculatedUnadjustedDateFromAdjusted(
          lastRegularEndDate, rollConvention, businessDayAdjustment, refData)
    }
    lastRegularEndDate
  }

  /**
   * Calculates the applicable business day adjustment to apply to the start date.
   *
   * This will be either 'startDateBusinessDayAdjustment' or 'businessDayAdjustment'.
   * 
   * @return the non-null business day adjustment to apply to the start date
   */
  private def calculatedStartDateBusinessDayAdjustment(): BusinessDayAdjustment = {
    MoreObjects.firstNonNull(startDateBusinessDayAdjustment, businessDayAdjustment)
  }

  /**
   * Calculates the applicable business day adjustment to apply to the end date.
   *
   * This will be either 'endDateBusinessDayAdjustment' or 'businessDayAdjustment'.
   * 
   * @return the non-null business day adjustment to apply to the end date
   */
  private def calculatedEndDateBusinessDayAdjustment(): BusinessDayAdjustment = {
    MoreObjects.firstNonNull(endDateBusinessDayAdjustment, businessDayAdjustment)
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the applicable start date.
   *
   * The result combines the start date and the appropriate business day adjustment.
   * If the override start date is present, it will be returned.
   * 
   * @return the calculated start date
   */
  def calculatedStartDate(): AdjustableDate = {
    if (overrideStartDate != null) {
      return overrideStartDate
    }
    AdjustableDate.of(startDate, calculatedStartDateBusinessDayAdjustment())
  }

  /**
   * Calculates the applicable end date.
   *
   * The result combines the end date and the appropriate business day adjustment.
   * 
   * @return the calculated end date
   */
  def calculatedEndDate(): AdjustableDate = {
    AdjustableDate.of(endDate, calculatedEndDateBusinessDayAdjustment())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance based on this schedule with the start date replaced.
   *
   * This returns a new instance with the schedule altered to have the specified start date.
   * The specified date is considered to be adjusted, thus `startDateBusinessDayAdjustment` is set to 'None'.
   * The `firstRegularStartDate` and `overrideStartDate` fields are also removed.
   *
   * The stub convention is typically altered to be 'SmartInitial'.
   * The algorithm retains the 'ShortInitial' and 'LongInitial' conventions as is.
   * The algorithm examines "Final" conventions to try and set the `lastRegularEndDate`
   * via schedule generation allowing the stub convention to become 'SmartInitial'.
   * 
   * @param adjustedStartDate the proposed start date, which is considered to be adjusted
   * @return a schedule with the proposed start date
   * @throws java.lang.IllegalArgumentException if the schedule start date cannot be replaced with the proposed start date
   */
  def replaceStartDate(adjustedStartDate: LocalDate): PeriodicSchedule = {
    if (adjustedStartDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Cannot alter leg to have start date after end date")
    }
    val builder = toBuilder()
        .startDate(adjustedStartDate)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .firstRegularStartDate(null)
        .overrideStartDate(null)
    if (stubConvention == null || (stubConvention eq StubConvention.BOTH) || (stubConvention eq StubConvention.NONE)) {
      // set the stub convention to SmartInitial to better handle the new start date
      builder.stubConvention(StubConvention.SMART_INITIAL)
    } else if (stubConvention.isFinal()) {
      if (lastRegularEndDate != null) {
        // last regular is set, so the final stub convention can be safely changed
        builder.stubConvention(StubConvention.SMART_INITIAL)
      } else {
        // calculate the last regular date so that SmartInitial can be used
        // if schedule generation fails, make no changes
        val supplier: Supplier[List[LocalDate]] = () => createUnadjustedDates()
        val it = Guavate.inOptional(Guavate.tryCatchToOptional(supplier)).iterator()
        while (it.hasNext()) {
          val dates = it.next()
          if (dates.size() > 2) {
            val lastRegular = dates.get(dates.size() - 2)
            builder.lastRegularEndDate(lastRegular)
            builder.stubConvention(StubConvention.SMART_INITIAL)
          }
        }
      }
    }
    builder.build()
  }
  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): PeriodicSchedule.Meta = {
    PeriodicSchedule.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date, which is the start of the first schedule period.
   *
   * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted effective date.
   *
   * In most cases, the start date of a financial instrument is just after the trade date,
   * such as two business days later. However, the start date of a schedule is permitted
   * to be any date, which includes dates before or after the trade date.
   * @return the value of the property, not null
   */
  def getStartDate(): LocalDate = {
    startDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date, which is the end of the last schedule period.
   *
   * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted maturity date or unadjusted termination date.
   * This date must be after the start date.
   * @return the value of the property, not null
   */
  def getEndDate(): LocalDate = {
    endDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the regular periodic frequency to use.
   *
   * Most dates are calculated using a regular periodic frequency, such as every 3 months.
   * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
   * @return the value of the property, not null
   */
  def getFrequency(): Frequency = {
    frequency
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply.
   *
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   *
   * The start date and end date may have their own business day adjustment rules.
   * If those are not present, then this adjustment is used instead.
   * @return the value of the property, not null
   */
  def getBusinessDayAdjustment(): BusinessDayAdjustment = {
    businessDayAdjustment
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional business day adjustment to apply to the start date.
   *
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   *
   * If this property is not present, the standard `businessDayAdjustment` property is used instead.
   * @return the optional value of the property, not null
   */
  def getStartDateBusinessDayAdjustment(): Optional[BusinessDayAdjustment] = {
    Optional.ofNullable(startDateBusinessDayAdjustment)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional business day adjustment to apply to the end date.
   *
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   *
   * If this property is not present, the standard `businessDayAdjustment` property is used instead.
   * @return the optional value of the property, not null
   */
  def getEndDateBusinessDayAdjustment(): Optional[BusinessDayAdjustment] = {
    Optional.ofNullable(endDateBusinessDayAdjustment)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to handle stubs.
   *
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * This property interacts with the "explicit dates" of `PeriodicSchedule.getFirstRegularStartDate()`
   * and `PeriodicSchedule.getLastRegularEndDate()`.
   *
   * The convention 'None' may be used to explicitly indicate there are no stubs.
   * There must be no explicit dates.
   * This will be validated during schedule construction.
   *
   * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
   * The stubs themselves must be specified using explicit dates.
   * This will be validated during schedule construction.
   *
   * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
   * and 'SmartFinal' are used to indicate the type of stub to be generated.
   * The exact behavior varies depending on whether there are explicit dates or not:
   *
   * If explicit dates are specified, then the combination of stub convention an explicit date
   * will be validated during schedule construction. For example, the combination of an explicit dated
   * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
   * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
   *
   * If explicit dates are not specified, then it is not required that a stub is generated.
   * The convention determines whether to generate dates from the start date forward, or the
   * end date backwards. Date generation may or may not result in a stub, but if it does then
   * the stub will be of the correct type.
   *
   * When the stub convention is not present, the generation of stubs is based on the presence or absence
   * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
   * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
   * @return the optional value of the property, not null
   */
  def getStubConvention(): Optional[StubConvention] = {
    Optional.ofNullable(stubConvention)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to roll dates.
   *
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   *
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the roll convention will be implied.
   * @return the optional value of the property, not null
   */
  def getRollConvention(): Optional[RollConvention] = {
    Optional.ofNullable(rollConvention)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional start date of the first regular schedule period, which is the end date of the initial stub.
   *
   * This is used to identify the boundary date between the initial stub and the first regular schedule period.
   *
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be on or after 'startDate' and on or before 'endDate'.
   *
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
   * @return the optional value of the property, not null
   */
  def getFirstRegularStartDate(): Optional[LocalDate] = {
    Optional.ofNullable(firstRegularStartDate)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional end date of the last regular schedule period, which is the start date of the final stub.
   *
   * This is used to identify the boundary date between the last regular schedule period and the final stub.
   *
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
   *
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
   * @return the optional value of the property, not null
   */
  def getLastRegularEndDate(): Optional[LocalDate] = {
    Optional.ofNullable(lastRegularEndDate)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional start date of the first schedule period, overriding normal schedule generation.
   *
   * This property is rarely used, and is generally needed when accrual starts before the effective date.
   * If specified, it overrides the start date of the first period once schedule generation has been completed.
   * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
   * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
   *
   * If a roll convention is explicitly specified and the regular start date does not match it,
   * then the override will be used when generating regular periods.
   *
   * If set, it should be different to the start date, although this is not validated.
   * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
   * and before 'endDate'.
   *
   * During schedule generation, if this is present it will be used to override the start date
   * of the first generated schedule period.
   * If not present, then the start of the first period will be the normal start date.
   * @return the optional value of the property, not null
   */
  def getOverrideStartDate(): Optional[AdjustableDate] = {
    Optional.ofNullable(overrideStartDate)
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): PeriodicSchedule.Builder = {
    new PeriodicSchedule.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[PeriodicSchedule]
      JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(startDateBusinessDayAdjustment, other.startDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(endDateBusinessDayAdjustment, other.endDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention) &&
          JodaBeanUtils.equal(firstRegularStartDate, other.firstRegularStartDate) &&
          JodaBeanUtils.equal(lastRegularEndDate, other.lastRegularEndDate) &&
          JodaBeanUtils.equal(overrideStartDate, other.overrideStartDate)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency)
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment)
    hash = hash * 31 + JodaBeanUtils.hashCode(startDateBusinessDayAdjustment)
    hash = hash * 31 + JodaBeanUtils.hashCode(endDateBusinessDayAdjustment)
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention)
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention)
    hash = hash * 31 + JodaBeanUtils.hashCode(firstRegularStartDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(lastRegularEndDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(overrideStartDate)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(384)
    buf.append("PeriodicSchedule{")
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ')
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ')
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ')
    buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ')
    buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ')
    buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ')
    buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ')
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ')
    buf.append("firstRegularStartDate").append('=').append(JodaBeanUtils.toString(firstRegularStartDate)).append(',').append(' ')
    buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(lastRegularEndDate)).append(',').append(' ')
    buf.append("overrideStartDate").append('=').append(JodaBeanUtils.toString(overrideStartDate))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
object PeriodicSchedule {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(PeriodicSchedule.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a stub convention and end-of-month flag.
   *
   * The business day adjustment is used for all dates.
   * The stub convention is used to determine whether there are any stubs.
   * If the end-of-month flag is true, then in any case of ambiguity the
   * end-of-month will be chosen.
   * 
   * @param unadjustedStartDate  start date, which is the start of the first schedule period
   * @param unadjustedEndDate  the end date, which is the end of the last schedule period
   * @param frequency  the regular periodic frequency
   * @param businessDayAdjustment  the business day adjustment to apply
   * @param stubConvention  the non-null convention defining how to handle stubs
   * @param preferEndOfMonth  whether to prefer the end-of-month when rolling
   * @return the definition
   */
  def of(
      unadjustedStartDate: LocalDate,
      unadjustedEndDate: LocalDate,
      frequency: Frequency,
      businessDayAdjustment: BusinessDayAdjustment,
      stubConvention: StubConvention,
      preferEndOfMonth: Boolean): PeriodicSchedule = {

    ArgChecker.notNull(unadjustedStartDate, "unadjustedStartDate")
    ArgChecker.notNull(unadjustedEndDate, "unadjustedEndDate")
    ArgChecker.notNull(frequency, "frequency")
    ArgChecker.notNull(businessDayAdjustment, "businessDayAdjustment")
    ArgChecker.notNull(stubConvention, "stubConvention")
    PeriodicSchedule.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConvention)
        .rollConvention(if (preferEndOfMonth) RollConventions.EOM else null)
        .build()
  }

  /**
   * Obtains an instance based on roll and stub conventions.
   *
   * The business day adjustment is used for all dates.
   * The stub convention is used to determine whether there are any stubs.
   * The roll convention is used to fine tune each rolled date.
   * 
   * @param unadjustedStartDate  start date, which is the start of the first schedule period
   * @param unadjustedEndDate  the end date, which is the end of the last schedule period
   * @param frequency  the regular periodic frequency
   * @param businessDayAdjustment  the business day adjustment to apply
   * @param stubConvention  the non-null convention defining how to handle stubs
   * @param rollConvention  the non-null convention defining how to roll dates
   * @return the definition
   */
  def of(
      unadjustedStartDate: LocalDate,
      unadjustedEndDate: LocalDate,
      frequency: Frequency,
      businessDayAdjustment: BusinessDayAdjustment,
      stubConvention: StubConvention,
      rollConvention: RollConvention): PeriodicSchedule = {

    ArgChecker.notNull(unadjustedStartDate, "unadjustedStartDate")
    ArgChecker.notNull(unadjustedEndDate, "unadjustedEndDate")
    ArgChecker.notNull(frequency, "frequency")
    ArgChecker.notNull(businessDayAdjustment, "businessDayAdjustment")
    ArgChecker.notNull(stubConvention, "stubConvention")
    ArgChecker.notNull(rollConvention, "rollConvention")
    PeriodicSchedule.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConvention)
        .rollConvention(rollConvention)
        .build()
  }

  // generate the schedule of dates backwards from the end, only called when stub convention is initial
  private def generateBackwards(
      schedule: PeriodicSchedule,
      start: LocalDate,
      end: LocalDate,
      frequency: Frequency,
      rollConv: RollConvention,
      stubConv: StubConvention,
      explicitStartDate: LocalDate,
      explicitFinalStub: Boolean,
      explicitEndDate: LocalDate): List[LocalDate] = {

    // validate
    if (!rollConv.matches(end)) {
      throw new ScheduleException(
          schedule, "Date '{}' does not match roll convention '{}' when starting to roll backwards", end, rollConv)
    }
    // generate
    val dates = new BackwardsList(estimateNumberPeriods(start, end, frequency))
    if (explicitFinalStub) {
      dates.addFirst(explicitEndDate)
    }
    dates.addFirst(end)
    var temp = rollConv.previous(end, frequency)
    while (temp.isAfter(start)) {
      dates.addFirst(temp)
      temp = rollConv.previous(temp, frequency)
    }
    // convert to long stub, but only if we actually have a stub
    val stub = !temp.equals(start)
    if (stub && dates.size() > 1 && stubConv.isStubLong(start, dates.get(0))) {
      dates.removeFirst()
    }
    dates.addFirst(explicitStartDate)
    dates
  }

  // dedicated list implementation for backwards looping for performance only implements those methods that are needed
  private class BackwardsList(capacity: Int) extends AbstractList[LocalDate] {
    private val array: Array[LocalDate] = new Array[LocalDate](capacity)
    private var first: Int = array.length

    override def get(index: Int): LocalDate = array(first + index)

    override def size(): Int = array.length - first

    def addFirst(date: LocalDate): Unit = {
      first -= 1
      array(first) = date
    }

    def removeFirst(): Unit = {
      first += 1
    }
  }

  // generate the schedule of dates forwards from the start, called when stub convention is not initial
  // start/end dates are regular start/end
  private def generateForwards(
      schedule: PeriodicSchedule,
      start: LocalDate,
      end: LocalDate,
      frequency: Frequency,
      rollConv: RollConvention,
      stubConv: StubConvention,
      explicitInitialStub: Boolean,
      explicitStartDate: LocalDate,
      explicitFinalStub: Boolean,
      explicitEndDate: LocalDate): List[LocalDate] = {

    // validate
    if (!rollConv.matches(start)) {
      throw new ScheduleException(
          schedule, "Date '{}' does not match roll convention '{}' when starting to roll forwards", start, rollConv)
    }
    // generate
    val dates: List[LocalDate] = new ArrayList[LocalDate](estimateNumberPeriods(start, end, frequency))
    if (explicitInitialStub) {
      dates.add(explicitStartDate)
      dates.add(start)
    } else {
      dates.add(explicitStartDate)
    }
    if (!start.equals(end)) {
      var temp = rollConv.next(start, frequency)
      while (temp.isBefore(end)) {
        dates.add(temp)
        temp = rollConv.next(temp, frequency)
      }
      // convert short stub to long stub, but only if we actually have a stub
      val stub = !temp.equals(end)
      if (stub && dates.size() > 1) {
        var applicableStubConv = stubConv
        if (stubConv eq StubConvention.NONE) {
          // handle edge case where the end date does not follow the EOM rule
          if ((rollConv eq RollConventions.EOM) &&
              frequency.isMonthBased() &&
              !explicitFinalStub &&
              start.getDayOfMonth() == start.lengthOfMonth() &&
              end.getDayOfMonth() == start.getDayOfMonth()) {
            // accept the date and move on using smart rules
            applicableStubConv = StubConvention.SMART_FINAL
          } else {
            throw new ScheduleException(
                schedule, "Period '{}' to '{}' resulted in a disallowed stub with frequency '{}'", start, end,
                frequency)
          }
        }
        // convert a short stub to a long one if necessary
        if (applicableStubConv.isStubLong(dates.get(dates.size() - 1), end)) {
          dates.remove(dates.size() - 1)
        }
      }
      dates.add(end)
    }
    if (explicitFinalStub) {
      dates.add(explicitEndDate)
    }
    dates
  }

  // roughly estimate the number of periods (overestimating)
  private def estimateNumberPeriods(start: LocalDate, end: LocalDate, frequency: Frequency): Int = {
    val termInYearsEstimate = end.getYear() - start.getYear() + 2
    (Math.max(frequency.eventsPerYearEstimate(), 1) * termInYearsEstimate).toInt
  }

  // calculates an unadjusted date
  // for EOM and day of month roll conventions the unadjusted date is based on the roll day-of-month
  // for other conventions, the nearest unadjusted roll date is calculated, adjusted and compared to the base date
  // this is known not to work for day of week conventions if the passed date has been adjusted forwards
  private def calculatedUnadjustedDateFromAdjusted(
      baseDate: LocalDate,
      rollConvention: RollConvention,
      businessDayAdjustment: BusinessDayAdjustment,
      refData: ReferenceData): LocalDate = {

    val rollDom = rollConvention.getDayOfMonth()
    if (rollDom > 0 && baseDate.getDayOfMonth() != rollDom) {
      val lengthOfMonth = baseDate.lengthOfMonth()
      val actualDom = Math.min(rollDom, lengthOfMonth)
      // startDate is already the expected day, then nothing to do
      if (baseDate.getDayOfMonth() != actualDom) {
        val rollImpliedDate = baseDate.withDayOfMonth(actualDom)
        val adjDate = businessDayAdjustment.adjust(rollImpliedDate, refData)
        if (adjDate.equals(baseDate)) {
          return rollImpliedDate
        }
      }
    } else if (rollDom == 0) {
      //0 roll day implies that the roll date is calculated relative to the month or week
      //Find the valid (unadjusted) roll date for the given month or week
      val rollImpliedDate = rollConvention.adjust(baseDate)
      if (!rollImpliedDate.equals(baseDate)) {
        //If roll date is relative to the month the assumption is that the adjusted date is not in a different month to
        //the original unadjusted date. This is safe as the roll day produced by monthly roll conventions are typically
        //not close to the end of the month and hence any reasonable adjustment will not move into the next month.
        //adjust() method for "day of week" roll conventions will roll forward from the passed date; hence this logic
        //will not work for "day of week" conventions if the passed baseDate has been adjusted to be after the original
        //unadjusted date (i.e. has been rolled forward).
        //Calculate the expected adjusted roll date, based on the valid unadjusted roll date
        val adjDate = businessDayAdjustment.adjust(rollImpliedDate, refData)
        //If the adjusted roll date equals the original base date then that the base date is in fact an adjusted date
        //and hence return the unadjusted date for building the schedule.
        if (adjDate.equals(baseDate)) {
          return rollImpliedDate
        }
      }

    }
    baseDate
  }
  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `PeriodicSchedule`.
   * @return the meta-bean, not null
   */
  def meta(): PeriodicSchedule.Meta = {
    PeriodicSchedule.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): PeriodicSchedule.Builder = {
    new PeriodicSchedule.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `PeriodicSchedule`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `startDate` property.
     */
    private val startDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "startDate", classOf[PeriodicSchedule], classOf[LocalDate])
    /**
     * The meta-property for the `endDate` property.
     */
    private val endDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "endDate", classOf[PeriodicSchedule], classOf[LocalDate])
    /**
     * The meta-property for the `frequency` property.
     */
    private val frequencyProperty: MetaProperty[Frequency] = DirectMetaProperty.ofImmutable(
        this, "frequency", classOf[PeriodicSchedule], classOf[Frequency])
    /**
     * The meta-property for the `businessDayAdjustment` property.
     */
    private val businessDayAdjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", classOf[PeriodicSchedule], classOf[BusinessDayAdjustment])
    /**
     * The meta-property for the `startDateBusinessDayAdjustment` property.
     */
    private val startDateBusinessDayAdjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "startDateBusinessDayAdjustment", classOf[PeriodicSchedule], classOf[BusinessDayAdjustment])
    /**
     * The meta-property for the `endDateBusinessDayAdjustment` property.
     */
    private val endDateBusinessDayAdjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "endDateBusinessDayAdjustment", classOf[PeriodicSchedule], classOf[BusinessDayAdjustment])
    /**
     * The meta-property for the `stubConvention` property.
     */
    private val stubConventionProperty: MetaProperty[StubConvention] = DirectMetaProperty.ofImmutable(
        this, "stubConvention", classOf[PeriodicSchedule], classOf[StubConvention])
    /**
     * The meta-property for the `rollConvention` property.
     */
    private val rollConventionProperty: MetaProperty[RollConvention] = DirectMetaProperty.ofImmutable(
        this, "rollConvention", classOf[PeriodicSchedule], classOf[RollConvention])
    /**
     * The meta-property for the `firstRegularStartDate` property.
     */
    private val firstRegularStartDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "firstRegularStartDate", classOf[PeriodicSchedule], classOf[LocalDate])
    /**
     * The meta-property for the `lastRegularEndDate` property.
     */
    private val lastRegularEndDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "lastRegularEndDate", classOf[PeriodicSchedule], classOf[LocalDate])
    /**
     * The meta-property for the `overrideStartDate` property.
     */
    private val overrideStartDateProperty: MetaProperty[AdjustableDate] = DirectMetaProperty.ofImmutable(
        this, "overrideStartDate", classOf[PeriodicSchedule], classOf[AdjustableDate])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "frequency",
        "businessDayAdjustment",
        "startDateBusinessDayAdjustment",
        "endDateBusinessDayAdjustment",
        "stubConvention",
        "rollConvention",
        "firstRegularStartDate",
        "lastRegularEndDate",
        "overrideStartDate")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          startDateProperty
        case -1607727319 => // endDate
          endDateProperty
        case -70023844 => // frequency
          frequencyProperty
        case -1065319863 => // businessDayAdjustment
          businessDayAdjustmentProperty
        case 429197561 => // startDateBusinessDayAdjustment
          startDateBusinessDayAdjustmentProperty
        case -734327136 => // endDateBusinessDayAdjustment
          endDateBusinessDayAdjustmentProperty
        case -31408449 => // stubConvention
          stubConventionProperty
        case -10223666 => // rollConvention
          rollConventionProperty
        case 2011803076 => // firstRegularStartDate
          firstRegularStartDateProperty
        case -1540679645 => // lastRegularEndDate
          lastRegularEndDateProperty
        case -599936828 => // overrideStartDate
          overrideStartDateProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): PeriodicSchedule.Builder = {
      new PeriodicSchedule.Builder()
    }

    override def beanType(): Class[? <: PeriodicSchedule] = {
      classOf[PeriodicSchedule]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `startDate` property.
     * @return the meta-property, not null
     */
    def startDate(): MetaProperty[LocalDate] = {
      startDateProperty
    }

    /**
     * The meta-property for the `endDate` property.
     * @return the meta-property, not null
     */
    def endDate(): MetaProperty[LocalDate] = {
      endDateProperty
    }

    /**
     * The meta-property for the `frequency` property.
     * @return the meta-property, not null
     */
    def frequency(): MetaProperty[Frequency] = {
      frequencyProperty
    }

    /**
     * The meta-property for the `businessDayAdjustment` property.
     * @return the meta-property, not null
     */
    def businessDayAdjustment(): MetaProperty[BusinessDayAdjustment] = {
      businessDayAdjustmentProperty
    }

    /**
     * The meta-property for the `startDateBusinessDayAdjustment` property.
     * @return the meta-property, not null
     */
    def startDateBusinessDayAdjustment(): MetaProperty[BusinessDayAdjustment] = {
      startDateBusinessDayAdjustmentProperty
    }

    /**
     * The meta-property for the `endDateBusinessDayAdjustment` property.
     * @return the meta-property, not null
     */
    def endDateBusinessDayAdjustment(): MetaProperty[BusinessDayAdjustment] = {
      endDateBusinessDayAdjustmentProperty
    }

    /**
     * The meta-property for the `stubConvention` property.
     * @return the meta-property, not null
     */
    def stubConvention(): MetaProperty[StubConvention] = {
      stubConventionProperty
    }

    /**
     * The meta-property for the `rollConvention` property.
     * @return the meta-property, not null
     */
    def rollConvention(): MetaProperty[RollConvention] = {
      rollConventionProperty
    }

    /**
     * The meta-property for the `firstRegularStartDate` property.
     * @return the meta-property, not null
     */
    def firstRegularStartDate(): MetaProperty[LocalDate] = {
      firstRegularStartDateProperty
    }

    /**
     * The meta-property for the `lastRegularEndDate` property.
     * @return the meta-property, not null
     */
    def lastRegularEndDate(): MetaProperty[LocalDate] = {
      lastRegularEndDateProperty
    }

    /**
     * The meta-property for the `overrideStartDate` property.
     * @return the meta-property, not null
     */
    def overrideStartDate(): MetaProperty[AdjustableDate] = {
      overrideStartDateProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          bean.asInstanceOf[PeriodicSchedule].getStartDate()
        case -1607727319 => // endDate
          bean.asInstanceOf[PeriodicSchedule].getEndDate()
        case -70023844 => // frequency
          bean.asInstanceOf[PeriodicSchedule].getFrequency()
        case -1065319863 => // businessDayAdjustment
          bean.asInstanceOf[PeriodicSchedule].getBusinessDayAdjustment()
        case 429197561 => // startDateBusinessDayAdjustment
          bean.asInstanceOf[PeriodicSchedule].startDateBusinessDayAdjustment
        case -734327136 => // endDateBusinessDayAdjustment
          bean.asInstanceOf[PeriodicSchedule].endDateBusinessDayAdjustment
        case -31408449 => // stubConvention
          bean.asInstanceOf[PeriodicSchedule].stubConvention
        case -10223666 => // rollConvention
          bean.asInstanceOf[PeriodicSchedule].rollConvention
        case 2011803076 => // firstRegularStartDate
          bean.asInstanceOf[PeriodicSchedule].firstRegularStartDate
        case -1540679645 => // lastRegularEndDate
          bean.asInstanceOf[PeriodicSchedule].lastRegularEndDate
        case -599936828 => // overrideStartDate
          bean.asInstanceOf[PeriodicSchedule].overrideStartDate
        case _ =>
          super.propertyGet(bean, propertyName, quiet)
      }
    }

    override protected def propertySet(bean: Bean, propertyName: String, newValue: Object, quiet: Boolean): Unit = {
      metaProperty(propertyName)
      if (!quiet) {
        throw new UnsupportedOperationException("Property cannot be written: " + propertyName)
      }
    }

  }

  object Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    private[schedule] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `PeriodicSchedule`.
   */
  final class Builder private[schedule] () extends DirectFieldsBeanBuilder[PeriodicSchedule] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _startDate: LocalDate = uninitialized
    private var _endDate: LocalDate = uninitialized
    private var _frequency: Frequency = uninitialized
    private var _businessDayAdjustment: BusinessDayAdjustment = uninitialized
    private var _startDateBusinessDayAdjustment: BusinessDayAdjustment = uninitialized
    private var _endDateBusinessDayAdjustment: BusinessDayAdjustment = uninitialized
    private var _stubConvention: StubConvention = uninitialized
    private var _rollConvention: RollConvention = uninitialized
    private var _firstRegularStartDate: LocalDate = uninitialized
    private var _lastRegularEndDate: LocalDate = uninitialized
    private var _overrideStartDate: AdjustableDate = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[schedule] def this(beanToCopy: PeriodicSchedule) = {
      this()
      this._startDate = beanToCopy.getStartDate()
      this._endDate = beanToCopy.getEndDate()
      this._frequency = beanToCopy.getFrequency()
      this._businessDayAdjustment = beanToCopy.getBusinessDayAdjustment()
      this._startDateBusinessDayAdjustment = beanToCopy.startDateBusinessDayAdjustment
      this._endDateBusinessDayAdjustment = beanToCopy.endDateBusinessDayAdjustment
      this._stubConvention = beanToCopy.stubConvention
      this._rollConvention = beanToCopy.rollConvention
      this._firstRegularStartDate = beanToCopy.firstRegularStartDate
      this._lastRegularEndDate = beanToCopy.lastRegularEndDate
      this._overrideStartDate = beanToCopy.overrideStartDate
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          _startDate
        case -1607727319 => // endDate
          _endDate
        case -70023844 => // frequency
          _frequency
        case -1065319863 => // businessDayAdjustment
          _businessDayAdjustment
        case 429197561 => // startDateBusinessDayAdjustment
          _startDateBusinessDayAdjustment
        case -734327136 => // endDateBusinessDayAdjustment
          _endDateBusinessDayAdjustment
        case -31408449 => // stubConvention
          _stubConvention
        case -10223666 => // rollConvention
          _rollConvention
        case 2011803076 => // firstRegularStartDate
          _firstRegularStartDate
        case -1540679645 => // lastRegularEndDate
          _lastRegularEndDate
        case -599936828 => // overrideStartDate
          _overrideStartDate
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          this._startDate = newValue.asInstanceOf[LocalDate]
        case -1607727319 => // endDate
          this._endDate = newValue.asInstanceOf[LocalDate]
        case -70023844 => // frequency
          this._frequency = newValue.asInstanceOf[Frequency]
        case -1065319863 => // businessDayAdjustment
          this._businessDayAdjustment = newValue.asInstanceOf[BusinessDayAdjustment]
        case 429197561 => // startDateBusinessDayAdjustment
          this._startDateBusinessDayAdjustment = newValue.asInstanceOf[BusinessDayAdjustment]
        case -734327136 => // endDateBusinessDayAdjustment
          this._endDateBusinessDayAdjustment = newValue.asInstanceOf[BusinessDayAdjustment]
        case -31408449 => // stubConvention
          this._stubConvention = newValue.asInstanceOf[StubConvention]
        case -10223666 => // rollConvention
          this._rollConvention = newValue.asInstanceOf[RollConvention]
        case 2011803076 => // firstRegularStartDate
          this._firstRegularStartDate = newValue.asInstanceOf[LocalDate]
        case -1540679645 => // lastRegularEndDate
          this._lastRegularEndDate = newValue.asInstanceOf[LocalDate]
        case -599936828 => // overrideStartDate
          this._overrideStartDate = newValue.asInstanceOf[AdjustableDate]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): PeriodicSchedule = {
      new PeriodicSchedule(
          _startDate,
          _endDate,
          _frequency,
          _businessDayAdjustment,
          _startDateBusinessDayAdjustment,
          _endDateBusinessDayAdjustment,
          _stubConvention,
          _rollConvention,
          _firstRegularStartDate,
          _lastRegularEndDate,
          _overrideStartDate)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date, which is the start of the first schedule period.
     *
     * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted effective date.
     *
     * In most cases, the start date of a financial instrument is just after the trade date,
     * such as two business days later. However, the start date of a schedule is permitted
     * to be any date, which includes dates before or after the trade date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    def startDate(startDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(startDate, "startDate")
      this._startDate = startDate
      this
    }

    /**
     * Sets the end date, which is the end of the last schedule period.
     *
     * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted maturity date or unadjusted termination date.
     * This date must be after the start date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    def endDate(endDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(endDate, "endDate")
      this._endDate = endDate
      this
    }

    /**
     * Sets the regular periodic frequency to use.
     *
     * Most dates are calculated using a regular periodic frequency, such as every 3 months.
     * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    def frequency(frequency: Frequency): Builder = {
      JodaBeanUtils.notNull(frequency, "frequency")
      this._frequency = frequency
      this
    }

    /**
     * Sets the business day adjustment to apply.
     *
     * Each date in the calculated schedule is determined without taking into account weekends and holidays.
     * The adjustment specified here is used to convert those dates to valid business days.
     *
     * The start date and end date may have their own business day adjustment rules.
     * If those are not present, then this adjustment is used instead.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    def businessDayAdjustment(businessDayAdjustment: BusinessDayAdjustment): Builder = {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment")
      this._businessDayAdjustment = businessDayAdjustment
      this
    }

    /**
     * Sets the optional business day adjustment to apply to the start date.
     *
     * The start date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the start date to a valid business day.
     *
     * If this property is not present, the standard `businessDayAdjustment` property is used instead.
     * @param startDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    def startDateBusinessDayAdjustment(startDateBusinessDayAdjustment: BusinessDayAdjustment): Builder = {
      this._startDateBusinessDayAdjustment = startDateBusinessDayAdjustment
      this
    }

    /**
     * Sets the optional business day adjustment to apply to the end date.
     *
     * The end date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the end date to a valid business day.
     *
     * If this property is not present, the standard `businessDayAdjustment` property is used instead.
     * @param endDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    def endDateBusinessDayAdjustment(endDateBusinessDayAdjustment: BusinessDayAdjustment): Builder = {
      this._endDateBusinessDayAdjustment = endDateBusinessDayAdjustment
      this
    }

    /**
     * Sets the optional convention defining how to handle stubs.
     *
     * The stub convention is used during schedule construction to determine whether the irregular
     * remaining period occurs at the start or end of the schedule.
     * It also determines whether the irregular period is shorter or longer than the regular period.
     * This property interacts with the "explicit dates" of `PeriodicSchedule.getFirstRegularStartDate()`
     * and `PeriodicSchedule.getLastRegularEndDate()`.
     *
     * The convention 'None' may be used to explicitly indicate there are no stubs.
     * There must be no explicit dates.
     * This will be validated during schedule construction.
     *
     * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
     * The stubs themselves must be specified using explicit dates.
     * This will be validated during schedule construction.
     *
     * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
     * and 'SmartFinal' are used to indicate the type of stub to be generated.
     * The exact behavior varies depending on whether there are explicit dates or not:
     *
     * If explicit dates are specified, then the combination of stub convention an explicit date
     * will be validated during schedule construction. For example, the combination of an explicit dated
     * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
     * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
     *
     * If explicit dates are not specified, then it is not required that a stub is generated.
     * The convention determines whether to generate dates from the start date forward, or the
     * end date backwards. Date generation may or may not result in a stub, but if it does then
     * the stub will be of the correct type.
     *
     * When the stub convention is not present, the generation of stubs is based on the presence or absence
     * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
     * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
     * @param stubConvention  the new value
     * @return this, for chaining, not null
     */
    def stubConvention(stubConvention: StubConvention): Builder = {
      this._stubConvention = stubConvention
      this
    }

    /**
     * Sets the optional convention defining how to roll dates.
     *
     * The schedule periods are determined at the high level by repeatedly adding
     * the frequency to the start date, or subtracting it from the end date.
     * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the roll convention will be implied.
     * @param rollConvention  the new value
     * @return this, for chaining, not null
     */
    def rollConvention(rollConvention: RollConvention): Builder = {
      this._rollConvention = rollConvention
      this
    }

    /**
     * Sets the optional start date of the first regular schedule period, which is the end date of the initial stub.
     *
     * This is used to identify the boundary date between the initial stub and the first regular schedule period.
     *
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be on or after 'startDate' and on or before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
     * @param firstRegularStartDate  the new value
     * @return this, for chaining, not null
     */
    def firstRegularStartDate(firstRegularStartDate: LocalDate): Builder = {
      this._firstRegularStartDate = firstRegularStartDate
      this
    }

    /**
     * Sets the optional end date of the last regular schedule period, which is the start date of the final stub.
     *
     * This is used to identify the boundary date between the last regular schedule period and the final stub.
     *
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
     * @param lastRegularEndDate  the new value
     * @return this, for chaining, not null
     */
    def lastRegularEndDate(lastRegularEndDate: LocalDate): Builder = {
      this._lastRegularEndDate = lastRegularEndDate
      this
    }

    /**
     * Sets the optional start date of the first schedule period, overriding normal schedule generation.
     *
     * This property is rarely used, and is generally needed when accrual starts before the effective date.
     * If specified, it overrides the start date of the first period once schedule generation has been completed.
     * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
     * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
     *
     * If a roll convention is explicitly specified and the regular start date does not match it,
     * then the override will be used when generating regular periods.
     *
     * If set, it should be different to the start date, although this is not validated.
     * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
     * and before 'endDate'.
     *
     * During schedule generation, if this is present it will be used to override the start date
     * of the first generated schedule period.
     * If not present, then the start of the first period will be the normal start date.
     * @param overrideStartDate  the new value
     * @return this, for chaining, not null
     */
    def overrideStartDate(overrideStartDate: AdjustableDate): Builder = {
      this._overrideStartDate = overrideStartDate
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(384)
      buf.append("PeriodicSchedule.Builder{")
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(_startDate)).append(',').append(' ')
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(_endDate)).append(',').append(' ')
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(_frequency)).append(',').append(' ')
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(_businessDayAdjustment)).append(',').append(' ')
      buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(_startDateBusinessDayAdjustment)).append(',').append(' ')
      buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(_endDateBusinessDayAdjustment)).append(',').append(' ')
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(_stubConvention)).append(',').append(' ')
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(_rollConvention)).append(',').append(' ')
      buf.append("firstRegularStartDate").append('=').append(JodaBeanUtils.toString(_firstRegularStartDate)).append(',').append(' ')
      buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(_lastRegularEndDate)).append(',').append(' ')
      buf.append("overrideStartDate").append('=').append(JodaBeanUtils.toString(_overrideStartDate))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
