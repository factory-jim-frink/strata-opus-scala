/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.ByteArrayInputStream

/**
 * Scala 3 fixture with the bridge shape of the real `ArrayByteSource`: covariant overrides of
 * Guava's `ByteSource` methods, which are declared `throws IOException`.
 * Compiled only into `target/build-test-fixtures` for the `RestoreBridgeExceptions` controls.
 */
final class ArrayByteSource(private val array: Array[Byte]) extends com.google.common.io.ByteSource {
  override def openStream(): ByteArrayInputStream = new ByteArrayInputStream(array)
  override def openBufferedStream(): ByteArrayInputStream = openStream()
  override def size(): Long = array.length.toLong
  def describe(): String = "fixture"
}
