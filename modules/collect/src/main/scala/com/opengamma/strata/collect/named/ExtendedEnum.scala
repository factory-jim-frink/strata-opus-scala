/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Optional
import java.util.function.Supplier
import java.util.logging.Logger
import java.util.regex.Pattern

import scala.jdk.CollectionConverters.*

import org.joda.convert.RenameHandler

import com.google.common.base.Throwables
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.io.IniFile
import com.opengamma.strata.collect.io.PropertySet
import com.opengamma.strata.collect.io.ResourceConfig
import com.opengamma.strata.collect.tuple.Pair

/**
 * Manager for extended enums controlled by code or configuration.
 *
 * The standard Java `Enum` is a fixed set of constants defined at compile time.
 * In many scenarios this can be too limiting and this class provides an alternative.
 *
 * An INI configuration file is used to define the set of named instances.
 * For more information on the process of loading the configuration file, see `ResourceConfig`.
 *
 * The named instances are loaded via provider classes.
 * A provider class is either an implementation of `NamedLookup` or a class
 * providing `public static final` enum constants.
 *
 * The configuration file also supports the notion of alternate names (aliases).
 * This allows many different names to be used to lookup the same instance.
 *
 * Three sections control the loading of additional information.
 *
 * The 'providers' section contains a number of properties, one for each provider.
 * The key is the full class name of the provider.
 * The value is 'constants', 'lookup' or 'instance', and is used to obtain a `NamedLookup` instance.
 * A 'constants' provider must contain public static constants of the correct type,
 * which will be reflectively located and wrapped in a `NamedLookup`.
 * A 'lookup' provider must implement `NamedLookup` and have a no-args constructor.
 * An 'instance' provider must have a static variable named "INSTANCE" of type `NamedLookup`.
 *
 * The 'alternates' section contains a number of properties, one for each alternate name.
 * The key is the alternate name, the value is the standard name.
 * Alternate names are used when looking up an extended enum.
 *
 * The 'externals' sections contains a number of properties intended to allow external enum names to be mapped.
 * Unlike 'alternates', which are always included, 'externals' are only included when requested.
 * There may be multiple external <i>groups</i> to handle different external providers of data.
 * For example, the mapping used by FpML may differ from that used by Bloomberg.
 *
 * Each 'externals' section has a name of the form 'externals.Foo', where 'Foo' is the name of the group.
 * Each property line in the section is of the same format as the 'alternates' section.
 * It maps the external name to the standard name.
 *
 * It is intended that this class is used as a helper class to load the configuration
 * and manage the map of names to instances. It should be created and used by the author
 * of the main abstract extended enum class, and not be application developers.
 *
 * @tparam T  the type of the enum
 */
final class ExtendedEnum[T <: Named] private (
    rawType: Class[T],
    rawLookups: ImmutableList[NamedLookup[T]],
    rawAlternateNames: ImmutableMap[String, String],
    rawExternalNames: ImmutableMap[String, ImmutableMap[String, String]],
    rawLenientRegex: ImmutableList[Pair[Pattern, String]]) {

  /**
   * The enum type.
   */
  private val `type`: Class[T] = ArgChecker.notNull(rawType, "type")
  /**
   * The lookup functions defining the standard names.
   */
  private val lookups: ImmutableList[NamedLookup[T]] = ArgChecker.notNull(rawLookups, "lookups")
  /**
   * Returns the complete map of alternate name to standard name.
   *
   * The map is keyed by the alternate name.
   *
   * @return the map of alternate names
   */
  val alternateNames: ImmutableMap[String, String] = ArgChecker.notNull(rawAlternateNames, "alternateNames")
  /**
   * The map of external names, keyed by the group name.
   * The first map holds groups of external names.
   * The inner map holds the mapping from external name to our name.
   */
  private val externalNameGroupMap: ImmutableMap[String, ImmutableMap[String, String]] =
    ArgChecker.notNull(rawExternalNames, "externalNames")
  /**
   * The list of regex patterns for lenient lookup.
   */
  private val lenientRegex: ImmutableList[Pair[Pattern, String]] = ArgChecker.notNull(rawLenientRegex, "lenientRegex")

  //-------------------------------------------------------------------------
  /**
   * Gets the enum type.
   *
   * @return the enum type
   */
  def getType(): Class[T] = {
    `type`
  }

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
    val standardName = alternateNames.getOrDefault(name, name)
    val it = lookups.iterator()
    while (it.hasNext()) {
      val instance = it.next().lookup(standardName)
      if (instance != null) {
        return Optional.of(instance)
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
    val standardName = alternateNames.getOrDefault(name, name)
    val it = lookups.iterator()
    while (it.hasNext()) {
      val instance = it.next().lookup(standardName)
      if (instance != null) {
        return instance
      }
    }
    throw new IllegalArgumentException(`type`.getSimpleName() + " name not found: " + name)
  }

  /**
   * Looks up an instance by name and type.
   *
   * This finds the instance matching the specified name, ensuring it is of the specified type.
   * Instances may have alternate names (aliases), thus the returned instance
   * may have a name other than that requested.
   *
   * @tparam S  the enum subtype
   * @param subtype  the enum subtype to match
   * @param name  the enum name to return
   * @return the named enum
   * @throws java.lang.IllegalArgumentException if the name is not found or has the wrong type
   */
  def lookup[S <: T](name: String, subtype: Class[S]): S = {
    val result = lookup(name)
    if (!subtype.isInstance(result)) {
      throw new IllegalArgumentException(`type`.getSimpleName() + " name found but did not match expected type: " + name)
    }
    subtype.cast(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the map of known instances by name.
   *
   * This method returns all known instances.
   * It is permitted for an enum provider implementation to return an empty map,
   * thus the map may not be complete.
   * The map may include instances keyed under an alternate name, such as names
   * in upper case, however it will not include the base set of
   * alternate names.
   *
   * @return the map of enum instance by name
   */
  def lookupAll(): ImmutableMap[String, T] = {
    val map: Map[String, T] = new HashMap[String, T]()
    val it = lookups.iterator()
    while (it.hasNext()) {
      val lookupMap: Map[String, T] = it.next().lookupAll()
      val entryIt = lookupMap.entrySet().iterator()
      while (entryIt.hasNext()) {
        val entry = entryIt.next()
        map.putIfAbsent(entry.getKey(), entry.getValue())
      }
    }
    ImmutableMap.copyOf(map)
  }

  /**
   * Returns the map of known instances by normalized name.
   *
   * This method returns all known instances, keyed by the normalized name.
   * This is equivalent to the result of `lookupAll()` adjusted such
   * that each entry is keyed by the result of `Named#getName()`.
   *
   * @return the map of enum instance by name
   */
  def lookupAllNormalized(): ImmutableMap[String, T] = {
    // add values that are keyed under the normalized name
    // keep values keyed under a non-normalized name
    val result: Map[String, T] = new HashMap[String, T]()
    val others: Map[String, T] = new HashMap[String, T]()
    val it = lookupAll().entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      val normalizedName = entry.getValue().getName()
      if (entry.getKey().equals(normalizedName)) {
        result.put(normalizedName, entry.getValue())
      } else {
        others.put(normalizedName, entry.getValue())
      }
    }
    // include any values that are only keyed under a non-normalized name
    val valueIt = others.values().iterator()
    while (valueIt.hasNext()) {
      val v = valueIt.next()
      result.putIfAbsent(v.getName(), v)
    }
    ImmutableMap.copyOf(result)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of groups that have external names defined.
   *
   * External names are used to map names used by external systems to the standard name used here.
   * There can be multiple groups of mappings to external systems,
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   *
   * @return the set of groups that have external names
   */
  def externalNameGroups(): ImmutableSet[String] = {
    externalNameGroupMap.keySet()
  }

  /**
   * Returns the mapping of external names to standard names for a group.
   *
   * External names are used to map names used by external systems to the standard name used here.
   * There can be multiple groups of mappings to external systems,
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   *
   * The result provides mapping between the external name and the standard name.
   *
   * @param group  the group name to find external names for
   * @return the map of external names for the group
   * @throws java.lang.IllegalArgumentException if the group is not found
   */
  def externalNames(group: String): ExtendedEnum.ExternalEnumNames[T] = {
    val externals = externalNameGroupMap.get(group)
    if (externals == null) {
      throw new IllegalArgumentException(`type`.getSimpleName() + " group not found: " + group)
    }
    new ExtendedEnum.ExternalEnumNames[T](this, group, externals)
  }

  //-------------------------------------------------------------------------
  /**
   * Looks up an instance by name leniently.
   *
   * This finds the instance matching the specified name using a lenient lookup strategy.
   * An extended enum may include additional configuration defining how lenient search occurs.
   *
   * @param name  the enum name to return
   * @return the named enum
   * @throws java.lang.IllegalArgumentException if the name is not found
   */
  def findLenient(name: String): Optional[T] = {
    val alreadyValid = find(name)
    if (alreadyValid.isPresent()) {
      return alreadyValid
    }
    var current = name.toUpperCase(Locale.ENGLISH)
    val it = lenientRegex.iterator()
    while (it.hasNext()) {
      val pair = it.next()
      val matcher = pair.getFirst().matcher(current)
      if (matcher.matches()) {
        current = matcher.replaceFirst(pair.getSecond())
      }
    }
    find(current)
  }

  //-------------------------------------------------------------------------
  override def toString(): String = {
    "ExtendedEnum[" + `type`.getSimpleName() + "]"
  }

}

object ExtendedEnum {

  /**
   * The logger.
   */
  private val log: Logger = Logger.getLogger(classOf[ExtendedEnum[?]].getName())
  /**
   * Section name used for providers.
   */
  private val PROVIDERS_SECTION: String = "providers"
  /**
   * Section name used for alternates.
   */
  private val ALTERNATES_SECTION: String = "alternates"
  /**
   * Section name used for externals.
   */
  private val EXTERNALS_SECTION: String = "externals."
  /**
   * Section name used for lenient patterns.
   */
  private val LENIENT_PATTERNS_SECTION: String = "lenientPatterns"

  //-------------------------------------------------------------------------
  /**
   * Obtains an extended enum instance.
   *
   * Calling this method loads configuration files to determine the extended enum values.
   * The configuration file has the same simple name as the specified type and is a
   * INI file with the suffix '.ini'.
   * See class-level documentation for more information.
   *
   * @tparam R  the type of the enum
   * @param type  the type to load
   * @return the extended enum
   */
  def of[R <: Named](`type`: Class[R]): ExtendedEnum[R] = {
    try {
      // load all matching files
      val name = `type`.getSimpleName() + ".ini"
      val config = ResourceConfig.combinedIniFile(name)
      // parse files
      val lookups = parseProviders(config, `type`)
      val alternateNames = parseAlternates(config)
      val externalNames = parseExternals(config)
      val lenientRegex = parseLenientPatterns(config)
      val msg: Supplier[String] = () => "Loaded extended enum: " + name + ", providers: " + lookups
      log.fine(msg)
      new ExtendedEnum[R](`type`, lookups, alternateNames, externalNames, lenientRegex)

    } catch {
      case ex: RuntimeException =>
        // logging used because this is loaded in a static variable
        log.severe("Failed to load ExtendedEnum for " + `type` + ": " + Throwables.getStackTraceAsString(ex))
        // return an empty instance to avoid ExceptionInInitializerError
        new ExtendedEnum[R](`type`, ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of())
    }
  }

  // parses the alternate names
  private def parseProviders[R <: Named](
      config: IniFile,
      enumType: Class[R]): ImmutableList[NamedLookup[R]] = {

    if (!config.contains(PROVIDERS_SECTION)) {
      return ImmutableList.of()
    }
    val section: PropertySet = config.section(PROVIDERS_SECTION)
    val builder = ImmutableList.builder[NamedLookup[R]]()
    for (key <- section.keys().asScala) {
      val cls: Class[?] =
        try {
          RenameHandler.INSTANCE.lookupType(key)
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Unable to find enum provider class: " + key, ex)
        }
      val value = section.value(key)
      if (value.equals("constants")) {
        // extract public static final constants
        builder.add(parseConstants(enumType, cls))

      } else if (value.equals("lookup")) {
        // class is a named lookup
        if (!classOf[NamedLookup[?]].isAssignableFrom(cls)) {
          throw new IllegalArgumentException("Enum provider class must implement NamedLookup " + cls.getName())
        }
        try {
          val cons: Constructor[?] = cls.getDeclaredConstructor()
          if (!Modifier.isPublic(cls.getModifiers())) {
            cons.setAccessible(true)
          }
          builder.add(cons.newInstance().asInstanceOf[NamedLookup[R]])
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Invalid enum provider constructor: new " + cls.getName() + "()", ex)
        }

      } else if (value.equals("instance")) {
        // class has a named lookup INSTANCE static field
        try {
          val field: Field = cls.getDeclaredField("INSTANCE")
          if (!Modifier.isStatic(field.getModifiers()) || !classOf[NamedLookup[?]].isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Invalid enum provider instance: " + cls.getName() + ".INSTANCE")
          }
          if (!Modifier.isPublic(cls.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
            field.setAccessible(true)
          }
          builder.add(field.get(null).asInstanceOf[NamedLookup[R]])
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Invalid enum provider instance: " + cls.getName() + ".INSTANCE", ex)
        }

      } else {
        throw new IllegalArgumentException("Provider value must be either 'constants' or 'lookup'")
      }
    }
    builder.build()
  }

  // parses the public static final constants
  // Scala 3 enum cases compile to public static fields that are not final, so a public static
  // field of an enum class that holds one of its own enum constants is also accepted
  private def parseConstants[R <: Named](enumType: Class[R], constantsType: Class[?]): NamedLookup[R] = {
    val fields: Array[Field] = constantsType.getDeclaredFields()
    val instances: Map[String, R] = new HashMap[String, R]()
    for (field <- fields) {
      val modifiers = field.getModifiers()
      val isFinal = Modifier.isFinal(modifiers)
      if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) &&
          (isFinal || constantsType.isEnum()) && enumType.isAssignableFrom(field.getType())) {
        if (Modifier.isPublic(constantsType.getModifiers()) == false) {
          field.setAccessible(true)
        }
        try {
          val value = field.get(null)
          if (isFinal || isEnumConstant(constantsType, value)) {
            val instance: R = enumType.cast(value)
            instances.putIfAbsent(instance.getName(), instance)
            instances.putIfAbsent(instance.getName().toUpperCase(Locale.ENGLISH), instance)
          }
        } catch {
          case ex: Exception =>
            throw new IllegalArgumentException("Unable to query field: " + field, ex)
        }
      }
    }
    val constants: ImmutableMap[String, R] = ImmutableMap.copyOf(instances)
    new NamedLookup[R] {
      override def lookupAll(): ImmutableMap[String, R] = {
        constants
      }
    }
  }

  // checks whether the value is one of the enum constants of the enum type
  private def isEnumConstant(enumClass: Class[?], value: Object): Boolean = {
    val enumConstants = enumClass.getEnumConstants()
    enumConstants != null && enumConstants.exists(constant => constant.asInstanceOf[AnyRef] eq value)
  }

  // parses the alternate names
  private def parseAlternates(config: IniFile): ImmutableMap[String, String] = {
    if (!config.contains(ALTERNATES_SECTION)) {
      return ImmutableMap.of()
    }
    val alternates: Map[String, String] = new HashMap[String, String]()
    val it = config.section(ALTERNATES_SECTION).asMap().entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      alternates.put(entry.getKey(), entry.getValue())
      alternates.putIfAbsent(entry.getKey().toUpperCase(Locale.ENGLISH), entry.getValue())
    }
    ImmutableMap.copyOf(alternates)
  }

  // parses the external names
  private def parseExternals(config: IniFile): ImmutableMap[String, ImmutableMap[String, String]] = {
    val builder = ImmutableMap.builder[String, ImmutableMap[String, String]]()
    for (sectionName <- config.sections().asScala) {
      if (sectionName.startsWith(EXTERNALS_SECTION)) {
        val group = sectionName.substring(EXTERNALS_SECTION.length())
        builder.put(group, config.section(sectionName).asMap())
      }
    }
    builder.build()
  }

  // parses the lenient patterns
  private def parseLenientPatterns(config: IniFile): ImmutableList[Pair[Pattern, String]] = {
    if (!config.contains(LENIENT_PATTERNS_SECTION)) {
      return ImmutableList.of()
    }
    val alternates: List[Pair[Pattern, String]] = new ArrayList[Pair[Pattern, String]]()
    val it = config.section(LENIENT_PATTERNS_SECTION).asMap().entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      alternates.add(Pair.of(Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE), entry.getValue()))
    }
    ImmutableList.copyOf(alternates)
  }

  //-------------------------------------------------------------------------
  /**
   * Maps names used by external systems to the standard name used here.
   *
   * A frequent problem in parsing external file formats is converting enum values.
   * This class provides a suitable mapping, allowing multiple external names to map to one standard name.
   *
   * A single instance represents the mapping for a single external group.
   * This allows the mapping for different groups to differ.
   * For example, the mapping used by FpML may differ from that used by Bloomberg.
   *
   * Instances of this class are configured via INI files and provided via `ExtendedEnum`.
   *
   * @tparam T  the type of the enum
   */
  final class ExternalEnumNames[T <: Named] private[named] (
      private val extendedEnum: ExtendedEnum[T],
      private val group: String,
      /**
       * Returns the complete map of external name to standard name.
       *
       * The map is keyed by the external name.
       *
       * @return the map of external names
       */
      val externalNames: ImmutableMap[String, String]) {

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
      val standardName = externalNames.getOrDefault(name, name)
      try {
        extendedEnum.lookup(standardName)
      } catch {
        case ex: IllegalArgumentException =>
          throw new IllegalArgumentException(Messages.format(
              "{}:{} unable to find external name: {}", extendedEnum.getType().getSimpleName(), group, name))
      }
    }

    /**
     * Looks up an instance by name and type.
     *
     * This finds the instance matching the specified name, ensuring it is of the specified type.
     * Instances may have alternate names (aliases), thus the returned instance
     * may have a name other than that requested.
     *
     * @tparam S  the enum subtype
     * @param subtype  the enum subtype to match
     * @param name  the enum name to return
     * @return the named enum
     * @throws java.lang.IllegalArgumentException if the name is not found or has the wrong type
     */
    def lookup[S <: T](name: String, subtype: Class[S]): S = {
      val result = lookup(name)
      if (!subtype.isInstance(result)) {
        throw new IllegalArgumentException(Messages.format(
            "{}:{} external name found but did not match expected type: {}",
            extendedEnum.getType().getSimpleName(),
            group,
            name))
      }
      subtype.cast(result)
    }

    /**
     * Looks up the external name given a standard enum instance.
     *
     * This searches the map of external names and returns the first matching entry
     * that maps to the given standard name.
     *
     * @param namedEnum  the named enum to find an external name for
     * @return the external name
     * @throws java.lang.IllegalArgumentException if there is no external name
     */
    def reverseLookup(namedEnum: T): String = {
      val name = namedEnum.getName()
      val it = externalNames.entrySet().iterator()
      while (it.hasNext()) {
        val entry = it.next()
        if (entry.getValue().equals(name)) {
          return entry.getKey()
        }
      }
      throw new IllegalArgumentException(Messages.format(
          "{}:{} external name not found for standard name: {}", extendedEnum.getType().getSimpleName(), group, name))
    }

    //-------------------------------------------------------------------------
    override def toString(): String = {
      "ExternalEnumNames[" + extendedEnum.getType().getSimpleName() + ":" + group + "]"
    }
  }

}
