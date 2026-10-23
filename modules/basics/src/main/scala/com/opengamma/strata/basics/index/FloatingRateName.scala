/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index

import java.util.Optional
import java.util.Set
import java.util.function.Supplier

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.collect.Iterables
import com.opengamma.strata.basics.currency.Currency
import com.opengamma.strata.basics.date.DaysAdjustment
import com.opengamma.strata.basics.date.Tenor
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.named.ExtendedEnum
import com.opengamma.strata.collect.named.Named

/**
 * A floating rate index name, such as Libor, Euribor or US Fed Fund.
 *
 * An index represented by this class relates to some form of floating rate.
 * This can include `IborIndex` and `OvernightIndex` values.
 *
 * This class is designed to match the FpML/ISDA floating rate index concept.
 * The FpML concept provides a single key for floating rates of a variety of
 * types, mixing  Ibor, Overnight, Price and Swap indices.
 * It also sometimes includes a source, such as 'Bloomberg' or 'Reuters'.
 * This class matches the single concept and provided a bridge the more
 * specific index implementations used for pricing.
 *
 * The most common implementations are provided in `FloatingRateNames`.
 *
 * The set of supported values, and their mapping to `IborIndex`, `PriceIndex`
 * and `OvernightIndex`, is defined in the `FloatingRateName.ini`
 * config file.
 */
trait FloatingRateName
    extends FloatingRate with Named {

  //-----------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this floating rate, such as 'GBP-LIBOR'.
   *
   * This name is used in serialization and can be parsed using `of(String)`.
   *
   * @return the external name
   */
  @ToString
  def getName(): String

  /**
   * Gets the type of the index - Ibor, Overnight or Price.
   *
   * @return index type - Ibor, Overnight or Price
   */
  def getType(): FloatingRateType

  /**
   * Gets the currency of the floating rate.
   *
   * @return the currency
   * @throws java.lang.IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  override def getCurrency(): Currency = {
    toFloatingRateIndex().getCurrency()
  }

  /**
   * Gets the active tenors that are applicable for this floating rate.
   *
   * Overnight and Price indices will return an empty set.
   *
   * @return the available tenors
   */
  def getTenors(): Set[Tenor]

  /**
   * Gets a default tenor applicable for this floating rate.
   *
   * This is useful for providing a basic default where errors need to be avoided.
   * The value returned is not intended to be based on market conventions.
   *
   * Ibor floating rates will return 3M, or 13W if that is not available, otherwise
   * the first entry from the set of tenors.
   * Overnight floating rates will return 1D.
   * All other floating rates will return return 1Y.
   *
   * @return the default tenor
   */
  def getDefaultTenor(): Tenor = {
    getType() match {
      case FloatingRateType.IBOR =>
        val tenors = getTenors()
        if (tenors.contains(Tenor.TENOR_3M)) {
          Tenor.TENOR_3M
        } else if (tenors.contains(Tenor.TENOR_13W)) {
          Tenor.TENOR_13W
        } else {
          tenors.iterator().next()
        }
      case FloatingRateType.OVERNIGHT_AVERAGED | FloatingRateType.OVERNIGHT_COMPOUNDED =>
        Tenor.TENOR_1D
      case _ =>
        Tenor.TENOR_1Y
    }
  }

  /**
   * Gets the normalized form of the floating rate name.
   *
   * The normalized for is the name that Strata uses for the index.
   * For example, the normalized form of 'GBP-LIBOR-BBA' is 'GBP-LIBOR',
   * and the normalized form of 'EUR-EURIBOR-Reuters' is 'EUR-EURIBOR'.
   * Note that for Ibor indices, the tenor is not present.
   *
   * @return the normalized name
   */
  def normalized(): FloatingRateName

  //-------------------------------------------------------------------------
  /**
   * Returns a floating rate index.
   *
   * Returns a `FloatingRateIndex` for this rate name.
   * Only Ibor, Overnight and Price indices are handled.
   * If the rate name is an Ibor rate, the default tenor is used.
   *
   * @return the index
   * @throws java.lang.IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  def toFloatingRateIndex(): FloatingRateIndex = {
    // code copied to avoid calling getDefaultTenor() unless necessary
    getType() match {
      case FloatingRateType.IBOR =>
        toIborIndex(getDefaultTenor())
      case FloatingRateType.OVERNIGHT_COMPOUNDED | FloatingRateType.OVERNIGHT_AVERAGED =>
        toOvernightIndex()
      case FloatingRateType.PRICE =>
        toPriceIndex()
      case _ =>
        throw new IllegalArgumentException("Floating rate index type not known: " + getType())
    }
  }

  /**
   * Returns a floating rate index.
   *
   * Returns a `FloatingRateIndex` for this rate name.
   * Only Ibor, Overnight and Price indices are handled.
   * If the rate name is an Ibor rate, the specified tenor is used.
   *
   * @param iborTenor  the tenor to use if this rate is Ibor
   * @return the index
   * @throws java.lang.IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  def toFloatingRateIndex(iborTenor: Tenor): FloatingRateIndex = {
    getType() match {
      case FloatingRateType.IBOR =>
        toIborIndex(iborTenor)
      case FloatingRateType.OVERNIGHT_COMPOUNDED | FloatingRateType.OVERNIGHT_AVERAGED =>
        toOvernightIndex()
      case FloatingRateType.PRICE =>
        toPriceIndex()
      case _ =>
        throw new IllegalArgumentException("Floating rate index type not known: " + getType())
    }
  }

  /**
   * Checks and returns an Ibor index.
   *
   * If this name represents an Ibor index, then this method returns the matching `IborIndex`.
   * If not, an exception is thrown.
   *
   * @param tenor  the tenor of the index
   * @return the index
   * @throws java.lang.IllegalStateException if the type is not an Ibor index type
   */
  def toIborIndex(tenor: Tenor): IborIndex

  /**
   * Checks and returns the fixing offset associated with the Ibor index.
   *
   * If this name represents an Ibor index, then this method returns the associated fixing offset.
   * If not, an exception is thrown.
   *
   * This method exists primarily to handle DKK CIBOR, where there are two floating rate names but
   * only one underlying index. The CIBOR index itself has a convention where the fixing date is 2 days
   * before the reset date and the effective date is 2 days after the fixing date, matching the name "DKK-CIBOR2-DKNA13".
   * The alternative name, "DKK-CIBOR-DKNA13", has the fixing date equal to the reset date, but with
   * the effective date two days later.
   *
   * @return the fixing offset applicable to the index
   * @throws java.lang.IllegalStateException if the type is not an Ibor index type
   */
  def toIborIndexFixingOffset(): DaysAdjustment = {
    toIborIndex(Iterables.getFirst(getTenors(), Tenor.TENOR_3M)).getFixingDateOffset()
  }

  /**
   * Converts to an `OvernightIndex`.
   *
   * If this name represents an Overnight index, then this method returns the matching `OvernightIndex`.
   * If not, an exception is thrown.
   *
   * @return the index
   * @throws java.lang.IllegalStateException if the type is not an Overnight index type
   */
  def toOvernightIndex(): OvernightIndex

  /**
   * Converts to an `PriceIndex`.
   *
   * If this name represents a price index, then this method returns the matching `PriceIndex`.
   * If not, an exception is thrown.
   *
   * @return the index
   * @throws java.lang.IllegalStateException if the type is not a price index type
   */
  def toPriceIndex(): PriceIndex

}

object FloatingRateName {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @FromString
  @static def of(uniqueName: String): FloatingRateName = {
    ArgChecker.notNull(uniqueName, "uniqueName")
    extendedEnum().lookup(uniqueName)
  }

  /**
   * Gets the extended enum helper.
   *
   * This helper allows instances of the floating rate to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  @static def extendedEnum(): ExtendedEnum[FloatingRateName] = {
    FloatingRateNames.enumLookup
  }

  /**
   * Parses a string, with extended handling of indices.
   *
   * This tries a number of ways to parse the input:
   *  - `FloatingRateName#of(String)`
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *
   * Note that if an `IborIndex` is parsed, the tenor will be lost.
   *
   * @param str  the string to parse
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  @static def parse(str: String): FloatingRateName = {
    parseImpl(str)
  }

  /**
   * Tries to parse a string, with extended handling of indices.
   *
   * This tries a number of ways to parse the input:
   *  - `FloatingRateName#of(String)`
   *  - `IborIndex#of(String)`
   *  - `OvernightIndex#of(String)`
   *  - `PriceIndex#of(String)`
   *
   * Note that if an `IborIndex` is parsed, the tenor will be lost.
   *
   * @param str  the string to parse
   * @return the floating rate, empty if not found
   */
  @static def tryParse(str: String): Optional[FloatingRateName] = {
    tryParseImpl(str)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the default Ibor index for a currency.
   *
   * @param currency  the currency to find the default for
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if there is no default for the currency
   */
  @static def defaultIborIndex(currency: Currency): FloatingRateName = {
    FloatingRateNameIniLookup.INSTANCE.defaultIborIndex(currency)
  }

  /**
   * Gets the default Overnight index for a currency.
   *
   * @param currency  the currency to find the default for
   * @return the floating rate
   * @throws java.lang.IllegalArgumentException if there is no default for the currency
   */
  @static def defaultOvernightIndex(currency: Currency): FloatingRateName = {
    FloatingRateNameIniLookup.INSTANCE.defaultOvernightIndex(currency)
  }

  // lambdas are created outside the static interface methods to avoid illegal interface lambda helpers
  private def parseImpl(str: String): FloatingRateName = {
    ArgChecker.notNull(str, "str")
    val failure: Supplier[IllegalArgumentException] =
      () => new IllegalArgumentException("Floating rate name not known: " + str)
    tryParseImpl(str).orElseThrow(failure)
  }

  private def tryParseImpl(str: String): Optional[FloatingRateName] = {
    val frnOpt: Optional[FloatingRateName] = FloatingRateName.extendedEnum().find(str)
    if (frnOpt.isPresent()) {
      frnOpt
    } else {
      FloatingRateIndex.tryParse(str).map[FloatingRateName](index => index.getFloatingRateName())
    }
  }

}
