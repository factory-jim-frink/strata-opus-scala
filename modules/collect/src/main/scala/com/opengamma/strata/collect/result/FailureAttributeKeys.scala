/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import scala.annotation.static

/**
 * Common attribute keys for `FailureItem.getAttributes()`.
 *
 * The attribute keys are used to help interpret the failure message.
 */
final class FailureAttributeKeys private ()

object FailureAttributeKeys {

  // generic keys
  /**
   * The attribute for specifying file id.
   */
  @static final val FILE_ID: String = "fileId"
  /**
   * The attribute for specifying the name of the file that caused the error.
   */
  @static final val FILE_NAME: String = "fileName"
  /**
   * The attribute for specifying the file summary of the file that caused the error.
   */
  @static final val FILE_SUMMARY: String = "fileSummary"
  /**
   * The attribute for specifying the format associated with the error.
   */
  @static final val FORMAT: String = "format"  // not fileFormat for legacy reasons
  /**
   * The attribute for specifying the line number in which the error occurred.
   */
  @static final val LINE_NUMBER: String = "lineNumber"
  /**
   * The attribute for the value that caused the failure.
   */
  @static final val VALUE: String = "value"
  /**
   * The attribute for the name of the parameter.
   */
  @static final val PARAM: String = "param"
  /**
   * The attribute for the type that caused the failure.
   */
  @static final val TYPE: String = "type"
  /**
   * The attribute for the options that were valid.
   */
  @static final val OPTIONS: String = "options"
  /**
   * The attribute for specifying the message from a runtime exception.
   */
  @static final val EXCEPTION_MESSAGE: String = "exceptionMessage"
  /**
   * The attribute for decoding the message to extract the template locations.
   */
  @static final val TEMPLATE_LOCATION: String = "templateLocation"

  // business/product keys
  /**
   * The attribute for specifying the LEI associated with the error.
   */
  @static final val LEGAL_ENTITY_ID: String = "legalEntityId"
  /**
   * The attribute for specifying the account associated with the error.
   */
  @static final val ACCOUNT: String = "account"
  /**
   * The attribute for specifying the broker associated with the error.
   */
  @static final val BROKER: String = "broker"
  /**
   * The attribute for specifying the CCP associated with the error.
   */
  @static final val CCP: String = "ccp"
  /**
   * The attribute for specifying the product type associated with the error.
   */
  @static final val PRODUCT_TYPE: String = "productType"

  /**
   * The attribute for specifying the root cause of the error.
   */
  @static final val ROOT_CAUSE: String = "rootCause"

  /**
   * The attribute for specifying the short message.
   */
  @static final val SHORT_MESSAGE: String = "shortMessage"

}
