/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.function.Consumer
import java.util.stream.Stream

import org.joda.beans.ImmutableBean

import com.google.common.collect.ImmutableList
import com.google.common.io.CharSource
import com.google.common.io.LineProcessor

/**
 * A char source implementation that is also a Joda-Bean.
 *
 * See `StringCharSource`.
 *
 * Various methods override `CharSource` to throw `UncheckedIOException` instead of `IOException`.
 *
 * @constructor Creates an instance.
 */
abstract class BeanCharSource protected () extends CharSource with ImmutableBean {

  //-------------------------------------------------------------------------
  /**
   * Gets the file name of the source.
   *
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   *
   * @return the file name, empty if not known
   */
  def getFileName(): Optional[String] = {
    Optional.empty()
  }

  /**
   * Gets the file name of the source.
   *
   * Most sources originate from a file-based location.
   * This is captured and returned here where available.
   *
   * @return the file name
   * @throws java.lang.IllegalArgumentException if the file name is not known
   */
  def getFileNameOrThrow(): String = {
    getFileName().orElseThrow(() => new IllegalArgumentException("No file name present on char source"))
  }

  //-------------------------------------------------------------------------
  override def lines(): Stream[String] = {
    try {
      super.lines()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def isEmpty(): Boolean = {
    try {
      super.isEmpty()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def length(): Long = {
    try {
      super.length()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def read(): String = {
    try {
      super.read()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def readFirstLine(): String = {
    try {
      super.readFirstLine()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def readLines(): ImmutableList[String] = {
    try {
      super.readLines()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def readLines[T](processor: LineProcessor[T]): T = {
    try {
      super.readLines(processor)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  override def forEachLine(action: Consumer[? >: String]): Unit = {
    try {
      super.forEachLine(action)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the content of the source into memory.
   *
   * @return the string-based source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def load(): StringCharSource = {
    StringCharSource.from(this)
  }

  /**
   * Converts this char source to a byte source in UTF-8.
   *
   * @return the equivalent byte source
   */
  def asByteSourceUtf8(): BeanByteSource = {
    asByteSource(StandardCharsets.UTF_8)
  }

  override def asByteSource(charset: Charset): BeanByteSource = {
    ArrayByteSource.from(super.asByteSource(charset))
  }

}
