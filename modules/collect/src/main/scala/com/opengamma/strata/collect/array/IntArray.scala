/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.function.IntBinaryOperator
import java.util.function.IntUnaryOperator
import java.util.stream.IntStream

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
import com.google.common.primitives.Ints
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.function.IntIntConsumer
import com.opengamma.strata.collect.function.IntTernaryOperator

/**
 * An immutable array of `int` values.
 *
 * This provides functionality similar to `List` but for `int[]`.
 *
 * In mathematical terms, this is a vector, or one-dimensional matrix.
 */
@SerialVersionUID(1L)
final class IntArray private (
    /**
     * The underlying array of ints.
     */
    private val array: Array[Int])
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
  def get(index: Int): Int = {
    array(index)
  }

  /**
   * Checks if this array contains the specified value.
   *
   * @param value  the value to find
   * @return true if the value is contained in this array
   */
  def contains(value: Int): Boolean = {
    Ints.contains(array, value)
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def indexOf(value: Int): Int = {
    Ints.indexOf(array, value)
  }

  /**
   * Find the index of the first occurrence of the specified value.
   *
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  def lastIndexOf(value: Int): Int = {
    Ints.lastIndexOf(array, value)
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
  def copyInto(destination: Array[Int], offset: Int): Unit = {
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
  def subArray(fromIndexInclusive: Int): IntArray = {
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
  def subArray(fromIndexInclusive: Int, toIndexExclusive: Int): IntArray = {
    IntArray.copyOf(array, fromIndexInclusive, toIndexExclusive)
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent `int[]`.
   *
   * @return a copy of the underlying array
   */
  def toArray(): Array[Int] = {
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
  def toArrayUnsafe(): Array[Int] = {
    array
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   *
   * @return a list wrapping this array
   */
  def toList(): List[java.lang.Integer] = {
    new IntArray.ImmList(this)
  }

  /**
   * Returns a stream over the array values.
   *
   * @return a stream over the values in the array
   */
  def stream(): IntStream = {
    IntStream.of(array*)
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
  def forEach(action: IntIntConsumer): Unit = {
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
  def `with`(index: Int, newValue: Int): IntArray = {
    if (array(index) == newValue) {
      return this
    }
    val result = array.clone()
    result(index) = newValue
    new IntArray(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified amount added to each value.
   *
   * This is used to add to the contents of this array, returning a new array.
   *
   * This is a special case of `map(IntUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to add, may be negative
   * @return a copy of this array with the amount added to each value
   */
  def plus(amount: Int): IntArray = {
    if (amount == 0) {
      return this
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + amount
      i += 1
    }
    new IntArray(result)
  }

  /**
   * Returns an instance with the specified amount subtracted from each value.
   *
   * This is used to subtract from the contents of this array, returning a new array.
   *
   * This is a special case of `map(IntUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param amount  the amount to subtract, may be negative
   * @return a copy of this array with the amount subtracted from each value
   */
  def minus(amount: Int): IntArray = {
    if (amount == 0) {
      return this
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - amount
      i += 1
    }
    new IntArray(result)
  }

  /**
   * Returns an instance with each value multiplied by the specified factor.
   *
   * This is used to multiply the contents of this array, returning a new array.
   *
   * This is a special case of `map(IntUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param factor  the multiplicative factor
   * @return a copy of this array with the each value multiplied by the factor
   */
  def multipliedBy(factor: Int): IntArray = {
    if (factor == 1) {
      return this
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * factor
      i += 1
    }
    new IntArray(result)
  }

  /**
   * Returns an instance with each value divided by the specified divisor.
   *
   * This is used to divide the contents of this array, returning a new array.
   *
   * This is a special case of `map(IntUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param divisor  the value by which the array values are divided
   * @return a copy of this array with the each value divided by the divisor
   */
  def dividedBy(divisor: Int): IntArray = {
    if (divisor == 1) {
      return this
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) / divisor
      i += 1
    }
    new IntArray(result)
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
  def map(operator: IntUnaryOperator): IntArray = {
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsInt(array(i))
      i += 1
    }
    new IntArray(result)
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
  def mapWithIndex(function: IntBinaryOperator): IntArray = {
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = function.applyAsInt(i, array(i))
      i += 1
    }
    new IntArray(result)
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
   * This is a special case of `combine(IntArray, IntBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements added
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def plus(other: IntArray): IntArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + other.array(i)
      i += 1
    }
    new IntArray(result)
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
   * This is a special case of `combine(IntArray, IntBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements subtracted
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def minus(other: IntArray): IntArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) - other.array(i)
      i += 1
    }
    new IntArray(result)
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
   * This is a special case of `combine(IntArray, IntBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements multiplied
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def multipliedBy(other: IntArray): IntArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * other.array(i)
      i += 1
    }
    new IntArray(result)
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
   * This is a special case of `combine(IntArray, IntBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements divided
   * @throws java.lang.IllegalArgumentException if the arrays have different sizes
   */
  def dividedBy(other: IntArray): IntArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) / other.array(i)
      i += 1
    }
    new IntArray(result)
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
  def combine(other: IntArray, operator: IntBinaryOperator): IntArray = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = new Array[Int](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsInt(array(i), other.array(i))
      i += 1
    }
    new IntArray(result)
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
  def combineReduce(other: IntArray, operator: IntTernaryOperator): Int = {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    var result: Int = 0
    var i = 0
    while (i < array.length) {
      result = operator.applyAsInt(result, array(i), other.array(i))
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
  def concat(arrayToConcat: Int*): IntArray = {
    // the Java varargs bridge wraps the caller's array without copying it
    val toConcat: Array[Int] = arrayToConcat match {
      case wrapped: ArraySeq.ofInt => wrapped.unsafeArray
      case _ => arrayToConcat.toArray
    }
    if (array.length == 0) {
      return IntArray.copyOf(toConcat)
    }
    if (toConcat.length == 0) {
      return this
    }
    val result = new Array[Int](array.length + toConcat.length)
    System.arraycopy(array, 0, result, 0, array.length)
    System.arraycopy(toConcat, 0, result, array.length, toConcat.length)
    new IntArray(result)
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
  def concat(arrayToConcat: IntArray): IntArray = {
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
   * This uses `Arrays#sort(int[])`.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @return a sorted copy of this array
   */
  def sorted(): IntArray = {
    if (array.length < 2) {
      return this
    }
    val result = array.clone()
    Arrays.sort(result)
    new IntArray(result)
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
  def min(): Int = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find minimum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var min: Int = java.lang.Integer.MAX_VALUE
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
  def max(): Int = {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find maximum of an empty array")
    }
    if (array.length == 1) {
      return array(0)
    }
    var max: Int = java.lang.Integer.MIN_VALUE
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
   * This is a special case of `reduce(int, IntBinaryOperator)`.
   *
   * @return the total of all the values
   */
  def sum(): Int = {
    var total: Int = 0
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
  def reduce(identity: Int, operator: IntBinaryOperator): Int = {
    var result = identity
    var i = 0
    while (i < array.length) {
      result = operator.applyAsInt(result, array(i))
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    IntArray.Meta.META
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: IntArray =>
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

object IntArray {

  /**
   * An empty int array.
   */
  @static private val EMPTY_INT_ARRAY: Array[Int] = new Array[Int](0)
  /**
   * An empty array.
   */
  @static final val EMPTY: IntArray = new IntArray(EMPTY_INT_ARRAY)

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
  def of(): IntArray = {
    EMPTY
  }

  /**
   * Obtains an immutable array with a single value.
   *
   * @param value  the single value
   * @return an array containing the specified value
   */
  def of(value: Int): IntArray = {
    new IntArray(Array[Int](value))
  }

  /**
   * Obtains an immutable array with two values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @return an array containing the specified values
   */
  def of(value1: Int, value2: Int): IntArray = {
    new IntArray(Array[Int](value1, value2))
  }

  /**
   * Obtains an immutable array with three values.
   *
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @return an array containing the specified values
   */
  def of(value1: Int, value2: Int, value3: Int): IntArray = {
    new IntArray(Array[Int](value1, value2, value3))
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
  def of(value1: Int, value2: Int, value3: Int, value4: Int): IntArray = {
    new IntArray(Array[Int](value1, value2, value3, value4))
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
  def of(value1: Int, value2: Int, value3: Int, value4: Int, value5: Int): IntArray = {
    new IntArray(Array[Int](value1, value2, value3, value4, value5))
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
  def of(value1: Int, value2: Int, value3: Int, value4: Int, value5: Int, value6: Int): IntArray = {
    new IntArray(Array[Int](value1, value2, value3, value4, value5, value6))
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
      value1: Int,
      value2: Int,
      value3: Int,
      value4: Int,
      value5: Int,
      value6: Int,
      value7: Int): IntArray = {

    new IntArray(Array[Int](value1, value2, value3, value4, value5, value6, value7))
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
      value1: Int,
      value2: Int,
      value3: Int,
      value4: Int,
      value5: Int,
      value6: Int,
      value7: Int,
      value8: Int): IntArray = {

    new IntArray(Array[Int](value1, value2, value3, value4, value5, value6, value7, value8))
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
      value1: Int,
      value2: Int,
      value3: Int,
      value4: Int,
      value5: Int,
      value6: Int,
      value7: Int,
      value8: Int,
      otherValues: Int*): IntArray = {

    // the Java varargs bridge wraps the caller's array without copying it
    val others: Array[Int] = otherValues match {
      case wrapped: ArraySeq.ofInt => wrapped.unsafeArray
      case _ => otherValues.toArray
    }
    val base = new Array[Int](others.length + 8)
    base(0) = value1
    base(1) = value2
    base(2) = value3
    base(3) = value4
    base(4) = value5
    base(5) = value6
    base(6) = value7
    base(7) = value8
    System.arraycopy(others, 0, base, 8, others.length)
    new IntArray(base)
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
  def of(size: Int, valueFunction: IntUnaryOperator): IntArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Int](size)
    Arrays.setAll(array, valueFunction)
    new IntArray(array)
  }

  /**
   * Obtains an instance with entries filled from a stream.
   *
   * The stream is converted to an array using `IntStream#toArray()`.
   *
   * @param stream  the stream of elements
   * @return an array initialized using the stream
   */
  def of(stream: IntStream): IntArray = {
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
  def ofUnsafe(array: Array[Int]): IntArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new IntArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance from a collection of `Integer`.
   *
   * The order of the values in the returned array is the order in which elements are returned
   * from the iterator of the collection.
   *
   * @param collection  the collection to initialize from
   * @return an array containing the values from the collection in iteration order
   */
  def copyOf(collection: Collection[java.lang.Integer]): IntArray = {
    if (collection.size() == 0) {
      return EMPTY
    }
    collection match {
      case list: ImmList =>
        list.underlying
      case _ =>
        new IntArray(Ints.toArray(collection))
    }
  }

  /**
   * Obtains an instance from an array of `int`.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy, cloned
   * @return an array containing the specified values
   */
  def copyOf(array: Array[Int]): IntArray = {
    if (array.length == 0) {
      return EMPTY
    }
    new IntArray(array.clone())
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
  def copyOf(array: Array[Int], fromIndex: Int): IntArray = {
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
  def copyOf(array: Array[Int], fromIndexInclusive: Int, toIndexExclusive: Int): IntArray = {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length)
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length)
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return EMPTY
    }
    new IntArray(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   *
   * @param size  the number of elements
   * @return an array filled with zeroes
   */
  def filled(size: Int): IntArray = {
    if (size == 0) {
      return EMPTY
    }
    new IntArray(new Array[Int](size))
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   *
   * @param size  the number of elements
   * @param value  the value of all the elements
   * @return an array filled with the specified value
   */
  def filled(size: Int, value: Int): IntArray = {
    if (size == 0) {
      return EMPTY
    }
    val array = new Array[Int](size)
    Arrays.fill(array, value)
    new IntArray(array)
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `Iterator` representation of the array.
   */
  private[array] class ImmIterator(array: Array[Int]) extends ListIterator[java.lang.Integer] {

    private var index: Int = 0

    override def hasNext(): Boolean = {
      index < array.length
    }

    override def hasPrevious(): Boolean = {
      index > 0
    }

    override def next(): java.lang.Integer = {
      if (hasNext()) {
        val value = array(index)
        index += 1
        return java.lang.Integer.valueOf(value)
      }
      throw new NoSuchElementException("Iteration has reached the last element")
    }

    override def previous(): java.lang.Integer = {
      if (hasPrevious()) {
        index -= 1
        return java.lang.Integer.valueOf(array(index))
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
      throw new UnsupportedOperationException("Unable to remove from IntArray")
    }

    override def set(value: java.lang.Integer): Unit = {
      throw new UnsupportedOperationException("Unable to set value in IntArray")
    }

    override def add(value: java.lang.Integer): Unit = {
      throw new UnsupportedOperationException("Unable to add value to IntArray")
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable `List` representation of the array.
   */
  @SerialVersionUID(1L)
  private[array] class ImmList(private[array] val underlying: IntArray)
    extends AbstractList[java.lang.Integer] with RandomAccess with Serializable {

    override def size(): Int = {
      underlying.size()
    }

    override def get(index: Int): java.lang.Integer = {
      java.lang.Integer.valueOf(underlying.get(index))
    }

    override def contains(obj: Object): Boolean = {
      obj match {
        case value: java.lang.Integer => underlying.contains(value.intValue())
        case _ => false
      }
    }

    override def indexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Integer => underlying.indexOf(value.intValue())
        case _ => -1
      }
    }

    override def lastIndexOf(obj: Object): Int = {
      obj match {
        case value: java.lang.Integer => underlying.lastIndexOf(value.intValue())
        case _ => -1
      }
    }

    override def iterator(): ListIterator[java.lang.Integer] = {
      listIterator()
    }

    override def listIterator(): ListIterator[java.lang.Integer] = {
      new ImmIterator(underlying.array)
    }

    override protected def removeRange(fromIndex: Int, toIndex: Int): Unit = {
      throw new UnsupportedOperationException("Unable to remove range from IntArray")
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

    override def builder(): BeanBuilder[IntArray] = {
      new BasicImmutableBeanBuilder[IntArray](this) {
        private var array: Array[Int] = EMPTY_INT_ARRAY

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            array.clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[IntArray] = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            this.array = ArgChecker.notNull(value, "value").asInstanceOf[Array[Int]].clone()
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): IntArray = {
          new IntArray(array)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[IntArray]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[array] object Meta {

    private[array] val META: MetaBean = new Meta()

    private val ARRAY: MetaProperty[Array[Int]] = new BasicMetaProperty[Array[Int]]("array") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[IntArray]
      }

      override def propertyType(): Class[Array[Int]] = {
        classOf[Array[Int]]
      }

      override def propertyGenericType(): Type = {
        classOf[Array[Int]]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): Array[Int] = {
        bean.asInstanceOf[IntArray].toArray()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("array", ARRAY)
  }

}
