/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.io.Serializable
import java.time.Period

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.ArgChecker

/**
 * A code used in the market to indicate both the start date and tenor of a financial instrument.
 *
 * In Strata, a `Tenor` is the actual tenor of an instrument, from start to end.
 * This class represents the code used in the market which also effectively describes the start date.
 * Four key dates are needed to understand how this code works.
 *  - Trade date, the date that the trade is agreed
 *  - Spot date, the base for date calculations, typically 2 business days after the trade date, known as the spot lag
 *  - Start date, the date on which accrual starts, generally the spot date unless forward starting
 *  - End date, the date on which accrual ends
 *
 * The period from start date to end date is represented by `Tenor`.
 * `MarketTenor` includes the tenor, but also allows the market conventional spot lag to be overridden.
 *
 * `MarketTenor` represents the 4 special cases used in the market as well as more normal tenors:
 *  - ON - Overnight, from today to tomorrow, spot lag of 0 and tenor of 1 day
 *  - TN - Tomorrow-Next, from tomorrow to the next day, spot lag of 1 and tenor of 1 day
 *  - SN - Spot-Next, from spot to the next day, market conventional spot lag and tenor of 1 day
 *  - SW - Spot-Week, from spot for one week, market conventional spot lag and tenor of 1 week
 *  - "Normal" tenors - 2W, 1M, 1Y etc - from spot for the specified period, market conventional spot lag
 *
 * Note that if the market conventional spot lag is 1 day, TN and SN would resolve to the same dates.
 * Note also that SN and SW exist for clarity, they might also be expressed as 1D and 1W with the spot implied.
 * Other date combinations are possible in theory but tend not to exist in the market, for example a three day
 * trade starting tomorrow, something which might require a code like T3D.
 */
@SerialVersionUID(1L)
final class MarketTenor private (
    /**
     * The code of the tenor.
     */
    private val code: String,
    /**
     * The tenor.
     */
    private val tenor: Tenor,
    /**
     * The spot lag or 0, 1 or MARKET_CONVENTION_LAG.
     */
    private val spotLagIndicator: Int)
  extends Comparable[MarketTenor] with Serializable {

  ArgChecker.notNull(code, "code")
  ArgChecker.notNull(tenor, "tenor")

  // safe deserialization
  private def readResolve(): Object = {
    MarketTenor.parse(code)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the market tenor code.
   *
   * @return the code
   */
  def getCode(): String = {
    code
  }

  /**
   * Gets the tenor of the instrument.
   *
   * The tenor for ON, TN and SN is 1 day and for SW is 1 week.
   *
   * @return the tenor of the instrument
   */
  def getTenor(): Tenor = {
    tenor
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the market tenor implies a non-standard spot lag.
   *
   * This returns true for ON and TN as they need special date handling.
   * SN and SW return false as they imply market conventional spot.
   *
   * @return true if this market tenor is short
   */
  def isNonStandardSpotLag(): Boolean = {
    spotLagIndicator != MarketTenor.MARKET_CONVENTION_LAG
  }

  /**
   * Adjusts the market conventional spot lag to match the market tenor.
   *
   * The resulting lag will be 0 days for ON and 1 day for TN.
   * Otherwise the input lag will be unchanged.
   *
   * @param marketConventionalSpotLag  the market conventional spot lag
   * @return true if this market tenor is short
   */
  def adjustSpotLag(marketConventionalSpotLag: DaysAdjustment): DaysAdjustment = {
    if (isNonStandardSpotLag()) {
      // this way ensures the result is a valid business day
      DaysAdjustment.ofBusinessDays(spotLagIndicator, marketConventionalSpotLag.getResultCalendar())
    } else {
      marketConventionalSpotLag
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this market tenor to another market tenor.
   *
   * Comparing tenors is a hard problem in general, but for commonly used tenors the outcome is as expected.
   * This uses the same sort order as `Tenor` with ON and TN first.
   *
   * @param other  the other tenor
   * @return negative if this is less than the other, zero if equal and positive if greater
   */
  override def compareTo(other: MarketTenor): Int = {
    if (isNonStandardSpotLag() || other.isNonStandardSpotLag()) {
      Integer.compare(spotLagIndicator, other.spotLagIndicator)
    } else {
      tenor.compareTo(other.tenor)
    }
  }

  /**
   * Checks if this market tenor equals another market tenor.
   *
   * The comparison checks the code.
   *
   * @param obj  the other market tenor, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj == null || (getClass() ne obj.getClass())) {
      false
    } else {
      val other = obj.asInstanceOf[MarketTenor]
      code.equals(other.code)
    }
  }

  /**
   * Returns a suitable hash code for the market tenor.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    code.hashCode()
  }

  /**
   * Returns a formatted string representing the market tenor.
   *
   * @return the formatted market tenor
   */
  @ToString
  override def toString(): String = {
    code
  }

}

object MarketTenor {

  /**
   * A tenor code for Overnight, meaning from today to tomorrow.
   */
  @static final val ON: MarketTenor = new MarketTenor("ON", Tenor.ofDays(1), 0)
  /**
   * A tenor code for Tomorrow-Next, meaning from tomorrow to the next day.
   */
  @static final val TN: MarketTenor = new MarketTenor("TN", Tenor.ofDays(1), 1)
  /**
   * A tenor code for Spot-Next, meaning from the spot date to the next day.
   * The spot date is usually two working days after today, but this varies by currency.
   */
  @static final val SN: MarketTenor = new MarketTenor("SN", Tenor.ofDays(1), MARKET_CONVENTION_LAG)
  /**
   * A tenor code for Spot-Week, meaning one week starting from the spot date.
   * The spot date is usually two working days after today, but this varies by currency.
   */
  @static final val SW: MarketTenor = new MarketTenor("SW", Tenor.ofWeeks(1), MARKET_CONVENTION_LAG)

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a `Tenor` with spot implied.
   *
   * A tenor of 1D will return SN.
   * A tenor of 1W will return SW.
   * Other tenors will return a market tenor with the same code as the tenor.
   *
   * @param tenor  the tenor
   * @return the tenor
   */
  def ofSpot(tenor: Tenor): MarketTenor = {
    if (tenor.getPeriod().equals(Period.ofDays(1))) {
      MarketTenor.SN
    } else if (tenor.getPeriod().equals(Period.ofDays(7))) {
      MarketTenor.SW
    } else {
      new MarketTenor(tenor.toString(), tenor, MARKET_CONVENTION_LAG)
    }
  }

  /**
   * Obtains an instance from a number of days from spot.
   *
   * A tenor of 1D will return SN.
   * A tenor of 7D will return SW.
   *
   * @param days  the tenor in days
   * @return the tenor
   */
  def ofSpotDays(days: Int): MarketTenor = {
    if (days == 1) {
      MarketTenor.SN
    } else if (days == 7) {
      MarketTenor.SW
    } else {
      val tenor = Tenor.ofDays(days)
      new MarketTenor(tenor.toString(), tenor, MARKET_CONVENTION_LAG)
    }
  }

  /**
   * Obtains an instance from a number of months from spot.
   *
   * @param months  the tenor in months
   * @return the tenor
   */
  def ofSpotMonths(months: Int): MarketTenor = {
    val tenor = Tenor.ofMonths(months)
    new MarketTenor(tenor.toString(), tenor, MARKET_CONVENTION_LAG)
  }

  /**
   * Obtains an instance from a number of years from spot.
   *
   * @param years  the tenor in years
   * @return the tenor
   */
  def ofSpotYears(years: Int): MarketTenor = {
    val tenor = Tenor.ofYears(years)
    new MarketTenor(tenor.toString(), tenor, MARKET_CONVENTION_LAG)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the market tenor.
   *
   * This parses ON, TN, SN, SW and all formats accepted by `Tenor#parse(String)`.
   *
   * @param toParse  the string representing the tenor
   * @return the tenor
   * @throws java.lang.IllegalArgumentException if the tenor cannot be parsed
   */
  @FromString
  def parse(toParse: String): MarketTenor = {
    ArgChecker.notEmpty(toParse, "toParse")
    toParse match {
      case "ON" => MarketTenor.ON
      case "TN" => MarketTenor.TN
      case "SN" => MarketTenor.SN
      case "SW" => MarketTenor.SW
      case _ => MarketTenor.ofSpot(Tenor.parse(toParse))
    }
  }

  /**
   * Constant used to indicate market convention swap lag.
   */
  // a constant (untyped final val) is inlined at every use, so it needs no field that a
  // @static declaration would expose as public static final through access from the class
  private final val MARKET_CONVENTION_LAG = Integer.MAX_VALUE

}
