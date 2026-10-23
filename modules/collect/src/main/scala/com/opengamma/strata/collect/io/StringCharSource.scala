/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.Reader
import java.io.Serializable
import java.io.StringReader
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional

import scala.annotation.static
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

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.CharSink
import com.google.common.io.CharSource
import com.google.common.io.CharStreams
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A char source implementation that explicitly wraps a `String`.
 *
 * This implementation allows `IOException` to be avoided in many cases,
 * and to be able to create and retrieve the internal array unsafely.
 */
@SerialVersionUID(1L)
final class StringCharSource private[io] (
    /**
     * The string.
     */
    private val value: String,
    rawFileName: String)
  extends BeanCharSource with ImmutableBean with Serializable {

  /**
   * The file name, null if not known.
   */
  private val fileName: String = Strings.emptyToNull(rawFileName)

  //-------------------------------------------------------------------------
  // creates an instance
  private def this(value: String) = {
    this(value, null)
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    StringCharSource.Meta.META
  }

  override def getFileName(): Optional[String] = {
    Optional.ofNullable(fileName)
  }

  /**
   * Returns an instance with the file name updated.
   *
   * If a path is passed in, only the file name is retained.
   *
   * @param fileName the file name, an empty string can be used to remove the file name
   * @return a source with the specified file name
   */
  def withFileName(fileName: String): StringCharSource = {
    ArgChecker.notNull(fileName, "fileName")
    val lastSlash = fileName.lastIndexOf('/')
    new StringCharSource(value, fileName.substring(lastSlash + 1))
  }

  //-------------------------------------------------------------------------
  override def openStream(): Reader = {
    new StringReader(value)
  }

  override def isEmpty(): Boolean = {
    value.length() == 0
  }

  /**
   * Gets the length, which is always known.
   *
   * @return the length, which is always known
   */
  override def lengthIfKnown(): com.google.common.base.Optional[java.lang.Long] = {
    com.google.common.base.Optional.of(java.lang.Long.valueOf(length()))
  }

  override def length(): Long = {
    value.length().toLong
  }

  @throws[IOException]
  override def copyTo(appendable: Appendable): Long = {
    appendable.append(value)
    value.length().toLong
  }

  @throws[IOException]
  override def copyTo(sink: CharSink): Long = {
    sink.write(value)
    value.length().toLong
  }

  override def read(): String = {
    value
  }

  override def load(): StringCharSource = {
    this
  }

  //-------------------------------------------------------------------------
  override def asByteSourceUtf8(): ArrayByteSource = {
    asByteSource(StandardCharsets.UTF_8)
  }

  override def asByteSource(charset: Charset): ArrayByteSource = {
    new ArrayByteSource(value.getBytes(charset), fileName)
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[StringCharSource]
      JodaBeanUtils.equal(fileName, other.fileName) &&
          JodaBeanUtils.equal(value, other.value)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(fileName)
    hash = hash * 31 + JodaBeanUtils.hashCode(value)
    hash
  }

  override def toString(): String = {
    "StringCharSource[" + length() + " chars" + (if (fileName != null) ", " + fileName else "") + "]"
  }

}

object StringCharSource {

  /**
   * An empty source.
   */
  @static final val EMPTY: StringCharSource = new StringCharSource("")

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Meta.META)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   *
   * @param str  the string
   * @return the char source
   */
  def of(str: String): StringCharSource = {
    new StringCharSource(str)
  }

  /**
   * Obtains an instance from a UTF-8 byte array.
   *
   * @param bytes  the UTF-8 bytes
   * @return the char source
   */
  def fromBytesUtf8(bytes: Array[Byte]): StringCharSource = {
    fromBytes(bytes, StandardCharsets.UTF_8)
  }

  /**
   * Obtains an instance from a byte array.
   *
   * @param bytes  the bytes
   * @param charset  the encoding
   * @return the char source
   */
  def fromBytes(bytes: Array[Byte], charset: Charset): StringCharSource = {
    new StringCharSource(new String(bytes, charset))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from another char source.
   *
   * @param other  the other char source
   * @return the char source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def from(other: CharSource): StringCharSource = {
    other match {
      case stringSource: StringCharSource =>
        stringSource
      case beanSource: BeanCharSource =>
        readFrom(other, beanSource.getFileName().orElse(null))
      case _ =>
        // handle all other char sources
        val str = other.toString()
        if (str.equals("CharSource.empty()")) {
          EMPTY
        } else {
          var fileName: String = null
          if (str.startsWith("Files.asByteSource(")) {
            // extract the file name from toString()
            val pos = str.indexOf(')', 19)
            fileName = Paths.get(str.substring(19, pos)).getFileName().toString()
          } else if (str.startsWith("MoreFiles.asByteSource(")) {
            // extract the path name from toString()
            val pos = str.indexOf(',', 23)
            fileName = Paths.get(str.substring(23, pos)).getFileName().toString()
          } else if (str.startsWith("Resources.asByteSource(")) {
            // extract the URI from toString()
            val pos = str.indexOf(')', 23)
            val path = str.substring(23, pos)
            val lastSlash = path.lastIndexOf('/')
            fileName = path.substring(lastSlash + 1)
          }
          readFrom(other, fileName)
        }
    }
  }

  // reads the other source, wrapping any IOException
  private def readFrom(other: CharSource, fileName: String): StringCharSource = {
    val reader: CheckedSupplier[String] = () => other.read()
    new StringCharSource(Unchecked.wrap(reader), fileName)
  }

  /**
   * Obtains an instance from a `Reader`.
   *
   * This method use the supplier to open the reader, extract the chars and close the reader.
   * It is intended that invoking the supplier opens the reader.
   * It is not intended that an already open reader is supplied.
   *
   * @param readerSupplier  the supplier of the reader
   * @return the char source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def from(readerSupplier: CheckedSupplier[? <: Reader]): StringCharSource = {
    val loader: CheckedSupplier[StringCharSource] = () => {
      // equivalent of try-with-resources
      val in: Reader = readerSupplier.get()
      var primary: Throwable = null
      try {
        from(in: Readable)
      } catch {
        case ex: Throwable =>
          primary = ex
          throw ex
      } finally {
        if (in != null) {
          if (primary != null) {
            try {
              in.close()
            } catch {
              case closeEx: Throwable => primary.addSuppressed(closeEx)
            }
          } else {
            in.close()
          }
        }
      }
    }
    Unchecked.wrap(loader)
  }

  /**
   * Obtains an instance from a `Readable`.
   *
   * This method uses an already open reader, extracting the chars.
   * The stream is not closed - that is the responsibility of the caller.
   *
   * @param reader  the open reader, which will not be closed
   * @return the char source
   * @throws java.io.IOException if an IO error occurs
   */
  @throws[IOException]
  def from(reader: Readable): StringCharSource = {
    val chars = CharStreams.toString(reader)
    new StringCharSource(chars)
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  private[io] final class Meta private () extends BasicMetaBean {

    override def isBuildable(): Boolean = {
      true
    }

    override def builder(): BeanBuilder[StringCharSource] = {
      new BasicImmutableBeanBuilder[StringCharSource](this) {
        private var value: String = uninitialized
        private var fileName: String = uninitialized

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.VALUE.name())) {
            value
          } else if (propertyName.equals(Meta.FILE_NAME.name())) {
            fileName
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[StringCharSource] = {
          if (propertyName.equals(Meta.VALUE.name())) {
            this.value = ArgChecker.notNull(value, "value").asInstanceOf[String]
          } else if (propertyName.equals(Meta.FILE_NAME.name())) {
            this.fileName = value.asInstanceOf[String]
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): StringCharSource = {
          ArgChecker.notNull(value, "value")
          new StringCharSource(value, fileName)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[StringCharSource]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[io] object Meta {

    private[io] val META: MetaBean = new Meta()

    private val VALUE: MetaProperty[String] = new BasicMetaProperty[String]("value") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[StringCharSource]
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
        bean.asInstanceOf[StringCharSource].read()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private val FILE_NAME: MetaProperty[String] = new BasicMetaProperty[String]("fileName") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[StringCharSource]
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
        bean.asInstanceOf[StringCharSource].getFileName().orElse(null)
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("value", VALUE, "fileName", FILE_NAME)
  }

}
