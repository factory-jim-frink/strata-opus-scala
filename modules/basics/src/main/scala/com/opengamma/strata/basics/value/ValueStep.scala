/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value

import java.io.Serializable
import java.time.LocalDate
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional
import java.util.OptionalInt

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

import com.opengamma.strata.basics.schedule.SchedulePeriod

/**
 * A single step in the variation of a value over time.
 *
 * A financial value, such as the notional or interest rate, may vary over time.
 * This class represents a single change in the value within `ValueSchedule`.
 *
 * The date of the change is either specified explicitly, or in relative terms via an index.
 * The adjustment to the value can also be specified absolutely, or in relative terms.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class ValueStep private (
    /**
     * The index of the schedule period boundary at which the change occurs.
     *
     * This property is used to define the date that the step occurs in relative terms.
     * The date is identified by specifying the zero-based index of the schedule period boundary.
     * The change will occur at the start of the specified period.
     * Thus an index of zero is the start of the first period or initial stub.
     * The index must be one or greater, as a change is not permitted at the start of the first period.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val periodIndex: Integer,
    /**
     * The date of the schedule period boundary at which the change occurs.
     *
     * This property is used to define the date that the step occurs in absolute terms.
     * This must be one of the unadjusted dates in the schedule period schedule.
     * This is an unadjusted date and calculation period business day adjustments will apply.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val date: LocalDate,
    /**
     * The value representing the change that occurs.
     *
     * The adjustment can be an absolute value, or various kinds of relative values.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val value: ValueAdjustment)
  extends ImmutableBean with Serializable {

  JodaBeanUtils.notNull(value, "value")
  validate()

  //-------------------------------------------------------------------------
  /**
   * Finds the index of this value step in the schedule.
   *
   * @param periods  the list of schedule periods
   * @return the index of the schedule period
   */
  private[value] def findIndex(periods: List[SchedulePeriod]): Int = {
    // either periodIndex or date is non-null, not both
    if (periodIndex != null) {
      // index based
      if (periodIndex.intValue() >= periods.size()) {
        throw new IllegalArgumentException("ValueStep index is beyond last schedule period")
      }
      periodIndex.intValue()
    } else {
      // date based, match one of the unadjusted period boundaries
      var i = 0
      while (i < periods.size()) {
        val period = periods.get(i)
        if (period.getUnadjustedStartDate().equals(date)) {
          return i
        }
        i += 1
      }
      // try adjusted boundaries instead of unadjusted ones
      i = 0
      while (i < periods.size()) {
        val period = periods.get(i)
        if (period.getStartDate().equals(date)) {
          return i
        }
        i += 1
      }
      -1
    }
  }

  /**
   * Finds the index of the previous value step in the schedule.
   *
   * This will only be called on a date-based step.
   *
   * @param periods  the list of schedule periods, at least size 1, only date-based
   * @return the index of the schedule period
   */
  private[value] def findPreviousIndex(periods: List[SchedulePeriod]): Int = {
    val firstPeriod = periods.get(0)
    if (date.isBefore(firstPeriod.getUnadjustedStartDate())) {
      throw new IllegalArgumentException(
          "ValueStep date is before the start of the schedule: " + date + " < " + firstPeriod.getUnadjustedStartDate())
    }
    var i = 1
    while (i < periods.size()) {
      val period = periods.get(i)
      if (period.getUnadjustedStartDate().isAfter(date)) {
        return i - 1
      }
      i += 1
    }
    val lastPeriod = periods.get(periods.size() - 1)
    if (date.isAfter(lastPeriod.getUnadjustedEndDate())) {
      throw new IllegalArgumentException(
          "ValueStep date is after the end of the schedule: " + date + " > " + lastPeriod.getUnadjustedEndDate())
    }
    periods.size() - 1
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    if (periodIndex == null && date == null) {
      throw new IllegalArgumentException("Either the 'periodIndex' or 'date' must be set")
    }
    if (periodIndex != null) {
      if (date != null) {
        throw new IllegalArgumentException("Either the 'periodIndex' or 'date' must be set, not both")
      }
      if (periodIndex.intValue() < 1) {
        throw new IllegalArgumentException("The 'periodIndex' must not be zero or negative")
      }
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): ValueStep.Meta = {
    ValueStep.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index of the schedule period boundary at which the change occurs.
   *
   * This property is used to define the date that the step occurs in relative terms.
   * The date is identified by specifying the zero-based index of the schedule period boundary.
   * The change will occur at the start of the specified period.
   * Thus an index of zero is the start of the first period or initial stub.
   * The index must be one or greater, as a change is not permitted at the start of the first period.
   *
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
   * @return the optional value of the property, not null
   */
  def getPeriodIndex(): OptionalInt = {
    if (periodIndex != null) OptionalInt.of(periodIndex.intValue()) else OptionalInt.empty()
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the schedule period boundary at which the change occurs.
   *
   * This property is used to define the date that the step occurs in absolute terms.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * This is an unadjusted date and calculation period business day adjustments will apply.
   *
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the optional value of the property, not null
   */
  def getDate(): Optional[LocalDate] = {
    Optional.ofNullable(date)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value representing the change that occurs.
   *
   * The adjustment can be an absolute value, or various kinds of relative values.
   * @return the value of the property, not null
   */
  def getValue(): ValueAdjustment = {
    value
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): ValueStep.Builder = {
    new ValueStep.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[ValueStep]
      JodaBeanUtils.equal(periodIndex, other.periodIndex) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(value, other.value)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(periodIndex)
    hash = hash * 31 + JodaBeanUtils.hashCode(date)
    hash = hash * 31 + JodaBeanUtils.hashCode(value)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(128)
    buf.append("ValueStep{")
    buf.append("periodIndex").append('=').append(JodaBeanUtils.toString(periodIndex)).append(',').append(' ')
    buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ')
    buf.append("value").append('=').append(JodaBeanUtils.toString(value))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ValueStep {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ValueStep.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that applies at the specified schedule period index.
   *
   * This factory is used to define the date that the step occurs in relative terms.
   * The date is identified by specifying the zero-based index of the schedule period boundary.
   * The change will occur at the start of the specified period.
   * Thus an index of zero is the start of the first period or initial stub.
   * The index must be one or greater, as a change is not permitted at the start of the first period.
   *
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
   *
   * The value may be absolute or relative, as per `ValueAdjustment`.
   *
   * @param periodIndex  the index of the period of the value change
   * @param value  the adjustment to make to the value
   * @return the varying step
   */
  def of(periodIndex: Int, value: ValueAdjustment): ValueStep = {
    new ValueStep(Integer.valueOf(periodIndex), null, value)
  }

  /**
   * Obtains an instance that applies at the specified date.
   *
   * This factory obtains a step that causes the value to change at the specified date.
   *
   * The value may be absolute or relative, as per `ValueAdjustment`.
   *
   * @param date  the start date of the value change
   * @param value  the adjustment to make to the value
   * @return the varying step
   */
  def of(date: LocalDate, value: ValueAdjustment): ValueStep = {
    new ValueStep(null, date, value)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ValueStep`.
   * @return the meta-bean, not null
   */
  def meta(): ValueStep.Meta = {
    ValueStep.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): ValueStep.Builder = {
    new ValueStep.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ValueStep`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `periodIndex` property.
     */
    private val periodIndexProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "periodIndex", classOf[ValueStep], classOf[Integer])
    /**
     * The meta-property for the `date` property.
     */
    private val dateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "date", classOf[ValueStep], classOf[LocalDate])
    /**
     * The meta-property for the `value` property.
     */
    private val valueProperty: MetaProperty[ValueAdjustment] = DirectMetaProperty.ofImmutable(
        this, "value", classOf[ValueStep], classOf[ValueAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "periodIndex",
        "date",
        "value")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -980601967 => // periodIndex
          periodIndexProperty
        case 3076014 => // date
          dateProperty
        case 111972721 => // value
          valueProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ValueStep.Builder = {
      new ValueStep.Builder()
    }

    override def beanType(): Class[? <: ValueStep] = {
      classOf[ValueStep]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `periodIndex` property.
     * @return the meta-property, not null
     */
    def periodIndex(): MetaProperty[Integer] = {
      periodIndexProperty
    }

    /**
     * The meta-property for the `date` property.
     * @return the meta-property, not null
     */
    def date(): MetaProperty[LocalDate] = {
      dateProperty
    }

    /**
     * The meta-property for the `value` property.
     * @return the meta-property, not null
     */
    def value(): MetaProperty[ValueAdjustment] = {
      valueProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -980601967 => // periodIndex
          bean.asInstanceOf[ValueStep].periodIndex
        case 3076014 => // date
          bean.asInstanceOf[ValueStep].date
        case 111972721 => // value
          bean.asInstanceOf[ValueStep].getValue()
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
    private[value] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `ValueStep`.
   */
  final class Builder private[value] () extends DirectFieldsBeanBuilder[ValueStep] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _periodIndex: Integer = uninitialized
    private var _date: LocalDate = uninitialized
    private var _value: ValueAdjustment = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[value] def this(beanToCopy: ValueStep) = {
      this()
      this._periodIndex = beanToCopy.periodIndex
      this._date = beanToCopy.date
      this._value = beanToCopy.getValue()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -980601967 => // periodIndex
          _periodIndex
        case 3076014 => // date
          _date
        case 111972721 => // value
          _value
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -980601967 => // periodIndex
          this._periodIndex = newValue.asInstanceOf[Integer]
        case 3076014 => // date
          this._date = newValue.asInstanceOf[LocalDate]
        case 111972721 => // value
          this._value = newValue.asInstanceOf[ValueAdjustment]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): ValueStep = {
      new ValueStep(
          _periodIndex,
          _date,
          _value)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of the schedule period boundary at which the change occurs.
     *
     * This property is used to define the date that the step occurs in relative terms.
     * The date is identified by specifying the zero-based index of the schedule period boundary.
     * The change will occur at the start of the specified period.
     * Thus an index of zero is the start of the first period or initial stub.
     * The index must be one or greater, as a change is not permitted at the start of the first period.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
     * @param periodIndex  the new value
     * @return this, for chaining, not null
     */
    def periodIndex(periodIndex: Integer): Builder = {
      this._periodIndex = periodIndex
      this
    }

    /**
     * Sets the date of the schedule period boundary at which the change occurs.
     *
     * This property is used to define the date that the step occurs in absolute terms.
     * This must be one of the unadjusted dates in the schedule period schedule.
     * This is an unadjusted date and calculation period business day adjustments will apply.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    def date(date: LocalDate): Builder = {
      this._date = date
      this
    }

    /**
     * Sets the value representing the change that occurs.
     *
     * The adjustment can be an absolute value, or various kinds of relative values.
     * @param value  the new value, not null
     * @return this, for chaining, not null
     */
    def value(value: ValueAdjustment): Builder = {
      JodaBeanUtils.notNull(value, "value")
      this._value = value
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("ValueStep.Builder{")
      buf.append("periodIndex").append('=').append(JodaBeanUtils.toString(_periodIndex)).append(',').append(' ')
      buf.append("date").append('=').append(JodaBeanUtils.toString(_date)).append(',').append(' ')
      buf.append("value").append('=').append(JodaBeanUtils.toString(_value))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
