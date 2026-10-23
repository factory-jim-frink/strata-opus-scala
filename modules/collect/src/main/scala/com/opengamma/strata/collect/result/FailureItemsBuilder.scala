/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import java.util.List

import com.google.common.collect.ImmutableList

/**
 * A builder for a list of failure items.
 *
 * This provides a builder to create `FailureItems`.
 *
 * @constructor Creates an instance.
 */
final class FailureItemsBuilder private[result] () {

  /**
   * The mutable list of failures.
   */
  private val listBuilder: ImmutableList.Builder[FailureItem] = ImmutableList.builder[FailureItem]()

  //-------------------------------------------------------------------------
  /**
   * Adds a failure to the list.
   *
   * @param failure  the failure to add
   * @return this, for chaining
   */
  def addFailure(failure: FailureItem): FailureItemsBuilder = {
    listBuilder.add(failure)
    this
  }

  /**
   * Adds a list of failures to the list.
   *
   * @param failures  the failures to add
   * @return this, for chaining
   */
  def addAllFailures(failures: List[FailureItem]): FailureItemsBuilder = {
    listBuilder.addAll(failures)
    this
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the resulting instance.
   *
   * @return the result
   */
  def build(): FailureItems = {
    FailureItems.of(listBuilder.build())
  }

}
