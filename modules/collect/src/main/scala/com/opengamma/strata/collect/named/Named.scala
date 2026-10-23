/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

import java.lang.reflect.Method

import scala.annotation.static

import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A named instance.
 *
 * This simple interface is used to define objects that can be identified by a unique name.
 * The name contains enough information to be able to recreate the instance.
 *
 * Implementations should provide a static method `of(String)` that allows the
 * instance to be created from the name.
 */
trait Named {

  //-------------------------------------------------------------------------
  /**
   * Gets the unique name of the instance.
   *
   * The name contains enough information to be able to recreate the instance.
   *
   * @return the unique name
   */
  def getName(): String

}

object Named {

  /**
   * Obtains an instance of the specified named type by name.
   *
   * This method operates by reflection.
   * It requires a static method `of(String)` method to be present on the type specified.
   * If the method does not exist an exception is thrown.
   *
   * @tparam T  the named type
   * @param type  the named type with the `of(String)` method
   * @param name  the name to find
   * @return the instance of the named type
   * @throws java.lang.IllegalArgumentException if the specified name could not be found
   */
  @static def of[T <: Named](`type`: Class[T], name: String): T = {
    ofByReflection(`type`, name)
  }

  // lambdas are created outside the static interface method to avoid illegal interface lambda helpers
  private def ofByReflection[T <: Named](`type`: Class[T], name: String): T = {
    val supplier: CheckedSupplier[T] = () => {
      val method: Method = `type`.getMethod("of", classOf[String])
      `type`.cast(method.invoke(null, name))
    }
    Unchecked.wrap(supplier)
  }

}
