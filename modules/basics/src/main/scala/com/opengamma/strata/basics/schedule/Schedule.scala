/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.io.Serializable
import java.time.LocalDate
import java.time.Period
import java.util.ArrayList
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.google.common.collect.ImmutableList
import com.opengamma.strata.basics.date.DateAdjuster
import com.opengamma.strata.basics.date.DayCount.ScheduleInfo
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.tuple.Pair

/**
 * A complete schedule of periods (date ranges), with both unadjusted and adjusted dates.
 *
 * The schedule consists of one or more adjacent periods (date ranges).
 * This is typically used as the basis for financial calculations, such as accrual of interest.
 *
 * It is recommended to create a `Schedule` using a `PeriodicSchedule`.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class Schedule private (
    rawPeriods: List[SchedulePeriod],
    /**
     * The periodic frequency used when building the schedule.
     *
     * If the schedule was not built from a regular periodic frequency,
     * then the frequency should be a suitable estimate.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val frequency: Frequency,
    /**
     * The roll convention used when building the schedule.
     *
     * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val rollConvention: RollConvention)
  extends ScheduleInfo with ImmutableBean with Serializable {

  JodaBeanUtils.notEmpty(rawPeriods, "periods")
  JodaBeanUtils.notNull(frequency, "frequency")
  JodaBeanUtils.notNull(rollConvention, "rollConvention")

  /**
   * The schedule periods.
   *
   * There will be at least one period.
   * The periods are ordered from earliest to latest.
   * It is intended that each period is adjacent to the next one, however each
   * period is independent and non-adjacent periods are allowed.
   */
  @(PropertyDefinition @field)(validate = "notEmpty")
  private val periods: ImmutableList[SchedulePeriod] = ImmutableList.copyOf[SchedulePeriod](rawPeriods)

  //-------------------------------------------------------------------------
  /**
   * Gets the number of periods in the schedule.
   *
   * This returns the number of periods, which will be at least one.
   * 
   * @return the number of periods
   */
  def size(): Int = {
    periods.size()
  }

  /**
   * Checks if this schedule represents a single 'Term' period.
   *
   * A 'Term' schedule has one period and a frequency of 'Term'.
   * 
   * @return true if this is a 'Term' schedule
   */
  def isTerm(): Boolean = {
    size() == 1 && frequency.equals(Frequency.TERM)
  }

  /**
   * Checks if this schedule has a single period.
   * 
   * @return true if this is a single period
   */
  def isSinglePeriod(): Boolean = {
    size() == 1
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a schedule period by index.
   *
   * This returns a period using a zero-based index.
   * 
   * @param index  the zero-based period index
   * @return the schedule period
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def getPeriod(index: Int): SchedulePeriod = {
    periods.get(index)
  }

  /**
   * Gets the first schedule period.
   * 
   * @return the first schedule period
   */
  def getFirstPeriod(): SchedulePeriod = {
    periods.get(0)
  }

  /**
   * Gets the last schedule period.
   * 
   * @return the last schedule period
   */
  def getLastPeriod(): SchedulePeriod = {
    periods.get(periods.size() - 1)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the schedule.
   *
   * The first date in the schedule, typically treated as inclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the schedule start date
   */
  override def getStartDate(): LocalDate = {
    getFirstPeriod().getStartDate()
  }

  /**
   * Gets the end date of the schedule.
   *
   * The last date in the schedule, typically treated as exclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the schedule end date
   */
  override def getEndDate(): LocalDate = {
    getLastPeriod().getEndDate()
  }

  /**
   * Gets the unadjusted start date.
   *
   * The start date before any business day adjustment.
   * 
   * @return the unadjusted schedule start date
   */
  def getUnadjustedStartDate(): LocalDate = {
    getFirstPeriod().getUnadjustedStartDate()
  }

  /**
   * Gets the unadjusted end date.
   *
   * The end date before any business day adjustment.
   * 
   * @return the unadjusted schedule end date
   */
  def getUnadjustedEndDate(): LocalDate = {
    getLastPeriod().getUnadjustedEndDate()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the initial stub if it exists.
   *
   * There is an initial stub if the first period is a stub and the frequency is not 'Term'.
   *
   * A period will be allocated to one and only one of `getInitialStub()`,
   * `getRegularPeriods()` and `getFinalStub()`.
   * 
   * @return the initial stub, empty if no initial stub
   */
  def getInitialStub(): Optional[SchedulePeriod] = {
    if (isInitialStub()) Optional.of(getFirstPeriod()) else Optional.empty()
  }

  // checks if there is an initial stub
  private def isInitialStub(): Boolean = {
    !isTerm() && !getFirstPeriod().isRegular(frequency, rollConvention)
  }

  /**
   * Gets the final stub if it exists.
   *
   * There is a final stub if there is more than one period and the last
   * period is a stub.
   *
   * A period will be allocated to one and only one of `getInitialStub()`,
   * `getRegularPeriods()` and `getFinalStub()`.
   * 
   * @return the final stub, empty if no final stub
   */
  def getFinalStub(): Optional[SchedulePeriod] = {
    if (isFinalStub()) Optional.of(getLastPeriod()) else Optional.empty()
  }

  // checks if there is a final stub
  private def isFinalStub(): Boolean = {
    !isSinglePeriod() && !getLastPeriod().isRegular(frequency, rollConvention)
  }

  /**
   * Gets the stubs if they exist.
   *
   * This method returns the initial and final stub.
   * A flag is used to handle the case where there are no regular periods and it is unclear whether
   * the stub is initial or final.
   *
   * A period will be allocated to one and only one of `getStubs` and `getRegularPeriods()`.
   * 
   * @param preferFinal true to prefer final if there is only one period
   * @return the stubs, empty if no stub
   */
  def getStubs(preferFinal: Boolean): Pair[Optional[SchedulePeriod], Optional[SchedulePeriod]] = {
    val initialStub = getInitialStub()
    if (preferFinal && size() == 1 && initialStub.isPresent()) {
      return Pair.of(Optional.empty[SchedulePeriod](), initialStub)
    }
    Pair.of(initialStub, getFinalStub())
  }

  /**
   * Gets the regular schedule periods.
   *
   * The regular periods exclude any initial or final stub.
   * In most cases, the periods returned will be regular, corresponding to the periodic
   * frequency and roll convention, however there are cases when this is not true.
   * This includes the case where `isTerm()` returns true.
   * See `SchedulePeriod.isRegular(Frequency, RollConvention)`.
   *
   * A period will be allocated to one and only one of `getInitialStub()`,
   * `getRegularPeriods()` and `getFinalStub()`.
   * 
   * @return the non-stub schedule periods
   */
  def getRegularPeriods(): ImmutableList[SchedulePeriod] = {
    if (isTerm()) {
      return periods
    }
    val startStub = if (isInitialStub()) 1 else 0
    val endStub = if (isFinalStub()) 1 else 0
    if (startStub == 0 && endStub == 0) periods else periods.subList(startStub, periods.size() - endStub)
  }

  /**
   * Gets the complete list of unadjusted dates.
   *
   * This returns a list including all the unadjusted period boundary dates.
   * This is the same as a list containing the unadjusted start date of the schedule
   * followed by the unadjusted end date of each period.
   * 
   * @return the list of unadjusted dates, in order
   */
  def getUnadjustedDates(): ImmutableList[LocalDate] = {
    val dates = ImmutableList.builder[LocalDate]()
    dates.add(getUnadjustedStartDate())
    val it = periods.iterator()
    while (it.hasNext()) {
      dates.add(it.next().getUnadjustedEndDate())
    }
    dates.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the end of month convention is in use.
   *
   * If true then when building a schedule, dates will be at the end-of-month if the
   * first date in the series is at the end-of-month.
   * 
   * @return true if the end of month convention is in use
   */
  override def isEndOfMonthConvention(): Boolean = {
    rollConvention eq RollConventions.EOM
  }

  /**
   * Finds the period end date given a date in the period.
   *
   * The first matching period is returned.
   * The adjusted start and end dates of each period are used in the comparison.
   * The start date is included, the end date is excluded.
   * 
   * @param date  the date to find
   * @return the end date of the period that includes the specified date
   */
  override def getPeriodEndDate(date: LocalDate): LocalDate = {
    periods.stream()
        .filter(p => p.contains(date))
        .map[LocalDate](p => p.getEndDate())
        .findFirst()
        .orElseThrow(() => new IllegalArgumentException("Date is not contained in any period"))
  }

  //-------------------------------------------------------------------------
  /**
   * Merges this schedule to form a new schedule with a single 'Term' period.
   *
   * The result will have one period of type 'Term', with dates matching this schedule.
   * 
   * @return the merged 'Term' schedule
   */
  def mergeToTerm(): Schedule = {
    if (isTerm()) {
      return this
    }
    val first = getFirstPeriod()
    val last = getLastPeriod()
    Schedule.ofTerm(SchedulePeriod.of(
        first.getStartDate(),
        last.getEndDate(),
        first.getUnadjustedStartDate(),
        last.getUnadjustedEndDate()))
  }

  /**
   * Merges this schedule to form a new schedule by combining the schedule periods.
   *
   * This produces a schedule where some periods are merged together.
   * For example, this could be used to convert a 3 monthly schedule into a 6 monthly schedule.
   *
   * The merging is controlled by the group size, which defines the number of periods
   * to merge together in the result. For example, to convert a 3 monthly schedule into
   * a 6 monthly schedule the group size would be 2 (6 divided by 3).
   *
   * A group size of zero or less will throw an exception.
   * A group size of 1 will return this schedule providing that the specified start and end date match.
   * A larger group size will return a schedule where each group of regular periods are merged.
   *
   * The specified dates must be one of the dates of this schedule (unadjusted or adjusted).
   * All periods of this schedule before the first regular start date, if any, will form a single period in the result.
   * All periods of this schedule after the last regular start date, if any, will form a single period in the result.
   * If this schedule has an initial or final stub, it may be merged with a regular period as part of the process.
   *
   * For example, a schedule with an initial stub and 5 regular periods can be grouped by 2 if the
   * specified `firstRegularStartDate` equals the end of the first regular period.
   * 
   * @param groupSize  the group size
   * @param firstRegularStartDate  the unadjusted start date of the first regular payment period
   * @param lastRegularEndDate  the unadjusted end date of the last regular payment period
   * @return the merged schedule
   * @throws java.lang.IllegalArgumentException if the group size is zero or less
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the merged schedule cannot be created because the dates don't
   *   match this schedule or the regular periods don't match the grouping size
   */
  def merge(groupSize: Int, firstRegularStartDate: LocalDate, lastRegularEndDate: LocalDate): Schedule = {
    ArgChecker.notNegativeOrZero(groupSize, "groupSize")
    ArgChecker.inOrderOrEqual(firstRegularStartDate, lastRegularEndDate, "firstRegularStartDate", "lastRegularEndDate")
    if (isSinglePeriod() || groupSize == 1) {
      return this
    }
    // determine stubs and regular
    var startRegularIndex = -1
    var endRegularIndex = -1
    var i = 0
    while (i < size()) {
      val period = periods.get(i)
      if (period.getUnadjustedStartDate().equals(firstRegularStartDate) || period.getStartDate().equals(firstRegularStartDate)) {
        startRegularIndex = i
      }
      if (period.getUnadjustedEndDate().equals(lastRegularEndDate) || period.getEndDate().equals(lastRegularEndDate)) {
        endRegularIndex = i + 1
      }
      i += 1
    }
    if (startRegularIndex < 0) {
      throw new ScheduleException(
          "Unable to merge schedule, firstRegularStartDate {} does not match any date in the underlying schedule {}",
          firstRegularStartDate,
          getUnadjustedDates())
    }
    if (endRegularIndex < 0) {
      throw new ScheduleException(
          "Unable to merge schedule, lastRegularEndDate {} does not match any date in the underlying schedule {}",
          lastRegularEndDate,
          getUnadjustedDates())
    }
    val numberRegular = endRegularIndex - startRegularIndex
    if ((numberRegular % groupSize) != 0) {
      val newFrequency = frequency.getPeriod().multipliedBy(groupSize)
      throw new ScheduleException(
          "Unable to merge schedule, firstRegularStartDate {} and lastRegularEndDate {} cannot be used to " +
              "create regular periods of frequency '{}'",
          firstRegularStartDate, lastRegularEndDate, newFrequency)
    }
    val newSchedule: List[SchedulePeriod] = new ArrayList[SchedulePeriod]()
    if (startRegularIndex > 0) {
      newSchedule.add(createSchedulePeriod(periods.subList(0, startRegularIndex)))
    }
    i = startRegularIndex
    while (i < endRegularIndex) {
      newSchedule.add(createSchedulePeriod(periods.subList(i, i + groupSize)))
      i += groupSize
    }
    if (endRegularIndex < periods.size()) {
      newSchedule.add(createSchedulePeriod(periods.subList(endRegularIndex, periods.size())))
    }
    // build schedule
    Schedule.builder()
        .periods(newSchedule)
        .frequency(Frequency.of(frequency.getPeriod().multipliedBy(groupSize)))
        .rollConvention(rollConvention)
        .build()
  }

  /**
   * Merges this schedule to form a new schedule by combining the regular schedule periods.
   *
   * This produces a schedule where some periods are merged together.
   * For example, this could be used to convert a 3 monthly schedule into a 6 monthly schedule.
   *
   * The merging is controlled by the group size, which defines the number of periods
   * to merge together in the result. For example, to convert a 3 monthly schedule into
   * a 6 monthly schedule the group size would be 2 (6 divided by 3).
   *
   * A group size of zero or less will throw an exception.
   * A group size of 1 will return this schedule.
   * A larger group size will return a schedule where each group of regular periods are merged.
   * The roll flag is used to determine the direction in which grouping occurs.
   *
   * Any existing stub periods are considered to be special, and are not merged.
   * Even if the grouping results in an excess period, such as 10 periods with a group size
   * of 3, the excess period will not be merged with a stub.
   *
   * If this period is a 'Term' period, this schedule is returned.
   * 
   * @param groupSize  the group size
   * @param rollForwards  whether to roll forwards (true) or backwards (false)
   * @return the merged schedule
   * @throws java.lang.IllegalArgumentException if the group size is zero or less
   */
  def mergeRegular(groupSize: Int, rollForwards: Boolean): Schedule = {
    ArgChecker.notNegativeOrZero(groupSize, "groupSize")
    if (isSinglePeriod() || groupSize == 1) {
      return this
    }
    val newSchedule: List[SchedulePeriod] = new ArrayList[SchedulePeriod]()
    // retain initial stub
    val initialStub = getInitialStub()
    if (initialStub.isPresent()) {
      newSchedule.add(initialStub.get())
    }
    // merge regular, handling stubs via min/max
    val regularPeriods = getRegularPeriods()
    val regularSize = regularPeriods.size()
    val remainder = regularSize % groupSize
    val startIndex = if (rollForwards || remainder == 0) 0 else -(groupSize - remainder)
    var i = startIndex
    while (i < regularSize) {
      val from = Math.max(i, 0)
      val to = Math.min(i + groupSize, regularSize)
      newSchedule.add(createSchedulePeriod(regularPeriods.subList(from, to)))
      i += groupSize
    }
    // retain final stub
    val finalStub = getFinalStub()
    if (finalStub.isPresent()) {
      newSchedule.add(finalStub.get())
    }
    // build schedule
    Schedule.builder()
        .periods(newSchedule)
        .frequency(Frequency.of(frequency.getPeriod().multipliedBy(groupSize)))
        .rollConvention(rollConvention)
        .build()
  }

  // creates a schedule period
  private def createSchedulePeriod(accruals: List[SchedulePeriod]): SchedulePeriod = {
    val first = accruals.get(0)
    if (accruals.size() == 1) {
      return first
    }
    val last = accruals.get(accruals.size() - 1)
    SchedulePeriod.of(
        first.getStartDate(),
        last.getEndDate(),
        first.getUnadjustedStartDate(),
        last.getUnadjustedEndDate())
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this schedule to a schedule where the start and end dates are
   * adjusted using the specified adjuster.
   *
   * The result will have the same number of periods, but each start date and
   * end date is replaced by the adjusted date as returned by the adjuster.
   * The unadjusted start date and unadjusted end date of each period will not be changed.
   *
   * A special rule is applied to the first start date and the last end date.
   * If the first period once adjusted is empty then the unadjusted start date is used instead of the adjusted one.
   * If the last period once adjusted is empty then the unadjusted end date is used instead of the adjusted one.
   * This rule avoids some unnecessary exceptions.
   * 
   * @param adjuster  the adjuster to use
   * @return the adjusted schedule
   * @throws java.lang.IllegalArgumentException if a period is invalid once adjusted
   */
  def toAdjusted(adjuster: DateAdjuster): Schedule = {
    // implementation needs to return 'this' if unchanged to optimize downstream code
    var adjusted = false
    val builder = ImmutableList.builder[SchedulePeriod]()
    val size = periods.size()
    var i = 0
    while (i < size) {
      val period = periods.get(i)
      val mergeType = if (i == 0) -1 else (if (i == size - 1) 1 else 0)
      val adjPeriod = period.toAdjusted(adjuster, mergeType)
      builder.add(adjPeriod)
      adjusted |= (adjPeriod ne period)
      i += 1
    }
    if (adjusted) new Schedule(builder.build(), frequency, rollConvention) else this
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this schedule to a schedule where every adjusted date is reset
   * to the unadjusted equivalent.
   *
   * The result will have the same number of periods, but each start date and
   * end date is replaced by the matching unadjusted start or end date.
   * 
   * @return the equivalent unadjusted schedule
   */
  def toUnadjusted(): Schedule = {
    toBuilder()
        .periods(periods.stream()
            .map[SchedulePeriod](p => p.toUnadjusted())
            .collect(Guavate.toImmutableList[SchedulePeriod]()))
        .build()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): Schedule.Meta = {
    Schedule.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the schedule periods.
   *
   * There will be at least one period.
   * The periods are ordered from earliest to latest.
   * It is intended that each period is adjacent to the next one, however each
   * period is independent and non-adjacent periods are allowed.
   * @return the value of the property, not empty
   */
  def getPeriods(): ImmutableList[SchedulePeriod] = {
    periods
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic frequency used when building the schedule.
   *
   * If the schedule was not built from a regular periodic frequency,
   * then the frequency should be a suitable estimate.
   * @return the value of the property, not null
   */
  override def getFrequency(): Frequency = {
    frequency
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the roll convention used when building the schedule.
   *
   * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
   * @return the value of the property, not null
   */
  def getRollConvention(): RollConvention = {
    rollConvention
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): Schedule.Builder = {
    new Schedule.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[Schedule]
      JodaBeanUtils.equal(periods, other.periods) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(periods)
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency)
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(128)
    buf.append("Schedule{")
    buf.append("periods").append('=').append(JodaBeanUtils.toString(periods)).append(',').append(' ')
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ')
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object Schedule {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Schedule.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a 'Term' instance based on a single period.
   *
   * A 'Term' schedule has one period with a frequency of 'Term'.
   * 
   * @param period  the single period
   * @return the merged 'Term' schedule
   */
  def ofTerm(period: SchedulePeriod): Schedule = {
    ArgChecker.notNull(period, "period")
    Schedule.builder()
        .periods(ImmutableList.of(period))
        .frequency(Frequency.TERM)
        .rollConvention(RollConventions.NONE)
        .build()
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `Schedule`.
   * @return the meta-bean, not null
   */
  def meta(): Schedule.Meta = {
    Schedule.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): Schedule.Builder = {
    new Schedule.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `Schedule`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `periods` property.
     */
    private val periodsProperty: MetaProperty[ImmutableList[SchedulePeriod]] = DirectMetaProperty.ofImmutable(
        this, "periods", classOf[Schedule], classOf[ImmutableList[?]].asInstanceOf[Class[ImmutableList[SchedulePeriod]]])
    /**
     * The meta-property for the `frequency` property.
     */
    private val frequencyProperty: MetaProperty[Frequency] = DirectMetaProperty.ofImmutable(
        this, "frequency", classOf[Schedule], classOf[Frequency])
    /**
     * The meta-property for the `rollConvention` property.
     */
    private val rollConventionProperty: MetaProperty[RollConvention] = DirectMetaProperty.ofImmutable(
        this, "rollConvention", classOf[Schedule], classOf[RollConvention])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "periods",
        "frequency",
        "rollConvention")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -678739246 => // periods
          periodsProperty
        case -70023844 => // frequency
          frequencyProperty
        case -10223666 => // rollConvention
          rollConventionProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): Schedule.Builder = {
      new Schedule.Builder()
    }

    override def beanType(): Class[? <: Schedule] = {
      classOf[Schedule]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `periods` property.
     * @return the meta-property, not null
     */
    def periods(): MetaProperty[ImmutableList[SchedulePeriod]] = {
      periodsProperty
    }

    /**
     * The meta-property for the `frequency` property.
     * @return the meta-property, not null
     */
    def frequency(): MetaProperty[Frequency] = {
      frequencyProperty
    }

    /**
     * The meta-property for the `rollConvention` property.
     * @return the meta-property, not null
     */
    def rollConvention(): MetaProperty[RollConvention] = {
      rollConventionProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -678739246 => // periods
          bean.asInstanceOf[Schedule].getPeriods()
        case -70023844 => // frequency
          bean.asInstanceOf[Schedule].getFrequency()
        case -10223666 => // rollConvention
          bean.asInstanceOf[Schedule].getRollConvention()
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
   * The bean-builder for `Schedule`.
   */
  final class Builder private[schedule] () extends DirectFieldsBeanBuilder[Schedule] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _periods: List[SchedulePeriod] = ImmutableList.of[SchedulePeriod]()
    private var _frequency: Frequency = uninitialized
    private var _rollConvention: RollConvention = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[schedule] def this(beanToCopy: Schedule) = {
      this()
      this._periods = beanToCopy.getPeriods()
      this._frequency = beanToCopy.getFrequency()
      this._rollConvention = beanToCopy.getRollConvention()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -678739246 => // periods
          _periods
        case -70023844 => // frequency
          _frequency
        case -10223666 => // rollConvention
          _rollConvention
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -678739246 => // periods
          this._periods = newValue.asInstanceOf[List[SchedulePeriod]]
        case -70023844 => // frequency
          this._frequency = newValue.asInstanceOf[Frequency]
        case -10223666 => // rollConvention
          this._rollConvention = newValue.asInstanceOf[RollConvention]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): Schedule = {
      new Schedule(
          _periods,
          _frequency,
          _rollConvention)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the schedule periods.
     *
     * There will be at least one period.
     * The periods are ordered from earliest to latest.
     * It is intended that each period is adjacent to the next one, however each
     * period is independent and non-adjacent periods are allowed.
     * @param periods  the new value, not empty
     * @return this, for chaining, not null
     */
    def periods(periods: List[SchedulePeriod]): Builder = {
      JodaBeanUtils.notEmpty(periods, "periods")
      this._periods = periods
      this
    }

    /**
     * Sets the `periods` property in the builder
     * from an array of objects.
     * @param periods  the new value, not empty
     * @return this, for chaining, not null
     */
    @varargs
    def periods(periods: SchedulePeriod*): Builder = {
      this.periods(ImmutableList.copyOf[SchedulePeriod](periods.toArray[SchedulePeriod]))
    }

    /**
     * Sets the periodic frequency used when building the schedule.
     *
     * If the schedule was not built from a regular periodic frequency,
     * then the frequency should be a suitable estimate.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    def frequency(frequency: Frequency): Builder = {
      JodaBeanUtils.notNull(frequency, "frequency")
      this._frequency = frequency
      this
    }

    /**
     * Sets the roll convention used when building the schedule.
     *
     * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
     * @param rollConvention  the new value, not null
     * @return this, for chaining, not null
     */
    def rollConvention(rollConvention: RollConvention): Builder = {
      JodaBeanUtils.notNull(rollConvention, "rollConvention")
      this._rollConvention = rollConvention
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("Schedule.Builder{")
      buf.append("periods").append('=').append(JodaBeanUtils.toString(_periods)).append(',').append(' ')
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(_frequency)).append(',').append(' ')
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(_rollConvention))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
