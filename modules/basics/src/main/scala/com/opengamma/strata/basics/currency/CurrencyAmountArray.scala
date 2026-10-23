/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.function.IntFunction
import java.util.stream.Stream

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
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.array.DoubleArray

/**
 * An array of currency amounts with the same currency.
 *
 * This represents an array of `CurrencyAmount` in a single currency.
 * Internally, it stores the data using a single `Currency` and a `DoubleArray`.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class CurrencyAmountArray private (
    /**
     * The currency.
     * All amounts have the same currency.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val currency: Currency,
    /**
     * The values.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val values: DoubleArray)
  extends FxConvertible[CurrencyAmountArray] with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(currency, "currency")
  JodaBeanUtils.notNull(values, "values")

  //-------------------------------------------------------------------------
  /**
   * Gets the size of the array.
   *
   * @return the array size
   */
  def size(): Int = {
    values.size()
  }

  /**
   * Gets the amount at the specified index.
   *
   * @param index  the zero-based index to retrieve
   * @return the amount at the specified index
   */
  def get(index: Int): CurrencyAmount = {
    CurrencyAmount.of(currency, values.get(index))
  }

  /**
   * Returns a stream of the amounts.
   *
   * @return a stream of the amounts
   */
  def stream(): Stream[CurrencyAmount] = {
    values.stream().mapToObj[CurrencyAmount](amount => CurrencyAmount.of(currency, amount))
  }

  override def convertedTo(resultCurrency: Currency, fxRateProvider: FxRateProvider): CurrencyAmountArray = {
    if (currency.equals(resultCurrency)) {
      this
    } else {
      val fxRate = fxRateProvider.fxRate(currency, resultCurrency)
      val convertedValues = values.multipliedBy(fxRate)
      new CurrencyAmountArray(resultCurrency, convertedValues)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new array containing the values from this array added to the values in the other array.
   *
   * The amounts are added to the matching element in this array.
   * The currency must be the same as the currency of this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added to the values in the other array
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes or different currencies
   */
  def plus(other: CurrencyAmountArray): CurrencyAmountArray = {
    if (other.size() != size()) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}",
          Integer.valueOf(size()),
          Integer.valueOf(other.size())))
    }
    if (!other.currency.equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, other.currency))
    }
    CurrencyAmountArray.of(currency, values.plus(other.values))
  }

  /**
   * Returns a new array containing the values from this array with the specified amount added.
   *
   * The amount is added to each element in this array.
   * The currency must be the same as the currency of this array.
   *
   * @param amount  the amount to add
   * @return a new array containing the values from this array with the specified amount added
   * @throws java.lang.IllegalArgumentException if the array and the amount have different currencies
   */
  def plus(amount: CurrencyAmount): CurrencyAmountArray = {
    if (!amount.getCurrency().equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, amount.getCurrency()))
    }
    CurrencyAmountArray.of(currency, values.plus(amount.getAmount()))
  }

  /**
   * Returns a new array containing the values from this array with the values from the other array subtracted.
   *
   * The amounts are subtracted from the matching element in this array.
   * The currency must be the same as the currency of this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array with the values from the other array subtracted
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes or different currencies
   */
  def minus(other: CurrencyAmountArray): CurrencyAmountArray = {
    if (other.size() != size()) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}",
          Integer.valueOf(size()),
          Integer.valueOf(other.size())))
    }
    if (!other.currency.equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, other.currency))
    }
    CurrencyAmountArray.of(currency, values.minus(other.values))
  }

  /**
   * Returns a new array containing the values from this array with the specified amount subtracted.
   *
   * The amount is subtracted from each element in this array.
   * The currency must be the same as the currency of this array.
   *
   * @param amount  the amount to subtract
   * @return a new array containing the values from this array with the specified amount subtracted
   * @throws java.lang.IllegalArgumentException if the array and the amount have different currencies
   */
  def minus(amount: CurrencyAmount): CurrencyAmountArray = {
    if (!amount.getCurrency().equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, amount.getCurrency()))
    }
    CurrencyAmountArray.of(currency, values.minus(amount.getAmount()))
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): CurrencyAmountArray.Meta = {
    CurrencyAmountArray.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency.
   * All amounts have the same currency.
   * @return the value of the property, not null
   */
  def getCurrency(): Currency = {
    currency
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the values.
   * @return the value of the property, not null
   */
  def getValues(): DoubleArray = {
    values
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[CurrencyAmountArray]
      JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(values, other.values)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(currency)
    hash = hash * 31 + JodaBeanUtils.hashCode(values)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(96)
    buf.append("CurrencyAmountArray{")
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ')
    buf.append("values").append('=').append(JodaBeanUtils.toString(values))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object CurrencyAmountArray {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(CurrencyAmountArray.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param currency  the currency of the values
   * @param values  the values
   * @return an instance with the specified currency and values
   */
  def of(currency: Currency, values: DoubleArray): CurrencyAmountArray = {
    new CurrencyAmountArray(currency, values)
  }

  /**
   * Obtains an instance from the specified list of amounts.
   *
   * All amounts must have the same currency.
   *
   * @param amounts  the amounts, at least size one
   * @return an instance with the specified amounts
   * @throws java.lang.IllegalArgumentException if multiple currencies are found
   */
  def of(amounts: List[CurrencyAmount]): CurrencyAmountArray = {
    val currency: Currency = amounts.stream()
        .map[Currency](ca => ca.getCurrency())
        .distinct()
        .reduce(Guavate.ensureOnlyOne[Currency]())
        .get()
    val values: Array[Double] = amounts.stream()
        .mapToDouble(ca => ca.getAmount())
        .toArray()
    new CurrencyAmountArray(currency, DoubleArray.ofUnsafe(values))
  }

  /**
   * Obtains an instance using a function to create the entries.
   *
   * The function is passed the index and returns the `CurrencyAmount` for that index.
   *
   * In some cases it may be possible to specify the currency with a function providing a `double`.
   * To do this, use `DoubleArray#of(int, java.util.function.IntToDoubleFunction)` and
   * then call `of(Currency, DoubleArray)`.
   *
   * @param size  the number of elements, at least size one
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws java.lang.IllegalArgumentException is size is zero or less
   */
  def of(size: Int, valueFunction: IntFunction[CurrencyAmount]): CurrencyAmountArray = {
    ArgChecker.notNegativeOrZero(size, "size")
    val array = new Array[Double](size)
    val ca0 = valueFunction.apply(0)
    val currency = ca0.getCurrency()
    array(0) = ca0.getAmount()
    var i = 1
    while (i < size) {
      val ca = valueFunction.apply(i)
      if (!ca.getCurrency().equals(currency)) {
        throw new IllegalArgumentException(Messages.format("Currencies differ: {} and {}", currency, ca.getCurrency()))
      }
      array(i) = ca.getAmount()
      i += 1
    }
    new CurrencyAmountArray(currency, DoubleArray.ofUnsafe(array))
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `CurrencyAmountArray`.
   * @return the meta-bean, not null
   */
  def meta(): CurrencyAmountArray.Meta = {
    CurrencyAmountArray.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `CurrencyAmountArray`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `currency` property.
     */
    private val currencyProperty: MetaProperty[Currency] = DirectMetaProperty.ofImmutable(
        this, "currency", classOf[CurrencyAmountArray], classOf[Currency])
    /**
     * The meta-property for the `values` property.
     */
    private val valuesProperty: MetaProperty[DoubleArray] = DirectMetaProperty.ofImmutable(
        this, "values", classOf[CurrencyAmountArray], classOf[DoubleArray])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "values")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 575402001 => // currency
          currencyProperty
        case -823812830 => // values
          valuesProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: CurrencyAmountArray] = {
      new CurrencyAmountArray.Builder()
    }

    override def beanType(): Class[? <: CurrencyAmountArray] = {
      classOf[CurrencyAmountArray]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `currency` property.
     * @return the meta-property, not null
     */
    def currency(): MetaProperty[Currency] = {
      currencyProperty
    }

    /**
     * The meta-property for the `values` property.
     * @return the meta-property, not null
     */
    def values(): MetaProperty[DoubleArray] = {
      valuesProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 575402001 => // currency
          bean.asInstanceOf[CurrencyAmountArray].getCurrency()
        case -823812830 => // values
          bean.asInstanceOf[CurrencyAmountArray].getValues()
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
    private[currency] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `CurrencyAmountArray`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[CurrencyAmountArray] {

    private var currency: Currency = uninitialized
    private var values: DoubleArray = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 575402001 => // currency
          currency
        case -823812830 => // values
          values
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 575402001 => // currency
          this.currency = newValue.asInstanceOf[Currency]
        case -823812830 => // values
          this.values = newValue.asInstanceOf[DoubleArray]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): CurrencyAmountArray = {
      new CurrencyAmountArray(
          currency,
          values)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("CurrencyAmountArray.Builder{")
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ')
      buf.append("values").append('=').append(JodaBeanUtils.toString(values))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
