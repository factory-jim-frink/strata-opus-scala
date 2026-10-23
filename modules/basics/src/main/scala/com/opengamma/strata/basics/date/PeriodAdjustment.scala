/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

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
import org.joda.beans.gen.ImmutableValidator
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.Resolvable

/**
 * An adjustment that alters a date by adding a period of calendar days, months and years.
 *
 * This adjustment adds a `Period` to the input date using an addition convention,
 * followed by an adjustment to ensure the result is a valid business day.
 *
 * Addition is performed using standard calendar addition.
 * It is not possible to add a number of business days using this class.
 * See `DaysAdjustment` for an alternative that can handle addition of business days.
 *
 * There are two steps in the calculation:
 *
 * In step one, the period is added using the specified `PeriodAdditionConvention`.
 *
 * In step two, the result of step one is optionally adjusted to be a business day
 * using a `BusinessDayAdjustment`.
 *
 * For example, a rule represented by this class might be: "the end date is 5 years after
 * the start date, with end-of-month rule based on the last business day of the month,
 * adjusted to be a valid London business day using the 'ModifiedFollowing' convention".
 */
@BeanDefinition
@SerialVersionUID(1L)
final class PeriodAdjustment private (
    /**
     * The period to be added.
     *
     * When the adjustment is performed, this period will be added to the input date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val period: Period,
    /**
     * The addition convention to apply.
     *
     * When the adjustment is performed, this convention is used to refine the adjusted date.
     * The most common convention is to move the end date to the last business day of the month
     * if the start date is the last business day of the month.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val additionConvention: PeriodAdditionConvention,
    /**
     * The business day adjustment that is performed to the result of the addition.
     *
     * This adjustment is applied to the result of the addition calculation.
     *
     * If no adjustment is required, use the 'None' business day adjustment.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val adjustment: BusinessDayAdjustment)
  extends Resolvable[DateAdjuster] with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(period, "period")
  JodaBeanUtils.notNull(additionConvention, "additionConvention")
  JodaBeanUtils.notNull(adjustment, "adjustment")
  validate()

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    if (additionConvention.isMonthBased() && period.getDays() != 0) {
      throw new IllegalArgumentException("Period must not contain days when addition convention is month-based")
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the period and then applying the business day adjustment.
   *
   * The calculation is performed in two steps.
   *
   * Step one, use `PeriodAdditionConvention#adjust(LocalDate, Period, HolidayCalendar)` to add the period.
   *
   * Step two, use `BusinessDayAdjustment#adjust(LocalDate, ReferenceData)` to adjust the result of step one.
   *
   * @param date  the date to adjust
   * @param refData  the reference data, used to find the holiday calendar
   * @return the adjusted date
   */
  def adjust(date: LocalDate, refData: ReferenceData): LocalDate = {
    val holCal = adjustment.getCalendar().resolve(refData)
    val bda = adjustment.getConvention()
    bda.adjust(additionConvention.adjust(date, period, holCal), holCal)
  }

  /**
   * Resolves this adjustment using the specified reference data, returning an adjuster.
   *
   * This returns a `DateAdjuster` that performs the same calculation as this adjustment.
   * It binds the holiday calendar, looked up from the reference data, into the result.
   * As such, there is no need to pass the reference data in again.
   *
   * @param refData  the reference data, used to find the holiday calendar
   * @return the adjuster, bound to a specific holiday calendar
   */
  override def resolve(refData: ReferenceData): DateAdjuster = {
    val holCal = adjustment.getCalendar().resolve(refData)
    val bda = adjustment.getConvention()
    date => bda.adjust(additionConvention.adjust(date, period, holCal), holCal)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the adjustment.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    val buf = new java.lang.StringBuilder(64)
    buf.append(period)
    if (additionConvention ne PeriodAdditionConventions.NONE) {
      buf.append(" with ").append(additionConvention)
    }
    if (adjustment.equals(BusinessDayAdjustment.NONE) == false) {
      buf.append(" then apply ").append(adjustment)
    }
    buf.toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): PeriodAdjustment.Meta = {
    PeriodAdjustment.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the period to be added.
   *
   * When the adjustment is performed, this period will be added to the input date.
   * @return the value of the property, not null
   */
  def getPeriod(): Period = {
    period
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the addition convention to apply.
   *
   * When the adjustment is performed, this convention is used to refine the adjusted date.
   * The most common convention is to move the end date to the last business day of the month
   * if the start date is the last business day of the month.
   * @return the value of the property, not null
   */
  def getAdditionConvention(): PeriodAdditionConvention = {
    additionConvention
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment that is performed to the result of the addition.
   *
   * This adjustment is applied to the result of the addition calculation.
   *
   * If no adjustment is required, use the 'None' business day adjustment.
   * @return the value of the property, not null
   */
  def getAdjustment(): BusinessDayAdjustment = {
    adjustment
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): PeriodAdjustment.Builder = {
    new PeriodAdjustment.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[PeriodAdjustment]
      JodaBeanUtils.equal(period, other.period) &&
          JodaBeanUtils.equal(additionConvention, other.additionConvention) &&
          JodaBeanUtils.equal(adjustment, other.adjustment)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(period)
    hash = hash * 31 + JodaBeanUtils.hashCode(additionConvention)
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object PeriodAdjustment {

  /**
   * An instance that performs no adjustment.
   */
  @static final val NONE: PeriodAdjustment =
    new PeriodAdjustment(Period.ZERO, PeriodAdditionConventions.NONE, BusinessDayAdjustment.NONE)

  @static private val REGISTERED: Boolean = {
    MetaBean.register(PeriodAdjustment.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can adjust a date by the specified period.
   *
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * @param period  the period to add to the input date
   * @param additionConvention  the convention used to perform the addition
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  def of(
      period: Period,
      additionConvention: PeriodAdditionConvention,
      adjustment: BusinessDayAdjustment): PeriodAdjustment = {

    new PeriodAdjustment(period, additionConvention, adjustment)
  }

  /**
   * Obtains an instance that can adjust a date by the specified period using the
   * last day of month convention.
   *
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * The period must consist only of months and/or years.
   *
   * @param period  the period to add to the input date
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  def ofLastDay(period: Period, adjustment: BusinessDayAdjustment): PeriodAdjustment = {
    new PeriodAdjustment(period, PeriodAdditionConventions.LAST_DAY, adjustment)
  }

  /**
   * Obtains an instance that can adjust a date by the specified period using the
   * last business day of month convention.
   *
   * When adjusting a date, the specified period is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * The period must consist only of months and/or years.
   *
   * @param period  the period to add to the input date
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the period adjustment
   */
  def ofLastBusinessDay(period: Period, adjustment: BusinessDayAdjustment): PeriodAdjustment = {
    new PeriodAdjustment(period, PeriodAdditionConventions.LAST_BUSINESS_DAY, adjustment)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `PeriodAdjustment`.
   * @return the meta-bean, not null
   */
  def meta(): PeriodAdjustment.Meta = {
    PeriodAdjustment.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): PeriodAdjustment.Builder = {
    new PeriodAdjustment.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `PeriodAdjustment`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `period` property.
     */
    private val periodProperty: MetaProperty[Period] = DirectMetaProperty.ofImmutable(
        this, "period", classOf[PeriodAdjustment], classOf[Period])
    /**
     * The meta-property for the `additionConvention` property.
     */
    private val additionConventionProperty: MetaProperty[PeriodAdditionConvention] = DirectMetaProperty.ofImmutable(
        this, "additionConvention", classOf[PeriodAdjustment], classOf[PeriodAdditionConvention])
    /**
     * The meta-property for the `adjustment` property.
     */
    private val adjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "adjustment", classOf[PeriodAdjustment], classOf[BusinessDayAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "period",
        "additionConvention",
        "adjustment")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -991726143 => // period
          periodProperty
        case 1652975501 => // additionConvention
          additionConventionProperty
        case 1977085293 => // adjustment
          adjustmentProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): PeriodAdjustment.Builder = {
      new PeriodAdjustment.Builder()
    }

    override def beanType(): Class[? <: PeriodAdjustment] = {
      classOf[PeriodAdjustment]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `period` property.
     * @return the meta-property, not null
     */
    def period(): MetaProperty[Period] = {
      periodProperty
    }

    /**
     * The meta-property for the `additionConvention` property.
     * @return the meta-property, not null
     */
    def additionConvention(): MetaProperty[PeriodAdditionConvention] = {
      additionConventionProperty
    }

    /**
     * The meta-property for the `adjustment` property.
     * @return the meta-property, not null
     */
    def adjustment(): MetaProperty[BusinessDayAdjustment] = {
      adjustmentProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -991726143 => // period
          bean.asInstanceOf[PeriodAdjustment].getPeriod()
        case 1652975501 => // additionConvention
          bean.asInstanceOf[PeriodAdjustment].getAdditionConvention()
        case 1977085293 => // adjustment
          bean.asInstanceOf[PeriodAdjustment].getAdjustment()
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
    private[date] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `PeriodAdjustment`.
   */
  final class Builder private[date] () extends DirectFieldsBeanBuilder[PeriodAdjustment] {

    private var periodField: Period = uninitialized
    private var additionConventionField: PeriodAdditionConvention = uninitialized
    private var adjustmentField: BusinessDayAdjustment = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[date] def this(beanToCopy: PeriodAdjustment) = {
      this()
      this.periodField = beanToCopy.getPeriod()
      this.additionConventionField = beanToCopy.getAdditionConvention()
      this.adjustmentField = beanToCopy.getAdjustment()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -991726143 => // period
          periodField
        case 1652975501 => // additionConvention
          additionConventionField
        case 1977085293 => // adjustment
          adjustmentField
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -991726143 => // period
          this.periodField = newValue.asInstanceOf[Period]
        case 1652975501 => // additionConvention
          this.additionConventionField = newValue.asInstanceOf[PeriodAdditionConvention]
        case 1977085293 => // adjustment
          this.adjustmentField = newValue.asInstanceOf[BusinessDayAdjustment]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): PeriodAdjustment = {
      new PeriodAdjustment(
          periodField,
          additionConventionField,
          adjustmentField)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period to be added.
     *
     * When the adjustment is performed, this period will be added to the input date.
     * @param period  the new value, not null
     * @return this, for chaining, not null
     */
    def period(period: Period): Builder = {
      JodaBeanUtils.notNull(period, "period")
      this.periodField = period
      this
    }

    /**
     * Sets the addition convention to apply.
     *
     * When the adjustment is performed, this convention is used to refine the adjusted date.
     * The most common convention is to move the end date to the last business day of the month
     * if the start date is the last business day of the month.
     * @param additionConvention  the new value, not null
     * @return this, for chaining, not null
     */
    def additionConvention(additionConvention: PeriodAdditionConvention): Builder = {
      JodaBeanUtils.notNull(additionConvention, "additionConvention")
      this.additionConventionField = additionConvention
      this
    }

    /**
     * Sets the business day adjustment that is performed to the result of the addition.
     *
     * This adjustment is applied to the result of the addition calculation.
     *
     * If no adjustment is required, use the 'None' business day adjustment.
     * @param adjustment  the new value, not null
     * @return this, for chaining, not null
     */
    def adjustment(adjustment: BusinessDayAdjustment): Builder = {
      JodaBeanUtils.notNull(adjustment, "adjustment")
      this.adjustmentField = adjustment
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("PeriodAdjustment.Builder{")
      buf.append("period").append('=').append(JodaBeanUtils.toString(periodField)).append(',').append(' ')
      buf.append("additionConvention").append('=').append(JodaBeanUtils.toString(additionConventionField)).append(',').append(' ')
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustmentField))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
