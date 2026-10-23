/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value

import java.io.Serializable
import java.time.LocalDate
import java.util.List
import java.util.Map
import java.util.NoSuchElementException

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutableValidator
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.collect.ImmutableList
import com.opengamma.strata.basics.schedule.Frequency
import com.opengamma.strata.basics.schedule.RollConvention
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * A sequence of steps that vary a value over time.
 *
 * A financial value, such as the notional or interest rate, may vary over time.
 * This class represents a sequence of changes in the value within `ValueSchedule`.
 *
 * The sequence is defined by a start date, end date and frequency.
 * The adjustment at each step is defined using `ValueAdjustment`.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class ValueStepSequence private (
    /**
     * The first date in the sequence.
     *
     * This sequence will change the value on this date, but not before.
     * This must be one of the unadjusted dates in the schedule period schedule.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val firstStepDate: LocalDate,
    /**
     * The last date in the sequence.
     *
     * This sequence will change the value on this date, but not after.
     * This must be one of the unadjusted dates in the schedule period schedule.
     *
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * The date '2015-02-01' is an unadjusted schedule period boundary, and so may be specified here.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val lastStepDate: LocalDate,
    /**
     * The frequency of the sequence.
     *
     * This sequence will change the value on each date between the start and end defined by this frequency.
     * The frequency is interpreted relative to the frequency of a `Schedule`.
     * It must be equal or greater than the related schedule.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val frequency: Frequency,
    /**
     * The adjustment representing the change that occurs at each step.
     *
     * The adjustment type must not be 'Replace'.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val adjustment: ValueAdjustment)
  extends ImmutableBean with Serializable {

  JodaBeanUtils.notNull(firstStepDate, "firstStepDate")
  JodaBeanUtils.notNull(lastStepDate, "lastStepDate")
  JodaBeanUtils.notNull(frequency, "frequency")
  JodaBeanUtils.notNull(adjustment, "adjustment")
  validate()

  @ImmutableValidator
  private def validate(): Unit = {
    ArgChecker.inOrderOrEqual(firstStepDate, lastStepDate, "firstStepDate", "lastStepDate")
    ArgChecker.isTrue(adjustment.getType() ne ValueAdjustmentType.REPLACE, "ValueAdjustmentType must not be 'Replace'")
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves the sequence to a list of steps.
   *
   * @param existingSteps  the existing list of steps
   * @param rollConv  the roll convention
   * @return the steps
   */
  private[value] def resolve(existingSteps: List[ValueStep], rollConv: RollConvention): List[ValueStep] = {
    val steps: ImmutableList.Builder[ValueStep] = ImmutableList.builder[ValueStep]()
    steps.addAll(existingSteps)
    var prev: LocalDate = rollConv.adjust(firstStepDate)
    var date: LocalDate = rollConv.adjust(firstStepDate)
    val adjustedLastStepDate: LocalDate = rollConv.adjust(lastStepDate)
    while (!date.isAfter(adjustedLastStepDate)) {
      steps.add(ValueStep.of(date, adjustment))
      prev = date
      date = rollConv.next(date, frequency)
    }
    if (!prev.equals(adjustedLastStepDate)) {
      throw new IllegalArgumentException(Messages.format(
          "ValueStepSequence lastStepDate did not match frequency '{}' using roll convention '{}', {} != {}",
          frequency, rollConv, adjustedLastStepDate, prev))
    }
    steps.build()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): ValueStepSequence.Meta = {
    ValueStepSequence.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first date in the sequence.
   *
   * This sequence will change the value on this date, but not before.
   * This must be one of the unadjusted dates in the schedule period schedule.
   *
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the value of the property, not null
   */
  def getFirstStepDate(): LocalDate = {
    firstStepDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date in the sequence.
   *
   * This sequence will change the value on this date, but not after.
   * This must be one of the unadjusted dates in the schedule period schedule.
   *
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2015-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the value of the property, not null
   */
  def getLastStepDate(): LocalDate = {
    lastStepDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the frequency of the sequence.
   *
   * This sequence will change the value on each date between the start and end defined by this frequency.
   * The frequency is interpreted relative to the frequency of a `Schedule`.
   * It must be equal or greater than the related schedule.
   * @return the value of the property, not null
   */
  def getFrequency(): Frequency = {
    frequency
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment representing the change that occurs at each step.
   *
   * The adjustment type must not be 'Replace'.
   * @return the value of the property, not null
   */
  def getAdjustment(): ValueAdjustment = {
    adjustment
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[ValueStepSequence]
      JodaBeanUtils.equal(firstStepDate, other.firstStepDate) &&
          JodaBeanUtils.equal(lastStepDate, other.lastStepDate) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(adjustment, other.adjustment)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(firstStepDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(lastStepDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency)
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(160)
    buf.append("ValueStepSequence{")
    buf.append("firstStepDate").append('=').append(JodaBeanUtils.toString(firstStepDate)).append(',').append(' ')
    buf.append("lastStepDate").append('=').append(JodaBeanUtils.toString(lastStepDate)).append(',').append(' ')
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ')
    buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ValueStepSequence {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ValueStepSequence.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the dates, frequency and change.
   *
   * @param firstStepDate  the first date of the sequence
   * @param lastStepDate  the last date of the sequence
   * @param frequency  the frequency of changes
   * @param adjustment  the adjustment at each step
   * @return the varying step
   */
  def of(
      firstStepDate: LocalDate,
      lastStepDate: LocalDate,
      frequency: Frequency,
      adjustment: ValueAdjustment): ValueStepSequence = {

    new ValueStepSequence(firstStepDate, lastStepDate, frequency, adjustment)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ValueStepSequence`.
   * @return the meta-bean, not null
   */
  def meta(): ValueStepSequence.Meta = {
    ValueStepSequence.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ValueStepSequence`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `firstStepDate` property.
     */
    private val firstStepDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "firstStepDate", classOf[ValueStepSequence], classOf[LocalDate])
    /**
     * The meta-property for the `lastStepDate` property.
     */
    private val lastStepDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "lastStepDate", classOf[ValueStepSequence], classOf[LocalDate])
    /**
     * The meta-property for the `frequency` property.
     */
    private val frequencyProperty: MetaProperty[Frequency] = DirectMetaProperty.ofImmutable(
        this, "frequency", classOf[ValueStepSequence], classOf[Frequency])
    /**
     * The meta-property for the `adjustment` property.
     */
    private val adjustmentProperty: MetaProperty[ValueAdjustment] = DirectMetaProperty.ofImmutable(
        this, "adjustment", classOf[ValueStepSequence], classOf[ValueAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "firstStepDate",
        "lastStepDate",
        "frequency",
        "adjustment")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -1025397910 => // firstStepDate
          firstStepDateProperty
        case -292412080 => // lastStepDate
          lastStepDateProperty
        case -70023844 => // frequency
          frequencyProperty
        case 1977085293 => // adjustment
          adjustmentProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: ValueStepSequence] = {
      new ValueStepSequence.Builder()
    }

    override def beanType(): Class[? <: ValueStepSequence] = {
      classOf[ValueStepSequence]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `firstStepDate` property.
     * @return the meta-property, not null
     */
    def firstStepDate(): MetaProperty[LocalDate] = {
      firstStepDateProperty
    }

    /**
     * The meta-property for the `lastStepDate` property.
     * @return the meta-property, not null
     */
    def lastStepDate(): MetaProperty[LocalDate] = {
      lastStepDateProperty
    }

    /**
     * The meta-property for the `frequency` property.
     * @return the meta-property, not null
     */
    def frequency(): MetaProperty[Frequency] = {
      frequencyProperty
    }

    /**
     * The meta-property for the `adjustment` property.
     * @return the meta-property, not null
     */
    def adjustment(): MetaProperty[ValueAdjustment] = {
      adjustmentProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -1025397910 => // firstStepDate
          bean.asInstanceOf[ValueStepSequence].getFirstStepDate()
        case -292412080 => // lastStepDate
          bean.asInstanceOf[ValueStepSequence].getLastStepDate()
        case -70023844 => // frequency
          bean.asInstanceOf[ValueStepSequence].getFrequency()
        case 1977085293 => // adjustment
          bean.asInstanceOf[ValueStepSequence].getAdjustment()
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
   * The bean-builder for `ValueStepSequence`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[ValueStepSequence] {

    private var firstStepDate: LocalDate = uninitialized
    private var lastStepDate: LocalDate = uninitialized
    private var frequency: Frequency = uninitialized
    private var adjustment: ValueAdjustment = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -1025397910 => // firstStepDate
          firstStepDate
        case -292412080 => // lastStepDate
          lastStepDate
        case -70023844 => // frequency
          frequency
        case 1977085293 => // adjustment
          adjustment
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -1025397910 => // firstStepDate
          this.firstStepDate = newValue.asInstanceOf[LocalDate]
        case -292412080 => // lastStepDate
          this.lastStepDate = newValue.asInstanceOf[LocalDate]
        case -70023844 => // frequency
          this.frequency = newValue.asInstanceOf[Frequency]
        case 1977085293 => // adjustment
          this.adjustment = newValue.asInstanceOf[ValueAdjustment]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): ValueStepSequence = {
      new ValueStepSequence(
          firstStepDate,
          lastStepDate,
          frequency,
          adjustment)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(160)
      buf.append("ValueStepSequence.Builder{")
      buf.append("firstStepDate").append('=').append(JodaBeanUtils.toString(firstStepDate)).append(',').append(' ')
      buf.append("lastStepDate").append('=').append(JodaBeanUtils.toString(lastStepDate)).append(',').append(' ')
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ')
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
