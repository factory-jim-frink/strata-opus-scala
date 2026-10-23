/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.io.Serializable
import java.time.LocalDate
import java.time.Period
import java.util.Map
import java.util.NoSuchElementException

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutablePreBuild
import org.joda.beans.gen.ImmutableValidator
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.google.common.collect.ComparisonChain
import com.opengamma.strata.basics.date.BusinessDayAdjustment
import com.opengamma.strata.basics.date.DateAdjuster
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.collect.ArgChecker

/**
 * A period in a schedule.
 *
 * This consists of a single period (date range) within a schedule.
 * This is typically used as the basis for financial calculations, such as accrual of interest.
 *
 * Two pairs of dates are provided, start/end and unadjustedStart/unadjustedEnd.
 * The period itself runs from `startDate` to `endDate`.
 * The `unadjustedStartDate` and `unadjustedEndDate` are the dates used to
 * calculate the `startDate` and `endDate` when applying business day adjustment.
 *
 * For example, consider a schedule that has periods every three months on the 10th of the month.
 * From time to time, the scheduled date will be a weekend or holiday.
 * In this case, a rule may apply moving the date to a valid business day.
 * If this happens, then the "unadjusted" date is the original date in the periodic schedule
 * and the "adjusted" date is the related valid business day.
 * Note that not all schedules apply a business day adjustment.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class SchedulePeriod private (
    /**
     * The start date of this period, used for financial calculations such as interest accrual.
     *
     * The first date in the schedule period, typically treated as inclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val startDate: LocalDate,
    /**
     * The end date of this period, used for financial calculations such as interest accrual.
     *
     * The last date in the schedule period, typically treated as exclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val endDate: LocalDate,
    /**
     * The unadjusted start date.
     *
     * The start date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the start date.
     *
     * When building, this will default to the start date if not specified.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val unadjustedStartDate: LocalDate,
    /**
     * The unadjusted end date.
     *
     * The end date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the end date.
     *
     * When building, this will default to the end date if not specified.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val unadjustedEndDate: LocalDate)
  extends ImmutableBean with Comparable[SchedulePeriod] with Serializable {

  JodaBeanUtils.notNull(startDate, "startDate")
  JodaBeanUtils.notNull(endDate, "endDate")
  JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate")
  JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate")
  validate()

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    ArgChecker.inOrderNotEqual(unadjustedStartDate, unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate")
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate")
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the length of the period.
   *
   * This returns the length of the period, considering the adjusted start and end dates.
   * The calculation does not involve a day count or holiday calendar.
   * The period is calculated using `Period.between(LocalDate, LocalDate)` and as
   * such includes the start date and excludes the end date.
   * 
   * @return the length of the period
   */
  def length(): Period = {
    Period.between(startDate, endDate)
  }

  /**
   * Calculates the number of days in the period.
   *
   * This returns the actual number of days in the period, considering the adjusted start and end dates.
   * The calculation does not involve a day count or holiday calendar.
   * The length includes one date and excludes the other.
   * 
   * @return the actual number of days in the period
   */
  def lengthInDays(): Int = {
    Math.toIntExact(endDate.toEpochDay() - startDate.toEpochDay())
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the year fraction using the specified day count.
   *
   * Additional information from the schedule is made available to the day count algorithm.
   * 
   * @param dayCount  the day count convention
   * @param schedule  the schedule that contains this period
   * @return the year fraction, calculated via the day count
   */
  def yearFraction(dayCount: DayCount, schedule: Schedule): Double = {
    ArgChecker.notNull(dayCount, "dayCount")
    ArgChecker.notNull(schedule, "schedule")
    dayCount.yearFraction(startDate, endDate, schedule)
  }

  /**
   * Checks if this period is regular according to the specified frequency and roll convention.
   *
   * A schedule period is normally created from a frequency and roll convention.
   * These can therefore be used to determine if the period is regular, which simply
   * means that the period end date can be generated from the start date and vice versa.
   * 
   * @param frequency  the frequency
   * @param rollConvention  the roll convention
   * @return true if the period is regular
   */
  def isRegular(frequency: Frequency, rollConvention: RollConvention): Boolean = {
    ArgChecker.notNull(frequency, "frequency")
    ArgChecker.notNull(rollConvention, "rollConvention")
    rollConvention.next(unadjustedStartDate, frequency).equals(unadjustedEndDate) &&
        rollConvention.previous(unadjustedEndDate, frequency).equals(unadjustedStartDate)
  }

  /**
   * Checks if this period contains the specified date.
   *
   * The adjusted start and end dates are used in the comparison.
   * The start date is included, the end date is excluded.
   * 
   * @param date  the date to check
   * @return true if this period contains the date
   */
  def contains(date: LocalDate): Boolean = {
    ArgChecker.notNull(date, "date")
    !date.isBefore(startDate) && date.isBefore(endDate)
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a sub-schedule within this period.
   *
   * The sub-schedule will have the one or more periods.
   * The schedule is bounded by the unadjusted start and end date of this period.
   * The frequency and roll convention are used to build unadjusted schedule dates.
   * The stub convention is used to handle any remaining time when the new frequency
   * does not evenly divide into the period.
   *
   * @param frequency  the frequency of the sub-schedule
   * @param rollConvention  the roll convention to use for rolling
   * @param stubConvention  the stub convention to use for any excess
   * @param adjustment  the business day adjustment to apply to the sub-schedule
   * @return the sub-schedule
   * @throws com.opengamma.strata.basics.schedule.ScheduleException if the schedule cannot be created
   */
  def subSchedule(
      frequency: Frequency,
      rollConvention: RollConvention,
      stubConvention: StubConvention,
      adjustment: BusinessDayAdjustment): PeriodicSchedule = {

    PeriodicSchedule.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(adjustment)
        .rollConvention(rollConvention)
        .stubConvention(stubConvention)
        .build()
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this period to one where the start and end dates are adjusted using the specified adjuster.
   *
   * The start date of the result will be the start date of this period as altered by the specified adjuster.
   * The end date of the result will be the end date of this period as altered by the specified adjuster.
   * The unadjusted start date and unadjusted end date will be the same as in this period.
   *
   * The adjuster will typically be obtained from `BusinessDayAdjustment.resolve(ReferenceData)`.
   * 
   * @param adjuster  the adjuster to use
   * @return the adjusted schedule period
   * @throws java.lang.IllegalArgumentException if a period is invalid once adjusted
   */
  def toAdjusted(adjuster: DateAdjuster): SchedulePeriod = {
    // implementation needs to return 'this' if unchanged to optimize downstream code
    val resultStart = adjuster.adjust(startDate)
    val resultEnd = adjuster.adjust(endDate)
    if (resultStart.equals(startDate) && resultEnd.equals(endDate)) {
      return this
    }
    SchedulePeriod.of(resultStart, resultEnd, unadjustedStartDate, unadjustedEndDate)
  }

  // converts to adjusted handling short periods that adjust to empty adjusted periods
  // the merge type, -1 to keep the start date unadjusted, 0 to throw, 1 to keep the end date unadjusted
  private[schedule] def toAdjusted(adjuster: DateAdjuster, mergeType: Int): SchedulePeriod = {
    // implementation needs to return 'this' if unchanged to optimize downstream code
    var resultStart = adjuster.adjust(startDate)
    var resultEnd = adjuster.adjust(endDate)
    // retain as unadjusted (do not refactor! this checks int before LocalDate for performance)
    if (mergeType == -1 && resultStart.equals(resultEnd)) {
      resultStart = startDate
    } else if (mergeType == 1 && resultStart.equals(resultEnd)) {
      resultEnd = endDate
    }
    // return "no change" which optimizes up the stack
    if (resultStart.equals(startDate) && resultEnd.equals(endDate)) {
      return this
    }
    SchedulePeriod.of(resultStart, resultEnd, unadjustedStartDate, unadjustedEndDate)
  }

  /**
   * Converts this period to one where the start and end dates are set to the unadjusted dates.
   *
   * The start date of the result will be the unadjusted start date of this period.
   * The end date of the result will be the unadjusted end date of this period.
   * The unadjusted start date and unadjusted end date will be the same as in this period.
   * 
   * @return the unadjusted schedule period
   */
  def toUnadjusted(): SchedulePeriod = {
    if (unadjustedStartDate.equals(startDate) && unadjustedEndDate.equals(endDate)) {
      return this
    }
    SchedulePeriod.of(unadjustedStartDate, unadjustedEndDate)
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this period to another by unadjusted start date, then unadjusted end date.
   * 
   * @param other  the other period
   * @return the comparison value
   */
  override def compareTo(other: SchedulePeriod): Int = {
    ComparisonChain.start()
        .compare(unadjustedStartDate, other.unadjustedStartDate)
        .compare(unadjustedEndDate, other.unadjustedEndDate)
        .result()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): SchedulePeriod.Meta = {
    SchedulePeriod.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of this period, used for financial calculations such as interest accrual.
   *
   * The first date in the schedule period, typically treated as inclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  def getStartDate(): LocalDate = {
    startDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of this period, used for financial calculations such as interest accrual.
   *
   * The last date in the schedule period, typically treated as exclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  def getEndDate(): LocalDate = {
    endDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted start date.
   *
   * The start date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the start date.
   *
   * When building, this will default to the start date if not specified.
   * @return the value of the property, not null
   */
  def getUnadjustedStartDate(): LocalDate = {
    unadjustedStartDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted end date.
   *
   * The end date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the end date.
   *
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  def getUnadjustedEndDate(): LocalDate = {
    unadjustedEndDate
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): SchedulePeriod.Builder = {
    new SchedulePeriod.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[SchedulePeriod]
      JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(160)
    buf.append("SchedulePeriod{")
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ')
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ')
    buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ')
    buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object SchedulePeriod {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(SchedulePeriod.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the adjusted and unadjusted dates.
   * 
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @param unadjustedStartDate  the unadjusted start date
   * @param unadjustedEndDate  the adjusted end date
   * @return the period
   */
  def of(
      startDate: LocalDate,
      endDate: LocalDate,
      unadjustedStartDate: LocalDate,
      unadjustedEndDate: LocalDate): SchedulePeriod = {

    new SchedulePeriod(startDate, endDate, unadjustedStartDate, unadjustedEndDate)
  }

  /**
   * Obtains an instance from two dates.
   *
   * This factory is used when there is no business day adjustment of schedule dates.
   * 
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @return the period
   */
  def of(startDate: LocalDate, endDate: LocalDate): SchedulePeriod = {
    new SchedulePeriod(startDate, endDate, startDate, endDate)
  }

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private def preBuild(builder: Builder): Unit = {
    if (builder._unadjustedStartDate == null) {
      builder._unadjustedStartDate = builder._startDate
    }
    if (builder._unadjustedEndDate == null) {
      builder._unadjustedEndDate = builder._endDate
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `SchedulePeriod`.
   * @return the meta-bean, not null
   */
  def meta(): SchedulePeriod.Meta = {
    SchedulePeriod.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): SchedulePeriod.Builder = {
    new SchedulePeriod.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `SchedulePeriod`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `startDate` property.
     */
    private val startDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "startDate", classOf[SchedulePeriod], classOf[LocalDate])
    /**
     * The meta-property for the `endDate` property.
     */
    private val endDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "endDate", classOf[SchedulePeriod], classOf[LocalDate])
    /**
     * The meta-property for the `unadjustedStartDate` property.
     */
    private val unadjustedStartDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", classOf[SchedulePeriod], classOf[LocalDate])
    /**
     * The meta-property for the `unadjustedEndDate` property.
     */
    private val unadjustedEndDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", classOf[SchedulePeriod], classOf[LocalDate])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          startDateProperty
        case -1607727319 => // endDate
          endDateProperty
        case 1457691881 => // unadjustedStartDate
          unadjustedStartDateProperty
        case 31758114 => // unadjustedEndDate
          unadjustedEndDateProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): SchedulePeriod.Builder = {
      new SchedulePeriod.Builder()
    }

    override def beanType(): Class[? <: SchedulePeriod] = {
      classOf[SchedulePeriod]
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
     * The meta-property for the `unadjustedStartDate` property.
     * @return the meta-property, not null
     */
    def unadjustedStartDate(): MetaProperty[LocalDate] = {
      unadjustedStartDateProperty
    }

    /**
     * The meta-property for the `unadjustedEndDate` property.
     * @return the meta-property, not null
     */
    def unadjustedEndDate(): MetaProperty[LocalDate] = {
      unadjustedEndDateProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          bean.asInstanceOf[SchedulePeriod].getStartDate()
        case -1607727319 => // endDate
          bean.asInstanceOf[SchedulePeriod].getEndDate()
        case 1457691881 => // unadjustedStartDate
          bean.asInstanceOf[SchedulePeriod].getUnadjustedStartDate()
        case 31758114 => // unadjustedEndDate
          bean.asInstanceOf[SchedulePeriod].getUnadjustedEndDate()
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
   * The bean-builder for `SchedulePeriod`.
   */
  final class Builder private[schedule] () extends DirectFieldsBeanBuilder[SchedulePeriod] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private[schedule] var _startDate: LocalDate = uninitialized
    private[schedule] var _endDate: LocalDate = uninitialized
    private[schedule] var _unadjustedStartDate: LocalDate = uninitialized
    private[schedule] var _unadjustedEndDate: LocalDate = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[schedule] def this(beanToCopy: SchedulePeriod) = {
      this()
      this._startDate = beanToCopy.getStartDate()
      this._endDate = beanToCopy.getEndDate()
      this._unadjustedStartDate = beanToCopy.getUnadjustedStartDate()
      this._unadjustedEndDate = beanToCopy.getUnadjustedEndDate()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          _startDate
        case -1607727319 => // endDate
          _endDate
        case 1457691881 => // unadjustedStartDate
          _unadjustedStartDate
        case 31758114 => // unadjustedEndDate
          _unadjustedEndDate
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
        case 1457691881 => // unadjustedStartDate
          this._unadjustedStartDate = newValue.asInstanceOf[LocalDate]
        case 31758114 => // unadjustedEndDate
          this._unadjustedEndDate = newValue.asInstanceOf[LocalDate]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): SchedulePeriod = {
      preBuild(this)
      new SchedulePeriod(
          _startDate,
          _endDate,
          _unadjustedStartDate,
          _unadjustedEndDate)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date of this period, used for financial calculations such as interest accrual.
     *
     * The first date in the schedule period, typically treated as inclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    def startDate(startDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(startDate, "startDate")
      this._startDate = startDate
      this
    }

    /**
     * Sets the end date of this period, used for financial calculations such as interest accrual.
     *
     * The last date in the schedule period, typically treated as exclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    def endDate(endDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(endDate, "endDate")
      this._endDate = endDate
      this
    }

    /**
     * Sets the unadjusted start date.
     *
     * The start date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the start date.
     *
     * When building, this will default to the start date if not specified.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    def unadjustedStartDate(unadjustedStartDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate")
      this._unadjustedStartDate = unadjustedStartDate
      this
    }

    /**
     * Sets the unadjusted end date.
     *
     * The end date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the end date.
     *
     * When building, this will default to the end date if not specified.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    def unadjustedEndDate(unadjustedEndDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate")
      this._unadjustedEndDate = unadjustedEndDate
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(160)
      buf.append("SchedulePeriod.Builder{")
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(_startDate)).append(',').append(' ')
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(_endDate)).append(',').append(' ')
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(_unadjustedStartDate)).append(',').append(' ')
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(_unadjustedEndDate))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
