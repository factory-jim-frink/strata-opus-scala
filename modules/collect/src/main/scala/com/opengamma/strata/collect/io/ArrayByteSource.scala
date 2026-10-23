/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import com.google.common.base.Preconditions.checkArgument

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Arrays
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

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.common.io.ByteProcessor
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.google.common.io.CharSource
import com.google.common.primitives.Bytes
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedConsumer
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * A byte source implementation that explicitly wraps a byte array.
 *
 * This implementation allows `IOException` to be avoided in many cases,
 * and to be able to create and retrieve the internal array unsafely.
 */
@SerialVersionUID(1L)
final class ArrayByteSource private[io] (
    /**
     * The byte array.
     */
    private val array: Array[Byte],
    rawFileName: String)
  extends BeanByteSource with ImmutableBean with Serializable {

  /**
   * The file name, null if not known.
   */
  private val fileName: String = Strings.emptyToNull(rawFileName)

  //-------------------------------------------------------------------------
  // creates an instance, without copying the array
  private def this(array: Array[Byte]) = {
    this(array, null)
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    ArrayByteSource.Meta.META
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
  def withFileName(fileName: String): ArrayByteSource = {
    ArgChecker.notNull(fileName, "fileName")
    val lastSlash = fileName.lastIndexOf('/')
    new ArrayByteSource(array, fileName.substring(lastSlash + 1))
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the underlying array.
   *
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the array returned by this method.
   * Doing so would violate the immutability of this class.
   *
   * @return the raw array
   */
  def readUnsafe(): Array[Byte] = {
    array
  }

  /**
   * Reads the source, converting to UTF-8.
   *
   * @return the UTF-8 string
   */
  override def readUtf8(): String = {
    new String(array, StandardCharsets.UTF_8)
  }

  /**
   * Reads the source, converting to UTF-8 using a Byte-Order Mark if available.
   *
   * @return the UTF-8 string
   */
  override def readUtf8UsingBom(): String = {
    UnicodeBom.bytesToString(array)
  }

  override def asCharSourceUtf8(): StringCharSource = {
    // no need to bridge, as scalac is already doing that
    new StringCharSource(readUtf8(), fileName)
  }

  // converted to a bridge method named asCharSourceUtf8 by the bridger plugin
  @targetName("asCharSourceUtf8$$bridge")
  override def asCharSourceUtf8Bridge(): CharSource = { // CSIGNORE
    // bridged below for backwards compatibility
    asCharSourceUtf8()
  }

  override def asCharSource(charset: Charset): StringCharSource = {
    new StringCharSource(new String(array, charset), fileName)
  }

  override def asCharSourceUtf8UsingBom(): StringCharSource = {
    // bridged below for backwards compatibility
    new StringCharSource(UnicodeBom.bytesToString(array), fileName)
  }

  // converted to a bridge method named asCharSourceUtf8UsingBom by the bridger plugin
  @targetName("asCharSourceUtf8UsingBom$$bridge")
  override def asCharSourceUtf8UsingBomBridge(): CharSource = { // CSIGNORE
    UnicodeBom.toCharSource(this: ByteSource)
  }

  //-------------------------------------------------------------------------
  override def hash(hashFunction: HashFunction): HashCode = {
    // overridden to use array directly for performance
    hashFunction.hashBytes(array)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the MD5 hash of the bytes.
   *
   * The returned hash is in byte form.
   *
   * @return the MD5 hash
   * @deprecated Use `toHash(HashFunction)`
   */
  @Deprecated
  @deprecated("Use toHash(HashFunction)", "")
  def toMd5(): ArrayByteSource = {
    toHash(Hashing.md5())
  }

  /**
   * Returns the SHA-512 hash of the bytes.
   *
   * @return the SHA-512 hash
   * @deprecated Use `toHash(HashFunction)`
   */
  @Deprecated
  @deprecated("Use toHash(HashFunction)", "")
  def toSha512(): ArrayByteSource = {
    ArrayByteSource.ofUnsafe(hash(Hashing.sha512()).asBytes())
  }

  /**
   * Encodes the byte source using base-64.
   *
   * @return the base-64 encoded form
   */
  override def toBase64(): ArrayByteSource = {
    // overridden to use array directly for performance
    ArrayByteSource.ofUnsafe(Base64.getEncoder().encode(array))
  }

  /**
   * Encodes the byte source using base-64, returning a string.
   *
   * Equivalent to `toBase64().readUtf8()`.
   *
   * @return the base-64 encoded string
   */
  override def toBase64String(): String = {
    // overridden to use array directly for performance
    Base64.getEncoder().encodeToString(array)
  }

  /**
   * Encodes the byte source.
   *
   * @param codec  the codec to use
   * @return the encoded form
   */
  def encode(codec: ByteSourceCodec): ArrayByteSource = {
    codec.encode(array, fileName)
  }

  /**
   * Decodes the byte source.
   *
   * @param codec  the codec to use
   * @return the decoded form
   */
  def decode(codec: ByteSourceCodec): ArrayByteSource = {
    codec.decode(array, fileName)
  }

  /**
   * Encodes the byte source using hex, sometimes referred to as base-16, returning a string.
   *
   * @return the hex encoded string
   */
  def toHexString(): String = {
    BaseEncoding.base16().encode(array)
  }

  //-------------------------------------------------------------------------
  override def openStream(): ByteArrayInputStream = {
    new ByteArrayInputStream(array)
  }

  override def openBufferedStream(): ByteArrayInputStream = {
    openStream()
  }

  override def isEmpty(): Boolean = {
    array.length == 0
  }

  /**
   * Gets the size, which is always known.
   *
   * @return the size, which is always known
   */
  override def sizeIfKnown(): com.google.common.base.Optional[java.lang.Long] = {
    com.google.common.base.Optional.of(java.lang.Long.valueOf(size()))
  }

  override def size(): Long = {
    array.length.toLong
  }

  override def slice(offset: Long, length: Long): ArrayByteSource = {
    checkArgument(offset >= 0, "offset (%s) may not be negative", offset)
    checkArgument(length >= 0, "length (%s) may not be negative", length)
    if (offset > array.length) {
      ArrayByteSource.EMPTY
    } else {
      val minPos = offset.toInt
      val len: Long = Math.min(Math.min(length, Integer.MAX_VALUE.toLong), array.length.toLong)
      val maxPos = Math.min(minPos + len, array.length.toLong).toInt
      new ArrayByteSource(Arrays.copyOfRange(array, minPos, maxPos), fileName)
    }
  }

  @throws[IOException]
  override def copyTo(output: OutputStream): Long = {
    output.write(array)
    array.length.toLong
  }

  override def read(): Array[Byte] = {
    array.clone()
  }

  @throws[IOException]
  override def read[T](processor: ByteProcessor[T]): T = {
    processor.processBytes(array, 0, array.length)
    processor.getResult()
  }

  @throws[IOException]
  override def contentEquals(other: ByteSource): Boolean = {
    other match {
      case otherArray: ArrayByteSource => JodaBeanUtils.equal(array, otherArray.array)
      case _ => super.contentEquals(other)
    }
  }

  override def load(): ArrayByteSource = {
    this
  }

  //-------------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[ArrayByteSource]
      JodaBeanUtils.equal(fileName, other.fileName) &&
          JodaBeanUtils.equal(array, other.array)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(fileName)
    hash = hash * 31 + JodaBeanUtils.hashCode(array)
    hash
  }

  override def toString(): String = {
    "ArrayByteSource[" + size() + " bytes" + (if (fileName != null) ", " + fileName else "") + "]"
  }

}

object ArrayByteSource {

  /**
   * An empty source.
   */
  @static final val EMPTY: ArrayByteSource = new ArrayByteSource(new Array[Byte](0))

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Meta.META)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance, copying the array.
   *
   * @param array  the array, copied
   * @return the byte source
   */
  def copyOf(array: Array[Byte]): ArrayByteSource = {
    new ArrayByteSource(array.clone())
  }

  /**
   * Obtains an instance by copying part of an array.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy
   * @param fromIndex  the offset from the start of the array
   * @return an array containing the specified values
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def copyOf(array: Array[Byte], fromIndex: Int): ArrayByteSource = {
    copyOf(array, fromIndex, array.length)
  }

  /**
   * Obtains an instance by copying part of an array.
   *
   * The input array is copied and not mutated.
   *
   * @param array  the array to copy
   * @param fromIndexInclusive  the start index of the input array to copy from
   * @param toIndexExclusive  the end index of the input array to copy to
   * @return an array containing the specified values
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def copyOf(array: Array[Byte], fromIndexInclusive: Int, toIndexExclusive: Int): ArrayByteSource = {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length)
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length)
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      EMPTY
    } else {
      new ArrayByteSource(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive))
    }
  }

  /**
   * Obtains an instance, not copying the array.
   *
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   *
   * @param array  the array, not copied
   * @return the byte source
   */
  def ofUnsafe(array: Array[Byte]): ArrayByteSource = {
    new ArrayByteSource(array)
  }

  /**
   * Obtains an instance from a string using UTF-8.
   *
   * @param str  the string to store using UTF-8
   * @return the byte source
   */
  def ofUtf8(str: String): ArrayByteSource = {
    new ArrayByteSource(str.getBytes(StandardCharsets.UTF_8))
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from another byte source.
   *
   * @param other  the other byte source
   * @return the byte source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def from(other: ByteSource): ArrayByteSource = {
    other match {
      case arraySource: ArrayByteSource =>
        arraySource
      case beanSource: BeanByteSource =>
        readFrom(other, beanSource.getFileName().orElse(null))
      case _ if other.getClass().getName().equals("com.google.common.io.ByteSource$ByteArrayByteSource") =>
        // extract the byte[] without using reflection
        // if the Guava implementation changes this could break, but that seems unlikely
        val processor: ByteProcessor[Array[Byte]] = new ByteProcessor[Array[Byte]]() {
          private var captured: Array[Byte] = uninitialized

          @throws[IOException]
          override def processBytes(buf: Array[Byte], off: Int, len: Int): Boolean = {
            if (captured != null) {
              // this defends against the Guava implementation being changed
              captured = Bytes.concat(captured, Arrays.copyOfRange(buf, off, off + len))
            } else if (off == 0 && len == buf.length) {
              // this is the normal case where we can just assign the source
              captured = buf
            } else {
              // this happens if the source has been sliced
              captured = Arrays.copyOfRange(buf, off, off + len)
            }
            true
          }

          override def getResult(): Array[Byte] = {
            captured
          }
        }
        val reader: CheckedSupplier[ArrayByteSource] = () => new ArrayByteSource(other.read(processor))
        Unchecked.wrap(reader)
      case _ =>
        // handle all other byte sources
        val str = other.toString()
        if (str.equals("ByteSource.empty()")) {
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
  private def readFrom(other: ByteSource, fileName: String): ArrayByteSource = {
    val reader: CheckedSupplier[Array[Byte]] = () => other.read()
    new ArrayByteSource(Unchecked.wrap(reader), fileName)
  }

  /**
   * Obtains an instance from an input stream.
   *
   * This method use the supplier to open the input stream, extract the bytes and close the stream.
   * It is intended that invoking the supplier opens the stream.
   * It is not intended that an already open stream is supplied.
   *
   * @param inputStreamSupplier  the supplier of the input stream
   * @return the byte source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def from(inputStreamSupplier: CheckedSupplier[? <: InputStream]): ArrayByteSource = {
    val loader: CheckedSupplier[ArrayByteSource] = () => {
      closing(inputStreamSupplier.get()) { in =>
        from(in: InputStream)
      }
    }
    Unchecked.wrap(loader)
  }

  /**
   * Obtains an instance from an input stream.
   *
   * This method uses an already open input stream, extracting the bytes.
   * The stream is not closed - that is the responsibility of the caller.
   *
   * @param inputStream  the open input stream, which will not be closed
   * @return the byte source
   * @throws java.io.IOException if an IO error occurs
   */
  @throws[IOException]
  def from(inputStream: InputStream): ArrayByteSource = {
    val bytes = ByteStreams.toByteArray(inputStream)
    new ArrayByteSource(bytes)
  }

  /**
   * Obtains an instance from an input stream, specifying the expected size.
   *
   * This method uses an already open input stream, extracting the bytes.
   * The stream is not closed - that is the responsibility of the caller.
   *
   * @param inputStream  the open input stream, which will not be closed
   * @param expectedSize  the expected size of the input, not negative
   * @return the byte source
   * @throws java.io.IOException if an IO error occurs
   */
  @throws[IOException]
  def from(inputStream: InputStream, expectedSize: Int): ArrayByteSource = {
    ArgChecker.notNegative(expectedSize, "expectedSize")
    val main = new Array[Byte](expectedSize)
    var remaining = expectedSize
    var shortResult: ArrayByteSource = null
    while (remaining > 0 && shortResult == null) {
      val offset = expectedSize - remaining
      val read = inputStream.read(main, offset, remaining)
      if (read == -1) {
        // actual stream size < expected size
        shortResult = new ArrayByteSource(Arrays.copyOf(main, offset))
      } else {
        remaining -= read
      }
    }
    if (shortResult != null) {
      shortResult
    } else {
      // bytes is now full
      val firstExcess = inputStream.read()
      if (firstExcess == -1) {
        // actual stream size == expected size
        new ArrayByteSource(main)
      } else {
        // actual stream size > expected size
        val excess = ByteStreams.toByteArray(inputStream)
        val result = Arrays.copyOf(main, main.length + 1 + excess.length)
        result(main.length) = firstExcess.toByte
        System.arraycopy(excess, 0, result, main.length + 1, excess.length)
        new ArrayByteSource(result)
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that captures the contents of an output stream.
   *
   * This method provides an output stream.
   * When bytes are written to the output stream, they are captured in the resulting byte source.
   * The caller is not responsible for closing the stream.
   *
   * @param handler  the handler that writes to the output stream
   * @return the byte source
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def fromOutput(handler: CheckedConsumer[OutputStream]): ArrayByteSource = {
    val writer: CheckedSupplier[ArrayByteSource] = () => {
      closing(new ByteArrayOutputStream(1024)) { baos =>
        handler.accept(baos)
        new ArrayByteSource(baos.toByteArray())
      }
    }
    Unchecked.wrap(writer)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a base-64 encoded string.
   *
   * @param base64  the base64 string to convert
   * @return the decoded byte source
   * @throws java.lang.IllegalArgumentException if the input is not Base64 encoded
   */
  def fromBase64(base64: String): ArrayByteSource = {
    new ArrayByteSource(Base64.getDecoder().decode(base64))
  }

  /**
   * Obtains an instance from a hex encoded string, sometimes referred to as base-16.
   *
   * @param hex  the hex string to convert
   * @return the decoded byte source
   * @throws java.lang.IllegalArgumentException if the input is not hex encoded
   */
  def fromHex(hex: String): ArrayByteSource = {
    new ArrayByteSource(BaseEncoding.base16().decode(hex))
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

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  private[io] final class Meta private () extends BasicMetaBean {

    override def isBuildable(): Boolean = {
      true
    }

    override def builder(): BeanBuilder[ArrayByteSource] = {
      new BasicImmutableBeanBuilder[ArrayByteSource](this) {
        private var array: Array[Byte] = new Array[Byte](0)
        private var fileName: String = uninitialized

        override def get(propertyName: String): Object = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            array  // not cloned for performance
          } else if (propertyName.equals(Meta.FILE_NAME.name())) {
            fileName
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
        }

        override def set(propertyName: String, value: Object): BeanBuilder[ArrayByteSource] = {
          if (propertyName.equals(Meta.ARRAY.name())) {
            this.array = ArgChecker.notNull(value, "value").asInstanceOf[Array[Byte]]  // not cloned for performance
          } else if (propertyName.equals(Meta.FILE_NAME.name())) {
            this.fileName = value.asInstanceOf[String]
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName)
          }
          this
        }

        override def build(): ArrayByteSource = {
          val byteSource = ArrayByteSource.ofUnsafe(array)
          if (fileName != null) byteSource.withFileName(fileName) else byteSource
        }
      }
    }

    override def beanType(): Class[? <: Bean] = {
      classOf[ArrayByteSource]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      Meta.MAP
    }
  }

  private[io] object Meta {

    private[io] val META: MetaBean = new Meta()

    private[io] val ARRAY: MetaProperty[Array[Byte]] = new BasicMetaProperty[Array[Byte]]("array") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[ArrayByteSource]
      }

      override def propertyType(): Class[Array[Byte]] = {
        classOf[Array[Byte]]
      }

      override def propertyGenericType(): Type = {
        classOf[Array[Byte]]
      }

      override def style(): PropertyStyle = {
        PropertyStyle.IMMUTABLE
      }

      override def annotations(): List[Annotation] = {
        ImmutableList.of[Annotation]()
      }

      override def get(bean: Bean): Array[Byte] = {
        bean.asInstanceOf[ArrayByteSource].read()
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private[io] val FILE_NAME: MetaProperty[String] = new BasicMetaProperty[String]("fileName") {

      override def metaBean(): MetaBean = {
        META
      }

      override def declaringType(): Class[?] = {
        classOf[ArrayByteSource]
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
        bean.asInstanceOf[ArrayByteSource].getFileName().orElse(null)
      }

      override def set(bean: Bean, value: Object): Unit = {
        throw new UnsupportedOperationException("Property cannot be written: " + name())
      }
    }

    private[io] val MAP: ImmutableMap[String, MetaProperty[?]] =
      ImmutableMap.of[String, MetaProperty[?]]("array", ARRAY, "fileName", FILE_NAME)
  }

}
