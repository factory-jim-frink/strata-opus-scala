/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.io.InputStream
import java.util.HashMap
import java.util.Map
import java.util.function.ToIntFunction

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.io.ByteSource
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedSupplier

/**
 * An XML file.
 *
 * Represents an XML file together with the ability to parse it from a `ByteSource`.
 *
 * This uses the standard StAX API to parse the file.
 * Once parsed, the XML is represented as a DOM-like structure, see `XmlElement`.
 * This approach is suitable for XML files where the size of the parsed XML file is
 * known to be manageable in memory.
 *
 * Note that the `XmlElement` representation does not express all XML features.
 * No support is provided for processing instructions, comments or mixed content.
 * In addition, it is not possible to determine the difference between empty content and no children.
 *
 * There is no support for namespaces.
 * All namespace prefixes are dropped.
 * There are cases where this can be a problem, but most of the time lenient parsing is helpful.
 */
final class XmlFile private (rawRoot: XmlElement, rawRefs: Map[String, XmlElement]) {

  /**
   * The root element.
   */
  private val root: XmlElement = ArgChecker.notNull(rawRoot, "root")
  /**
   * The map of references.
   */
  private val refs: ImmutableMap[String, XmlElement] = ImmutableMap.copyOf(rawRefs)

  //-------------------------------------------------------------------------
  /**
   * Gets the root element of this file.
   *
   * @return the root element
   */
  def getRoot(): XmlElement = {
    root
  }

  /**
   * Gets the reference map of id to element.
   *
   * This is used to decode references, such as an id/href pair.
   *
   * For example, if one part of the XML has `<foo id="fooId">`, the map will
   * contain an entry mapping "fooId" to the parsed element `<foo>`.
   *
   * @return the map of id to element
   */
  def getReferences(): ImmutableMap[String, XmlElement] = {
    refs
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this file equals another.
   *
   * The comparison checks the content and reference map.
   *
   * @param obj  the other section, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: XmlFile =>
          root.equals(other.root) && refs.equals(other.refs)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code for the file.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    root.hashCode() ^ refs.hashCode()
  }

  /**
   * Returns a string describing the file.
   *
   * @return the descriptive string
   */
  override def toString(): String = {
    root.toString()
  }

}

object XmlFile {

  // the XML factory
  // originally recreated each time because of JDK-8028111, but use of Java 8u20 and Java 9+ now widespread
  private val XML_FACTORY: XMLInputFactory = {
    // see https://bugs.openjdk.java.net/browse/JDK-8183519 where JDK deprecated the wrong method
    // to avoid a warning on 9 this code uses newInstance() even though newFactory() is more correct
    // there is no difference in behavior between the two methods
    val factory = XMLInputFactory.newInstance()
    factory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, java.lang.Boolean.TRUE)
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, java.lang.Boolean.FALSE)
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, java.lang.Boolean.FALSE)
    factory
  }

  //-----------------------------------------------------------------------
  /**
   * Parses the specified source as an XML file to an in-memory DOM-like structure.
   *
   * This parses the specified byte source expecting an XML file format.
   * The resulting instance can be queried for the root element.
   *
   * @param source  the XML source data
   * @return the parsed file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: ByteSource): XmlFile = {
    of(source, "")
  }

  /**
   * Parses the specified source as an XML file to an in-memory DOM-like structure.
   *
   * This parses the specified byte source expecting an XML file format.
   * The resulting instance can be queried for the root element.
   *
   * This supports capturing attribute references, such as an id/href pair.
   * Wherever the parser finds an attribute with the specified name, the element is added
   * to the internal map, accessible by calling `getReferences()`.
   *
   * For example, if one part of the XML has `<foo id="fooId">`, the references map will
   * contain an entry mapping "fooId" to the parsed element `<foo>`.
   *
   * @param source  the XML source data
   * @param refAttrName  the attribute name that should be parsed as a reference
   * @return the parsed file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def of(source: ByteSource, refAttrName: String): XmlFile = {
    ArgChecker.notNull(source, "source")
    parseSource(source, xmlReader => {
      val refs = new HashMap[String, XmlElement]()
      val root = parse(xmlReader, refAttrName, refs)
      new XmlFile(root, refs)
    })
  }

  /**
   * Parses the element names and structure from the specified XML, filtering to reduce memory usage.
   *
   * This parses the specified byte source expecting an XML file format.
   * The filter function takes the element name and decides how many child levels should be returned in the response.
   * Always returning `Integer.MAX_VALUE` will not filter the children.
   * For example, a function could check if the name is "trade" and return only the immediate children by returning 1.
   *
   * @param source  the XML source data
   * @param filterFn  the filter function to use
   * @return the parsed file
   * @throws java.io.UncheckedIOException if an IO exception occurs
   * @throws java.lang.IllegalArgumentException if the file cannot be parsed
   */
  def parseElements(source: ByteSource, filterFn: ToIntFunction[String]): XmlElement = {
    ArgChecker.notNull(source, "source")
    ArgChecker.notNull(filterFn, "filterFn")
    val safeFilterFn: ToIntFunction[String] = name => Math.max(filterFn.applyAsInt(name), 0)
    parseSource(source, xmlReader => parseElements(xmlReader, safeFilterFn, Integer.MAX_VALUE))
  }

  // opens the source and parses it, closing the XML reader and the stream as try-with-resources would
  private def parseSource[T](source: ByteSource, parser: XMLStreamReader => T): T = {
    val loader: CheckedSupplier[T] = () => {
      val in: InputStream = source.openBufferedStream()
      var primary: Throwable = null
      try {
        val xmlReader = XML_FACTORY.createXMLStreamReader(in)
        try {
          parser(xmlReader)
        } finally {
          xmlReader.close()
        }
      } catch {
        case ex: Throwable =>
          primary = ex
          throw ex
      } finally {
        if (in != null) {
          if (primary != null) {
            try {
              in.close()
            } catch {
              case closeEx: Throwable => primary.addSuppressed(closeEx)
            }
          } else {
            in.close()
          }
        }
      }
    }
    Unchecked.wrap(loader)
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the tree from the StAX stream reader, capturing references.
   *
   * The reader should be created using `XML_FACTORY`.
   *
   * This method supports capturing attribute references, such as an id/href pair.
   * Wherever the parser finds an attribute with the specified name, the element is added
   * to the specified map. Note that the map is mutated.
   *
   * @param reader  the StAX stream reader, positioned at or before the element to be parsed
   * @param refAttr  the attribute name that should be parsed as a reference, null if not applicable
   * @param refs  the mutable map of references to update, null if not applicable
   * @return the parsed element
   * @throws java.lang.IllegalArgumentException if the input cannot be parsed
   */
  private def parse(reader: XMLStreamReader, refAttr: String, refs: Map[String, XmlElement]): XmlElement = {
    try {
      // parse start element
      val elementName = parseElementName(reader)
      val attrs = parseAttributes(reader)

      // parse children or content
      val childBuilder = ImmutableList.builder[XmlElement]()
      var content = ""
      var event = reader.next()
      while (event != XMLStreamConstants.END_ELEMENT) {
        if (event == XMLStreamConstants.START_ELEMENT) {
          // parse child when start element found
          childBuilder.add(parse(reader, refAttr, refs))
        } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
          // append content when characters found
          // since XMLStreamReader has IS_COALESCING=true means there should only be one content call
          content += reader.getText()
        }
        event = reader.next()
      }
      val children = childBuilder.build()
      val parsed =
        if (children.isEmpty()) {
          XmlElement.ofContent(elementName, attrs, content)
        } else {
          XmlElement.ofChildren(elementName, attrs, children)
        }
      val ref = attrs.get(refAttr)
      if (ref != null) {
        refs.put(ref, parsed)
      }
      parsed

    } catch {
      case ex: XMLStreamException =>
        throw new IllegalArgumentException(ex)
    }
  }

  // parses the element structure from the input, filtering as necessary
  private def parseElements(reader: XMLStreamReader, filterFn: ToIntFunction[String], currentLevel: Int): XmlElement = {
    try {
      // parse start element
      val elementName = parseElementName(reader)

      // parse children or content
      val childBuilder = ImmutableList.builder[XmlElement]()
      var event = reader.next()
      while (event != XMLStreamConstants.END_ELEMENT) {
        if (event == XMLStreamConstants.START_ELEMENT) {
          val childLevel = if (currentLevel == Integer.MAX_VALUE) filterFn.applyAsInt(elementName) else currentLevel - 1
          val child = parseElements(reader, filterFn, childLevel)
          if (childLevel > 0) {
            childBuilder.add(child)
          }
        }
        event = reader.next()
      }
      val children = childBuilder.build()
      val parsed =
        if (children.isEmpty()) {
          XmlElement.ofContent(elementName, "")
        } else {
          XmlElement.ofChildren(elementName, children)
        }
      parsed

    } catch {
      case ex: XMLStreamException =>
        throw new IllegalArgumentException(ex)
    }
  }

  // find the start element and parses the name
  @throws[XMLStreamException]
  private def parseElementName(reader: XMLStreamReader): String = {
    var event = reader.getEventType()
    while (event != XMLStreamConstants.START_ELEMENT) {
      event = reader.next()
    }
    reader.getLocalName()
  }

  // parses attributes into a map
  private def parseAttributes(reader: XMLStreamReader): ImmutableMap[String, String] = {
    var attrs: ImmutableMap[String, String] = null
    val attributeCount = reader.getAttributeCount() + reader.getNamespaceCount()
    if (attributeCount == 0) {
      attrs = ImmutableMap.of[String, String]()
    } else {
      val builder = ImmutableMap.builder[String, String]()
      var i = 0
      while (i < reader.getAttributeCount()) {
        builder.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i))
        i += 1
      }
      attrs = builder.build()
    }
    attrs
  }

}
