/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.UncheckedIOException
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional

import scala.annotation.static
import scala.annotation.targetName
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.PropertyStyle
import org.joda.beans.impl.BasicImmutableBeanBuilder
import org.joda.beans.impl.BasicMetaBean
import org.joda.beans.impl.BasicMetaProperty

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker

/**
 * A byte source implementation that is also a Joda-Bean.
 *
 * See `ArrayByteSource`, `UriByteSource` and `FileByteSource`.
 *
 * @constructor Creates an instance.
 */
abstract class BeanByteSource protected () extends ByteSource with ImmutableBean {

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
    getFileName().orElseThrow(() => new IllegalArgumentException("No file name present on byte source"))
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the byte source is empty, throwing an unchecked exception.
   *
   * This overrides `ByteSource` to throw `UncheckedIOException` instead of `IOException`.
   *
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  override def isEmpty(): Boolean = {
    try {
      super.isEmpty()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  /**
   * Gets the size of the byte source, throwing an unchecked exception.
   *
   * This overrides `ByteSource` to throw `UncheckedIOException` instead of `IOException`.
   *
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  override def size(): Long = {
    try {
      super.size()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Reads the source as a byte array, throwing an unchecked exception.
   *
   * This overrides `ByteSource` to throw `UncheckedIOException` instead of `IOException`.
   *
   * @return the byte array
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  override def read(): Array[Byte] = {
    try {
      super.read()
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  /**
   * Reads the source, converting to UTF-8.
   *
   * @return the UTF-8 string
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def readUtf8(): String = {
    new String(read(), StandardCharsets.UTF_8)
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   *
   * @return the UTF-8 string
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def readUtf8UsingBom(): String = {
    UnicodeBom.bytesToString(read())
  }

  override def asCharSource(charset: Charset): BeanCharSource = {
    // no need to bridge, as scalac is already doing that
    new BeanByteSource.AsBeanCharSource(this, charset)
  }

  /**
   * Returns a `CharSource` for the same bytes, converted to UTF-8.
   *
   * @return the equivalent `CharSource`
   */
  def asCharSourceUtf8(): BeanCharSource = {
    // bridged below for backwards compatibility
    asCharSource(StandardCharsets.UTF_8)
  }

  // converted to a bridge method named asCharSourceUtf8 by the bridger plugin
  @targetName("asCharSourceUtf8$$bridge")
  def asCharSourceUtf8Bridge(): CharSource = { // CSIGNORE
    asCharSourceUtf8()
  }

  /**
   * Returns a `CharSource` for the File, converted to UTF-8 using a Byte-Order Mark if available.
   *
   * @return the equivalent `CharSource`
   */
  def asCharSourceUtf8UsingBom(): BeanCharSource = {
    // bridged below for backwards compatibility
    UnicodeBom.toCharSource(this)
  }

  // converted to a bridge method named asCharSourceUtf8UsingBom by the bridger plugin
  @targetName("asCharSourceUtf8UsingBom$$bridge")
  def asCharSourceUtf8UsingBomBridge(): CharSource = { // CSIGNORE
    UnicodeBom.toCharSource(this: ByteSource)
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the content of the byte source into memory.
   *
   * @return the byte array
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def load(): ArrayByteSource = {
    ArrayByteSource.from(this)
  }

  //-------------------------------------------------------------------------
  override def hash(hashFunction: HashFunction): HashCode = {
    try {
      super.hash(hashFunction)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  /**
   * Returns a new byte source containing the hash of the content of this byte source.
   *
   * The returned hash is in byte form.
   *
   * @param hashFunction  the hash function to use, see `Hashing`
   * @return the new byte source representing the hash
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def toHash(hashFunction: HashFunction): ArrayByteSource = {
    ArrayByteSource.ofUnsafe(hash(hashFunction).asBytes())
  }

  /**
   * Returns a new byte source containing the hash of the content of this byte source.
   *
   * The returned hash is in string form.
   * This form is intended to be compatible with tools like the UNIX `md5sum` command.
   *
   * @param hashFunction  the hash function to use, see `Hashing`
   * @return the new byte source representing the hash
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def toHashString(hashFunction: HashFunction): String = {
    hash(hashFunction).toString()
  }

  /**
   * Encodes the byte source using base-64.
   *
   * @return the base-64 encoded form
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def toBase64(): ArrayByteSource = {
    val baos = new ByteArrayOutputStream()
    try {
      closing(openBufferedStream()) { in =>
        closing(Base64.getEncoder().wrap(baos)) { out =>
          ByteStreams.copy(in, out)
        }
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
    ArrayByteSource.ofUnsafe(baos.toByteArray())
  }

  /**
   * Encodes the byte source using base-64, returning a string.
   *
   * Equivalent to `toBase64().readUtf8()`.
   *
   * @return the base-64 encoded string
   */
  def toBase64String(): String = {
    toBase64().readUtf8()
  }

  // equivalent of try-with-resources, adding any close failure as suppressed to the primary failure
  private def closing[R <: AutoCloseable, A](resource: R)(body: R => A): A = {
    var primary: Throwable = null
    try {
      body(resource)
    } catch {
      case ex: Throwable =>
        primary = ex
        throw ex
    } finally {
      if (resource != null) {
        if (primary != null) {
          try {
            resource.close()
          } catch {
            case closeEx: Throwable => primary.addSuppressed(closeEx)
          }
        } else {
          resource.close()
        }
      }
    }
  }

}

object BeanByteSource {

  //-------------------------------------------------------------------------
  // a char source that decorates this byte source
  private[io] class AsBeanCharSource private[io] (
      rawUnderlying: BeanByteSource,
      rawCharset: Charset)
    extends BeanCharSource {

    private val underlying: BeanByteSource = ArgChecker.notNull(rawUnderlying, "underlying")
    private val charset: Charset = ArgChecker.notNull(rawCharset, "charset")

    override def metaBean(): MetaBean = {
      AsBeanCharSource.Meta.META
    }

    override def getFileName(): Optional[String] = {
      underlying.getFileName()
    }

    override def asByteSource(charset: Charset): BeanByteSource = {
      if (charset.equals(this.charset)) {
        underlying
      } else {
        super.asByteSource(charset)
      }
    }

    @throws[IOException]
    override def openStream(): Reader = {
      new InputStreamReader(underlying.openStream(), charset)
    }

    override def read(): String = {
      new String(underlying.read(), charset)
    }

    // accessors used by the meta-bean
    private[io] def underlyingSource(): BeanByteSource = underlying

    private[io] def charsetName(): String = charset.name()

    override def equals(obj: Any): Boolean = {
      if (obj.asInstanceOf[AnyRef] eq this) {
        true
      } else if (obj != null && (obj.getClass() eq this.getClass())) {
        val other = obj.asInstanceOf[AsBeanCharSource]
        JodaBeanUtils.equal(underlying, other.underlying) &&
            JodaBeanUtils.equal(charset, other.charset)
      } else {
        false
      }
    }

    override def hashCode(): Int = {
      var hash = getClass().hashCode()
      hash = hash * 31 + JodaBeanUtils.hashCode(underlying)
      hash = hash * 31 + JodaBeanUtils.hashCode(charset)
      hash
    }

    override def toString(): String = {
      underlying.toString() + ".asCharSource(" + charset + ")"
    }
  }

  private[io] object AsBeanCharSource {

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

      override def builder(): BeanBuilder[AsBeanCharSource] = {
        new BasicImmutableBeanBuilder[AsBeanCharSource](this) {
          private var underlying: BeanByteSource = uninitialized
          private var charset: Charset = uninitialized

          override def get(propertyName: String): Object = {
            if (propertyName.equals(Meta.UNDERLYING.name())) {
              underlying
            } else if (propertyName.equals(Meta.CHARSET.name())) {
              charset.name()
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName)
            }
          }

          override def set(propertyName: String, value: Object): BeanBuilder[AsBeanCharSource] = {
            if (propertyName.equals(Meta.UNDERLYING.name())) {
              this.underlying = ArgChecker.notNull(value, "underlying").asInstanceOf[BeanByteSource]
            } else if (propertyName.equals(Meta.CHARSET.name())) {
              this.charset = Charset.forName(ArgChecker.notNull(value, "charset").asInstanceOf[String])
            } else {
              throw new NoSuchElementException("Unknown property: " + propertyName)
            }
            this
          }

          override def build(): AsBeanCharSource = {
            ArgChecker.notNull(underlying, "underlying")
            ArgChecker.notNull(charset, "charset")
            new AsBeanCharSource(underlying, charset)
          }
        }
      }

      override def beanType(): Class[? <: Bean] = {
        classOf[AsBeanCharSource]
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
          classOf[AsBeanCharSource]
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
          bean.asInstanceOf[AsBeanCharSource].underlyingSource()
        }

        override def set(bean: Bean, value: Object): Unit = {
          throw new UnsupportedOperationException("Property cannot be written: " + name())
        }
      }

      private[io] val CHARSET: MetaProperty[String] = new BasicMetaProperty[String]("charset") {

        override def metaBean(): MetaBean = {
          META
        }

        override def declaringType(): Class[?] = {
          classOf[AsBeanCharSource]
        }

        override def propertyType(): Class[String] = {
          classOf[String]
        }

        override def propertyGenericType(): Type = {
          classOf[String]
        }

        override def style(): PropertyStyle = {
          PropertyStyle.IMMUTABLE
        }

        override def annotations(): List[Annotation] = {
          ImmutableList.of[Annotation]()
        }

        override def get(bean: Bean): String = {
          bean.asInstanceOf[AsBeanCharSource].charsetName()
        }

        override def set(bean: Bean, value: Object): Unit = {
          throw new UnsupportedOperationException("Property cannot be written: " + name())
        }
      }

      private[io] val MAP: ImmutableMap[String, MetaProperty[?]] =
        ImmutableMap.of[String, MetaProperty[?]]("underlying", UNDERLYING, "charset", CHARSET)
    }
  }

}
