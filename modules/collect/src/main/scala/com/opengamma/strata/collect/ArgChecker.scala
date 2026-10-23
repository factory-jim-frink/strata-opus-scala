/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.util.Collection
import java.util.LinkedHashSet
import java.util.Map
import java.util.Set
import java.util.regex.Pattern

import scala.annotation.static
import scala.annotation.varargs

import com.google.common.base.CharMatcher
import com.google.common.math.DoubleMath
import com.opengamma.strata.collect.array.Matrix

/**
 * Contains utility methods for checking inputs to methods.
 *
 * This utility is used throughout the system to validate inputs to methods.
 * Most of the methods return their validated input, allowing patterns like this:
 * {{{
 *  // constructor
 *  public Person(String name, int age) {
 *    this.name = ArgChecker.notBlank(name, "name");
 *    this.age = ArgChecker.notNegative(age, "age");
 * }}}
 */
final class ArgChecker private ()

// The isTrue/isFalse overloads are plain companion methods, so that the Java varargs forms keep their
// static forwarders (a same-named @static method would suppress them). They throw via the @static
// argumentFailure helper so that ArgChecker remains the top stack-frame class. All other public methods
// are true @static methods of ArgChecker for the same reason.
object ArgChecker {

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified boolean is true.
   *
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isTrue(collection.contains("value"));
   * }}}
   *
   * It is strongly recommended to pass an additional message argument using
   * `isTrue(boolean, String)`.
   *
   * @param validIfTrue  a boolean resulting from testing an argument
   * @throws java.lang.IllegalArgumentException if the test value is false
   */
  def isTrue(validIfTrue: Boolean): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw argumentFailure("Invalid argument, expression must be true")
    }
  }

  /**
   * Checks that the specified boolean is true.
   *
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isTrue(collection.contains("value"), "Collection must contain 'value'");
   * }}}
   *
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message, not null
   * @throws java.lang.IllegalArgumentException if the test value is false
   */
  def isTrue(validIfTrue: Boolean, message: String): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw argumentFailure(message)
    }
  }

  /**
   * Checks that the specified boolean is true.
   *
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isTrue(collection.contains("value"), "Collection must contain 'value': {}", collection);
   * }}}
   *
   * This returns `void`, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments
   * @throws java.lang.IllegalArgumentException if the test value is false
   */
  @varargs
  def isTrue(validIfTrue: Boolean, message: String, arg: Object*): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw argumentFailure(Messages.format(message, arg*))
    }
  }

  /**
   * Checks that the specified boolean is true.
   *
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isTrue(value > check, "Value must be greater than check: {}", value);
   * }}}
   *
   * This returns `void`, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   *
   * The message is produced using a template that contains zero or one "{}" placeholders.
   * The placeholder, if present, is replaced by the argument.
   * If there is no placeholder, the argument is appended to the end of the message.
   *
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message argument
   * @throws java.lang.IllegalArgumentException if the test value is false
   */
  def isTrue(validIfTrue: Boolean, message: String, arg: Long): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw argumentFailure(Messages.format(message, java.lang.Long.valueOf(arg): Object))
    }
  }

  /**
   * Checks that the specified boolean is true.
   *
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isTrue(value > check, "Value must be greater than check: {}", value);
   * }}}
   *
   * This returns `void`, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   *
   * The message is produced using a template that contains zero or one "{}" placeholders.
   * The placeholder, if present, is replaced by the argument.
   * If there is no placeholder, the argument is appended to the end of the message.
   *
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message argument
   * @throws java.lang.IllegalArgumentException if the test value is false
   */
  def isTrue(validIfTrue: Boolean, message: String, arg: Double): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw argumentFailure(Messages.format(message, java.lang.Double.valueOf(arg): Object))
    }
  }

  /**
   * Checks that the specified boolean is false.
   *
   * Given the input argument, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value'");
   * }}}
   *
   * This returns `void`, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   *
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message, not null
   * @throws java.lang.IllegalArgumentException if the test value is true
   */
  def isFalse(validIfFalse: Boolean, message: String): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (validIfFalse) {
      throw argumentFailure(message)
    }
  }

  /**
   * Checks that the specified boolean is false.
   *
   * Given the input argument, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * {{{
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value': {}", collection);
   * }}}
   *
   * This returns `void`, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments, not null
   * @throws java.lang.IllegalArgumentException if the test value is true
   */
  @varargs
  def isFalse(validIfFalse: Boolean, message: String, arg: Object*): Unit = {
    // return void, not the argument, as no need to check a boolean method argument
    if (validIfFalse) {
      throw argumentFailure(Messages.format(message, arg*))
    }
  }

  // creates the exception in ArgChecker itself, so that it is the top stack-frame class
  @static private def argumentFailure(message: String): IllegalArgumentException = {
    new IllegalArgumentException(message)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null.
   *
   * Given the input argument, this returns only if it is non-null.
   * For example, in a constructor:
   * {{{
   *  this.name = ArgChecker.notNull(name, "name");
   * }}}
   *
   * @tparam T the type of the input argument reflected in the result
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null
   */
  @static def notNull[T](argument: T, name: String): T = {
    if (argument == null) {
      throw new IllegalArgumentException(notNullMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notNullMsg(name: String): String = {
    "Argument '" + name + "' must not be null"
  }

  /**
   * Checks that the specified item is non-null.
   *
   * Given the input argument, this returns only if it is non-null.
   * One use for this method is in a stream:
   * {{{
   *  ArgChecker.notNull(coll, "coll")
   *  coll.stream()
   *    .map(ArgChecker::notNullItem)
   *    ...
   * }}}
   *
   * @tparam T the type of the input argument reflected in the result
   * @param argument  the argument to check, null throws an exception
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null
   */
  @static def notNullItem[T](argument: T): T = {
    if (argument == null) {
      throw new IllegalArgumentException("Argument array/collection/map must not contain null")
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and matches the specified pattern.
   *
   * Given the input argument, this returns only if it is non-null and matches
   * the regular expression pattern specified.
   * For example, in a constructor:
   * {{{
   *  this.name = ArgChecker.matches(REGEX_NAME, name, "name");
   * }}}
   *
   * @param pattern  the pattern to check against, not null
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def matches(pattern: Pattern, argument: String, name: String): String = {
    notNull(pattern, "pattern")
    notNull(argument, name)
    if (!pattern.matcher(argument).matches()) {
      throw new IllegalArgumentException(matchesMsg(pattern, name, argument))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def matchesMsg(pattern: Pattern, name: String, value: String): String = {
    "Argument '" + name + "' with value '" + value + "' must match pattern: " + pattern
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and matches the specified pattern.
   *
   * Given the input argument, this returns only if it is non-null and matches
   * the regular expression pattern specified.
   * For example, in a constructor:
   * {{{
   *  this.name = ArgChecker.matches(REGEX_NAME, name, "name");
   * }}}
   *
   * @param pattern  the pattern to check against, not null
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def matches(
      matcher: CharMatcher,
      minLength: Int,
      maxLength: Int,
      argument: String,
      name: String,
      equivalentRegex: String): String = {

    notNull(matcher, "pattern")
    notNull(argument, name)
    if (argument.length() < minLength || argument.length() > maxLength || !matcher.matchesAllOf(argument)) {
      throw new IllegalArgumentException(matchesMsg(matcher, name, argument, equivalentRegex))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def matchesMsg(matcher: CharMatcher, name: String, value: String, equivalentRegex: String): String = {
    "Argument '" + name + "' with value '" + value + "' must match pattern: " + equivalentRegex
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and not blank.
   *
   * Given the input argument, this returns the input only if it is non-null
   * and contains at least one non whitespace character.
   * This is often linked with a call to `trim()`.
   * For example, in a constructor:
   * {{{
   *  this.name = ArgChecker.notBlank(name, "name").trim();
   * }}}
   *
   * The argument is trimmed using `String.trim()` to determine if it is empty.
   * The result is the original argument, not the trimmed one.
   *
   * @param argument  the argument to check, null or blank throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or blank
   */
  @static def notBlank(argument: String, name: String): String = {
    notNull(argument, name)
    if (argument.trim().isEmpty()) {
      throw new IllegalArgumentException(notBlankMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notBlankMsg(name: String): String = {
    "Argument '" + name + "' must not be blank"
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one character, which may be a whitespace character.
   * See also `notBlank(String, String)`.
   * For example, in a constructor:
   * {{{
   *  this.name = ArgChecker.notEmpty(name, "name");
   * }}}
   *
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty(argument: String, name: String): String = {
    notNull(argument, name)
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notEmptyMsg(name: String): String = {
    "Argument '" + name + "' must not be empty"
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * {{{
   *  this.names = ArgChecker.notEmpty(names, "names");
   * }}}
   *
   * @tparam T the type of the input array reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty[T <: AnyRef](argument: Array[T], name: String): Array[T] = {
    notNull(argument, name)
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name))
    }
    argument
  }

  /**
   * Checks that the specified argument is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * {{{
   *  this.names = ArgChecker.notEmpty(names, "names");
   * }}}
   *
   * @tparam T the type of the argument reflected in the result
   * @param argument the argument to check, null or empty throws an exception
   * @param name the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty[T <: Matrix](argument: T, name: String): T = {
    notNull(argument, name)
    if (argument.size() == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notEmptyArrayMsg(name: String): String = {
    "Argument array '" + name + "' must not be empty"
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.notEmpty(values, "values");
   * }}}
   *
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty(argument: Array[Int], name: String): Array[Int] = {
    notNull(argument, name)
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name))
    }
    argument
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.notEmpty(values, "values");
   * }}}
   *
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty(argument: Array[Long], name: String): Array[Long] = {
    notNull(argument, name)
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name))
    }
    argument
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.notEmpty(values, "values");
   * }}}
   *
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty(argument: Array[Double], name: String): Array[Double] = {
    notNull(argument, name)
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name))
    }
    argument
  }

  /**
   * Checks that the specified argument iterable is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.notEmpty(values, "values");
   * }}}
   *
   * @tparam T the element type of the input iterable reflected in the result
   * @tparam I the type of the input iterable, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty[T, I <: java.lang.Iterable[T]](argument: I, name: String): I = {
    notNull(argument, name)
    if (!argument.iterator().hasNext()) {
      throw new IllegalArgumentException(notEmptyIterableMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notEmptyIterableMsg(name: String): String = {
    "Argument iterable '" + name + "' must not be empty"
  }

  /**
   * Checks that the specified argument collection is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains at least one element.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.notEmpty(values, "values");
   * }}}
   *
   * @tparam T the element type of the input collection reflected in the result
   * @tparam C the type of the input collection, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty[T, C <: Collection[T]](argument: C, name: String): C = {
    notNull(argument, name)
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyCollectionMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notEmptyCollectionMsg(name: String): String = {
    "Argument collection '" + name + "' must not be empty"
  }

  /**
   * Checks that the specified argument map is non-null and not empty.
   *
   * Given the input argument, this returns only if it is non-null and contains at least one mapping.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * {{{
   *  this.keyValues = ArgChecker.notEmpty(keyValues, "keyValues");
   * }}}
   *
   * @tparam K the key type of the input map key, reflected in the result
   * @tparam V the value type of the input map value, reflected in the result
   * @tparam M the type of the input map, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or empty
   */
  @static def notEmpty[K, V, M <: Map[K, V]](argument: M, name: String): M = {
    notNull(argument, name)
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyMapMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notEmptyMapMsg(name: String): String = {
    "Argument map '" + name + "' must not be empty"
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument array is non-null and contains no nulls.
   *
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.noNulls(values, "values");
   * }}}
   *
   * @tparam T the type of the input array reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or contains nulls
   */
  @static def noNulls[T <: AnyRef](argument: Array[T], name: String): Array[T] = {
    notNull(argument, name)
    var i = 0
    while (i < argument.length) {
      if (argument(i) eq null) {
        throw new IllegalArgumentException("Argument array '" + name + "' must not contain null at index " + i)
      }
      i += 1
    }
    argument
  }

  /**
   * Checks that the specified argument collection is non-null and contains no nulls.
   *
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.noNulls(values, "values");
   * }}}
   *
   * @tparam T the element type of the input iterable reflected in the result
   * @tparam I the type of the input iterable, reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or contains nulls
   */
  @static def noNulls[T, I <: java.lang.Iterable[T]](argument: I, name: String): I = {
    notNull(argument, name)
    val it = argument.iterator()
    while (it.hasNext()) {
      val obj: Any = it.next()
      if (obj == null) {
        throw new IllegalArgumentException("Argument iterable '" + name + "' must not contain null")
      }
    }
    argument
  }

  /**
   * Checks that the specified argument map is non-null and contains no nulls.
   *
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * {{{
   *  this.keyValues = ArgChecker.noNulls(keyValues, "keyValues");
   * }}}
   *
   * @tparam K the key type of the input map key, reflected in the result
   * @tparam V the value type of the input map value, reflected in the result
   * @tparam M the type of the input map, reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or contains nulls
   */
  @static def noNulls[K, V, M <: Map[K, V]](argument: M, name: String): M = {
    notNull(argument, name)
    val it = argument.entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      if (entry.getKey() == null) {
        throw new IllegalArgumentException("Argument map '" + name + "' must not contain a null key")
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException("Argument map '" + name + "' must not contain a null value")
      }
    }
    argument
  }

  //-----------------------------------------------------------------------
  /**
   * Checks that the specified argument array is non-null and does not contain any duplicate values.
   *
   * Given the input argument, this returns only if it is non-null and does not contain duplicate values.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.noDuplicates(values, "values");
   * }}}
   *
   * If you know the argument is sorted increasing then `noDuplicatesSorted(double[], String)` might be more
   * performant.
   *
   * @param argument  the argument to check, null or duplicate values throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null or contains duplicate values
   */
  @static def noDuplicates(argument: Array[Double], name: String): Array[Double] = {
    notNull(argument, name)
    if (argument.length > 1) {
      val seen: Set[java.lang.Double] = new LinkedHashSet[java.lang.Double]()
      var i = 0
      while (i < argument.length) {
        if (!seen.add(java.lang.Double.valueOf(argument(i)))) {
          throw new IllegalArgumentException(noDuplicatesArrayMsg(name))
        }
        i += 1
      }
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def noDuplicatesArrayMsg(name: String): String = {
    "Argument array '" + name + "' must not contain duplicates"
  }

  /**
   * Checks that the specified argument array is non-null, sorted, and does not contain any duplicate values.
   *
   * Given the input argument, this returns only if it is non-null, sorted, and does not contain duplicate values.
   * For example, in a constructor:
   * {{{
   *  this.values = ArgChecker.noDuplicatesSorted(values, "values");
   * }}}
   *
   * @param argument  the argument to check, null, out of order or duplicate values throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`, not null
   * @throws java.lang.IllegalArgumentException if the input is null, unsorted, or contains duplicate values
   */
  @static def noDuplicatesSorted(argument: Array[Double], name: String): Array[Double] = {
    notNull(argument, name)
    var i = 1
    while (i < argument.length) {
      if (argument(i) == argument(i - 1)) {
        throw new IllegalArgumentException(noDuplicatesArrayMsg(name))
      } else if (argument(i) < argument(i - 1)) {
        throw new IllegalArgumentException(noDuplicatesSortedArrayMsg(name))
      }
      i += 1
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def noDuplicatesSortedArrayMsg(name: String): String = {
    "Argument array '" + name + "' must be sorted and not contain duplicates"
  }

  //-------------------------------------------------------------------------

  /**
   * Checks that the argument is not positive.
   *
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is positive
   */
  @static def notPositive(argument: Int, name: String): Int = {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, Integer.valueOf(argument)))
    }
    argument
  }

  @static private def notPositiveMsg(name: String, argument: Object): String = {
    "Argument '" + name + "' must not be positive but has value " + argument
  }

  /**
   * Checks that the argument is not positive.
   *
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is positive
   */
  @static def notPositive(argument: Long, name: String): Long = {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, java.lang.Long.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is not positive.
   *
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is positive
   */
  @static def notPositive(argument: Double, name: String): Double = {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, java.lang.Double.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is not positive.
   *
   * Given the input argument, this returns only if it is not null and is less than or equal to zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is null or positive
   */
  @static def notPositive(argument: Decimal, name: String): Decimal = {
    notNull(argument, name)
    if (argument.unscaledValue() > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument))
    }
    argument
  }

  /**
   * Checks that the argument is not positive if the arguement is not null.
   *
   * Given the input argument, this returns only if it is null or if it is less than or equal to zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notPositiveIfPresent(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is not null and is positive
   */
  @static def notPositiveIfPresent(argument: Decimal, name: String): Decimal = {
    if (argument != null && argument.unscaledValue() > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative.
   *
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative
   */
  @static def notNegative(argument: Int, name: String): Int = {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, Integer.valueOf(argument)))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notNegativeMsg(name: String, argument: Object): String = {
    "Argument '" + name + "' must not be negative but has value " + argument
  }

  /**
   * Checks that the argument is not negative.
   *
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative
   */
  @static def notNegative(argument: Long, name: String): Long = {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, java.lang.Long.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is not negative.
   *
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative
   */
  @static def notNegative(argument: Double, name: String): Double = {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, java.lang.Double.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is not negative.
   *
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative
   */
  @static def notNegative(argument: Decimal, name: String): Decimal = {
    if (argument.unscaledValue() < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, argument))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is a number and not NaN.
   *
   * Given the input argument, this returns only if it is an actual number.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNaN(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is NaN
   */
  @static def notNaN(argument: Double, name: String): Double = {
    if (java.lang.Double.isNaN(argument)) {
      throw new IllegalArgumentException(notNaNMsg(name))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notNaNMsg(name: String): String = {
    "Argument '" + name + "' must not be NaN"
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative or zero.
   *
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative or zero
   */
  @static def notNegativeOrZero(argument: Int, name: String): Int = {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, Integer.valueOf(argument)))
    }
    argument
  }

  // extracted to aid inlining performance
  @static private def notNegativeOrZeroMsg(name: String, argument: Object): String = {
    "Argument '" + name + "' must not be negative or zero but has value " + argument
  }

  /**
   * Checks that the argument is not negative or zero.
   *
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative or zero
   */
  @static def notNegativeOrZero(argument: Long, name: String): Long = {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, java.lang.Long.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is not negative or zero.
   *
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative or zero
   */
  @static def notNegativeOrZero(argument: Double, name: String): Double = {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, java.lang.Double.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is greater than zero to within a given accuracy.
   *
   * Given the input argument, this returns only if it is greater than zero
   * using the `eps` accuracy for zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegativeOrZero(amount, 0.0001d, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the absolute value of the argument is less than eps
   */
  @static def notNegativeOrZero(argument: Double, tolerance: Double, name: String): Double = {
    if (DoubleMath.fuzzyEquals(argument, 0d, tolerance)) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero")
    }
    if (argument < 0) {
      throw new IllegalArgumentException("Argument '" + name + "' must be greater than zero but has value " + argument)
    }
    argument
  }

  /**
   * Checks that the argument is not negative or zero.
   *
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * }}}
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the input is negative or zero
   */
  @static def notNegativeOrZero(argument: Decimal, name: String): Decimal = {
    if (argument.unscaledValue() <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, argument))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not equal to zero.
   *
   * Given the input argument, this returns only if it is not zero.
   * Both positive and negative zero are checked.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notZero(amount, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is zero
   */
  @static def notZero(argument: Double, name: String): Double = {
    if (argument == 0d || argument == -0d) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero")
    }
    argument
  }

  /**
   * Checks that the argument is not equal to zero to within a given accuracy.
   *
   * Given the input argument, this returns only if it is not zero comparing
   * using the `eps` accuracy.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.notZero(amount, 0.0001d, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the absolute value of the argument is less than the tolerance
   */
  @static def notZero(argument: Double, tolerance: Double, name: String): Double = {
    if (DoubleMath.fuzzyEquals(argument, 0d, tolerance)) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero")
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by `low <= x < high`.
   *
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRange(amount, 0d, 1d, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRange(argument: Double, lowInclusive: Double, highExclusive: Double, name: String): Double = {
    if (argument < lowInclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}",
            java.lang.Double.valueOf(lowInclusive), name,
            java.lang.Double.valueOf(highExclusive), java.lang.Double.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low <= x <= high`.
   *
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRangeInclusive(amount, 0d, 1d, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeInclusive(argument: Double, lowInclusive: Double, highInclusive: Double, name: String): Double = {
    if (argument < lowInclusive || argument > highInclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}",
            java.lang.Double.valueOf(lowInclusive), name,
            java.lang.Double.valueOf(highInclusive), java.lang.Double.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low < x < high`.
   *
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRangeExclusive(amount, 0d, 1d, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeExclusive(argument: Double, lowExclusive: Double, highExclusive: Double, name: String): Double = {
    if (argument <= lowExclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}",
            java.lang.Double.valueOf(lowExclusive), name,
            java.lang.Double.valueOf(highExclusive), java.lang.Double.valueOf(argument)))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by `low <= x < high`.
   *
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRange(amount, 0, 1, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRange(argument: Int, lowInclusive: Int, highExclusive: Int, name: String): Int = {
    if (argument < lowInclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}",
            java.lang.Integer.valueOf(lowInclusive), name,
            java.lang.Integer.valueOf(highExclusive), java.lang.Integer.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low <= x <= high`.
   *
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRangeInclusive(amount, 0, 1, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeInclusive(argument: Int, lowInclusive: Int, highInclusive: Int, name: String): Int = {
    if (argument < lowInclusive || argument > highInclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}",
            java.lang.Integer.valueOf(lowInclusive), name,
            java.lang.Integer.valueOf(highInclusive), java.lang.Integer.valueOf(argument)))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low < x < high`.
   *
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.amount = ArgChecker.inRangeExclusive(amount, 0, 1, "amount");
   * }}}
   *
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeExclusive(argument: Int, lowExclusive: Int, highExclusive: Int, name: String): Int = {
    if (argument <= lowExclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}",
            java.lang.Integer.valueOf(lowExclusive), name,
            java.lang.Integer.valueOf(highExclusive), java.lang.Integer.valueOf(argument)))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by `low <= x < high` using the items' natural order.
   *
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * {{{
   *  this.duration = ArgChecker.inRangeComparable(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * }}}
   *
   * @tparam T the type of the value
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeComparable[T <: Comparable[T]](argument: T, lowInclusive: T, highExclusive: T, name: String): T = {
    if (argument == null || argument.compareTo(lowInclusive) < 0 || argument.compareTo(highExclusive) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}", lowInclusive, name, highExclusive, argument))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low <= x <= high` using the items' natural order.
   *
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.duration = ArgChecker.inRangeComparableInclusive(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * }}}
   *
   * @tparam T the type of the value
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeComparableInclusive[T <: Comparable[T]](
      argument: T,
      lowInclusive: T,
      highInclusive: T,
      name: String): T = {

    if (argument == null || argument.compareTo(lowInclusive) < 0 || argument.compareTo(highInclusive) > 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}", lowInclusive, name, highInclusive, argument))
    }
    argument
  }

  /**
   * Checks that the argument is within the range defined by `low < x < high` using the items' natural order.
   *
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * {{{
   *  this.duration = ArgChecker.inRangeComparableExclusive(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * }}}
   *
   * @tparam T the type of the value
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input `argument`
   * @throws java.lang.IllegalArgumentException if the argument is outside the valid range
   */
  @static def inRangeComparableExclusive[T <: Comparable[T]](
      argument: T,
      lowExclusive: T,
      highExclusive: T,
      name: String): T = {

    if (argument == null || argument.compareTo(lowExclusive) <= 0 || argument.compareTo(highExclusive) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}", lowExclusive, name, highExclusive, argument))
    }
    argument
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the two values are in order and not equal.
   *
   * Given two comparable instances, this checks that the first is "less than" the second.
   * Two equal values also throw the exception.
   *
   * @tparam T the type
   * @param obj1  the first object, null throws an exception
   * @param obj2  the second object, null throws an exception
   * @param name1  the first argument name, not null
   * @param name2  the second argument name, not null
   * @throws java.lang.IllegalArgumentException if either input is null or they are not in order
   */
  @static def inOrderNotEqual[T](obj1: Comparable[? >: T], obj2: T, name1: String, name2: String): Unit = {
    notNull(obj1, name1)
    notNull(obj2, name2)
    if (obj1.compareTo(obj2) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Invalid order: Expected '{}' < '{}', but found: '{}' >= '{}'",
            name1, name2, obj1.asInstanceOf[Object], obj2.asInstanceOf[Object]))
    }
  }

  /**
   * Checks that the two values are in order or equal.
   *
   * Given two comparable instances, this checks that the first is "less than" or "equal to" the second.
   *
   * @tparam T the type
   * @param obj1  the first object, null throws an exception
   * @param obj2  the second object, null throws an exception
   * @param name1  the first argument name, not null
   * @param name2  the second argument name, not null
   * @throws java.lang.IllegalArgumentException if either input is null or they are not in order
   */
  @static def inOrderOrEqual[T](obj1: Comparable[? >: T], obj2: T, name1: String, name2: String): Unit = {
    notNull(obj1, name1)
    notNull(obj2, name2)
    if (obj1.compareTo(obj2) > 0) {
      throw new IllegalArgumentException(
          Messages.format("Invalid order: Expected '{}' <= '{}', but found: '{}' > '{}'",
            name1, name2, obj1.asInstanceOf[Object], obj2.asInstanceOf[Object]))
    }
  }

}
