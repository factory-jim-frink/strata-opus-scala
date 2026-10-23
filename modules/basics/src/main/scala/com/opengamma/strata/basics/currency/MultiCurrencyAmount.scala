/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.io.Serializable
import java.util.Arrays
import java.util.HashMap
import java.util.Map
import java.util.NoSuchElementException
import java.util.SortedSet
import java.util.function.DoubleUnaryOperator
import java.util.function.UnaryOperator
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs

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

import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.MapStream

/**
 * A map of currency amounts keyed by currency.
 *
 * This is a container holding multiple `CurrencyAmount` instances.
 * The amounts do not necessarily have the same worth or value in each currency.
 *
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class MultiCurrencyAmount private (
    // the choice of a set as the internal storage is driven by serialization concerns
    // the ideal storage form would be Map<Currency, CurrencyAmount> but this
    // would duplicate the currency in the serialized form
    // a set was chosen as a suitable middle ground
    /**
     * The set of currency amounts.
     * Each currency will occur only once, as per a map keyed by currency.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val amounts: ImmutableSortedSet[CurrencyAmount])
  extends FxConvertible[CurrencyAmount] with ImmutableBean with Serializable {

  // the primary constructor is used where the input is already validated

  private def this(amounts: SortedSet[CurrencyAmount]) = {
    this(MultiCurrencyAmount.copyNotNull(amounts))
    validate()
  }

  /**
   * Validate against duplicate currencies.
   */
  @ImmutableValidator
  private def validate(): Unit = {
    val currencyCount = amounts.stream()
        .map[CurrencyAmount](ca => ArgChecker.notNullItem(ca))
        .map[Currency](ca => ca.getCurrency())
        .distinct()
        .count()
    if (currencyCount < amounts.size()) {
      throw new IllegalArgumentException("Duplicate currency not allowed: " + amounts)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of stored currencies.
   *
   * @return the set of currencies in this amount
   */
  def getCurrencies(): ImmutableSet[Currency] = {
    amounts.stream()
        .map[Currency](ca => ca.getCurrency())
        .collect(Guavate.toImmutableSet[Currency]())
  }

  /**
   * Gets the number of stored amounts.
   *
   * @return the number of amounts
   */
  def size(): Int = {
    amounts.size()
  }

  /**
   * Checks if this multi-amount contains an amount for the specified currency.
   *
   * @param currency  the currency to find
   * @return true if this amount contains a value for the currency
   */
  def contains(currency: Currency): Boolean = {
    ArgChecker.notNull(currency, "currency")
    amounts.stream().anyMatch(ca => ca.getCurrency().equals(currency))
  }

  /**
   * Gets the `CurrencyAmount` for the specified currency, throwing an exception if not found.
   *
   * @param currency  the currency to find an amount for
   * @return the amount
   * @throws java.lang.IllegalArgumentException if the currency is not found
   */
  def getAmount(currency: Currency): CurrencyAmount = {
    ArgChecker.notNull(currency, "currency")
    amounts.stream()
        .filter(ca => ca.getCurrency().equals(currency))
        .findFirst()
        .orElseThrow[IllegalArgumentException](() => new IllegalArgumentException("Unknown currency " + currency))
  }

  /**
   * Gets the `CurrencyAmount` for the specified currency, returning zero if not found.
   *
   * @param currency  the currency to find an amount for
   * @return the amount
   */
  def getAmountOrZero(currency: Currency): CurrencyAmount = {
    ArgChecker.notNull(currency, "currency")
    amounts.stream()
        .filter(ca => ca.getCurrency().equals(currency))
        .findFirst()
        .orElseGet(() => CurrencyAmount.zero(currency))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   * If the currency is already present, the amount is added to the existing amount.
   * If the currency is not yet present, the currency-amount is added to the map.
   * The addition uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param currency  the currency to add to
   * @param amountToAdd  the amount to add
   * @return an amount based on this with the specified amount added
   */
  def plus(currency: Currency, amountToAdd: Double): MultiCurrencyAmount = {
    plus(CurrencyAmount.of(currency, amountToAdd))
  }

  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   * If the currency is already present, the amount is added to the existing amount.
   * If the currency is not yet present, the currency-amount is added to the map.
   * The addition uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add
   * @return an amount based on this with the specified amount added
   */
  def plus(amountToAdd: CurrencyAmount): MultiCurrencyAmount = {
    ArgChecker.notNull(amountToAdd, "amountToAdd")
    Stream.concat[CurrencyAmount](amounts.stream(), Stream.of[CurrencyAmount](amountToAdd))
        .collect(MultiCurrencyAmount.toMultiCurrencyAmount())
  }

  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount added.
   *
   * This adds the specified amount to this monetary amount, returning a new object.
   * If the currency is already present, the amount is added to the existing amount.
   * If the currency is not yet present, the currency-amount is added to the map.
   * The addition uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToAdd  the amount to add
   * @return an amount based on this with the specified amount added
   */
  def plus(amountToAdd: MultiCurrencyAmount): MultiCurrencyAmount = {
    ArgChecker.notNull(amountToAdd, "amountToAdd")
    Stream.concat[CurrencyAmount](amounts.stream(), amountToAdd.stream())
        .collect(MultiCurrencyAmount.toMultiCurrencyAmount())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount subtracted.
   *
   * This subtracts the specified amount from this monetary amount, returning a new object.
   * If the currency is already present, the amount is subtracted from the existing amount.
   * If the currency is not yet present, the negated amount is included.
   * The subtraction uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param currency  the currency to subtract from
   * @param amountToAdd  the amount to subtract
   * @return an amount based on this with the specified amount subtracted
   */
  def minus(currency: Currency, amountToAdd: Double): MultiCurrencyAmount = {
    plus(CurrencyAmount.of(currency, -amountToAdd))
  }

  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount subtracted.
   *
   * This subtracts the specified amount from this monetary amount, returning a new object.
   * If the currency is already present, the amount is subtracted from the existing amount.
   * If the currency is not yet present, the negated amount is included.
   * The subtraction uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract
   * @return an amount based on this with the specified amount subtracted
   */
  def minus(amountToSubtract: CurrencyAmount): MultiCurrencyAmount = {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract")
    plus(amountToSubtract.negated())
  }

  /**
   * Returns a copy of this `MultiCurrencyAmount` with the specified amount subtracted.
   *
   * This subtracts the specified amount from this monetary amount, returning a new object.
   * If the currency is already present, the amount is subtracted from the existing amount.
   * If the currency is not yet present, the negated amount is included.
   * The subtraction uses standard `double` arithmetic.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param amountToSubtract  the amount to subtract
   * @return an amount based on this with the specified amount subtracted
   */
  def minus(amountToSubtract: MultiCurrencyAmount): MultiCurrencyAmount = {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract")
    plus(amountToSubtract.negated())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this `MultiCurrencyAmount` with all the amounts multiplied by the factor.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param factor  the multiplicative factor
   * @return an amount based on this with all the amounts multiplied by the factor
   */
  def multipliedBy(factor: Double): MultiCurrencyAmount = {
    mapAmounts(a => a * factor)
  }

  /**
   * Returns a copy of this `CurrencyAmount` with the amount negated.
   *
   * This takes this amount and negates it. If any amount is 0.0 or -0.0 the negated amount is 0.0.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return an amount based on this with the amount negated
   */
  def negated(): MultiCurrencyAmount = {
    // Zero is treated as a special case to avoid creating -0.0 which produces surprising equality behaviour
    mapAmounts(a => if (a == 0d) 0d else -a)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the currency amounts.
   *
   * This provides access to the entire set of amounts.
   *
   * @return a stream over the individual amounts
   */
  def stream(): Stream[CurrencyAmount] = {
    amounts.stream()
  }

  /**
   * Applies an operation to the amounts.
   *
   * This is generally used to apply a mathematical operation to the amounts.
   * For example, the operator could multiply the amounts by a constant, or take the inverse.
   * {{{
   *   multiplied = base.mapAmount(value -> value * 3);
   * }}}
   *
   * @param mapper  the operator to be applied to the amounts
   * @return a copy of this amount with the mapping applied to the original amounts
   */
  def mapAmounts(mapper: DoubleUnaryOperator): MultiCurrencyAmount = {
    ArgChecker.notNull(mapper, "mapper")
    amounts.stream()
        .map[CurrencyAmount](ca => ca.mapAmount(mapper))
        .collect(MultiCurrencyAmount.collectorInternal())
  }

  /**
   * Applies an operation to the currency amounts.
   *
   * The operator is called once for each currency in this amount.
   * The operator may return an amount with a different currency.
   * The result will be the total of the altered amounts.
   *
   * @param operator  the operator to be applied to the amounts
   * @return a copy of this amount with the mapping applied to the original amounts
   */
  def mapCurrencyAmounts(operator: UnaryOperator[CurrencyAmount]): MultiCurrencyAmount = {
    ArgChecker.notNull(operator, "operator")
    amounts.stream()
        .map[CurrencyAmount](ca => operator.apply(ca))
        .collect(MultiCurrencyAmount.toMultiCurrencyAmount())
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this amount to an equivalent amount the specified currency.
   *
   * The result will be expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   *
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, which should be expressed in the specified currency
   * @throws java.lang.RuntimeException if no FX rate could be found
   */
  override def convertedTo(resultCurrency: Currency, rateProvider: FxRateProvider): CurrencyAmount = {
    if (amounts.size() == 1) {
      amounts.first().convertedTo(resultCurrency, rateProvider)
    } else {
      var total = 0d
      val it = amounts.iterator()
      while (it.hasNext()) {
        val amount = it.next()
        total += rateProvider.convert(amount.getAmount(), amount.getCurrency(), resultCurrency)
      }
      CurrencyAmount.of(resultCurrency, total)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this `MultiCurrencyAmount` to a map keyed by currency.
   *
   * @return the amounts in a map keyed by currency
   */
  def toMap(): ImmutableSortedMap[Currency, java.lang.Double] = {
    amounts.stream()
        .collect(Guavate.toImmutableSortedMap[CurrencyAmount, Currency, java.lang.Double](
            ca => ca.getCurrency(), ca => java.lang.Double.valueOf(ca.getAmount())))
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the amount as a string.
   *
   * The format includes each currency-amount.
   *
   * @return the currency amount
   */
  override def toString(): String = {
    amounts.toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): MultiCurrencyAmount.Meta = {
    MultiCurrencyAmount.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of currency amounts.
   * Each currency will occur only once, as per a map keyed by currency.
   * @return the value of the property, not null
   */
  def getAmounts(): ImmutableSortedSet[CurrencyAmount] = {
    amounts
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[MultiCurrencyAmount]
      JodaBeanUtils.equal(amounts, other.amounts)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(amounts)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object MultiCurrencyAmount {

  /**
   * An empty instance.
   */
  @static private val EMPTY: MultiCurrencyAmount = new MultiCurrencyAmount(ImmutableSortedSet.of[CurrencyAmount]())

  @static private val REGISTERED: Boolean = {
    MetaBean.register(MultiCurrencyAmount.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty `MultiCurrencyAmount`.
   *
   * @return the empty instance
   */
  def empty(): MultiCurrencyAmount = {
    EMPTY
  }

  /**
   * Obtains an instance from a currency and amount.
   *
   * @param currency  the currency
   * @param amount  the amount
   * @return the amount
   */
  def of(currency: Currency, amount: Double): MultiCurrencyAmount = {
    ArgChecker.notNull(currency, "currency")
    new MultiCurrencyAmount(ImmutableSortedSet.of[CurrencyAmount](CurrencyAmount.of(currency, amount)))
  }

  /**
   * Obtains an instance from an array of `CurrencyAmount` objects.
   *
   * It is an error for the input to contain the same currency twice.
   *
   * @param amounts  the amounts
   * @return the amount
   */
  @varargs
  def of(amounts: CurrencyAmount*): MultiCurrencyAmount = {
    ArgChecker.notNull(amounts, "amounts")
    if (amounts.length == 0) {
      EMPTY
    } else {
      of(Arrays.asList[CurrencyAmount](amounts*))
    }
  }

  /**
   * Obtains an instance from a list of `CurrencyAmount` objects.
   *
   * It is an error for the input to contain the same currency twice.
   *
   * @param amounts  the amounts
   * @return the amount
   */
  def of(amounts: java.lang.Iterable[CurrencyAmount]): MultiCurrencyAmount = {
    ArgChecker.noNulls(amounts, "amounts")
    val map: Map[Currency, CurrencyAmount] = new HashMap[Currency, CurrencyAmount]()
    val it = amounts.iterator()
    while (it.hasNext()) {
      val amount = it.next()
      if (map.put(amount.getCurrency(), amount) != null) {
        throw new IllegalArgumentException("Currency is duplicated: " + amount.getCurrency())
      }
    }
    new MultiCurrencyAmount(ImmutableSortedSet.copyOf[CurrencyAmount](map.values()))
  }

  /**
   * Obtains an instance from a map of currency to amount.
   *
   * @param map  the map of currency to amount
   * @return the amount
   */
  def of(map: Map[Currency, java.lang.Double]): MultiCurrencyAmount = {
    ArgChecker.noNulls(map, "map")
    MapStream.of(map)
        .map[CurrencyAmount]((ccy, amount) => CurrencyAmount.of(ccy, amount.doubleValue()))
        .collect(MultiCurrencyAmount.collectorInternal())
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the total of a list of `CurrencyAmount` objects.
   *
   * If the input contains the same currency more than once, the amounts are added together.
   * For example, an input of (EUR 100, EUR 200, CAD 100) would result in (EUR 300, CAD 100).
   *
   * @param amounts  the amounts
   * @return the amount
   */
  def total(amounts: java.lang.Iterable[CurrencyAmount]): MultiCurrencyAmount = {
    ArgChecker.notNull(amounts, "amounts")
    Guavate.stream(amounts).collect(toMultiCurrencyAmount())
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a collector that can be used to create a multi-currency amount from a stream of amounts.
   *
   * If the input contains the same currency more than once, the amounts are added together.
   * For example, an input of (EUR 100, EUR 200, CAD 100) would result in (EUR 300, CAD 100).
   *
   * @return the collector
   */
  def toMultiCurrencyAmount(): Collector[CurrencyAmount, ?, MultiCurrencyAmount] = {
    Collector.of[CurrencyAmount, Map[Currency, CurrencyAmount], MultiCurrencyAmount](
        // accumulate into a map
        () => new HashMap[Currency, CurrencyAmount](),
        // merge two CurrencyAmounts if same currency
        (map, ca) => {
          map.merge(ArgChecker.notNull(ca, "amount").getCurrency(), ca, (a, b) => a.plus(b))
          ()
        },
        // combine two maps
        (map1, map2) => {
          map2.values().forEach(ca2 => {
            map1.merge(ca2.getCurrency(), ca2, (a, b) => a.plus(b))
            ()
          })
          map1
        },
        // convert to MultiCurrencyAmount
        map => new MultiCurrencyAmount(ImmutableSortedSet.copyOf[CurrencyAmount](map.values())),
        Collector.Characteristics.UNORDERED)
  }

  /**
   * Returns a collector that can be used to create a multi-currency amount
   * from a stream of amounts where each amount has a different currency.
   *
   * Each amount in the stream must have a different currency.
   *
   * @return the collector
   */
  private def collectorInternal(): Collector[CurrencyAmount, ?, MultiCurrencyAmount] = {
    // this method must not be exposed publicly as misuse creates an instance with invalid state
    // it exists because when used internally it offers better performance than collector()
    Collectors.collectingAndThen(
        Guavate.toImmutableSortedSet[CurrencyAmount](),
        (set: ImmutableSortedSet[CurrencyAmount]) => new MultiCurrencyAmount(set))
  }

  // the null check and copy performed by the validating constructor
  private def copyNotNull(amounts: SortedSet[CurrencyAmount]): ImmutableSortedSet[CurrencyAmount] = {
    JodaBeanUtils.notNull(amounts, "amounts")
    ImmutableSortedSet.copyOfSorted(amounts)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `MultiCurrencyAmount`.
   * @return the meta-bean, not null
   */
  def meta(): MultiCurrencyAmount.Meta = {
    MultiCurrencyAmount.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `MultiCurrencyAmount`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `amounts` property.
     */
    private val amountsProperty: MetaProperty[ImmutableSortedSet[CurrencyAmount]] = DirectMetaProperty.ofImmutable(
        this, "amounts", classOf[MultiCurrencyAmount],
        classOf[ImmutableSortedSet[?]].asInstanceOf[Class[ImmutableSortedSet[CurrencyAmount]]])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "amounts")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -879772901 => // amounts
          amountsProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: MultiCurrencyAmount] = {
      new MultiCurrencyAmount.Builder()
    }

    override def beanType(): Class[? <: MultiCurrencyAmount] = {
      classOf[MultiCurrencyAmount]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `amounts` property.
     * @return the meta-property, not null
     */
    def amounts(): MetaProperty[ImmutableSortedSet[CurrencyAmount]] = {
      amountsProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -879772901 => // amounts
          bean.asInstanceOf[MultiCurrencyAmount].getAmounts()
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
   * The bean-builder for `MultiCurrencyAmount`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[MultiCurrencyAmount] {

    private var amounts: SortedSet[CurrencyAmount] = ImmutableSortedSet.of[CurrencyAmount]()

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -879772901 => // amounts
          amounts
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -879772901 => // amounts
          this.amounts = newValue.asInstanceOf[SortedSet[CurrencyAmount]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): MultiCurrencyAmount = {
      new MultiCurrencyAmount(
          amounts)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(64)
      buf.append("MultiCurrencyAmount.Builder{")
      buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
