/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency

import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Map
import java.util.Optional
import java.util.stream.Stream

import scala.annotation.static

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.MapStream
import com.opengamma.strata.collect.array.DoubleMatrix

/**
 * A mutable builder class for `FxMatrix`.
 */
class FxMatrixBuilder private (
    /**
     * The currencies held by the builder pointing to their position
     * in the rates array. An ordered map is used so that it retains
     * order which means the `toString` method of `FxMatrix`
     * is clearer.
     */
    private val currencies: LinkedHashMap[Currency, Integer],
    /**
     * A 2 dimensional array holding the rates. Each row of the array holds the
     * value of 1 unit of Currency (that the row represents) in each of the
     * alternate currencies.
     *
     * The array is square with its order being a power of 2. This means that there
     * may be empty rows/cols at the bottom/right of the matrix. Leaving this space
     * means that adding currencies can be done more efficiently as the array only
     * needs to be resized (via copying) relatively infrequently..
     */
    private var rates: Array[Array[Double]]) {

  /**
   * Rates that have been requested to be added, but which do not
   * have a currency in common with the currencies already present.
   * As additional currencies are added, this map will be checked to
   * see if rates can be handled.
   *
   * If this map is not empty by the point that build is called,
   * an `IllegalStateException` will be thrown.
   */
  private val disjointRates: Map[CurrencyPair, java.lang.Double] = new HashMap[CurrencyPair, java.lang.Double]()

  private[currency] def this() = {
    this(
        new LinkedHashMap[Currency, Integer](),
        Array.ofDim[Double](FxMatrixBuilder.MINIMAL_MATRIX_SIZE, FxMatrixBuilder.MINIMAL_MATRIX_SIZE))
  }

  private[currency] def this(currencies: ImmutableMap[Currency, Integer], rates: Array[Array[Double]]) = {
    // Ensure there is space to add at least one new currency
    this(
        new LinkedHashMap[Currency, Integer](currencies),
        FxMatrixBuilder.copyArray(rates, FxMatrixBuilder.size(currencies.size() + 1)))
  }

  //-------------------------------------------------------------------------
  /**
   * Build a new `FxMatrix` from the data in the builder.
   *
   * @return a new `FxMatrix`
   * @throws java.lang.IllegalStateException if an attempt was made to add currencies
   *   which have no currency in common with other rates
   */
  def build(): FxMatrix = {
    if (!disjointRates.isEmpty()) {
      throw new IllegalStateException("Received rates with no currencies in common with other: " + disjointRates)
    }
    // Trim array down to the correct size - we have to copy the array
    // anyway to ensure immutability, so we may as well remove any
    // unused rows
    new FxMatrix(ImmutableMap.copyOf(currencies), DoubleMatrix.ofUnsafe(FxMatrixBuilder.copyArray(rates, currencies.size())))
  }

  /**
   * Adds a new rate for a currency pair to the builder. See
   * `addRate(Currency, Currency, double)` for full
   * explanation.
   *
   * @param currencyPair  the currency pair to be added
   * @param rate  the FX rate between the base currency of the pair and the
   *   counter currency. The rate indicates the value of one unit of the base
   *   currency in terms of the counter currency.
   * @return the builder updated with the new rate
   */
  def addRate(currencyPair: CurrencyPair, rate: Double): FxMatrixBuilder = {
    ArgChecker.notNull(currencyPair, "currencyPair")
    addRate(currencyPair.getBase(), currencyPair.getCounter(), rate)
  }

  /**
   * Add a new pair of currencies to the builder.
   *
   * An invocation of the method with `builder.addRate(GBP, USD, 1.6)`
   * indicates that 1 pound sterling is worth 1.6 US dollars. It is
   * equivalent to: `builder.addRate(USD, GBP, 1 / 1.6)` (1 US dollar
   * is worth 0.625 pounds sterling) for all cases except where the USD/GBP
   * rates is already in the matrix and so will be updated.
   *
   * There are a number of possible outcomes when this  method is called:
   *
   *  - The builder is currently empty. In this case these currencies
   *    and rates will be added as the initial pair.
   *  - The builder is non-empty and neither of the currencies are
   *    currently in the matrix. In this case the currencies cannot be
   *    immediately added as there is no common currency to allow the
   *    cross rates to be calculated. The currencies and rates are put
   *    into a pending set for later processing, for example after another
   *    pair containing one of the currencies and a currency already in
   *    the matrix is added. If no such event occurs, then an exception
   *    will be thrown when `build()` is called.
   *  - The builder is non-empty and one of the currencies in the pair
   *    is already in the matrix, whilst the other is not. In this case
   *    the pair and rate is added to the matrix and all the cross rates
   *    to the other currencies are calculated.
   *  - The builder is non-empty and contains both of the currencies in
   *    the pair. In this case the pair is treated as an update to the
   *    rate already in the matrix. The first currency (ccy1) is treated
   *    as the reference currency and the second currency (ccy2) is the
   *    updated currency. All rates involving the updated currency will
   *    be recalculated using the new rate.
   *    Note that due to one of the rates being treated as a reference, this
   *    operation is not symmetric. That is, the result of
   *    `matrix.addRate(USD, EUR, 1.23)` will be different to the
   *    result of `matrix.addRate(EUR, USD, 1 / 1.23)` when there
   *    are other currencies present in the builder.
   *
   * @param ccy1  the first currency of the pair
   * @param ccy2  the second currency of the pair
   * @param rate  the FX rate between the first currency and the second currency.
   *   The rate indicates the value of one unit of the first currency in terms
   *   of the second currency.
   * @return the builder updated with the new rate
   */
  def addRate(ccy1: Currency, ccy2: Currency, rate: Double): FxMatrixBuilder = {
    ArgChecker.notNull(ccy1, "ccy1")
    ArgChecker.notNull(ccy2, "ccy2")

    if (currencies.isEmpty()) {
      addInitialCurrencyPair(ccy1, ccy2, rate)
    } else {
      addCurrencyPair(ccy1, ccy2, rate)
    }
    this
  }

  /**
   * Adds a collection of new rates for currency pairs to the builder.
   * Pairs that are already in the builder are treated as updates to the
   * existing rates -> !e.getKey().equals(commonCurrency) && !currencies.containsKey(e.getKey())
   *
   * @param rates  the currency pairs and rates to be added
   * @return the builder updated with the new rates
   */
  def addRates(rates: Map[CurrencyPair, java.lang.Double]): FxMatrixBuilder = {
    ArgChecker.notNull(rates, "rates")

    if (!rates.isEmpty()) {
      ensureCapacity(rates.keySet().stream()
          .flatMap[Currency](cp => Stream.of[Currency](cp.getBase(), cp.getCounter())))

      MapStream.of(rates).forEach((pair, rate) => addRate(pair, rate.doubleValue()))
    }
    this
  }

  private[currency] def merge(other: FxMatrixBuilder): FxMatrixBuilder = {

    // Find the common currencies
    val common: Optional[Currency] = currencies.keySet()
        .stream()
        .filter(ccy => other.currencies.containsKey(ccy))
        .findFirst()

    val commonCurrency: Currency = common.orElseThrow[IllegalArgumentException](() => new IllegalArgumentException(
        "There are no currencies in common between " + currencies.keySet() + " and " + other.currencies.keySet()))

    // Add in all currencies that we don't already have
    MapStream.of(other.currencies)
        .filterKeys(ccy => !ccy.equals(commonCurrency) && !currencies.containsKey(ccy))
        .forEach((ccy, idx) => addCurrencyPair(commonCurrency, ccy, other.getRate(commonCurrency, ccy)))

    this
  }

  private def getRate(ccy1: Currency, ccy2: Currency): Double = {
    val i = currencies.get(ccy1).intValue()
    val j = currencies.get(ccy2).intValue()
    rates(i)(j)
  }

  private def addCurrencyPair(ccy1: Currency, ccy2: Currency, rate: Double): Unit = {

    // Only resize if there's a danger we can't fit a new currency in
    if (rates.length < currencies.size() + 1) {
      ensureCapacity(Stream.of[Currency](ccy1, ccy2))
    }

    if (!currencies.containsKey(ccy1) && !currencies.containsKey(ccy2)) {
      // Neither currency present - add to disjoint set
      disjointRates.put(CurrencyPair.of(ccy1, ccy2), java.lang.Double.valueOf(rate))
    } else if (currencies.containsKey(ccy1) && currencies.containsKey(ccy2)) {
      // We already have a rate for this currency pair
      updateRate(ccy1, ccy2, rate)
    } else {
      // We have exactly one of the currencies already
      addNewRate(ccy1, ccy2, rate)

      // With a new rate added we may be able to handle the disjoint
      retryDisjoints()
    }
  }

  private def retryDisjoints(): Unit = {

    ensureCapacity(disjointRates.keySet()
        .stream()
        .flatMap[Currency](cp => Stream.of[Currency](cp.getBase(), cp.getCounter())))

    var continue = true
    while (continue) {
      val initialSize = disjointRates.size()
      val addable: ImmutableMap[CurrencyPair, java.lang.Double] = MapStream.of(disjointRates)
          .filterKeys(pair => currencies.containsKey(pair.getBase()) || currencies.containsKey(pair.getCounter()))
          .toMap()

      MapStream.of(addable).forEach((pair, rate) => addNewRate(pair.getBase(), pair.getCounter(), rate.doubleValue()))
      addable.keySet().stream().forEach(pair => disjointRates.remove(pair))

      if (disjointRates.size() == initialSize) {
        // No effect so break out
        continue = false
      }
    }
  }

  private def addNewRate(ccy1: Currency, ccy2: Currency, rate: Double): Unit = {

    val existing = if (currencies.containsKey(ccy1)) ccy1 else ccy2
    val other = if (existing eq ccy1) ccy2 else ccy1

    val updatedRate = if (existing eq ccy2) 1.0 / rate else rate
    val indexRef = currencies.get(existing).intValue()
    val indexOther = currencies.size()

    currencies.put(other, Integer.valueOf(indexOther))
    rates(indexOther)(indexOther) = 1.0

    var i = 0
    while (i < indexOther) {
      val convertedRate = updatedRate * rates(i)(indexRef)
      rates(i)(indexOther) = convertedRate
      rates(indexOther)(i) = 1.0 / convertedRate
      i += 1
    }
  }

  // We take the first currency as the reference and the second as
  // the currency to be updated
  private def updateRate(ccy1: Currency, ccy2: Currency, rate: Double): Unit = {

    val index1 = currencies.get(ccy1).intValue()
    val index2 = currencies.get(ccy2).intValue()

    var i = 0
    while (i < currencies.size()) {
      // Nothing to do - we know and want rates[index2][index2] = 1
      if (i != index2) {
        val convertedRate = rate * rates(i)(index1)
        rates(i)(index2) = convertedRate
        rates(index2)(i) = 1.0 / convertedRate
      }
      i += 1
    }
  }

  private def addInitialCurrencyPair(ccy1: Currency, ccy2: Currency, rate: Double): Unit = {
    // No need for capacity check, as initial size is always enough
    currencies.put(ccy1, Integer.valueOf(0))
    currencies.put(ccy2, Integer.valueOf(1))
    rates(0)(0) = 1.0
    rates(0)(1) = rate
    rates(1)(1) = 1.0
    rates(1)(0) = 1.0 / rate
  }

  private def ensureCapacity(potentialCurrencies: Stream[Currency]): Unit = {
    // If adding the currencies would mean we have more
    // currencies than matrix size, create an expanded array
    val requiredOrder =
        Stream.concat[Currency](currencies.keySet().stream(), potentialCurrencies)
            .distinct()
            .count()
            .toInt

    ensureCapacity(requiredOrder)
  }

  private def ensureCapacity(requiredOrder: Int): Unit = {
    if (requiredOrder > rates.length) {
      rates = FxMatrixBuilder.copyArray(rates, FxMatrixBuilder.size(requiredOrder))
    }
  }

}

object FxMatrixBuilder {

  /**
   * The minimum size of the FX rate matrix. This is intended such
   * that a number rates of can be added without needing to resize.
   */
  @static private final val MINIMAL_MATRIX_SIZE = 8

  // size the matrix to either the minimal matrix size, or a power of 2
  // sufficient to hold the required currencies
  private def size(requiredCapacity: Int): Int = {
    val lowerPower = Integer.highestOneBit(requiredCapacity)
    Math.max(if (requiredCapacity == lowerPower) requiredCapacity else lowerPower << 2, MINIMAL_MATRIX_SIZE)
  }

  //-------------------------------------------------------------------------
  // copies the array trimming to the specified size
  private def copyArray(rates: Array[Array[Double]], requestedSize: Int): Array[Array[Double]] = {
    val order = Math.min(rates.length, requestedSize)
    val copy = Array.ofDim[Double](requestedSize, requestedSize)
    var i = 0
    while (i < order) {
      System.arraycopy(rates(i), 0, copy(i), 0, order)
      i += 1
    }
    copy
  }

}
