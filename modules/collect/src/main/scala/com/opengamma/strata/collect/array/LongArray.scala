/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.function.IntToLongFunction
import java.util.function.LongBinaryOperator
import java.util.function.LongUnaryOperator
import java.util.stream.LongStream

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
import com.google.common.primitives.Longs
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.function.IntLongConsumer
import com.opengamma.strata.collect.function.IntLongToLongFunction
import com.opengamma.strata.collect.function.LongTernaryOperator

/**
 * An immutable array of `long` values.
 *
 * This provides functionality similar to `List` but for `long[]`.
 *
 * In mathematical terms, this is a vector, or one-dimensional matrix.
 */
@SerialVersionUID(1L)
final class LongArray private (
    /**
     * The underlying array of longs.
     */
    private val array: Array[Long])
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
  def get(index: Int): Long = {
    array(index)
  }

  /**
   * Checks if this array contains the specified value.
   *
   * @param value  the value to find
   * @return true if the value is contained in this array
   */
  def contains(value: Long): Boolean = {
    Longs.contains(array, value)
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def indexOf(value: Long): Int = {
    Longs.indexOf(array, value)
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def lastIndexOf(value: Long): Int = {
    Longs.lastIndexOf(array, value)
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
  def copyInto(destination: Array[Long], offset: Int): Unit = {
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
  def subArray(fromIndexInclusive: Int): LongArray = {
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
  def subArray(fromIndexInclusive: Int, toIndexExclusive: Int): LongArray = {
    LongArray.copyOf(array, fromIndexInclusive, toIndexExclusive)
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent `long[]`.
   *
   * @return a copy of the underlying array
   */
  def toArray(): Array[Long] = {
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
  def toArrayUnsafe(): Array[Long] = {
    array
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   *
   * @return a list wrapping this array
   */
  def toList(): List[java.lang.Long] = {
    new LongArray.ImmList(this)
  }

  /**
   * Returns a stream over the array values.
   *
   * @return a stream over the values in the array
   */
  def stream(): LongStream = {
    LongStream.of(array*)
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
  def forEach(action: IntLongConsumer): Unit = {
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
  def `with`(index: Int, newValue: Long): LongArray = {
    if (array(index) == newValue) {
      return this
    }
    val result = array.clone()
    result(index) = newValue
    new LongArray(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified amount added to each value.
   *
   * This is used to add to the contents of this array, returning a new array.
   *
   * This is a special case of `map(LongUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to add, may be negative
   * @return a copy of this array with the amount added to each value
   */
  def plus(amount: Long): LongArray = {
    if (amount == 0) {
      return this
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + amount
      i += 1
    }
    new LongArray(result)
  }

  /**
   * Returns an instance with the specified amount subtracted from each value.
   *
   * This is used to subtract from the contents of this array, returning a new array.
   *
   * This is a special case of `map(LongUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to subtract, may be negative
   * @return a copy of this array with the amount subtracted from each value
   */
  def minus(amount: Long): LongArray = {
    if (amount == 0) {
      return this
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - amount
      i += 1
    }
    new LongArray(result)
  }

  /**
   * Returns an instance with each value multiplied by the specified factor.
   *
   * This is used to multiply the contents of this array, returning a new array.
   *
   * This is a special case of `map(LongUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param factor  the multiplicative factor
   * @return a copy of this array with the each value multiplied by the factor
   */
  def multipliedBy(factor: Long): LongArray = {
    if (factor == 1) {
      return this
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * factor
      i += 1
    }
    new LongArray(result)
  }

  /**
   * Returns an instance with each value divided by the specified divisor.
   *
   * This is used to divide the contents of this array, returning a new array.
   *
   * This is a special case of `map(LongUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param divisor  the value by which the array values are divided
   * @return a copy of this array with the each value divided by the divisor
   */
  def dividedBy(divisor: Long): LongArray = {
    if (divisor == 1) {
      return this
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) / divisor
      i += 1
    }
    new LongArray(result)
  }

  /**
   * Returns an instance with an operation applied to each value in the array.
   *
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The operator only receives the value.
   * For example, the operator could multiply and add each element.
   * {{{
   *   result = base.map(value -> value * 3 + 4);
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param operator  the operator to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  def map(operator: LongUnaryOperator): LongArray = {
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsLong(array(i))
      i += 1
    }
    new LongArray(result)
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
  def mapWithIndex(function: IntLongToLongFunction): LongArray = {
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = function.applyAsLong(i, array(i))
      i += 1
    }
    new LongArray(result)
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
   * This is a special case of `combine(LongArray, LongBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements added
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def plus(other: LongArray): LongArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + other.array(i)
      i += 1
    }
    new LongArray(result)
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
   * This is a special case of `combine(LongArray, LongBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements subtracted
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def minus(other: LongArray): LongArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - other.array(i)
      i += 1
    }
    new LongArray(result)
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
   * This is a special case of `combine(LongArray, LongBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements multiplied
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def multipliedBy(other: LongArray): LongArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * other.array(i)
      i += 1
    }
    new LongArray(result)
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
   * This is a special case of `combine(LongArray, LongBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements divided
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def dividedBy(other: LongArray): LongArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) / other.array(i)
      i += 1
    }
    new LongArray(result)
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
  def combine(other: LongArray, operator: LongBinaryOperator): LongArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Long](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsLong(array(i), other.array(i))
      i += 1
    }
    new LongArray(result)
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
  def combineReduce(other: LongArray, operator: LongTernaryOperator): Long = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    var result: Long = 0
    var i = 0
    while (i < array.length) {
      result = operator.applyAsLong(result, array(i), other.array(i))
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
  def concat(arrayToConcat: Long*): LongArray = {
    // the Java varargs bridge wraps the caller's array without copying it
    val toConcat: Array[Long] = arrayToConcat match {
      case wrapped: ArraySeq.ofLong => wrapped.unsafeArray
      case _ => arrayToConcat.toArray
    }
    if (array.length == 0) {
      return LongArray.copyOf(toConcat)
    }
    if (toConcat.length == 0) {
      return this
    }
    val result = new Array[Long](array.length + toConcat.length)
    System.arraycopy(array, 0, result, 0, array.length)
    System.arraycopy(toConcat, 0, result, array.length, toConcat.length)
    new LongArray(result)
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
  def concat(arrayToConcat: LongArray): LongArray = {
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
   * This uses `Arrays#sort(long[])`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return a sorted copy of this array
   */
  def sorted(): LongArray = {
    if (array.length < 2) {
      return this
    }
    val result = array.clone()
    Arrays.sort(result)
    new LongArray(result)
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
  def min(): Long = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find minimum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var min: Long = java.lang.Long.MAX_VALUE
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
  def max(): Long = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find maximum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var max: Long = java.lang.Long.MIN_VALUE
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
   * This is a special case of `reduce(long, LongBinaryOperator)`.
   *
   * @return the total of all the values
   */
  def sum(): Long = {
    var total: Long = 0
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
  def reduce(identity: Long, operator: LongBinaryOperator): Long = {
    var result = identity
    var i = 0
    while (i < array.length) {
      result = operator.applyAsLong(result, array(i))
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    LongArray.Meta.META
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: LongArray =>
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

object LongArray {

  /**
   * An empty long array.
   */
  @static private val EMPTY_LONG_ARRAY: Array[Long] = new Array[Long](0)
  /**
   * An empty array.
   */
  @static final val EMPTY: LongArray = new LongArray(EMPTY_LONG_ARRAY)

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
  def of(): LongArray = {
    EMPTY
  }

  /**
   * Obtains an immutable array with a single value.
   *
   * @param value  the single value
   * @return an array containing the specified value
   */
  def of(value: Long): LongArray = {
    new LongArray(Array[Long](value))
  }

  /**
   * Obtains an immutable array with two values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @return an array containing the specified values
   */
  def of(value1: Long, value2: Long): LongArray = {
    new LongArray(Array[Long](value1, value2))
  }

  /**
   * Obtains an immutable array with three values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @return an array containing the specified values
   */
  def of(value1: Long, value2: Long, value3: Long): LongArray = {
    new LongArray(Array[Long](value1, value2, value3))
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
  def of(value1: Long, value2: Long, value3: Long, value4: Long): LongArray = {
    new LongArray(Array[Long](value1, value2, value3, value4))
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
  def of(value1: Long, value2: Long, value3: Long, value4: Long, value5: Long): LongArray = {
    new LongArray(Array[Long](value1, value2, value3, value4, value5))
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
  def of(value1: Long, value2: Long, value3: Long, value4: Long, value5: Long, value6: Long): LongArray = {
    new LongArray(Array[Long](value1, value2, value3, value4, value5, value6))
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
      value1: Long,
      value2: Long,
      value3: Long,
      value4: Long,
      value5: Long,
      value6: Long,
      value7: Long): LongArray = {

    new LongArray(Array[Long](value1, value2, value3, value4, value5, value6, value7))
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
      value1: Long,
      value2: Long,
      value3: Long,
      value4: Long,
      value5: Long,
      value6: Long,
      value7: Long,
      value8: Long): LongArray = {

    new LongArray(Array[Long](value1, value2, value3, value4, value5, value6, value7, value8))
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
      value1: Long,
      value2: Long,
      value3: Long,
      value4: Long,
      value5: Long,
      value6: Long,
      value7: Long,
      value8: Long,
      otherValues: Long*): LongArray = {

    // the Java varargs bridge wraps the caller's array without copying it
    val others: Array[Long] = otherValues match {
      case wrapped: ArraySeq.ofLong => wrapped.unsafeArray
      case _ => otherValues.toArray
    }
    val base = new Array[Long](others.length + 8)
    base(0) = value1
    base(1) = value2
    base(2) = value3
    base(3) = value4
    base(4) = value5
    base(5) = value6
    base(6) = value7
    base(7) = value8
    System.arraycopy(others, 0, base, 8, others.length)
    new LongArray(base)
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
  def of(size: Int, valueFunction: IntToLongFunction): LongArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Long](size)
    Arrays.setAll(array, valueFunction)
    new LongArray(array)
  }

  /**
   * Obtains an instance with entries filled from a stream.
   *
   * The stream is converted to an array using `LongStream#toArray()`.
   *
   * @param stream  the stream of elements
   * @return an array initialized using the stream
   */
  def of(stream: LongStream): LongArray = {
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
  def ofUnsafe(array: Array[Long]): LongArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new LongArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance from a collection of `Long`.
   *
   * The order of the values in the returned array is the order in which elements are returned
   * from the iterator of the collection.
   *
   * @param collection  the collection to initialize from
   * @return an array containing the values from the collection in iteration order
   */
  def copyOf(collection: Collection[java.lang.Long]): LongArray = {
    if (collection.size() == 0) {
      return EMPTY
    }
    collection match {
      case list: ImmList =>
        list.underlying
      case _ =>
        new LongArray(Longs.toArray(collection))
    }
  }

  /**
   * Obtains an instance from an array of `long`.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy, cloned
   * @return an array containing the specified values
   */
  def copyOf(array: Array[Long]): LongArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new LongArray(array.clone())
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
  def copyOf(array: Array[Long], fromIndex: Int): LongArray = {
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
  def copyOf(array: Array[Long], fromIndexInclusive: Int, toIndexExclusive: Int): LongArray = {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length)
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length)
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return EMPTY
    }
    new LongArray(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   *
   * @param size  the number of elements
   * @return an array filled with zeroes
   */
  def filled(size: Int): LongArray = {
    if (size == 0) {
      return EMPTY
    }
    new LongArray(new Array[Long](size))
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   *
   * @param size  the number of elements
   * @param value  the value of all the elements
   * @return an array filled with the specified value
   */
  def filled(size: Int, value: Long): LongArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Long](size)
    Arrays.fill(array, value)
    new LongArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `Iterator` representation of the array.
   */
  private[array] class ImmIterator(array: Array[Long]) extends ListIterator[java.lang.Long] {

    private var index: Int = 0

    override def hasNext(): Boolean = {
      index < array.length
    }

    override def hasPrevious(): Boolean = {
      index > 0
    }

    override def next(): java.lang.Long = {
      if (hasNext()) {
        val value = array(index)
        index += 1
        return java.lang.Long.valueOf(value)
      }
      throw new NoSuchElementException("Iteration has reached the last element")
    }

    override def previous(): java.lang.Long = {
      if (hasPrevious()) {
        index -= 1
        return java.lang.Long.valueOf(array(index))
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
      throw new UnsupportedOperationException("Unable to remove from LongArray")
    }

    override def set(value: java.lang.Long): Unit = {
      throw new UnsupportedOperationException("Unable to set value in LongArray")
    }

    override def add(value: java.lang.Long): Unit = {
      throw new UnsupportedOperationException("Unable to add value to LongArray")
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `List` representation of the array.
   */
  @SerialVersionUID(1L)
  private[array] class ImmList(private[array] val underlying: LongArray)
    extends AbstractList[java.lang.Long] with RandomAccess with Serializable {

    override def size(): Int = {
      underlying.size()
    }

    override def get(index: Int): java.lang.Long = {
      java.lang.Long.valueOf(underlying.get(index))
    }

    override def contains(obj: Object): Boolean = {
      obj match {
        case value: java.lang.Long => underlying.contains(value.longValue())
        case _ => false
      }
    }

    override def indexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Long => underlying.indexOf(value.longValue())
        case _ => -1
      }
    }

    override def lastIndexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Long => underlying.lastIndexOf(value.longValue())
        case _ => -1
      }
    }

    override def iterator(): ListIterator[java.lang.Long] = {
      listIterator()
    }

    override def listIterator(): ListIterator[java.lang.Long] = {
      new ImmIterator(underlying.array)
    }

    override protected def removeRange(fromIndex: Int, toIndex: Int): Unit = {
      throw new UnsupportedOperationException("Unable to remove range from LongArray")
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

    override def builder(): BeanBuilder[LongArray] = {
      new BasicImmutableBeanBuilder[LongArray](this) {
        private var array: Array[Long] = EMPTY_LONG_ARRAY

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            array.clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[LongArray] = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            this.array = ArgChecker.notNull(value, "value").asInstanceOf[Array[Long]].clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): LongArray = {
          new LongArray(array)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[LongArray]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[array] object Meta {

    private[array] val META: MetaBean = new Meta()

    private val ARRAY: MetaProperty[Array[Long]] = new BasicMetaProperty[Array[Long]]("array") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[LongArray]
      }

      override def propertyType(): Class[Array[Long]] = {
        classOf[Array[Long]]
      }

      override def propertyGenericType(): Type = {
        classOf[Array[Long]]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): Array[Long] = {
        bean.asInstanceOf[LongArray].toArray()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("array", ARRAY)
  }

}
