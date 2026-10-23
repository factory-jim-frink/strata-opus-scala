/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.io.Serializable
import java.time.LocalDate
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
import org.joda.beans.gen.ImmutableDefaults
import org.joda.beans.gen.ImmutablePreBuild
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.DayCount
import com.opengamma.strata.basics.date.HolidayCalendar
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.Tenor

/**
 * An overnight index, such as Sonia or Eonia.
 *
 * An index represented by this class relates to lending over one night.
 * The rate typically refers to "Today/Tomorrow" but might refer to "Tomorrow/Next".
 *
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class ImmutableOvernightIndex private (
    /**
     * The index name, such as 'GBP-SONIA'.
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
     * The calendar that the index uses.
     *
     * All dates are calculated with reference to the same calendar.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val fixingCalendar: HolidayCalendarId,
    /**
     * The number of days to add to the fixing date to obtain the publication date.
     *
     * In most cases, the fixing rate is available on the fixing date.
     * In a few cases, publication of the fixing rate is delayed until the following business day.
     * This property is zero if publication is on the fixing date, or one if it is the next day.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val publicationDateOffset: Int,
    /**
     * The number of days to add to the fixing date to obtain the effective date.
     *
     * In most cases, the settlement date and start of the implied deposit is on the fixing date.
     * In a few cases, the settlement date is the following business day.
     * This property is zero if settlement is on the fixing date, or one if it is the next day.
     * Maturity is always one business day after the settlement date.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val effectiveDateOffset: Int,
    /**
     * The day count convention.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val dayCount: DayCount,
    /**
     * The default day count convention for the associated fixed leg.
     *
     * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
     * The day count convention of the fixed leg often differs from that of the index,
     * and the default is value is available here.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val defaultFixedLegDayCount: DayCount)
  extends OvernightIndex with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(name, "name")
  JodaBeanUtils.notNull(currency, "currency")
  JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar")
  JodaBeanUtils.notNull(Integer.valueOf(publicationDateOffset), "publicationDateOffset")
  JodaBeanUtils.notNull(Integer.valueOf(effectiveDateOffset), "effectiveDateOffset")
  JodaBeanUtils.notNull(dayCount, "dayCount")
  JodaBeanUtils.notNull(defaultFixedLegDayCount, "defaultFixedLegDayCount")

  //-------------------------------------------------------------------------
  override def getTenor(): Tenor = {
    Tenor.TENOR_1D
  }

  override def getFloatingRateName(): FloatingRateName = {
    FloatingRateName.of(name)
  }

  //-------------------------------------------------------------------------
  override def calculatePublicationFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingCal: HolidayCalendar = fixingCalendar.resolve(refData)
    fixingCal.shift(fixingCal.nextOrSame(fixingDate), publicationDateOffset)
  }

  override def calculateEffectiveFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingCal: HolidayCalendar = fixingCalendar.resolve(refData)
    fixingCal.shift(fixingCal.nextOrSame(fixingDate), effectiveDateOffset)
  }

  override def calculateMaturityFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingCal: HolidayCalendar = fixingCalendar.resolve(refData)
    fixingCal.shift(fixingCal.nextOrSame(fixingDate), effectiveDateOffset + 1)
  }

  override def calculateFixingFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingCal: HolidayCalendar = fixingCalendar.resolve(refData)
    fixingCal.shift(fixingCal.nextOrSame(effectiveDate), -effectiveDateOffset)
  }

  override def calculateMaturityFromEffective(effectiveDate: LocalDate, refData: ReferenceData): LocalDate = {
    val fixingCal: HolidayCalendar = fixingCalendar.resolve(refData)
    fixingCal.shift(fixingCal.nextOrSame(effectiveDate), 1)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: ImmutableOvernightIndex => name.equals(other.name)
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
  override def metaBean(): ImmutableOvernightIndex.Meta = {
    ImmutableOvernightIndex.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index name, such as 'GBP-SONIA'.
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
   * Gets the calendar that the index uses.
   *
   * All dates are calculated with reference to the same calendar.
   * @return the value of the property, not null
   */
  override def getFixingCalendar(): HolidayCalendarId = {
    fixingCalendar
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the publication date.
   *
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   * @return the value of the property, not null
   */
  override def getPublicationDateOffset(): Int = {
    publicationDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the effective date.
   *
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   * @return the value of the property, not null
   */
  override def getEffectiveDateOffset(): Int = {
    effectiveDateOffset
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
  def toBuilder(): ImmutableOvernightIndex.Builder = {
    new ImmutableOvernightIndex.Builder(this)
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ImmutableOvernightIndex {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ImmutableOvernightIndex.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private def applyDefaults(builder: Builder): Unit = {
    builder._active = true
  }

  @ImmutablePreBuild
  private def preBuild(builder: Builder): Unit = {
    if (builder._defaultFixedLegDayCount == null) {
      builder._defaultFixedLegDayCount = builder._dayCount
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ImmutableOvernightIndex`.
   * @return the meta-bean, not null
   */
  def meta(): ImmutableOvernightIndex.Meta = {
    ImmutableOvernightIndex.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): ImmutableOvernightIndex.Builder = {
    new ImmutableOvernightIndex.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ImmutableOvernightIndex`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `name` property.
     */
    private val nameProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "name", classOf[ImmutableOvernightIndex], classOf[String])
    /**
     * The meta-property for the `currency` property.
     */
    private val currencyProperty: MetaProperty[Currency] = DirectMetaProperty.ofImmutable(
        this, "currency", classOf[ImmutableOvernightIndex], classOf[Currency])
    /**
     * The meta-property for the `active` property.
     */
    private val activeProperty: MetaProperty[java.lang.Boolean] = DirectMetaProperty.ofImmutable(
        this, "active", classOf[ImmutableOvernightIndex], java.lang.Boolean.TYPE)
    /**
     * The meta-property for the `fixingCalendar` property.
     */
    private val fixingCalendarProperty: MetaProperty[HolidayCalendarId] = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", classOf[ImmutableOvernightIndex], classOf[HolidayCalendarId])
    /**
     * The meta-property for the `publicationDateOffset` property.
     */
    private val publicationDateOffsetProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "publicationDateOffset", classOf[ImmutableOvernightIndex], Integer.TYPE)
    /**
     * The meta-property for the `effectiveDateOffset` property.
     */
    private val effectiveDateOffsetProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", classOf[ImmutableOvernightIndex], Integer.TYPE)
    /**
     * The meta-property for the `dayCount` property.
     */
    private val dayCountProperty: MetaProperty[DayCount] = DirectMetaProperty.ofImmutable(
        this, "dayCount", classOf[ImmutableOvernightIndex], classOf[DayCount])
    /**
     * The meta-property for the `defaultFixedLegDayCount` property.
     */
    private val defaultFixedLegDayCountProperty: MetaProperty[DayCount] = DirectMetaProperty.ofImmutable(
        this, "defaultFixedLegDayCount", classOf[ImmutableOvernightIndex], classOf[DayCount])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currency",
        "active",
        "fixingCalendar",
        "publicationDateOffset",
        "effectiveDateOffset",
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
        case 1901198637 => // publicationDateOffset
          publicationDateOffsetProperty
        case 1571923688 => // effectiveDateOffset
          effectiveDateOffsetProperty
        case 1905311443 => // dayCount
          dayCountProperty
        case -2037801138 => // defaultFixedLegDayCount
          defaultFixedLegDayCountProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ImmutableOvernightIndex.Builder = {
      new ImmutableOvernightIndex.Builder()
    }

    override def beanType(): Class[? <: ImmutableOvernightIndex] = {
      classOf[ImmutableOvernightIndex]
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
     * The meta-property for the `publicationDateOffset` property.
     * @return the meta-property, not null
     */
    def publicationDateOffset(): MetaProperty[Integer] = {
      publicationDateOffsetProperty
    }

    /**
     * The meta-property for the `effectiveDateOffset` property.
     * @return the meta-property, not null
     */
    def effectiveDateOffset(): MetaProperty[Integer] = {
      effectiveDateOffsetProperty
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
          bean.asInstanceOf[ImmutableOvernightIndex].getName()
        case 575402001 => // currency
          bean.asInstanceOf[ImmutableOvernightIndex].getCurrency()
        case -1422950650 => // active
          Boolean.box(bean.asInstanceOf[ImmutableOvernightIndex].isActive())
        case 394230283 => // fixingCalendar
          bean.asInstanceOf[ImmutableOvernightIndex].getFixingCalendar()
        case 1901198637 => // publicationDateOffset
          Integer.valueOf(bean.asInstanceOf[ImmutableOvernightIndex].getPublicationDateOffset())
        case 1571923688 => // effectiveDateOffset
          Integer.valueOf(bean.asInstanceOf[ImmutableOvernightIndex].getEffectiveDateOffset())
        case 1905311443 => // dayCount
          bean.asInstanceOf[ImmutableOvernightIndex].getDayCount()
        case -2037801138 => // defaultFixedLegDayCount
          bean.asInstanceOf[ImmutableOvernightIndex].getDefaultFixedLegDayCount()
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
   * The bean-builder for `ImmutableOvernightIndex`.
   */
  final class Builder private[index] () extends DirectFieldsBeanBuilder[ImmutableOvernightIndex] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _name: String = uninitialized
    private var _currency: Currency = uninitialized
    private[ImmutableOvernightIndex] var _active: Boolean = uninitialized
    private var _fixingCalendar: HolidayCalendarId = uninitialized
    private var _publicationDateOffset: Int = uninitialized
    private var _effectiveDateOffset: Int = uninitialized
    private[ImmutableOvernightIndex] var _dayCount: DayCount = uninitialized
    private[ImmutableOvernightIndex] var _defaultFixedLegDayCount: DayCount = uninitialized

    applyDefaults(this)

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[index] def this(beanToCopy: ImmutableOvernightIndex) = {
      this()
      this._name = beanToCopy.getName()
      this._currency = beanToCopy.getCurrency()
      this._active = beanToCopy.isActive()
      this._fixingCalendar = beanToCopy.getFixingCalendar()
      this._publicationDateOffset = beanToCopy.getPublicationDateOffset()
      this._effectiveDateOffset = beanToCopy.getEffectiveDateOffset()
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
        case 1901198637 => // publicationDateOffset
          Integer.valueOf(_publicationDateOffset)
        case 1571923688 => // effectiveDateOffset
          Integer.valueOf(_effectiveDateOffset)
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
        case 1901198637 => // publicationDateOffset
          this._publicationDateOffset = newValue.asInstanceOf[Integer].intValue()
        case 1571923688 => // effectiveDateOffset
          this._effectiveDateOffset = newValue.asInstanceOf[Integer].intValue()
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

    override def build(): ImmutableOvernightIndex = {
      preBuild(this)
      new ImmutableOvernightIndex(
          _name,
          _currency,
          _active,
          _fixingCalendar,
          _publicationDateOffset,
          _effectiveDateOffset,
          _dayCount,
          _defaultFixedLegDayCount)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index name, such as 'GBP-SONIA'.
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
     * Sets the calendar that the index uses.
     *
     * All dates are calculated with reference to the same calendar.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingCalendar(fixingCalendar: HolidayCalendarId): Builder = {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar")
      this._fixingCalendar = fixingCalendar
      this
    }

    /**
     * Sets the number of days to add to the fixing date to obtain the publication date.
     *
     * In most cases, the fixing rate is available on the fixing date.
     * In a few cases, publication of the fixing rate is delayed until the following business day.
     * This property is zero if publication is on the fixing date, or one if it is the next day.
     * @param publicationDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def publicationDateOffset(publicationDateOffset: Int): Builder = {
      JodaBeanUtils.notNull(Integer.valueOf(publicationDateOffset), "publicationDateOffset")
      this._publicationDateOffset = publicationDateOffset
      this
    }

    /**
     * Sets the number of days to add to the fixing date to obtain the effective date.
     *
     * In most cases, the settlement date and start of the implied deposit is on the fixing date.
     * In a few cases, the settlement date is the following business day.
     * This property is zero if settlement is on the fixing date, or one if it is the next day.
     * Maturity is always one business day after the settlement date.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def effectiveDateOffset(effectiveDateOffset: Int): Builder = {
      JodaBeanUtils.notNull(Integer.valueOf(effectiveDateOffset), "effectiveDateOffset")
      this._effectiveDateOffset = effectiveDateOffset
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
      val buf = new java.lang.StringBuilder(288)
      buf.append("ImmutableOvernightIndex.Builder{")
      buf.append("name").append('=').append(JodaBeanUtils.toString(_name)).append(',').append(' ')
      buf.append("currency").append('=').append(JodaBeanUtils.toString(_currency)).append(',').append(' ')
      buf.append("active").append('=').append(JodaBeanUtils.toString(Boolean.box(_active))).append(',').append(' ')
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(_fixingCalendar)).append(',').append(' ')
      buf.append("publicationDateOffset").append('=').append(JodaBeanUtils.toString(Integer.valueOf(_publicationDateOffset))).append(',').append(' ')
      buf.append("effectiveDateOffset").append('=').append(JodaBeanUtils.toString(Integer.valueOf(_effectiveDateOffset))).append(',').append(' ')
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(_dayCount)).append(',').append(' ')
      buf.append("defaultFixedLegDayCount").append('=').append(JodaBeanUtils.toString(_defaultFixedLegDayCount))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
