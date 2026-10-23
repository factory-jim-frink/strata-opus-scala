/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple

import java.io.Serializable
import java.util.Map
import java.util.NoSuchElementException
import java.util.function.BiFunction
import java.util.function.BinaryOperator

import scala.annotation.meta.field
import scala.annotation.static
import scala.compiletime.uninitialized

import org.joda.beans.Bean
import org.joda.beans.BeanBuilder
import org.joda.beans.ImmutableBean
import org.joda.beans.JodaBeanUtils
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.gen.BeanDefinition
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.collect.ComparisonChain
import com.google.common.collect.ImmutableList

/**
 * An immutable triple consisting of three elements.
 *
 * This implementation refers to the elements as 'first', 'second' and 'third'.
 * The elements cannot be null.
 *
 * Although the implementation is immutable, there is no restriction on the objects
 * that may be stored. If mutable objects are stored in the triple, then the triple
 * itself effectively becomes mutable.
 *
 * This class is immutable and thread-safe if the stored objects are immutable.
 *
 * @tparam A the first element type
 * @tparam B the second element type
 * @tparam C the third element type
 */
@BeanDefinition(builderScope = "private")
@SerialVersionUID(1L)
final class Triple[A, B, C] private (
    /**
     * The first element in this triple.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val first: A,
    /**
     * The second element in this triple.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val second: B,
    /**
     * The third element in this triple.
     */
    @(PropertyDefinition @field)(validate = "notNull")
    private val third: C)
  extends ImmutableBean with Tuple with Comparable[Triple[A, B, C]] with Serializable {

  JodaBeanUtils.notNull(first, "first")
  JodaBeanUtils.notNull(second, "second")
  JodaBeanUtils.notNull(third, "third")

  /**
   * Combines this instance with another.
   *
   * @tparam Q  the type of the first value in the other instance
   * @tparam R  the type of the second value in the other instance
   * @tparam S  the type of the third value in the other instance
   * @param other  the other triple
   * @param combinerFirst  the combiner of first values
   * @param combinerSecond  the combiner of second values
   * @param combinerThird  the combiner of third values
   * @return the combined triple instance
   */
  def combinedWith[Q, R, S](
      other: Triple[Q, R, S],
      combinerFirst: BiFunction[? >: A, ? >: Q, ? <: A],
      combinerSecond: BiFunction[? >: B, ? >: R, ? <: B],
      combinerThird: BiFunction[? >: C, ? >: S, ? <: C]): Triple[A, B, C] = {

    Triple.of[A, B, C](
        combinerFirst.apply(first, other.getFirst()),
        combinerSecond.apply(second, other.getSecond()),
        combinerThird.apply(third, other.getThird()))
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of elements held by this triple.
   *
   * @return size 3
   */
  override def size(): Int = {
    3
  }

  /**
   * Gets the elements from this triple as a list.
   *
   * The list returns each element in the triple in order.
   *
   * @return the elements as an immutable list
   */
  override def elements(): ImmutableList[Object] = {
    ImmutableList.of[Object](first.asInstanceOf[Object], second.asInstanceOf[Object], third.asInstanceOf[Object])
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the triple based on the first element followed by the second
   * element followed by the third element.
   *
   * The element types must be `Comparable`.
   *
   * @param other  the other triple
   * @return negative if this is less, zero if equal, positive if greater
   * @throws java.lang.ClassCastException if either object is not comparable
   */
  override def compareTo(other: Triple[A, B, C]): Int = {
    ComparisonChain.start()
        .compare(first.asInstanceOf[Comparable[?]], other.first.asInstanceOf[Comparable[?]])
        .compare(second.asInstanceOf[Comparable[?]], other.second.asInstanceOf[Comparable[?]])
        .compare(third.asInstanceOf[Comparable[?]], other.third.asInstanceOf[Comparable[?]])
        .result()
  }

  /**
   * Gets the triple using a standard string format.
   *
   * The standard format is '[\$first, \$second, \$third]'. Spaces around the values are trimmed.
   *
   * @return the triple as a string
   */
  override def toString(): String = {
    new java.lang.StringBuilder()
        .append('[')
        .append(first)
        .append(", ")
        .append(second)
        .append(", ")
        .append(third)
        .append(']')
        .toString()
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): Triple.Meta[A, B, C] = {
    Triple.Meta.INSTANCE.asInstanceOf[Triple.Meta[A, B, C]]
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first element in this triple.
   * @return the value of the property, not null
   */
  def getFirst(): A = {
    first
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second element in this triple.
   * @return the value of the property, not null
   */
  def getSecond(): B = {
    second
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the third element in this triple.
   * @return the value of the property, not null
   */
  def getThird(): C = {
    third
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[Triple[?, ?, ?]]
      JodaBeanUtils.equal(first, other.first) &&
          JodaBeanUtils.equal(second, other.second) &&
          JodaBeanUtils.equal(third, other.third)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(first)
    hash = hash * 31 + JodaBeanUtils.hashCode(second)
    hash = hash * 31 + JodaBeanUtils.hashCode(third)
    hash
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

object Triple {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(Triple.Meta.INSTANCE)
    true
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a triple inferring the types.
   *
   * @tparam A the first element type
   * @tparam B the second element type
   * @tparam C the third element type
   * @param first  the first element
   * @param second  the second element
   * @param third  the third element
   * @return a triple formed from the three parameters
   */
  def of[A, B, C](first: A, second: B, third: C): Triple[A, B, C] = {
    new Triple[A, B, C](first, second, third)
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a combiner of triple instances.
   *
   * This is useful if you have a stream of `Triple<A, B, C>` and would like to call reduce.
   *
   * e.g
   * {{{
   * tripleList.stream()
   *     .reduce(Triple.combining(A::combinedWith, B::combinedWith, C::combinedWith))
   * }}}
   *
   * @tparam A  the type of the first values
   * @tparam B  the type of the second values
   * @tparam C  the type of the third values
   * @param combinerFirst  the combiner of first values
   * @param combinerSecond  the combiner of second values
   * @param combinerThird  the combiner of third values
   * @return the combiner of triple instances
   */
  def combining[A, B, C](
      combinerFirst: BiFunction[? >: A, ? >: A, ? <: A],
      combinerSecond: BiFunction[? >: B, ? >: B, ? <: B],
      combinerThird: BiFunction[? >: C, ? >: C, ? <: C]): BinaryOperator[Triple[A, B, C]] = {

    (triple1, triple2) => triple1.combinedWith(triple2, combinerFirst, combinerSecond, combinerThird)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `Triple`.
   * @return the meta-bean, not null
   */
  def meta(): Triple.Meta[?, ?, ?] = {
    Triple.Meta.INSTANCE
  }

  /**
   * The meta-bean for `Triple`.
   * @tparam R  the first generic type
   * @tparam S  the second generic type
   * @tparam T  the second generic type
   * @param cls1  the first generic type
   * @param cls2  the second generic type
   * @param cls3  the third generic type
   * @return the meta-bean, not null
   */
  def metaTriple[R, S, T](cls1: Class[R], cls2: Class[S], cls3: Class[T]): Triple.Meta[R, S, T] = {
    Triple.Meta.INSTANCE.asInstanceOf[Triple.Meta[R, S, T]]
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `Triple`.
   * @tparam A  the type
   * @tparam B  the type
   * @tparam C  the type
   */
  final class Meta[A, B, C] private () extends DirectMetaBean {

    /**
     * The meta-property for the `first` property.
     */
    private val firstProperty: MetaProperty[A] = DirectMetaProperty.ofImmutable(
        this, "first", classOf[Triple[?, ?, ?]], classOf[Object]).asInstanceOf[MetaProperty[A]]
    /**
     * The meta-property for the `second` property.
     */
    private val secondProperty: MetaProperty[B] = DirectMetaProperty.ofImmutable(
        this, "second", classOf[Triple[?, ?, ?]], classOf[Object]).asInstanceOf[MetaProperty[B]]
    /**
     * The meta-property for the `third` property.
     */
    private val thirdProperty: MetaProperty[C] = DirectMetaProperty.ofImmutable(
        this, "third", classOf[Triple[?, ?, ?]], classOf[Object]).asInstanceOf[MetaProperty[C]]
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "first",
        "second",
        "third")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 97440432 => // first
          firstProperty
        case -906279820 => // second
          secondProperty
        case 110331239 => // third
          thirdProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: Triple[A, B, C]] = {
      new Triple.Builder[A, B, C]()
    }

    override def beanType(): Class[? <: Triple[A, B, C]] = {
      classOf[Triple[?, ?, ?]].asInstanceOf[Class[? <: Triple[A, B, C]]]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `first` property.
     * @return the meta-property, not null
     */
    def first(): MetaProperty[A] = {
      firstProperty
    }

    /**
     * The meta-property for the `second` property.
     * @return the meta-property, not null
     */
    def second(): MetaProperty[B] = {
      secondProperty
    }

    /**
     * The meta-property for the `third` property.
     * @return the meta-property, not null
     */
    def third(): MetaProperty[C] = {
      thirdProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 97440432 => // first
          bean.asInstanceOf[Triple[?, ?, ?]].getFirst().asInstanceOf[Object]
        case -906279820 => // second
          bean.asInstanceOf[Triple[?, ?, ?]].getSecond().asInstanceOf[Object]
        case 110331239 => // third
          bean.asInstanceOf[Triple[?, ?, ?]].getThird().asInstanceOf[Object]
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
    private[tuple] val INSTANCE: Meta[?, ?, ?] = new Meta[Object, Object, Object]()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `Triple`.
   * @tparam A  the type
   * @tparam B  the type
   * @tparam C  the type
   */
  private final class Builder[A, B, C] () extends DirectPrivateBeanBuilder[Triple[A, B, C]] {

    private var first: A = uninitialized
    private var second: B = uninitialized
    private var third: C = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 97440432 => // first
          first.asInstanceOf[Object]
        case -906279820 => // second
          second.asInstanceOf[Object]
        case 110331239 => // third
          third.asInstanceOf[Object]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder[A, B, C] = {
      propertyName.hashCode() match {
        case 97440432 => // first
          this.first = newValue.asInstanceOf[A]
        case -906279820 => // second
          this.second = newValue.asInstanceOf[B]
        case 110331239 => // third
          this.third = newValue.asInstanceOf[C]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): Triple[A, B, C] = {
      new Triple[A, B, C](
          first,
          second,
          third)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("Triple.Builder{")
      buf.append("first").append('=').append(JodaBeanUtils.toString(first)).append(',').append(' ')
      buf.append("second").append('=').append(JodaBeanUtils.toString(second)).append(',').append(' ')
      buf.append("third").append('=').append(JodaBeanUtils.toString(third))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
