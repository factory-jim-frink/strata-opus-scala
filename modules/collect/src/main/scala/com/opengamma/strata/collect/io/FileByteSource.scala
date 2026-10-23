/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.io.UncheckedIOException
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional

import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.PropertyStyle
import org.joda.beans.impl.BasicImmutableBeanBuilder
import org.joda.beans.impl.BasicMetaBean
import org.joda.beans.impl.BasicMetaProperty

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A byte source implementation that obtains data from a file.
 *
 * This implementation differs from Guava in that it is a Joda-Bean.
 * In addition, `read()` throws `UncheckedIOException` instead of `IOException`.
 */
@SerialVersionUID(1L)
final class FileByteSource private (rawFile: File)
  extends BeanByteSource with ImmutableBean with Serializable {

  /**
   * The underlying file.
   */
  private val file: File = ArgChecker.notNull(rawFile, "file")

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    FileByteSource.Meta.META
  }

  override def getFileName(): Optional[String] = {
    Optional.of(file.getName())
  }

  /**
   * Gets the File.
   *
   * @return the File
   */
  def getFile(): File = {
    file
  }

  //-------------------------------------------------------------------------
  @throws[IOException]
  override def openStream(): InputStream = {
    new FileInputStream(file)
  }

  override def sizeIfKnown(): com.google.common.base.Optional[java.lang.Long] = {
    if (file.isFile()) {
      com.google.common.base.Optional.of(java.lang.Long.valueOf(file.length()))
    } else {
      com.google.common.base.Optional.absent[java.lang.Long]()
    }
  }

  override def size(): Long = {
    if (!file.isFile()) {
      throw new UncheckedIOException(new FileNotFoundException(file.toString()))
    }
    file.length()
  }

  override def load(): ArrayByteSource = {
    val reader: CheckedSupplier[Array[Byte]] = () => read()
    new ArrayByteSource(Unchecked.wrap(reader), file.getName())
  }

  //-------------------------------------------------------------------------
  override def equals(other: Any): Boolean = {
    other match {
      case otherSource: FileByteSource => file.equals(otherSource.file)
      case _ => false
    }
  }

  override def hashCode(): Int = {
    file.hashCode()
  }

  override def toString(): String = {
    "FileByteSource[" + file + "]"
  }

}

object FileByteSource {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Meta.META)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance based on the underlying file.
   *
   * @param file  the file
   * @return the byte source
   */
  def of(file: File): FileByteSource = {
    new FileByteSource(file)
  }

  /**
   * Creates an instance based on a file path.
   *
   * @param path  the path to a file
   * @return the byte source
   */
  def of(path: Path): FileByteSource = {
    new FileByteSource(path.toFile())
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  private[io] final class Meta private () extends BasicMetaBean {

    override def isBuildable(): Boolean = {
      true
    }

    override def builder(): BeanBuilder[FileByteSource] = {
      new BasicImmutableBeanBuilder[FileByteSource](this) {
        private var file: File = uninitialized

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.PROP_FILE.name())) {
            file
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[FileByteSource] = {
          if (propertyName.equals(Meta.PROP_FILE.name())) {
            this.file = ArgChecker.notNull(value, "value").asInstanceOf[File]
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): FileByteSource = {
          new FileByteSource(file)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[FileByteSource]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[io] object Meta {

    private[io] val META: MetaBean = new Meta()

    private[io] val PROP_FILE: MetaProperty[File] = new BasicMetaProperty[File]("file") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[FileByteSource]
      }

      override def propertyType(): Class[File] = {
        classOf[File]
      }

      override def propertyGenericType(): Type = {
        classOf[File]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): File = {
        bean.asInstanceOf[FileByteSource].getFile()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private[io] val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("file", PROP_FILE)
  }

}
