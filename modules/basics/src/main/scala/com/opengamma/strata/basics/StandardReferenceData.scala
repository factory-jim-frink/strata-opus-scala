/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics

import java.util.HashMap
import java.util.Map

import com.google.common.collect.ImmutableMap
import com.opengamma.strata.basics.date.HolidayCalendar
import com.opengamma.strata.basics.date.HolidayCalendars

/**
 * Provides standard reference data for holiday calendars in common currencies.
 */
private[basics] final class StandardReferenceData private () {

}

private[basics] object StandardReferenceData {

  /**
   * Standard reference data.
   */
  private[basics] val STANDARD: ImmutableReferenceData = {
    val map: Map[ReferenceDataId[?], Object] = new HashMap[ReferenceDataId[?], Object]()
    val it = HolidayCalendars.extendedEnum().lookupAllNormalized().values().iterator()
    while (it.hasNext()) {
      val cal: HolidayCalendar = it.next()
      map.put(cal.getId(), cal)
    }
    ImmutableReferenceData.of(map)
  }
  /**
   * Minimal reference data.
   */
  private[basics] val MINIMAL: ImmutableReferenceData = {
    val builder: ImmutableMap.Builder[ReferenceDataId[?], Object] = ImmutableMap.builder[ReferenceDataId[?], Object]()
    builder.put(HolidayCalendars.NO_HOLIDAYS.getId(), HolidayCalendars.NO_HOLIDAYS)
    builder.put(HolidayCalendars.SAT_SUN.getId(), HolidayCalendars.SAT_SUN)
    builder.put(HolidayCalendars.FRI_SAT.getId(), HolidayCalendars.FRI_SAT)
    builder.put(HolidayCalendars.THU_FRI.getId(), HolidayCalendars.THU_FRI)
    ImmutableReferenceData.of(builder.build())
  }

}
