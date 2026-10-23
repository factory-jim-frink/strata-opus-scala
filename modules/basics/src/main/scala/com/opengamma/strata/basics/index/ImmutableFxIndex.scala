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
import org.joda.beans.gen.ImmutablePreBuild
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.currency.CurrencyPair
import com.opengamma.strata.basics.date.BusinessDayAdjustment
import com.opengamma.strata.basics.date.BusinessDayConventions
import com.opengamma.strata.basics.date.DateAdjuster
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.HolidayCalendar
import com.opengamma.strata.basics.date.HolidayCalendarId
import com.opengamma.strata.basics.date.HolidayCalendarIds

/**
 * A foreign exchange index implementation based on an immutable set of rules.
 *
 * A standard immutable implementation of `FxIndex` that defines the currency pair
 * and the rule for converting from fixing to maturity.
 *
 * In most cases, applications should refer to indices by name, using `FxIndex#of(String)`.
 * The named index will typically be resolved to an instance of this class.
 * As such, it is recommended to use the `FxIndex` interface in application
 * code rather than directly referring to this class.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class ImmutableFxIndex private (
    /**
     * The index name, such as 'EUR/GBP-ECB'.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val name: String,
    /**
     * The currency pair.
     *
     * An index defines an FX rate in a single direction, such as from EUR to USD.
     * This currency pair defines that direction.
     *
     * In most cases, the same index can be used to convert in both directions
     * by taking the rate or the reciprocal as necessary.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val currencyPair: CurrencyPair,
    /**
     * The calendar that determines which dates are fixing dates.
     *
     * The fixing date is when the rate is determined.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val fixingCalendar: HolidayCalendarId,
    /**
     * The adjustment applied to the maturity date to obtain the fixing date.
     *
     * The maturity date is the start date of the indexed deposit.
     * In most cases, the fixing date is 2 days before the maturity date.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val fixingDateOffset: DaysAdjustment,
    /**
     * The adjustment applied to the fixing date to obtain the maturity date.
     *
     * The maturity date is the start date of the indexed deposit.
     * In most cases, the maturity date is 2 days after the fixing date.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val maturityDateOffset: DaysAdjustment)
  extends FxIndex with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(name, "name")
  JodaBeanUtils.notNull(currencyPair, "currencyPair")
  JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar")
  JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset")
  JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset")

  //-------------------------------------------------------------------------
  override def calculateMaturityFromFixing(fixingDate: LocalDate, refData: ReferenceData): LocalDate = {
    // handle case where the input date is not a valid fixing date
    val fixingCal = fixingCalendar.resolve(refData)
    val fixingBusinessDay = fixingCal.nextOrSame(fixingDate)
    // find the maturity date using the offset and calendar in DaysAdjustment
    maturityDateOffset.adjust(fixingBusinessDay, refData)
  }

  override def calculateFixingFromMaturity(maturityDate: LocalDate, refData: ReferenceData): LocalDate = {
    // handle case where the input date is not a valid maturity date
    val maturityBusinessDay = maturityDateCalendar().resolve(refData).nextOrSame(maturityDate)
    // find the fixing date iteratively
    val fixingCal = fixingCalendar.resolve(refData)
    val maturityFromFixing = maturityDateOffset.resolve(refData)
    var fixingDate = maturityBusinessDay
    while (fixingCal.isHoliday(fixingDate) || maturityFromFixing.adjust(fixingDate).isAfter(maturityBusinessDay)) {
      fixingDate = fixingDate.minusDays(1)
    }
    fixingDate
  }

  // finds the calendar of the maturity date
  private def maturityDateCalendar(): HolidayCalendarId = {
    val cal = maturityDateOffset.getResultCalendar()
    if (cal eq HolidayCalendarIds.NO_HOLIDAYS) fixingCalendar else cal
  }

  override def resolve(refData: ReferenceData): Function[LocalDate, FxIndexObservation] = {
    val fixingCal = fixingCalendar.resolve(refData)
    val maturityAdj = maturityDateOffset.resolve(refData)
    val fn: Function[LocalDate, FxIndexObservation] = fixingDate => create(fixingDate, fixingCal, maturityAdj)
    fn
  }

  // creates an observation
  private def create(
      fixingDate: LocalDate,
      fixingCal: HolidayCalendar,
      maturityAdjuster: DateAdjuster): FxIndexObservation = {

    val fixingBusinessDay = fixingCal.nextOrSame(fixingDate)
    val maturityDate = maturityAdjuster.adjust(fixingBusinessDay)
    new FxIndexObservation(this, fixingDate, maturityDate)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: ImmutableFxIndex => name.equals(other.name)
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
  override def metaBean(): ImmutableFxIndex.Meta = {
    ImmutableFxIndex.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index name, such as 'EUR/GBP-ECB'.
   * @return the value of the property, not null
   */
  override def getName(): String = {
    name
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair.
   *
   * An index defines an FX rate in a single direction, such as from EUR to USD.
   * This currency pair defines that direction.
   *
   * In most cases, the same index can be used to convert in both directions
   * by taking the rate or the reciprocal as necessary.
   * @return the value of the property, not null
   */
  override def getCurrencyPair(): CurrencyPair = {
    currencyPair
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
   * Gets the adjustment applied to the maturity date to obtain the fixing date.
   *
   * The maturity date is the start date of the indexed deposit.
   * In most cases, the fixing date is 2 days before the maturity date.
   * @return the value of the property, not null
   */
  override def getFixingDateOffset(): DaysAdjustment = {
    fixingDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the fixing date to obtain the maturity date.
   *
   * The maturity date is the start date of the indexed deposit.
   * In most cases, the maturity date is 2 days after the fixing date.
   * @return the value of the property, not null
   */
  override def getMaturityDateOffset(): DaysAdjustment = {
    maturityDateOffset
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): ImmutableFxIndex.Builder = {
    new ImmutableFxIndex.Builder(this)
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ImmutableFxIndex {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ImmutableFxIndex.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private def preBuild(builder: Builder): Unit = {
    if (builder._fixingDateOffset == null && builder._fixingCalendar != null && builder._maturityDateOffset != null) {
      val days = builder._maturityDateOffset.getDays()
      val maturityCal = builder._maturityDateOffset.getCalendar()
      if (maturityCal.combinedWith(builder._fixingCalendar).equals(maturityCal)) {
        builder._fixingDateOffset = DaysAdjustment.ofBusinessDays(-days, maturityCal)
      } else {
        builder._fixingDateOffset = DaysAdjustment.ofBusinessDays(
            -days, maturityCal, BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, builder._fixingCalendar))
      }
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ImmutableFxIndex`.
   * @return the meta-bean, not null
   */
  def meta(): ImmutableFxIndex.Meta = {
    ImmutableFxIndex.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): ImmutableFxIndex.Builder = {
    new ImmutableFxIndex.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ImmutableFxIndex`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `name` property.
     */
    private val nameProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "name", classOf[ImmutableFxIndex], classOf[String])
    /**
     * The meta-property for the `currencyPair` property.
     */
    private val currencyPairProperty: MetaProperty[CurrencyPair] = DirectMetaProperty.ofImmutable(
        this, "currencyPair", classOf[ImmutableFxIndex], classOf[CurrencyPair])
    /**
     * The meta-property for the `fixingCalendar` property.
     */
    private val fixingCalendarProperty: MetaProperty[HolidayCalendarId] = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", classOf[ImmutableFxIndex], classOf[HolidayCalendarId])
    /**
     * The meta-property for the `fixingDateOffset` property.
     */
    private val fixingDateOffsetProperty: MetaProperty[DaysAdjustment] = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", classOf[ImmutableFxIndex], classOf[DaysAdjustment])
    /**
     * The meta-property for the `maturityDateOffset` property.
     */
    private val maturityDateOffsetProperty: MetaProperty[DaysAdjustment] = DirectMetaProperty.ofImmutable(
        this, "maturityDateOffset", classOf[ImmutableFxIndex], classOf[DaysAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currencyPair",
        "fixingCalendar",
        "fixingDateOffset",
        "maturityDateOffset")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 3373707 => // name
          nameProperty
        case 1005147787 => // currencyPair
          currencyPairProperty
        case 394230283 => // fixingCalendar
          fixingCalendarProperty
        case 873743726 => // fixingDateOffset
          fixingDateOffsetProperty
        case 1574797394 => // maturityDateOffset
          maturityDateOffsetProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ImmutableFxIndex.Builder = {
      new ImmutableFxIndex.Builder()
    }

    override def beanType(): Class[? <: ImmutableFxIndex] = {
      classOf[ImmutableFxIndex]
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
     * The meta-property for the `currencyPair` property.
     * @return the meta-property, not null
     */
    def currencyPair(): MetaProperty[CurrencyPair] = {
      currencyPairProperty
    }

    /**
     * The meta-property for the `fixingCalendar` property.
     * @return the meta-property, not null
     */
    def fixingCalendar(): MetaProperty[HolidayCalendarId] = {
      fixingCalendarProperty
    }

    /**
     * The meta-property for the `fixingDateOffset` property.
     * @return the meta-property, not null
     */
    def fixingDateOffset(): MetaProperty[DaysAdjustment] = {
      fixingDateOffsetProperty
    }

    /**
     * The meta-property for the `maturityDateOffset` property.
     * @return the meta-property, not null
     */
    def maturityDateOffset(): MetaProperty[DaysAdjustment] = {
      maturityDateOffsetProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 3373707 => // name
          bean.asInstanceOf[ImmutableFxIndex].getName()
        case 1005147787 => // currencyPair
          bean.asInstanceOf[ImmutableFxIndex].getCurrencyPair()
        case 394230283 => // fixingCalendar
          bean.asInstanceOf[ImmutableFxIndex].getFixingCalendar()
        case 873743726 => // fixingDateOffset
          bean.asInstanceOf[ImmutableFxIndex].getFixingDateOffset()
        case 1574797394 => // maturityDateOffset
          bean.asInstanceOf[ImmutableFxIndex].getMaturityDateOffset()
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
   * The bean-builder for `ImmutableFxIndex`.
   */
  final class Builder private[index] () extends DirectFieldsBeanBuilder[ImmutableFxIndex] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private[index] var _name: String = uninitialized
    private[index] var _currencyPair: CurrencyPair = uninitialized
    private[index] var _fixingCalendar: HolidayCalendarId = uninitialized
    private[index] var _fixingDateOffset: DaysAdjustment = uninitialized
    private[index] var _maturityDateOffset: DaysAdjustment = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[index] def this(beanToCopy: ImmutableFxIndex) = {
      this()
      this._name = beanToCopy.getName()
      this._currencyPair = beanToCopy.getCurrencyPair()
      this._fixingCalendar = beanToCopy.getFixingCalendar()
      this._fixingDateOffset = beanToCopy.getFixingDateOffset()
      this._maturityDateOffset = beanToCopy.getMaturityDateOffset()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 3373707 => // name
          _name
        case 1005147787 => // currencyPair
          _currencyPair
        case 394230283 => // fixingCalendar
          _fixingCalendar
        case 873743726 => // fixingDateOffset
          _fixingDateOffset
        case 1574797394 => // maturityDateOffset
          _maturityDateOffset
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 3373707 => // name
          this._name = newValue.asInstanceOf[String]
        case 1005147787 => // currencyPair
          this._currencyPair = newValue.asInstanceOf[CurrencyPair]
        case 394230283 => // fixingCalendar
          this._fixingCalendar = newValue.asInstanceOf[HolidayCalendarId]
        case 873743726 => // fixingDateOffset
          this._fixingDateOffset = newValue.asInstanceOf[DaysAdjustment]
        case 1574797394 => // maturityDateOffset
          this._maturityDateOffset = newValue.asInstanceOf[DaysAdjustment]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): ImmutableFxIndex = {
      preBuild(this)
      new ImmutableFxIndex(
          _name,
          _currencyPair,
          _fixingCalendar,
          _fixingDateOffset,
          _maturityDateOffset)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index name, such as 'EUR/GBP-ECB'.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    def name(name: String): Builder = {
      JodaBeanUtils.notNull(name, "name")
      this._name = name
      this
    }

    /**
     * Sets the currency pair.
     *
     * An index defines an FX rate in a single direction, such as from EUR to USD.
     * This currency pair defines that direction.
     *
     * In most cases, the same index can be used to convert in both directions
     * by taking the rate or the reciprocal as necessary.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    def currencyPair(currencyPair: CurrencyPair): Builder = {
      JodaBeanUtils.notNull(currencyPair, "currencyPair")
      this._currencyPair = currencyPair
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
     * Sets the adjustment applied to the maturity date to obtain the fixing date.
     *
     * The maturity date is the start date of the indexed deposit.
     * In most cases, the fixing date is 2 days before the maturity date.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingDateOffset(fixingDateOffset: DaysAdjustment): Builder = {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset")
      this._fixingDateOffset = fixingDateOffset
      this
    }

    /**
     * Sets the adjustment applied to the fixing date to obtain the maturity date.
     *
     * The maturity date is the start date of the indexed deposit.
     * In most cases, the maturity date is 2 days after the fixing date.
     * @param maturityDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    def maturityDateOffset(maturityDateOffset: DaysAdjustment): Builder = {
      JodaBeanUtils.notNull(maturityDateOffset, "maturityDateOffset")
      this._maturityDateOffset = maturityDateOffset
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(192)
      buf.append("ImmutableFxIndex.Builder{")
      buf.append("name").append('=').append(JodaBeanUtils.toString(_name)).append(',').append(' ')
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(_currencyPair)).append(',').append(' ')
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(_fixingCalendar)).append(',').append(' ')
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(_fixingDateOffset)).append(',').append(' ')
      buf.append("maturityDateOffset").append('=').append(JodaBeanUtils.toString(_maturityDateOffset))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
