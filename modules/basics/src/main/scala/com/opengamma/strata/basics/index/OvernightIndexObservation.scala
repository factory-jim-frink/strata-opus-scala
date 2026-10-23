/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap

import com.opengamma.strata.basics.ReferenceData
import com.opengamma.strata.basics.currency.Currency

/**
 * Information about a single observation of an Overnight index.
 *
 * Observing an Overnight index requires knowledge of the index, fixing date,
 * publication date, effective date and maturity date.
 */
@BeanDefinition
@SerialVersionUID(1L)
final class OvernightIndexObservation private (
    /**
     * The Overnight index.
     *
     * The rate will be queried from this index.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val index: OvernightIndex,
    /**
     * The date of the index fixing.
     *
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by `OvernightIndex#getFixingCalendar()`.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val fixingDate: LocalDate,
    /**
     * The date that the rate implied by the fixing date is published.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)`.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val publicationDate: LocalDate,
    /**
     * The effective date of the investment implied by the fixing date.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)`.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val effectiveDate: LocalDate,
    /**
     * The maturity date of the investment implied by the fixing date.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)`.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val maturityDate: LocalDate,
    /**
     * The year fraction of the investment implied by the fixing date.
     *
     * This is calculated using the day count of the index.
     * It represents the fraction of the year between the effective date and the maturity date.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val yearFraction: Double)
  extends IndexObservation with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(index, "index")
  JodaBeanUtils.notNull(fixingDate, "fixingDate")
  JodaBeanUtils.notNull(publicationDate, "publicationDate")
  JodaBeanUtils.notNull(effectiveDate, "effectiveDate")
  JodaBeanUtils.notNull(maturityDate, "maturityDate")
  JodaBeanUtils.notNull(Double.box(yearFraction), "yearFraction")

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the Overnight index.
   * 
   * @return the currency of the index
   */
  def getCurrency(): Currency = {
    index.getCurrency()
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this observation to another based on the index and fixing date.
   *
   * The publication, effective and maturity dates are ignored.
   * 
   * @param obj  the other observation
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[OvernightIndexObservation]
      JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingDate, other.fixingDate)
    } else {
      false
    }
  }

  /**
   * Returns a hash code based on the index and fixing date.
   *
   * The maturity date is ignored.
   * 
   * @return the hash code
   */
  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + index.hashCode()
    hash * 31 + fixingDate.hashCode()
  }

  override def toString(): String = {
    new java.lang.StringBuilder(64)
        .append("OvernightIndexObservation[")
        .append(index)
        .append(" on ")
        .append(fixingDate)
        .append(']')
        .toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): OvernightIndexObservation.Meta = {
    OvernightIndexObservation.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Overnight index.
   *
   * The rate will be queried from this index.
   * @return the value of the property, not null
   */
  override def getIndex(): OvernightIndex = {
    index
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the index fixing.
   *
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by `OvernightIndex#getFixingCalendar()`.
   * @return the value of the property, not null
   */
  def getFixingDate(): LocalDate = {
    fixingDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the rate implied by the fixing date is published.
   *
   * This is an adjusted date with any business day rule applied.
   * This must be equal to `OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)`.
   * @return the value of the property, not null
   */
  def getPublicationDate(): LocalDate = {
    publicationDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective date of the investment implied by the fixing date.
   *
   * This is an adjusted date with any business day rule applied.
   * This must be equal to `OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)`.
   * @return the value of the property, not null
   */
  def getEffectiveDate(): LocalDate = {
    effectiveDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the maturity date of the investment implied by the fixing date.
   *
   * This is an adjusted date with any business day rule applied.
   * This must be equal to `OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)`.
   * @return the value of the property, not null
   */
  def getMaturityDate(): LocalDate = {
    maturityDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction of the investment implied by the fixing date.
   *
   * This is calculated using the day count of the index.
   * It represents the fraction of the year between the effective date and the maturity date.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * @return the value of the property, not null
   */
  def getYearFraction(): Double = {
    yearFraction
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  def toBuilder(): OvernightIndexObservation.Builder = {
    new OvernightIndexObservation.Builder(this)
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object OvernightIndexObservation {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(OvernightIndexObservation.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an `IborRateObservation` from an index and fixing date.
   *
   * The reference data is used to find the maturity date from the fixing date.
   * 
   * @param index  the index
   * @param fixingDate  the fixing date
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate observation
   */
  def of(index: OvernightIndex, fixingDate: LocalDate, refData: ReferenceData): OvernightIndexObservation = {
    val publicationDate = index.calculatePublicationFromFixing(fixingDate, refData)
    val effectiveDate = index.calculateEffectiveFromFixing(fixingDate, refData)
    val maturityDate = index.calculateMaturityFromEffective(effectiveDate, refData)
    OvernightIndexObservation.builder()
        .index(index)
        .fixingDate(fixingDate)
        .publicationDate(publicationDate)
        .effectiveDate(effectiveDate)
        .maturityDate(maturityDate)
        .yearFraction(index.getDayCount().yearFraction(effectiveDate, maturityDate))
        .build()
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `OvernightIndexObservation`.
   * @return the meta-bean, not null
   */
  def meta(): OvernightIndexObservation.Meta = {
    OvernightIndexObservation.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  def builder(): OvernightIndexObservation.Builder = {
    new OvernightIndexObservation.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `OvernightIndexObservation`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `index` property.
     */
    private val indexProperty: MetaProperty[OvernightIndex] = DirectMetaProperty.ofImmutable(
        this, "index", classOf[OvernightIndexObservation], classOf[OvernightIndex])
    /**
     * The meta-property for the `fixingDate` property.
     */
    private val fixingDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "fixingDate", classOf[OvernightIndexObservation], classOf[LocalDate])
    /**
     * The meta-property for the `publicationDate` property.
     */
    private val publicationDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "publicationDate", classOf[OvernightIndexObservation], classOf[LocalDate])
    /**
     * The meta-property for the `effectiveDate` property.
     */
    private val effectiveDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "effectiveDate", classOf[OvernightIndexObservation], classOf[LocalDate])
    /**
     * The meta-property for the `maturityDate` property.
     */
    private val maturityDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "maturityDate", classOf[OvernightIndexObservation], classOf[LocalDate])
    /**
     * The meta-property for the `yearFraction` property.
     */
    private val yearFractionProperty: MetaProperty[java.lang.Double] = DirectMetaProperty.ofImmutable(
        this, "yearFraction", classOf[OvernightIndexObservation], java.lang.Double.TYPE)
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fixingDate",
        "publicationDate",
        "effectiveDate",
        "maturityDate",
        "yearFraction")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 100346066 => // index
          indexProperty
        case 1255202043 => // fixingDate
          fixingDateProperty
        case 1470566394 => // publicationDate
          publicationDateProperty
        case -930389515 => // effectiveDate
          effectiveDateProperty
        case -414641441 => // maturityDate
          maturityDateProperty
        case -1731780257 => // yearFraction
          yearFractionProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): OvernightIndexObservation.Builder = {
      new OvernightIndexObservation.Builder()
    }

    override def beanType(): Class[? <: OvernightIndexObservation] = {
      classOf[OvernightIndexObservation]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `index` property.
     * @return the meta-property, not null
     */
    def index(): MetaProperty[OvernightIndex] = {
      indexProperty
    }

    /**
     * The meta-property for the `fixingDate` property.
     * @return the meta-property, not null
     */
    def fixingDate(): MetaProperty[LocalDate] = {
      fixingDateProperty
    }

    /**
     * The meta-property for the `publicationDate` property.
     * @return the meta-property, not null
     */
    def publicationDate(): MetaProperty[LocalDate] = {
      publicationDateProperty
    }

    /**
     * The meta-property for the `effectiveDate` property.
     * @return the meta-property, not null
     */
    def effectiveDate(): MetaProperty[LocalDate] = {
      effectiveDateProperty
    }

    /**
     * The meta-property for the `maturityDate` property.
     * @return the meta-property, not null
     */
    def maturityDate(): MetaProperty[LocalDate] = {
      maturityDateProperty
    }

    /**
     * The meta-property for the `yearFraction` property.
     * @return the meta-property, not null
     */
    def yearFraction(): MetaProperty[java.lang.Double] = {
      yearFractionProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 100346066 => // index
          bean.asInstanceOf[OvernightIndexObservation].getIndex()
        case 1255202043 => // fixingDate
          bean.asInstanceOf[OvernightIndexObservation].getFixingDate()
        case 1470566394 => // publicationDate
          bean.asInstanceOf[OvernightIndexObservation].getPublicationDate()
        case -930389515 => // effectiveDate
          bean.asInstanceOf[OvernightIndexObservation].getEffectiveDate()
        case -414641441 => // maturityDate
          bean.asInstanceOf[OvernightIndexObservation].getMaturityDate()
        case -1731780257 => // yearFraction
          Double.box(bean.asInstanceOf[OvernightIndexObservation].getYearFraction())
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
   * The bean-builder for `OvernightIndexObservation`.
   */
  final class Builder private[index] () extends DirectFieldsBeanBuilder[OvernightIndexObservation] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _index: OvernightIndex = uninitialized
    private var _fixingDate: LocalDate = uninitialized
    private var _publicationDate: LocalDate = uninitialized
    private var _effectiveDate: LocalDate = uninitialized
    private var _maturityDate: LocalDate = uninitialized
    private var _yearFraction: Double = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[index] def this(beanToCopy: OvernightIndexObservation) = {
      this()
      this._index = beanToCopy.getIndex()
      this._fixingDate = beanToCopy.getFixingDate()
      this._publicationDate = beanToCopy.getPublicationDate()
      this._effectiveDate = beanToCopy.getEffectiveDate()
      this._maturityDate = beanToCopy.getMaturityDate()
      this._yearFraction = beanToCopy.getYearFraction()
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 100346066 => // index
          _index
        case 1255202043 => // fixingDate
          _fixingDate
        case 1470566394 => // publicationDate
          _publicationDate
        case -930389515 => // effectiveDate
          _effectiveDate
        case -414641441 => // maturityDate
          _maturityDate
        case -1731780257 => // yearFraction
          Double.box(_yearFraction)
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 100346066 => // index
          this._index = newValue.asInstanceOf[OvernightIndex]
        case 1255202043 => // fixingDate
          this._fixingDate = newValue.asInstanceOf[LocalDate]
        case 1470566394 => // publicationDate
          this._publicationDate = newValue.asInstanceOf[LocalDate]
        case -930389515 => // effectiveDate
          this._effectiveDate = newValue.asInstanceOf[LocalDate]
        case -414641441 => // maturityDate
          this._maturityDate = newValue.asInstanceOf[LocalDate]
        case -1731780257 => // yearFraction
          this._yearFraction = newValue.asInstanceOf[java.lang.Double].doubleValue()
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): OvernightIndexObservation = {
      new OvernightIndexObservation(
          _index,
          _fixingDate,
          _publicationDate,
          _effectiveDate,
          _maturityDate,
          _yearFraction)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Overnight index.
     *
     * The rate will be queried from this index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    def index(index: OvernightIndex): Builder = {
      JodaBeanUtils.notNull(index, "index")
      this._index = index
      this
    }

    /**
     * Sets the date of the index fixing.
     *
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by `OvernightIndex#getFixingCalendar()`.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    def fixingDate(fixingDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(fixingDate, "fixingDate")
      this._fixingDate = fixingDate
      this
    }

    /**
     * Sets the date that the rate implied by the fixing date is published.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)`.
     * @param publicationDate  the new value, not null
     * @return this, for chaining, not null
     */
    def publicationDate(publicationDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(publicationDate, "publicationDate")
      this._publicationDate = publicationDate
      this
    }

    /**
     * Sets the effective date of the investment implied by the fixing date.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)`.
     * @param effectiveDate  the new value, not null
     * @return this, for chaining, not null
     */
    def effectiveDate(effectiveDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(effectiveDate, "effectiveDate")
      this._effectiveDate = effectiveDate
      this
    }

    /**
     * Sets the maturity date of the investment implied by the fixing date.
     *
     * This is an adjusted date with any business day rule applied.
     * This must be equal to `OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)`.
     * @param maturityDate  the new value, not null
     * @return this, for chaining, not null
     */
    def maturityDate(maturityDate: LocalDate): Builder = {
      JodaBeanUtils.notNull(maturityDate, "maturityDate")
      this._maturityDate = maturityDate
      this
    }

    /**
     * Sets the year fraction of the investment implied by the fixing date.
     *
     * This is calculated using the day count of the index.
     * It represents the fraction of the year between the effective date and the maturity date.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * @param yearFraction  the new value, not null
     * @return this, for chaining, not null
     */
    def yearFraction(yearFraction: Double): Builder = {
      JodaBeanUtils.notNull(Double.box(yearFraction), "yearFraction")
      this._yearFraction = yearFraction
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(224)
      buf.append("OvernightIndexObservation.Builder{")
      buf.append("index").append('=').append(JodaBeanUtils.toString(_index)).append(',').append(' ')
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(_fixingDate)).append(',').append(' ')
      buf.append("publicationDate").append('=').append(JodaBeanUtils.toString(_publicationDate)).append(',').append(' ')
      buf.append("effectiveDate").append('=').append(JodaBeanUtils.toString(_effectiveDate)).append(',').append(' ')
      buf.append("maturityDate").append('=').append(JodaBeanUtils.toString(_maturityDate)).append(',').append(' ')
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(Double.box(_yearFraction)))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
