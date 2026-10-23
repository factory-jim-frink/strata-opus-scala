/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io

import java.lang.StringBuilder
import java.util.List

import com.google.common.base.Strings
import com.opengamma.strata.collect.ArgChecker

/**
 * An ASCII table generator.
 *
 * Provides the ability to generate a simple ASCII table, typically used on the command line.
 * All data is provided as strings, with formatting the responsibility of the caller.
 *
 * @constructor Restricted constructor.
 */
final class AsciiTable private ()

object AsciiTable {

  /**
   * Line separator.
   */
  private val LINE_SEPARATOR: String = System.lineSeparator()

  //-------------------------------------------------------------------------
  /**
   * Generates the ASCII table.
   *
   * The caller specifies the headers for each column and the alignment to use,
   * plus the list of lists representing the data. All data is provided as strings,
   * with formatting the responsibility of the caller.
   *
   * @param headers  the table headers
   * @param alignments  the table alignments, must match the size of the headers
   * @param cells  the table cells, outer list of rows, inner list of columns
   * @return the table
   * @throws java.lang.IllegalArgumentException if the number of columns specified is inconsistent
   */
  def generate(
      headers: List[String],
      alignments: List[AsciiTableAlignment],
      cells: List[? <: List[String]]): String = {

    val colCount = alignments.size()
    val rowCount = cells.size()
    ArgChecker.isTrue(
        headers.size() == colCount,
        "Number of headers {} must match number of alignments {}",
        java.lang.Integer.valueOf(headers.size()),
        java.lang.Integer.valueOf(colCount))

    // find max length of each column
    val colLengths = new Array[Int](colCount)
    var colIdx = 0
    while (colIdx < colCount) {
      colLengths(colIdx) = headers.get(colIdx).length()
      colIdx += 1
    }
    var rowIdx = 0
    while (rowIdx < rowCount) {
      val row: List[String] = cells.get(rowIdx)
      ArgChecker.isTrue(
          row.size() == colCount,
          "Table of cells has incorrect number of columns {} in row {}",
          java.lang.Integer.valueOf(row.size()),
          java.lang.Integer.valueOf(rowIdx))
      var colIdx2 = 0
      while (colIdx2 < colCount) {
        colLengths(colIdx2) = Math.max(colLengths(colIdx2), row.get(colIdx2).length())
        colIdx2 += 1
      }
      rowIdx += 1
    }
    var colTotalLength = 3  // allow for last vertical separator and windows line separator
    colIdx = 0
    while (colIdx < colCount) {
      colTotalLength += colLengths(colIdx) + 3  // each column has two spaces and a vertical separator
      colIdx += 1
    }

    // write table
    val buf = new StringBuilder((rowCount + 3) * colTotalLength)
    writeSeparatorLine(buf, colLengths)
    writeDataLine(buf, colLengths, alignments, headers)
    writeSeparatorLine(buf, colLengths)
    rowIdx = 0
    while (rowIdx < rowCount) {
      writeDataLine(buf, colLengths, alignments, cells.get(rowIdx))
      rowIdx += 1
    }
    writeSeparatorLine(buf, colLengths)
    buf.toString()
  }

  // write a separator line
  private def writeSeparatorLine(buf: StringBuilder, colLengths: Array[Int]): Unit = {
    var colIdx = 0
    while (colIdx < colLengths.length) {
      buf.append('+')
      var i = 0
      while (i < colLengths(colIdx) + 2) {
        buf.append('-')
        i += 1
      }
      colIdx += 1
    }
    buf.append('+').append(LINE_SEPARATOR)
  }

  // write a data line
  private def writeDataLine(
      buf: StringBuilder,
      colLengths: Array[Int],
      alignments: List[AsciiTableAlignment],
      values: List[String]): Unit = {

    var colIdx = 0
    while (colIdx < colLengths.length) {
      val value = Strings.nullToEmpty(values.get(colIdx))
      buf.append('|')
          .append(' ')
          .append(formatValue(buf, colLengths(colIdx), alignments.get(colIdx), value))
          .append(' ')
      colIdx += 1
    }
    buf.append('|').append(LINE_SEPARATOR)
  }

  // writes a data item
  private def formatValue(
      buf: StringBuilder,
      colLength: Int,
      alignment: AsciiTableAlignment,
      value: String): String = {

    if (alignment eq AsciiTableAlignment.RIGHT) {
      Strings.padStart(value, colLength, ' ')
    } else {
      Strings.padEnd(value, colLength, ' ')
    }
  }

}
