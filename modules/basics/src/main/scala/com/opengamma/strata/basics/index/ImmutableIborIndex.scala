/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Map
import java.util.NoSuchElementException
import java.util.function.Function

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutableConstructor
import org.joda.beans.gen.ImmutableDefaults
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.google.common.base.MoreObjects
import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.DateAdjuster
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.HolidayCalendar
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.HolidayCalendarIds
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.basics.date.TenorAdjustment
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * An Ibor index implementation based on an immutable set of rules.
 *
 * A standard immutable implementation of `IborIndex` that defines the currency
 * and the rules for converting from fixing to effective and maturity.
 *
 * In most cases, applications should refer to indices by name, using `IborIndex#of(String)`.
 * The named index will typically be resolved to an instance of this class.
 * As such, it is recommended to use the `IborIndex` interface in application
 * code rather than directly referring to this class.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class ImmutableIborIndex @ImmutableConstructor private (
    /**
     * The index name, such as 'GBP-LIBOR-3M'.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val name: String,
    /**
     * The currency of the index.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val currency: Currency,
    /**
     * Whether the index is active, defaulted to true.
     *
     * Over time some indices become inactive and are no longer produced.
     * If this occurs, this flag will be set to false.
     */
    @(PropertyDefinition @field)(overrideGet = true)
    private val active: Boolean,
    /**
     * The calendar that determines which dates are fixing dates.
     *
     * The fixing date is when the rate is determined.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val fixingCalendar: HolidayCalendarId,
    /**
     * The fixing time.
     *
     * The rate is fixed at the fixing time of the fixing date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val fixingTime: LocalTime,
    /**
    * The fixing time-zone.
    *
    * The time-zone of the fixing time.
    */
    @(PropertyDefinition @field)(validate = "notNull")
    private val fixingZone: ZoneId,
    /**
     * The adjustment applied to the effective date to obtain the fixing date.
     *
     * The fixing date is the date on which the index is to be observed.
     * In most cases, the fixing date is 0 or 2 days before the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val fixingDateOffset: DaysAdjustment,
    /**
     * The adjustment applied to the fixing date to obtain the effective date.
     *
     * The effective date is the start date of the indexed deposit.
     * In most cases, the effective date is 0 or 2 days after the fixing date.
     * This data structure allows the complex rules of some indices to be represented.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val effectiveDateOffset: DaysAdjustment,
    /**
     * The adjustment applied to the effective date to obtain the maturity date.
     *
     * The maturity date is the end date of the indexed deposit and is relative to the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val maturityDateOffset: TenorAdjustment,
    /**
     * The day count convention.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val dayCount: DayCount,
    rawDefaultFixedLegDayCount: DayCount)
  extends IborIndex with ImmutableBean with Serializable {

  // creates an instance
  ArgChecker.notNull(name, "name")
  ArgChecker.notNull(currency, "currency")
  ArgChecker.notNull(fixingCalendar, "fixingCalendar")
  ArgChecker.notNull(fixingTime, "fixingTime")
  ArgChecker.notNull(fixingZone, "fixingZone")
  ArgChecker.notNull(fixingDateOffset, "fixingDateOffset")
  ArgChecker.notNull(effectiveDateOffset, "effectiveDateOffset")
  ArgChecker.notNull(maturityDateOffset, "maturityDateOffset")
  ArgChecker.notNull(dayCount, "dayCount")
  /**
   * The default day count convention for the associated fixed leg.
   *
   * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
   * The day count convention of the fixed leg often differs from that of the index,
   * and the default is value is available here.
   */
  @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
  private val defaultFixedLegDayCount: DayCount = MoreObjects.firstNonNull(rawDefaultFixedLegDayCount, dayCount)
  /**
   * The floating rate name, such as 'GBP-LIBOR'.
   */
  @transient
  private val floatingRateName: String = {  // derived
    // derive from name, but don't store FloatingRateName, to avoid directly linking data at this point
    val suffix = "-" + maturityDateOffset.getTenor().toString()
    if (!name.endsWith(suffix)) {
      throw new IllegalArgumentException(Messages.format(
          "IborIndex name '{}' must end with tenor '{}'", name, maturityDateOffset.getTenor().toString()))
    }
    name.substring(0, name.length() - suffix.length())
  }

  // ensure standard constructor is invoked
  private def readResolve(): Object = {
    new ImmutableIborIndex(
        name,
        currency,
        active,
        fixingCalendar,
        fixingTime,
        fixingZone,
        fixingDateOffset,
        effectiveDateOffset,
        maturityDateOffset,
        dayCount,
        defaultFixedLegDayCount)
  }

  //-------------------------------------------------------------------------
  override def getTenor(): Tenor = {
    maturityDateOffset.getTenor()
  }

  override def getFloatingRateName(): FloatingRateName = {
    FloatingRateName.of(floatingRateName)
  }

  //-------------------------------------------------------------------------
  override def calculateFixingDateTime(fixingDate: LocalDate): ZonedDateTime = {
    fixingDate.atTime(fixingTime).atZone(fixingZone)
  }

  override def calculateEffectiveFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingBusinessDay = fixingCalendar.resolve(refData).nextOrSame(fixingDate)
    effectiveDateOffset.adjust(fixingBusinessDay, refData)
  }

  override def calculateMaturityFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingBusinessDay = fixingCalendar.resolve(refData).nextOrSame(fixingDate)
    maturityDateOffset.adjust(effectiveDateOffset.adjust(fixingBusinessDay, refData), refData)
  }

  override def calculateFixingFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate = {
    val effectiveBusinessDay = effectiveDateCalendar(refData).nextOrSame(effectiveDate)
    fixingDateOffset.adjust(effectiveBusinessDay, refData)
  }

  override def calculateMaturityFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate = {
    val effectiveBusinessDay = effectiveDateCalendar(refData).nextOrSame(effectiveDate)
    maturityDateOffset.adjust(effectiveBusinessDay, refData)
  }

  // finds the calendar of the effective date
  private def effectiveDateCalendar(refData: ReferenceData): HolidayCalendar = {
    var cal = effectiveDateOffset.getResultCalendar()
    if (cal eq HolidayCalendarIds.NO_HOLIDAYS) {
      cal = fixingCalendar
    }
    cal.resolve(refData)
  }

  override def resolve(refData: ReferenceData): Function[LocalDate, IborIndexObservation] = {
    val fixingCal = fixingCalendar.resolve(refData)
    val effectiveAdjuster = effectiveDateOffset.resolve(refData)
    val maturityAdjuster = maturityDateOffset.resolve(refData)
    fixingDate => create(fixingDate, fixingCal, effectiveAdjuster, maturityAdjuster)
  }

  // creates an observation
  private def create(
      fixingDate: LocalDate,
      fixingCal: HolidayCalendar,
      effectiveAdjuster: DateAdjuster,
      maturityAdjuster: DateAdjuster): IborIndexObservation = {

    val fixingBusinessDay = fixingCal.nextOrSame(fixingDate)
    val effectiveDate = effectiveAdjuster.adjust(fixingBusinessDay)
    val maturityDate = maturityAdjuster.adjust(effectiveDate)
    val yearFraction = dayCount.yearFraction(effectiveDate, maturityDate)
    new IborIndexObservation(this, fixingDate, effectiveDate, maturityDate, yearFraction)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: ImmutableIborIndex => name.equals(other.name)
        case _ => false
      }
    }
  }

  override def hashCode(): Int = {
    name.hashCode()
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  override def toString(): String = {
    getName()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): ImmutableIborIndex.Meta = {
    ImmutableIborIndex.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index name, such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  override def getName(): String = {
    name
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the index.
   * @return the value of the property, not null
   */
  override def getCurrency(): Currency = {
    currency
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the index is active, defaulted to true.
   *
   * Over time some indices become inactive and are no longer produced.
   * If this occurs, this flag will be set to false.
   * @return the value of the property
   */
  override def isActive(): Boolean = {
    active
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar that determines which dates are fixing dates.
   *
   * The fixing date is when the rate is determined.
   * @return the value of the property, not null
   */
  override def getFixingCalendar(): HolidayCalendarId = {
    fixingCalendar
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing time.
   *
   * The rate is fixed at the fixing time of the fixing date.
   * @return the value of the property, not null
   */
  def getFixingTime(): LocalTime = {
    fixingTime
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing time-zone.
   *
   * The time-zone of the fixing time.
   * @return the value of the property, not null
   */
  def getFixingZone(): ZoneId = {
    fixingZone
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the fixing date.
   *
   * The fixing date is the date on which the index is to be observed.
   * In most cases, the fixing date is 0 or 2 days before the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  override def getFixingDateOffset(): DaysAdjustment = {
    fixingDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the fixing date to obtain the effective date.
   *
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  override def getEffectiveDateOffset(): DaysAdjustment = {
    effectiveDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the maturity date.
   *
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * @return the value of the property, not null
   */
  override def getMaturityDateOffset(): TenorAdjustment = {
    maturityDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * @return the value of the property, not null
   */
  override def getDayCount(): DayCount = {
    dayCount
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the default day count convention for the associated fixed leg.
   *
   * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
   * The day count convention of the fixed leg often differs from that of the index,
   * and the default is value is available here.
   * @return the value of the property, not null
   */
  override def getDefaultFixedLegDayCount(): DayCount = {
    defaultFixedLegDayCount
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): ImmutableIborIndex.Builder = {
    new ImmutableIborIndex.Builder(this)
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ImmutableIborIndex {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ImmutableIborIndex.Meta.INSTANCE)
    true
  }

  @ImmutableDefaults
  private def applyDefaults(builder: Builder): Unit = {
    builder._active = true
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ImmutableIborIndex`.
   * @return the meta-bean, not null
   */
  def meta(): ImmutableIborIndex.Meta = {
    ImmutableIborIndex.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): ImmutableIborIndex.Builder = {
    new ImmutableIborIndex.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ImmutableIborIndex`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `name` property.
     */
    private val nameProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "name", classOf[ImmutableIborIndex], classOf[String])
    /**
     * The meta-property for the `currency` property.
     */
    private val currencyProperty: MetaProperty[Currency] = DirectMetaProperty.ofImmutable(
        this, "currency", classOf[ImmutableIborIndex], classOf[Currency])
    /**
     * The meta-property for the `active` property.
     */
    private val activeProperty: MetaProperty[java.lang.Boolean] = DirectMetaProperty.ofImmutable(
        this, "active", classOf[ImmutableIborIndex], java.lang.Boolean.TYPE)
    /**
     * The meta-property for the `fixingCalendar` property.
     */
    private val fixingCalendarProperty: MetaProperty[HolidayCalendarId] = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", classOf[ImmutableIborIndex], classOf[HolidayCalendarId])
    /**
     * The meta-property for the `fixingTime` property.
     */
    private val fixingTimeProperty: MetaProperty[LocalTime] = DirectMetaProperty.ofImmutable(
        this, "fixingTime", classOf[ImmutableIborIndex], classOf[LocalTime])
    /**
     * The meta-property for the `fixingZone` property.
     */
    private val fixingZoneProperty: MetaProperty[ZoneId] = DirectMetaProperty.ofImmutable(
        this, "fixingZone", classOf[ImmutableIborIndex], classOf[ZoneId])
    /**
     * The meta-property for the `fixingDateOffset` property.
     */
    private val fixingDateOffsetProperty: MetaProperty[DaysAdjustment] = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", classOf[ImmutableIborIndex], classOf[DaysAdjustment])
    /**
     * The meta-property for the `effectiveDateOffset` property.
     */
    private val effectiveDateOffsetProperty: MetaProperty[DaysAdjustment] = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", classOf[ImmutableIborIndex], classOf[DaysAdjustment])
    /**
     * The meta-property for the `maturityDateOffset` property.
     */
    private val maturityDateOffsetProperty: MetaProperty[TenorAdjustment] = DirectMetaProperty.ofImmutable(
        this, "maturityDateOffset", classOf[ImmutableIborIndex], classOf[TenorAdjustment])
    /**
     * The meta-property for the `dayCount` property.
     */
    private val dayCountProperty: MetaProperty[DayCount] = DirectMetaProperty.ofImmutable(
        this, "dayCount", classOf[ImmutableIborIndex], classOf[DayCount])
    /**
     * The meta-property for the `defaultFixedLegDayCount` property.
     */
    private val defaultFixedLegDayCountProperty: MetaProperty[DayCount] = DirectMetaProperty.ofImmutable(
        this, "defaultFixedLegDayCount", classOf[ImmutableIborIndex], classOf[DayCount])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currency",
        "active",
        "fixingCalendar",
        "fixingTime",
        "fixingZone",
        "fixingDateOffset",
        "effectiveDateOffset",
        "maturityDateOffset",
        "dayCount",
        "defaultFixedLegDayCount")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 3373707 => // name
          nameProperty
        case 575402001 => // currency
          currencyProperty
        case -1422950650 => // active
          activeProperty
        case 394230283 => // fixingCalendar
          fixingCalendarProperty
        case 1255686170 => // fixingTime
          fixingTimeProperty
        case 1255870713 => // fixingZone
          fixingZoneProperty
        case 873743726 => // fixingDateOffset
          fixingDateOffsetProperty
        case 1571923688 => // effectiveDateOffset
          effectiveDateOffsetProperty
        case 1574797394 => // maturityDateOffset
          maturityDateOffsetProperty
        case 1905311443 => // dayCount
          dayCountProperty
        case -2037801138 => // defaultFixedLegDayCount
          defaultFixedLegDayCountProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ImmutableIborIndex.Builder = {
      new ImmutableIborIndex.Builder()
    }

    override def beanType(): Class[? <: ImmutableIborIndex] = {
      classOf[ImmutableIborIndex]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `name` property.
     * @return the meta-property, not null
     */
    def name(): MetaProperty[String] = {
      nameProperty
    }

    /**
     * The meta-property for the `currency` property.
     * @return the meta-property, not null
     */
    def currency(): MetaProperty[Currency] = {
      currencyProperty
    }

    /**
     * The meta-property for the `active` property.
     * @return the meta-property, not null
     */
    def active(): MetaProperty[java.lang.Boolean] = {
      activeProperty
    }

    /**
     * The meta-property for the `fixingCalendar` property.
     * @return the meta-property, not null
     */
    def fixingCalendar(): MetaProperty[HolidayCalendarId] = {
      fixingCalendarProperty
    }

    /**
     * The meta-property for the `fixingTime` property.
     * @return the meta-property, not null
     */
    def fixingTime(): MetaProperty[LocalTime] = {
      fixingTimeProperty
    }

    /**
     * The meta-property for the `fixingZone` property.
     * @return the meta-property, not null
     */
    def fixingZone(): MetaProperty[ZoneId] = {
      fixingZoneProperty
    }

    /**
     * The meta-property for the `fixingDateOffset` property.
     * @return the meta-property, not null
     */
    def fixingDateOffset(): MetaProperty[DaysAdjustment] = {
      fixingDateOffsetProperty
    }

    /**
     * The meta-property for the `effectiveDateOffset` property.
     * @return the meta-property, not null
     */
    def effectiveDateOffset(): MetaProperty[DaysAdjustment] = {
      effectiveDateOffsetProperty
    }

    /**
     * The meta-property for the `maturityDateOffset` property.
     * @return the meta-property, not null
     */
    def maturityDateOffset(): MetaProperty[TenorAdjustment] = {
      maturityDateOffsetProperty
    }

    /**
     * The meta-property for the `dayCount` property.
     * @return the meta-property, not null
     */
    def dayCount(): MetaProperty[DayCount] = {
      dayCountProperty
    }

    /**
     * The meta-property for the `defaultFixedLegDayCount` property.
     * @return the meta-property, not null
     */
    def defaultFixedLegDayCount(): MetaProperty[DayCount] = {
      defaultFixedLegDayCountProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 3373707 => // name
          bean.asInstanceOf[ImmutableIborIndex].getName()
        case 575402001 => // currency
          bean.asInstanceOf[ImmutableIborIndex].getCurrency()
        case -1422950650 => // active
          Boolean.box(bean.asInstanceOf[ImmutableIborIndex].isActive())
        case 394230283 => // fixingCalendar
          bean.asInstanceOf[ImmutableIborIndex].getFixingCalendar()
        case 1255686170 => // fixingTime
          bean.asInstanceOf[ImmutableIborIndex].getFixingTime()
        case 1255870713 => // fixingZone
          bean.asInstanceOf[ImmutableIborIndex].getFixingZone()
        case 873743726 => // fixingDateOffset
          bean.asInstanceOf[ImmutableIborIndex].getFixingDateOffset()
        case 1571923688 => // effectiveDateOffset
          bean.asInstanceOf[ImmutableIborIndex].getEffectiveDateOffset()
        case 1574797394 => // maturityDateOffset
          bean.asInstanceOf[ImmutableIborIndex].getMaturityDateOffset()
        case 1905311443 => // dayCount
          bean.asInstanceOf[ImmutableIborIndex].getDayCount()
        case -2037801138 => // defaultFixedLegDayCount
          bean.asInstanceOf[ImmutableIborIndex].getDefaultFixedLegDayCount()
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
    private[index] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `ImmutableIborIndex`.
   */
  final class Builder private[index] () extends DirectFieldsBeanBuilder[ImmutableIborIndex] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _name: String = uninitialized
    private var _currency: Currency = uninitialized
    private[ImmutableIborIndex] var _active: Boolean = uninitialized
    private var _fixingCalendar: HolidayCalendarId = uninitialized
    private var _fixingTime: LocalTime = uninitialized
    private var _fixingZone: ZoneId = uninitialized
    private var _fixingDateOffset: DaysAdjustment = uninitialized
    private var _effectiveDateOffset: DaysAdjustment = uninitialized
    private var _maturityDateOffset: TenorAdjustment = uninitialized
    private var _dayCount: DayCount = uninitialized
    private var _defaultFixedLegDayCount: DayCount = uninitialized

    applyDefaults(this)

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[index] def this(beanToCopy: ImmutableIborIndex) = {
      this()
      this._name = beanToCopy.getName()
      this._currency = beanToCopy.getCurrency()
      this._active = beanToCopy.isActive()
      this._fixingCalendar = beanToCopy.getFixingCalendar()
      this._fixingTime = beanToCopy.getFixingTime()
      this._fixingZone = beanToCopy.getFixingZone()
      this._fixingDateOffset = beanToCopy.getFixingDateOffset()
      this._effectiveDateOffset = beanToCopy.getEffectiveDateOffset()
      this._maturityDateOffset = beanToCopy.getMaturityDateOffset()
      this._dayCount = beanToCopy.getDayCount()
      this._defaultFixedLegDayCount = beanToCopy.getDefaultFixedLegDayCount()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 3373707 => // name
          _name
        case 575402001 => // currency
          _currency
        case -1422950650 => // active
          Boolean.box(_active)
        case 394230283 => // fixingCalendar
          _fixingCalendar
        case 1255686170 => // fixingTime
          _fixingTime
        case 1255870713 => // fixingZone
          _fixingZone
        case 873743726 => // fixingDateOffset
          _fixingDateOffset
        case 1571923688 => // effectiveDateOffset
          _effectiveDateOffset
        case 1574797394 => // maturityDateOffset
          _maturityDateOffset
        case 1905311443 => // dayCount
          _dayCount
        case -2037801138 => // defaultFixedLegDayCount
          _defaultFixedLegDayCount
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 3373707 => // name
          this._name = newValue.asInstanceOf[String]
        case 575402001 => // currency
          this._currency = newValue.asInstanceOf[Currency]
        case -1422950650 => // active
          this._active = newValue.asInstanceOf[java.lang.Boolean].booleanValue()
        case 394230283 => // fixingCalendar
          this._fixingCalendar = newValue.asInstanceOf[HolidayCalendarId]
        case 1255686170 => // fixingTime
          this._fixingTime = newValue.asInstanceOf[LocalTime]
        case 1255870713 => // fixingZone
          this._fixingZone = newValue.asInstanceOf[ZoneId]
        case 873743726 => // fixingDateOffset
          this._fixingDateOffset = newValue.asInstanceOf[DaysAdjustment]
        case 1571923688 => // effectiveDateOffset
          this._effectiveDateOffset = newValue.asInstanceOf[DaysAdjustment]
        case 1574797394 => // maturityDateOffset
          this._maturityDateOffset = newValue.asInstanceOf[TenorAdjustment]
        case 1905311443 => // dayCount
          this._dayCount = newValue.asInstanceOf[DayCount]
        case -2037801138 => // defaultFixedLegDayCount
          this._defaultFixedLegDayCount = newValue.asInstanceOf[DayCount]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): ImmutableIborIndex = {
      new ImmutableIborIndex(
          _name,
          _currency,
          _active,
          _fixingCalendar,
          _fixingTime,
          _fixingZone,
          _fixingDateOffset,
          _effectiveDateOffset,
          _maturityDateOffset,
          _dayCount,
          _defaultFixedLegDayCount)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index name, such as 'GBP-LIBOR-3M'.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    def name(name: String): Builder = {
      JodaBeanUtils.notNull(name, "name")
      this._name = name
      this
    }

    /**
     * Sets the currency of the index.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    def currency(currency: Currency): Builder = {
      JodaBeanUtils.notNull(currency, "currency")
      this._currency = currency
      this
    }

    /**
     * Sets whether the index is active, defaulted to true.
     *
     * Over time some indices become inactive and are no longer produced.
     * If this occurs, this flag will be set to false.
     * @param active  the new value
     * @return this, for chaining, not null
     */
    def active(active: Boolean): Builder = {
      this._active = active
      this
    }

    /**
     * Sets the calendar that determines which dates are fixing dates.
     *
     * The fixing date is when the rate is determined.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingCalendar(fixingCalendar: HolidayCalendarId): Builder = {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar")
      this._fixingCalendar = fixingCalendar
      this
    }

    /**
     * Sets the fixing time.
     *
     * The rate is fixed at the fixing time of the fixing date.
     * @param fixingTime  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingTime(fixingTime: LocalTime): Builder = {
      JodaBeanUtils.notNull(fixingTime, "fixingTime")
      this._fixingTime = fixingTime
      this
    }

    /**
     * Sets the fixing time-zone.
     *
     * The time-zone of the fixing time.
     * @param fixingZone  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingZone(fixingZone: ZoneId): Builder = {
      JodaBeanUtils.notNull(fixingZone, "fixingZone")
      this._fixingZone = fixingZone
      this
    }

    /**
     * Sets the adjustment applied to the effective date to obtain the fixing date.
     *
     * The fixing date is the date on which the index is to be observed.
     * In most cases, the fixing date is 0 or 2 days before the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingDateOffset(fixingDateOffset: DaysAdjustment): Builder = {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset")
      this._fixingDateOffset = fixingDateOffset
      this
    }

    /**
     * Sets the adjustment applied to the fixing date to obtain the effective date.
     *
     * The effective date is the start date of the indexed deposit.
     * In most cases, the effective date is 0 or 2 days after the fixing date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def effectiveDateOffset(effectiveDateOffset: DaysAdjustment): Builder = {
      JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset")
      this._effectiveDateOffset = effectiveDateOffset
      this
    }

    /**
     * Sets the adjustment applied to the effective date to obtain the maturity date.
     *
     * The maturity date is the end date of the indexed deposit and is relative to the effective date.
     * This data structure allows the complex rules of some indices to be represented.
     * @param maturityDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def maturityDateOffset(maturityDateOffset: TenorAdjustment): Builder = {
      JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset")
      this._maturityDateOffset = maturityDateOffset
      this
    }

    /**
     * Sets the day count convention.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    def dayCount(dayCount: DayCount): Builder = {
      JodaBeanUtils.notNull(dayCount, "dayCount")
      this._dayCount = dayCount
      this
    }

    /**
     * Sets the default day count convention for the associated fixed leg.
     *
     * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
     * The day count convention of the fixed leg often differs from that of the index,
     * and the default is value is available here.
     * @param defaultFixedLegDayCount  the new value, not null
     * @return this, for chaining, not null
     */
    def defaultFixedLegDayCount(defaultFixedLegDayCount: DayCount): Builder = {
      JodaBeanUtils.notNull(defaultFixedLegDayCount, "defaultFixedLegDayCount")
      this._defaultFixedLegDayCount = defaultFixedLegDayCount
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(384)
      buf.append("ImmutableIborIndex.Builder{")
      buf.append("name").append('=').append(JodaBeanUtils.toString(_name)).append(',').append(' ')
      buf.append("currency").append('=').append(JodaBeanUtils.toString(_currency)).append(',').append(' ')
      buf.append("active").append('=').append(JodaBeanUtils.toString(Boolean.box(_active))).append(',').append(' ')
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(_fixingCalendar)).append(',').append(' ')
      buf.append("fixingTime").append('=').append(JodaBeanUtils.toString(_fixingTime)).append(',').append(' ')
      buf.append("fixingZone").append('=').append(JodaBeanUtils.toString(_fixingZone)).append(',').append(' ')
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(_fixingDateOffset)).append(',').append(' ')
      buf.append("effectiveDateOffset").append('=').append(JodaBeanUtils.toString(_effectiveDateOffset)).append(',').append(' ')
      buf.append("maturityDateOffset").append('=').append(JodaBeanUtils.toString(_maturityDateOffset)).append(',').append(' ')
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(_dayCount)).append(',').append(' ')
      buf.append("defaultFixedLegDayCount").append('=').append(JodaBeanUtils.toString(_defaultFixedLegDayCount))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
