/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.regex.Matcher
import java.util.regex.Pattern

import scala.annotation.varargs

import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.result.FailureAttributeKeys
import com.opengamma.strata.collect.tuple.Pair

/**
 * Contains utility methods for managing messages.
 */
final class Messages private ()

object Messages {

  private val REGEX_WORDS_AND_EMPTY: Pattern = Pattern.compile("\\{(\\w*)\\}") //This will match both {}, and {anything}
  private val TEMPLATE_LOCATION_SPLITTER: Splitter = Splitter.on('|')
  private val TEMPLATE_PART_SPLITTER: Splitter = Splitter.on(':')

  //-------------------------------------------------------------------------
  /**
   * Formats a templated message inserting a single argument.
   *
   * This method combines a template message with a single argument.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   *
   * The message template contains zero to many "{}" placeholders.
   * The first placeholder is replaced by the string form of the argument.
   * Subsequent placeholders are not replaced.
   * If there is no placeholder, then the argument is appended to the end of the message.
   * No attempt is made to format the argument.
   *
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   *
   * @param messageTemplate  the message template with "{}" placeholders, null returns empty string
   * @param arg  the message argument, null treated as string "null"
   * @return the formatted message
   */
  def format(messageTemplate: String, arg: Object): String = {
    if (messageTemplate == null) {
      return format("", arg)
    }
    val placeholderPos = messageTemplate.indexOf("{}", 0)
    val argStr = String.valueOf(arg)
    val builder = new java.lang.StringBuilder(messageTemplate.length() + argStr.length() + 3)
    if (placeholderPos >= 0) {
      builder
          .append(messageTemplate.substring(0, placeholderPos))
          .append(argStr)
          .append(messageTemplate.substring(placeholderPos + 2))
    } else {
      builder.append(messageTemplate).append(" - [").append(argStr).append(']')
    }
    builder.toString()
  }

  /**
   * Formats a templated message inserting arguments.
   *
   * This method combines a template message with a list of specific arguments.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   *
   * The message template contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the end of the message.
   * No attempt is made to format the arguments.
   *
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   *
   * @param messageTemplate  the message template with "{}" placeholders, null returns empty string
   * @param args  the message arguments, null treated as empty array
   * @return the formatted message
   */
  @varargs
  def format(messageTemplate: String, args: Object*): String = {
    if (messageTemplate == null) {
      return format("", args*)
    }
    if (args == null) {
      return format(messageTemplate, Seq.empty[Object]*)
    }
    val argsLength = args.length
    // try to make builder big enough for the message and the args
    val builder = new java.lang.StringBuilder(messageTemplate.length() + argsLength * 20)
    // insert placeholders
    var argIndex = 0
    var curPos = 0
    var nextPlaceholderPos = messageTemplate.indexOf("{}", curPos)
    while (nextPlaceholderPos >= 0 && argIndex < argsLength) {
      builder.append(messageTemplate.substring(curPos, nextPlaceholderPos)).append(args(argIndex))
      argIndex += 1
      curPos = nextPlaceholderPos + 2
      nextPlaceholderPos = messageTemplate.indexOf("{}", curPos)
    }
    // append remainder of message template
    builder.append(messageTemplate.substring(curPos))
    // append remaining args
    if (argIndex < argsLength) {
      builder.append(" - [")
      var i = argIndex
      while (i < argsLength) {
        if (i > argIndex) {
          builder.append(", ")
        }
        builder.append(args(i))
        i += 1
      }
      builder.append(']')
    }
    builder.toString()
  }

  /**
   * Formats a templated message inserting named arguments, returning the implied attribute map.
   *
   * A typical template would look like:
   * {{{
   * Messages.formatWithAttributes("Foo={foo}, Bar={}", "abc", 123)
   * }}}
   * This will return a `Pair` with a String and a Map.
   * The String will be the formatted message: `"Foo=abc, Bar=123"`.
   * The Map will look like: `{"foo": "abc"}`.
   *
   * This method combines a template message with a list of specific arguments.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   *
   * The message template contains zero to many "{name}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the message.
   * No attempt is made to format the arguments.
   *
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   *
   * If the template contains a named placeholder, then the output will contain a populated attribute map
   * for all placeholders including those without names. The output will include a 'templateLocation'
   * attribute identifying the location of the named placeholders.
   *
   * @param messageTemplate  the message template with "{}" and "{name}" placeholders, null returns empty string
   * @param args  the message arguments, null treated as empty array
   * @return the formatted message
   */
  @varargs
  def formatWithAttributes(messageTemplate: String, args: Object*): Pair[String, Map[String, String]] = {
    // NOTE: a templateLocation is used (rather than just storing the template) to avoid holding two very similar
    // copies of the message in memory, which could be significant if the message is long

    if (messageTemplate == null) {
      return formatWithAttributes("", args*)
    }
    if (args == null) {
      return formatWithAttributes(messageTemplate)
    }
    val argsLength = args.length

    // do not use an ImmutableMap, as we avoid throwing exceptions in case of duplicate keys.
    val attributes: Map[String, String] = new HashMap[String, String]()
    val matcher: Matcher = REGEX_WORDS_AND_EMPTY.matcher(messageTemplate)
    var argIndex = 0

    val outputMessageBuffer = new StringBuffer(128)
    val templateLocationBuffer = new StringBuffer(32)
    var hasNamed = false
    var previousEnd = 0
    while (matcher.find()) {
      // if the number of placeholders is greater than the number of arguments, then not all placeholders are replaced.
      if (argIndex < argsLength) {
        var attributeName = matcher.group(1)
        val replacement = String.valueOf(args(argIndex))
        outputMessageBuffer.append(messageTemplate.substring(previousEnd, matcher.start()))
        outputMessageBuffer.append(replacement)
        if (!attributeName.isEmpty()) {
          hasNamed = true
          val oldAttrValue = attributes.get(attributeName)
          if (oldAttrValue != null && !oldAttrValue.equals(replacement)) {
            attributeName = "arg" + (argIndex + 1)
          }
          attributes.put(attributeName, replacement)
          // each location is stored as 'name:startPos:length'
          // length is stored to ensure the location does not depend on the attribute values
          templateLocationBuffer.append(if (templateLocationBuffer.length() > 0) "|" else "")
              .append(attributeName)
              .append(':')
              .append(outputMessageBuffer.length() - replacement.length())
              .append(':')
              .append(replacement.length())
        }
        previousEnd = matcher.end()
        argIndex += 1
      }
    }
    outputMessageBuffer.append(messageTemplate.substring(previousEnd))

    // append remaining args
    if (argIndex < argsLength) {
      if (hasNamed) {
        templateLocationBuffer.append(if (templateLocationBuffer.length() > 0) "|" else "")
            .append("+:")
            .append(outputMessageBuffer.length())
      }
      outputMessageBuffer.append(" - [")
      var i = argIndex
      while (i < argsLength) {
        if (i > argIndex) {
          outputMessageBuffer.append(", ")
        }
        outputMessageBuffer.append(args(i))
        i += 1
      }
      outputMessageBuffer.append(']')
    }

    // capture the template if named arguments were used
    if (hasNamed) {
      attributes.put(FailureAttributeKeys.TEMPLATE_LOCATION, templateLocationBuffer.toString())
    } else {
      attributes.clear()
    }
    Pair.of(outputMessageBuffer.toString(), ImmutableMap.copyOf(attributes))
  }

  /**
   * Recreates the template from the message and templateLocation code.
   *
   * This method returns the input message if the input is not valid.
   *
   * @param message  the message, null tolerant
   * @param templateLocation  the template location, null tolerant
   * @return the message
   */
  def recreateTemplate(message: String, templateLocation: String): String = {
    if (Strings.nullToEmpty(message).isEmpty() || Strings.nullToEmpty(templateLocation).isEmpty()) {
      return message
    }
    try {
      val buf = new StringBuffer()
      var lastPos = 0
      val it = TEMPLATE_LOCATION_SPLITTER.split(templateLocation).iterator()
      while (it.hasNext()) {
        val entry = it.next()
        val attrName = Guavate.substringBeforeFirst(entry, ":")
        val remainder = Guavate.substringAfterFirst(entry, ":")
        if (attrName.equals("+")) {
          val pos = Integer.parseInt(remainder)
          buf.append(message.substring(lastPos, pos))
          return buf.toString()
        }
        val pos = Integer.parseInt(Guavate.substringBeforeFirst(remainder, ":"))
        val len = Integer.parseInt(Guavate.substringAfterFirst(remainder, ":"))
        buf.append(message.substring(lastPos, pos)).append('{').append(attrName).append('}')
        lastPos = pos + len
      }
      buf.append(message.substring(lastPos))
      buf.toString()
    } catch {
      case ex: RuntimeException => message
    }
  }

  /**
   * Merges two template locations.
   *
   * This takes two template locations and the length of the first message and returns the combined template.
   *
   * @param location1  the first location, null tolerant
   * @param location2  the second location, null tolerant
   * @param message1Length  the length of the formatted message associated with location1
   * @return the merged location, empty if unable to merge
   */
  def mergeTemplateLocations(location1: String, location2: String, message1Length: Int): String = {
    if (message1Length < 0) {
      return ""
    }
    if (Strings.nullToEmpty(location2).isEmpty()) {
      return Strings.nullToEmpty(location1)
    }
    try {
      // remove any + suffix from first template
      var adjLocation1 = Strings.nullToEmpty(location1)
      adjLocation1 = Guavate.substringBeforeFirst(Strings.nullToEmpty(location1), "|+:")
      adjLocation1 = if (adjLocation1.startsWith("+:")) "" else adjLocation1
      // build the resulting template by appending the adjusted first template, then each part of the second template
      val buf = new java.lang.StringBuilder(64)
      buf.append(adjLocation1)
      val it = TEMPLATE_LOCATION_SPLITTER.split(location2).iterator()
      while (it.hasNext()) {
        val entry = it.next()
        val parts: List[String] = TEMPLATE_PART_SPLITTER.splitToList(entry)
        parts.size() match {
          case 2 =>
            // +:len, need to increase pos
            val attrName = parts.get(0)
            if (!attrName.equals("+")) {
              return ""
            }
            val pos = Integer.parseInt(parts.get(1))
            buf.append(if (buf.length() == 0) "" else "|").append(attrName).append(':').append(pos + message1Length)
          case 3 =>
            // attrName:pos:len, need to increase pos
            val attrName = parts.get(0)
            val pos = Integer.parseInt(parts.get(1))
            val len = Integer.parseInt(parts.get(2))
            buf.append(if (buf.length() == 0) "" else "|").append(attrName).append(':').append(pos + message1Length).append(':').append(len)
          case _ =>
            return ""
        }
      }
      buf.toString()
    } catch {
      case ex: RuntimeException => ""
    }
  }

}
