/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries

import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoField.DAY_OF_WEEK
import java.time.temporal.ChronoUnit.DAYS
import java.util.Arrays
import java.util.List
import java.util.Map
import java.util.NoSuchElementException
import java.util.OptionalDouble
import java.util.function.DoubleUnaryOperator
import java.util.function.Function
import java.util.function.ObjDoubleConsumer
import java.util.function.Supplier
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.Stream

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
import org.joda.beans.gen.ImmutableConstructor
import org.joda.beans.gen.PropertyDefinition
import org.joda.beans.impl.direct.DirectMetaBean
import org.joda.beans.impl.direct.DirectMetaProperty
import org.joda.beans.impl.direct.DirectMetaPropertyMap
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder

import com.google.common.collect.Ordering
import com.google.common.primitives.Doubles
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Guavate
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.function.ObjDoublePredicate
import com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation

/**
 * An immutable implementation of `LocalDateDoubleTimeSeries` where the
 * data stored is expected to be dense. For example, points for every
 * working day in a month. If sparser data is being used then
 * `SparseLocalDateDoubleTimeSeries` is likely to be a better
 * choice for the data.
 *
 * This implementation uses arrays internally.
 */
@BeanDefinition(builderScope = "private", metaScope = "package")
@SerialVersionUID(1L)
private[timeseries] final class DenseLocalDateDoubleTimeSeries private (
    rawStartDate: LocalDate,
    rawPoints: Array[Double],
    rawDateCalculation: DenseTimeSeriesCalculation,
    trusted: Boolean,
    rawSize: Int)
  extends ImmutableBean with LocalDateDoubleTimeSeries with Serializable {

  // Private constructor, the trusted flag indicates whether the
  // points array should be cloned. If trusted, it will not be cloned.
  // size is the size of the time series + 1 if known, 0 if unknown
  ArgChecker.notNull(rawPoints, "points")

  /**
   * Date corresponding to first element in the array. All other
   * values can be calculated using date arithmetic to find
   * correct point.
   */
  @(PropertyDefinition @field)(validate = "notNull")
  private val startDate: LocalDate = ArgChecker.notNull(rawStartDate, "startDate")

  /**
   * The values in the series.
   * The date for each value is calculated using the position
   * in the array and the start date.
   */
  @(PropertyDefinition @field)(get = "private", validate = "notNull")
  private val points: Array[Double] = if (trusted) rawPoints else rawPoints.clone()

  /**
   * Whether we should store data for the weekends (NaN will be stored
   * if no data is available).
   */
  @(PropertyDefinition @field)(get = "private", validate = "notNull")
  private val dateCalculation: DenseTimeSeriesCalculation = ArgChecker.notNull(rawDateCalculation, "dateCalculation")

  // the Java fields were named 'isEmpty' and 'size', which clash with the methods in Scala; both are transient
  /**
   * Whether this time series is empty.
   * 0 is unknown, -1 is empty, 1 is non-empty.
   * Kept separate from size so we don't iterate the whole points array unless necessary.
   */
  @transient private var isEmptyCache: Int = uninitialized // not a property, derived from points and cached as is an O(n) lookup

  /**
   * The size of the time series.
   * The amount of valid values.
   * Offset by one to account for 0 being the unset value of an int, hence the actual size is size - 1.
   */
  @transient private var sizeCache: Int = rawSize // not a property, derived from points and cached as is an O(n) lookup

  if (rawSize != 0) {
    this.isEmptyCache = if (rawSize > 1) 1 else -1
  }

  @ImmutableConstructor
  private def this(
      startDate: LocalDate,
      points: Array[Double],
      dateCalculation: DenseTimeSeriesCalculation) = {
    this(startDate, points, dateCalculation, false, 0)
  }

  //-------------------------------------------------------------------------
  override def isEmpty(): Boolean = {
    // threadsafe via racy single-check idiom
    var e = isEmptyCache
    if (e == 0) {
      e = calculateIsEmpty()
    }
    e < 0
  }

  // extracted to aid inlining
  private def calculateIsEmpty(): Int = {
    val s = sizeCache
    var any = false
    // take cached size if already calculated
    if (s != 0) {
      any = s > 1
    } else {
      var i = 0
      while (!any && i < points.length) {
        if (isValidPoint(points(i))) {
          any = true
        }
        i += 1
      }
      if (!any) {
        // set size to 0 if it wasn't calculated
        this.sizeCache = 1
      }
    }
    val e = if (any) 1 else -1
    this.isEmptyCache = e
    e
  }

  override def size(): Int = {
    // threadsafe via racy single-check idiom
    var s = sizeCache
    if (s == 0) {
      s = calculateSize()
    }
    // size field is the actual size plus 1
    s - 1
  }

  // extracted to aid inlining
  private def calculateSize(): Int = {
    var s = 1
    // check if we are not known empty
    if (isEmptyCache >= 0) {
      var i = 0
      while (i < points.length) {
        if (isValidPoint(points(i))) {
          s += 1
        }
        i += 1
      }
    }
    sizeCache = s
    isEmptyCache = if (s > 1) 1 else -1
    s
  }

  override def containsDate(date: LocalDate): Boolean = {
    get(date).isPresent()
  }

  override def get(date: LocalDate): OptionalDouble = {
    if (!isEmpty() && !date.isBefore(startDate) && dateCalculation.allowsDate(date)) {
      val position = dateCalculation.calculatePosition(startDate, date)
      if (position < points.length) {
        val value = points(position)
        if (isValidPoint(value)) {
          return OptionalDouble.of(value)
        }
      }
    }
    OptionalDouble.empty()
  }

  //-------------------------------------------------------------------------
  private def reversedValidIndices(): IntStream = {
    // As there is no way of constructing an IntStream from
    // n to m where n > m, we go from -n to m and then
    // take the additive inverse (sigh!)
    IntStream.rangeClosed(1 - points.length, 0)
        .map(i => -i)
        .filter(i => isValidIndex(i))
  }

  private def calculateDateFromPosition(i: Int): LocalDate = {
    dateCalculation.calculateDateFromPosition(startDate, i)
  }

  override def getLatestDate(): LocalDate = {
    val exceptionSupplier: Supplier[NoSuchElementException] =
      () => new NoSuchElementException("Unable to return latest date, time-series is empty")
    reversedValidIndices()
        .mapToObj[LocalDate](i => calculateDateFromPosition(i))
        .findFirst()
        .orElseThrow(exceptionSupplier)
  }

  override def getLatestValue(): Double = {
    val exceptionSupplier: Supplier[NoSuchElementException] =
      () => new NoSuchElementException("Unable to return latest value, time-series is empty")
    reversedValidIndices()
        .mapToDouble(i => points(i))
        .findFirst()
        .orElseThrow(exceptionSupplier)
  }

  //-------------------------------------------------------------------------
  override def subSeries(startInclusive: LocalDate, endExclusive: LocalDate): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(startInclusive, "startInclusive")
    ArgChecker.notNull(endExclusive, "endExclusive")
    if (endExclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException(
          "Invalid sub series, end before start: " + startInclusive + " to " + endExclusive)
    }

    // special case when this is empty or when the dates are the same
    // or the series don't intersect
    if (isEmpty() ||
        startInclusive.equals(endExclusive) ||
        !startDate.isBefore(endExclusive) ||
        startInclusive.isAfter(getLatestDate())) {
      return LocalDateDoubleTimeSeries.empty()
    }

    val resolvedStart = dateCalculation.adjustDate(Ordering.natural[LocalDate]().max(startInclusive, startDate))
    val startIndex = dateCalculation.calculatePosition(startDate, resolvedStart)
    val endIndex = dateCalculation.calculatePosition(startDate, endExclusive)
    new DenseLocalDateDoubleTimeSeries(
        resolvedStart,
        Arrays.copyOfRange(points, Math.max(0, startIndex), Math.min(points.length, endIndex)),
        dateCalculation,
        true,
        0)
  }

  override def headSeries(numPoints: Int): LocalDateDoubleTimeSeries = {
    ArgChecker.notNegative(numPoints, "numPoints")

    if (numPoints == 0) {
      return LocalDateDoubleTimeSeries.empty()
    } else if (numPoints > size()) {
      return this
    }
    val endPosition = findHeadPoints(numPoints)
    new DenseLocalDateDoubleTimeSeries(startDate, Arrays.copyOf(points, endPosition), dateCalculation)
  }

  private def findHeadPoints(required: Int): Int = {
    // Take enough points that aren't NaN
    // else we need the entire series
    validIndices()
        .skip(required.toLong)
        .findFirst()
        .orElse(points.length)
  }

  override def tailSeries(numPoints: Int): LocalDateDoubleTimeSeries = {
    ArgChecker.notNegative(numPoints, "numPoints")

    if (numPoints == 0) {
      return LocalDateDoubleTimeSeries.empty()
    } else if (numPoints > size()) {
      return this
    }

    val startPoint = findTailPoints(numPoints)

    new DenseLocalDateDoubleTimeSeries(
        calculateDateFromPosition(startPoint),
        Arrays.copyOfRange(points, startPoint, points.length),
        dateCalculation)
  }

  private def findTailPoints(required: Int): Int = {
    reversedValidIndices()
        .skip((required - 1).toLong)
        .findFirst()
        .orElse(0)
  }

  //-------------------------------------------------------------------------
  override def stream(): Stream[LocalDateDoublePoint] = {
    validIndices()
        .mapToObj[LocalDateDoublePoint](i => LocalDateDoublePoint.of(calculateDateFromPosition(i), points(i)))
  }

  override def values(): DoubleStream = {
    Arrays.stream(points).filter(d => isValidPoint(d))
  }

  override def dates(): Stream[LocalDate] = {
    validIndices()
        .mapToObj[LocalDate](i => calculateDateFromPosition(i))
  }

  private def validIndices(): IntStream = {
    IntStream.range(0, points.length)
        .filter(i => isValidIndex(i))
  }

  private def isValidIndex(i: Int): Boolean = {
    isValidPoint(points(i))
  }

  //-------------------------------------------------------------------------
  override def filter(predicate: ObjDoublePredicate[LocalDate]): LocalDateDoubleTimeSeries = {
    val filteredPoints: Stream[LocalDateDoublePoint] =
      stream().filter(pt => predicate.test(pt.getDate(), pt.getValue()))

    // As we may have changed the density of the series by filtering
    // go via the builder to get the best implementation
    new LocalDateDoubleTimeSeriesBuilder(filteredPoints).build()
  }

  override def mapDates(mapper: Function[? >: LocalDate, ? <: LocalDate]): LocalDateDoubleTimeSeries = {
    val mappedDates: List[LocalDate] = dates().map[LocalDate](mapper).collect(Guavate.toImmutableList[LocalDate]())
    mappedDates.stream().reduce((earlier, later) => checkAscending(earlier, later))
    LocalDateDoubleTimeSeries.builder().putAll(mappedDates, Doubles.asList(points*)).build()
  }

  override def mapValues(mapper: DoubleUnaryOperator): LocalDateDoubleTimeSeries = {
    val values = DoubleStream.of(points*).map(d => if (isValidPoint(d)) applyMapper(mapper, d) else d)
    new DenseLocalDateDoubleTimeSeries(startDate, values.toArray(), dateCalculation, true, sizeCache)
  }

  private def applyMapper(mapper: DoubleUnaryOperator, d: Double): Double = {
    val value = mapper.applyAsDouble(d)
    if (!isValidPoint(value)) {
      throw new IllegalArgumentException("Mapper must not map to NaN")
    }
    value
  }

  private def isValidPoint(d: Double): Boolean = {
    !java.lang.Double.isNaN(d)
  }

  override def forEach(action: ObjDoubleConsumer[LocalDate]): Unit = {
    validIndices().forEach(i => action.accept(calculateDateFromPosition(i), points(i)))
  }

  override def toBuilder(): LocalDateDoubleTimeSeriesBuilder = {
    new LocalDateDoubleTimeSeriesBuilder(stream())
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Checks the dates are in ascending order, throws an exception if not.
   *
   * @param earlier  the date that should be earlier
   * @param later  the date that should be later
   * @return the later date if it is after the earlier date, otherwise throw an exception
   * @throws java.lang.IllegalArgumentException if the dates are not in ascending order
   */
  private def checkAscending(earlier: LocalDate, later: LocalDate): LocalDate = {
    if (earlier.isBefore(later)) {
      return later
    }
    throw new IllegalArgumentException(
        Messages.format(
            "Dates must be in ascending order after calling mapDates but {} and {} are not",
            earlier,
            later))
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): DenseLocalDateDoubleTimeSeries.Meta = {
    DenseLocalDateDoubleTimeSeries.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * Gets date corresponding to first element in the array. All other
   * values can be calculated using date arithmetic to find
   * correct point.
   * @return the value of the property, not null
   */
  def getStartDate(): LocalDate = {
    startDate
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the values in the series.
   * The date for each value is calculated using the position
   * in the array and the start date.
   * @return the value of the property, not null
   */
  private def getPoints(): Array[Double] = {
    points.clone()
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether we should store data for the weekends (NaN will be stored
   * if no data is available).
   * @return the value of the property, not null
   */
  private def getDateCalculation(): DenseTimeSeriesCalculation = {
    dateCalculation
  }

  //-----------------------------------------------------------------------
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) {
      true
    } else if (obj != null && (obj.getClass() eq this.getClass())) {
      val other = obj.asInstanceOf[DenseLocalDateDoubleTimeSeries]
      JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(points, other.points) &&
          JodaBeanUtils.equal(dateCalculation, other.dateCalculation)
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    var hash = getClass().hashCode()
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate)
    hash = hash * 31 + JodaBeanUtils.hashCode(points)
    hash = hash * 31 + JodaBeanUtils.hashCode(dateCalculation)
    hash
  }

  override def toString(): String = {
    val buf = new java.lang.StringBuilder(128)
    buf.append("DenseLocalDateDoubleTimeSeries{")
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ')
    buf.append("points").append('=').append(JodaBeanUtils.toString(points)).append(',').append(' ')
    buf.append("dateCalculation").append('=').append(JodaBeanUtils.toString(dateCalculation))
    buf.append('}')
    buf.toString()
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

private[timeseries] object DenseLocalDateDoubleTimeSeries {

  @static private val REGISTERED: Boolean = {
    MetaBean.register(DenseLocalDateDoubleTimeSeries.Meta.INSTANCE)
    true
  }

  /**
   * Enum indicating whether there are positions in the points
   * array for weekends and providing the different date
   * calculations for each case.
   */
  private[timeseries] enum DenseTimeSeriesCalculation extends java.lang.Enum[DenseTimeSeriesCalculation] {

    /**
     * Data is not held for weekends.
     */
    case SKIP_WEEKENDS
    /**
     * Data is held for weekends.
     */
    case INCLUDE_WEEKENDS

    /**
     * Calculates the position in the array where the supplied date should
     * be located given a start date. As no information is held about the
     * actual array, callers must check array bounds.
     *
     * @param startDate  the start date for the series (the value for this
     *   entry will be stored at position 0 in the array)
     * @param date  the date to calculate a position for
     * @return the position in the array where the date would be located
     */
    private[timeseries] def calculatePosition(startDate: LocalDate, date: LocalDate): Int = {
      this match {
        case SKIP_WEEKENDS =>
          val unadjusted = DAYS.between(startDate, date).toInt
          // If the day for the start date is after the day of the date of
          // interest then there is an additional weekend that the
          // integer division will not handle
          // compare:
          //   Tues 8th -> Wed 16th, 8 days span, 8 / 7 = 1 weekend, correct so not further adjustment
          //   Tues 8th -> Mon 14th, 6 days span, 6 / 7 = 0 weekend, incorrect so we need to add adjustment
          val weekendAdjustment = if (startDate.getDayOfWeek().compareTo(date.getDayOfWeek()) > 0) 1 else 0
          val numWeekends = (unadjusted / 7) + weekendAdjustment
          unadjusted - (2 * numWeekends)
        case INCLUDE_WEEKENDS =>
          DAYS.between(startDate, date).toInt
      }
    }

    /**
     * Given a start date and a position in an array, calculate what date
     * the position holds data for.
     *
     * @param startDate  the start date for the series (the value for this
     *   entry will be stored at position 0 in the array)
     * @param position  the position in the array to calculate a date for
     * @return the date the position in the array holds data for
     */
    private[timeseries] def calculateDateFromPosition(startDate: LocalDate, position: Int): LocalDate = {
      this match {
        case SKIP_WEEKENDS =>
          val numWeekends = position / 5
          val remaining = position % 5
          // As above we add adjustment for an uncaptured weekend
          val endPointAdjustment = if (remaining < (6 - startDate.get(DAY_OF_WEEK))) 0 else 2
          startDate.plusDays(((7 * numWeekends) + remaining + endPointAdjustment).toLong)
        case INCLUDE_WEEKENDS =>
          startDate.plusDays(position.toLong)
      }
    }

    /**
     * Indicates if the specified date would be a possible date
     * for the calculation.
     *
     * @param date  the date to check
     * @return true if the calculation would allow the date
     */
    private[timeseries] def allowsDate(date: LocalDate): Boolean = {
      this match {
        case SKIP_WEEKENDS =>
          !DenseTimeSeriesCalculation.isWeekend(date)
        case INCLUDE_WEEKENDS =>
          true
      }
    }

    /**
     * Adjusts the supplied data such that it is a valid
     * date from the calculation's point of view.
     *
     * @param date  the date to adjust
     * @return the adjusted date
     */
    def adjustDate(date: LocalDate): LocalDate = {
      this match {
        case SKIP_WEEKENDS =>
          if (allowsDate(date)) date else date.plusDays((8 - date.get(DAY_OF_WEEK)).toLong)
        case INCLUDE_WEEKENDS =>
          date
      }
    }
  }

  private[timeseries] object DenseTimeSeriesCalculation {

    // Sufficient for the moment, in the future we may need to
    // vary depending on a non-Western weekend
    private def isWeekend(date: LocalDate): Boolean = {
      date.get(DAY_OF_WEEK) > 5
    }
  }

  /**
   * Package protected factory method intended to be called
   * by the `LocalDateDoubleTimeSeriesBuilder`. As such
   * all the information passed is assumed to be consistent.
   *
   * @param startDate  the earliest date included in the time-series
   * @param endDate  the latest date included in the time-series
   * @param values  stream holding the time-series points
   * @param dateCalculation  the date calculation method to be used
   * @return a new time-series
   */
  // @static because the inherited static LocalDateDoubleTimeSeries.of suppresses the static forwarder
  @static def of(
      startDate: LocalDate,
      endDate: LocalDate,
      values: Stream[LocalDateDoublePoint],
      dateCalculation: DenseTimeSeriesCalculation): LocalDateDoubleTimeSeries = {

    val points = new Array[Double](dateCalculation.calculatePosition(startDate, endDate) + 1)
    Arrays.fill(points, java.lang.Double.NaN)
    var size = 1
    val it = Guavate.in(values).iterator()
    while (it.hasNext()) {
      val pt = it.next()
      points(dateCalculation.calculatePosition(startDate, pt.getDate())) = pt.getValue()
      size += 1
    }
    new DenseLocalDateDoubleTimeSeries(startDate, points, dateCalculation, true, size)
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `DenseLocalDateDoubleTimeSeries`.
   * @return the meta-bean, not null
   */
  def meta(): DenseLocalDateDoubleTimeSeries.Meta = {
    DenseLocalDateDoubleTimeSeries.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `DenseLocalDateDoubleTimeSeries`.
   */
  private[timeseries] final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `startDate` property.
     */
    private val startDateProperty: MetaProperty[LocalDate] = DirectMetaProperty.ofImmutable(
        this, "startDate", classOf[DenseLocalDateDoubleTimeSeries], classOf[LocalDate])
    /**
     * The meta-property for the `points` property.
     */
    private val pointsProperty: MetaProperty[Array[Double]] = DirectMetaProperty.ofImmutable(
        this, "points", classOf[DenseLocalDateDoubleTimeSeries], classOf[Array[Double]])
    /**
     * The meta-property for the `dateCalculation` property.
     */
    private val dateCalculationProperty: MetaProperty[DenseTimeSeriesCalculation] = DirectMetaProperty.ofImmutable(
        this, "dateCalculation", classOf[DenseLocalDateDoubleTimeSeries], classOf[DenseTimeSeriesCalculation])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "points",
        "dateCalculation")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          startDateProperty
        case -982754077 => // points
          pointsProperty
        case -152592837 => // dateCalculation
          dateCalculationProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: DenseLocalDateDoubleTimeSeries] = {
      new DenseLocalDateDoubleTimeSeries.Builder()
    }

    override def beanType(): Class[? <: DenseLocalDateDoubleTimeSeries] = {
      classOf[DenseLocalDateDoubleTimeSeries]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `startDate` property.
     * @return the meta-property, not null
     */
    def startDate(): MetaProperty[LocalDate] = {
      startDateProperty
    }

    /**
     * The meta-property for the `points` property.
     * @return the meta-property, not null
     */
    def points(): MetaProperty[Array[Double]] = {
      pointsProperty
    }

    /**
     * The meta-property for the `dateCalculation` property.
     * @return the meta-property, not null
     */
    def dateCalculation(): MetaProperty[DenseTimeSeriesCalculation] = {
      dateCalculationProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          bean.asInstanceOf[DenseLocalDateDoubleTimeSeries].getStartDate()
        case -982754077 => // points
          bean.asInstanceOf[DenseLocalDateDoubleTimeSeries].getPoints()
        case -152592837 => // dateCalculation
          bean.asInstanceOf[DenseLocalDateDoubleTimeSeries].getDateCalculation()
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

  private[timeseries] object Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    private[timeseries] val INSTANCE: Meta = new Meta()
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for `DenseLocalDateDoubleTimeSeries`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[DenseLocalDateDoubleTimeSeries] {

    private var startDate: LocalDate = uninitialized
    private var points: Array[Double] = uninitialized
    private var dateCalculation: DenseTimeSeriesCalculation = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          startDate
        case -982754077 => // points
          points
        case -152592837 => // dateCalculation
          dateCalculation
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case -2129778896 => // startDate
          this.startDate = newValue.asInstanceOf[LocalDate]
        case -982754077 => // points
          this.points = newValue.asInstanceOf[Array[Double]]
        case -152592837 => // dateCalculation
          this.dateCalculation = newValue.asInstanceOf[DenseTimeSeriesCalculation]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): DenseLocalDateDoubleTimeSeries = {
      new DenseLocalDateDoubleTimeSeries(
          startDate,
          points,
          dateCalculation)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(128)
      buf.append("DenseLocalDateDoubleTimeSeries.Builder{")
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ')
      buf.append("points").append('=').append(JodaBeanUtils.toString(points)).append(',').append(' ')
      buf.append("dateCalculation").append('=').append(JodaBeanUtils.toString(dateCalculation))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
