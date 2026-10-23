/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

/**
 * Exception thrown if reference data cannot be found.
 *
 * Creates the exception passing the exception message.
 *
 * @param message  the exception message, null tolerant
 */
@SerialVersionUID(1L)
class ReferenceDataNotFoundException(message: String) extends RuntimeException(message) {

}
