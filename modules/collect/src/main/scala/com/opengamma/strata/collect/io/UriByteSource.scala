/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
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
 * A byte source implementation that obtains data from a URI.
 *
 * This implementation differs from Guava in that it is a Joda-Bean.
 * In addition, `read()` throws `UncheckedIOException` instead of `IOException`.
 */
@SerialVersionUID(1L)
final class UriByteSource private (rawUri: URI)
  extends BeanByteSource with ImmutableBean with Serializable {

  /**
   * The underlying URI.
   */
  private val uri: URI = ArgChecker.notNull(rawUri, "uri")

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    UriByteSource.Meta.META
  }

  override def getFileName(): Optional[String] = {
    if (!uri.isOpaque()) {
      val path = uri.getPath()
      val lastSlash = path.lastIndexOf('/')
      Optional.of(path.substring(lastSlash + 1))
    } else {
      Optional.empty()
    }
  }

  /**
   * Gets the URI.
   *
   * @return the URI
   */
  def getUri(): URI = {
    uri
  }

  //-------------------------------------------------------------------------
  @throws[IOException]
  override def openStream(): InputStream = {
    try {
      uri.toURL().openStream()
    } catch {
      case ex: MalformedURLException => throw new IllegalStateException(ex)
    }
  }

  override def load(): ArrayByteSource = {
    val reader: CheckedSupplier[Array[Byte]] = () => read()
    new ArrayByteSource(Unchecked.wrap(reader), getFileName().orElse(null))
  }

  //-------------------------------------------------------------------------
  override def equals(other: Any): Boolean = {
    other match {
      case otherSource: UriByteSource => uri.equals(otherSource.uri)
      case _ => false
    }
  }

  override def hashCode(): Int = {
    uri.hashCode()
  }

  override def toString(): String = {
    "UriByteSource[" + uri + "]"
  }

}

object UriByteSource {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Meta.META)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance based on the underlying URI.
   *
   * @param uri  the URI
   * @return the byte source
   */
  def of(uri: URI): UriByteSource = {
    new UriByteSource(uri)
  }

  /**
   * Creates an instance based on the underlying URL.
   *
   * @param url  the URL
   * @return the byte source
   */
  def of(url: URL): UriByteSource = {
    try {
      new UriByteSource(url.toURI())
    } catch {
      case ex: URISyntaxException => throw new IllegalArgumentException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  private[io] final class Meta private () extends BasicMetaBean {

    override def isBuildable(): Boolean = {
      true
    }

    override def builder(): BeanBuilder[UriByteSource] = {
      new BasicImmutableBeanBuilder[UriByteSource](this) {
        private var uri: URI = uninitialized

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.PROP_URI.name())) {
            uri
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[UriByteSource] = {
          if (propertyName.equals(Meta.PROP_URI.name())) {
            this.uri = ArgChecker.notNull(value, "value").asInstanceOf[URI]
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): UriByteSource = {
          new UriByteSource(uri)
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[UriByteSource]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[io] object Meta {

    private[io] val META: MetaBean = new Meta()

    private[io] val PROP_URI: MetaProperty[URI] = new BasicMetaProperty[URI]("uri") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[UriByteSource]
      }

      override def propertyType(): Class[URI] = {
        classOf[URI]
      }

      override def propertyGenericType(): Type = {
        classOf[URI]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): URI = {
        bean.asInstanceOf[UriByteSource].getUri()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private[io] val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("uri", PROP_URI)
  }

}
