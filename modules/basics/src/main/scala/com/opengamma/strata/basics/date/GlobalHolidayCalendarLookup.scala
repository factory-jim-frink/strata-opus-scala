/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

import scala.annotation.static

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.collect.io.ResourceLocator
import com.opengamma.strata.collect.named.NamedLookup

/**
 * Provider that loads common global holiday calendars from binary form on the classpath.
 */
final class GlobalHolidayCalendarLookup private () extends NamedLookup[HolidayCalendar] {

  // lookup of conventions
  private val map: ImmutableMap[String, HolidayCalendar] = {
    // not a static initializer, as Hotspot does not work in static code
    val builder: ImmutableMap.Builder[String, HolidayCalendar] = ImmutableMap.builder[String, HolidayCalendar]()
    val locator =
      ResourceLocator.ofClasspath("com/opengamma/strata/basics/date/GlobalHolidayCalendars.bin")
    try {
      // equivalent of nested try-with-resources
      val fis: InputStream = locator.getByteSource().openStream()
      var primaryOuter: Throwable = null
      try {
        val in = new DataInputStream(fis)
        var primaryInner: Throwable = null
        try {
          if (in.readByte().toInt != 'H'.toInt || in.readByte().toInt != 'C'.toInt ||
              in.readByte().toInt != 'a'.toInt || in.readByte().toInt != 'l'.toInt) {
            System.err.println("ERROR: Corrupt holiday calendar data file")
          } else {
            val calSize: Short = in.readShort()
            var i = 0
            while (i < calSize) {
              val cal: HolidayCalendar = ImmutableHolidayCalendar.readExternal(in)
              builder.put(cal.getId().getName(), cal)
              i += 1
            }
          }
        } catch {
          case ex: Throwable =>
            primaryInner = ex
            throw ex
        } finally {
          GlobalHolidayCalendarLookup.closeResource(in, primaryInner)
        }
      } catch {
        case ex: Throwable =>
          primaryOuter = ex
          throw ex
      } finally {
        GlobalHolidayCalendarLookup.closeResource(fis, primaryOuter)
      }
    } catch {
      case ex: IOException =>
        System.err.println("ERROR: Unable to parse holiday calendar data file: " + ex.getMessage())
        ex.printStackTrace()
    }
    builder.build()
  }

  //-------------------------------------------------------------------------
  override def lookupAll(): ImmutableMap[String, HolidayCalendar] = {
    map
  }

}

object GlobalHolidayCalendarLookup {

  /**
   * The singleton instance of the lookup.
   */
  @static final val INSTANCE: GlobalHolidayCalendarLookup = new GlobalHolidayCalendarLookup()

  // closes the resource, adding any close failure as suppressed to the primary failure
  private def closeResource(resource: AutoCloseable, primary: Throwable): Unit = {
    if (resource != null) {
      if (primary != null) {
        try {
          resource.close()
        } catch {
          case closeEx: Throwable => primary.addSuppressed(closeEx)
        }
      } else {
        resource.close()
      }
    }
  }

}
