/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array

import java.io.Serializable
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.AbstractList
import java.util.Arrays
import java.util.Collection
import java.util.List
import java.util.ListIterator
import java.util.Map
import java.util.NoSuchElementException
import java.util.RandomAccess
import java.util.function.DoubleBinaryOperator
import java.util.function.DoubleUnaryOperator
import java.util.function.IntToDoubleFunction
import java.util.stream.DoubleStream

import scala.annotation.static
import scala.annotation.varargs
import scala.collection.immutable.ArraySeq

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.PropertyStyle
import org.joda.beans.impl.BasicImmutableBeanBuilder
import org.joda.beans.impl.BasicMetaBean
import org.joda.beans.impl.BasicMetaProperty

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.primitives.Doubles
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.DoubleArrayMath
import com.opengamma.strata.collect.function.DoubleTernaryOperator
import com.opengamma.strata.collect.function.IntDoubleConsumer
import com.opengamma.strata.collect.function.IntDoubleToDoubleFunction

/**
 * An immutable array of `double` values.
 *
 * This provides functionality similar to `List` but for `double[]`.
 *
 * In mathematical terms, this is a vector, or one-dimensional matrix.
 */
@SerialVersionUID(1L)
final class DoubleArray private (
    /**
     * The underlying array of doubles.
     */
    private val array: Array[Double])
  extends Matrix with ImmutableBean with Serializable {

  //-------------------------------------------------------------------------
  /**
   * Gets the number of dimensions of this array.
   *
   * @return one
   */
  override def dimensions(): Int = {
    1
  }

  /**
   * Gets the size of this array.
   *
   * @return the array size, zero or greater
   */
  override def size(): Int = {
    array.length
  }

  /**
   * Checks if this array is empty.
   *
   * @return true if empty
   */
  def isEmpty(): Boolean = {
    array.length == 0
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value at the specified index in this array.
   *
   * @param index  the zero-based index to retrieve
   * @return the value at the index
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def get(index: Int): Double = {
    array(index)
  }

  /**
   * Checks if this array contains the specified value.
   *
   * The value is checked using `Double.doubleToLongBits` in order to match `equals`.
   * This also allow this method to be used to find any occurrences of NaN.
   *
   * @param value  the value to find
   * @return true if the value is contained in this array
   */
  def contains(value: Double): Boolean = {
    if (array.length > 0) {
      val bits = java.lang.Double.doubleToLongBits(value)
      var i = 0
      while (i < array.length) {
        if (java.lang.Double.doubleToLongBits(array(i)) == bits) {
          return true
        }
        i += 1
      }
    }
    false
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * The value is checked using `Double.doubleToLongBits` in order to match `equals`.
   * This also allow this method to be used to find any occurrences of NaN.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def indexOf(value: Double): Int = {
    if (array.length > 0) {
      val bits = java.lang.Double.doubleToLongBits(value)
      var i = 0
      while (i < array.length) {
        if (java.lang.Double.doubleToLongBits(array(i)) == bits) {
          return i
        }
        i += 1
      }
    }
    -1
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * The value is checked using `Double.doubleToLongBits` in order to match `equals`.
   * This also allow this method to be used to find any occurrences of NaN.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def lastIndexOf(value: Double): Int = {
    if (array.length > 0) {
      val bits = java.lang.Double.doubleToLongBits(value)
      var i = array.length - 1
      while (i >= 0) {
        if (java.lang.Double.doubleToLongBits(array(i)) == bits) {
          return i
        }
        i -= 1
      }
    }
    -1
  }

  //-------------------------------------------------------------------------
  /**
   * Copies this array into the specified array.
   *
   * The specified array must be at least as large as this array.
   * If it is larger, then the remainder of the array will be untouched.
   *
   * @param destination  the array to copy into
   * @param offset  the offset in the destination array to start from
   * @throws java.lang.IndexOutOfBoundsException if the destination array is not large enough
   *   or the offset is negative
   */
  def copyInto(destination: Array[Double], offset: Int): Unit = {
    if (destination.length < array.length + offset) {
      throw new IndexOutOfBoundsException("Destination array is not large enough")
    }
    System.arraycopy(array, 0, destination, offset, array.length)
  }

  /**
   * Returns an array holding the values from the specified index onwards.
   *
   * @param fromIndexInclusive  the start index of the array to copy from
   * @return an array instance with the specified bounds
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def subArray(fromIndexInclusive: Int): DoubleArray = {
    subArray(fromIndexInclusive, array.length)
  }

  /**
   * Returns an array holding the values between the specified from and to indices.
   *
   * @param fromIndexInclusive  the start index of the array to copy from
   * @param toIndexExclusive  the end index of the array to copy to
   * @return an array instance with the specified bounds
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def subArray(fromIndexInclusive: Int, toIndexExclusive: Int): DoubleArray = {
    DoubleArray.copyOf(array, fromIndexInclusive, toIndexExclusive)
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent `double[]`.
   *
   * @return a copy of the underlying array
   */
  def toArray(): Array[Double] = {
    array.clone()
  }

  /**
   * Returns the underlying array.
   *
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the array returned by this method.
   * Doing so would violate the immutability of this class.
   *
   * @return the raw array
   */
  def toArrayUnsafe(): Array[Double] = {
    array
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   *
   * @return a list wrapping this array
   */
  def toList(): List[java.lang.Double] = {
    new DoubleArray.ImmList(this)
  }

  /**
   * Returns a stream over the array values.
   *
   * @return a stream over the values in the array
   */
  def stream(): DoubleStream = {
    DoubleStream.of(array*)
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each value in the array.
   *
   * This is used to perform an action on the contents of this array.
   * The action receives both the index and the value.
   * For example, the action could print out the array.
   * {{{
   *   base.forEach((index, value) -> System.out.println(index + ": " + value));
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param action  the action to be applied
   */
  def forEach(action: IntDoubleConsumer): Unit = {
    var i = 0
    while (i < array.length) {
      action.accept(i, array(i))
      i += 1
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an instance with the value at the specified index changed.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param index  the zero-based index to set
   * @param newValue  the new value to store
   * @return a copy of this array with the value at the index changed
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def `with`(index: Int, newValue: Double): DoubleArray = {
    if (java.lang.Double.doubleToLongBits(array(index)) == java.lang.Double.doubleToLongBits(newValue)) {
      return this
    }
    val result = array.clone()
    result(index) = newValue
    new DoubleArray(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified amount added to each value.
   *
   * This is used to add to the contents of this array, returning a new array.
   *
   * This is a special case of `map(DoubleUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to add, may be negative
   * @return a copy of this array with the amount added to each value
   */
  def plus(amount: Double): DoubleArray = {
    if (amount == 0d) {
      return this
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + amount
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance with the specified amount subtracted from each value.
   *
   * This is used to subtract from the contents of this array, returning a new array.
   *
   * This is a special case of `map(DoubleUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to subtract, may be negative
   * @return a copy of this array with the amount subtracted from each value
   */
  def minus(amount: Double): DoubleArray = {
    if (amount == 0d) {
      return this
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - amount
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance with each value multiplied by the specified factor.
   *
   * This is used to multiply the contents of this array, returning a new array.
   *
   * This is a special case of `map(DoubleUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param factor  the multiplicative factor
   * @return a copy of this array with the each value multiplied by the factor
   */
  def multipliedBy(factor: Double): DoubleArray = {
    if (factor == 1d) {
      return this
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * factor
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance with each value divided by the specified divisor.
   *
   * This is used to divide the contents of this array, returning a new array.
   *
   * This is a special case of `map(DoubleUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param divisor  the value by which the array values are divided
   * @return a copy of this array with the each value divided by the divisor
   */
  def dividedBy(divisor: Double): DoubleArray = {
    if (divisor == 1d) {
      return this
    }
    // multiplication is cheaper than division so it is more efficient to do the division once and multiply each element
    val factor: Double = 1 / divisor
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * factor
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance with an operation applied to each value in the array.
   *
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The operator only receives the value.
   * For example, the operator could take the inverse of each element.
   * {{{
   *   result = base.map(value -> 1 / value);
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param operator  the operator to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  def map(operator: DoubleUnaryOperator): DoubleArray = {
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsDouble(array(i))
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance with an operation applied to each indexed value in the array.
   *
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The function receives both the index and the value.
   * For example, the operator could multiply the value by the index.
   * {{{
   *   result = base.mapWithIndex((index, value) -> index * value);
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param function  the function to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  def mapWithIndex(function: IntDoubleToDoubleFunction): DoubleArray = {
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = function.applyAsDouble(i, array(i))
      i += 1
    }
    new DoubleArray(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance where each element is the sum of the matching values
   * in this array and the other array.
   *
   * This is used to add two arrays, returning a new array.
   * Element `n` in the resulting array is equal to element `n` in this array
   * plus element `n` in the other array.
   * The arrays must be of the same size.
   *
   * This is a special case of `combine(DoubleArray, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements added
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def plus(other: DoubleArray): DoubleArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + other.array(i)
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance where each element is equal to the difference between the
   * matching values in this array and the other array.
   *
   * This is used to subtract the second array from the first, returning a new array.
   * Element `n` in the resulting array is equal to element `n` in this array
   * minus element `n` in the other array.
   * The arrays must be of the same size.
   *
   * This is a special case of `combine(DoubleArray, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements subtracted
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def minus(other: DoubleArray): DoubleArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - other.array(i)
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance where each element is equal to the product of the
   * matching values in this array and the other array.
   *
   * This is used to multiply each value in this array by the corresponding value in the other array,
   * returning a new array.
   *
   * Element `n` in the resulting array is equal to element `n` in this array
   * multiplied by element `n` in the other array.
   * The arrays must be of the same size.
   *
   * This is a special case of `combine(DoubleArray, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements multiplied
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def multipliedBy(other: DoubleArray): DoubleArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * other.array(i)
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance where each element is calculated by dividing values in this array by values in the other array.
   *
   * This is used to divide each value in this array by the corresponding value in the other array,
   * returning a new array.
   *
   * Element `n` in the resulting array is equal to element `n` in this array
   * divided by element `n` in the other array.
   * The arrays must be of the same size.
   *
   * This is a special case of `combine(DoubleArray, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements divided
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def dividedBy(other: DoubleArray): DoubleArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) / other.array(i)
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Returns an instance where each element is formed by some combination of the matching
   * values in this array and the other array.
   *
   * This is used to combine two arrays, returning a new array.
   * Element `n` in the resulting array is equal to the result of the operator
   * when applied to element `n` in this array and element `n` in the other array.
   * The arrays must be of the same size.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @param operator  the operator used to combine each pair of values
   * @return a copy of this array combined with the specified array
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def combine(other: DoubleArray, operator: DoubleBinaryOperator): DoubleArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsDouble(array(i), other.array(i))
      i += 1
    }
    new DoubleArray(result)
  }

  /**
   * Combines this array and the other array returning a reduced value.
   *
   * This is used to combine two arrays, returning a single reduced value.
   * The operator is called once for each element in the arrays.
   * The arrays must be of the same size.
   *
   * The first argument to the operator is the running total of the reduction, starting from zero.
   * The second argument to the operator is the element from this array.
   * The third argument to the operator is the element from the other array.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @param operator  the operator used to combine each pair of values with the current total
   * @return the result of the reduction
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def combineReduce(other: DoubleArray, operator: DoubleTernaryOperator): Double = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    var result: Double = 0
    var i = 0
    while (i < array.length) {
      result = operator.applyAsDouble(result, array(i), other.array(i))
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an array that combines this array and the specified array.
   *
   * The result will have a length equal to `this.size() + arrayToConcat.length`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param arrayToConcat  the array to add to the end of this array
   * @return a copy of this array with the specified array added at the end
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  @varargs
  def concat(arrayToConcat: Double*): DoubleArray = {
    // the Java varargs bridge wraps the caller's array without copying it
    val toConcat: Array[Double] = arrayToConcat match {
      case wrapped: ArraySeq.ofDouble => wrapped.unsafeArray
      case _ => arrayToConcat.toArray
    }
    if (array.length == 0) {
      return DoubleArray.copyOf(toConcat)
    }
    if (toConcat.length == 0) {
      return this
    }
    val result = new Array[Double](array.length + toConcat.length)
    System.arraycopy(array, 0, result, 0, array.length)
    System.arraycopy(toConcat, 0, result, array.length, toConcat.length)
    new DoubleArray(result)
  }

  /**
   * Returns an array that combines this array and the specified array.
   *
   * The result will have a length equal to `this.size() + newArray.length`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param arrayToConcat  the new array to add to the end of this array
   * @return a copy of this array with the specified array added at the end
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def concat(arrayToConcat: DoubleArray): DoubleArray = {
    if (array.length == 0) {
      return arrayToConcat
    }
    if (arrayToConcat.array.length == 0) {
      return this
    }
    concat(arrayToConcat.array*)
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a sorted copy of this array.
   *
   * This uses `Arrays#sort(double[])`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return a sorted copy of this array
   */
  def sorted(): DoubleArray = {
    if (array.length < 2) {
      return this
    }
    val result = array.clone()
    Arrays.sort(result)
    new DoubleArray(result)
  }

  /**
   * Returns the minimum value held in the array.
   *
   * If the array is empty, then an exception is thrown.
   * If the array contains NaN, then the result is NaN.
   *
   * @return the minimum value
   * @throws java.lang.IllegalStateException if the array is empty
   */
  def min(): Double = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find minimum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var min = java.lang.Double.POSITIVE_INFINITY
    var i = 0
    while (i < array.length) {
      min = Math.min(min, array(i))
      i += 1
    }
    min
  }

  /**
   * Returns the minimum value held in the array.
   *
   * If the array is empty, then an exception is thrown.
   * If the array contains NaN, then the result is NaN.
   *
   * @return the maximum value
   * @throws java.lang.IllegalStateException if the array is empty
   */
  def max(): Double = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find maximum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var max = java.lang.Double.NEGATIVE_INFINITY
    var i = 0
    while (i < array.length) {
      max = Math.max(max, array(i))
      i += 1
    }
    max
  }

  /**
   * Returns the sum of all the values in the array.
   *
   * This is a special case of `reduce(double, DoubleBinaryOperator)`.
   *
   * @return the total of all the values
   */
  def sum(): Double = {
    var total: Double = 0
    var i = 0
    while (i < array.length) {
      total += array(i)
      i += 1
    }
    total
  }

  /**
   * Reduces this array returning a single value.
   *
   * This is used to reduce the values in this array to a single value.
   * The operator is called once for each element in the arrays.
   * The first argument to the operator is the running total of the reduction, starting from zero.
   * The second argument to the operator is the element.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param identity  the identity value to start from
   * @param operator  the operator used to combine the value with the current total
   * @return the result of the reduction
   */
  def reduce(identity: Double, operator: DoubleBinaryOperator): Double = {
    var result = identity
    var i = 0
    while (i < array.length) {
      result = operator.applyAsDouble(result, array(i))
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    DoubleArray.Meta.META
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this array equals another within the specified tolerance.
   *
   * This returns true if the two instances have `double` values that are
   * equal within the specified tolerance.
   *
   * @param other  the other array
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  def equalWithTolerance(other: DoubleArray, tolerance: Double): Boolean = {
    DoubleArrayMath.fuzzyEquals(array, other.array, tolerance)
  }

  /**
   * Checks if this array equals zero within the specified tolerance.
   *
   * This returns true if all the `double` values equal zero within the specified tolerance.
   *
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  def equalZeroWithTolerance(tolerance: Double): Boolean = {
    DoubleArrayMath.fuzzyEqualsZero(array, tolerance)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: DoubleArray =>
        Arrays.equals(array, other.array)
      case _ =>
        false
    }
  }

  override def hashCode(): Int = {
    Arrays.hashCode(array)
  }

  override def toString(): String = {
    Arrays.toString(array)
  }

}

object DoubleArray {

  /**
   * An empty array.
   */
  @static final val EMPTY: DoubleArray = new DoubleArray(new Array[Double](0))

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Meta.META)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty immutable array.
   *
   * @return the empty immutable array
   */
  def of(): DoubleArray = {
    EMPTY
  }

  /**
   * Obtains an immutable array with a single value.
   *
   * @param value  the single value
   * @return an array containing the specified value
   */
  def of(value: Double): DoubleArray = {
    new DoubleArray(Array[Double](value))
  }

  /**
   * Obtains an immutable array with two values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @return an array containing the specified values
   */
  def of(value1: Double, value2: Double): DoubleArray = {
    new DoubleArray(Array[Double](value1, value2))
  }

  /**
   * Obtains an immutable array with three values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @return an array containing the specified values
   */
  def of(value1: Double, value2: Double, value3: Double): DoubleArray = {
    new DoubleArray(Array[Double](value1, value2, value3))
  }

  /**
   * Obtains an immutable array with four values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @return an array containing the specified values
   */
  def of(value1: Double, value2: Double, value3: Double, value4: Double): DoubleArray = {
    new DoubleArray(Array[Double](value1, value2, value3, value4))
  }

  /**
   * Obtains an immutable array with five values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @return an array containing the specified values
   */
  def of(
      value1: Double,
      value2: Double,
      value3: Double,
      value4: Double,
      value5: Double): DoubleArray = {

    new DoubleArray(Array[Double](value1, value2, value3, value4, value5))
  }

  /**
   * Obtains an immutable array with six values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @return an array containing the specified values
   */
  def of(
      value1: Double,
      value2: Double,
      value3: Double,
      value4: Double,
      value5: Double,
      value6: Double): DoubleArray = {

    new DoubleArray(Array[Double](value1, value2, value3, value4, value5, value6))
  }

  /**
   * Obtains an immutable array with seven values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @return an array containing the specified values
   */
  def of(
      value1: Double,
      value2: Double,
      value3: Double,
      value4: Double,
      value5: Double,
      value6: Double,
      value7: Double): DoubleArray = {

    new DoubleArray(Array[Double](value1, value2, value3, value4, value5, value6, value7))
  }

  /**
   * Obtains an immutable array with eight values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @param value8  the eighth value
   * @return an array containing the specified values
   */
  def of(
      value1: Double,
      value2: Double,
      value3: Double,
      value4: Double,
      value5: Double,
      value6: Double,
      value7: Double,
      value8: Double): DoubleArray = {

    new DoubleArray(Array[Double](value1, value2, value3, value4, value5, value6, value7, value8))
  }

  /**
   * Obtains an immutable array with more than eight values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @param value8  the eighth value
   * @param otherValues  the other values
   * @return an array containing the specified values
   */
  @varargs
  def of(
      value1: Double,
      value2: Double,
      value3: Double,
      value4: Double,
      value5: Double,
      value6: Double,
      value7: Double,
      value8: Double,
      otherValues: Double*): DoubleArray = {

    // the Java varargs bridge wraps the caller's array without copying it
    val others: Array[Double] = otherValues match {
      case wrapped: ArraySeq.ofDouble => wrapped.unsafeArray
      case _ => otherValues.toArray
    }
    val base = new Array[Double](others.length + 8)
    base(0) = value1
    base(1) = value2
    base(2) = value3
    base(3) = value4
    base(4) = value5
    base(5) = value6
    base(6) = value7
    base(7) = value8
    System.arraycopy(others, 0, base, 8, others.length)
    new DoubleArray(base)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with entries filled using a function.
   *
   * The function is passed the array index and returns the value for that index.
   *
   * @param size  the number of elements
   * @param valueFunction  the function used to populate the value
   * @return an array initialized using the function
   */
  def of(size: Int, valueFunction: IntToDoubleFunction): DoubleArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Double](size)
    Arrays.setAll(array, valueFunction)
    new DoubleArray(array)
  }

  /**
   * Obtains an instance with entries filled from a stream.
   *
   * The stream is converted to an array using `DoubleStream#toArray()`.
   *
   * @param stream  the stream of elements
   * @return an array initialized using the stream
   */
  def of(stream: DoubleStream): DoubleArray = {
    ofUnsafe(stream.toArray())
  }

  /**
   * Obtains an instance by wrapping an array.
   *
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   *
   * @param array  the array to assign
   * @return an array instance wrapping the specified array
   */
  def ofUnsafe(array: Array[Double]): DoubleArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new DoubleArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance from a collection of `Double`.
   *
   * The order of the values in the returned array is the order in which elements are returned
   * from the iterator of the collection.
   *
   * @param collection  the collection to initialize from
   * @return an array containing the values from the collection in iteration order
   */
  def copyOf(collection: Collection[java.lang.Double]): DoubleArray = {
    if (collection.size() == 0) {
      return EMPTY
    }
    collection match {
      case list: ImmList =>
        list.underlying
      case _ =>
        new DoubleArray(Doubles.toArray(collection))
    }
  }

  /**
   * Obtains an instance from an array of `double`.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy, cloned
   * @return an array containing the specified values
   */
  def copyOf(array: Array[Double]): DoubleArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new DoubleArray(array.clone())
  }

  /**
   * Obtains an instance by copying part of an array.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy
   * @param fromIndex  the offset from the start of the array
   * @return an array containing the specified values
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def copyOf(array: Array[Double], fromIndex: Int): DoubleArray = {
    copyOf(array, fromIndex, array.length)
  }

  /**
   * Obtains an instance by copying part of an array.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy
   * @param fromIndexInclusive  the start index of the input array to copy from
   * @param toIndexExclusive  the end index of the input array to copy to
   * @return an array containing the specified values
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def copyOf(array: Array[Double], fromIndexInclusive: Int, toIndexExclusive: Int): DoubleArray = {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length)
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length)
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return EMPTY
    }
    new DoubleArray(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   *
   * @param size  the number of elements
   * @return an array filled with zeroes
   */
  def filled(size: Int): DoubleArray = {
    if (size == 0) {
      return EMPTY
    }
    new DoubleArray(new Array[Double](size))
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   *
   * @param size  the number of elements
   * @param value  the value of all the elements
   * @return an array filled with the specified value
   */
  def filled(size: Int, value: Double): DoubleArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Double](size)
    Arrays.fill(array, value)
    new DoubleArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `Iterator` representation of the array.
   */
  private[array] class ImmIterator(array: Array[Double]) extends ListIterator[java.lang.Double] {

    private var index: Int = 0

    override def hasNext(): Boolean = {
      index < array.length
    }

    override def hasPrevious(): Boolean = {
      index > 0
    }

    override def next(): java.lang.Double = {
      if (hasNext()) {
        val value = array(index)
        index += 1
        return java.lang.Double.valueOf(value)
      }
      throw new NoSuchElementException("Iteration has reached the last element")
    }

    override def previous(): java.lang.Double = {
      if (hasPrevious()) {
        index -= 1
        return java.lang.Double.valueOf(array(index))
      }
      throw new NoSuchElementException("Iteration has reached the first element")
    }

    override def nextIndex(): Int = {
      index
    }

    override def previousIndex(): Int = {
      index - 1
    }

    override def remove(): Unit = {
      throw new UnsupportedOperationException("Unable to remove from DoubleArray")
    }

    override def set(value: java.lang.Double): Unit = {
      throw new UnsupportedOperationException("Unable to set value in DoubleArray")
    }

    override def add(value: java.lang.Double): Unit = {
      throw new UnsupportedOperationException("Unable to add value to DoubleArray")
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `List` representation of the array.
   */
  @SerialVersionUID(1L)
  private[array] class ImmList(private[array] val underlying: DoubleArray)
    extends AbstractList[java.lang.Double] with RandomAccess with Serializable {

    override def size(): Int = {
      underlying.size()
    }

    override def get(index: Int): java.lang.Double = {
      java.lang.Double.valueOf(underlying.get(index))
    }

    override def contains(obj: Object): Boolean = {
      obj match {
        case value: java.lang.Double => underlying.contains(value.doubleValue())
        case _ => false
      }
    }

    override def indexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Double => underlying.indexOf(value.doubleValue())
        case _ => -1
      }
    }

    override def lastIndexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Double => underlying.lastIndexOf(value.doubleValue())
        case _ => -1
      }
    }

    override def iterator(): ListIterator[java.lang.Double] = {
      listIterator()
    }

    override def listIterator(): ListIterator[java.lang.Double] = {
      new ImmIterator(underlying.array)
    }

    override protected def removeRange(fromIndex: Int, toIndex: Int): Unit = {
      throw new UnsupportedOperationException("Unable to remove range from DoubleArray")
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  private[array] final class Meta private () extends BasicMetaBean {

    override def isBuildable(): Boolean = {
      true
    }

    override def builder(): BeanBuilder[DoubleArray] = {
      new BasicImmutableBeanBuilder[DoubleArray](this) {
        private var array: Array[Double] = DoubleArrayMath.EMPTY_DOUBLE_ARRAY

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            array.clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[DoubleArray] = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            this.array = ArgChecker.notNull(value, "value").asInstanceOf[Array[Double]].clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): DoubleArray = {
          new DoubleArray(array)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[DoubleArray]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[array] object Meta {

    private[array] val META: MetaBean = new Meta()

    private val ARRAY: MetaProperty[Array[Double]] = new BasicMetaProperty[Array[Double]]("array") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[DoubleArray]
      }

      override def propertyType(): Class[Array[Double]] = {
        classOf[Array[Double]]
      }

      override def propertyGenericType(): Type = {
        classOf[Array[Double]]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): Array[Double] = {
        bean.asInstanceOf[DoubleArray].toArray()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("array", ARRAY)
  }

}
