/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule

import java.util.Optional

import scala.annotation.varargs

import com.opengamma.strata.collect.Messages

/**
 * Exception thrown when a schedule cannot be calculated.
 */
@SerialVersionUID(1L)
final class ScheduleException private (
    /**
     * The invalid schedule definition.
     */
    private val definition: PeriodicSchedule,
    message: String,
    formatted: Boolean)
  extends IllegalArgumentException(message) {

  /**
   * Creates an instance.
   *
   * The message is formatted using `Messages.format(String, Object...)`.
   * Message formatting is null tolerant to avoid hiding this exception.
   * 
   * @param msgTemplate  the message template, null tolerant
   * @param msgArguments  the message arguments, null tolerant
   */
  @varargs
  def this(msgTemplate: String, msgArguments: Object*) = {
    this(null, Messages.format(msgTemplate, msgArguments*), true)
  }

  /**
   * Creates an instance, specifying the definition that caused the problem.
   *
   * The message is formatted using `Messages.format(String, Object...)`.
   * Message formatting is null tolerant to avoid hiding this exception.
   * 
   * @param definition  the invalid schedule definition, null tolerant
   * @param msgTemplate  the message template, null tolerant
   * @param msgArguments  the message arguments, null tolerant
   */
  @varargs
  def this(definition: PeriodicSchedule, msgTemplate: String, msgArguments: Object*) = {
    this(definition, Messages.format(msgTemplate, msgArguments*), true)
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the invalid schedule definition.
   * 
   * @return the optional definition
   */
  def getDefinition(): Optional[PeriodicSchedule] = {
    Optional.ofNullable(definition)
  }

}
