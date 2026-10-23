/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.location

import java.io.Serializable
import java.util.Locale
import java.util.Optional
import java.util.Set
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.function.Function

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.base.CharMatcher
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSortedSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.MapStream
import com.opengamma.strata.collect.io.PropertiesFile
import com.opengamma.strata.collect.io.ResourceLocator

/**
 * A country or territory.
 *
 * This class represents a country or territory that it is useful to identify.
 * Any two letter code may be used, however it is intended to use codes based on ISO-3166-1 alpha-2.
 *
 * This class is immutable and thread-safe.
 *
 * Restricted constructor.
 *
 * @param code  the two letter country code
 */
@SerialVersionUID(1L)
final class Country private (
    /**
     * The country code.
     */
    private val code: String)
  extends Comparable[Country] with Serializable {

  /**
   * Ensure singleton on deserialization.
   *
   * @return the singleton
   */
  private def readResolve(): Object = {
    Country.of(code)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the two letter ISO code.
   *
   * @return the two letter ISO code
   */
  def getCode(): String = {
    code
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the ISO-3166-1 alpha-3 three letter country code.
   *
   * @return the three letter country code
   * @throws java.lang.IllegalArgumentException if the country is invalid
   */
  def getCode3Char(): String = {
    if (Country.COUNTRY_CODES.get().containsValue(this)) {
      Country.COUNTRY_CODES.get().inverse().get(this)
    } else {
      throw new IllegalArgumentException("Unknown country: " + this.getCode())
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this country to another.
   *
   * The comparison sorts alphabetically by the two letter country code.
   *
   * @param other  the other country
   * @return negative if less, zero if equal, positive if greater
   */
  override def compareTo(other: Country): Int = {
    code.compareTo(other.code)
  }

  /**
   * Checks if this country equals another country.
   *
   * The comparison checks the two letter country code.
   *
   * @param obj  the other country, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj == null || (getClass() ne obj.getClass())) {
      false
    } else {
      val other = obj.asInstanceOf[Country]
      code.equals(other.code)
    }
  }

  /**
   * Returns a suitable hash code for the country.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    code.hashCode()
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string representation of the country, which is the two letter code.
   *
   * @return the two letter country code
   */
  @ToString
  override def toString(): String = {
    code
  }

}

object Country {

  /**
   * Loads the ISO alpha-2 and alpha-3 country codes into a bidirectional map.
   */
  @static private val COUNTRY_CODES: Supplier[ImmutableBiMap[String, Country]] = {
    val toCountry: Function[String, Country] = code => Country.of(code)
    Suppliers.memoize(() =>
      ImmutableBiMap.copyOf(MapStream.of(PropertiesFile.of(
                  ResourceLocator.ofClasspath(classOf[Country], "country.properties").getCharSource())
              .getProperties().asMap())
          .mapValues[Country](toCountry)
          .toMap()))
  }

  /**
   * A cache of instances.
   */
  @static private val CACHE: ConcurrentMap[String, Country] = new ConcurrentSkipListMap[String, Country]()
  /**
   * The matcher for the code.
   */
  @static private val CODE_MATCHER: CharMatcher = CharMatcher.inRange('A', 'Z')

  // selected countries of Europe
  /**
   * The region of 'EU' - Europe (special status in ISO-3166).
   */
  @static final val EU: Country = of("EU")
  /**
   * The country 'AT' - Austria.
   */
  @static final val AT: Country = of("AT")
  /**
   * The country 'BE' - Belgium.
   */
  @static final val BE: Country = of("BE")
  /**
   * The country 'CH' - Switzerland.
   */
  @static final val CH: Country = of("CH")
  /**
   * The currency 'CZ' - Czech Republic.
   */
  @static final val CZ: Country = of("CZ")
  /**
   * The country 'DE' - Germany.
   */
  @static final val DE: Country = of("DE")
  /**
   * The country 'DK' - Denmark.
   */
  @static final val DK: Country = of("DK")
  /**
   * The currency 'ES' - Spain.
   */
  @static final val ES: Country = of("ES")
  /**
   * The currency 'FI' - Finland.
   */
  @static final val FI: Country = of("FI")
  /**
   * The currency 'FR' - France.
   */
  @static final val FR: Country = of("FR")
  /**
   * The country 'GB' - United Kingdom.
   */
  @static final val GB: Country = of("GB")
  /**
   * The country 'GR' - Greece.
   */
  @static final val GR: Country = of("GR")
  /**
   * The currency 'HU' = Hungary.
   */
  @static final val HU: Country = of("HU")
  /**
   * The currency 'IE' - Ireland.
   */
  @static final val IE: Country = of("IE")
  /**
   * The currency 'IS' - Iceland.
   */
  @static final val IS: Country = of("IS")
  /**
   * The currency 'IT' - Italy.
   */
  @static final val IT: Country = of("IT")
  /**
   * The currency 'LU' - Luxembourg.
   */
  @static final val LU: Country = of("LU")
  /**
   * The currency 'NL' - Netherlands.
   */
  @static final val NL: Country = of("NL")
  /**
   * The currency 'NO' - Norway.
   */
  @static final val NO: Country = of("NO")
  /**
   * The currency 'PL' = Poland.
   */
  @static final val PL: Country = of("PL")
  /**
   * The currency 'PT' - Portugal.
   */
  @static final val PT: Country = of("PT")
  /**
   * The currency 'SE' - Sweden.
   */
  @static final val SE: Country = of("SE")
  /**
   * The currency 'SK' - Slovakia.
   */
  @static final val SK: Country = of("SK")
  /**
   * The country 'TR' - Turkey.
   */
  @static final val TR: Country = of("TR")

  // selected countries of the Americas
  /**
   * The country 'AR' - Argentina.
   */
  @static final val AR: Country = of("AR")
  /**
   * The country 'BR' - Brazil.
   */
  @static final val BR: Country = of("BR")
  /**
   * The country 'CA' - Canada.
   */
  @static final val CA: Country = of("CA")
  /**
   * The country 'CL' - Chile.
   */
  @static final val CL: Country = of("CL")
  /**
   * The country 'MX' - Mexico.
   */
  @static final val MX: Country = of("MX")
  /**
   * The country 'US' - United States.
   */
  @static final val US: Country = of("US")

  // selected countries of the Rest of the World
  /**
   * The country 'AU' - Australia.
   */
  @static final val AU: Country = of("AU")
  /**
   * The country 'CN' - China.
   */
  @static final val CN: Country = of("CN")
  /**
   * The currency 'EG' - Egypt.
   */
  @static final val EG: Country = of("EG")
  /**
   * The currency 'HK' - Hong Kong.
   */
  @static final val HK: Country = of("HK")
  /**
   * The country 'ID' - Indonesia.
   */
  @static final val ID: Country = of("ID")
  /**
   * The country 'IL' - Israel.
   */
  @static final val IL: Country = of("IL")
  /**
   * The country 'IN' - India.
   */
  @static final val IN: Country = of("IN")
  /**
   * The country 'JP' - Japan.
   */
  @static final val JP: Country = of("JP")
  /**
   * The country 'KR' - South Korea.
   */
  @static final val KR: Country = of("KR")
  /**
   * The country 'MY' - Malaysia.
   */
  @static final val MY: Country = of("MY")
  /**
   * The country 'NZ' - New Zealand.
   */
  @static final val NZ: Country = of("NZ")
  /**
   * The currency 'RU' = Russia.
   */
  @static final val RU: Country = of("RU")
  /**
   * The country 'SA' - Saudi Arabia.
   */
  @static final val SA: Country = of("SA")
  /**
   * The country 'SG' - Singapore.
   */
  @static final val SG: Country = of("SG")
  /**
   * The country 'TH' - Thailand.
   */
  @static final val TH: Country = of("TH")
  /**
   * The country 'ZA' - South Africa.
   */
  @static final val ZA: Country = of("ZA")


  //-------------------------------------------------------------------------
  /**
   * Obtains the set of available countries.
   *
   * This contains all the countries that have been defined at the point
   * that the method is called.
   *
   * @return an immutable set containing all registered countries
   */
  @static def getAvailableCountries(): Set[Country] = {
    COUNTRY_CODES.get()
    ImmutableSortedSet.copyOf[Country](CACHE.values())
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified ISO-3166-1 alpha-2
   * two letter country code dynamically creating a country if necessary.
   *
   * A country is uniquely identified by ISO-3166-1 alpha-2 two letter code.
   * This method creates the country if it is not known.
   *
   * @param countryCode  the two letter country code, ASCII and upper case
   * @return the singleton instance
   * @throws java.lang.IllegalArgumentException if the country code is invalid
   */
  @FromString
  @static def of(countryCode: String): Country = {
    ArgChecker.notNull(countryCode, "countryCode")
    CACHE.computeIfAbsent(countryCode, c => addCode(c))
  }

  // add code
  @static private def addCode(countryCode: String): Country = {
    ArgChecker.matches(CODE_MATCHER, 2, 2, countryCode, "countryCode", "[A-Z][A-Z]")
    new Country(countryCode)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a string to obtain a `Country`.
   *
   * The parse is identical to `of(String)` except that it will convert
   * letters to upper case first.
   *
   * @param countryCode  the two letter country code, ASCII
   * @return the singleton instance
   * @throws java.lang.IllegalArgumentException if the country code is invalid
   */
  @static def parse(countryCode: String): Country = {
    ArgChecker.notNull(countryCode, "countryCode")
    of(countryCode.toUpperCase(Locale.ENGLISH))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified ISO-3166-1 alpha-3
   * three letter country code dynamically creating a country if necessary.
   *
   * A country is uniquely identified by ISO-3166-1 alpha-3 three letter code.
   * This method creates the country if it is not known.
   *
   * @param countryCode  the three letter country code, ASCII and upper case
   * @return the singleton instance
   * @throws java.lang.IllegalArgumentException if the country code is invalid
   */
  @static def of3Char(countryCode: String): Country = {
    ArgChecker.matches(CODE_MATCHER, 3, 3, countryCode, "countryCode", "[A-Z][A-Z][A-Z]")
    Optional.ofNullable(COUNTRY_CODES.get().get(countryCode))
        .orElseThrow(() => new IllegalArgumentException("Unknown country code: " + countryCode))
  }

}
