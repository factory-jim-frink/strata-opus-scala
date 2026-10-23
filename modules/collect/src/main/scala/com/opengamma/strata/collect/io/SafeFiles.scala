/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.util.List
import java.util.function.Function
import java.util.stream.Stream

import scala.annotation.varargs

import com.opengamma.strata.collect.Guavate

/**
 * Provides methods to operate on files using `Path` that avoid leaking file handles.
 */
final class SafeFiles private ()

object SafeFiles {

  //-------------------------------------------------------------------------
  /**
   * Lists the elements in the specified directory without recursing into subdirectories.
   *
   * This is a safe wrapper for `Files.list(Path)`.
   *
   * @param dir  the path to list files
   * @return the list of paths in the specified directory
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def listAll(dir: Path): List[Path] = {
    list[List[Path]](dir, stream => stream.collect(Guavate.toImmutableList[Path]()))
  }

  /**
   * Streams the elements in the specified directory without recursing into subdirectories.
   *
   * This is a safe wrapper for `Files.list(Path)`.
   *
   * @tparam T  the type of the result
   * @param dir  the path to list files
   * @param function  the function to apply to the stream
   * @return the result of the function
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def list[T](dir: Path, function: Function[Stream[Path], T]): T = {
    try {
      applyAndClose(Files.list(dir), function)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   *
   * This is a safe wrapper for `Files.walk(Path, FileVisitOption...)`.
   * It recurses into all subdirectories and does not follow links.
   *
   * @param dir  the start path to list files from
   * @return the list of paths in the specified directory
   * @throws java.lang.IllegalArgumentException if maxDepth is negative
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def walkAll(dir: Path): List[Path] = {
    walk[List[Path]](dir, Integer.MAX_VALUE, stream => stream.collect(Guavate.toImmutableList[Path]()))
  }

  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   *
   * This is a safe wrapper for `Files.walk(Path, int, FileVisitOption...)`.
   *
   * @param dir  the start path to list files from
   * @param maxDepth  the maximum depth to recurse to, one for one-level and so on
   * @param options  the options
   * @return the list of paths in the specified directory
   * @throws java.lang.IllegalArgumentException if maxDepth is negative
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  @varargs
  def walkAll(dir: Path, maxDepth: Int, options: FileVisitOption*): List[Path] = {
    walk[List[Path]](dir, maxDepth, stream => stream.collect(Guavate.toImmutableList[Path]()), options*)
  }

  /**
   * Lists the elements in the specified directory, recursing depth-first into subdirectories.
   *
   * This is a safe wrapper for `Files.walk(Path, int, FileVisitOption...)`.
   *
   * @tparam T  the type of the result
   * @param dir  the start path to list files from
   * @param maxDepth  the maximum depth to recurse to, one for one-level and so on
   * @param function  the function to apply to the stream
   * @param options  the options
   * @return the result of the function
   * @throws java.lang.IllegalArgumentException if maxDepth is negative
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  @varargs
  def walk[T](dir: Path, maxDepth: Int, function: Function[Stream[Path], T], options: FileVisitOption*): T = {
    try {
      applyAndClose(Files.walk(dir, maxDepth, options*), function)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the specified file as a list of lines using UTF-8.
   *
   * This is a safe wrapper for `Files.lines(Path)`.
   *
   * @param file  the path to read
   * @return the list of lines in the specified file
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def linesAll(file: Path): List[String] = {
    lines[List[String]](file, stream => stream.collect(Guavate.toImmutableList[String]()))
  }

  /**
   * Streams the lines in the specified file using UTF-8.
   *
   * This is a safe wrapper for `Files.lines(Path)`.
   *
   * @tparam T  the type of the result
   * @param file  the path to read
   * @param function  the function to apply to the stream
   * @return the result of the function
   * @throws java.lang.SecurityException if access is rejected by a security manager
   * @throws java.io.UncheckedIOException if an IO error occurs
   */
  def lines[T](file: Path, function: Function[Stream[String], T]): T = {
    try {
      applyAndClose(Files.lines(file), function)
    } catch {
      case ex: IOException => throw new UncheckedIOException(ex)
    }
  }

  // equivalent of try-with-resources, adding any close failure as suppressed to the primary failure
  private def applyAndClose[S <: Stream[?], T](stream: S, function: Function[S, T]): T = {
    var primary: Throwable = null
    try {
      function.apply(stream)
    } catch {
      case ex: Throwable =>
        primary = ex
        throw ex
    } finally {
      if (stream != null) {
        if (primary != null) {
          try {
            stream.close()
          } catch {
            case closeEx: Throwable => primary.addSuppressed(closeEx)
          }
        } else {
          stream.close()
        }
      }
    }
  }

}
