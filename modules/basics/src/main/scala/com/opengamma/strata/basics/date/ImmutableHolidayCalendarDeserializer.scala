/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Arrays
import java.util.NoSuchElementException
import java.util.Objects
import java.util.Set
import java.util.concurrent.ConcurrentMap

import scala.annotation.static

import org.joda.beans.BeanBuilder
import org.joda.beans.MetaBean
import org.joda.beans.MetaProperty
import org.joda.beans.impl.BufferingBeanBuilder
import org.joda.beans.impl.StandaloneMetaProperty
import org.joda.beans.ser.DefaultDeserializer

import com.opengamma.strata.basics.date.ImmutableHolidayCalendar.Meta

/**
 * Deserialize `ImmutableHolidayCalendar` handling old format.
 */
final class ImmutableHolidayCalendarDeserializer private[date] () extends DefaultDeserializer {

  override def createBuilder(beanType: Class[?], metaBean: MetaBean): BeanBuilder[?] = {
    BufferingBeanBuilder.of(metaBean)
  }

  override def findMetaProperty(beanType: Class[?], metaBean: MetaBean, propertyName: String): MetaProperty[?] = {
    try {
      metaBean.metaProperty(propertyName)
    } catch {
      case ex: NoSuchElementException =>
        if (ImmutableHolidayCalendarDeserializer.HOLIDAYS.name().equals(propertyName)) {
          ImmutableHolidayCalendarDeserializer.HOLIDAYS
        } else if (ImmutableHolidayCalendarDeserializer.WEEKEND_DAYS.name().equals(propertyName)) {
          ImmutableHolidayCalendarDeserializer.WEEKEND_DAYS
        } else {
          throw ex
        }
    }
  }

  override def build(beanType: Class[?], builder: BeanBuilder[?]): Object = {
    val buffer: ConcurrentMap[MetaProperty[?], Object] = builder.asInstanceOf[BufferingBeanBuilder[?]].getBuffer()
    val id: HolidayCalendarId = builder.get(ImmutableHolidayCalendarDeserializer.ID)
    if (buffer.containsKey(ImmutableHolidayCalendarDeserializer.HOLIDAYS) &&
        buffer.containsKey(ImmutableHolidayCalendarDeserializer.WEEKEND_DAYS)) {
      val holidays: Set[LocalDate] = builder.get(ImmutableHolidayCalendarDeserializer.HOLIDAYS)
      val weekendDays: Set[DayOfWeek] = builder.get(ImmutableHolidayCalendarDeserializer.WEEKEND_DAYS)
      ImmutableHolidayCalendar.of(id, holidays, weekendDays)
    } else {
      val weekends: Int = builder.get(ImmutableHolidayCalendarDeserializer.WEEKENDS).intValue()
      val startYear: Int = builder.get(ImmutableHolidayCalendarDeserializer.START_YEAR).intValue()
      val lookup: Array[Int] = builder.get(ImmutableHolidayCalendarDeserializer.LOOKUP)
      new ImmutableHolidayCalendar(id, weekends, startYear, lookup, false)
    }
  }

}

object ImmutableHolidayCalendarDeserializer {

  @static private val META_BEAN: Meta = ImmutableHolidayCalendar.meta()
  @static private val ID: MetaProperty[HolidayCalendarId] = META_BEAN.id()
  @static private val WEEKENDS: MetaProperty[Integer] = META_BEAN.weekends()
  @static private val START_YEAR: MetaProperty[Integer] = META_BEAN.startYear()
  @static private val LOOKUP: MetaProperty[Array[Int]] = META_BEAN.lookup()

  @static private val HOLIDAYS: MetaProperty[Set[LocalDate]] =
    StandaloneMetaProperty.of(
        "holidays", META_BEAN, classOf[Set[?]], new SetType(classOf[LocalDate]))
        .asInstanceOf[MetaProperty[Set[LocalDate]]]
  @static private val WEEKEND_DAYS: MetaProperty[Set[DayOfWeek]] =
    StandaloneMetaProperty.of(
        "weekendDays", META_BEAN, classOf[Set[?]], new SetType(classOf[DayOfWeek]))
        .asInstanceOf[MetaProperty[Set[DayOfWeek]]]

  //-------------------------------------------------------------------------
  // the generic type `Set<E>`, equal to and with the same hash code as the JDK reflection type
  private final class SetType(elementType: Class[?]) extends ParameterizedType {

    override def getActualTypeArguments(): Array[Type] = {
      Array[Type](elementType)
    }

    override def getRawType(): Type = {
      classOf[Set[?]]
    }

    override def getOwnerType(): Type = {
      null
    }

    override def equals(obj: Any): Boolean = {
      obj match {
        case other: ParameterizedType =>
          (other eq this) ||
              (Objects.equals(getOwnerType(), other.getOwnerType()) &&
                  Objects.equals(getRawType(), other.getRawType()) &&
                  Arrays.equals(getActualTypeArguments().asInstanceOf[Array[Object]],
                      other.getActualTypeArguments().asInstanceOf[Array[Object]]))
        case _ =>
          false
      }
    }

    override def hashCode(): Int = {
      Arrays.hashCode(getActualTypeArguments().asInstanceOf[Array[Object]]) ^
          Objects.hashCode(getOwnerType()) ^
          Objects.hashCode(getRawType())
    }

    override def toString(): String = {
      classOf[Set[?]].getName() + "<" + elementType.getTypeName() + ">"
    }

  }

}
