/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.Reader
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional

import scala.annotation.static
import scala.annotation.targetName
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.PropertyStyle
import org.joda.beans.impl.BasicImmutableBeanBuilder
import org.joda.beans.impl.BasicMetaBean
import org.joda.beans.impl.BasicMetaProperty

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker

/**
 * Utilities that allow code to use the Unicode Byte Order Mark.
 *
 * A Unicode file may contain a Byte Order Mark (BOM) that specifies which
 * encoding is used. Sadly, neither the JDK nor Guava handle this properly.
 *
 * This class supports the BOM for UTF-8, UTF-16LE and UTF-16BE.
 * The UTF-32 formats are rarely seen and cannot be easily determined as
 * the UTF-32 BOMs are similar to the UTF-16 BOMs.
 *
 * @constructor Restricted constructor.
 */
final class UnicodeBom private ()

object UnicodeBom {

  private final val X_FE: Byte = 0xFE.toByte
  private final val X_EF: Byte = 0xEF.toByte
  private final val X_FF: Byte = 0xFF.toByte
  private final val X_BF: Byte = 0xBF.toByte
  private final val X_BB: Byte = 0xBB.toByte

  //-------------------------------------------------------------------------
  /**
   * Converts a `byte[]` to a `String`.
   *
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   *
   * @param input  the input byte array
   * @return the equivalent string
   */
  // the JVM name is toString, which Scala code cannot use as it clashes with Object.toString()
  @static
  @targetName("toString")
  def bytesToString(input: Array[Byte]): String = {
    if (input.length >= 3 && input(0) == X_EF && input(1) == X_BB && input(2) == X_BF) {
      new String(input, 3, input.length - 3, StandardCharsets.UTF_8)

    } else if (input.length >= 2 && input(0) == X_FE && input(1) == X_FF) {
      new String(input, 2, input.length - 2, StandardCharsets.UTF_16BE)

    } else if (input.length >= 2 && input(0) == X_FF && input(1) == X_FE) {
      new String(input, 2, input.length - 2, StandardCharsets.UTF_16LE)

    } else {
      new String(input, StandardCharsets.UTF_8)
    }
  }

  /**
   * Converts a `ByteSource` to a `CharSource`.
   *
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   *
   * @param byteSource  the byte source
   * @return the char source, that uses the BOM to determine the encoding
   */
  def toCharSource(byteSource: ByteSource): CharSource = {
    new CharSource() {

      @throws[IOException]
      override def openStream(): Reader = {
        toReader(byteSource.openStream())
      }

      override def toString(): String = {
        "UnicodeBom.toCharSource(" + byteSource.toString() + ")"
      }
    }
  }

  /**
   * Converts a `BeanByteSource` to a `BeanCharSource`.
   *
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   *
   * @param byteSource  the byte source
   * @return the char source, that uses the BOM to determine the encoding
   */
  def toCharSource(byteSource: BeanByteSource): BeanCharSource = {
    new UnicodeBomCharSource(byteSource)
  }

  /**
   * Converts an `InputStream` to a `Reader`.
   *
   * This ensures that any Unicode byte order marker is used correctly.
   * The default encoding is UTF-8 if no BOM is found.
   *
   * @param inputStream  the input stream to wrap
   * @return the reader, that uses the BOM to determine the encoding
   * @throws java.io.IOException if an IO error occurs
   */
  @throws[IOException]
  def toReader(inputStream: InputStream): Reader = {
    new BomReader(inputStream)
  }

  //-------------------------------------------------------------------------
  /**
   * Reader that manages the BOM.
   */
  private final class BomReader @throws[IOException]() (inputStream: InputStream) extends Reader(inputStream) {

    private val underlying: InputStreamReader = {
      var encoding: Charset = null
      val bom = new Array[Byte](BomReader.MAX_BOM_SIZE)

      // read first 3 bytes such that they can be pushed back later
      val pushbackStream = new PushbackInputStream(inputStream, BomReader.MAX_BOM_SIZE)
      val bytesRead = ByteStreams.read(pushbackStream, bom, 0, 3)

      // look for BOM and adapt, defauling to UTF-8
      if (bytesRead >= 3 && bom(0) == X_EF && bom(1) == X_BB && bom(2) == X_BF) {
        encoding = StandardCharsets.UTF_8
        pushbackStream.unread(bom, 3, (bytesRead - 3))

      } else if (bytesRead >= 2 && bom(0) == X_FE && bom(1) == X_FF) {
        encoding = StandardCharsets.UTF_16BE
        pushbackStream.unread(bom, 2, (bytesRead - 2))

      } else if (bytesRead >= 2 && bom(0) == X_FF && bom(1) == X_FE) {
        encoding = StandardCharsets.UTF_16LE
        pushbackStream.unread(bom, 2, (bytesRead - 2))

      } else {
        encoding = StandardCharsets.UTF_8
        pushbackStream.unread(bom, 0, bytesRead)
      }

      // use Java standard code now we know the encoding
      new InputStreamReader(pushbackStream, encoding)
    }

    @throws[IOException]
    override def read(target: CharBuffer): Int = {
      underlying.read(target)
    }

    @throws[IOException]
    override def read(): Int = {
      underlying.read()
    }

    @throws[IOException]
    override def read(cbuf: Array[Char]): Int = {
      underlying.read(cbuf)
    }

    @throws[IOException]
    override def read(cbuf: Array[Char], off: Int, len: Int): Int = {
      underlying.read(cbuf, off, len)
    }

    @throws[IOException]
    override def skip(n: Long): Long = {
      underlying.skip(n)
    }

    @throws[IOException]
    override def ready(): Boolean = {
      underlying.ready()
    }

    override def markSupported(): Boolean = {
      underlying.markSupported()
    }

    @throws[IOException]
    override def mark(readAheadLimit: Int): Unit = {
      underlying.mark(readAheadLimit)
    }

    @throws[IOException]
    override def reset(): Unit = {
      underlying.reset()
    }

    @throws[IOException]
    override def close(): Unit = {
      underlying.close()
    }
  }

  private object BomReader {
    private final val MAX_BOM_SIZE = 4
  }

  //-------------------------------------------------------------------------
  // a char source that decorates a byte source with Unicode BOM processing
  private[io] class UnicodeBomCharSource private[io] (rawUnderlying: BeanByteSource) extends BeanCharSource {

    private val underlying: BeanByteSource = ArgChecker.notNull(rawUnderlying, "underlying")

    override def metaBean(): MetaBean = {
      UnicodeBomCharSource.Meta.META
    }

    override def getFileName(): Optional[String] = {
      underlying.getFileName()
    }

    @throws[IOException]
    override def openStream(): Reader = {
      toReader(underlying.openStream())
    }

    // accessor used by the meta-bean
    private[io] def underlyingSource(): BeanByteSource = underlying

    override def equals(obj: Any): Boolean = {
      if (obj.asInstanceOf[AnyRef] eq this) {
        true
      } else if (obj != null && (obj.getClass() eq this.getClass())) {
        val other = obj.asInstanceOf[UnicodeBomCharSource]
        JodaBeanUtils.equal(underlying, other.underlying)
      } else {
        false
      }
    }

    override def hashCode(): Int = {
      var hash = getClass().hashCode()
      hash = hash * 31 + JodaBeanUtils.hashCode(underlying)
      hash
    }

    override def toString(): String = {
      "UnicodeBom.toCharSource(" + underlying.toString() + ")"
    }
  }

  private[io] object UnicodeBomCharSource {

    @static private val REGISTERED: Boolean = {
      MetaBean.register(Meta.META)
      true
    }

    //-------------------------------------------------------------------------
    /**
     * Meta bean.
     */
    private[io] final class Meta private () extends BasicMetaBean {

      override def isBuildable(): Boolean = {
        true
      }

      override def builder(): BeanBuilder[UnicodeBomCharSource] = {
        new BasicImmutableBeanBuilder[UnicodeBomCharSource](this) {
          private var underlying: BeanByteSource = uninitialized

          override def get(propertyName: String): Object = {
            if (propertyName.equals(Meta.UNDERLYING.name())) {
              underlying
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName)
            }
          }

          override def set(propertyName: String, value: Object): BeanBuilder[UnicodeBomCharSource] = {
            if (propertyName.equals(Meta.UNDERLYING.name())) {
              this.underlying = ArgChecker.notNull(value, "underlying").asInstanceOf[BeanByteSource]
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName)
            }
            this
          }

          override def build(): UnicodeBomCharSource = {
            ArgChecker.notNull(underlying, "underlying")
            new UnicodeBomCharSource(underlying)
          }
        }
      }

      override def beanType(): Class[? <: Bean] = {
        classOf[UnicodeBomCharSource]
      }

      override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
        Meta.MAP
      }
    }

    private[io] object Meta {

      private[io] val META: MetaBean = new Meta()

      private[io] val UNDERLYING: MetaProperty[BeanByteSource] = new BasicMetaProperty[BeanByteSource]("underlying") {

        override def metaBean(): MetaBean = {
          META
        }

        override def declaringType(): Class[?] = {
          classOf[UnicodeBomCharSource]
        }

        override def propertyType(): Class[BeanByteSource] = {
          classOf[BeanByteSource]
        }

        override def propertyGenericType(): Type = {
          classOf[BeanByteSource]
        }

        override def style(): PropertyStyle = {
          PropertyStyle.IMMUTABLE
        }

        override def annotations(): List[Annotation] = {
          ImmutableList.of[Annotation]()
        }

        override def get(bean: Bean): BeanByteSource = {
          bean.asInstanceOf[UnicodeBomCharSource].underlyingSource()
        }

        override def set(bean: Bean, value: Object): Unit = {
          throw new UnsupportedOperationException("Property cannot be written: " + name())
        }
      }

      private[io] val MAP: ImmutableMap[String, MetaProperty[?]] =
        ImmutableMap.of[String, MetaProperty[?]]("underlying", UNDERLYING)
    }
  }

}
