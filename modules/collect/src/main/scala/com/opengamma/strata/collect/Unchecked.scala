/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.io.IOException
import java.io.UncheckedIOException
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.function.UnaryOperator

import com.google.common.base.Throwables
import com.opengamma.strata.collect.function.CheckedBiConsumer
import com.opengamma.strata.collect.function.CheckedBiFunction
import com.opengamma.strata.collect.function.CheckedBiPredicate
import com.opengamma.strata.collect.function.CheckedBinaryOperator
import com.opengamma.strata.collect.function.CheckedConsumer
import com.opengamma.strata.collect.function.CheckedFunction
import com.opengamma.strata.collect.function.CheckedPredicate
import com.opengamma.strata.collect.function.CheckedRunnable
import com.opengamma.strata.collect.function.CheckedSupplier
import com.opengamma.strata.collect.function.CheckedUnaryOperator

/**
 * Static utility methods that convert checked exceptions to unchecked.
 *
 * Two `wrap()` methods are provided that can wrap an arbitrary piece of logic
 * and convert checked exceptions to unchecked.
 *
 * A number of other methods are provided that allow a lambda block to be decorated
 * to avoid handling checked exceptions.
 * For example, the method `File.getCanonicalFile()` throws an `IOException`
 * which can be handled as follows:
 * {{{
 *  stream.map(Unchecked.function(file -> file.getCanonicalFile())
 * }}}
 *
 * Each method accepts a functional interface that is defined to throw `Throwable`.
 * Catching `Throwable` means that any method can be wrapped.
 * Any `InvocationTargetException` is extracted and processed recursively.
 * Any `IOException` is converted to an `UncheckedIOException`.
 * Any `ReflectiveOperationException` is converted to an `UncheckedReflectiveOperationException`.
 * Any `Error` or `RuntimeException` is re-thrown without alteration.
 * Any other exception is wrapped in a `RuntimeException`.
 */
final class Unchecked private ()

object Unchecked {

  //-------------------------------------------------------------------------
  /**
   * Wraps a block of code, converting checked exceptions to unchecked.
   * {{{
   *   Unchecked.wrap(() -> {
   *     // any code that throws a checked exception
   * }}}
   *
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @param block  the code block to wrap
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.RuntimeException if an exception occurs
   */
  def wrap(block: CheckedRunnable): Unit = {
    try {
      block.run()
    } catch {
      case ex: Throwable =>
        throw propagate(ex)
    }
  }

  /**
   * Wraps a block of code, converting checked exceptions to unchecked.
   * {{{
   *   Unchecked.wrap(() -> {
   *     // any code that throws a checked exception
   * }}}
   *
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the type of the result
   * @param block  the code block to wrap
   * @return the result of invoking the block
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.RuntimeException if an exception occurs
   */
  def wrap[T](block: CheckedSupplier[T]): T = {
    try {
      block.get()
    } catch {
      case ex: Throwable =>
        throw propagate(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `Runnable` interface.
   *
   * This wraps the specified runnable returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @param runnable  the runnable to be decorated
   * @return the runnable instance that handles checked exceptions
   */
  def runnable(runnable: CheckedRunnable): Runnable = {
    () => {
      try {
        runnable.run()
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `Function` interface.
   *
   * This wraps the specified function returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the input type of the function
   * @tparam R the return type of the function
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  def function[T, R](function: CheckedFunction[T, R]): Function[T, R] = {
    (t: T) => {
      try {
        function.apply(t)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  /**
   * Converts checked exceptions to unchecked based on the `BiFunction` interface.
   *
   * This wraps the specified function returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the first input type of the function
   * @tparam U the second input type of the function
   * @tparam R the return type of the function
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  def biFunction[T, U, R](function: CheckedBiFunction[T, U, R]): BiFunction[T, U, R] = {
    (t: T, u: U) => {
      try {
        function.apply(t, u)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `UnaryOperator` interface.
   *
   * This wraps the specified operator returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the type of the operator
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  def unaryOperator[T](function: CheckedUnaryOperator[T]): UnaryOperator[T] = {
    (t: T) => {
      try {
        function.apply(t)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  /**
   * Converts checked exceptions to unchecked based on the `BinaryOperator` interface.
   *
   * This wraps the specified operator returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the type of the operator
   * @param function  the function to be decorated
   * @return the function instance that handles checked exceptions
   */
  def binaryOperator[T](function: CheckedBinaryOperator[T]): BinaryOperator[T] = {
    (t: T, u: T) => {
      try {
        function.apply(t, u)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `Predicate` interface.
   *
   * This wraps the specified predicate returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the type of the predicate
   * @param predicate  the predicate to be decorated
   * @return the predicate instance that handles checked exceptions
   */
  def predicate[T](predicate: CheckedPredicate[T]): Predicate[T] = {
    (t: T) => {
      try {
        predicate.test(t)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  /**
   * Converts checked exceptions to unchecked based on the `BiPredicate` interface.
   *
   * This wraps the specified predicate returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the first type of the predicate
   * @tparam U the second type of the predicate
   * @param predicate  the predicate to be decorated
   * @return the predicate instance that handles checked exceptions
   */
  def biPredicate[T, U](predicate: CheckedBiPredicate[T, U]): BiPredicate[T, U] = {
    (t: T, u: U) => {
      try {
        predicate.test(t, u)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `Consumer` interface.
   *
   * This wraps the specified consumer returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the type of the consumer
   * @param consumer  the consumer to be decorated
   * @return the consumer instance that handles checked exceptions
   */
  def consumer[T](consumer: CheckedConsumer[T]): Consumer[T] = {
    (t: T) => {
      try {
        consumer.accept(t)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  /**
   * Converts checked exceptions to unchecked based on the `BiConsumer` interface.
   *
   * This wraps the specified consumer returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam T the first type of the consumer
   * @tparam U the second type of the consumer
   * @param consumer  the consumer to be decorated
   * @return the consumer instance that handles checked exceptions
   */
  def biConsumer[T, U](consumer: CheckedBiConsumer[T, U]): BiConsumer[T, U] = {
    (t: T, u: U) => {
      try {
        consumer.accept(t, u)
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts checked exceptions to unchecked based on the `Supplier` interface.
   *
   * This wraps the specified supplier returning an instance that handles checked exceptions.
   * If a checked exception is thrown it is converted to an `UncheckedIOException`
   * or `RuntimeException` as appropriate.
   *
   * @tparam R the result type of the supplier
   * @param supplier  the supplier to be decorated
   * @return the supplier instance that handles checked exceptions
   */
  def supplier[R](supplier: CheckedSupplier[R]): Supplier[R] = {
    () => {
      try {
        supplier.get()
      } catch {
        case ex: Throwable =>
          throw propagate(ex)
      }
    }
  }

  /**
   * Propagates `throwable` as-is if possible, or by wrapping in a `RuntimeException` if not.
   *
   *   - If `throwable` is an `InvocationTargetException` the cause is extracted and processed recursively.
   *   - If `throwable` is an `CompletionException` the cause is extracted and processed recursively.
   *   - If `throwable` is an `ExecutionException` the cause is extracted and processed recursively.
   *   - If `throwable` is an `Error` or `RuntimeException` it is propagated as-is.
   *   - If `throwable` is an `IOException` it is wrapped in `UncheckedIOException` and thrown.
   *   - If `throwable` is an `ReflectiveOperationException` it is wrapped in
   *     `UncheckedReflectiveOperationException` and thrown.
   *   - Otherwise `throwable` is wrapped in a `RuntimeException` and thrown.
   * This method always throws an exception. The return type is a convenience to satisfy the type system
   * when the enclosing method returns a value. For example:
   * {{{
   *   T foo() {
   *     try {
   *       return methodWithCheckedException();
   *     } catch (Exception e) {
   *       throw Unchecked.propagate(e);
   *     }
   * }}}
   *
   * @param throwable the `Throwable` to propagate
   * @return never returns as an exception is always thrown
   */
  def propagate(throwable: Throwable): RuntimeException = {
    throwable match {
      case ex: InvocationTargetException =>
        throw propagate(ex.getCause())
      case ex: CompletionException =>
        throw propagate(ex.getCause())
      case ex: ExecutionException =>
        throw propagate(ex.getCause())
      case ex: IOException =>
        throw new UncheckedIOException(ex)
      case ex: ReflectiveOperationException =>
        throw new UncheckedReflectiveOperationException(ex)
      case _ =>
        Throwables.throwIfUnchecked(throwable)
        throw new RuntimeException(throwable)
    }
  }

}
