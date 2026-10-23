/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.io.Serializable
import java.util.Map
import java.util.NoSuchElementException
import java.util.OptionalInt
import java.util.Set

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

import com.google.common.collect.ImmutableSet
import com.opengamma.strata.basics.date.BusinessDayAdjustment
import com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.collect.Guavate

/**
 * An immutable floating rate index name, such as Libor, Euribor or US Fed Fund.
 *
 * This is the standard immutable implementation of `FloatingRateName`.
 */
@BeanDefinition(builderScope = "package")
@SerialVersionUID(1L)
final class ImmutableFloatingRateName private (
    /**
     * The external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
     */
    @(PropertyDefinition @field)(validate = "notEmpty")
    private val externalName: String,
    /**
     * The root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
     * This name matches that used by `IborIndex` or `OvernightIndex`.
     * Typically, multiple `FloatingRateName` names map to one Ibor or Overnight index.
     */
    @(PropertyDefinition @field)(validate = "notEmpty")
    private val indexName: String,
    /**
     * The type of the index.
     */
    @(PropertyDefinition @field)(validate = "notNull", overrideGet = true)
    private val `type`: FloatingRateType,
    /**
     * The fixing date offset, in days, optional.
     * This is used when a floating rate name implies a non-standard fixing date offset.
     * This is only used for Ibor Indices, and currently only for DKK CIBOR.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val fixingDateOffsetDays: Integer)
  extends FloatingRateName with ImmutableBean with Serializable {

  JodaBeanUtils.notEmpty(externalName, "externalName")
  JodaBeanUtils.notEmpty(indexName, "indexName")
  JodaBeanUtils.notNull(`type`, "type")

  //-------------------------------------------------------------------------
  override def getName(): String = {
    externalName
  }

  override def getFloatingRateName(): FloatingRateName = {
    this
  }

  override def getTenors(): Set[Tenor] = {
    if (!`type`.isIbor()) {
      return ImmutableSet.of[Tenor]()
    }
    IborIndex.extendedEnum().lookupAll().values().stream()
        .filter(index => index.getName().startsWith(indexName))
        .filter(index => index.isActive())
        .map[Tenor](index => index.getTenor())
        .sorted()
        .collect(Guavate.toImmutableSet[Tenor]())
  }

  override def normalized(): FloatingRateName = {
    if (`type`.isIbor() && indexName.endsWith("-")) {
      FloatingRateName.of(indexName.substring(0, indexName.length() - 1))
    } else {
      FloatingRateName.of(indexName)
    }
  }

  //-------------------------------------------------------------------------
  override def toIborIndex(tenor: Tenor): IborIndex = {
    if (!`type`.isIbor()) {
      throw new IllegalStateException("Incorrect index type, expected Ibor: " + externalName)
    }
    IborIndex.of(indexName + tenor.normalized().toString())
  }

  override def toIborIndexFixingOffset(): DaysAdjustment = {
    val base = super.toIborIndexFixingOffset()
    if (fixingDateOffsetDays == null) {
      base
    } else if (fixingDateOffsetDays.intValue() == 0) {
      DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, base.getResultCalendar()))
    } else {
      base.toBuilder().days(fixingDateOffsetDays.intValue()).build().normalized()
    }
  }

  override def toOvernightIndex(): OvernightIndex = {
    if (!`type`.isOvernight()) {
      throw new IllegalStateException("Incorrect index type, expected Overnight: " + externalName)
    }
    if (indexName.endsWith(ImmutableFloatingRateName.AVERAGE_SUFFIX)) {
      OvernightIndex.of(indexName.substring(0, indexName.length() - 4))
    } else {
      OvernightIndex.of(indexName)
    }
  }

  override def toPriceIndex(): PriceIndex = {
    if (!`type`.isPrice()) {
      throw new IllegalStateException("Incorrect index type, expected Price: " + externalName)
    }
    PriceIndex.of(indexName)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: ImmutableFloatingRateName => externalName.equals(other.externalName)
        case _ => false
      }
    }
  }

  override def hashCode(): Int = {
    externalName.hashCode()
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
  override def metaBean(): ImmutableFloatingRateName.Meta = {
    ImmutableFloatingRateName.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
   * @return the value of the property, not empty
   */
  def getExternalName(): String = {
    externalName
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
   * This name matches that used by `IborIndex` or `OvernightIndex`.
   * Typically, multiple `FloatingRateName` names map to one Ibor or Overnight index.
   * @return the value of the property, not empty
   */
  def getIndexName(): String = {
    indexName
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the index.
   * @return the value of the property, not null
   */
  override def getType(): FloatingRateType = {
    `type`
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date offset, in days, optional.
   * This is used when a floating rate name implies a non-standard fixing date offset.
   * This is only used for Ibor Indices, and currently only for DKK CIBOR.
   * @return the optional value of the property, not null
   */
  def getFixingDateOffsetDays(): OptionalInt = {
    if (fixingDateOffsetDays != null) OptionalInt.of(fixingDateOffsetDays.intValue()) else OptionalInt.empty()
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  private[index] def toBuilder(): ImmutableFloatingRateName.Builder = {
    new ImmutableFloatingRateName.Builder(this)
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object ImmutableFloatingRateName {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(ImmutableFloatingRateName.Meta.INSTANCE)
    true
  }

  /**
   * Special suffix that can be used to distinguish averaged indices.
   */
  private val AVERAGE_SUFFIX: String = "-AVG"

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified external name, index name and type.
   *
   * @param externalName  the unique name
   * @param indexName  the name of the index
   * @param type  the type - Ibor, Overnight or Price
   * @return the name
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  // @static because the inherited static FloatingRateName.of suppresses the static forwarder
  @static def of(externalName: String, indexName: String, `type`: FloatingRateType): ImmutableFloatingRateName = {
    new ImmutableFloatingRateName(externalName, indexName, `type`, null)
  }

  /**
   * Obtains an instance from the specified external name, index name and type.
   *
   * @param externalName  the unique name
   * @param indexName  the name of the index
   * @param type  the type - Ibor, Overnight or Price
   * @param fixingDateOffsetDays  the fixing date offset, in days, negative to use the standard
   * @return the name
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @static def of(
      externalName: String,
      indexName: String,
      `type`: FloatingRateType,
      fixingDateOffsetDays: Int): ImmutableFloatingRateName = {

    new ImmutableFloatingRateName(
        externalName, indexName, `type`, if (fixingDateOffsetDays >= 0) Integer.valueOf(fixingDateOffsetDays) else null)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `ImmutableFloatingRateName`.
   * @return the meta-bean, not null
   */
  def meta(): ImmutableFloatingRateName.Meta = {
    ImmutableFloatingRateName.Meta.INSTANCE
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  private[index] def builder(): ImmutableFloatingRateName.Builder = {
    new ImmutableFloatingRateName.Builder()
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `ImmutableFloatingRateName`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `externalName` property.
     */
    private val externalNameProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "externalName", classOf[ImmutableFloatingRateName], classOf[String])
    /**
     * The meta-property for the `indexName` property.
     */
    private val indexNameProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "indexName", classOf[ImmutableFloatingRateName], classOf[String])
    /**
     * The meta-property for the `type` property.
     */
    private val typeProperty: MetaProperty[FloatingRateType] = DirectMetaProperty.ofImmutable(
        this, "type", classOf[ImmutableFloatingRateName], classOf[FloatingRateType])
    /**
     * The meta-property for the `fixingDateOffsetDays` property.
     */
    private val fixingDateOffsetDaysProperty: MetaProperty[Integer] = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffsetDays", classOf[ImmutableFloatingRateName], classOf[Integer])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "externalName",
        "indexName",
        "type",
        "fixingDateOffsetDays")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -1386121994 => // externalName
          externalNameProperty
        case -807707011 => // indexName
          indexNameProperty
        case 3575610 => // type
          typeProperty
        case -594001179 => // fixingDateOffsetDays
          fixingDateOffsetDaysProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): ImmutableFloatingRateName.Builder = {
      new ImmutableFloatingRateName.Builder()
    }

    override def beanType(): Class[? <: ImmutableFloatingRateName] = {
      classOf[ImmutableFloatingRateName]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `externalName` property.
     * @return the meta-property, not null
     */
    def externalName(): MetaProperty[String] = {
      externalNameProperty
    }

    /**
     * The meta-property for the `indexName` property.
     * @return the meta-property, not null
     */
    def indexName(): MetaProperty[String] = {
      indexNameProperty
    }

    /**
     * The meta-property for the `type` property.
     * @return the meta-property, not null
     */
    def `type`(): MetaProperty[FloatingRateType] = {
      typeProperty
    }

    /**
     * The meta-property for the `fixingDateOffsetDays` property.
     * @return the meta-property, not null
     */
    def fixingDateOffsetDays(): MetaProperty[Integer] = {
      fixingDateOffsetDaysProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -1386121994 => // externalName
          bean.asInstanceOf[ImmutableFloatingRateName].getExternalName()
        case -807707011 => // indexName
          bean.asInstanceOf[ImmutableFloatingRateName].getIndexName()
        case 3575610 => // type
          bean.asInstanceOf[ImmutableFloatingRateName].getType()
        case -594001179 => // fixingDateOffsetDays
          bean.asInstanceOf[ImmutableFloatingRateName].fixingDateOffsetDays
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
   * The bean-builder for `ImmutableFloatingRateName`.
   */
  final class Builder private[index] () extends DirectFieldsBeanBuilder[ImmutableFloatingRateName] {

    // field names differ from the property names because Scala does not allow a field and a setter to share a name
    private var _externalName: String = uninitialized
    private var _indexName: String = uninitialized
    private var _type: FloatingRateType = uninitialized
    private var _fixingDateOffsetDays: Integer = uninitialized

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private[index] def this(beanToCopy: ImmutableFloatingRateName) = {
      this()
      this._externalName = beanToCopy.getExternalName()
      this._indexName = beanToCopy.getIndexName()
      this._type = beanToCopy.getType()
      this._fixingDateOffsetDays = beanToCopy.fixingDateOffsetDays
    }

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -1386121994 => // externalName
          _externalName
        case -807707011 => // indexName
          _indexName
        case 3575610 => // type
          _type
        case -594001179 => // fixingDateOffsetDays
          _fixingDateOffsetDays
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -1386121994 => // externalName
          this._externalName = newValue.asInstanceOf[String]
        case -807707011 => // indexName
          this._indexName = newValue.asInstanceOf[String]
        case 3575610 => // type
          this._type = newValue.asInstanceOf[FloatingRateType]
        case -594001179 => // fixingDateOffsetDays
          this._fixingDateOffsetDays = newValue.asInstanceOf[Integer]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def set(property: MetaProperty[?], value: Object): Builder = {
      super.set(property, value)
      this
    }

    override def build(): ImmutableFloatingRateName = {
      new ImmutableFloatingRateName(
          _externalName,
          _indexName,
          _type,
          _fixingDateOffsetDays)
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
     * @param externalName  the new value, not empty
     * @return this, for chaining, not null
     */
    def externalName(externalName: String): Builder = {
      JodaBeanUtils.notEmpty(externalName, "externalName")
      this._externalName = externalName
      this
    }

    /**
     * Sets the root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
     * This name matches that used by `IborIndex` or `OvernightIndex`.
     * Typically, multiple `FloatingRateName` names map to one Ibor or Overnight index.
     * @param indexName  the new value, not empty
     * @return this, for chaining, not null
     */
    def indexName(indexName: String): Builder = {
      JodaBeanUtils.notEmpty(indexName, "indexName")
      this._indexName = indexName
      this
    }

    /**
     * Sets the type of the index.
     * @param type  the new value, not null
     * @return this, for chaining, not null
     */
    def `type`(`type`: FloatingRateType): Builder = {
      JodaBeanUtils.notNull(`type`, "type")
      this._type = `type`
      this
    }

    /**
     * Sets the fixing date offset, in days, optional.
     * This is used when a floating rate name implies a non-standard fixing date offset.
     * This is only used for Ibor Indices, and currently only for DKK CIBOR.
     * @param fixingDateOffsetDays  the new value
     * @return this, for chaining, not null
     */
    def fixingDateOffsetDays(fixingDateOffsetDays: Integer): Builder = {
      this._fixingDateOffsetDays = fixingDateOffsetDays
      this
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(160)
      buf.append("ImmutableFloatingRateName.Builder{")
      buf.append("externalName").append('=').append(JodaBeanUtils.toString(_externalName)).append(',').append(' ')
      buf.append("indexName").append('=').append(JodaBeanUtils.toString(_indexName)).append(',').append(' ')
      buf.append("type").append('=').append(JodaBeanUtils.toString(_type)).append(',').append(' ')
      buf.append("fixingDateOffsetDays").append('=').append(JodaBeanUtils.toString(_fixingDateOffsetDays))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
