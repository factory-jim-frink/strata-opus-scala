/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UncheckedIOException
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedFunction
import com.opengamma.strata.collect.named.EnumNames
import com.opengamma.strata.collect.named.NamedEnum

/**
 * Encodes and decodes common data formats.
 */
enum ByteSourceCodec(
    encoder: CheckedFunction[OutputStream, OutputStream],
    decoder: CheckedFunction[InputStream, InputStream],
    suffix: String)
  extends java.lang.Enum[ByteSourceCodec] with NamedEnum {

  /**
   * Encode base-64.
   */
  case BASE64 extends ByteSourceCodec(
      (base: OutputStream) => Base64.getEncoder().wrap(base),
      (base: InputStream) => Base64.getDecoder().wrap(base),
      ".base64")
  /**
   * Encode using gz.
   */
  case GZ extends ByteSourceCodec(
      (base: OutputStream) => new GZIPOutputStream(base),
      (base: InputStream) => new GZIPInputStream(base),
      ".gz")
  /**
   * Encode using gz then base-64.
   */
  case GZ_BASE64 extends ByteSourceCodec(
      (base: OutputStream) => new GZIPOutputStream(Base64.getEncoder().wrap(base)),  // CSIGNORE
      (base: InputStream) => new GZIPInputStream(Base64.getDecoder().wrap(base)),  // CSIGNORE
      ".gz.base64")

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   *
   * @return the formatted string representing the type
   */
  @ToString
  override def toString(): String = {
    ByteSourceCodec.NAMES.format(this)
  }

  //-------------------------------------------------------------------------
  // encodes the bytes to a source
  private[io] def encode(bytes: Array[Byte], fileName: String): ArrayByteSource = {
    val baos = encode(bytes)
    new ArrayByteSource(baos.toByteArray(), if (fileName == null) null else fileName + suffix)
  }

  // encodes the bytes
  private def encode(bytes: Array[Byte]): ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream(1024)
    try {
      // equivalent of try-with-resources
      val out: OutputStream = encoder.apply(baos)
      var primary: Throwable = null
      try {
        out.write(bytes)
      } catch {
        case ex: Throwable =>
          primary = ex
          throw ex
      } finally {
        if (out != null) {
          if (primary != null) {
            try {
              out.close()
            } catch {
              case closeEx: Throwable => primary.addSuppressed(closeEx)
            }
          } else {
            out.close()
          }
        }
      }
    } catch {
      case ex: Throwable => throw Unchecked.propagate(ex)
    }
    baos
  }

  // decodes the input stream
  private[io] def decode(bytes: Array[Byte], fileName: String): ArrayByteSource = {
    try {
      val decoded = ArrayByteSource.from(decode(bytes))
      if (fileName == null) {
        decoded
      } else {
        decoded.withFileName(
            if (fileName.endsWith(suffix)) fileName.substring(0, fileName.length() - suffix.length()) else fileName)
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  // decodes the input stream
  @throws[IOException]
  private def decode(bytes: Array[Byte]): InputStream = {
    try {
      decoder.apply(new ByteArrayInputStream(bytes))
    } catch {
      case ex: Throwable => throw Unchecked.propagate(ex)
    }
  }

}

object ByteSourceCodec {

  // helper for name conversions
  private lazy val NAMES: EnumNames[ByteSourceCodec] = EnumNames.of(classOf[ByteSourceCodec])

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   *
   * Parsing handles the mixed case form produced by `toString()` and
   * the upper and lower case variants of the enum constant name.
   *
   * @param name  the name to parse
   * @return the type
   * @throws java.lang.IllegalArgumentException if the name is not known
   */
  // @static because the inherited static Named.of suppresses the static forwarder
  @FromString
  @static def of(name: String): ByteSourceCodec = {
    NAMES.parse(name)
  }

}
