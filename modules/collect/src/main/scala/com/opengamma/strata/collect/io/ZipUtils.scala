/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.lang.StringBuilder
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Optional
import java.util.Set
import java.util.function.BiConsumer
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

import scala.annotation.meta.field
import scala.annotation.static

import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.TypedMetaBean
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.light.LightMetaBean

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker

/**
 * Utility class to simplify accessing and creating zip files, and other packed formats.
 */
final class ZipUtils private ()

object ZipUtils {
  // need to watch out for ZIP slip attack when unzipping to file system
  // https://github.com/snyk/zip-slip-vulnerability
  private val DUMMY_PATH: Path = Paths.get("/dummy/")

  //-------------------------------------------------------------------------
  /**
   * Unzips the source returning the file names that are contained.
   *
   * The result is a set of relative path names within the zip.
   * Only files are returned, not folders.
   *
   * @param source  the byte source to unzip
   * @return the set of relative path names
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unzipPathNames(source: BeanByteSource): Set[String] = {
    val entryNames = ImmutableSet.builder[String]()
    try {
      closing(new ZipInputStream(source.openStream())) { in =>
        var entry = in.getNextEntry()
        while (entry != null) {
          validateZipPathName(DUMMY_PATH, entry)
          if (!entry.isDirectory()) {
            entryNames.add(entry.getName())
          }
          in.closeEntry()
          entry = in.getNextEntry()
        }
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
    entryNames.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips a single file from the source in memory.
   *
   * Callers can use `unzipPathNames(BeanByteSource)` to find the available names.
   * The relative path name must match exactly that in the zip.
   *
   * @param source  the byte source to unzip
   * @param relativePathName  the exact relative path name that the file is stored as
   * @return the unzipped file, empty if not found
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unzipPathNameInMemory(source: BeanByteSource, relativePathName: String): Optional[ArrayByteSource] = {
    ArgChecker.notNull(relativePathName, "relativePathName")
    try {
      closing(new ZipInputStream(source.openStream())) { in =>
        var found: ArrayByteSource = null
        var entry = in.getNextEntry()
        while (entry != null && found == null) {
          validateZipPathName(DUMMY_PATH, entry)
          if (!entry.isDirectory() && entry.getName().equals(relativePathName)) {
            found = extractInputStream(in, entry.getName())
          } else {
            in.closeEntry()
            entry = in.getNextEntry()
          }
        }
        if (found != null) Optional.of(found) else Optional.empty[ArrayByteSource]()
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source to a file path.
   *
   * Empty folders in the zip will not be created.
   *
   * @param source  the byte source to unzip
   * @param path  the path to unzip to
   * @throws java.io.UncheckedIOException if an IO error occurs
   * @throws java.lang.SecurityException if the path is not absolute and the calling thread cannot access system property "user.dir"
   */
  def unzip(source: BeanByteSource, path: Path): Unit = {
    val absolutePath = path.toAbsolutePath()
    val deduplicate: Set[ZipKey] = new HashSet[ZipKey]()
    try {
      closing(new ZipInputStream(source.openStream())) { in =>
        var entry = in.getNextEntry()
        while (entry != null) {
          val resolved = validateZipPathName(absolutePath, entry)
          if (!entry.isDirectory()) {
            if (deduplicate.add(new ZipKey(entry, resolved))) {
              Files.createDirectories(resolved)
              Files.copy(in, resolved, StandardCopyOption.REPLACE_EXISTING)
            }
          }
          in.closeEntry()
          entry = in.getNextEntry()
        }
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a zip file from the list of files in memory.
   *
   * Each source must have a file name.
   * The output will not contain subfolders.
   *
   * @param sources  the byte sources to zip
   * @return the zip file
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def zipInMemory(sources: List[? <: BeanByteSource]): ArrayByteSource = {
    val instantMillis = System.currentTimeMillis()
    try {
      closing(new ByteArrayOutputStream()) { baos =>
        closing(new ZipOutputStream(baos)) { out =>
          val it = sources.iterator()
          while (it.hasNext()) {
            val source: BeanByteSource = it.next()
            val entry = new ZipEntry(
                source.getFileName()
                    .orElseThrow(() => new IllegalArgumentException("ByteSource must have a name in order to be zipped")))
            entry.setTime(instantMillis)
            out.putNextEntry(entry)
            source.copyTo(out)
            out.closeEntry()
          }
        }
        ArrayByteSource.ofUnsafe(baos.toByteArray())
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, returning a map.
   *
   * This method loads the whole unpacked file into memory.
   * Where possible, `unpackInMemory(BeanByteSource, BiConsumer)` should be preferred
   * as it only loads files one at a time.
   *
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   *
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unpackInMemory(source: BeanByteSource): Map[String, ArrayByteSource] = {
    val builder = ImmutableMap.builder[String, ArrayByteSource]()
    unpackInMemory(source, (name, extracted) => builder.put(name, extracted): Unit)
    builder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Unpacks the source into memory, invoking the consumer for each entry.
   *
   * Unpacking handles ZIP, GZ and BASE64 formats based entirely on the suffix of the input file name.
   * If the input suffix is not recognized as a packed format, the consumer is invoked with the original file.
   * This method is not recursive.
   *
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unpackInMemory(source: BeanByteSource, consumer: BiConsumer[String, ArrayByteSource]): Unit = {
    val fileName = source.getFileName().orElse("")
    if (suffixMatches(fileName, ".zip")) {
      unzipInMemory(source, consumer)

    } else if (suffixMatches(fileName, ".gz")) {
      ungzInMemory(source, fileName, consumer)

    } else if (suffixMatches(fileName, ".base64")) {
      try {
        closing(Base64.getDecoder().wrap(source.openBufferedStream())) { in =>
          val shortFileName = fileName.substring(0, fileName.length() - 7)
          val extracted = extractInputStream(in, shortFileName)
          consumer.accept(shortFileName, extracted)
        }
      } catch {
        case ex: IOException => throw new UncheckedIOException(ex)
      }

    } else {
      consumer.accept(fileName, source.load())
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, returning a map.
   *
   * This method loads the whole unpacked file into memory.
   * Where possible, `unzipInMemory(BeanByteSource, BiConsumer)` should be preferred
   * as it only loads files one at a time.
   *
   * Unlike `unpackInMemory(BeanByteSource)`, this method always treats the input as a zip file.
   *
   * @param source  the byte source to unpack
   * @return the map of unzipped byte sources, keyed by relative path name
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unzipInMemory(source: BeanByteSource): Map[String, ArrayByteSource] = {
    val builder = ImmutableMap.builder[String, ArrayByteSource]()
    unzipInMemory(source, (name, extracted) => builder.put(name, extracted): Unit)
    builder.build()
  }

  //-------------------------------------------------------------------------
  /**
   * Unzips the source into memory, invoking the consumer for each entry.
   *
   * Unlike `unpackInMemory(BeanByteSource, BiConsumer)`, this method always treats the input as a zip file.
   *
   * @param source  the byte source to unpack
   * @param consumer  the consumer, which is passed the relative path name and content for each entry
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def unzipInMemory(source: BeanByteSource, consumer: BiConsumer[String, ArrayByteSource]): Unit = {
    val deduplicate: Set[ZipKey] = new HashSet[ZipKey]()
    try {
      closing(new ZipInputStream(source.openStream())) { in =>
        var entry = in.getNextEntry()
        while (entry != null) {
          val resolved = validateZipPathName(DUMMY_PATH, entry)
          if (!entry.isDirectory()) {
            if (deduplicate.add(new ZipKey(entry, resolved))) {
              val extractedBytes = extractInputStream(in, entry.getName())
              consumer.accept(entry.getName(), extractedBytes)
            }
          }
          in.closeEntry()
          entry = in.getNextEntry()
        }
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Provides a new source that decrypts the specified source ZIP.
   *
   * This returns a wrapper around the input source that provides decryption.
   * The result is normally passed directly into one of the other methods on this class.
   *
   * @param source  the byte source to unpack
   * @param password  the password to decrypt the input
   * @return the decrypted zip file
   */
  def decryptZip(source: BeanByteSource, password: String): BeanByteSource = {
    new ZipDecryptByteSource(source, password)
  }

  //-------------------------------------------------------------------------
  // ungz the contents
  private def ungzInMemory(source: BeanByteSource, fileName: String, consumer: BiConsumer[String, ArrayByteSource]): Unit = {
    try {
      closing(new GZIPInputStream(source.openStream())) { in =>
        val shortFileName = fileName.substring(0, fileName.length() - 3)
        val extractedBytes = extractInputStream(in, shortFileName)
        consumer.accept(shortFileName, extractedBytes)
      }
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  // safely extract the suffix
  private def suffixMatches(name: String, suffix: String): Boolean = {
    name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length())
  }

  // unzip the input, trying to recover from large files or ZIP bombs
  @throws[IOException]
  private def extractInputStream(in: InputStream, fileName: String): ArrayByteSource = {
    try {
      ArrayByteSource.from(in).withFileName(fileName)
    } catch {
      case ex: OutOfMemoryError =>
        System.gc()
        throw new IOException("Unzipped input too large: " + fileName)
    }
  }

  // prevent ZIP slip attack
  @throws[ZipException]
  private def validateZipPathName(rootPath: Path, entry: ZipEntry): Path = {
    val normalizedRootPath = rootPath.normalize()
    val resolved = normalizedRootPath.resolve(entry.getName()).normalize()
    if (!resolved.startsWith(normalizedRootPath)) {
      throw new ZipException("ZIP file contains illegal file name: " + entry.getName())
    }
    resolved
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
  // handle duplicate entries in a zip file
  // while such a zip file is stupid, it is apparently valid
  private class ZipKey private[ZipUtils] (entry: ZipEntry, private val resolvedPath: Path) {
    private val crc: Long = entry.getCrc()
    private val size: Long = entry.getSize()

    override def equals(obj: Any): Boolean = {
      obj match {
        case key: ZipKey =>
          resolvedPath.equals(key.resolvedPath) &&
              crc == key.crc &&
              size == key.size
        case _ => false
      }
    }

    override def hashCode(): Int = {
      super.hashCode()
    }
  }

  //-------------------------------------------------------------------------
  // byte source to wrap the underlying source
  private[io] final class ZipDecryptByteSource private (
      @(PropertyDefinition @field)
      private val underlying: BeanByteSource,
      private val password: Array[Char])
    extends BeanByteSource with ImmutableBean {

    //-------------------------------------------------------------------------
    private def this(underlying: BeanByteSource) = {
      this(underlying, null: Array[Char])
      throw new IllegalStateException("ZipDecryptByteSource cannot be deserialized as it contains a password")
    }

    private[ZipUtils] def this(underlying: BeanByteSource, password: String) = {
      this(ArgChecker.notNull(underlying, "underlying"), ArgChecker.notNull(password, "password").toCharArray())
    }

    override def getFileName(): Optional[String] = {
      underlying.getFileName()
    }

    @throws[IOException]
    override def openStream(): InputStream = {
      new ZipDecryptInputStream(underlying.openStream(), password)
    }

    // originally Joda-Bean generated
    //-----------------------------------------------------------------------
    override def metaBean(): TypedMetaBean[ZipDecryptByteSource] = {
      ZipDecryptByteSource.META_BEAN
    }

    /**
     * Gets the underlying.
     * @return the value of the property, not null
     */
    def getUnderlying(): BeanByteSource = {
      underlying
    }

    override def equals(obj: Any): Boolean = {
      if (obj.asInstanceOf[AnyRef] eq this) {
        true
      } else if (obj != null && (obj.getClass() eq this.getClass())) {
        val other = obj.asInstanceOf[ZipDecryptByteSource]
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
      val buf = new StringBuilder(64)
      buf.append("ZipDecryptByteSource{")
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying))
      buf.append('}')
      buf.toString()
    }
  }

  private[io] object ZipDecryptByteSource {

    // initialized in the class static initializer, so the lookup has private access to the bean class
    @static private[io] val META_BEAN: TypedMetaBean[ZipDecryptByteSource] =
      LightMetaBean.of(
          classOf[ZipDecryptByteSource],
          MethodHandles.lookup(),
          Array[String]("underlying"),
          new Array[Object](0)*)

    @static private val REGISTERED: Boolean = {
      MetaBean.register(META_BEAN)
      true
    }

    /**
     * The meta-bean for `ZipDecryptByteSource`.
     * @return the meta-bean, not null
     */
    def meta(): TypedMetaBean[ZipDecryptByteSource] = {
      META_BEAN
    }
  }
}
