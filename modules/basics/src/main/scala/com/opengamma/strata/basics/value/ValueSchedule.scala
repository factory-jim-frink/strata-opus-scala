/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value

import java.io.Serializable
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
import com.opengamma.strata.basics.schedule.RollConvention
import com.opengamma.strata.basics.schedule.Schedule
import com.opengamma.strata.basics.schedule.SchedulePeriod
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.array.DoubleArray

/**
 * A value that can vary over time.
 *
 * This represents a single initial value and any adjustments over the lifetime of a trade.
 * Adjustments may be specified in absolute or relative terms.
 *
 * The adjustments may be specified as individual steps or as a sequence of steps.
 * An individual step is a change that occurs at a specific date, identified either by
 * the date or the index within the schedule. A sequence of steps consists of a start date,
 * end date and frequency, with the same change applying many times.
 * All changes must occur on dates that are period boundaries in the specified schedule.
 *
 * It is possible to specify both individual steps and a sequence, however this is not recommended.
 * It it is done, then the individual steps and sequence steps must resolve to different dates.
 *
 * The value is specified as a `double` with the context adding additional meaning.
 * If the value represents an amount of money then the currency is specified separately.
 * If the value represents a rate then a 5% rate is expressed as 0.05.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class ValueSchedule private (
    /**
     * The initial value.
     *
     * This is used for the lifetime of the trade unless specifically varied.
     */
    @(PropertyDefinition @field)
    private val initialValue: Double,
    rawSteps: List[ValueStep],
    /**
     * The sequence of steps changing the value.
     *
     * This allows a regular pattern of steps to be encoded.
     * All step dates must be unique, thus the list of steps must not contain any date implied by this sequence.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val stepSequence: ValueStepSequence)
  extends ImmutableBean with Serializable {

  JodaBeanUtils.notNull(rawSteps, "steps")
  /**
   * The steps defining the change in the value.
   *
   * Each step consists of a key locating the date of the change and the adjustment that occurs.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val steps: List[ValueStep] = ImmutableList.copyOf[ValueStep](rawSteps)

  //-------------------------------------------------------------------------
  /**
   * Resolves the value and adjustments against a specific schedule.
   *
   * This converts a schedule into a list of values, one for each schedule period.
   *
   * @param schedule  the schedule
   * @return the values, one for each schedule period
   */
  def resolveValues(schedule: Schedule): DoubleArray = {
    resolveValues(schedule.getPeriods(), schedule.getRollConvention())
  }

  // resolve the values
  private def resolveValues(periods: List[SchedulePeriod], rollConv: RollConvention): DoubleArray = {
    // handle simple case where there are no steps
    if (steps.size() == 0 && stepSequence == null) {
      DoubleArray.filled(periods.size(), initialValue)
    } else {
      resolveSteps(periods, rollConv)
    }
  }

  // resolve the steps, broken into a separate method to aid inlining
  private def resolveSteps(periods: List[SchedulePeriod], rollConv: RollConvention): DoubleArray = {
    val size = periods.size()
    val result = new Array[Double](size)
    val resolvedSteps: List[ValueStep] =
      getStepSequence().map[List[ValueStep]](seq => seq.resolve(steps, rollConv)).orElse(steps)
    // expand ValueStep to array of adjustments matching the periods
    // the steps are not sorted, so use fixed size array to absorb incoming data
    val expandedSteps = new Array[ValueAdjustment](size)
    val invalidSteps = new ArrayList[ValueStep]()
    resolvedSteps.forEach { step =>
      val index = step.findIndex(periods)
      if (index < 0) {
        invalidSteps.add(step)
      } else {
        if (expandedSteps(index) != null && !expandedSteps(index).equals(step.getValue())) {
          throw new IllegalArgumentException(Messages.format(
              "Invalid ValueSchedule, two steps resolved to the same schedule period starting on {}, schedule defined as {}",
              periods.get(index).getUnadjustedStartDate(), this))
        }
        expandedSteps(index) = step.getValue()
      }
    }
    // apply each adjustment
    var value = initialValue
    var i = 0
    while (i < size) {
      if (expandedSteps(i) != null) {
        value = expandedSteps(i).adjust(value)
      }
      result(i) = value
      i += 1
    }
    // ensure that invalid steps cause no changes
    invalidSteps.forEach { step =>
      val baseValue = result(step.findPreviousIndex(periods))
      val adjusted = step.getValue().adjust(baseValue)
      if (adjusted != baseValue) {
        throw new IllegalArgumentException("ValueStep date does not match a period boundary: " + step.getDate().get())
      }
    }
    // return result
    DoubleArray.ofUnsafe(result)
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): ValueSchedule.Meta = {
    ValueSchedule.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the initial value.
   *
   * This is used for the lifetime of the trade unless specifically varied.
   * @return the value of the property
   */
  def getInitialValue(): Double = {
    initialValue
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the steps defining the change in the value.
   *
   * Each step consists of a key locating the date of the change and the adjustment that occurs.
   * @return the value of the property, not null
   */
  def getSteps(): List[ValueStep] = {
    steps
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sequence of steps changing the value.
   *
   * This allows a regular pattern of steps to be encoded.
   * All step dates must be unique, thus the list of steps must not contain any date implied by this sequence.
   * @return the optional value of the property, not null
   */
  def getStepSequence(): Optional[ValueStepSequence] = {
    Optional.ofNullable(stepSequence)
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): ValueSchedule.Builder = {
    new ValueSchedule.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[ValueSchedule]
      JodaBeanUtils.equal(initialValue, other.initialValue) &&
          JodaBeanUtils.equal(steps, other.steps) &&
          JodaBeanUtils.equal(stepSequence, other.stepSequence)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(initialValue)
    hash = hash * 31 + JodaBeanUtils.hashCode(steps)
    hash = hash * 31 + JodaBeanUtils.hashCode(stepSequence)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(128)
    buf.append("ValueSchedule{")
    buf.append("initialValue").append('=').append(JodaBeanUtils.toString(Double.box(initialValue))).append(',').append(' ')
    buf.append("steps").append('=').append(JodaBeanUtils.toString(steps)).append(',').append(' ')
    buf.append("stepSequence").append('=').append(JodaBeanUtils.toString(stepSequence))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ValueSchedule {

  /**
   * A value schedule that always has the value zero.
   */
  @static final val ALWAYS_0: ValueSchedule = ValueSchedule.of(0)
  /**
   * A value schedule that always has the value one.
   */
  @static final val ALWAYS_1: ValueSchedule = ValueSchedule.of(1)

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ValueSchedule.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a single value that does not change over time.
   *
   * @param value  a single value that does not change over time
   * @return the value schedule
   */
  def of(value: Double): ValueSchedule = {
    new ValueSchedule(value, ImmutableList.of[ValueStep](), null)
  }

  /**
   * Obtains an instance from an initial value and a list of changes.
   *
   * Each step fully defines a single change in the value.
   * The date of each change can be specified as an absolute date or in relative terms.
   *
   * @param initialValue  the initial value used for the first period
   * @param steps  the full definition of how the value changes over time
   * @return the value schedule
   */
  @varargs
  def of(initialValue: Double, steps: ValueStep*): ValueSchedule = {
    new ValueSchedule(initialValue, ImmutableList.copyOf[ValueStep](steps.toArray[ValueStep]), null)
  }

  /**
   * Obtains an instance from an initial value and a list of changes.
   *
   * Each step fully defines a single change in the value.
   * The date of each change can be specified as an absolute date or in relative terms.
   *
   * @param initialValue  the initial value used for the first period
   * @param steps  the full definition of how the value changes over time
   * @return the value schedule
   */
  def of(initialValue: Double, steps: List[ValueStep]): ValueSchedule = {
    new ValueSchedule(initialValue, ImmutableList.copyOf[ValueStep](steps), null)
  }

  /**
   * Obtains an instance from an initial value and a sequence of steps.
   *
   * The sequence defines changes from one date to another date using a frequency.
   * For example, the value might change every year from 2011-06-01 to 2015-06-01.
   *
   * @param initialValue  the initial value used for the first period
   * @param stepSequence  the full definition of how the value changes over time
   * @return the value schedule
   */
  def of(initialValue: Double, stepSequence: ValueStepSequence): ValueSchedule = {
    new ValueSchedule(initialValue, ImmutableList.of[ValueStep](), stepSequence)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ValueSchedule`.
   * @return the meta-bean, not null
   */
  def meta(): ValueSchedule.Meta = {
    ValueSchedule.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): ValueSchedule.Builder = {
    new ValueSchedule.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ValueSchedule`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `initialValue` property.
     */
    private val initialValueProperty: MetaProperty[java.lang.Double] = DirectMetaProperty.ofImmutable(
        this, "initialValue", classOf[ValueSchedule], java.lang.Double.TYPE)
    /**
     * The meta-property for the `steps` property.
     */
    private val stepsProperty: MetaProperty[List[ValueStep]] = DirectMetaProperty.ofImmutable(
        this, "steps", classOf[ValueSchedule], classOf[List[?]].asInstanceOf[Class[List[ValueStep]]])
    /**
     * The meta-property for the `stepSequence` property.
     */
    private val stepSequenceProperty: MetaProperty[ValueStepSequence] = DirectMetaProperty.ofImmutable(
        this, "stepSequence", classOf[ValueSchedule], classOf[ValueStepSequence])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "initialValue",
        "steps",
        "stepSequence")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -418368371 => // initialValue
          initialValueProperty
        case 109761319 => // steps
          stepsProperty
        case 2141410989 => // stepSequence
          stepSequenceProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ValueSchedule.Builder = {
      new ValueSchedule.Builder()
    }

    override def beanType(): Class[? <: ValueSchedule] = {
      classOf[ValueSchedule]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `initialValue` property.
     * @return the meta-property, not null
     */
    def initialValue(): MetaProperty[java.lang.Double] = {
      initialValueProperty
    }

    /**
     * The meta-property for the `steps` property.
     * @return the meta-property, not null
     */
    def steps(): MetaProperty[List[ValueStep]] = {
      stepsProperty
    }

    /**
     * The meta-property for the `stepSequence` property.
     * @return the meta-property, not null
     */
    def stepSequence(): MetaProperty[ValueStepSequence] = {
      stepSequenceProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -418368371 => // initialValue
          Double.box(bean.asInstanceOf[ValueSchedule].getInitialValue())
        case 109761319 => // steps
          bean.asInstanceOf[ValueSchedule].getSteps()
        case 2141410989 => // stepSequence
          bean.asInstanceOf[ValueSchedule].stepSequence
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
   * The bean-builder for `ValueSchedule`.
   */
  final class Builder private[value] () extends DirectFieldsBeanBuilder[ValueSchedule] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _initialValue: Double = uninitialized
    private var _steps: List[ValueStep] = ImmutableList.of[ValueStep]()
    private var _stepSequence: ValueStepSequence = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[value] def this(beanToCopy: ValueSchedule) = {
      this()
      this._initialValue = beanToCopy.getInitialValue()
      this._steps = ImmutableList.copyOf[ValueStep](beanToCopy.getSteps())
      this._stepSequence = beanToCopy.stepSequence
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -418368371 => // initialValue
          Double.box(_initialValue)
        case 109761319 => // steps
          _steps
        case 2141410989 => // stepSequence
          _stepSequence
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -418368371 => // initialValue
          this._initialValue = newValue.asInstanceOf[java.lang.Double].doubleValue()
        case 109761319 => // steps
          this._steps = newValue.asInstanceOf[List[ValueStep]]
        case 2141410989 => // stepSequence
          this._stepSequence = newValue.asInstanceOf[ValueStepSequence]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): ValueSchedule = {
      new ValueSchedule(
          _initialValue,
          _steps,
          _stepSequence)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the initial value.
     *
     * This is used for the lifetime of the trade unless specifically varied.
     * @param initialValue  the new value
     * @return this, for chaining, not null
     */
    def initialValue(initialValue: Double): Builder = {
      this._initialValue = initialValue
      this
    }

    /**
     * Sets the steps defining the change in the value.
     *
     * Each step consists of a key locating the date of the change and the adjustment that occurs.
     * @param steps  the new value, not null
     * @return this, for chaining, not null
     */
    def steps(steps: List[ValueStep]): Builder = {
      JodaBeanUtils.notNull(steps, "steps")
      this._steps = steps
      this
    }

    /**
     * Sets the `steps` property in the builder
     * from an array of objects.
     * @param steps  the new value, not null
     * @return this, for chaining, not null
     */
    @varargs
    def steps(steps: ValueStep*): Builder = {
      this.steps(ImmutableList.copyOf[ValueStep](steps.toArray[ValueStep]))
    }

    /**
     * Sets the sequence of steps changing the value.
     *
     * This allows a regular pattern of steps to be encoded.
     * All step dates must be unique, thus the list of steps must not contain any date implied by this sequence.
     * @param stepSequence  the new value
     * @return this, for chaining, not null
     */
    def stepSequence(stepSequence: ValueStepSequence): Builder = {
      this._stepSequence = stepSequence
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("ValueSchedule.Builder{")
      buf.append("initialValue").append('=').append(JodaBeanUtils.toString(Double.box(_initialValue))).append(',').append(' ')
      buf.append("steps").append('=').append(JodaBeanUtils.toString(_steps)).append(',').append(' ')
      buf.append("stepSequence").append('=').append(JodaBeanUtils.toString(_stepSequence))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
