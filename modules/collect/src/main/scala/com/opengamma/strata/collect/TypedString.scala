/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.io.Serializable
import java.util.regex.Pattern

import org.joda.convert.ToString

import com.google.common.base.CharMatcher
import com.opengamma.strata.collect.named.Named

/**
 * An abstract class designed to enable typed strings.
 *
 * The purpose of `TypedString` is to provide a Java type to a concept
 * that might otherwise be represented as a string.
 * It could be thought of as a way to provide a type alias for a string.
 *
 * The string wrapped by this type must not be empty.
 *
 * Subclasses must be written as follows:
 * {{{
 *  public final class FooType
 *      extends TypedString<FooType> {
 *    private static final long serialVersionUID = 1L;
 *    @FromString
 *    public static FooType of(String name) {
 *      return new FooType(name);
 *    }
 *    private FooType(String name) {
 *      super(name);
 *    }
 *  }
 * }}}
 *
 * The net result is that an API can be written with methods taking
 * `FooType` as a method parameter instead of `String`.
 *
 * @tparam T  the implementation subclass of this class
 *
 * @constructor Creates an instance.
 * @param rawName  the name, not empty
 */
@SerialVersionUID(1L)
abstract class TypedString[T <: TypedString[T]] protected (rawName: String)
  extends Named with Comparable[T] with Serializable {

  /**
   * The name.
   */
  private val name: String = ArgChecker.notEmpty(rawName, "name")

  /**
   * The cached hash code, using the racy single-check idiom.
   */
  @transient
  private var cacheHashCode: Int = 0

  /**
   * Creates an instance, validating the name against a regex.
   *
   * In most cases, a `CharMatcher` will be faster than a regex `Pattern`,
   * typically by over an order of magnitude.
   *
   * @param name  the name, not empty
   * @param pattern  the regex pattern for validating the name
   * @param msg  the message to use to explain validation failure
   */
  protected def this(name: String, pattern: Pattern, msg: String) = {
    this({
      ArgChecker.notEmpty(name, "name")
      ArgChecker.notNull(pattern, "pattern")
      ArgChecker.notEmpty(msg, "msg")
      if (pattern.matcher(name).matches() == false) {
        throw new IllegalArgumentException(msg)
      }
      name
    })
  }

  /**
   * Creates an instance, validating the name against a matcher.
   *
   * In most cases, a `CharMatcher` will be faster than a regex `Pattern`,
   * typically by over an order of magnitude.
   *
   * @param name  the name, not empty
   * @param matcher  the matcher for validating the name
   * @param msg  the message to use to explain validation failure
   */
  protected def this(name: String, matcher: CharMatcher, msg: String) = {
    this({
      ArgChecker.notEmpty(name, "name")
      ArgChecker.notNull(matcher, "pattern")
      ArgChecker.notEmpty(msg, "msg")
      if (matcher.matchesAllOf(name) == false) {
        throw new IllegalArgumentException(msg)
      }
      name
    })
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name.
   *
   * @return the name
   */
  override def getName(): String = {
    name
  }

  /**
   * Compares this type to another.
   *
   * Instances are compared in alphabetical order based on the name.
   *
   * @param other  the object to compare to
   * @return the comparison
   */
  override final def compareTo(other: T): Int = {
    name.compareTo(other.toString())
  }

  /**
   * Checks if this type equals another.
   *
   * Instances are compared based on the name.
   *
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  override final def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    if (obj != null && (obj.getClass() eq getClass())) {
      val other = obj.asInstanceOf[TypedString[?]]
      return name.equals(other.name)
    }
    false
  }

  /**
   * Returns a suitable hash code.
   *
   * @return a suitable hash code
   */
  override final def hashCode(): Int = {
    // threadsafe via racy single-check
    var h = cacheHashCode
    if (h == 0) {
      h = name.hashCode() ^ getClass().hashCode()
      cacheHashCode = h
    }
    h
  }

  /**
   * Returns the name.
   *
   * @return the string form, not empty
   */
  @ToString
  override final def toString(): String = {
    name
  }

}
