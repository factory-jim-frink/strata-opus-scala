/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function

/**
 * A checked version of `UnaryOperator`.
 *
 * This is intended to be used with `Unchecked`.
 *
 * @tparam T the type of the object parameter
 */
@FunctionalInterface
trait CheckedUnaryOperator[T] extends CheckedFunction[T, T] {

}
