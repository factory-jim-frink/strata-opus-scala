/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

import scala.annotation.targetName

import com.google.common.io.CharSource
import com.google.common.io.Files
import com.google.common.io.MoreFiles
import com.google.common.io.Resources

/**
 * Helper that allows `CharSource` objects to be created.
 */
final class CharSources private () {

  // converted to a bridge method named ofContent by the bridger plugin
  @targetName("ofContent$$bridge")
  def ofContentBridge(content: String): CharSource = { // CSIGNORE
    CharSources.ofContent(content)
  }

  // converted to a bridge method named ofContent by the bridger plugin
  @targetName("ofContent$$bridge")
  def ofContentBridge(content: Array[Byte]): CharSource = { // CSIGNORE
    CharSources.ofContent(content)
  }

  // converted to a bridge method named ofContent by the bridger plugin
  @targetName("ofContent$$bridge")
  def ofContentBridge(content: Array[Byte], charset: Charset): CharSource = { // CSIGNORE
    CharSources.ofContent(content, charset)
  }

}

object CharSources {

  /**
   * Obtains an instance of `CharSource` from a file name, specified as a String.
   *
   * @param fileName  the file name, as a String
   * @return a new instance of `CharSource` with UTF-8 for charset.
   */
  def ofFileName(fileName: String): CharSource = {
    Files.asCharSource(new File(fileName), StandardCharsets.UTF_8)
  }

  /**
   * Obtains an instance of `CharSource` from a file name, specified as a String.
   * This also takes in a specific character set, as a `Charset`.
   *
   * @param fileName  the file name, as a String
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of `CharSource`
   */
  def ofFileName(fileName: String, charset: Charset): CharSource = {
    Files.asCharSource(new File(fileName), charset)
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of `CharSource` from a file object, specified as a `File`.
   *
   * @param file  the file object
   * @return a new instance of `CharSource` with UTF-8 for charset.
   */
  def ofFile(file: File): CharSource = {
    Files.asCharSource(file, StandardCharsets.UTF_8)
  }

  /**
   * Obtains an instance of `CharSource` from a file object, specified as a `File`.
   * This also takes in a specific character set, as a `Charset`.
   *
   * @param file  the file object
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of `CharSource`
   */
  def ofFile(file: File, charset: Charset): CharSource = {
    Files.asCharSource(file, charset)
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of `CharSource` from a file path, specified as a `Path`.
   *
   * @param path  the path to create a `CharSource` from
   * @return a new instance of `CharSource` with UTF-8 for charset.
   */
  def ofPath(path: Path): CharSource = {
    MoreFiles.asCharSource(path, StandardCharsets.UTF_8)
  }

  /**
   * Obtains an instance of `CharSource` from a file path, specified as a `Path`.
   * This also takes in a specific character set, as a `Charset`.
   *
   * @param path  the path to create a `CharSource` from
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of `CharSource`
   */
  def ofPath(path: Path, charset: Charset): CharSource = {
    MoreFiles.asCharSource(path, charset)
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of `CharSource` from a URL, specified as a `URL` object.
   *
   * @param url  the url to create a `CharSource` from
   * @return a new instance of `CharSource` with UTF-8 for charset.
   */
  def ofUrl(url: URL): CharSource = {
    Resources.asCharSource(url, StandardCharsets.UTF_8)
  }

  /**
   * Obtains an instance of `CharSource` from an URL, specified as a `URL` object.
   * This also takes in a specific character set, as a `Charset`.
   *
   * @param url  the url to create a `CharSource` from
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of `CharSource`.
   */
  def ofUrl(url: URL, charset: Charset): CharSource = {
    Resources.asCharSource(url, charset)
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of `CharSource` from a text variable, specified as a `String` object.
   *
   * @param content  the text to create a `CharSource` for
   * @return a new instance of `CharSource`
   */
  def ofContent(content: String): StringCharSource = {
    // bridged for backwards compatibility
    StringCharSource.of(content)
  }

  //---------------------------------------------------------------------------------------------
  /**
   * Obtains an instance of `CharSource` from a text variable, specified as a byte array.
   *
   * @param content  the text to create a `CharSource` for
   * @return a new instance of `CharSource` with UTF-8 for charset
   */
  def ofContent(content: Array[Byte]): StringCharSource = {
    // bridged for backwards compatibility
    StringCharSource.fromBytesUtf8(content)
  }

  /**
   * Obtains an instance of `CharSource` from a text variable, specified as a byte array.
   * This also takes in a specific character set, as a `Charset`.
   *
   * @param content  the text to create a `CharSource` for
   * @param charset  the charset to build the new CharSource based on
   * @return a new instance of `CharSource`
   */
  def ofContent(content: Array[Byte], charset: Charset): StringCharSource = {
    // bridged for backwards compatibility
    StringCharSource.fromBytes(content, charset)
  }

  //-------------------------------------------------------------------------
  /**
   * Extracts the file name from a source.
   *
   * @param source  the source to extract the file name from
   * @return the file name
   */
  def extractFileName(source: CharSource): String = {
    source match {
      case beanSource: BeanCharSource =>
        beanSource.getFileName().orElse("Unknown.txt")
      case null =>
        // this is used in error messages, so avoid any chance of NPE hiding the real error
        "Null.txt"
      case _ =>
        // handle all other char sources
        val str = source.toString()
        if (str.startsWith("Files.asByteSource(")) {
          // extract the file name from toString()
          val pos = str.indexOf(')', 19)
          Paths.get(str.substring(19, pos)).getFileName().toString()
        } else if (str.startsWith("MoreFiles.asByteSource(")) {
          // extract the path name from toString()
          val pos = str.indexOf(',', 23)
          Paths.get(str.substring(23, pos)).getFileName().toString()
        } else if (str.startsWith("Resources.asByteSource(")) {
          // extract the URI from toString()
          val pos = str.indexOf(')', 23)
          val path = str.substring(23, pos)
          val lastSlash = path.lastIndexOf('/')
          path.substring(lastSlash + 1)
        } else {
          "Unknown.txt"
        }
    }
  }

}
