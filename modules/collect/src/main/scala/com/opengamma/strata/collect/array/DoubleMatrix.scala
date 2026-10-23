/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array

import java.io.Serializable
import java.util.Arrays
import java.util.Map
import java.util.NoSuchElementException
import java.util.function.DoubleBinaryOperator
import java.util.function.DoubleUnaryOperator
import java.util.function.IntFunction
import java.util.function.IntToDoubleFunction

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs
import scala.collection.immutable.ArraySeq
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutableConstructor
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.function.IntIntDoubleConsumer
import com.opengamma.strata.collect.function.IntIntDoubleToDoubleFunction
import com.opengamma.strata.collect.function.IntIntToDoubleFunction

/**
 * An immutable two-dimensional array of `double` values.
 *
 * This provides functionality similar to `List` but for a rectangular `double[][]`.
 *
 * In mathematical terms, this is a two-dimensional matrix.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class DoubleMatrix private[array] (
    /**
     * The underlying array of doubles.
     */
    @(PropertyDefinition @field)(validate = "notNull", get = "")
    private val array: Array[Array[Double]],
    /**
     * The number of rows.
     */
    @transient private val rows: Int,  // derived, not a property
    /**
     * The number of columns.
     */
    @transient private val columns: Int)  // derived, not a property
  extends Matrix with Serializable with ImmutableBean {

  /**
   * The number of elements.
   */
  @transient private val elements: Int = rows * columns  // derived, not a property

  @ImmutableConstructor
  private def this(array: Array[Array[Double]]) = {
    this(
        DoubleMatrix.validatedArray(array),
        array.length,
        if (array.length == 0) 0 else array(0).length)
  }

  // ensure standard constructor is invoked
  private def readResolve(): Object = {
    new DoubleMatrix(array)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of dimensions of this matrix.
   *
   * @return two
   */
  override def dimensions(): Int = {
    2
  }

  /**
   * Gets the size of this matrix.
   *
   * This is the total number of elements.
   *
   * @return the matrix size, zero or greater
   */
  override def size(): Int = {
    elements
  }

  /**
   * Gets the number of rows of this matrix.
   *
   * @return the number of rows
   */
  def rowCount(): Int = {
    rows
  }

  /**
   * Gets the number of columns of this matrix.
   *
   * @return the number of columns
   */
  def columnCount(): Int = {
    columns
  }

  /**
   * Checks if this matrix is square.
   *
   * A square matrix has the same number of rows and columns.
   *
   * @return true if square
   */
  def isSquare(): Boolean = {
    rows == columns
  }

  /**
   * Checks if this matrix is empty.
   *
   * @return true if empty
   */
  def isEmpty(): Boolean = {
    elements == 0
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value at the specified row and column in this matrix.
   *
   * @param row  the zero-based row index to retrieve
   * @param column  the zero-based column index to retrieve
   * @return the value at the row and column
   * @throws java.lang.IndexOutOfBoundsException if either index is invalid
   */
  def get(row: Int, column: Int): Double = {
    array(row)(column)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the row at the specified index.
   *
   * @param row  the zero-based row index to retrieve
   * @return the row
   */
  def row(row: Int): DoubleArray = {
    DoubleArray.ofUnsafe(array(row))
  }

  /**
   * Gets the row at the specified index as an independent array.
   *
   * @param row  the zero-based row index to retrieve
   * @return the row as a cloned array
   */
  def rowArray(row: Int): Array[Double] = {
    array(row).clone()
  }

  /**
   * Gets the column at the specified index.
   *
   * @param column  the zero-based column index to retrieve
   * @return the column
   */
  def column(column: Int): DoubleArray = {
    val valueFunction: IntToDoubleFunction = i => array(i)(column)
    DoubleArray.of(rows, valueFunction)
  }

  /**
   * Gets the column at the specified index as an independent array.
   *
   * @param column  the zero-based column index to retrieve
   * @return the column as a cloned array
   */
  def columnArray(column: Int): Array[Double] = {
    this.column(column).toArrayUnsafe()
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent `double[][]`.
   *
   * @return an array of arrays containing a copy of matrix elements
   */
  def toArray(): Array[Array[Double]] = {
    DoubleMatrix.deepClone(array, rows, columns)
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
  def toArrayUnsafe(): Array[Array[Double]] = {
    array
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each value in the matrix.
   *
   * This is used to perform an action on the contents of this matrix.
   * The action receives the row, the column and the value.
   * For example, the action could print out the matrix.
   * {{{
   *   base.forEach((row, col, value) -> System.out.println(row + ": " + col + ": " + value));
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param action  the action to be applied
   */
  def forEach(action: IntIntDoubleConsumer): Unit = {
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        action.accept(i, j, array(i)(j))
        j += 1
      }
      i += 1
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an instance with the value at the specified index changed.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param row  the zero-based row index to retrieve
   * @param column  the zero-based column index to retrieve
   * @param newValue  the new value to store
   * @return a copy of this matrix with the value at the index changed
   * @throws java.lang.IndexOutOfBoundsException if either index is invalid
   */
  def `with`(row: Int, column: Int, newValue: Double): DoubleMatrix = {
    if (java.lang.Double.doubleToLongBits(array(row)(column)) == java.lang.Double.doubleToLongBits(newValue)) {
      return this
    }
    val result = array.clone()  // shallow clone rows array
    result(row) = result(row).clone()  // clone the column actually being changed, share the rest
    result(row)(column) = newValue
    new DoubleMatrix(result, rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with each value multiplied by the specified factor.
   *
   * This is used to multiply the contents of this matrix, returning a new matrix.
   *
   * This is a special case of `map(DoubleUnaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param factor  the multiplicative factor
   * @return a copy of this matrix with the each value multiplied by the factor
   */
  def multipliedBy(factor: Double): DoubleMatrix = {
    if (factor == 1d) {
      return this
    }
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = array(i)(j) * factor
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  /**
   * Returns an instance with an operation applied to each value in the matrix.
   *
   * This is used to perform an operation on the contents of this matrix, returning a new matrix.
   * The operator only receives the value.
   * For example, the operator could take the inverse of each element.
   * {{{
   *   result = base.map(value -> 1 / value);
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param operator  the operator to be applied
   * @return a copy of this matrix with the operator applied to the original values
   */
  def map(operator: DoubleUnaryOperator): DoubleMatrix = {
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = operator.applyAsDouble(array(i)(j))
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  /**
   * Returns an instance with an operation applied to each indexed value in the matrix.
   *
   * This is used to perform an operation on the contents of this matrix, returning a new matrix.
   * The function receives the row index, column index and the value.
   * For example, the operator could multiply the value by the index.
   * {{{
   *   result = base.mapWithIndex((index, value) -> index * value);
   * }}}
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param function  the function to be applied
   * @return a copy of this matrix with the operator applied to the original values
   */
  def mapWithIndex(function: IntIntDoubleToDoubleFunction): DoubleMatrix = {
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = function.applyAsDouble(i, j, array(i)(j))
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance where each element is the sum of the matching values
   * in this array and the other matrix.
   *
   * This is used to add two matrices, returning a new matrix.
   * Element `(i,j)` in the resulting matrix is equal to element `(i,j)` in this matrix
   * plus element `(i,j)` in the other matrix.
   * The matrices must be of the same size.
   *
   * This is a special case of `combine(DoubleMatrix, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other matrix
   * @return a copy of this matrix with matching elements added
   * @throws java.lang.IllegalArgumentException if the matrices have different sizes
   */
  def plus(other: DoubleMatrix): DoubleMatrix = {
    if (rows != other.rows || columns != other.columns) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = array(i)(j) + other.array(i)(j)
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  /**
   * Returns an instance where each element is equal to the difference between the
   * matching values in this matrix and the other matrix.
   *
   * This is used to subtract the second matrix from the first, returning a new matrix.
   * Element `(i,j)` in the resulting matrix is equal to element `(i,j)` in this matrix
   * minus element `(i,j)` in the other matrix.
   * The matrices must be of the same size.
   *
   * This is a special case of `combine(DoubleMatrix, DoubleBinaryOperator)`.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other matrix
   * @return a copy of this matrix with matching elements subtracted
   * @throws java.lang.IllegalArgumentException if the matrices have different sizes
   */
  def minus(other: DoubleMatrix): DoubleMatrix = {
    if (rows != other.rows || columns != other.columns) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = array(i)(j) - other.array(i)(j)
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  /**
   * Returns an instance where each element is formed by some combination of the matching
   * values in this matrix and the other matrix.
   *
   * This is used to combine two matrices, returning a new matrix.
   * Element `(i,j)` in the resulting matrix is equal to the result of the operator
   * when applied to element `(i,j)` in this array and element `(i,j)` in the other array.
   * The arrays must be of the same size.
   *
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other matrix
   * @param operator  the operator used to combine each pair of values
   * @return a copy of this matrix combined with the specified matrix
   * @throws java.lang.IllegalArgumentException if the matrices have different sizes
   */
  def combine(other: DoubleMatrix, operator: DoubleBinaryOperator): DoubleMatrix = {
    if (rows != other.rows || columns != other.columns) {
      throw new IllegalArgumentException("Arrays have different sizes")
    }
    val result = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result(i)(j) = operator.applyAsDouble(array(i)(j), other.array(i)(j))
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(result, rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of all the values in the matrix.
   *
   * This is a special case of `reduce(double, DoubleBinaryOperator)`.
   *
   * @return the total of all the values
   */
  def total(): Double = {
    var total: Double = 0
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < columns) {
        total += array(i)(j)
        j += 1
      }
      i += 1
    }
    total
  }

  /**
   * Reduces this matrix returning a single value.
   *
   * This is used to reduce the values in this matrix to a single value.
   * The operator is called once for each element in the matrix.
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
    while (i < rows) {
      var j = 0
      while (j < columns) {
        result = operator.applyAsDouble(result, array(i)(j))
        j += 1
      }
      i += 1
    }
    result
  }

  //-------------------------------------------------------------------------
  /**
   * Transposes the matrix.
   *
   * This converts a matrix of `m x n` into a matrix of `n x m`.
   * Each element is moved to the opposite position.
   *
   * @return the transposed matrix
   */
  def transpose(): DoubleMatrix = {
    val valueFunction: IntIntToDoubleFunction = (i, j) => array(j)(i)
    DoubleMatrix.of(columns, rows, valueFunction)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      return true
    }
    obj match {
      case other: DoubleMatrix =>
        if (columns != other.columns || rows != other.rows) {
          return false
        }
        var i = 0
        while (i < rows) {
          var j = 0
          while (j < columns) {
            if (java.lang.Double.doubleToLongBits(array(i)(j)) != java.lang.Double.doubleToLongBits(other.array(i)(j))) {
              return false
            }
            j += 1
          }
          i += 1
        }
        true
      case _ =>
        false
    }
  }

  override def hashCode(): Int = {
    var result = 1
    var i = 0
    while (i < rows) {
      result = 31 * result + Arrays.hashCode(array(i))
      i += 1
    }
    result
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuffer()
    var k = 0
    while (k < array.length) {
      val d = array(k)
      var i = 0
      while (i < d.length) {
        buf.append(d(i))
        buf.append(if (i == d.length - 1) "\n" else " ")
        i += 1
      }
      k += 1
    }
    buf.toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): DoubleMatrix.Meta = {
    DoubleMatrix.Meta.INSTANCE
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object DoubleMatrix {

  /**
   * An empty array.
   */
  @static final val EMPTY: DoubleMatrix = new DoubleMatrix(Array.ofDim[Double](0, 0), 0, 0)

  @static private val REGISTERED: Boolean = {
    MetaBean.register(DoubleMatrix.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance.
   *
   * @return the empty immutable matrix
   */
  def of(): DoubleMatrix = {
    EMPTY
  }

  /**
   * Obtains an immutable array with the specified size and values.
   *
   * The first two arguments specify the size.
   * The remaining arguments specify the values, all of row 0, then row 1, and so on.
   * There must be be `rows * columns` values.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param values  the values
   * @return an array containing the specified value
   * @throws java.lang.IllegalArgumentException if the values array if the incorrect length
   */
  @varargs
  def of(rows: Int, columns: Int, values: Double*): DoubleMatrix = {
    // the Java varargs bridge wraps the caller's array without copying it
    val valuesArray: Array[Double] = values match {
      case wrapped: ArraySeq.ofDouble => wrapped.unsafeArray
      case _ => values.toArray
    }
    if (valuesArray.length != rows * columns) {
      throw new IllegalArgumentException("Values array not of length rows * columns")
    }
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < valuesArray.length) {
      array(i / columns)(i % columns) = valuesArray(i)
      i += 1
    }
    new DoubleMatrix(array, rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with entries filled using a function.
   *
   * The function is passed the row and column index, returning the value.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param valueFunction  the function used to populate the value
   * @return a matrix initialized using the function
   */
  def of(rows: Int, columns: Int, valueFunction: IntIntToDoubleFunction): DoubleMatrix = {
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < array.length) {
      val inner = array(i)
      var j = 0
      while (j < inner.length) {
        inner(j) = valueFunction.applyAsDouble(i, j)
        j += 1
      }
      i += 1
    }
    new DoubleMatrix(array, rows, columns)
  }

  /**
   * Obtains an instance with entries filled using a function.
   *
   * The function is passed the row index, returning the column values.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param valuesFunction  the function used to populate the values
   * @return a matrix initialized using the function
   */
  def ofArrays(rows: Int, columns: Int, valuesFunction: IntFunction[Array[Double]]): DoubleMatrix = {
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < array.length) {
      val values = valuesFunction.apply(i)
      if (values.length != columns) {
        throw new IllegalArgumentException(Messages.format(
            "Function returned array of incorrect length {}, expected {}",
            Integer.valueOf(values.length),
            Integer.valueOf(columns)))
      }
      array(i) = values.clone()
      i += 1
    }
    new DoubleMatrix(array, rows, columns)
  }

  /**
   * Obtains an instance with entries filled using a function.
   *
   * The function is passed the row index, returning the column values.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param valuesFunction  the function used to populate the values
   * @return a matrix initialized using the function
   */
  def ofArrayObjects(rows: Int, columns: Int, valuesFunction: IntFunction[DoubleArray]): DoubleMatrix = {
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < array.length) {
      val values = valuesFunction.apply(i)
      if (values.size() != columns) {
        throw new IllegalArgumentException(Messages.format(
            "Function returned array of incorrect length {}, expected {}",
            Integer.valueOf(values.size()),
            Integer.valueOf(columns)))
      }
      array(i) = values.toArrayUnsafe()
      i += 1
    }
    new DoubleMatrix(array, rows, columns)
  }

  /**
   * Obtains an instance by wrapping a `double[][]`.
   *
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   *
   * The `double[][]` must be rectangular, with the same length for each row.
   * This is not validated.
   *
   * @param array  the array to assign
   * @return a matrix wrapping the specified array
   */
  def ofUnsafe(array: Array[Array[Double]]): DoubleMatrix = {
    val rows = array.length
    if (rows == 0 || array(0).length == 0) {
      return EMPTY
    }
    new DoubleMatrix(array, rows, array(0).length)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a `double[][]`.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy, cloned
   * @return a matrix containing the specified values
   */
  def copyOf(array: Array[Array[Double]]): DoubleMatrix = {
    val rows = array.length
    if (rows == 0 || array(0).length == 0) {
      return EMPTY
    }
    val columns = array(0).length
    new DoubleMatrix(deepClone(array, rows, columns), rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @return a matrix filled with zeroes
   */
  def filled(rows: Int, columns: Int): DoubleMatrix = {
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    new DoubleMatrix(Array.ofDim[Double](rows, columns), rows, columns)
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   *
   * @param rows  the number of rows
   * @param columns  the number of columns
   * @param value  the value of all the elements
   * @return a matrix filled with the specified value
   */
  def filled(rows: Int, columns: Int, value: Double): DoubleMatrix = {
    if (rows == 0 || columns == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < array.length) {
      Arrays.fill(array(i), value)
      i += 1
    }
    new DoubleMatrix(array, rows, columns)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an identity matrix.
   *
   * An identity matrix is square. It has every value equal to zero, except those
   * on the primary diagonal, which are one.
   *
   * @param size  the size of the matrix
   * @return an identity matrix of the specified size
   */
  def identity(size: Int): DoubleMatrix = {
    if (size == 0) {
      return EMPTY
    }
    val array = Array.ofDim[Double](size, size)
    var i = 0
    while (i < size) {
      array(i)(i) = 1d
      i += 1
    }
    new DoubleMatrix(array, size, size)
  }

  /**
   * Obtains a diagonal matrix from the specified array.
   *
   * A diagonal matrix is square. It only has values on the primary diagonal,
   * and those values are taken from the specified array.
   *
   * @param array  the array to use to create the matrix
   * @return an identity matrix of the specified size
   */
  def diagonal(array: DoubleArray): DoubleMatrix = {
    val size = array.size()
    if (size == 0) {
      return EMPTY
    }
    val data = Array.ofDim[Double](size, size)
    var i = 0
    while (i < size) {
      data(i)(i) = array.get(i)
      i += 1
    }
    new DoubleMatrix(data, size, size)
  }

  // validates the array, sharing the empty array when there are no rows
  private def validatedArray(array: Array[Array[Double]]): Array[Array[Double]] = {
    ArgChecker.notNull(array, "array")
    if (array.length == 0) EMPTY.array else array
  }

  // depp clone a double[][]
  private def deepClone(input: Array[Array[Double]], rows: Int, columns: Int): Array[Array[Double]] = {
    val cloned = Array.ofDim[Double](rows, columns)
    var i = 0
    while (i < rows) {
      cloned(i) = input(i).clone()
      i += 1
    }
    cloned
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `DoubleMatrix`.
   * @return the meta-bean, not null
   */
  def meta(): DoubleMatrix.Meta = {
    DoubleMatrix.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `DoubleMatrix`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `array` property.
     */
    private val arrayProperty: MetaProperty[Array[Array[Double]]] = DirectMetaProperty.ofImmutable(
        this, "array", classOf[DoubleMatrix], classOf[Array[Array[Double]]])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "array")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 93090393 => // array
          arrayProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: DoubleMatrix] = {
      new DoubleMatrix.Builder()
    }

    override def beanType(): Class[? <: DoubleMatrix] = {
      classOf[DoubleMatrix]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `array` property.
     * @return the meta-property, not null
     */
    def array(): MetaProperty[Array[Array[Double]]] = {
      arrayProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 93090393 => // array
          bean.asInstanceOf[DoubleMatrix].array
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
    private[array] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `DoubleMatrix`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[DoubleMatrix] {

    private var array: Array[Array[Double]] = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 93090393 => // array
          array
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 93090393 => // array
          this.array = newValue.asInstanceOf[Array[Array[Double]]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): DoubleMatrix = {
      new DoubleMatrix(
          array)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(64)
      buf.append("DoubleMatrix.Builder{")
      buf.append("array").append('=').append(JodaBeanUtils.toString(array))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
