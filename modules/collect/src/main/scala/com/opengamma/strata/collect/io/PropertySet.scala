/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.util.Map
import java.util.Optional

import com.google.common.base.Joiner
import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.MapStream

/**
 * A map of key-value properties.
 *
 * This class represents a map of key to value.
 * Multiple values may be associated with each key.
 *
 * This class is generally created by reading an INI or properties file.
 * See `IniFile` and `PropertiesFile`.
 */
final class PropertySet private (
    /**
     * The key-value pairs.
     */
    private val keyValueMap: ImmutableListMultimap[String, String]) {
  // this class is common between IniFile and PropertiesFile

  //-------------------------------------------------------------------------
  /**
   * Returns the keys and values as a `MapStream`.
   *
   * There may be multiple values for the same key.
   * The iteration order of the stream matches that of the input data.
   *
   * @return the map stream
   */
  def stream(): MapStream[String, String] = {
    MapStream.of(keyValueMap)
  }

  /**
   * Returns the set of keys of this property set.
   *
   * The iteration order of the map matches that of the input data.
   *
   * @return the set of keys
   */
  def keys(): ImmutableSet[String] = {
    ImmutableSet.copyOf(keyValueMap.keySet())
  }

  /**
   * Returns the property set as a multimap.
   *
   * The iteration order of the map matches that of the input data.
   *
   * @return the key-value map
   */
  def asMultimap(): ImmutableListMultimap[String, String] = {
    keyValueMap
  }

  /**
   * Returns the property set as a map.
   *
   * The iteration order of the map matches that of the input data.
   *
   * If a key has multiple values, the values will be returned as a comma separated list.
   *
   * @return the key-value map
   */
  def asMap(): ImmutableMap[String, String] = {
    val builder = ImmutableMap.builder[String, String]()
    val it = keys().iterator()
    while (it.hasNext()) {
      val key = it.next()
      builder.put(key, value(key))
    }
    builder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this property set is empty.
   *
   * @return true if the set is empty
   */
  def isEmpty(): Boolean = {
    keyValueMap.isEmpty()
  }

  /**
   * Checks if this property set contains the specified key.
   *
   * @param key  the key name
   * @return true if the key exists
   */
  def contains(key: String): Boolean = {
    ArgChecker.notNull(key, "key")
    keyValueMap.containsKey(key)
  }

  /**
   * Gets a single value from this property set.
   *
   * This returns the value associated with the specified key.
   *
   * If a key has multiple values, the values will be returned as a comma separated list.
   *
   * @param key  the key name
   * @return the value
   * @throws java.lang.IllegalArgumentException if the key does not exist
   */
  def value(key: String): String = {
    ArgChecker.notNull(key, "key")
    val values = keyValueMap.get(key)
    if (values.size() == 0) {
      throw new IllegalArgumentException("Unknown key: " + key)
    }
    if (values.size() > 1) {
      return Joiner.on(',').join(values)
    }
    values.get(0)
  }

  /**
   * Finds a single value in this property set.
   *
   * This returns the value associated with the specified key, empty if not present.
   *
   * If a key has multiple values, the values will be returned as a comma separated list.
   *
   * @param key  the key name
   * @return the value, empty if not found
   * @throws java.lang.IllegalArgumentException if more than one value is associated
   */
  def findValue(key: String): Optional[String] = {
    ArgChecker.notNull(key, "key")
    val values = keyValueMap.get(key)
    if (values.size() == 0) {
      return Optional.empty()
    }
    if (values.size() > 1) {
      return Optional.of(Joiner.on(',').join(values))
    }
    Optional.of(values.get(0))
  }

  /**
   * Gets the list of values associated with the specified key.
   *
   * A key-values instance may contain multiple values for each key.
   * This method returns that list of values.
   * The iteration order of the map matches that of the input data.
   * The returned list may be empty.
   *
   * @param key  the key name
   * @return the list of values associated with the key
   */
  def valueList(key: String): ImmutableList[String] = {
    ArgChecker.notNull(key, "key")
    MoreObjects.firstNonNull[ImmutableList[String]](keyValueMap.get(key), ImmutableList.of[String]())
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this property set with another.
   *
   * This property set takes precedence. Where a key exists in both sets the values in the other
   * property set will be discarded.
   * Any order of any additional keys will be retained, with those keys located after the base set of keys.
   *
   * @param other  the other property set
   * @return the combined property set
   */
  def combinedWith(other: PropertySet): PropertySet = {
    ArgChecker.notNull(other, "other")
    if (other.isEmpty()) {
      return this
    }
    if (isEmpty()) {
      return other
    }
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    val map = ImmutableListMultimap.builder[String, String]()
    map.putAll(this.keyValueMap)
    val it = other.keyValueMap.keySet().iterator()
    while (it.hasNext()) {
      val key = it.next()
      if (!this.contains(key)) {
        map.putAll(key, other.valueList(key))
      }
    }
    new PropertySet(map.build())
  }

  //-------------------------------------------------------------------------
  /**
   * Overrides this property set with another.
   *
   * The specified property set takes precedence.
   * The order of any existing keys will be retained, with the value replaced.
   * Any order of any additional keys will be retained, with those keys located after the base set of keys.
   *
   * @param other  the other property set
   * @return the combined property set
   */
  def overrideWith(other: PropertySet): PropertySet = {
    ArgChecker.notNull(other, "other")
    if (other.isEmpty()) {
      return this
    }
    if (isEmpty()) {
      return other
    }
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    val map = ImmutableListMultimap.builder[String, String]()
    val thisIt = this.keyValueMap.keySet().iterator()
    while (thisIt.hasNext()) {
      val key = thisIt.next()
      if (other.contains(key)) {
        map.putAll(key, other.valueList(key))
      } else {
        map.putAll(key, this.valueList(key))
      }
    }
    val otherIt = other.keyValueMap.keySet().iterator()
    while (otherIt.hasNext()) {
      val key = otherIt.next()
      if (!this.contains(key)) {
        map.putAll(key, other.valueList(key))
      }
    }
    new PropertySet(map.build())
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this property set equals another.
   *
   * The comparison checks the content.
   *
   * @param obj  the other section, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: PropertySet =>
          keyValueMap.equals(other.keyValueMap)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the property set.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    keyValueMap.hashCode()
  }

  /**
   * Returns a string describing the property set.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    keyValueMap.toString()
  }

}

object PropertySet {

  /**
   * The empty instance.
   */
  private val EMPTY: PropertySet = new PropertySet(ImmutableListMultimap.of[String, String]())

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty property set.
   *
   * The result contains no properties.
   *
   * @return an empty property set
   */
  def empty(): PropertySet = {
    EMPTY
  }

  /**
   * Obtains an instance from a map.
   *
   * The returned instance will have one value for each key.
   *
   * @param keyValues  the key-values to create the instance with
   * @return the property set
   */
  def of(keyValues: Map[String, String]): PropertySet = {
    ArgChecker.notNull(keyValues, "keyValues")
    val builder = ImmutableListMultimap.builder[String, String]()
    val it = keyValues.entrySet().iterator()
    while (it.hasNext()) {
      builder.put(it.next())
    }
    new PropertySet(builder.build())
  }

  /**
   * Obtains an instance from a map allowing for multiple values for each key.
   *
   * The returned instance may have more than one value for each key.
   *
   * @param keyValues  the key-values to create the instance with
   * @return the property set
   */
  def of(keyValues: Multimap[String, String]): PropertySet = {
    ArgChecker.notNull(keyValues, "keyValues")
    new PropertySet(ImmutableListMultimap.copyOf(keyValues))
  }

}
