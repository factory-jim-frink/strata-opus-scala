/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array

/**
 * Base interface for all matrix types.
 *
 * An n-dimensional matrix of elements of a fixed size.
 * A 1-dimensional matrix is typically known as an array.
 */
trait Matrix {

  /**
   * Gets the number of dimensions of the matrix.
   *
   * Each matrix type has a fixed number of dimensions, returned by this method.
   *
   * @return the dimensions of the matrix
   */
  def dimensions(): Int

  /**
   * Gets the size of the matrix.
   *
   * This is the total number of elements in the matrix.
   *
   * @return the size of the matrix
   */
  def size(): Int

}
