/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamException
import java.io.ObjectStreamField
import java.io.Serializable
import java.time.LocalDate
import java.util.Arrays
import java.util.Collection
import java.util.Map
import java.util.NoSuchElementException
import java.util.OptionalDouble
import java.util.function.DoubleUnaryOperator
import java.util.function.Function
import java.util.function.ObjDoubleConsumer
import java.util.stream.Collectors
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

import com.google.common.primitives.Doubles
import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Messages
import com.opengamma.strata.collect.function.ObjDoublePredicate

/**
 * A immutable implementation of `LocalDateDoubleTimeSeries` where the
 * data stored is expected to be relatively sparse.
 *
 * A sparse time-series has a relatively low density of dates with values.
 * For example, a few points spread throughout a year.
 * If more or less continuous data is being used then `DenseLocalDateDoubleTimeSeries`
 * is likely to be a better choice for the data.
 *
 * This implementation uses arrays internally.
 */
@BeanDefinition(builderScope = "private", metaScope = "package")
@SerialVersionUID(1L)
private[timeseries] final class SparseLocalDateDoubleTimeSeries private (
    // the Java fields were named 'dates' and 'values', which clash with dates() and values() in Scala;
    // Joda-Beans finds the '_' prefixed fields, and the serialized form is mapped explicitly
    // by serialPersistentFields/writeObject/readObject
    /**
     * The dates in the series.
     * The dates are ordered from earliest to latest.
     */
    @(PropertyDefinition @field)(get = "manual", validate = "notNull")
    @transient private val _dates: Array[LocalDate],
    /**
     * The values in the series.
     * The date for each value is at the matching array index.
     */
    @(PropertyDefinition @field)(get = "manual", validate = "notNull")
    @transient private val _values: Array[Double],
    trusted: Boolean)
  extends ImmutableBean with Serializable with LocalDateDoubleTimeSeries {

  // the primary constructor exists to avoid clones where possible
  // because Joda-Beans owns the main constructor, this one has a weird flag
  // use createUnsafe() instead of calling this directly

  /** Holds the deserialized instance between readObject() and readResolve(). */
  @transient private var deserialized: SparseLocalDateDoubleTimeSeries = uninitialized

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, validating the supplied arrays.
   *
   * The arrays are cloned as this constructor is called from Joda-Beans.
   *
   * @param dates  the dates
   * @param values  the values
   */
  @ImmutableConstructor
  private def this(dates: Array[LocalDate], values: Array[Double]) = {
    this(
        {
          ArgChecker.noNulls(dates, "dates")
          ArgChecker.notNull(values, "values")
          SparseLocalDateDoubleTimeSeries.validate(dates, values)
          dates.clone()
        },
        values.clone(),
        true)
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dates in the series.
   * The dates are ordered from earliest to latest.
   * @return the value of the property, not null
   */
  private def getDates(): Array[LocalDate] = {
    _dates.clone()
  }

  /**
   * Gets the values in the series.
   * The date for each value is at the matching array index.
   * @return the value of the property, not null
   */
  private def getValues(): Array[Double] = {
    _values.clone()
  }

  //-------------------------------------------------------------------------
  override def size(): Int = {
    _dates.length
  }

  override def isEmpty(): Boolean = {
    _dates.length == 0
  }

  override def containsDate(date: LocalDate): Boolean = {
    findDatePosition(date) >= 0
  }

  override def get(date: LocalDate): OptionalDouble = {
    val position = findDatePosition(date)
    if (position >= 0) OptionalDouble.of(_values(position)) else OptionalDouble.empty()
  }

  private def findDatePosition(date: LocalDate): Int = {
    Arrays.binarySearch(_dates.asInstanceOf[Array[Object]], date)
  }

  //-------------------------------------------------------------------------
  override def getLatestDate(): LocalDate = {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty")
    }
    _dates(_dates.length - 1)
  }

  override def getLatestValue(): Double = {
    if (isEmpty()) {
      throw new NoSuchElementException("Unable to return latest, time-series is empty")
    }
    _values(_values.length - 1)
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
    if (isEmpty() || startInclusive.equals(endExclusive)) {
      return SparseLocalDateDoubleTimeSeries.EMPTY
    }
    // where in the array would start/end be (whether or not it's actually in the series)
    var startPos = Arrays.binarySearch(_dates.asInstanceOf[Array[Object]], startInclusive)
    startPos = if (startPos >= 0) startPos else -startPos - 1
    var endPos = Arrays.binarySearch(_dates.asInstanceOf[Array[Object]], endExclusive)
    endPos = if (endPos >= 0) endPos else -endPos - 1
    // create sub-series
    val timesArray = Arrays.copyOfRange(_dates, startPos, endPos)
    val valuesArray = Arrays.copyOfRange(_values, startPos, endPos)
    SparseLocalDateDoubleTimeSeries.createUnsafe(timesArray, valuesArray)
  }

  override def headSeries(numPoints: Int): LocalDateDoubleTimeSeries = {
    ArgChecker.notNegative(numPoints, "numPoints")
    if (numPoints == 0) {
      return SparseLocalDateDoubleTimeSeries.EMPTY
    } else if (numPoints >= size()) {
      return this
    }
    val datesArray = Arrays.copyOfRange(_dates, 0, numPoints)
    val valuesArray = Arrays.copyOfRange(_values, 0, numPoints)
    SparseLocalDateDoubleTimeSeries.createUnsafe(datesArray, valuesArray)
  }

  override def tailSeries(numPoints: Int): LocalDateDoubleTimeSeries = {
    ArgChecker.notNegative(numPoints, "numPoints")
    if (numPoints == 0) {
      return SparseLocalDateDoubleTimeSeries.EMPTY
    } else if (numPoints >= size()) {
      return this
    }
    val datesArray = Arrays.copyOfRange(_dates, size() - numPoints, size())
    val valuesArray = Arrays.copyOfRange(_values, size() - numPoints, size())
    SparseLocalDateDoubleTimeSeries.createUnsafe(datesArray, valuesArray)
  }

  //-------------------------------------------------------------------------
  override def stream(): Stream[LocalDateDoublePoint] = {
    IntStream.range(0, size()).mapToObj[LocalDateDoublePoint](i => LocalDateDoublePoint.of(_dates(i), _values(i)))
  }

  override def dates(): Stream[LocalDate] = {
    Stream.of(_dates*)
  }

  override def values(): DoubleStream = {
    DoubleStream.of(_values*)
  }

  //-------------------------------------------------------------------------
  override def forEach(action: ObjDoubleConsumer[LocalDate]): Unit = {
    ArgChecker.notNull(action, "action")
    var i = 0
    while (i < size()) {
      action.accept(_dates(i), _values(i))
      i += 1
    }
  }

  override def mapDates(mapper: Function[? >: LocalDate, ? <: LocalDate]): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(mapper, "mapper")
    val dates: Array[LocalDate] =
      Arrays.stream(this._dates).map[LocalDate](mapper).toArray[LocalDate](size => new Array[LocalDate](size))
    // Check the dates are still in ascending order after the mapping
    Arrays.stream(dates).reduce((earlier, later) => checkAscending(earlier, later))
    SparseLocalDateDoubleTimeSeries.createUnsafe(dates, _values)
  }

  override def mapValues(mapper: DoubleUnaryOperator): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(mapper, "mapper")
    SparseLocalDateDoubleTimeSeries.createUnsafe(_dates, DoubleStream.of(_values*).map(mapper).toArray())
  }

  override def filter(predicate: ObjDoublePredicate[LocalDate]): LocalDateDoubleTimeSeries = {
    ArgChecker.notNull(predicate, "predicate")
    // build up result in arrays keeping track of count of retained dates
    val resDates = new Array[LocalDate](size())
    val resValues = new Array[Double](size())
    var resCount = 0
    var i = 0
    while (i < size()) {
      if (predicate.test(_dates(i), _values(i))) {
        resDates(resCount) = _dates(i)
        resValues(resCount) = _values(i)
        resCount += 1
      }
      i += 1
    }
    SparseLocalDateDoubleTimeSeries.createUnsafe(Arrays.copyOf(resDates, resCount), Arrays.copyOf(resValues, resCount))
  }

  //-------------------------------------------------------------------------
  override def toBuilder(): LocalDateDoubleTimeSeriesBuilder = {
    new LocalDateDoubleTimeSeriesBuilder(_dates, _values)
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this time-series is equal to another time-series.
   *
   * Compares this `LocalDateDoubleTimeSeries` with another ensuring
   * that the dates and values are the same.
   *
   * @param obj  the object to check, null returns false
   * @return true if this is equal to the other date
   */
  override def equals(obj: Any): Boolean = {
    if (this eq obj.asInstanceOf[AnyRef]) {
      return true
    }
    obj match {
      case other: SparseLocalDateDoubleTimeSeries =>
        Arrays.equals(_dates.asInstanceOf[Array[Object]], other._dates.asInstanceOf[Array[Object]]) &&
            Arrays.equals(_values, other._values)
      case _ =>
        false
    }
  }

  /**
   * A hash code for this time-series.
   *
   * @return a suitable hash code
   */
  override def hashCode(): Int = {
    31 * Arrays.hashCode(_dates.asInstanceOf[Array[Object]]) + Arrays.hashCode(_values)
  }

  /**
   * Returns a string representation of the time-series.
   *
   * @return the string
   */
  override def toString(): String = {
    stream()
        .map[String](pt => pt.toString())
        .collect(Collectors.joining(", ", "[", "]"))
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

  //-------------------------------------------------------------------------
  // writes the baseline serialized form, fields 'dates' and 'values'
  @throws[IOException]
  private def writeObject(out: ObjectOutputStream): Unit = {
    val fields = out.putFields()
    fields.put("dates", _dates)
    fields.put("values", _values)
    out.writeFields()
  }

  // reads the baseline serialized form, without validation as per default Java deserialization
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream): Unit = {
    val fields = in.readFields()
    deserialized = new SparseLocalDateDoubleTimeSeries(
        fields.get("dates", null).asInstanceOf[Array[LocalDate]],
        fields.get("values", null).asInstanceOf[Array[Double]],
        true)
  }

  @throws[ObjectStreamException]
  private def readResolve(): Object = {
    deserialized
  }

  //------------------------- AUTOGENERATED START -------------------------
  override def metaBean(): SparseLocalDateDoubleTimeSeries.Meta = {
    SparseLocalDateDoubleTimeSeries.Meta.INSTANCE
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

private[timeseries] object SparseLocalDateDoubleTimeSeries {

  /**
   * An empty time-series.
   */
  @static final val EMPTY: LocalDateDoubleTimeSeries =
    new SparseLocalDateDoubleTimeSeries(new Array[LocalDate](0), new Array[Double](0))

  @static private val REGISTERED: Boolean = {
    MetaBean.register(SparseLocalDateDoubleTimeSeries.Meta.INSTANCE)
    true
  }

  /** The serialized form, matching the original Java field names and types. */
  @static private val serialPersistentFields: Array[ObjectStreamField] = Array(
      new ObjectStreamField("dates", classOf[Array[LocalDate]]),
      new ObjectStreamField("values", classOf[Array[Double]]))

  //-------------------------------------------------------------------------
  /**
   * Obtains a time-series from matching arrays of dates and values.
   *
   * The two arrays must be the same size and must be sorted from earliest to latest.
   *
   * @param dates  the date list
   * @param values  the value list
   * @return the time-series
   */
  // @static because the inherited static LocalDateDoubleTimeSeries.of suppresses the static forwarder
  @static def of(dates: Collection[LocalDate], values: Collection[java.lang.Double]): SparseLocalDateDoubleTimeSeries = {
    ArgChecker.noNulls(dates, "dates")
    ArgChecker.noNulls(values, "values")
    val datesArray = dates.toArray(new Array[LocalDate](dates.size()))
    val valuesArray = Doubles.toArray(values)
    validate(datesArray, valuesArray)
    createUnsafe(datesArray, valuesArray)
  }

  // creates time-series by directly assigning the input arrays
  // must only be called when safe to do so
  private def createUnsafe(dates: Array[LocalDate], values: Array[Double]): SparseLocalDateDoubleTimeSeries = {
    new SparseLocalDateDoubleTimeSeries(dates, values, true)
  }

  // validates the arrays are same length and in order
  private def validate(dates: Array[LocalDate], values: Array[Double]): Unit = {
    ArgChecker.isTrue(dates.length == values.length,
        "Arrays are of different sizes - dates: {}, values: {}",
        Integer.valueOf(dates.length), Integer.valueOf(values.length))
    var maxDate = LocalDate.MIN
    var i = 0
    while (i < dates.length) {
      val date = dates(i)
      ArgChecker.isTrue(date.isAfter(maxDate),
          "Dates must be in ascending order but: {} is not after: {}", date, maxDate)
      maxDate = date
      i += 1
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for `SparseLocalDateDoubleTimeSeries`.
   * @return the meta-bean, not null
   */
  def meta(): SparseLocalDateDoubleTimeSeries.Meta = {
    SparseLocalDateDoubleTimeSeries.Meta.INSTANCE
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for `SparseLocalDateDoubleTimeSeries`.
   */
  private[timeseries] final class Meta private () extends DirectMetaBean {

    /**
     * The meta-property for the `dates` property.
     */
    private val datesProperty: MetaProperty[Array[LocalDate]] = DirectMetaProperty.ofImmutable(
        this, "dates", classOf[SparseLocalDateDoubleTimeSeries], classOf[Array[LocalDate]])
    /**
     * The meta-property for the `values` property.
     */
    private val valuesProperty: MetaProperty[Array[Double]] = DirectMetaProperty.ofImmutable(
        this, "values", classOf[SparseLocalDateDoubleTimeSeries], classOf[Array[Double]])
    /**
     * The meta-properties.
     */
    private val metaPropertyMapValue: Map[String, MetaProperty[?]] = new DirectMetaPropertyMap(
        this, null,
        "dates",
        "values")

    override protected def metaPropertyGet(propertyName: String): MetaProperty[?] = {
      propertyName.hashCode() match {
        case 95356549 => // dates
          datesProperty
        case -823812830 => // values
          valuesProperty
        case _ =>
          super.metaPropertyGet(propertyName)
      }
    }

    override def builder(): BeanBuilder[? <: SparseLocalDateDoubleTimeSeries] = {
      new SparseLocalDateDoubleTimeSeries.Builder()
    }

    override def beanType(): Class[? <: SparseLocalDateDoubleTimeSeries] = {
      classOf[SparseLocalDateDoubleTimeSeries]
    }

    override def metaPropertyMap(): Map[String, MetaProperty[?]] = {
      metaPropertyMapValue
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the `dates` property.
     * @return the meta-property, not null
     */
    def dates(): MetaProperty[Array[LocalDate]] = {
      datesProperty
    }

    /**
     * The meta-property for the `values` property.
     * @return the meta-property, not null
     */
    def values(): MetaProperty[Array[Double]] = {
      valuesProperty
    }

    //-----------------------------------------------------------------------
    override protected def propertyGet(bean: Bean, propertyName: String, quiet: Boolean): Object = {
      propertyName.hashCode() match {
        case 95356549 => // dates
          bean.asInstanceOf[SparseLocalDateDoubleTimeSeries].getDates()
        case -823812830 => // values
          bean.asInstanceOf[SparseLocalDateDoubleTimeSeries].getValues()
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
   * The bean-builder for `SparseLocalDateDoubleTimeSeries`.
   */
  private final class Builder () extends DirectPrivateBeanBuilder[SparseLocalDateDoubleTimeSeries] {

    private var dates: Array[LocalDate] = uninitialized
    private var values: Array[Double] = uninitialized

    //-----------------------------------------------------------------------
    override def get(propertyName: String): Object = {
      propertyName.hashCode() match {
        case 95356549 => // dates
          dates
        case -823812830 => // values
          values
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
    }

    override def set(propertyName: String, newValue: Object): Builder = {
      propertyName.hashCode() match {
        case 95356549 => // dates
          this.dates = newValue.asInstanceOf[Array[LocalDate]]
        case -823812830 => // values
          this.values = newValue.asInstanceOf[Array[Double]]
        case _ =>
          throw new NoSuchElementException("Unknown property: " + propertyName)
      }
      this
    }

    override def build(): SparseLocalDateDoubleTimeSeries = {
      new SparseLocalDateDoubleTimeSeries(
          dates,
          values)
    }

    //-----------------------------------------------------------------------
    override def toString(): String = {
      val buf = new java.lang.StringBuilder(96)
      buf.append("SparseLocalDateDoubleTimeSeries.Builder{")
      buf.append("dates").append('=').append(JodaBeanUtils.toString(dates)).append(',').append(' ')
      buf.append("values").append('=').append(JodaBeanUtils.toString(values))
      buf.append('}')
      buf.toString()
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
