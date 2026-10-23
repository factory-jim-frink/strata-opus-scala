/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.function.BiFunction
import java.util.function.Supplier
import java.util.logging.Logger

import scala.annotation.static

import com.google.common.base.Splitter
import com.google.common.collect.ImmutableList
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * Provides access to configuration files.
 *
 * A standard approach to configuration is provided by this class.
 * Any configuration information provided by this library can be overridden or added to by applications.
 *
 * By default, there are three groups of recognized configuration directories:
 *
 *  - base
 *  - library
 *  - application
 *
 * Each group consists of ten directories using a numeric suffix:
 *
 *  - `com/opengamma/strata/config/base`
 *  - `com/opengamma/strata/config/base1`
 *  - `com/opengamma/strata/config/base2`
 *  - ...
 *  - `com/opengamma/strata/config/base9`
 *  - `com/opengamma/strata/config/library`
 *  - `com/opengamma/strata/config/library1`
 *  - ...
 *  - `com/opengamma/strata/config/library9`
 *  - `com/opengamma/strata/config/application`
 *  - `com/opengamma/strata/config/application1`
 *  - ...
 *  - `com/opengamma/strata/config/application9`
 *
 * These form a complete set of thirty directories that are searched for configuration.
 *
 * The search strategy looks for the same file name in each of the thirty directories.
 * All the files that are found are then merged, with directories lower down the list taking priorty.
 * Thus, any configuration file in the 'application9' directory will override the same file
 * in the 'appication1' directory, which will override the same file in the 'library' group,
 * which will further override the same file in the 'base' group.
 *
 * The 'base' group is reserved for Strata.
 * The 'library' group is reserved for libraries built directly on Strata.
 *
 * The set of configuration directories can be changed using the system property
 * 'com.opengamma.strata.config.directories'.
 * This must be a comma separated list, such as 'base,base1,base2,override,application'.
 *
 * In general, the configuration managed by this class will be in INI format.
 * The `combinedIniFile(String)` method is the main entry point, returning a single
 * INI file merged from all available configuration files.
 */
final class ResourceConfig private ()

// the private state lives in the companion so that it is initialized on first use of the methods,
// not when the public constant is read, as the Java compile-time constant did not initialize the class
object ResourceConfig {

  /**
   * The system property defining the comma separated list of groups.
   */
  @static final val RESOURCE_DIRS_PROPERTY: String = "com.opengamma.strata.config.directories"
  /**
   * The logger.
   */
  private val log: Logger = Logger.getLogger(classOf[ResourceConfig].getName())
  /**
   * The package/folder location for the configuration.
   */
  private val CONFIG_PACKAGE: String = "META-INF/com/opengamma/strata/config/"
  /**
   * The default set of directories to query configuration files in.
   */
  private val DEFAULT_DIRS: ImmutableList[String] = ImmutableList.of(
      "base",
      "base1",
      "base2",
      "base3",
      "base4",
      "base5",
      "base6",
      "base7",
      "base8",
      "base9",
      "library",
      "library1",
      "library2",
      "library3",
      "library4",
      "library5",
      "library6",
      "library7",
      "library8",
      "library9",
      "application",
      "application1",
      "application2",
      "application3",
      "application4",
      "application5",
      "application6",
      "application7",
      "application8",
      "application9")
  /**
   * The resource groups.
   * Always falls back to the known set in case of error.
   */
  private val RESOURCE_DIRS: ImmutableList[String] = {
    var dirs: List[String] = DEFAULT_DIRS
    var property: String = null
    try {
      property = System.getProperty(RESOURCE_DIRS_PROPERTY)
    } catch {
      case ex: Exception =>
        log.warning("Unable to access system property: " + ex.toString())
    }
    if (property != null && !property.isEmpty()) {
      try {
        dirs = Splitter.on(',').trimResults().splitToList(property)
      } catch {
        case ex: Exception =>
          log.warning("Invalid system property: " + property + ": " + ex.toString())
      }
      val it = dirs.iterator()
      while (it.hasNext()) {
        val dir = it.next()
        if (!dir.matches("[A-Za-z0-9-]+")) {
          log.warning("Invalid system property directory, must match regex [A-Za-z0-9-]+: " + dir)
        }
      }
    }
    log.config("Using directories: " + dirs)
    ImmutableList.copyOf(dirs)
  }
  /**
   * INI section name used for chaining.
   */
  private val CHAIN_SECTION: String = "chain"
  /**
   * INI property name used for chaining.
   */
  private val CHAIN_NEXT: String = "chainNextFile"
  /**
   * INI property name used for removing sections.
   */
  private val CHAIN_REMOVE: String = "chainRemoveSections"

  //-------------------------------------------------------------------------
  /**
   * Returns a combined INI file formed by merging INI files with the specified name.
   *
   * This finds the all files with the specified name in the configuration directories.
   * Each file is loaded, with the result being formed by merging the files into one.
   * See `combinedIniFile(List)` for more details on the merge process.
   *
   * @param resourceName  the resource name
   * @return the resource locators
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalStateException if there is a configuration error
   */
  def combinedIniFile(resourceName: String): IniFile = {
    ArgChecker.notNull(resourceName, "resourceName")
    ResourceConfig.combinedIniFile(ResourceConfig.orderedResources(resourceName))
  }

  /**
   * Returns a combined INI file formed by merging the specified INI files.
   *
   * The result of this method is formed by merging the specified files together.
   * The files are combined in order forming a chain.
   * The first file in the list has the lowest priority.
   * The last file in the list has the highest priority.
   *
   * The algorithm starts with all the sections and properties from the highest priority file.
   * It then adds any sections or properties from subsequent files that are not already present.
   *
   * The algorithm can be controlled by providing a '[chain]' section.
   * Within the 'chain' section, if 'chainNextFile' is 'false', then processing stops,
   * and lower priority files are ignored. If the 'chainRemoveSections' property is specified,
   * the listed sections are ignored from the files lower in the chain.
   *
   * @param resources  the INI file resources to read
   * @return the combined chained INI file
   * @throws java.io.UncheckedIOException if an IO error occurs
   * @throws java.lang.IllegalArgumentException if the configuration is invalid
   */
  def combinedIniFile(resources: List[ResourceLocator]): IniFile = {
    ArgChecker.notNull(resources, "resources")
    val sectionMap: Map[String, PropertySet] = new LinkedHashMap[String, PropertySet]()
    val merger: BiFunction[PropertySet, PropertySet, PropertySet] = (base, other) => base.overrideWith(other)
    val it = resources.iterator()
    while (it.hasNext()) {
      val resource = it.next()
      val file = IniFile.of(resource.getCharSource())
      if (file.contains(CHAIN_SECTION)) {
        val chainSection = file.section(CHAIN_SECTION)
        // remove everything from lower priority files if not chaining
        if (chainSection.contains(CHAIN_NEXT) && java.lang.Boolean.parseBoolean(chainSection.value(CHAIN_NEXT)) == false) {
          sectionMap.clear()
        } else {
          // remove sections from lower priority files
          sectionMap.keySet().removeAll(chainSection.valueList(CHAIN_REMOVE))
        }
      }
      // add entries, replacing existing data
      val sectionIt = file.asMap().keySet().iterator()
      while (sectionIt.hasNext()) {
        val sectionName = sectionIt.next()
        if (!sectionName.equals(CHAIN_SECTION)) {
          sectionMap.merge(sectionName, file.section(sectionName), merger)
        }
      }
    }
    IniFile.of(sectionMap)
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an ordered list of resource locators.
   *
   * This finds the all files with the specified name in the configuration directories.
   * The result is ordered from the lowest priority (base) file to the highest priority (application) file.
   * The result will always contain at least one file, but it may contain more than one.
   *
   * @param resourceName  the resource name
   * @return the resource locators
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalStateException if there is a configuration error
   */
  def orderedResources(resourceName: String): List[ResourceLocator] = {
    ArgChecker.notNull(resourceName, "resourceName")
    val finder: CheckedSupplier[List[ResourceLocator]] = () => orderedResources0(resourceName)
    Unchecked.wrap(finder)
  }

  // find the list of resources
  @throws[IOException]
  private def orderedResources0(classpathResourceName: String): List[ResourceLocator] = {
    val classLoader = ResourceLocator.classLoader()
    val names: List[String] = new ArrayList[String]()
    val result: List[ResourceLocator] = new ArrayList[ResourceLocator]()
    val dirIt = RESOURCE_DIRS.iterator()
    while (dirIt.hasNext()) {
      val dir = dirIt.next()
      val name = CONFIG_PACKAGE + dir + "/" + classpathResourceName
      names.add(name)
      val urls: List[URL] = Collections.list(classLoader.getResources(name))
      urls.size() match {
        case 0 =>
          // continue
        case 1 =>
          result.add(ResourceLocator.ofClasspathUrl(urls.get(0)))
        case _ =>
          // handle case where Strata is on the classpath more than once
          // only accept this if the data being read is the same in all URLs
          val baseResource = ResourceLocator.ofClasspathUrl(urls.get(0))
          var i = 1
          while (i < urls.size()) {
            val otherResource = ResourceLocator.ofClasspathUrl(urls.get(i))
            if (!baseResource.getByteSource().contentEquals(otherResource.getByteSource())) {
              log.severe("More than one file found on the classpath: " + name + ": " + urls)
              throw new IllegalStateException("More than one file found on the classpath: " + name + ": " + urls)
            }
            i += 1
          }
          result.add(baseResource)
      }
    }
    if (result.isEmpty()) {
      log.severe("No resource files found on the classpath: " + names)
      throw new IllegalStateException("No files found on the classpath: " + names)
    }
    val message: Supplier[String] = () => "Resources found: " + result
    log.config(message)
    result
  }

}
