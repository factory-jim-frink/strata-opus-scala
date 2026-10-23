/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.lang.invoke.MethodHandles
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Optional
import java.util.stream.Stream

import scala.annotation.meta.field
import scala.annotation.static

import org.joda.beans.ImmutableBean
import org.joda.beans.MetaBean
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.light.LightMetaBean

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.Messages

/**
 * A single element in the tree structure of XML.
 *
 * This class is a minimal, lightweight representation of an element in the XML tree.
 * The element has a name, attributes, and either content or children.
 *
 * Note that this representation does not express all XML features.
 * No support is provided for processing instructions, comments or mixed content.
 * In addition, it is not possible to determine the difference between empty content and no children.
 *
 * There is no explicit support for namespaces.
 * When creating instances, the caller may choose to use a convention to represent namespaces.
 * For example, element and attribute names may use prefixes or the standard `QName` format.
 */
final class XmlElement private (
    rawName: String,
    rawAttributes: ImmutableMap[String, String],
    rawContent: String,
    rawChildren: ImmutableList[XmlElement])
  extends ImmutableBean {

  /**
   * The element name.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val name: String = ArgChecker.notEmpty(rawName, "name")
  /**
   * The attributes.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val attributes: ImmutableMap[String, String] = ArgChecker.notNull(rawAttributes, "attributes")
  /**
   * The element content.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val content: String = ArgChecker.notNull(rawContent, "content")
  /**
   * The child nodes.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val children: ImmutableList[XmlElement] = ArgChecker.notNull(rawChildren, "children")

  //-----------------------------------------------------------------------
  /**
   * Gets the element name.
   *
   * @return the name
   */
  def getName(): String = {
    name
  }

  /**
   * Gets an attribute by name, throwing an exception if not found.
   *
   * This returns the value of the attribute with the specified name.
   * An exception is thrown if the attribute does not exist.
   *
   * @param attrName  the attribute name to find
   * @return the attribute value
   * @throws java.lang.IllegalArgumentException if the attribute name does not exist
   */
  def getAttribute(attrName: String): String = {
    val attrValue = attributes.get(attrName)
    if (attrValue == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unknown attribute '{}' on element '{}'", attrName, name))
    }
    attrValue
  }

  /**
   * Finds an attribute by name, or empty if not found.
   *
   * This returns the value of the attribute with the specified name.
   * If the attribute is not found, optional empty is returned.
   *
   * @param attrName  the attribute name to find
   * @return the attribute value, optional
   */
  def findAttribute(attrName: String): Optional[String] = {
    Optional.ofNullable(attributes.get(attrName))
  }

  /**
   * Gets the attributes.
   *
   * This returns all the attributes of this element.
   *
   * @return the attributes
   */
  def getAttributes(): ImmutableMap[String, String] = {
    attributes
  }

  /**
   * Checks if the element has content.
   *
   * Content exists if it is non-empty.
   *
   * @return the content
   */
  def hasContent(): Boolean = {
    content.length() > 0
  }

  /**
   * Gets the element content.
   *
   * If this element has no content, the empty string is returned.
   *
   * @return the content
   */
  def getContent(): String = {
    content
  }

  /**
   * Gets a child element by index.
   *
   * @param index  the index to find
   * @return the child
   * @throws java.lang.IndexOutOfBoundsException if the index is invalid
   */
  def getChild(index: Int): XmlElement = {
    children.get(index)
  }

  /**
   * Gets the child elements.
   *
   * This returns all the children of this element.
   *
   * @return the children
   */
  def getChildren(): ImmutableList[XmlElement] = {
    children
  }

  /**
   * Gets the child element with the specified name, throwing an exception if not found or more than one.
   *
   * This returns the child element with the specified name.
   * An exception is thrown if there is more than one matching child or the child does not exist.
   *
   * @param childName  the name to match
   * @return the child matching the name
   * @throws java.lang.IllegalArgumentException if there is more than one match or no matches
   */
  def getChild(childName: String): XmlElement = {
    findChild(childName)
        .orElseThrow[IllegalArgumentException](() => new IllegalArgumentException(Messages.format(
            "Unknown element '{}' in element '{}'", childName, name)))
  }

  /**
   * Finds the child element with the specified name, or empty if not found,
   * throwing an exception if more than one.
   *
   * This returns the child element with the specified name.
   * If the element is not found, optional empty is returned.
   *
   * @param childName  the name to match
   * @return the child matching the name, optional
   * @throws java.lang.IllegalArgumentException if there is more than one match
   */
  def findChild(childName: String): Optional[XmlElement] = {
    streamChildren(childName).reduce(Guavate.ensureOnlyOne[XmlElement]())
  }

  /**
   * Gets the child elements matching the specified name.
   *
   * This returns all the child elements with the specified name.
   *
   * @param childName  the name to match
   * @return the children matching the name
   */
  def getChildren(childName: String): ImmutableList[XmlElement] = {
    streamChildren(childName).collect(Guavate.toImmutableList[XmlElement]())
  }

  /**
   * Gets the child elements matching the specified name.
   *
   * This returns all the child elements with the specified name.
   *
   * @param childName  the name to match
   * @return the children matching the name
   */
  def streamChildren(childName: String): Stream[XmlElement] = {
    children.stream().filter((child: XmlElement) => child.getName().equals(childName))
  }

  //-------------------------------------------------------------------------
  override def metaBean(): MetaBean = {
    XmlElement.META_BEAN
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this element equals another.
   *
   * This compares the entire state of the element, including all children.
   *
   * @param obj  the other element, null returns false
   * @return true if equal
   */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else {
      obj match {
        case other: XmlElement =>
          name.equals(other.name) &&
              Objects.equals(content, other.content) &&
              attributes.equals(other.attributes) &&
              children.equals(other.children)
        case _ =>
          false
      }
    }
  }

  /**
   * Returns a suitable hash code.
   *
   * This includes the entire state of the element, including all children.
   *
   * @return the hash code
   */
  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + name.hashCode()
    result = prime * result + content.hashCode()
    result = prime * result + attributes.hashCode()
    result = prime * result + children.hashCode()
    result
  }

  /**
   * Returns a string summary of the element.
   *
   * The string form includes the attributes and content, but summarizes the child elements.
   *
   * @return the string form
   */
  override def toString(): String = {
    val buf = new java.lang.StringBuilder(512)
    buf.append('<').append(name)
    val attrIt = attributes.entrySet().iterator()
    while (attrIt.hasNext()) {
      val entry = attrIt.next()
      buf.append(' ').append(entry.getKey()).append('=').append('"').append(entry.getValue()).append('"')
    }
    buf.append('>')
    if (children.isEmpty()) {
      buf.append(content)
    } else {
      val childIt = children.iterator()
      while (childIt.hasNext()) {
        val child = childIt.next()
        buf.append(System.lineSeparator()).append(" <").append(child.getName()).append(" ... />")
      }
      buf.append(System.lineSeparator())
    }
    buf.append("</").append(name).append('>')
    buf.toString()
  }

}

object XmlElement {

  /**
   * The meta-bean.
   * This is a manually coded bean.
   */
  // @static so the lookup is obtained in the XmlElement class initializer and can access its private members
  @static private val META_BEAN: MetaBean = LightMetaBean.of(
      classOf[XmlElement],
      MethodHandles.lookup(),
      Array[String]("name", "attributes", "content", "children"),
      null,
      ImmutableMap.of[Object, Object](),
      null,
      ImmutableList.of[Object]())

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with content and no attributes.
   *
   * Returns an element representing XML with content, but no children.
   *
   * @param name  the element name, not empty
   * @param content  the content, empty if the element has no content
   * @return the element
   */
  def ofContent(name: String, content: String): XmlElement = {
    ofContent(name, ImmutableMap.of[String, String](), content)
  }

  /**
   * Obtains an instance with content and attributes.
   *
   * Returns an element representing XML with content and attributes but no children.
   *
   * @param name  the element name, not empty
   * @param attributes  the attributes, empty if the element has no attributes
   * @param content  the content, empty if the element has no content
   * @return the element
   */
  def ofContent(name: String, attributes: Map[String, String], content: String): XmlElement = {
    new XmlElement(name, ImmutableMap.copyOf(attributes), content, ImmutableList.of[XmlElement]())
  }

  /**
   * Obtains an instance with children and no attributes.
   *
   * Returns an element representing XML with children, but no content.
   *
   * @param name  the element name, not empty
   * @param children  the children, empty if the element has no children
   * @return the element
   */
  def ofChildren(name: String, children: List[XmlElement]): XmlElement = {
    ofChildren(name, ImmutableMap.of[String, String](), children)
  }

  /**
   * Obtains an instance with children and attributes.
   *
   * Returns an element representing XML with children and attributes, but no content.
   *
   * @param name  the element name, not empty
   * @param attributes  the attributes, empty if the element has no attributes
   * @param children  the children, empty if the element has no children
   * @return the element
   */
  def ofChildren(name: String, attributes: Map[String, String], children: List[XmlElement]): XmlElement = {
    new XmlElement(name, ImmutableMap.copyOf(attributes), "", ImmutableList.copyOf(children))
  }

}
