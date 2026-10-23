/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import com.opengamma.strata.collect.io.PropertiesFile
import com.opengamma.strata.collect.io.ResourceLocator

/**
 * Provides access to the version of Strata.
 */
final class Version private ()

object Version {

  /**
   * The version, which will be populated by the Maven build.
   */
  private val VERSION: String =
    PropertiesFile.of(ResourceLocator.ofClasspath(classOf[Version], "version.properties").getCharSource())
        .getProperties().value("version")

  //-------------------------------------------------------------------------
  /**
   * Gets the version of Strata.
   *
   * @return the version
   */
  def getVersionString(): String = {
    VERSION
  }

}
