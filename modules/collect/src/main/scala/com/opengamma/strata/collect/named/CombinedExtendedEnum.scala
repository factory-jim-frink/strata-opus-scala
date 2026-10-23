/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

import java.lang.reflect.Method
import java.util.Optional
import java.util.function.Supplier
import java.util.logging.Logger

import scala.jdk.CollectionConverters.*

import org.joda.convert.RenameHandler

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableList
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.io.IniFile
import com.opengamma.strata.collect.io.PropertySet
import com.opengamma.strata.collect.io.ResourceConfig

/**
 * Combines multiple extended enums into one lookup.
 *
 * Each `ExtendedEnum` is kept separate to ensure fast lookup of the common case.
 * This class uses a configuration file to determine the extended enums to combine.
 *
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 *
 * @tparam T  the type of the enum
 */
final class CombinedExtendedEnum[T <: Named] private (
    rawType: Class[T],
    rawChildren: ImmutableList[ExtendedEnum[? <: T]]) {

  /**
   * The combined enum type.
   */
  private val `type`: Class[T] = ArgChecker.notNull(rawType, "type")
  /**
   * The underlying extended enums.
   */
  private val children: ImmutableList[ExtendedEnum[? <: T]] = ArgChecker.notNull(rawChildren, "children")

  //-------------------------------------------------------------------------
  /**
   * Finds an instance by name.
   *
   * This finds the instance matching the specified name.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   *
   * @param name  the enum name to return
   * @return the named enum
   */
  def find(name: String): Optional[T] = {
    val it = children.iterator()
    while (it.hasNext()) {
      val child = it.next()
      val found = child.find(name).asInstanceOf[Optional[T]]
      if (found.isPresent()) {
        return found
      }
    }
    Optional.empty()
  }

  /**
   * Looks up an instance by name.
   *
   * This finds the instance matching the specified name.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   *
   * @param name  the enum name to return
   * @return the named enum
   * @throws java.lang.IllegalArgumentException if the name is not found
   */
  def lookup(name: String): T = {
    find(name)
        .orElseThrow(() => new IllegalArgumentException(`type`.getSimpleName() + " name not found: " + name))
  }

  //-------------------------------------------------------------------------
  override def toString(): String = {
    "CombinedExtendedEnum[" + `type`.getSimpleName() + "]"
  }

}

object CombinedExtendedEnum {

  /**
   * The logger.
   */
  private val log: Logger = Logger.getLogger(classOf[CombinedExtendedEnum[?]].getName())
  /**
   * Section name used for types.
   */
  private val TYPES_SECTION: String = "types"

  //-------------------------------------------------------------------------
  /**
   * Obtains a combined extended enum instance.
   *
   * Calling this method loads configuration files to determine which extended enums to combine.
   * The configuration file has the same simple name as the specified type and is a
   * INI file with the suffix '.ini'.
   *
   * @tparam R  the type of the enum
   * @param type  the type to load
   * @return the extended enum
   */
  def of[R <: Named](`type`: Class[R]): CombinedExtendedEnum[R] = {
    try {
      // load all matching files
      val name = `type`.getSimpleName() + ".ini"
      val config = ResourceConfig.combinedIniFile(name)
      // parse files
      val children = parseChildren(config, `type`)
      val msg: Supplier[String] = () => "Loaded combined extended enum: " + name + ", providers: " + children
      log.fine(msg)
      new CombinedExtendedEnum[R](`type`, children)

    } catch {
      case ex: RuntimeException =>
        // logging used because this is loaded in a static variable
        log.severe("Failed to load CombinedExtendedEnum for " + `type` + ": " + Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
        new CombinedExtendedEnum[R](`type`, ImmutableList.of())
    }
  }

  // parses the alternate names
  private def parseChildren[R <: Named](
      config: IniFile,
      enumType: Class[R]): ImmutableList[ExtendedEnum[? <: R]] = {

    if (!config.contains(TYPES_SECTION)) {
      return ImmutableList.of()
    }
    val section: PropertySet = config.section(TYPES_SECTION)
    val builder = ImmutableList.builder[ExtendedEnum[? <: R]]()
    for (key <- section.keys().asScala) {
      val cls: Class[?] =
        try {
          RenameHandler.INSTANCE.lookupType(key)
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Unable to find extended enum class: " + key, ex)
        }
      val method: Method =
        try {
          cls.getMethod("extendedEnum")
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Unable to find extendedEnum() method on class: " + cls.getName(), ex)
        }
      if (!method.getReturnType().equals(classOf[ExtendedEnum[?]])) {
        throw new IllegalArgumentException(
            "Method extendedEnum() does not return ExtendedEnum on class: " + cls.getName())
      }
      val result: ExtendedEnum[?] =
        try {
          classOf[ExtendedEnum[?]].cast(method.invoke(null))
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Unable to call extendedEnum() method on class: " + cls.getName(), ex)
        }
      if (!enumType.isAssignableFrom(result.getType())) {
        throw new IllegalArgumentException(
            "Method extendedEnum() returned an ExtendedEnum with an incompatible type on class: " + cls.getName())
      }
      builder.add(result.asInstanceOf[ExtendedEnum[? <: R]])
    }
    builder.build()
  }

}
