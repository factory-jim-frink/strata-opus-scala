/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.util.function.DoubleBinaryOperator
import java.util.function.DoubleUnaryOperator

import scala.annotation.static

import com.google.common.math.DoubleMath

/**
 * Contains utility methods for maths on double arrays.
 *
 * This utility is used throughout the system when working with double arrays.
 */
final class DoubleArrayMath private ()

object DoubleArrayMath {

  /**
   * An empty `double` array.
   */
  @static final val EMPTY_DOUBLE_ARRAY: Array[Double] = new Array[Double](0)
  /**
   * An empty `Double` array.
   */
  @static final val EMPTY_DOUBLE_OBJECT_ARRAY: Array[java.lang.Double] = new Array[java.lang.Double](0)

  //-------------------------------------------------------------------------
  /**
   * Converts a `double` array to a `Double` array.
   *
   * @param array  the array to convert
   * @return the converted array
   */
  @static def toObject(array: Array[Double]): Array[java.lang.Double] = {
    if (array.length == 0) {
      return EMPTY_DOUBLE_OBJECT_ARRAY
    }
    val result = new Array[java.lang.Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = java.lang.Double.valueOf(array(i))
      i += 1
    }
    result
  }

  /**
   * Converts a `Double` array to a `double` array.
   *
   * Throws an exception if null is found.
   *
   * @param array  the array to convert
   * @return the converted array
   * @throws java.lang.NullPointerException if null found
   */
  @static def toPrimitive(array: Array[java.lang.Double]): Array[Double] = {
    if (array.length == 0) {
      return EMPTY_DOUBLE_ARRAY
    }
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i).doubleValue()
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the sum total of all the elements in the array.
   *
   * The input array is not mutated.
   *
   * @param array  the array to sum
   * @return the sum total of all the elements
   */
  @static def sum(array: Array[Double]): Double = {
    var total = 0d
    var i = 0
    while (i < array.length) {
      total += array(i)
      i += 1
    }
    total
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an addition to each element in the array, returning a new array.
   *
   * The result is always a new array. The input array is not mutated.
   *
   * @param array  the input array, not mutated
   * @param valueToAdd  the value to add
   * @return the resulting array
   */
  @static def applyAddition(array: Array[Double], valueToAdd: Double): Array[Double] = {
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) + valueToAdd
      i += 1
    }
    result
  }

  /**
   * Applies a multiplication to each element in the array, returning a new array.
   *
   * The result is always a new array. The input array is not mutated.
   *
   * @param array  the input array, not mutated
   * @param valueToMultiplyBy  the value to multiply by
   * @return the resulting array
   */
  @static def applyMultiplication(array: Array[Double], valueToMultiplyBy: Double): Array[Double] = {
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = array(i) * valueToMultiplyBy
      i += 1
    }
    result
  }

  /**
   * Applies an operator to each element in the array, returning a new array.
   *
   * The result is always a new array. The input array is not mutated.
   *
   * @param array  the input array, not mutated
   * @param operator  the operator to use
   * @return the resulting array
   */
  @static def apply(array: Array[Double], operator: DoubleUnaryOperator): Array[Double] = {
    val result = new Array[Double](array.length)
    var i = 0
    while (i < array.length) {
      result(i) = operator.applyAsDouble(array(i))
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a constant value to each element in the array by mutation.
   *
   * The input array is mutated.
   *
   * @param array  the array to mutate
   * @param valueToAdd  the value to add
   */
  @static def mutateByAddition(array: Array[Double], valueToAdd: Double): Unit = {
    var i = 0
    while (i < array.length) {
      array(i) += valueToAdd
      i += 1
    }
  }

  /**
   * Adds values in two arrays together, mutating the first array.
   *
   * The arrays must be the same length. Each value in `arrayToAdd` is added to the value at the
   * corresponding index in `array`.
   *
   * @param array  the array to mutate
   * @param arrayToAdd  the array containing values to add
   */
  @static def mutateByAddition(array: Array[Double], arrayToAdd: Array[Double]): Unit = {
    val length = DoubleArrayMath.length(array, arrayToAdd)
    var i = 0
    while (i < length) {
      array(i) += arrayToAdd(i)
      i += 1
    }
  }

  /**
   * Multiplies each element in the array by a value by mutation.
   *
   * The input array is mutated.
   *
   * @param array  the array to mutate
   * @param valueToMultiplyBy  the value to multiply by
   */
  @static def mutateByMultiplication(array: Array[Double], valueToMultiplyBy: Double): Unit = {
    var i = 0
    while (i < array.length) {
      array(i) *= valueToMultiplyBy
      i += 1
    }
  }

  /**
   * Multiplies values in two arrays, mutating the first array.
   *
   * The arrays must be the same length. Each value in `array` is multiplied by the value at the
   * corresponding index in `arrayToMultiplyBy`.
   *
   * @param array  the array to mutate
   * @param arrayToMultiplyBy  the array containing values to multiply by
   */
  @static def mutateByMultiplication(array: Array[Double], arrayToMultiplyBy: Array[Double]): Unit = {
    val length = DoubleArrayMath.length(array, arrayToMultiplyBy)
    var i = 0
    while (i < length) {
      array(i) *= arrayToMultiplyBy(i)
      i += 1
    }
  }

  /**
   * Mutates each element in the array using an operator by mutation.
   *
   * The input array is mutated.
   *
   * @param array  the array to mutate
   * @param operator  the operator to use to perform the mutation
   */
  @static def mutate(array: Array[Double], operator: DoubleUnaryOperator): Unit = {
    var i = 0
    while (i < array.length) {
      array(i) = operator.applyAsDouble(array(i))
      i += 1
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two arrays, returning an array where each element is the sum of the two matching inputs.
   *
   * Each element in the result will be the sum of the matching index in the two input arrays.
   * The two input arrays must have the same length.
   *
   * For example:
   * {{{
   *  double[] array1 = {1, 5, 9};
   *  double[] array2 = {2, 3, 2};
   *  double[] result = DoubleArrayMath.combineByAddition(array1, array2);
   *  // result contains {3, 8, 11}
   * }}}
   *
   * The result is always a new array. The input arrays are not mutated.
   *
   * @param array1  the first array
   * @param array2  the second array
   * @return an array combining the two input arrays using the plus operator
   */
  @static def combineByAddition(array1: Array[Double], array2: Array[Double]): Array[Double] = {
    combine(array1, array2, (a, b) => a + b)
  }

  /**
   * Combines two arrays, returning an array where each element is the multiplication of the two matching inputs.
   *
   * Each element in the result will be the multiplication of the matching index in the two input arrays.
   * The two input arrays must have the same length.
   *
   * For example:
   * {{{
   *  double[] array1 = {1, 5, 9};
   *  double[] array2 = {2, 3, 4};
   *  double[] result = DoubleArrayMath.combineByMultiplication(array1, array2);
   *  // result contains {2, 15, 36}
   * }}}
   *
   * The result is always a new array. The input arrays are not mutated.
   *
   * @param array1  the first array
   * @param array2  the second array
   * @return an array combining the two input arrays using the multiply operator
   */
  @static def combineByMultiplication(array1: Array[Double], array2: Array[Double]): Array[Double] = {
    combine(array1, array2, (a, b) => a * b)
  }

  /**
   * Combines two arrays, returning an array where each element is the combination of the two matching inputs.
   *
   * Each element in the result will be the combination of the matching index in the two
   * input arrays using the operator. The two input arrays must have the same length.
   *
   * The result is always a new array. The input arrays are not mutated.
   *
   * @param array1  the first array
   * @param array2  the second array
   * @param operator  the operator to use when combining values
   * @return an array combining the two input arrays using the operator
   */
  @static def combine(array1: Array[Double], array2: Array[Double], operator: DoubleBinaryOperator): Array[Double] = {
    val length = DoubleArrayMath.length(array1, array2)
    val result = new Array[Double](length)
    var i = 0
    while (i < length) {
      result(i) = operator.applyAsDouble(array1(i), array2(i))
      i += 1
    }
    result
  }

  /**
   * Combines two arrays, returning an array where each element is the combination of the two matching inputs.
   *
   * Each element in the result will be the combination of the matching index in the two
   * input arrays using the operator.
   * The result will have the length of the longest of the two inputs.
   * Where one array is longer than the other, the values from the longer array will be used.
   *
   * The result is always a new array. The input arrays are not mutated.
   *
   * @param array1  the first array
   * @param array2  the second array
   * @param operator  the operator to use when combining values
   * @return an array combining the two input arrays using the operator
   */
  @static def combineLenient(
      array1: Array[Double],
      array2: Array[Double],
      operator: DoubleBinaryOperator): Array[Double] = {

    val len1 = array1.length
    val len2 = array2.length
    if (len1 == len2) {
      return combine(array1, array2, operator)
    }
    val size = Math.max(len1, len2)
    val result = new Array[Double](size)
    var i = 0
    while (i < size) {
      if (i < len1) {
        if (i < len2) {
          result(i) = operator.applyAsDouble(array1(i), array2(i))
        } else {
          result(i) = array1(i)
        }
      } else {
        result(i) = array2(i)
      }
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  /**
   * Compares each element in the array to zero within a tolerance.
   *
   * An empty array returns true;
   *
   * The input array is not mutated.
   *
   * @param array  the array to check
   * @param tolerance  the tolerance to use
   * @return true if the array is effectively equal to zero
   */
  @static def fuzzyEqualsZero(array: Array[Double], tolerance: Double): Boolean = {
    var i = 0
    while (i < array.length) {
      if (!DoubleMath.fuzzyEquals(array(i), 0d, tolerance)) {
        return false
      }
      i += 1
    }
    true
  }

  /**
   * Compares each element in the first array to the matching index in the second array within a tolerance.
   *
   * If the arrays differ in length, false is returned.
   *
   * The input arrays are not mutated.
   *
   * @param array1  the first array to check
   * @param array2  the second array to check
   * @param tolerance  the tolerance to use
   * @return true if the arrays are effectively equal
   */
  @static def fuzzyEquals(array1: Array[Double], array2: Array[Double], tolerance: Double): Boolean = {
    if (array1.length != array2.length) {
      return false
    }
    var i = 0
    while (i < array1.length) {
      if (!DoubleMath.fuzzyEquals(array1(i), array2(i), tolerance)) {
        return false
      }
      i += 1
    }
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Sorts the two arrays, retaining the associated values with the sorted keys.
   *
   * The two arrays must be the same size and represent a pair of key to value.
   * The sort order is determined by the array of keys.
   * The position of each value is changed to match that of the sorted keys.
   *
   * The input arrays are mutated.
   *
   * @param keys  the array of keys to sort
   * @param values  the array of associated values to retain
   */
  @static def sortPairs(keys: Array[Double], values: Array[Double]): Unit = {
    val len1 = keys.length
    if (len1 != values.length) {
      throw new IllegalArgumentException("Arrays cannot be sorted as they differ in length")
    }
    dualArrayQuickSort(keys, values, 0, len1 - 1)
  }

  @static private def dualArrayQuickSort(keys: Array[Double], values: Array[Double], left: Int, right: Int): Unit = {
    if (right > left) {
      val pivot = (left + right) >> 1
      val pivotNewIndex = partition(keys, values, left, right, pivot)
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1)
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right)
    }
  }

  @static private def partition(keys: Array[Double], values: Array[Double], left: Int, right: Int, pivot: Int): Int = {
    val pivotValue = keys(pivot)
    swap(keys, values, pivot, right)
    var storeIndex = left
    var i = left
    while (i < right) {
      if (keys(i) <= pivotValue) {
        swap(keys, values, i, storeIndex)
        storeIndex += 1
      }
      i += 1
    }
    swap(keys, values, storeIndex, right)
    storeIndex
  }

  @static private def swap(keys: Array[Double], values: Array[Double], first: Int, second: Int): Unit = {
    var t = keys(first)
    keys(first) = keys(second)
    keys(second) = t
    t = values(first)
    values(first) = values(second)
    values(second) = t
  }

  /**
   * Returns a copy of the first array in the order defined by the position values of the second array.
   *
   * The two arrays must be the same size.
   * The order is determined by the array of positions.
   * The result value at each entry is changed to the value at the position of the positions entry.
   * It is not checked that the positions array does not contain duplicates.
   *
   * The result is a new array. The input arrays are not mutated.
   *
   * e.g
   * {{{
   * double[] values = { 1d, 5d, 10d };
   * int[] positions = { 2, 0, 1 };
   * reorderedCopy(values, positions); // returns [10d, 1d, 5d]
   * }}}
   *
   * @param values  the array of values
   * @param positions  the array of positions
   * @return the reordered copy
   * @throws java.lang.IllegalArgumentException if any of the positions do not correspond to an index in the values
   */
  @static def reorderedCopy(values: Array[Double], positions: Array[Int]): Array[Double] = {
    val len1 = positions.length
    if (len1 != values.length) {
      throw new IllegalArgumentException("Value array cannot be reordered as they differ in length")
    }
    val result = new Array[Double](len1)
    var i = 0
    while (i < len1) {
      val key = positions(i)
      ArgChecker.inRange(key, 0, len1, "key")
      result(i) = values(key)
      i += 1
    }
    result
  }

  /**
   * Sorts the two arrays, retaining the associated values with the sorted keys.
   *
   * The two arrays must be the same size and represent a pair of key to value.
   * The sort order is determined by the array of keys.
   * The position of each value is changed to match that of the sorted keys.
   *
   * The input arrays are mutated.
   *
   * @param keys  the array of keys to sort
   * @param values  the array of associated values to retain
   */
  @static def sortPairs(keys: Array[Double], values: Array[Int]): Unit = {
    val len1 = keys.length
    if (len1 != values.length) {
      throw new IllegalArgumentException("Arrays cannot be sorted as they differ in length")
    }
    dualArrayQuickSort(keys, values, 0, len1 - 1)
  }

  @static private def dualArrayQuickSort(keys: Array[Double], values: Array[Int], left: Int, right: Int): Unit = {
    if (right > left) {
      val pivot = (left + right) >> 1
      val pivotNewIndex = partition(keys, values, left, right, pivot)
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1)
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right)
    }
  }

  @static private def partition(keys: Array[Double], values: Array[Int], left: Int, right: Int, pivot: Int): Int = {
    val pivotValue = keys(pivot)
    swap(keys, values, pivot, right)
    var storeIndex = left
    var i = left
    while (i < right) {
      if (keys(i) <= pivotValue) {
        swap(keys, values, i, storeIndex)
        storeIndex += 1
      }
      i += 1
    }
    swap(keys, values, storeIndex, right)
    storeIndex
  }

  @static private def swap(keys: Array[Double], values: Array[Int], first: Int, second: Int): Unit = {
    val t = keys(first)
    keys(first) = keys(second)
    keys(second) = t
    val t2 = values(first)
    values(first) = values(second)
    values(second) = t2
  }

  //-------------------------------------------------------------------------
  /**
   * Sorts the two arrays, retaining the associated values with the sorted keys.
   *
   * The two arrays must be the same size and represent a pair of key to value.
   * The sort order is determined by the array of keys.
   * The position of each value is changed to match that of the sorted keys.
   *
   * The input arrays are mutated.
   *
   * @tparam V the type of the values
   * @param keys  the array of keys to sort
   * @param values  the array of associated values to retain
   */
  @static def sortPairs[V <: AnyRef](keys: Array[Double], values: Array[V]): Unit = {
    val len1 = keys.length
    if (len1 != values.length) {
      throw new IllegalArgumentException("Arrays cannot be sorted as they differ in length")
    }
    dualArrayQuickSort(keys, values, 0, len1 - 1)
  }

  @static private def dualArrayQuickSort[T <: AnyRef](
      keys: Array[Double],
      values: Array[T],
      left: Int,
      right: Int): Unit = {

    if (right > left) {
      val pivot = (left + right) >> 1
      val pivotNewIndex = partition(keys, values, left, right, pivot)
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1)
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right)
    }
  }

  @static private def partition[T <: AnyRef](
      keys: Array[Double],
      values: Array[T],
      left: Int,
      right: Int,
      pivot: Int): Int = {

    val pivotValue = keys(pivot)
    swap(keys, values, pivot, right)
    var storeIndex = left
    var i = left
    while (i < right) {
      if (keys(i) <= pivotValue) {
        swap(keys, values, i, storeIndex)
        storeIndex += 1
      }
      i += 1
    }
    swap(keys, values, storeIndex, right)
    storeIndex
  }

  @static private def swap[T <: AnyRef](keys: Array[Double], values: Array[T], first: Int, second: Int): Unit = {
    val x = keys(first)
    keys(first) = keys(second)
    keys(second) = x
    val t = values(first)
    values(first) = values(second)
    values(second) = t
  }

  // return the array lengths if they are the same, otherwise throws an IllegalArgumentException
  @static private def length(array1: Array[Double], array2: Array[Double]): Int = {
    val len1 = array1.length
    val len2 = array2.length
    if (len1 != len2) {
      throw new IllegalArgumentException("Arrays cannot be combined as they differ in length")
    }
    len1
  }

}
