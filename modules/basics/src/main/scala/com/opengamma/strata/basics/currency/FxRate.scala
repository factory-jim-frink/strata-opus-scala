/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.Locale
import java.util.Map
import java.util.NoSuchElementException
import java.util.regex.Matcher
import java.util.regex.Pattern

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

import com.google.common.math.DoubleMath
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages

/**
 * A single foreign exchange rate between two currencies, such as 'EUR/USD 1.25'.
 *
 * This represents a rate of foreign exchange. The rate 'EUR/USD 1.25' consists of three
 * elements - the base currency 'EUR', the counter currency 'USD' and the rate '1.25'.
 * When performing a conversion a rate of '1.25' means that '1 EUR = 1.25 USD'.
 *
 * See `CurrencyPair` for the representation that does not contain a rate.
 *
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class FxRate private (
    /**
     * The currency pair.
     * The pair is formed of two parts, the base and the counter.
     * In the pair 'AAA/BBB' the base is 'AAA' and the counter is 'BBB'.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val pair: CurrencyPair,
    /**
     * The rate applicable to the currency pair.
     * One unit of the base currency is exchanged for this amount of the counter currency.
     */
    @(PropertyDefinition @field)(validate = "ArgChecker.notNegativeOrZero", get = "private")
    private val rate: Double)
  extends FxRateProvider with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(pair, "pair")
  ArgChecker.notNegativeOrZero(rate, "rate")
  validate()

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private def validate(): Unit = {
    if (pair.getBase().equals(pair.getCounter()) && rate != 1d) {
      throw new IllegalArgumentException("Conversion rate between identical currencies must be one")
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the inverse rate.
   *
   * The inverse rate has the same currencies but in reverse order.
   * The rate is the reciprocal of the original.
   *
   * @return the inverse pair
   */
  def inverse(): FxRate = {
    new FxRate(pair.inverse(), 1d / rate)
  }

  /**
   * Gets the FX rate for the specified currency pair.
   *
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: `(1 * baseCurrency = fxRate * counterCurrency)`.
   *
   * This will return the rate or inverse rate, or 1 if the two input currencies are the same.
   *
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws java.lang.IllegalArgumentException if no FX rate could be found
   */
  override def fxRate(baseCurrency: Currency, counterCurrency: Currency): Double = {
    if (baseCurrency.equals(counterCurrency)) {
      1d
    } else if (baseCurrency.equals(pair.getBase()) && counterCurrency.equals(pair.getCounter())) {
      rate
    } else if (counterCurrency.equals(pair.getBase()) && baseCurrency.equals(pair.getCounter())) {
      1d / rate
    } else {
      throw new IllegalArgumentException(Messages.format(
          "No FX rate found for {}/{}", baseCurrency, counterCurrency))
    }
  }

  /**
   * Derives an FX rate from two related FX rates.
   *
   * Given two FX rates it is possible to derive another rate if they have a currency in common.
   * For example, given rates for EUR/GBP and EUR/CHF it is possible to derive rates for GBP/CHF.
   * The result will always have a currency pair in the conventional order.
   *
   * The cross is only returned if the two pairs contains three currencies in total.
   * If the inputs are invalid, an exception is thrown.
   *
   *  - AAA/BBB and BBB/CCC - valid, producing AAA/CCC
   *  - AAA/BBB and CCC/BBB - valid, producing AAA/CCC
   *  - AAA/BBB and BBB/AAA - invalid, exception thrown
   *  - AAA/BBB and BBB/BBB - invalid, exception thrown
   *  - AAA/BBB and CCC/DDD - invalid, exception thrown
   *
   * @param other  the other rates
   * @return a set of FX rates derived from these rates and the other rates
   * @throws java.lang.IllegalArgumentException if the cross rate cannot be calculated
   */
  def crossRate(other: FxRate): FxRate = {
    pair.cross(other.pair).map[FxRate](cross => FxRate.computeCross(this, other, cross))
        .orElseThrow[IllegalArgumentException](() => new IllegalArgumentException(Messages.format(
            "Unable to cross when no unique common currency: {} and {}", pair, other.pair)))
  }

  /**
   * Returns an FX rate object representing the market convention rate between the two currencies.
   *
   * If the currency pair is the market convention pair, this method returns `this`, otherwise
   * it returns an `FxRate` with the inverse currency pair and reciprocal rate.
   *
   * @return an FX rate object representing the market convention rate between the two currencies
   */
  def toConventional(): FxRate = {
    if (pair.isConventional()) this else FxRate.of(pair.toConventional(), 1 / rate)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted string version of the currency pair.
   *
   * The format is '\${baseCurrency}/\${counterCurrency} \${rate}'.
   *
   * @return the formatted string
   */
  override def toString(): String = {
    pair.toString() + " " +
        (if (DoubleMath.isMathematicalInteger(rate)) java.lang.Long.toString(rate.toLong) else java.lang.Double.toString(rate))
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): FxRate.Meta = {
    FxRate.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair.
   * The pair is formed of two parts, the base and the counter.
   * In the pair 'AAA/BBB' the base is 'AAA' and the counter is 'BBB'.
   * @return the value of the property, not null
   */
  def getPair(): CurrencyPair = {
    pair
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate applicable to the currency pair.
   * One unit of the base currency is exchanged for this amount of the counter currency.
   * @return the value of the property
   */
  private def getRate(): Double = {
    rate
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[FxRate]
      JodaBeanUtils.equal(pair, other.pair) &&
          JodaBeanUtils.equal(rate, other.rate)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(pair)
    hash = hash * 31 + JodaBeanUtils.hashCode(rate)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object FxRate {

  /**
   * Regular expression to parse the textual format.
   */
  @static private val REGEX_FORMAT: Pattern = Pattern.compile("([A-Z]{3})[/]([A-Z]{3})[ ]([0-9+.-]+)")

  @static private val REGISTERED: Boolean = {
    MetaBean.register(FxRate.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from two currencies.
   *
   * The first currency is the base and the second is the counter.
   * The two currencies may be the same, but if they are then the rate must be one.
   *
   * @param base  the base currency
   * @param counter  the counter currency
   * @param rate  the conversion rate, greater than zero
   * @return the FX rate
   * @throws java.lang.IllegalArgumentException if the rate is invalid
   */
  def of(base: Currency, counter: Currency, rate: Double): FxRate = {
    new FxRate(CurrencyPair.of(base, counter), rate)
  }

  /**
   * Obtains an instance from a currency pair.
   *
   * The two currencies may be the same, but if they are then the rate must be one.
   *
   * @param pair  the currency pair
   * @param rate  the conversion rate, greater than zero
   * @return the FX rate
   * @throws java.lang.IllegalArgumentException if the rate is invalid
   */
  def of(pair: CurrencyPair, rate: Double): FxRate = {
    new FxRate(pair, rate)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a rate from a string with format AAA/BBB RATE.
   *
   * The parsed format is '\${baseCurrency}/\${counterCurrency} \${rate}'.
   * Currency parsing is case insensitive.
   *
   * @param rateStr  the rate as a string AAA/BBB RATE
   * @return the FX rate
   * @throws java.lang.IllegalArgumentException if the FX rate cannot be parsed
   */
  def parse(rateStr: String): FxRate = {
    ArgChecker.notNull(rateStr, "rateStr")
    val matcher: Matcher = REGEX_FORMAT.matcher(rateStr.toUpperCase(Locale.ENGLISH))
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid rate: " + rateStr)
    }
    try {
      val base = Currency.parse(matcher.group(1))
      val counter = Currency.parse(matcher.group(2))
      val rate = java.lang.Double.parseDouble(matcher.group(3))
      new FxRate(CurrencyPair.of(base, counter), rate)
    } catch {
      case ex: RuntimeException =>
        throw new IllegalArgumentException("Unable to parse rate: " + rateStr, ex)
    }
  }

  // computes the cross rate
  private def computeCross(fx1: FxRate, fx2: FxRate, crossPairAC: CurrencyPair): FxRate = {
    // aim is to convert AAA/BBB and BBB/CCC to AAA/CCC
    val currA = crossPairAC.getBase()
    val currC = crossPairAC.getCounter()
    // given the conventional cross rate pair, order the two rates to match
    val crossBaseCurrencyInFx1 = fx1.pair.contains(currA)
    val fxABorBA = if (crossBaseCurrencyInFx1) fx1 else fx2
    val fxBCorCB = if (crossBaseCurrencyInFx1) fx2 else fx1
    // extract the rates, taking the inverse if the pair is in the inverse order
    val rateAB = if (fxABorBA.getPair().getBase().equals(currA)) fxABorBA.rate else 1d / fxABorBA.rate
    val rateBC = if (fxBCorCB.getPair().getCounter().equals(currC)) fxBCorCB.rate else 1d / fxBCorCB.rate
    FxRate.of(crossPairAC, rateAB * rateBC)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `FxRate`.
   * @return the meta-bean, not null
   */
  def meta(): FxRate.Meta = {
    FxRate.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `FxRate`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `pair` property.
     */
    private val pairProperty: MetaProperty[CurrencyPair] = DirectMetaProperty.ofImmutable(
        this, "pair", classOf[FxRate], classOf[CurrencyPair])
    /**
     * The meta-property for the `rate` property.
     */
    private val rateProperty: MetaProperty[java.lang.Double] = DirectMetaProperty.ofImmutable(
        this, "rate", classOf[FxRate], java.lang.Double.TYPE)
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "pair",
        "rate")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 3433178 => // pair
          pairProperty
        case 3493088 => // rate
          rateProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: FxRate] = {
      new FxRate.Builder()
    }

    override def beanType(): Class[? <: FxRate] = {
      classOf[FxRate]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `pair` property.
     * @return the meta-property, not null
     */
    def pair(): MetaProperty[CurrencyPair] = {
      pairProperty
    }

    /**
     * The meta-property for the `rate` property.
     * @return the meta-property, not null
     */
    def rate(): MetaProperty[java.lang.Double] = {
      rateProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 3433178 => // pair
          bean.asInstanceOf[FxRate].getPair()
        case 3493088 => // rate
          java.lang.Double.valueOf(bean.asInstanceOf[FxRate].getRate())
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
   * The bean-builder for `FxRate`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[FxRate] {

    private var pair: CurrencyPair = uninitialized
    private var rate: Double = 0d

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 3433178 => // pair
          pair
        case 3493088 => // rate
          java.lang.Double.valueOf(rate)
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 3433178 => // pair
          this.pair = newValue.asInstanceOf[CurrencyPair]
        case 3493088 => // rate
          this.rate = newValue.asInstanceOf[java.lang.Double].doubleValue()
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): FxRate = {
      new FxRate(
          pair,
          rate)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("FxRate.Builder{")
      buf.append("pair").append('=').append(JodaBeanUtils.toString(pair)).append(',').append(' ')
      buf.append("rate").append('=').append(JodaBeanUtils.toString(java.lang.Double.valueOf(rate)))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
