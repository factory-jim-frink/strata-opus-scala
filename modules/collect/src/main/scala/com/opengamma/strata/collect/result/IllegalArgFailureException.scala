/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import scala.annotation.varargs

import com.opengamma.strata.collect.ArgChecker

/**
 * Exception thrown when input is invalid.
 *
 * @constructor Returns an exception wrapping the failure item.
 *
 * The failure item should have a reason of 'Invalid'.
 *
 * @param item  the failure item
 */
@SerialVersionUID(1L)
class IllegalArgFailureException(
    /** The failure item. */
    item: FailureItem)
  extends IllegalArgumentException(item.getMessage()) with FailureItemProvider {

  ArgChecker.notNull(item, "item")

  /**
   * Returns an exception from a message.
   *
   * The message is produced using a template that contains zero to many "{argName}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.formatWithAttributes(String, Object...)` for more details.
   *
   * @param messageTemplate  a message explaining the failure, not empty, uses "{argName}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   */
  @varargs
  def this(messageTemplate: String, messageArgs: Object*) = {
    this(FailureItem.of(FailureReason.INVALID, messageTemplate, messageArgs*))
  }

  /**
   * Returns an exception from a cause and message.
   *
   * The message is produced using a template that contains zero to many "{argName}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.formatWithAttributes(String, Object...)` for more details.
   *
   * @param cause  the cause
   * @param messageTemplate  a message explaining the failure, not empty, uses "{argName}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   */
  @varargs
  def this(cause: Throwable, messageTemplate: String, messageArgs: Object*) = {
    this(FailureItem.of(FailureReason.INVALID, cause, messageTemplate, messageArgs*))
    initCause(cause)
  }

  /**
   * Gets the failure item.
   *
   * @return the failure item
   */
  override def getFailureItem(): FailureItem = {
    item
  }

}
