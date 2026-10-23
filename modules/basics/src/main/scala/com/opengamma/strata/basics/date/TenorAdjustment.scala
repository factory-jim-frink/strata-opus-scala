/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

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
import org.joda.beans.gen.ImmutableValidator
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.Resolvable

/**
 * An adjustment that alters a date by adding a tenor.
 *
 * This adjustment adds a `Tenor` to the input date using an addition convention,
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
final class TenorAdjustment private (
    /**
     * The tenor to be added.
     *
     * When the adjustment is performed, this tenor will be added to the input date.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val tenor: Tenor,
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

  JodaBeanUtils.notNull(tenor, "tenor")
  JodaBeanUtils.notNull(additionConvention, "additionConvention")
  JodaBeanUtils.notNull(adjustment, "adjustment")
  validate()

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    if (additionConvention.isMonthBased() && tenor.isMonthBased() == false) {
      throw new IllegalArgumentException("Tenor must not contain days when addition convention is month-based")
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the tenor and then applying the business day adjustment.
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
    bda.adjust(additionConvention.adjust(date, tenor.getPeriod(), holCal), holCal)
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
    val period = tenor.getPeriod()
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
    buf.append(tenor)
    if (additionConvention ne PeriodAdditionConventions.NONE) {
      buf.append(" with ").append(additionConvention)
    }
    if (adjustment.equals(BusinessDayAdjustment.NONE) == false) {
      buf.append(" then apply ").append(adjustment)
    }
    buf.toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): TenorAdjustment.Meta = {
    TenorAdjustment.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor to be added.
   *
   * When the adjustment is performed, this tenor will be added to the input date.
   * @return the value of the property, not null
   */
  def getTenor(): Tenor = {
    tenor
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
  def toBuilder(): TenorAdjustment.Builder = {
    new TenorAdjustment.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[TenorAdjustment]
      JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(additionConvention, other.additionConvention) &&
          JodaBeanUtils.equal(adjustment, other.adjustment)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor)
    hash = hash * 31 + JodaBeanUtils.hashCode(additionConvention)
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object TenorAdjustment {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(TenorAdjustment.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can adjust a date by the specified tenor.
   *
   * When adjusting a date, the specified tenor is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * @param tenor  the tenor to add to the input date
   * @param additionConvention  the convention used to perform the addition
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the tenor adjustment
   */
  def of(
      tenor: Tenor,
      additionConvention: PeriodAdditionConvention,
      adjustment: BusinessDayAdjustment): TenorAdjustment = {

    new TenorAdjustment(tenor, additionConvention, adjustment)
  }

  /**
   * Obtains an instance that can adjust a date by the specified tenor using the
   * last day of month convention.
   *
   * When adjusting a date, the specified tenor is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * The period must consist only of months and/or years.
   *
   * @param tenor  the tenor to add to the input date
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the tenor adjustment
   */
  def ofLastDay(tenor: Tenor, adjustment: BusinessDayAdjustment): TenorAdjustment = {
    new TenorAdjustment(tenor, PeriodAdditionConventions.LAST_DAY, adjustment)
  }

  /**
   * Obtains an instance that can adjust a date by the specified tenor using the
   * last business day of month convention.
   *
   * When adjusting a date, the specified tenor is added to the input date.
   * The business day adjustment will then be used to ensure the result is a valid business day.
   *
   * The period must consist only of months and/or years.
   *
   * @param tenor  the tenor to add to the input date
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the tenor adjustment
   */
  def ofLastBusinessDay(tenor: Tenor, adjustment: BusinessDayAdjustment): TenorAdjustment = {
    new TenorAdjustment(tenor, PeriodAdditionConventions.LAST_BUSINESS_DAY, adjustment)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `TenorAdjustment`.
   * @return the meta-bean, not null
   */
  def meta(): TenorAdjustment.Meta = {
    TenorAdjustment.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): TenorAdjustment.Builder = {
    new TenorAdjustment.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `TenorAdjustment`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `tenor` property.
     */
    private val tenorProperty: MetaProperty[Tenor] = DirectMetaProperty.ofImmutable(
        this, "tenor", classOf[TenorAdjustment], classOf[Tenor])
    /**
     * The meta-property for the `additionConvention` property.
     */
    private val additionConventionProperty: MetaProperty[PeriodAdditionConvention] = DirectMetaProperty.ofImmutable(
        this, "additionConvention", classOf[TenorAdjustment], classOf[PeriodAdditionConvention])
    /**
     * The meta-property for the `adjustment` property.
     */
    private val adjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "adjustment", classOf[TenorAdjustment], classOf[BusinessDayAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "tenor",
        "additionConvention",
        "adjustment")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 110246592 => // tenor
          tenorProperty
        case 1652975501 => // additionConvention
          additionConventionProperty
        case 1977085293 => // adjustment
          adjustmentProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): TenorAdjustment.Builder = {
      new TenorAdjustment.Builder()
    }

    override def beanType(): Class[? <: TenorAdjustment] = {
      classOf[TenorAdjustment]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `tenor` property.
     * @return the meta-property, not null
     */
    def tenor(): MetaProperty[Tenor] = {
      tenorProperty
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
        case 110246592 => // tenor
          bean.asInstanceOf[TenorAdjustment].getTenor()
        case 1652975501 => // additionConvention
          bean.asInstanceOf[TenorAdjustment].getAdditionConvention()
        case 1977085293 => // adjustment
          bean.asInstanceOf[TenorAdjustment].getAdjustment()
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
   * The bean-builder for `TenorAdjustment`.
   */
  final class Builder private[date] () extends DirectFieldsBeanBuilder[TenorAdjustment] {

    private var tenorField: Tenor = uninitialized
    private var additionConventionField: PeriodAdditionConvention = uninitialized
    private var adjustmentField: BusinessDayAdjustment = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[date] def this(beanToCopy: TenorAdjustment) = {
      this()
      this.tenorField = beanToCopy.getTenor()
      this.additionConventionField = beanToCopy.getAdditionConvention()
      this.adjustmentField = beanToCopy.getAdjustment()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 110246592 => // tenor
          tenorField
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
        case 110246592 => // tenor
          this.tenorField = newValue.asInstanceOf[Tenor]
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

    override def build(): TenorAdjustment = {
      new TenorAdjustment(
          tenorField,
          additionConventionField,
          adjustmentField)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the tenor to be added.
     *
     * When the adjustment is performed, this tenor will be added to the input date.
     * @param tenor  the new value, not null
     * @return this, for chaining, not null
     */
    def tenor(tenor: Tenor): Builder = {
      JodaBeanUtils.notNull(tenor, "tenor")
      this.tenorField = tenor
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
      buf.append("TenorAdjustment.Builder{")
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenorField)).append(',').append(' ')
      buf.append("additionConvention").append('=').append(JodaBeanUtils.toString(additionConventionField)).append(',').append(' ')
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustmentField))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
