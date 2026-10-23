/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

/**
 * An unchecked reflection exception.
 *
 * This is used by `Unchecked` to wrap instances of `ReflectiveOperationException`.
 *
 * @constructor Creates an instance that wraps the underlying exception.
 * @param ex  the underlying exception, null tolerant
 */
@SerialVersionUID(1L)
final class UncheckedReflectiveOperationException(ex: ReflectiveOperationException) extends RuntimeException(ex) {

}
