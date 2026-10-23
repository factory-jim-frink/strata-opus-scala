/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamField
import java.io.Serializable
import java.util.Arrays
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Objects
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Stream

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs
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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate

/**
 * The result of an operation, either success or failure.
 *
 * This provides a functional approach to error handling, that can be used instead of exceptions.
 * A success result contains a non-null result value.
 * A failure result contains details of the failure that occurred.
 *
 * Methods using this approach to error handling are expected to return `Result<T>`
 * and not throw exceptions. The factory method `of(Supplier)` and related methods
 * can be used to capture exceptions and convert them to failure results.
 *
 * Application code using a result should also operate in a functional style.
 * Use `map(Function)` and `flatMap(Function)` in preference to
 * `isSuccess()` and `getValue()`.
 * {{{
 *  Result<Foo> intermediateResult = calculateIntermediateResult();
 *  return intermediateResult.flatMap(foo -> calculateFinalResult(foo, ...));
 * }}}
 *
 * Results can be generated using the factory methods on this class.
 *
 * @tparam T the type of the underlying result for a successful invocation
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class Result[T] private (
    initialValue: T,
    initialFailure: Failure)
  extends ImmutableBean with Serializable {
  // two properties are used where one might do to reduce serialized data size

  // the fields are only assigned here and by readObject()
  /**
   * The value.
   * This is only present if the result is a success.
   */
  @(PropertyDefinition @field)(get = "field")
  private var value: T = initialValue
  // a field named 'failure' would suppress the static 'failure' forwarders on this class,
  // thus the field is named 'failureField' and Java serialization maps it to 'failure'
  /**
   * The failure.
   * This is only present if the result is a failure.
   */
  @(PropertyDefinition @field)(get = "field")
  private var failureField: Failure = initialFailure

  // reads the fields of another instance
  private def valueOrNull(): T = {
    value
  }

  private def failureOrNull(): Failure = {
    failureField
  }

  @ImmutableValidator
  private def validate(): Unit = {
    if (value == null && failureField == null) {
      throw new IllegalArgumentException("Both value and failure are null")
    }
    if (value != null && failureField != null) {
      throw new IllegalArgumentException("Both value and failure are non-null")
    }
  }

  // writes the baseline serialized form, fields 'failure' and 'value'
  @throws[IOException]
  private def writeObject(out: ObjectOutputStream): Unit = {
    val fields = out.putFields()
    fields.put("failure", failureField)
    fields.put("value", value.asInstanceOf[Object])
    out.writeFields()
  }

  // reads the baseline serialized form, fields 'failure' and 'value'
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream): Unit = {
    val fields = in.readFields()
    failureField = fields.get("failure", null: Object).asInstanceOf[Failure]
    value = fields.get("value", null: Object).asInstanceOf[T]
  }

  //-------------------------------------------------------------------------
  /**
   * Indicates if this result represents a successful call and has a result available.
   *
   * This is the opposite of `isFailure()`.
   *
   * @return true if the result represents a success and a value is available
   */
  def isSuccess(): Boolean = {
    value != null
  }

  /**
   * Executes the given consumer if the result represents a successful call and has a result available.
   *
   * @param consumer the consumer to be decorated
   */
  def ifSuccess(consumer: Consumer[? >: T]): Unit = {
    if (value != null) {
      consumer.accept(value)
    }
  }

  /**
   * Indicates if this result represents a failure.
   *
   * This is the opposite of `isSuccess()`.
   *
   * @return true if the result represents a failure
   */
  def isFailure(): Boolean = {
    failureField != null
  }

  /**
   * Executes the given consumer if the result represents a failure.
   *
   * @param consumer the consumer to be decorated
   */
  def ifFailure(consumer: Consumer[Failure]): Unit = {
    if (failureField != null) {
      consumer.accept(failureField)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the result value if calculated successfully, empty if a failure occurred.
   *
   * @return the result value if success, empty if failure
   */
  def get(): Optional[T] = {
    Optional.ofNullable(value)
  }

  /**
   * Returns the actual result value if calculated successfully, throwing an
   * exception if a failure occurred.
   *
   * If this result is a failure then an `IllegalStateException` will be thrown.
   * To avoid this, call `isSuccess()` or `isFailure()` first.
   *
   * Application code is recommended to use `map(Function)` and
   * `flatMap(Function)` in preference to this method.
   *
   * @return the result value, only available if calculated successfully
   * @throws java.lang.IllegalStateException if called on a failure result
   */
  def getValue(): T = {
    if (isFailure()) {
      throw new IllegalStateException("Unable to get a value from a failure result: " + getFailure().getMessage())
    }
    value
  }

  /**
   * Returns the actual result value if calculated successfully, or the specified
   * default value if a failure occurred.
   *
   * If this result is a success then the result value is returned.
   * If this result is a failure then the default value is returned.
   * The default value may be null.
   *
   * Application code is recommended to use `map(Function)` and
   * `flatMap(Function)` in preference to this method.
   *
   * @param defaultValue  the default value to return if the result is a failure
   * @return either the result value or the default value
   */
  def getValueOrElse(defaultValue: T): T = {
    if (isSuccess()) value else defaultValue
  }

  /**
   * Returns the actual result value if calculated successfully, else the
   * specified function is applied to the `Failure` that occurred.
   *
   * If this result is a success then the result value is returned.
   * If this result is a failure then the function is applied to the failure.
   * The function must not be null.
   *
   * This method can be used in preference to `getValueOrElse(Object)`
   * when the default value is expensive to create. In such cases, the
   * default value will get created on each call, even though it will be
   * immediately discarded if the result is a success.
   *
   * @param mapper  function used to generate a default value. The function
   *   has no obligation to use the input `Failure` (in other words it can
   *   behave as a `Supplier<T>` if desired).
   * @return either the result value or the result of the function
   */
  def getValueOrElseApply(mapper: Function[Failure, T]): T = {
    ArgChecker.notNull(mapper, "mapper")
    if (isSuccess()) value else mapper.apply(failureField)
  }

  /**
   * Returns the failure instance indicating the reason why the calculation failed.
   *
   * If this result is a success then an IllegalStateException will be thrown.
   * To avoid this, call `isSuccess()` or `isFailure()` first.
   *
   * @return the details of the failure, only available if calculation failed
   * @throws java.lang.IllegalStateException if called on a success result
   */
  def getFailure(): Failure = {
    if (isSuccess()) {
      throw new IllegalStateException("Unable to get a failure from a success result")
    }
    failureField
  }

  //-------------------------------------------------------------------------
  /**
   * Processes a successful result by applying a function that alters the value.
   *
   * This operation allows post-processing of a result value.
   * The specified function represents a conversion to be performed on the value.
   *
   * If this result is a success, then the specified function is invoked.
   * The return value of the specified function is returned to the caller
   * wrapped in a success result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure `Result` returned.
   *
   * If this result is a failure, then `this` is returned.
   * The specified function is not invoked.
   *
   * For example, it allows a `double` to be converted to a string:
   * {{{
   *   result = ...
   *   return result.map(value -> Double.toString(value));
   * }}}
   *
   * @tparam R  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the new result
   */
  def map[R](function: Function[? >: T, ? <: R]): Result[R] = {
    if (isSuccess()) {
      try {
        Result.success[R](function.apply(value))
      } catch {
        case ex: Exception =>
          Result.failure[R](ex)
      }
    } else {
      Result.failure[R](this: Result[?])
    }
  }

  /**
   * Processes a failed result by applying a function that alters the failure.
   *
   * This operation allows post-processing of a result failure.
   * The specified function represents a conversion to be performed on the failure.
   *
   * If this result is a failure, then the specified function is invoked.
   * The return value of the specified function is returned to the caller
   * wrapped in a failure result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure `Result` returned.
   *
   * If this result is a success, then `this` is returned.
   * The specified function is not invoked.
   *
   * @param function  the function to transform the failure with
   * @return the new result
   */
  def mapFailure(function: Function[Failure, Failure]): Result[T] = {
    if (isFailure()) {
      try {
        Result.failure[T](function.apply(failureField))
      } catch {
        case ex: Exception =>
          Result.failure[T](ex)
      }
    } else {
      this
    }
  }

  /**
   * Processes a failed result by applying a function that alters the failure items.
   *
   * This operation allows post-processing of a result failure.
   * The specified function represents a conversion to be performed on the failure.
   *
   * If this result is a failure, then the specified function is invoked.
   * The return values of the specified function is returned to the caller
   * wrapped in a failure result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure `Result` returned.
   *
   * If this result is a success, then `this` is returned.
   * The specified function is not invoked.
   *
   * @param function  the function to transform the failure with
   * @return the new result
   */
  def mapFailureItems(function: Function[FailureItem, FailureItem]): Result[T] = {
    if (isFailure()) {
      try {
        Result.failure[T](failureField.mapItems(function))
      } catch {
        case ex: Exception =>
          Result.failure[T](ex)
      }
    } else {
      this
    }
  }

  /**
   * Processes a successful result by applying a function that returns another result.
   *
   * This operation allows chaining of function calls that produce a result.
   * The specified function will typically call another method that returns a result.
   *
   * If this result is a success, then the specified function is invoked.
   * The return value of the specified function is returned to the caller and may be
   * a success or failure. If an exception is thrown when the function
   * is invoked, this will be caught and a failure `Result` returned.
   *
   * If this result is a failure, then an equivalent failure is returned.
   * The specified function is not invoked.
   *
   * For example,
   * {{{
   *   result = ...
   *   return result.flatMap(value -> doSomething(value));
   * }}}
   *
   * @tparam R  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the new result
   */
  def flatMap[R](function: Function[? >: T, Result[R]]): Result[R] = {
    if (isSuccess()) {
      try {
        Objects.requireNonNull(function.apply(value))
      } catch {
        case ex: Exception =>
          Result.failure[R](ex)
      }
    } else {
      Result.failure[R](this: Result[?])
    }
  }

  /**
   * Combines this result with another result.
   *
   * This operation allows two results to be combined handling succeess and failure.
   *
   * If both results are a success, then the specified function is invoked to combine them.
   * The return value of the specified function is returned to the caller and may be
   * a success or failure.
   *
   * If either result is a failure, then a combination failure is returned.
   * The specified function is not invoked.
   * {{{
   *   result1 = ...
   *   result2 = ...
   *   return result1.combineWith(result2, (value1, value2) -> doSomething(value1, value2));
   * }}}
   *
   * @param other  another result
   * @param function  a function for combining values from two results
   * @tparam U  the type of the value in the other result
   * @tparam R  the type of the value in the returned result
   * @return a the result of combining the result values or a failure if either result is a failure
   */
  def combineWith[U, R](other: Result[U], function: BiFunction[T, U, Result[R]]): Result[R] = {
    ArgChecker.notNull(other, "other")
    ArgChecker.notNull(function, "function")
    if (isSuccess() && other.isSuccess()) {
      try {
        Objects.requireNonNull(function.apply(value, other.valueOrNull()))
      } catch {
        case ex: Exception =>
          Result.failure[R](ex)
      }
    } else {
      Result.failure[R](this, other)
    }
  }

  /**
   * Converts this result to a stream.
   *
   * If this result is a success then a single element stream containing the result value is returned.
   * If this result is a failure then an empty stream is returned.
   *
   * @return a stream of size one or zero
   */
  def stream(): Stream[T] = {
    if (isSuccess()) Stream.of[T](value) else Stream.empty[T]()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): Result.Meta[T] = {
    Result.Meta.INSTANCE.asInstanceOf[Result.Meta[T]]
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[Result[?]]
      JodaBeanUtils.equal(value, other.valueOrNull()) &&
          JodaBeanUtils.equal(failureField, other.failureOrNull())
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(value)
    hash = hash * 31 + JodaBeanUtils.hashCode(failureField)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(96)
    buf.append("Result{")
    buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ')
    buf.append("failure").append('=').append(JodaBeanUtils.toString(failureField))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object Result {

  // the baseline serialized form, with the field names of the Java implementation
  @static private final val serialPersistentFields: Array[ObjectStreamField] = Array(
      new ObjectStreamField("failure", classOf[Failure]),
      new ObjectStreamField("value", classOf[Object]))

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Result.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a successful result wrapping a value.
   *
   * This returns a successful result object for the non-null value.
   *
   * Note that passing an instance of `Failure` to this method would
   * be a programming error.
   *
   * @tparam R the type of the value
   * @param value  the result value
   * @return a successful result wrapping the value
   */
  def success[R](value: R): Result[R] = {
    new Result[R](ArgChecker.notNull(value, "value"), null)
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a failed result specifying the failure reason.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param reason  the result reason
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  @varargs
  def failure[R](reason: FailureReason, message: String, messageArgs: Object*): Result[R] = {
    ofFailure[R](Failure.of(FailureItem.ofAutoStackTrace(1, reason, message, messageArgs*)))
  }

  /**
   * Creates a success `Result` wrapping the value produced by the
   * supplier.
   *
   * Note that if the supplier throws an exception, this will be caught
   * and converted to a failure `Result`.
   * This recognizes and handles `FailureException` and `FailureItemProvider` exceptions.
   *
   * @tparam T the type of the value
   * @param supplier  supplier of the result value
   * @return a success `Result` wrapping the value produced by the supplier
   */
  def of[T](supplier: Supplier[T]): Result[T] = {
    try {
      success(supplier.get())
    } catch {
      case ex: Exception =>
        failure[T](ex)
    }
  }

  /**
   * Creates a `Result` wrapping the result produced by the supplier.
   *
   * Note that if the supplier throws an exception, this will be caught
   * and converted to a failure `Result`.
   * This recognizes and handles `FailureException` and `FailureItemProvider` exceptions.
   *
   * @tparam T the type of the result value
   * @param supplier  supplier of the result
   * @return a `Result` produced by the supplier
   */
  def wrap[T](supplier: Supplier[Result[T]]): Result[T] = {
    try {
      supplier.get()
    } catch {
      case ex: Exception =>
        failure[T](ex)
    }
  }

  /**
   * Creates a failed result caused by a throwable.
   *
   * This recognizes and handles `FailureException` and `FailureItemProvider` exceptions.
   * If the exception type is not recognized, the failure will have a reason of `ERROR`.
   *
   * @tparam R the expected type of the result
   * @param cause  the cause of the failure
   * @return a failure result
   */
  def failure[R](cause: Throwable): Result[R] = {
    failure[R](Failure.from(cause))
  }

  /**
   * Creates a failed result caused by an exception.
   *
   * This recognizes and handles `FailureException` and `FailureItemProvider` exceptions.
   * If the exception type is not recognized, the failure will have a reason of `ERROR`.
   *
   * @tparam R the expected type of the result
   * @param cause  the cause of the failure
   * @return a failure result
   */
  def failure[R](cause: Exception): Result[R] = {
    // this method is retained to ensure binary compatibility
    failure[R](Failure.from(cause))
  }

  /**
   * Creates a failed result caused by a throwable.
   *
   * The failure will have a reason of `ERROR`.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  @varargs
  def failure[R](cause: Throwable, message: String, messageArgs: Object*): Result[R] = {
    ofFailure[R](Failure.of(FailureReason.ERROR, cause, message, messageArgs*))
  }

  /**
   * Creates a failed result caused by an exception.
   *
   * The failure will have a reason of `ERROR`.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  @varargs
  def failure[R](cause: Exception, message: String, messageArgs: Object*): Result[R] = {
    // this method is retained to ensure binary compatibility
    ofFailure[R](Failure.of(FailureReason.ERROR, cause, message, messageArgs*))
  }

  /**
   * Creates a failed result caused by a throwable with a specified reason.
   *
   * @tparam R the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @return a failure result
   */
  def failure[R](reason: FailureReason, cause: Throwable): Result[R] = {
    ofFailure[R](Failure.of(reason, cause))
  }

  /**
   * Creates a failed result caused by an exception with a specified reason.
   *
   * @tparam R the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @return a failure result
   */
  def failure[R](reason: FailureReason, cause: Exception): Result[R] = {
    // this method is retained to ensure binary compatibility
    ofFailure[R](Failure.of(reason, cause))
  }

  /**
   * Creates a failed result caused by a throwable with a specified reason and message.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  @varargs
  def failure[R](
      reason: FailureReason,
      cause: Throwable,
      message: String,
      messageArgs: Object*): Result[R] = {

    ofFailure[R](Failure.of(reason, cause, message, messageArgs*))
  }

  /**
   * Creates a failed result caused by an exception with a specified reason and message.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  @varargs
  def failure[R](
      reason: FailureReason,
      cause: Exception,
      message: String,
      messageArgs: Object*): Result[R] = {

    // this method is retained to ensure binary compatibility
    ofFailure[R](Failure.of(reason, cause, message, messageArgs*))
  }

  /**
   * Returns a failed result from another failed result.
   *
   * This method ensures the result type matches the expected type.
   * If the specified result is a successful result then an exception is thrown.
   *
   * @tparam R the expected result type
   * @param failureResult  a failure result
   * @return a failure result of the expected type
   * @throws java.lang.IllegalArgumentException if the result is a success
   */
  def failure[R](failureResult: Result[?]): Result[R] = {
    if (failureResult.isSuccess()) {
      throw new IllegalArgumentException("Result must be a failure")
    }
    failureResult.asInstanceOf[Result[R]]
  }

  /**
   * Creates a failed result combining multiple failed results.
   *
   * The input results can be successes or failures, only the failures will be included in the created result.
   * Intended to be used with `anyFailures(Result...)`.
   * {{{
   *   if (Result.anyFailures(result1, result2, result3) {
   *     return Result.failure(result1, result2, result3);
   *   }
   * }}}
   *
   * @tparam R the expected type of the result
   * @param result1  the first result
   * @param result2  the second result
   * @param results  the rest of the results
   * @return a failed result wrapping multiple other failed results
   * @throws java.lang.IllegalArgumentException if all of the results are successes
   */
  @varargs
  def failure[R](result1: Result[?], result2: Result[?], results: Result[?]*): Result[R] = {
    ArgChecker.notNull(result1, "result1")
    ArgChecker.notNull(result2, "result2")
    ArgChecker.notNull(results, "results")
    val list: ImmutableList[Result[?]] = ImmutableList.builder[Result[?]]()
        .add(result1)
        .add(result2)
        .addAll(Arrays.asList[Result[?]](results*))
        .build()
    failure[R](list: java.lang.Iterable[? <: Result[?]])
  }

  /**
   * Creates a failed result combining multiple failed results.
   *
   * The input results can be successes or failures, only the failures will be included in the created result.
   * Intended to be used with `anyFailures(Iterable)`.
   * {{{
   *   if (Result.anyFailures(results) {
   *     return Result.failure(results);
   *   }
   * }}}
   *
   * @tparam R the expected type of the result
   * @param results  multiple results, of which at least one must be a failure, not empty
   * @return a failed result wrapping multiple other failed results
   * @throws java.lang.IllegalArgumentException if results is empty or contains nothing but successes
   */
  def failure[R](results: java.lang.Iterable[? <: Result[?]]): Result[R] = {
    ArgChecker.notEmpty(results, "results")
    val items: ImmutableSet[FailureItem] = Guavate.stream(results.asInstanceOf[java.lang.Iterable[Result[?]]])
        .filter(r => r.isFailure())
        .map[Failure](r => r.getFailure())
        .flatMap[FailureItem](f => f.getItems().stream())
        .collect(Guavate.toImmutableSet[FailureItem]())
    if (items.isEmpty()) {
      throw new IllegalArgumentException("All results were successes")
    }
    ofFailure[R](Failure.of(items))
  }

  /**
   * Creates a failed result containing a failure.
   *
   * This is useful for converting an existing `Failure` instance to a result.
   *
   * @tparam R the expected type of the result
   * @param failure  details of the failure
   * @return a failed result containing the specified failure
   */
  def failure[R](failure: Failure): Result[R] = {
    ofFailure[R](failure)
  }

  /**
   * Creates a failed result containing a failure item.
   *
   * This is useful for converting an existing `FailureItem` instance to a result.
   *
   * @tparam R the expected type of the result
   * @param failureItem  details of the failure
   * @return a failed result containing the specified failure
   */
  def failure[R](failureItem: FailureItem): Result[R] = {
    ofFailure[R](Failure.of(failureItem))
  }

  // creates an unvalidated failure instance, equivalent to the private failure constructor
  private def ofFailure[R](failure: Failure): Result[R] = {
    new Result[R](null.asInstanceOf[R], failure)
  }

  /**
   * Returns a success result containing the value if it is non-null, else returns a failure result
   * with the specified reason and message.
   *
   * This is useful for interoperability with APIs that return `null`, for example `Map.get()`, where
   * a missing value should be treated as a failure.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param value  the potentially null value
   * @param reason  the reason for the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a success result if the value is non-null, else a failure result
   */
  @varargs
  def ofNullable[R](
      value: R,
      reason: FailureReason,
      message: String,
      messageArgs: Object*): Result[R] = {

    if (value != null) {
      success(value)
    } else {
      failure[R](reason, message, messageArgs*)
    }
  }

  /**
   * Returns a success result containing the value if it is non-null, else returns a failure result
   * with a reason of `FailureReason.MISSING_DATA` and message to say an unexpected null was found.
   *
   * This is useful for interoperability with APIs that can return `null` but where null is not expected.
   *
   * @tparam R the expected type of the result
   * @param value  the potentially null value
   * @return a success result if the value is non-null, else a failure result
   */
  def ofNullable[R](value: R): Result[R] = {
    ofNullable[R](value, FailureReason.MISSING_DATA, "Found null where a value was expected")
  }

  /**
   * Returns a success result containing the value if present, else returns a failure result
   * with the specified reason and message.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.format(String, Object...)` for more details.
   *
   * @tparam R the expected type of the result
   * @param value  the potentially null value
   * @param reason  the reason for the failure
   * @param message  a message explaining the failure, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return a success result if the value is non-null, else a failure result
   */
  @varargs
  def ofOptional[R](
      value: Optional[R],
      reason: FailureReason,
      message: String,
      messageArgs: Object*): Result[R] = {

    value
        .map[Result[R]](v => Result.success(v))
        .orElseGet(() => Result.failure[R](reason, message, messageArgs*))
  }

  /**
   * Returns a success result containing the value if present, else returns a failure result
   * with a reason of `FailureReason.MISSING_DATA` and message to say an unexpected empty value was found.
   *
   * @tparam R the expected type of the result
   * @param value  the potentially null value
   * @return a success result if the value is non-null, else a failure result
   */
  def ofOptional[R](value: Optional[R]): Result[R] = {
    ofOptional[R](value, FailureReason.MISSING_DATA, "Found empty where a value was expected")
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if all the results are successful.
   *
   * @param results  the results to check
   * @return true if all of the results are successes
   */
  @varargs
  def allSuccessful(results: Result[?]*): Boolean = {
    Stream.of[Result[?]](results*).allMatch(r => r.isSuccess())
  }

  /**
   * Checks if all the results are successful.
   *
   * @param results  the results to check
   * @return true if all of the results are successes
   */
  def allSuccessful(results: java.lang.Iterable[? <: Result[?]]): Boolean = {
    Guavate.stream(results.asInstanceOf[java.lang.Iterable[Result[?]]]).allMatch(r => r.isSuccess())
  }

  /**
   * Checks if any of the results are failures.
   *
   * @param results  the results to check
   * @return true if any of the results are failures
   */
  @varargs
  def anyFailures(results: Result[?]*): Boolean = {
    Stream.of[Result[?]](results*).anyMatch(r => r.isFailure())
  }

  /**
   * Checks if any of the results are failures.
   *
   * @param results  the results to check
   * @return true if any of the results are failures
   */
  def anyFailures(results: java.lang.Iterable[? <: Result[?]]): Boolean = {
    Guavate.stream(results.asInstanceOf[java.lang.Iterable[Result[?]]]).anyMatch(r => r.isFailure())
  }

  /**
   * Counts how many of the results are failures.
   *
   * @param results  the results to check
   * @return the number of results that are failures
   */
  @varargs
  def countFailures(results: Result[?]*): Long = {
    Stream.of[Result[?]](results*).filter(r => r.isFailure()).count()
  }

  /**
   * Counts how many of the results are failures.
   *
   * @param results  the results to check
   * @return the number of results that are failures
   */
  def countFailures(results: java.lang.Iterable[? <: Result[?]]): Long = {
    Guavate.stream(results.asInstanceOf[java.lang.Iterable[Result[?]]]).filter(r => r.isFailure()).count()
  }

  /**
   * Takes a collection of results, checks if all of them are successes
   * and then applies the supplied function to the successes wrapping
   * the result in a success result. If any of the initial results was
   * a failure, then a failure result reflecting the failures in the
   * initial results is returned.
   *
   * If an exception is thrown when the function is applied, this will
   * be caught and a failure `Result` returned.
   *
   * The following code shows where this method can be used. The code:
   * {{{
   *   Set<Result<MyData>> results = goAndGatherData();
   *   if (Result.anyFailures(results)) {
   *     return Result.failure(results);
   *   } else {
   *     Set<FooData> combined =
   *         results.stream()
   *             .map(Result::getValue)
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return Result.success(combined);
   *   }
   * }}}
   * can be replaced with:
   * {{{
   *   Set<Result<MyData>> results = goAndGatherData();
   *   return Result.combine(results, myDataStream ->
   *       myDataStream
   *           .map(MyData::transformToFoo)
   *           .collect(toSet())
   *   );
   * }}}
   *
   * @param results  the results to be transformed if they are all successes
   * @param function  the function to apply to the stream of results if they were all successes
   * @tparam T  the type of the values held in the input results
   * @tparam R  the type of the values held in the transformed results
   * @return a success result holding the result of applying the function to the
   *   input results if they were all successes, a failure otherwise
   */
  def combine[T, R](
      results: java.lang.Iterable[? <: Result[T]],
      function: Function[Stream[T], R]): Result[R] = {

    try {
      if (allSuccessful(results: java.lang.Iterable[? <: Result[?]])) {
        success(function.apply(extractSuccesses(results)))
      } else {
        failure[R](results: java.lang.Iterable[? <: Result[?]])
      }
    } catch {
      case ex: Exception =>
        failure[R](ex)
    }
  }

  /**
   * Takes a collection of results, checks if all of them are successes
   * and then applies the supplied function to the successes. If any of
   * the initial results was a failure, then a failure result reflecting
   * the failures in the initial results is returned.
   *
   * If an exception is thrown when the function is applied, this will
   * be caught and a failure `Result` returned.
   *
   * The following code shows where this method can be used. The code:
   * {{{
   *   Set<Result<MyData>> results = goAndGatherData();
   *   if (Result.anyFailures(results)) {
   *     return Result.failure(results);
   *   } else {
   *     Set<FooData> combined =
   *         results.stream()
   *             .map(Result::getValue)
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return doSomethingReturningResult(combined); // this could fail
   *   }
   * }}}
   * can be replaced with:
   * {{{
   *   Set<Result<MyData>> results = goAndGatherData();
   *   return Result.flatCombine(results, myDataStream -> {
   *     Set<CombinedData> combined =
   *         myDataStream
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return doSomethingReturningResult(combined); // this could fail
   *   });
   * }}}
   *
   * @param results  the results to be transformed if they are all successes
   * @param function  the function to apply to the stream of results if they were all successes
   * @tparam T  the type of the values held in the input results
   * @tparam R  the type of the values held in the transformed results
   * @return a result holding the result of applying the function to the
   *   input results if they were all successes, a failure otherwise
   */
  def flatCombine[T, R](
      results: java.lang.Iterable[? <: Result[T]],
      function: Function[Stream[T], Result[R]]): Result[R] = {

    try {
      if (allSuccessful(results: java.lang.Iterable[? <: Result[?]])) {
        function.apply(extractSuccesses(results))
      } else {
        failure[R](results: java.lang.Iterable[? <: Result[?]])
      }
    } catch {
      case ex: Exception =>
        failure[R](ex)
    }
  }

  /**
   * Returns a collector that can be used to create a collected `Result`
   * from a stream of `Result` instances, using the provided collector.
   *
   * @tparam T the type of the success value in the `Result`
   * @tparam R the type of the collection returned by the collector
   * @param collector the collector to combine the results
   * @return a `Collector`
   */
  def toCombinedResult[T, R](collector: Collector[T, ?, R]): Collector[Result[? <: T], ?, Result[R]] = {
    toCombinedResultTyped(collector.asInstanceOf[Collector[T, Object, R]])
  }

  // captures the intermediate accumulation type of the collector
  private def toCombinedResultTyped[T, A, R](
      collector: Collector[T, A, R]): Collector[Result[? <: T], StreamBuilder[T, A, R], Result[R]] = {

    val supplier: Supplier[StreamBuilder[T, A, R]] = () => new Result.StreamBuilder[T, A, R](collector)
    val accumulator: BiConsumer[StreamBuilder[T, A, R], Result[? <: T]] = (builder, item) => builder.add(item)
    val combiner: BinaryOperator[StreamBuilder[T, A, R]] = (builder1, builder2) => builder1.combine(builder2)
    val finisher: Function[StreamBuilder[T, A, R], Result[R]] = builder => builder.build()
    Collector.of(supplier, accumulator, combiner, finisher)
  }

  private def extractSuccesses[T](results: java.lang.Iterable[? <: Result[T]]): Stream[T] = {
    Guavate.stream(results.asInstanceOf[java.lang.Iterable[Result[T]]]).map[T](r => r.getValue())
  }

  // mutable combined instance builder for use in stream collection
  // using a single dedicated collector is more efficient than a reduction with multiple calls to combinedWith()
  private final class StreamBuilder[T, A, R](collector: Collector[T, A, R]) {

    private val collection: A = collector.supplier().get()
    private val failures: ImmutableList.Builder[Failure] = ImmutableList.builder[Failure]()

    def add(item: Result[? <: T]): Unit = {
      val typedItem = item.asInstanceOf[Result[T]]
      typedItem.ifSuccess(value => collector.accumulator().accept(collection, value))
      typedItem.ifFailure(failure => {
        failures.add(failure)
        ()
      })
    }

    def combine(builder: Result.StreamBuilder[T, A, R]): StreamBuilder[T, A, R] = {
      collector.combiner().apply(collection, builder.collection)
      failures.addAll(builder.failures.build())
      this
    }

    def build(): Result[R] = {
      val failureItems: List[FailureItem] = failures.build().stream()
          .flatMap[FailureItem](failure => failure.getItems().stream())
          .collect(Guavate.toImmutableList[FailureItem]())
      if (failureItems.isEmpty()) {
        success(collector.finisher().apply(collection))
      } else {
        failure[R](Failure.of(failureItems))
      }
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `Result`.
   * @return the meta-bean, not null
   */
  def meta(): Result.Meta[?] = {
    Result.Meta.INSTANCE
  }

  /**
   * The meta-bean for `Result`.
   * @tparam R  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  def metaResult[R](cls: Class[R]): Result.Meta[R] = {
    Result.Meta.INSTANCE.asInstanceOf[Result.Meta[R]]
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `Result`.
   * @tparam T  the type
   */
  final class Meta[T] private () extends DirectMetaBean {

    /**
     * The meta-property for the `value` property.
     */
    private val valueProperty: MetaProperty[T] = DirectMetaProperty.ofImmutable(
        this, "value", classOf[Result[?]], classOf[Object]).asInstanceOf[MetaProperty[T]]
    /**
     * The meta-property for the `failure` property.
     */
    private val failureProperty: MetaProperty[Failure] = DirectMetaProperty.ofImmutable(
        this, "failure", classOf[Result[?]], classOf[Failure])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "value",
        "failure")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 111972721 => // value
          valueProperty
        case -1086574198 => // failure
          failureProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: Result[T]] = {
      new Result.Builder[T]()
    }

    override def beanType(): Class[? <: Result[T]] = {
      classOf[Result[?]].asInstanceOf[Class[? <: Result[T]]]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `value` property.
     * @return the meta-property, not null
     */
    def value(): MetaProperty[T] = {
      valueProperty
    }

    /**
     * The meta-property for the `failure` property.
     * @return the meta-property, not null
     */
    def failure(): MetaProperty[Failure] = {
      failureProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 111972721 => // value
          // the value field is the success value, which is null unless the result is a success
          bean.asInstanceOf[Result[Object]].get().orElse(null)
        case -1086574198 => // failure
          // the failure field is null whenever the result is a success
          val result = bean.asInstanceOf[Result[?]]
          if (result.isSuccess()) null else result.getFailure()
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
    private[result] val INSTANCE: Meta[?] = new Meta[Object]()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `Result`.
   * @tparam T  the type
   */
  private final class Builder[T] () extends DirectPrivateBeanBuilder[Result[T]] {

    private var value: T = uninitialized
    private var failure: Failure = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 111972721 => // value
          value.asInstanceOf[Object]
        case -1086574198 => // failure
          failure
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder[T] = {
      propertyName.hashCode() match {
        case 111972721 => // value
          this.value = newValue.asInstanceOf[T]
        case -1086574198 => // failure
          this.failure = newValue.asInstanceOf[Failure]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): Result[T] = {
      val result = new Result[T](
          value,
          failure)
      result.validate()
      result
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("Result.Builder{")
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ')
      buf.append("failure").append('=').append(JodaBeanUtils.toString(failure))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
