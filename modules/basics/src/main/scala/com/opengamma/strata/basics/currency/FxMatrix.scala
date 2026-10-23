/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.Arrays
import java.util.Map
import java.util.NoSuchElementException
import java.util.function.BiConsumer
import java.util.stream.Collector
import java.util.stream.Collectors

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

import com.google.common.base.Joiner
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.array.DoubleMatrix
import com.opengamma.strata.collect.tuple.Pair

/**
 * A matrix of foreign exchange rates.
 *
 * This provides a matrix of foreign exchange rates, such that the rate can be queried for any available pair.
 * For example, if the matrix contains the currencies 'USD', 'EUR' and 'GBP', then six rates can be queried,
 * 'EUR/USD', 'GBP/USD', 'EUR/GBP' and the three inverse rates.
 *
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
@SerialVersionUID(1L)
final class FxMatrix private[currency] (
    currenciesInput: Map[Currency, Integer],
    /**
     * The matrix with all the exchange rates. Each row represents the
     * rates required to convert a unit of particular currency to all
     * other currencies in the matrix.
     *
     * If currencies c1 and c2 are assigned indexes i and j respectively
     * in the `currencies` map, then the entry [i][j] is such that
     * 1 unit of currency c1 is worth `rates[i][j]` units of
     * currency c2.
     *
     * If `currencies.get(EUR)` = 0 and `currencies.get(USD)` = 1,
     * then the element `rates[0][1]` is likely to be around
     * 1.40 and `rates[1][0]` around 0.7142. The rate `rates[1][0]`
     * will be computed from fxRate[0][1] when the object is constructed
     * by the builder. All the element of the matrix are meaningful and coherent.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val rates: DoubleMatrix)
  extends FxRateProvider with ImmutableBean with Serializable {

  JodaBeanUtils.notNull(currenciesInput, "currencies")
  JodaBeanUtils.notNull(rates, "rates")

  /**
   * The map between the currencies and their position within the
   * `rates` array. Generally the position reflects the order
   * in which the currencies were added, so the first currency added
   * will be assigned 0, the second 1 etc.
   *
   * An ImmutableMap is used so that the currencies are correctly
   * ordered when the `toString()` method is called.
   */
  @(PropertyDefinition @field)(validate = "notNull", get = "")
  private val currencies: ImmutableMap[Currency, Integer] = ImmutableMap.copyOf(currenciesInput)

  //-------------------------------------------------------------------------
  /**
   * Returns the set of currencies held within this matrix.
   *
   * @return the currencies in this matrix
   */
  def getCurrencies(): ImmutableSet[Currency] = {
    currencies.keySet()
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the FX rate for the specified currency pair.
   *
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: `(1 * baseCurrency = fxRate * counterCurrency)`.
   *
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws java.lang.IllegalArgumentException if no FX rate could be found
   */
  override def fxRate(baseCurrency: Currency, counterCurrency: Currency): Double = {
    if (baseCurrency.equals(counterCurrency)) {
      1d
    } else {
      val index1: Integer = currencies.get(baseCurrency)
      val index2: Integer = currencies.get(counterCurrency)
      if (index1 != null && index2 != null) {
        rates.get(index1.intValue(), index2.intValue())
      } else {
        throw new IllegalArgumentException(Messages.format(
            "No FX rate found for {}/{}, matrix only contains rates for {}", baseCurrency, counterCurrency, currencies.keySet()))
      }
    }
  }

  /**
   * Converts a `CurrencyAmount` into an amount in the specified
   * currency using the rates in this matrix.
   *
   * @param amount  the `CurrencyAmount` to be converted
   * @param targetCurrency  the currency to convert the `CurrencyAmount` to
   * @return the amount converted to the requested currency
   */
  def convert(amount: CurrencyAmount, targetCurrency: Currency): CurrencyAmount = {
    ArgChecker.notNull(amount, "amount")
    ArgChecker.notNull(targetCurrency, "targetCurrency")
    // Only do conversion if we need to
    val originalCurrency = amount.getCurrency()
    if (originalCurrency.equals(targetCurrency)) {
      amount
    } else {
      CurrencyAmount.of(targetCurrency, convert(amount.getAmount(), originalCurrency, targetCurrency))
    }
  }

  /**
   * Converts a `MultipleCurrencyAmount` into an amount in the
   * specified currency using the rates in this matrix.
   *
   * @param amount  the `MultipleCurrencyAmount` to be converted
   * @param targetCurrency  the currency to convert all entries to
   * @return the total amount in the requested currency
   */
  def convert(amount: MultiCurrencyAmount, targetCurrency: Currency): CurrencyAmount = {
    ArgChecker.notNull(amount, "amount")
    ArgChecker.notNull(targetCurrency, "targetCurrency")

    // We could do this using the currency amounts but to
    // avoid creating extra objects we'll use doubles
    val total = amount.getAmounts()
        .stream()
        .mapToDouble(ca => convert(ca.getAmount(), ca.getCurrency(), targetCurrency))
        .sum()
    CurrencyAmount.of(targetCurrency, total)
  }

  //-------------------------------------------------------------------------
  /**
   * Merges the entries from the other matrix into this one.
   *
   * The other matrix should have at least one currency in common with this one.
   * The additional currencies from the other matrix are added one by one and
   * the exchange rate data created is coherent with some data in this matrix.
   *
   * Note that if the other matrix has more than one currency in common with
   * this one, and the rates for pairs of those currencies are different to
   * the equivalents in this matrix, then the rates between the additional
   * currencies is this matrix will differ from those in the original.
   *
   * @param other  the matrix to be merged into this one
   * @return a new matrix containing the rates from this matrix
   *   plus any rates for additional currencies from the other matrix
   */
  def merge(other: FxMatrix): FxMatrix = {
    toBuilder().merge(other.toBuilder()).build()
  }

  /**
   * Creates a new builder using the data from this matrix to
   * create a set of initial entries.
   *
   * @return a new builder containing the data from this matrix
   */
  def toBuilder(): FxMatrixBuilder = {
    new FxMatrixBuilder(currencies, rates.toArray())
  }

  override def toString(): String = {
    "FxMatrix[" + Joiner.on(", ").join(getCurrencies()) + " : " +
        Arrays.stream(rates.toArrayUnsafe()).map[String](row => Arrays.toString(row)).collect(Collectors.joining(",")) + "]"
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): FxMatrix.Meta = {
    FxMatrix.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the matrix with all the exchange rates. Each row represents the
   * rates required to convert a unit of particular currency to all
   * other currencies in the matrix.
   *
   * If currencies c1 and c2 are assigned indexes i and j respectively
   * in the `currencies` map, then the entry [i][j] is such that
   * 1 unit of currency c1 is worth `rates[i][j]` units of
   * currency c2.
   *
   * If `currencies.get(EUR)` = 0 and `currencies.get(USD)` = 1,
   * then the element `rates[0][1]` is likely to be around
   * 1.40 and `rates[1][0]` around 0.7142. The rate `rates[1][0]`
   * will be computed from fxRate[0][1] when the object is constructed
   * by the builder. All the element of the matrix are meaningful and coherent.
   * @return the value of the property, not null
   */
  def getRates(): DoubleMatrix = {
    rates
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[FxMatrix]
      JodaBeanUtils.equal(currencies, other.currencies) &&
          JodaBeanUtils.equal(rates, other.rates)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(currencies)
    hash = hash * 31 + JodaBeanUtils.hashCode(rates)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object FxMatrix {

  /**
   * An empty FX matrix containing neither currencies nor rates.
   */
  @static private val EMPTY: FxMatrix = builder().build()

  @static private val REGISTERED: Boolean = {
    MetaBean.register(FxMatrix.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty FX matrix.
   *
   * The result contains no currencies or rates.
   *
   * @return an empty matrix
   */
  def empty(): FxMatrix = {
    EMPTY
  }

  /**
   * Obtains an instance containing a single FX rate.
   *
   * This is most likely to be used in testing.
   *
   * An invocation of the method with `FxMatrix.of(CurrencyPair.of(GBP, USD), 1.6)`
   * indicates that 1 pound sterling is worth 1.6 US dollars.
   * The matrix can also be queried for the reverse rate, from USD to GBP.
   *
   * @param currencyPair  the currency pair to be added
   * @param rate  the FX rate between the base currency of the pair and the
   *   counter currency. The rate indicates the value of one unit of the base
   *   currency in terms of the counter currency.
   * @return a matrix containing the single FX rate
   */
  def of(currencyPair: CurrencyPair, rate: Double): FxMatrix = {
    FxMatrix.of(currencyPair.getBase(), currencyPair.getCounter(), rate)
  }

  /**
   * Obtains an instance containing a single FX rate.
   *
   * This is most likely to be used in testing.
   *
   * An invocation of the method with `FxMatrix.of(GBP, USD, 1.6)`
   * indicates that 1 pound sterling is worth 1.6 US dollars.
   * The matrix can also be queried for the reverse rate, from USD to GBP.
   *
   * @param ccy1  the first currency of the pair
   * @param ccy2  the second currency of the pair
   * @param rate  the FX rate between the first currency and the second currency.
   *   The rate indicates the value of one unit of the first currency in terms
   *   of the second currency.
   * @return a matrix containing the single FX rate
   */
  def of(ccy1: Currency, ccy2: Currency, rate: Double): FxMatrix = {
    new FxMatrixBuilder().addRate(ccy1, ccy2, rate).build()
  }

  /**
   * Creates a builder that can be used to build instances of `FxMatrix`.
   *
   * @return a new builder
   */
  def builder(): FxMatrixBuilder = {
    new FxMatrixBuilder()
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a `Collector` that allows a `Map.Entry` of currency pair to rate
   * to be streamed and collected into a new `FxMatrix`.
   *
   * @return a collector for creating an `FxMatrix` from a stream
   */
  def entriesToFxMatrix(): Collector[? >: Map.Entry[CurrencyPair, java.lang.Double], FxMatrixBuilder, FxMatrix] = {
    collector[Map.Entry[CurrencyPair, java.lang.Double]](
        (builder, entry) => builder.addRate(entry.getKey(), entry.getValue().doubleValue()))
  }

  /**
   * Creates a `Collector` that allows a collection of pairs each containing
   * a currency pair and a rate to be streamed and collected into a new `FxMatrix`.
   *
   * @return a collector for creating an `FxMatrix` from a stream
   */
  def pairsToFxMatrix(): Collector[? >: Pair[CurrencyPair, java.lang.Double], FxMatrixBuilder, FxMatrix] = {
    collector[Pair[CurrencyPair, java.lang.Double]](
        (builder, pair) => builder.addRate(pair.getFirst(), pair.getSecond().doubleValue()))
  }

  private def collector[T](accumulator: BiConsumer[FxMatrixBuilder, T]): Collector[T, FxMatrixBuilder, FxMatrix] = {
    Collector.of[T, FxMatrixBuilder, FxMatrix](
        () => FxMatrix.builder(),
        accumulator,
        (builder, other) => builder.merge(other),
        builder => builder.build())
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `FxMatrix`.
   * @return the meta-bean, not null
   */
  def meta(): FxMatrix.Meta = {
    FxMatrix.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `FxMatrix`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `currencies` property.
     */
    private val currenciesProperty: MetaProperty[ImmutableMap[Currency, Integer]] = DirectMetaProperty.ofImmutable(
        this, "currencies", classOf[FxMatrix], classOf[ImmutableMap[?, ?]].asInstanceOf[Class[ImmutableMap[Currency, Integer]]])
    /**
     * The meta-property for the `rates` property.
     */
    private val ratesProperty: MetaProperty[DoubleMatrix] = DirectMetaProperty.ofImmutable(
        this, "rates", classOf[FxMatrix], classOf[DoubleMatrix])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "currencies",
        "rates")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -1089470353 => // currencies
          currenciesProperty
        case 108285843 => // rates
          ratesProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: FxMatrix] = {
      new FxMatrix.Builder()
    }

    override def beanType(): Class[? <: FxMatrix] = {
      classOf[FxMatrix]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `currencies` property.
     * @return the meta-property, not null
     */
    def currencies(): MetaProperty[ImmutableMap[Currency, Integer]] = {
      currenciesProperty
    }

    /**
     * The meta-property for the `rates` property.
     * @return the meta-property, not null
     */
    def rates(): MetaProperty[DoubleMatrix] = {
      ratesProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -1089470353 => // currencies
          bean.asInstanceOf[FxMatrix].currencies
        case 108285843 => // rates
          bean.asInstanceOf[FxMatrix].getRates()
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
   * The bean-builder for `FxMatrix`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[FxMatrix] {

    private var currencies: Map[Currency, Integer] = ImmutableMap.of()
    private var rates: DoubleMatrix = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -1089470353 => // currencies
          currencies
        case 108285843 => // rates
          rates
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -1089470353 => // currencies
          this.currencies = newValue.asInstanceOf[Map[Currency, Integer]]
        case 108285843 => // rates
          this.rates = newValue.asInstanceOf[DoubleMatrix]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): FxMatrix = {
      new FxMatrix(
          currencies,
          rates)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("FxMatrix.Builder{")
      buf.append("currencies").append('=').append(JodaBeanUtils.toString(currencies)).append(',').append(' ')
      buf.append("rates").append('=').append(JodaBeanUtils.toString(rates))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
