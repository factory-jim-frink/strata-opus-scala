/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result

import java.io.Serializable
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Map
import java.util.NoSuchElementException
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

import scala.annotation.meta.field
import scala.annotation.static
import scala.annotation.varargs
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.ImmutableConstructor
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.base.Strings
import com.google.common.base.Throwables
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Interner
import com.google.common.collect.Interners
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.tuple.Pair

/**
 * Details of a single failed item.
 *
 * This is used in `Failure` and `FailureItems` to capture details of a single failure.
 * Details include the reason, message and stack trace.
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class FailureItem @ImmutableConstructor private (
    /**
     * The reason associated with the failure.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val reason: FailureReason,
    /**
     * The error message associated with the failure.
     */
    @(PropertyDefinition @field)(validate = "notEmpty")
    private val message: String,
    rawAttributes: Map[String, String],
    rawStackTrace: String,
    /**
     * The type of the throwable that caused the failure, not present if it wasn't caused by a throwable.
     */
    @(PropertyDefinition @field)(get = "optional")
    private val causeType: Class[? <: Throwable])
  extends ImmutableBean with Serializable {

  /**
   * The attributes associated with this failure.
   * Attributes can contain additional information about the failure. For example, a line number in a file or the ID of a trade.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val attributes: ImmutableMap[String, String] = ImmutableMap.copyOf(rawAttributes)
  JodaBeanUtils.notNull(reason, "reason")
  JodaBeanUtils.notEmpty(message, "message")
  JodaBeanUtils.notNull(rawStackTrace, "stackTrace")
  /**
   * Stack trace where the failure occurred.
   * If the failure was caused by an `Exception` its stack trace is used, otherwise it's the
   * location where the failure was created.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val stackTrace: String = FailureItem.INTERNER.intern(rawStackTrace)

  //-------------------------------------------------------------------------
  /**
   * Gets the message template that was used to create the message.
   *
   * This method derives the template from 'templateLocation' in the attributes.
   * This only works if the template location correctly matches the message.
   *
   * @return the message template
   */
  def getMessageTemplate(): String = {
    Messages.recreateTemplate(message, attributes.get(FailureAttributeKeys.TEMPLATE_LOCATION))
  }

  /**
   * Returns an instance with the specified attribute added.
   *
   * If the attribute map of this instance has the specified key, the value is replaced.
   *
   * @param key  the key to add
   * @param value  the value to add
   * @return the new failure item
   */
  def withAttribute(key: String, value: String): FailureItem = {
    val attributes: Map[String, String] = new HashMap[String, String](this.attributes)
    attributes.put(key, value)
    new FailureItem(reason, message, attributes, stackTrace, causeType)
  }

  /**
   * Returns an instance with the specified attributes added.
   *
   * If the attribute map of this instance has any of the new attribute keys, the values are replaced.
   *
   * @param attributes  the new attributes to add
   * @return the new failure item
   */
  def withAttributes(attributes: Map[String, String]): FailureItem = {
    val newAttributes: Map[String, String] = new HashMap[String, String](this.attributes)
    newAttributes.putAll(attributes)
    new FailureItem(reason, message, newAttributes, stackTrace, causeType)
  }

  /**
   * Processes the failure item by applying a function that alters the message.
   *
   * This operation allows wrapping a failure message with additional information that may have not been available
   * to the code that created the original failure.
   *
   * @param function  the function to transform the message with
   * @return the transformed instance
   */
  def mapMessage(function: Function[String, String]): FailureItem = {
    new FailureItem(reason, function.apply(message), attributes, stackTrace, causeType)
  }

  /**
   * Returns a string summary of the stack trace.
   *
   * @return the summary stack trace
   */
  def summarizeStackTrace(): Optional[String] = {
    if (stackTrace.startsWith(FailureItem.FAILURE_EXCEPTION)) {
      Optional.empty()
    } else {
      val endLine = stackTrace.indexOf("\n")
      var firstLine = if (endLine < 0) stackTrace else stackTrace.substring(0, endLine)
      firstLine =
        if (firstLine.endsWith(": " + message)) firstLine.substring(0, firstLine.length() - message.length() - 2)
        else firstLine
      Optional.of(firstLine)
    }
  }

  /**
   * Returns a string summary of the failure, as a single line excluding the stack trace.
   *
   * @return the summary string
   */
  override def toString(): String = {
    String.valueOf(reason) + ": " + message + summarizeStackTrace().map[String](str => ": " + str).orElse("")
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): FailureItem.Meta = {
    FailureItem.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reason associated with the failure.
   * @return the value of the property, not null
   */
  def getReason(): FailureReason = {
    reason
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the error message associated with the failure.
   * @return the value of the property, not empty
   */
  def getMessage(): String = {
    message
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the attributes associated with this failure.
   * Attributes can contain additional information about the failure. For example, a line number in a file or the ID of a trade.
   * @return the value of the property, not null
   */
  def getAttributes(): ImmutableMap[String, String] = {
    attributes
  }

  //-----------------------------------------------------------------------
  /**
   * Gets stack trace where the failure occurred.
   * If the failure was caused by an `Exception` its stack trace is used, otherwise it's the
   * location where the failure was created.
   * @return the value of the property, not null
   */
  def getStackTrace(): String = {
    stackTrace
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the throwable that caused the failure, not present if it wasn't caused by a throwable.
   * @return the optional value of the property, not null
   */
  def getCauseType(): Optional[Class[? <: Throwable]] = {
    Optional.ofNullable(causeType)
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[FailureItem]
      JodaBeanUtils.equal(reason, other.reason) &&
          JodaBeanUtils.equal(message, other.message) &&
          JodaBeanUtils.equal(attributes, other.attributes) &&
          JodaBeanUtils.equal(stackTrace, other.stackTrace) &&
          JodaBeanUtils.equal(causeType, other.causeType)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(reason)
    hash = hash * 31 + JodaBeanUtils.hashCode(message)
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes)
    hash = hash * 31 + JodaBeanUtils.hashCode(stackTrace)
    hash = hash * 31 + JodaBeanUtils.hashCode(causeType)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object FailureItem {

  /**
   * Attribute used to store the exception message.
   */
  @static final val EXCEPTION_MESSAGE_ATTRIBUTE: String = FailureAttributeKeys.EXCEPTION_MESSAGE

  @static private val REGISTERED: Boolean = {
    MetaBean.register(FailureItem.Meta.INSTANCE)
    true
  }

  /**
   * Header used when generating stack trace internally.
   */
  private val FAILURE_EXCEPTION: String = "com.opengamma.strata.collect.result.FailureItem: "
  /**
   * Stack traces can take up a lot of memory if a large number of failures are stored.
   * They are often duplicated many times so interning them can save a significant amount of memory.
   */
  private val INTERNER: Interner[String] = Interners.newWeakInterner[String]()
  /**
   * Walks the stack with the frames that Thread.getStackTrace() reports.
   */
  private val STACK_WALKER: StackWalker = StackWalker.getInstance(java.util.Set.of(StackWalker.Option.SHOW_REFLECT_FRAMES))
  /**
   * The end of a method descriptor whose last parameter is a Scala Seq.
   */
  private val SEQ_LAST_PARAMETER: String = "Lscala/collection/immutable/Seq;)"

  //-------------------------------------------------------------------------
  /**
   * Obtains a failure from a reason and message.
   *
   * The message is produced using a template that contains zero to many "{}" or "{abc}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If the placeholder has a name, its value is added to the attributes map with the name as a key.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.formatWithAttributes(String, Object...)` for more details.
   *
   * An exception will be created internally to obtain a stack trace.
   * The cause type will not be present in the resulting failure.
   *
   * @param reason  the reason
   * @param message  a message explaining the failure, not empty, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  @varargs
  def of(reason: FailureReason, message: String, messageArgs: Object*): FailureItem = {
    val msg: Pair[String, Map[String, String]] = Messages.formatWithAttributes(message, messageArgs*)
    ofAttributes(reason, msg.getFirst(), msg.getSecond())
  }

  /**
   * Obtains a failure from a reason and message.
   *
   * The failure will still have a stack trace, but the cause type will not be present.
   *
   * @param skipFrames  the number of caller frames to skip, not including this one
   * @param reason  the reason
   * @param message  the failure message, not empty
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  @varargs
  private[result] def ofAutoStackTrace(
      skipFrames: Int,
      reason: FailureReason,
      message: String,
      messageArgs: Object*): FailureItem = {

    ArgChecker.notNull(reason, "reason")
    ArgChecker.notEmpty(message, "message")
    val messageArgPair: Pair[String, Map[String, String]] = Messages.formatWithAttributes(message, messageArgs*)
    val stackTrace = localGetStackTraceAsString(messageArgPair.getFirst(), skipFrames)
    new FailureItem(reason, messageArgPair.getFirst(), messageArgPair.getSecond(), stackTrace, null)
  }

  /**
   * Obtains a failure from a reason and message.
   *
   * The failure will still have a stack trace, but the cause type will not be present.
   *
   * @param reason  the reason
   * @param message  the failure message, not empty
   * @param attributes the attributes associated with this failure
   * @return the failure
   */
  private def ofAttributes(reason: FailureReason, message: String, attributes: Map[String, String]): FailureItem = {
    ArgChecker.notNull(reason, "reason")
    ArgChecker.notEmpty(message, "message")
    val stackTrace = localGetStackTraceAsString(message, 0)
    new FailureItem(reason, message, attributes, stackTrace, null)
  }

  private def localGetStackTraceAsString(message: String, skipFrames: Int): String = {
    val builder = new java.lang.StringBuilder()
    val frames = javaStackFrames()
    // simulate full stack trace, pretending this class is a Throwable subclass
    builder.append(FAILURE_EXCEPTION).append(message).append("\n")
    // drop the first few frames because they are part of the immediate calling code
    var i = 0
    while (i < frames.size() && isFailureItemFrame(frames.get(i))) {
      i += 1
    }
    i += skipFrames
    while (i < frames.size()) {
      builder.append("\tat ").append(frameText(frames.get(i))).append("\n")
      i += 1
    }
    builder.toString()
  }

  // the frames that Thread.getStackTrace() would report, without the varargs and static forwarders
  // that Scala adds to Strata code, so that the frames match those of the equivalent Java code
  private def javaStackFrames(): java.util.List[StackWalker.StackFrame] = {
    val all = STACK_WALKER.walk[java.util.List[StackWalker.StackFrame]](
      frames => frames.collect(Collectors.toList[StackWalker.StackFrame]()))
    val result = new java.util.ArrayList[StackWalker.StackFrame](all.size())
    var i = 0
    while (i < all.size()) {
      if (i == 0 || !isForwarderFrame(all.get(i - 1), all.get(i))) {
        result.add(all.get(i))
      }
      i += 1
    }
    result
  }

  private def isForwarderFrame(callee: StackWalker.StackFrame, frame: StackWalker.StackFrame): Boolean = {
    isStrataScalaFrame(callee) &&
      isStrataScalaFrame(frame) &&
      callee.getMethodName().equals(frame.getMethodName()) &&
      (isVarargsForwarderFrame(callee, frame) || isStaticForwarderFrame(callee, frame))
  }

  // a @varargs forwarder takes an array where the method it calls takes a Seq as last parameter
  private def isVarargsForwarderFrame(callee: StackWalker.StackFrame, frame: StackWalker.StackFrame): Boolean = {
    val calleeDescriptor = callee.getDescriptor()
    val seqIndex = calleeDescriptor.indexOf(SEQ_LAST_PARAMETER)
    if (!callee.getClassName().equals(frame.getClassName()) || seqIndex < 0) {
      false
    } else {
      val parameters = calleeDescriptor.substring(0, seqIndex)
      val returnType = calleeDescriptor.substring(seqIndex + SEQ_LAST_PARAMETER.length())
      val descriptor = frame.getDescriptor()
      descriptor.startsWith(parameters + "[") &&
        descriptor.endsWith(")" + returnType) &&
        descriptor.indexOf(')') == descriptor.length() - returnType.length() - 1
    }
  }

  // a static forwarder in class X has no line numbers and calls the same method of the companion object X$
  private def isStaticForwarderFrame(callee: StackWalker.StackFrame, frame: StackWalker.StackFrame): Boolean = {
    callee.getClassName().equals(frame.getClassName() + "$") &&
      callee.getDescriptor().equals(frame.getDescriptor()) &&
      frame.getLineNumber() < 0
  }

  // Java code has a static method of class X where Scala has a method of the companion object X$
  private def frameText(frame: StackWalker.StackFrame): String = {
    val text = frame.toStackTraceElement().toString()
    val className = frame.getClassName()
    val target = className + "." + frame.getMethodName() + "("
    val index = text.indexOf(target)
    if (isStrataScalaFrame(frame) && className.endsWith("$") && index >= 0) {
      val javaClassName = className.substring(0, className.length() - 1)
      text.substring(0, index) + javaClassName + text.substring(index + className.length())
    } else {
      text
    }
  }

  private def isStrataScalaFrame(frame: StackWalker.StackFrame): Boolean = {
    val fileName = frame.getFileName()
    frame.getClassName().startsWith("com.opengamma.strata.") && fileName != null && fileName.endsWith(".scala")
  }

  private def isFailureItemFrame(frame: StackWalker.StackFrame): Boolean = {
    val className = frame.getClassName()
    val baseClassName = if (className.endsWith("$")) className.substring(0, className.length() - 1) else className
    baseClassName.equals(classOf[FailureItem].getName())
  }
  /**
   * Obtains a failure from a reason and exception.
   *
   * This recognizes and handles `FailureItemProvider` exceptions.
   *
   * @param reason  the reason
   * @param cause  the cause
   * @return the failure
   */
  def of(reason: FailureReason, cause: Throwable): FailureItem = {
    ArgChecker.notNull(reason, "reason")
    ArgChecker.notNull(cause, "cause")
    val causeMessage = extractCauseMessage(cause)
    FailureItem.of(reason, cause, causeMessage)
  }

  /**
   * Obtains a failure from a reason, throwable and message.
   *
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See `Messages.formatWithAttributes(String, Object...)` for more details.
   *
   * It can be useful to capture the underlying exception message. This should be achieved by adding
   * ': {exceptionMessage}' to the template and 'cause.toString()' or 'cause.getMessage()' to the arguments.
   *
   * This recognizes and handles `FailureItemProvider` exceptions.
   *
   * @param reason  the reason
   * @param cause  the cause
   * @param messageTemplate  a message explaining the failure, not empty, uses "{}" for inserting `messageArgs`
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  @varargs
  def of(reason: FailureReason, cause: Throwable, messageTemplate: String, messageArgs: Object*): FailureItem = {
    ArgChecker.notNull(reason, "reason")
    ArgChecker.notNull(cause, "cause")
    if (cause.isInstanceOf[FailureItemProvider]) {
      ofWrappedFailureItem(
          cause.asInstanceOf[FailureItemProvider].getFailureItem(), reason, messageTemplate, messageArgs)
    } else {
      val msg: Pair[String, Map[String, String]] = Messages.formatWithAttributes(messageTemplate, messageArgs*)
      val stackTrace = Throwables.getStackTraceAsString(cause).replace(System.lineSeparator(), "\n")
      val base = new FailureItem(reason, msg.getFirst(), msg.getSecond(), stackTrace, cause.getClass())
      val causeMessage = extractCauseMessage(cause)
      if (!base.getAttributes().containsKey(EXCEPTION_MESSAGE_ATTRIBUTE) && !Strings.isNullOrEmpty(causeMessage)) {
        base.withAttribute(EXCEPTION_MESSAGE_ATTRIBUTE, causeMessage)
      } else {
        base
      }
    }
  }

  // extracts the cause message
  private def extractCauseMessage(cause: Throwable): String = {
    val causeMessage = cause.getMessage()
    if (Strings.isNullOrEmpty(causeMessage)) cause.getClass().getSimpleName() else causeMessage
  }

  // handles a cause that contains a FailureItem
  private def ofWrappedFailureItem(
      underlying: FailureItem,
      reason: FailureReason,
      messageTemplate: String,
      messageArgs: Seq[Object]): FailureItem = {

    // strip trailing 'exceptionMessage'
    if (messageTemplate.endsWith("{exceptionMessage}")) {
      var adjustedTemplate = messageTemplate.substring(0, messageTemplate.length() - 18).trim()
      adjustedTemplate =
        if (adjustedTemplate.endsWith(":")) adjustedTemplate.substring(0, adjustedTemplate.length() - 1)
        else adjustedTemplate
      val adjustedArgs = if (messageArgs.length >= 1) messageArgs.slice(0, messageArgs.length - 1) else messageArgs
      ofWrappedFailureItem(underlying, reason, adjustedTemplate, adjustedArgs)
    } else {
      // format and combine message
      val msgPair: Pair[String, Map[String, String]] = Messages.formatWithAttributes(messageTemplate, messageArgs*)
      val baseMsg = msgPair.getFirst() + ": "
      val baseAttrs = msgPair.getSecond()
      val combinedMsg = baseMsg + underlying.getMessage()

      // combine attributes
      var underlyingLocation = underlying.getAttributes().getOrDefault(FailureAttributeKeys.TEMPLATE_LOCATION, "")
      val baseLocation = baseAttrs.getOrDefault(FailureAttributeKeys.TEMPLATE_LOCATION, "")
      val combinedAttrs: Map[String, String] = new LinkedHashMap[String, String](baseAttrs)
      combinedAttrs.remove(FailureAttributeKeys.TEMPLATE_LOCATION)
      val it = underlying.getAttributes().entrySet().iterator()
      while (it.hasNext()) {
        val entry = it.next()
        val key = entry.getKey()
        if (!FailureAttributeKeys.TEMPLATE_LOCATION.equals(key)) {
          var adjKey = key
          var count = 1
          while (combinedAttrs.containsKey(adjKey)) {
            adjKey = key + count
            count += 1
          }
          if (!adjKey.equals(key)) {
            underlyingLocation = underlyingLocation.replace(key, adjKey)
          }
          combinedAttrs.put(adjKey, entry.getValue())
        }
      }

      // adjust location
      val mergedLocation = Messages.mergeTemplateLocations(baseLocation, underlyingLocation, baseMsg.length())
      if (!mergedLocation.isEmpty()) {
        combinedAttrs.put(FailureAttributeKeys.TEMPLATE_LOCATION, mergedLocation)
      }
      new FailureItem(
          underlying.getReason(),
          combinedMsg,
          combinedAttrs,
          underlying.getStackTrace(),
          underlying.getCauseType().orElse(null))
    }
  }

  /**
   * Creates a failure item from the throwable.
   *
   * This recognizes and handles `FailureItemProvider` exceptions.
   *
   * @param th  the throwable to be processed
   * @return the failure item
   */
  def from(th: Throwable): FailureItem = {
    try {
      throw th
    } catch {
      case ex: Throwable =>
        if (ex.isInstanceOf[FailureItemProvider]) {
          ex.asInstanceOf[FailureItemProvider].getFailureItem()
        } else {
          of(FailureReason.ERROR, ex)
        }
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `FailureItem`.
   * @return the meta-bean, not null
   */
  def meta(): FailureItem.Meta = {
    FailureItem.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `FailureItem`.
   */
  final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `reason` property.
     */
    private val reasonProperty: MetaProperty[FailureReason] = DirectMetaProperty.ofImmutable(
        this, "reason", classOf[FailureItem], classOf[FailureReason])
    /**
     * The meta-property for the `message` property.
     */
    private val messageProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "message", classOf[FailureItem], classOf[String])
    /**
     * The meta-property for the `attributes` property.
     */
    private val attributesProperty: MetaProperty[ImmutableMap[String, String]] = DirectMetaProperty.ofImmutable(
        this, "attributes", classOf[FailureItem], classOf[ImmutableMap[?, ?]])
        .asInstanceOf[MetaProperty[ImmutableMap[String, String]]]
    /**
     * The meta-property for the `stackTrace` property.
     */
    private val stackTraceProperty: MetaProperty[String] = DirectMetaProperty.ofImmutable(
        this, "stackTrace", classOf[FailureItem], classOf[String])
    /**
     * The meta-property for the `causeType` property.
     */
    private val causeTypeProperty: MetaProperty[Class[? <: Throwable]] = DirectMetaProperty.ofImmutable(
        this, "causeType", classOf[FailureItem], classOf[Class[?]])
        .asInstanceOf[MetaProperty[Class[? <: Throwable]]]
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "reason",
        "message",
        "attributes",
        "stackTrace",
        "causeType")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -934964668 => // reason
          reasonProperty
        case 954925063 => // message
          messageProperty
        case 405645655 => // attributes
          attributesProperty
        case 2026279837 => // stackTrace
          stackTraceProperty
        case -1443456189 => // causeType
          causeTypeProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: FailureItem] = {
      new FailureItem.Builder()
    }

    override def beanType(): Class[? <: FailureItem] = {
      classOf[FailureItem]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `reason` property.
     * @return the meta-property, not null
     */
    def reason(): MetaProperty[FailureReason] = {
      reasonProperty
    }

    /**
     * The meta-property for the `message` property.
     * @return the meta-property, not null
     */
    def message(): MetaProperty[String] = {
      messageProperty
    }

    /**
     * The meta-property for the `attributes` property.
     * @return the meta-property, not null
     */
    def attributes(): MetaProperty[ImmutableMap[String, String]] = {
      attributesProperty
    }

    /**
     * The meta-property for the `stackTrace` property.
     * @return the meta-property, not null
     */
    def stackTrace(): MetaProperty[String] = {
      stackTraceProperty
    }

    /**
     * The meta-property for the `causeType` property.
     * @return the meta-property, not null
     */
    def causeType(): MetaProperty[Class[? <: Throwable]] = {
      causeTypeProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -934964668 => // reason
          bean.asInstanceOf[FailureItem].getReason()
        case 954925063 => // message
          bean.asInstanceOf[FailureItem].getMessage()
        case 405645655 => // attributes
          bean.asInstanceOf[FailureItem].getAttributes()
        case 2026279837 => // stackTrace
          bean.asInstanceOf[FailureItem].getStackTrace()
        case -1443456189 => // causeType
          bean.asInstanceOf[FailureItem].getCauseType().orElse(null)
        case _ =>
          super.propertyGet(bean, propertyName, quiet)
      }
    }

    override protected def propertySet(bean: Bean, propertyName: String, newValue: Object, quiet: Boolean): Unit = {
      metaProperty(propertyName)
      if (!quiet) {
        throw new UnsupportedOperationException("Property cannot be written: " + propertyName)
      }
    }

  }

  object Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    private[result] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `FailureItem`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[FailureItem] {

    private var reason: FailureReason = uninitialized
    private var message: String = uninitialized
    private var attributes: Map[String, String] = ImmutableMap.of[String, String]()
    private var stackTrace: String = uninitialized
    private var causeType: Class[? <: Throwable] = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -934964668 => // reason
          reason
        case 954925063 => // message
          message
        case 405645655 => // attributes
          attributes
        case 2026279837 => // stackTrace
          stackTrace
        case -1443456189 => // causeType
          causeType
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -934964668 => // reason
          this.reason = newValue.asInstanceOf[FailureReason]
        case 954925063 => // message
          this.message = newValue.asInstanceOf[String]
        case 405645655 => // attributes
          this.attributes = newValue.asInstanceOf[Map[String, String]]
        case 2026279837 => // stackTrace
          this.stackTrace = newValue.asInstanceOf[String]
        case -1443456189 => // causeType
          this.causeType = newValue.asInstanceOf[Class[? <: Throwable]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): FailureItem = {
      new FailureItem(
          reason,
          message,
          attributes,
          stackTrace,
          causeType)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(192)
      buf.append("FailureItem.Builder{")
      buf.append("reason").append('=').append(JodaBeanUtils.toString(reason)).append(',').append(' ')
      buf.append("message").append('=').append(JodaBeanUtils.toString(message)).append(',').append(' ')
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes)).append(',').append(' ')
      buf.append("stackTrace").append('=').append(JodaBeanUtils.toString(stackTrace)).append(',').append(' ')
      buf.append("causeType").append('=').append(JodaBeanUtils.toString(causeType))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
