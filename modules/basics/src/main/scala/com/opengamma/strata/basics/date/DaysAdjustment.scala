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

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.Resolvable
import com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING

/**
 * An adjustment that alters a date by adding a period of days.
 *
 * When processing dates in finance, the rules for adjusting a date by a number of days can be complex.
 * This class represents those rules, which operate in two steps - addition followed by adjustment.
 * There are two main ways to perform the addition:
 *
 * === Approach 1 - calendar days addition ===
 * This approach is triggered by using the `ofCalendarDays()` factory methods.
 * When adding a number of days to a date the addition is simple, no holidays or weekends apply.
 * For example, two days after Friday 15th August would be Sunday 17th, even though this is typically a weekend.
 * There are two steps in the calculation:
 *
 * In step one, the number of days is added without skipping any dates.
 *
 * In step two, the result of step one is optionally adjusted to be a business day
 * using a `BusinessDayAdjustment`.
 *
 * === Approach 2 - business days addition ===
 * With this approach the days to be added are treated as business days.
 * For example, two days after Friday 15th August would be Tuesday 19th, assuming a Saturday/Sunday
 * weekend and no other applicable holidays.
 *
 * This approach is triggered by using the `ofBusinessDays()` factory methods.
 * The distinction between business days, holidays and weekends is made using the specified holiday calendar.
 * There are two steps in the calculation:
 *
 * In step one, the number of days is added using `HolidayCalendar#shift(LocalDate, int)`.
 *
 * In step two, the result of step one is optionally adjusted to be a business day
 * using a `BusinessDayAdjustment`.
 *
 * At first glance, step two may seem pointless, as the result of step one will always be a valid business day.
 * However, the step two adjustment allows the possibility of applying a different holiday calendar.
 *
 * For example, a rule might have two parts: "first add 2 London business days, and then adjust the
 * result to be a valid New York business day using the 'ModifiedFollowing' convention".
 * Note that the holiday calendar differs in the two parts of the rule.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class DaysAdjustment private (
    /**
     * The number of days to be added.
     *
     * When the adjustment is performed, this amount will be added to the input date
     * using the calendar to determine the addition type.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val days: Int,
    /**
     * The holiday calendar that defines the meaning of a day when performing the addition.
     *
     * When the adjustment is performed, this calendar is used to determine which days are business days.
     *
     * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
     * considering any days as holidays or weekends.
     * If the holiday calendar is anything other than 'None' then addition uses that calendar,
     * effectively repeatedly finding the next business day.
     *
     * See the class-level documentation for more information.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val calendar: HolidayCalendarId,
    /**
     * The business day adjustment that is performed to the result of the addition.
     *
     * This adjustment is applied to the result of the period addition calculation.
     * If the addition is performed using business days then any adjustment here is expected to
     * have a different holiday calendar to that used during addition.
     *
     * If no adjustment is required, use the 'None' business day adjustment.
     *
     * See the class-level documentation for more information.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val adjustment: BusinessDayAdjustment)
  extends Resolvable[DateAdjuster] with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(Integer.valueOf(days), "days")
  JodaBeanUtils.notNull(calendar, "calendar")
  JodaBeanUtils.notNull(adjustment, "adjustment")

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date, adding the period in days using the holiday calendar
   * and then applying the business day adjustment.
   *
   * The calculation is performed in two steps.
   *
   * Step one, use `HolidayCalendar#shift(LocalDate, int)` to add the number of days.
   * If the holiday calendar is 'None' this will effectively add calendar days.
   *
   * Step two, use `BusinessDayAdjustment#adjust(LocalDate, ReferenceData)` to adjust the result of step one.
   *
   * @param date  the date to adjust
   * @param refData  the reference data, used to find the holiday calendar
   * @return the adjusted date
   */
  def adjust(date: LocalDate, refData: ReferenceData): LocalDate = {
    val added = calendar.resolve(refData).shift(date, days)
    adjustment.adjust(added, refData)
  }

  /**
   * Resolves this adjustment using the specified reference data, returning an adjuster.
   *
   * This returns a `DateAdjuster` that performs the same calculation as this adjustment.
   * It binds the holiday calendar, looked up from the reference data, into the result.
   * As such, there is no need to pass the reference data in again.
   *
   * The resulting adjuster will be [[normalized normalized]].
   *
   * @param refData  the reference data, used to find the holiday calendar
   * @return the adjuster, bound to a specific holiday calendar
   */
  override def resolve(refData: ReferenceData): DateAdjuster = {
    val holCalAdj = adjustment.getCalendar().resolve(refData)
    if (calendar eq HolidayCalendarIds.NO_HOLIDAYS) {
      val adjustmentConvention = adjustment.getConvention()
      date => adjustmentConvention.adjust(LocalDateUtils.plusDays(date, days), holCalAdj)
    } else {
      val holCalAdd = calendar.resolve(refData)
      val adjustmentConvention = adjustment.getConvention()
      date => adjustmentConvention.adjust(holCalAdd.shift(date, days), holCalAdj)
    }
  }

  /**
   * Gets the holiday calendar that will be applied to the result.
   *
   * This adjustment may contain more than one holiday calendar.
   * This method returns the calendar used last.
   * As such, the adjusted date will always be valid according to this calendar.
   *
   * @return the result holiday calendar
   */
  def getResultCalendar(): HolidayCalendarId = {
    var cal = adjustment.getCalendar()
    if (cal eq HolidayCalendarIds.NO_HOLIDAYS) {
      cal = calendar
    }
    cal
  }

  //-------------------------------------------------------------------------
  /**
   * Normalizes the adjustment.
   *
   * If the number of days is zero, the calendar is set no 'NoHolidays'.
   * If the number of days is non-zero and the calendar equals the adjustment calendar,
   * the adjustment is removed.
   *
   * @return the normalized adjustment
   */
  def normalized(): DaysAdjustment = {
    if (days == 0) {
      if (calendar ne HolidayCalendarIds.NO_HOLIDAYS) {
        DaysAdjustment.ofCalendarDays(days, adjustment)
      } else {
        this
      }
    } else if (calendar.equals(adjustment.getCalendar())) {
      DaysAdjustment.ofBusinessDays(days, calendar)
    } else {
      this
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the adjustment.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    val buf = new java.lang.StringBuilder(64)
    buf.append(days)
    if (calendar eq HolidayCalendarIds.NO_HOLIDAYS) {
      buf.append(" calendar day")
      if (days != 1) {
        buf.append("s")
      }
    } else {
      buf.append(" business day")
      if (days != 1) {
        buf.append("s")
      }
      buf.append(" using calendar ").append(calendar.getName())
    }
    if (adjustment.equals(BusinessDayAdjustment.NONE) == false) {
      buf.append(" then apply ").append(adjustment)
    }
    buf.toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): DaysAdjustment.Meta = {
    DaysAdjustment.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to be added.
   *
   * When the adjustment is performed, this amount will be added to the input date
   * using the calendar to determine the addition type.
   * @return the value of the property, not null
   */
  def getDays(): Int = {
    days
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the holiday calendar that defines the meaning of a day when performing the addition.
   *
   * When the adjustment is performed, this calendar is used to determine which days are business days.
   *
   * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
   * considering any days as holidays or weekends.
   * If the holiday calendar is anything other than 'None' then addition uses that calendar,
   * effectively repeatedly finding the next business day.
   *
   * See the class-level documentation for more information.
   * @return the value of the property, not null
   */
  def getCalendar(): HolidayCalendarId = {
    calendar
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment that is performed to the result of the addition.
   *
   * This adjustment is applied to the result of the period addition calculation.
   * If the addition is performed using business days then any adjustment here is expected to
   * have a different holiday calendar to that used during addition.
   *
   * If no adjustment is required, use the 'None' business day adjustment.
   *
   * See the class-level documentation for more information.
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
  def toBuilder(): DaysAdjustment.Builder = {
    new DaysAdjustment.Builder(this)
  }

  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[DaysAdjustment]
      (days == other.days) &&
          JodaBeanUtils.equal(calendar, other.calendar) &&
          JodaBeanUtils.equal(adjustment, other.adjustment)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(days)
    hash = hash * 31 + JodaBeanUtils.hashCode(calendar)
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object DaysAdjustment {

  /**
   * An instance that performs no adjustment.
   */
  @static final val NONE: DaysAdjustment =
    new DaysAdjustment(0, HolidayCalendarIds.NO_HOLIDAYS, BusinessDayAdjustment.NONE)

  @static private val REGISTERED: Boolean = {
    MetaBean.register(DaysAdjustment.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can adjust a date by a specific number of calendar days.
   *
   * When adjusting a date, the specified number of calendar days is added.
   * Holidays and weekends are not taken into account in the calculation.
   *
   * No business day adjustment is applied to the result of the addition.
   *
   * @param numberOfDays  the number of days
   * @return the days adjustment
   */
  def ofCalendarDays(numberOfDays: Int): DaysAdjustment = {
    new DaysAdjustment(numberOfDays, HolidayCalendarIds.NO_HOLIDAYS, BusinessDayAdjustment.NONE)
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of calendar days.
   *
   * When adjusting a date, the specified number of calendar days is added.
   * Holidays and weekends are not taken into account in the calculation.
   *
   * The business day adjustment is applied to the result of the addition.
   *
   * @param numberOfDays  the number of days
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the days adjustment
   */
  def ofCalendarDays(numberOfDays: Int, adjustment: BusinessDayAdjustment): DaysAdjustment = {
    new DaysAdjustment(numberOfDays, HolidayCalendarIds.NO_HOLIDAYS, adjustment)
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of business days.
   *
   * When adjusting a date, the specified number of business days is added.
   * This is equivalent to repeatedly finding the next business day.
   *
   * No business day adjustment is applied to the result of the addition.
   *
   * If the input is a holiday, the first business day counted will be the next business day.
   * If the input is a holiday and the number of days to add is zero, the result will be the next business day.
   *
   * @param numberOfDays  the number of days
   * @param holidayCalendar  the calendar that defines holidays and business days
   * @return the days adjustment
   */
  def ofBusinessDays(numberOfDays: Int, holidayCalendar: HolidayCalendarId): DaysAdjustment = {
    if (numberOfDays == 0) {
      new DaysAdjustment(0, HolidayCalendarIds.NO_HOLIDAYS, BusinessDayAdjustment.of(FOLLOWING, holidayCalendar))
    } else {
      new DaysAdjustment(numberOfDays, holidayCalendar, BusinessDayAdjustment.NONE)
    }
  }

  /**
   * Obtains an instance that can adjust a date by a specific number of business days.
   *
   * When adjusting a date, the specified number of business days is added.
   * This is equivalent to repeatedly finding the next business day.
   *
   * The business day adjustment is applied to the result of the addition.
   *
   * @param numberOfDays  the number of days
   * @param holidayCalendar  the calendar that defines holidays and business days
   * @param adjustment  the business day adjustment to apply to the result of the addition
   * @return the days adjustment
   */
  def ofBusinessDays(
      numberOfDays: Int,
      holidayCalendar: HolidayCalendarId,
      adjustment: BusinessDayAdjustment): DaysAdjustment = {

    new DaysAdjustment(numberOfDays, holidayCalendar, adjustment)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `DaysAdjustment`.
   * @return the meta-bean, not null
   */
  def meta(): DaysAdjustment.Meta = {
    DaysAdjustment.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): DaysAdjustment.Builder = {
    new DaysAdjustment.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `DaysAdjustment`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `days` property.
     */
    private val daysProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "days", classOf[DaysAdjustment], Integer.TYPE)
    /**
     * The meta-property for the `calendar` property.
     */
    private val calendarProperty: MetaProperty[HolidayCalendarId] = DirectMetaProperty.ofImmutable(
        this, "calendar", classOf[DaysAdjustment], classOf[HolidayCalendarId])
    /**
     * The meta-property for the `adjustment` property.
     */
    private val adjustmentProperty: MetaProperty[BusinessDayAdjustment] = DirectMetaProperty.ofImmutable(
        this, "adjustment", classOf[DaysAdjustment], classOf[BusinessDayAdjustment])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "days",
        "calendar",
        "adjustment")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 3076183 => // days
          daysProperty
        case -178324674 => // calendar
          calendarProperty
        case 1977085293 => // adjustment
          adjustmentProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): DaysAdjustment.Builder = {
      new DaysAdjustment.Builder()
    }

    override def beanType(): Class[? <: DaysAdjustment] = {
      classOf[DaysAdjustment]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `days` property.
     * @return the meta-property, not null
     */
    def days(): MetaProperty[Integer] = {
      daysProperty
    }

    /**
     * The meta-property for the `calendar` property.
     * @return the meta-property, not null
     */
    def calendar(): MetaProperty[HolidayCalendarId] = {
      calendarProperty
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
        case 3076183 => // days
          Integer.valueOf(bean.asInstanceOf[DaysAdjustment].getDays())
        case -178324674 => // calendar
          bean.asInstanceOf[DaysAdjustment].getCalendar()
        case 1977085293 => // adjustment
          bean.asInstanceOf[DaysAdjustment].getAdjustment()
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
   * The bean-builder for `DaysAdjustment`.
   */
  final class Builder private[date] () extends DirectFieldsBeanBuilder[DaysAdjustment] {

    private var daysField: Int = 0
    private var calendarField: HolidayCalendarId = null
    private var adjustmentField: BusinessDayAdjustment = null

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[date] def this(beanToCopy: DaysAdjustment) = {
      this()
      this.daysField = beanToCopy.getDays()
      this.calendarField = beanToCopy.getCalendar()
      this.adjustmentField = beanToCopy.getAdjustment()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 3076183 => // days
          Integer.valueOf(daysField)
        case -178324674 => // calendar
          calendarField
        case 1977085293 => // adjustment
          adjustmentField
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 3076183 => // days
          this.daysField = newValue.asInstanceOf[Integer].intValue()
        case -178324674 => // calendar
          this.calendarField = newValue.asInstanceOf[HolidayCalendarId]
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

    override def build(): DaysAdjustment = {
      new DaysAdjustment(
          daysField,
          calendarField,
          adjustmentField)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the number of days to be added.
     *
     * When the adjustment is performed, this amount will be added to the input date
     * using the calendar to determine the addition type.
     * @param days  the new value, not null
     * @return this, for chaining, not null
     */
    def days(days: Int): Builder = {
      JodaBeanUtils.notNull(Integer.valueOf(days), "days")
      this.daysField = days
      this
    }

    /**
     * Sets the holiday calendar that defines the meaning of a day when performing the addition.
     *
     * When the adjustment is performed, this calendar is used to determine which days are business days.
     *
     * If the holiday calendar is 'None' then addition uses simple date addition arithmetic without
     * considering any days as holidays or weekends.
     * If the holiday calendar is anything other than 'None' then addition uses that calendar,
     * effectively repeatedly finding the next business day.
     *
     * See the class-level documentation for more information.
     * @param calendar  the new value, not null
     * @return this, for chaining, not null
     */
    def calendar(calendar: HolidayCalendarId): Builder = {
      JodaBeanUtils.notNull(calendar, "calendar")
      this.calendarField = calendar
      this
    }

    /**
     * Sets the business day adjustment that is performed to the result of the addition.
     *
     * This adjustment is applied to the result of the period addition calculation.
     * If the addition is performed using business days then any adjustment here is expected to
     * have a different holiday calendar to that used during addition.
     *
     * If no adjustment is required, use the 'None' business day adjustment.
     *
     * See the class-level documentation for more information.
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
      buf.append("DaysAdjustment.Builder{")
      buf.append("days").append('=').append(JodaBeanUtils.toString(Integer.valueOf(daysField))).append(',').append(' ')
      buf.append("calendar").append('=').append(JodaBeanUtils.toString(calendarField)).append(',').append(' ')
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustmentField))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
