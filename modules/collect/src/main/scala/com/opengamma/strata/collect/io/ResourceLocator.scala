/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path

import scala.annotation.static

import org.joda.convert.FromString
import org.joda.convert.ToString

import com.google.common.io.CharSource
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate

/**
 * A locator for a resource, specified as a file, URL, path or classpath resource.
 *
 * An instance of this class provides access to a resource, such as a configuration file.
 * The resource data is accessed using `CharSource` or `ByteSource`.
 */
final class ResourceLocator private (
    /**
     * The resource locator.
     */
    private val locator: String,
    /**
     * The source.
     */
    private val source: BeanByteSource) {

  //-------------------------------------------------------------------------
  /**
   * Gets the string form of the locator.
   *
   * The string form of the locator describes the location of the resource.
   *
   * @return the locator string
   */
  def getLocator(): String = {
    locator
  }

  /**
   * Gets the byte source to access the resource.
   *
   * A byte source is a supplier of data.
   * The source itself is neither opened nor closed.
   *
   * @return the byte source
   */
  def getByteSource(): BeanByteSource = {
    source
  }

  /**
   * Gets the char source to access the resource using UTF-8.
   *
   * A char source is a supplier of data.
   * The source itself is neither opened nor closed.
   *
   * This method handles Unicode Byte Order Marks.
   *
   * @return the char source
   */
  def getCharSource(): BeanCharSource = {
    source.asCharSourceUtf8UsingBom()
  }

  // the Bridger plugin turns this method into a synthetic bridge named getCharSource for backwards compatibility
  def `getCharSource$$bridge`(): CharSource = { // CSIGNORE
    getCharSource()
  }

  /**
   * Gets the char source to access the resource specifying the character set.
   *
   * A char source is a supplier of data.
   * The source itself is neither opened nor closed.
   *
   * @param charset  the character set to use
   * @return the char source
   */
  def getCharSource(charset: Charset): BeanCharSource = {
    source.asCharSource(charset)
  }

  // the Bridger plugin turns this method into a synthetic bridge named getCharSource for backwards compatibility
  def `getCharSource$$bridge`(charset: Charset): CharSource = { // CSIGNORE
    getCharSource(charset)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this locator equals another locator.
   *
   * The comparison checks the locator string.
   *
   * @param obj  the other locator, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: ResourceLocator =>
          locator.equals(other.locator)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the locator.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    locator.hashCode()
  }

  /**
   * Returns a string describing the locator.
   *
   * This can be parsed using `of(String)`.
   *
   * @return the descriptive string
   */
  @ToString
  override def toString(): String = {
    locator
  }

}

// the factory methods are @static so that the caller-sensitive ofClasspath(String) sees the same stack frames as Java
object ResourceLocator {

  /**
   * The prefix for classpath resource locators.
   */
  @static final val CLASSPATH_URL_PREFIX: String = "classpath:"
  /**
   * The prefix for file resource locators.
   */
  @static final val FILE_URL_PREFIX: String = "file:"
  /**
   * The prefix for URL resource locators.
   */
  @static final val URL_PREFIX: String = "url:"

  //-------------------------------------------------------------------------
  /**
   * Creates a resource from a string locator.
   *
   * This accepts locators starting with 'classpath:', 'url:' or 'file:'.
   * It also accepts unprefixed locators, treated as files.
   *
   * @param locator  the string form of the resource locator
   * @return the resource locator
   */
  @FromString
  @static def of(locator: String): ResourceLocator = {
    ArgChecker.notNull(locator, "locator")
    try {
      if (locator.startsWith(CLASSPATH_URL_PREFIX)) {
        val urlStr = locator.substring(CLASSPATH_URL_PREFIX.length())
        ofClasspath(urlStr)

      } else if (locator.startsWith(FILE_URL_PREFIX)) {
        val fileStr = locator.substring(FILE_URL_PREFIX.length())
        ofFile(new File(fileStr))

      } else if (locator.startsWith(URL_PREFIX)) {
        val pathStr = locator.substring(URL_PREFIX.length())
        ofUrl(new URL(pathStr))

      } else {
        ofFile(new File(locator))
      }
    } catch {
      case ex: Exception =>
        throw new IllegalArgumentException("Invalid resource locator: " + locator, ex)
    }
  }

  /**
   * Creates a resource from a `File`.
   *
   * Windows separators are converted to UNIX style.
   * This takes no account of possible '/' characters in the name.
   *
   * @param file  the file to wrap
   * @return the resource locator
   */
  @static def ofFile(file: File): ResourceLocator = {
    ArgChecker.notNull(file, "file")
    var filename = file.toString()
    // convert Windows separators to unix
    filename = if (File.separatorChar == '\\') filename.replace('\\', '/') else filename
    new ResourceLocator(FILE_URL_PREFIX + filename, FileByteSource.of(file))
  }

  /**
   * Creates a resource from a `Path`.
   *
   * This will return either a file locator or a URL locator.
   *
   * @param path  path to the file to wrap
   * @return the resource locator
   * @throws java.lang.IllegalArgumentException if the path is neither a file nor a URL
   */
  @static def ofPath(path: Path): ResourceLocator = {
    ArgChecker.notNull(path, "path")
    try {
      ofFile(path.toFile())
    } catch {
      case ex: UnsupportedOperationException =>
        try {
          ofUrl(path.toUri().toURL())
        } catch {
          case ex2: MalformedURLException =>
            throw new IllegalArgumentException("Path could not be converted to a File or URL: " + path)
        }
    }
  }

  /**
   * Creates a resource from a `URL`.
   *
   * @param url  path to the file to wrap
   * @return the resource locator
   */
  @static def ofUrl(url: URL): ResourceLocator = {
    ArgChecker.notNull(url, "url")
    val filename = url.toString()
    new ResourceLocator(URL_PREFIX + filename, UriByteSource.of(url))
  }

  /**
   * Creates a resource from a fully qualified resource name.
   *
   * If the resource name does not start with a slash '/', one will be prepended.
   * Use `ofClasspath(Class, String)` to get a relative resource.
   *
   * In Java 9 and later, resources can be encapsulated due to the module system.
   * As such, this method is caller sensitive.
   * It finds the class of the method that called this one, and uses that to search for
   * resources using `Class.getResource(String)`.
   *
   * @param resourceName  the resource name, which will have a slash '/' prepended if missing
   * @return the resource locator
   */
  @static def ofClasspath(resourceName: String): ResourceLocator = {
    ArgChecker.notNull(resourceName, "classpathLocator")
    val searchName = if (resourceName.startsWith("/")) resourceName else "/" + resourceName
    val caller = Guavate.callerClass(3)
    ofClasspath(caller, searchName)
  }

  /**
   * Creates a resource locator for a classpath resource which is associated with a class.
   *
   * The classpath is searched using the same method as `Class.getResource`.
   *
   *  - If the resource name starts with '/' it is treated as an absolute path relative to the classpath root
   *  - Otherwise the resource name is treated as a path relative to the package containing the class
   *
   * @param cls  the class used to find the resource
   * @param resourceName  the resource name
   * @return the resource locator
   */
  @static def ofClasspath(cls: Class[?], resourceName: String): ResourceLocator = {
    ArgChecker.notNull(resourceName, "classpathLocator")
    val url = cls.getResource(resourceName)
    if (url == null) {
      throw new IllegalArgumentException("Resource not found: " + resourceName)
    }
    ofClasspathUrl(url)
  }

  /**
   * Creates a resource from a `URL`.
   *
   * @param url  the URL to wrap
   * @return the resource locator
   */
  @static def ofClasspathUrl(url: URL): ResourceLocator = {
    ArgChecker.notNull(url, "url")
    val locator = CLASSPATH_URL_PREFIX + url.toString()
    new ResourceLocator(locator, UriByteSource.of(url))
  }

  /**
   * Selects a suitable class loader.
   *
   * @return the class loader
   */
  @static private[io] def classLoader(): ClassLoader = {
    val loader = classOf[ResourceConfig].getClassLoader()
    if (loader != null) {
      return loader
    }
    Thread.currentThread().getContextClassLoader()
  }

}
