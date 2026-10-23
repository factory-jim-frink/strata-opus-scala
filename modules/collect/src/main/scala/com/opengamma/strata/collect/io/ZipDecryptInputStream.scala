/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `com.alutam.ziputils` package.
 * https://bitbucket.org/matulic/ziputils/src
 * Changes:
 * - package name
 * - reformat
 * - error messages
 */
/*
 *  Copyright 2011, 2012 Martin Matula (martin@alutam.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.opengamma.strata.collect.io

import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException

/**
 * Input stream converting a password-protected zip to an unprotected zip.
 */
private[io] class ZipDecryptInputStream private[io] (stream: InputStream, password: Array[Char]) extends InputStream {

  import ZipDecryptInputStream.ZipSection
  import ZipDecryptInputStream.ZipState

  private val delegate: InputStream = stream
  private val keys: Array[Int] = new Array[Int](3)
  private val pwdKeys: Array[Int] = new Array[Int](3)

  private var state: ZipState = ZipState.SIGNATURE
  private var isEncrypted: Boolean = false
  private var section: ZipSection = null
  private var skipBytes: Int = 0
  private var compressedSize: Int = 0
  private var crc: Int = 0

  private var bufOffset: Int = ZipDecryptInputStream.BUF_SIZE
  private val buf: Array[Int] = new Array[Int](ZipDecryptInputStream.BUF_SIZE)

  pwdKeys(0) = 305419896
  pwdKeys(1) = 591751049
  pwdKeys(2) = 878082192
  locally {
    var i = 0
    while (i < password.length) {
      ZipDecryptInputStream.updateKeyArray((password(i) & 0xff).toByte, pwdKeys)
      i += 1
    }
  }

  @throws[IOException]
  override def read(): Int = {
    // errors throw UncheckedIOException, because IOException is caught somewhere causing the message to be lost
    var result = delegateRead()
    if (skipBytes == 0) {
      // the HEADER case falls through to the DATA case
      var processData = false
      state match {
        case ZipState.SIGNATURE =>
          if (!peekAheadEquals(ZipDecryptInputStream.LFH_SIGNATURE)) {
            state = ZipState.TAIL
          } else {
            section = ZipSection.FILE_HEADER
            skipBytes = 5
            state = ZipState.FLAGS
          }
        case ZipState.FLAGS =>
          isEncrypted = (result & 1) != 0
          if ((result & 64) == 64) {
            throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, ZIP has strong encryption"))
          }
          if ((result & 8) == 8) {
            compressedSize = -1
            state = ZipState.FN_LENGTH
            skipBytes = 19
          } else {
            state = ZipState.CRC
            skipBytes = 10
          }
          if (isEncrypted) {
            result -= 1
          }
        case ZipState.CRC =>
          crc = result
          state = ZipState.COMPRESSED_SIZE
        case ZipState.COMPRESSED_SIZE =>
          val values = new Array[Int](4)
          peekAhead(values)
          compressedSize = 0
          var valueInc = if (isEncrypted) ZipDecryptInputStream.DECRYPT_HEADER_SIZE else 0
          var i = 0
          while (i < 4) {
            compressedSize += values(i) << (8 * i)
            values(i) -= valueInc
            if (values(i) < 0) {
              valueInc = 1
              values(i) += 256
            } else {
              valueInc = 0
            }
            i += 1
          }
          overrideBuffer(values)
          result = values(0)
          if (section eq ZipSection.DATA_DESCRIPTOR) {
            state = ZipState.SIGNATURE
          } else {
            state = ZipState.FN_LENGTH
          }
          skipBytes = 7
        case ZipState.FN_LENGTH =>
          val values = new Array[Int](4)
          peekAhead(values)
          skipBytes = 3 + values(0) + values(2) + (values(1) + values(3)) * 256
          if (!isEncrypted) {
            if (compressedSize > 0) {
              throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, ZIP is not password protected"))
            }
            state = ZipState.SIGNATURE
          } else {
            state = ZipState.HEADER
          }
        case ZipState.HEADER =>
          section = ZipSection.FILE_DATA
          initKeys()
          var lastValue: Byte = 0
          var i = 0
          while (i < ZipDecryptInputStream.DECRYPT_HEADER_SIZE) {
            lastValue = (result ^ decryptByte()).toByte
            updateKeys(lastValue)
            result = delegateRead()
            i += 1
          }
          if ((lastValue & 0xff) != crc) {
            throw new UncheckedIOException(new IOException("Unable to decrypt ZIP file, wrong password"))
          }
          compressedSize -= ZipDecryptInputStream.DECRYPT_HEADER_SIZE
          state = ZipState.DATA
          // fall through
          processData = true
        case ZipState.DATA =>
          processData = true
        case _ =>
          // do nothing
      }
      if (processData) {
        if (compressedSize == -1 && peekAheadEquals(ZipDecryptInputStream.DD_SIGNATURE)) {
          section = ZipSection.DATA_DESCRIPTOR
          skipBytes = 5
          state = ZipState.CRC
        } else {
          result = (result ^ decryptByte()) & 0xff
          updateKeys(result.toByte)
          compressedSize -= 1
          if (compressedSize == 0) {
            state = ZipState.SIGNATURE
          }
        }
      }
    } else {
      skipBytes -= 1
    }
    result
  }

  @throws[IOException]
  private def delegateRead(): Int = {
    bufOffset += 1
    if (bufOffset >= ZipDecryptInputStream.BUF_SIZE) {
      fetchData(0)
      bufOffset = 0
    }
    buf(bufOffset)
  }

  @throws[IOException]
  private def peekAheadEquals(values: Array[Int]): Boolean = {
    prepareBuffer(values)
    var equal = true
    var i = 0
    while (equal && i < values.length) {
      if (buf(bufOffset + i) != values(i)) {
        equal = false
      }
      i += 1
    }
    equal
  }

  @throws[IOException]
  private def prepareBuffer(values: Array[Int]): Unit = {
    if (values.length > (ZipDecryptInputStream.BUF_SIZE - bufOffset)) {
      var i = bufOffset
      while (i < ZipDecryptInputStream.BUF_SIZE) {
        buf(i - bufOffset) = buf(i)
        i += 1
      }
      fetchData(ZipDecryptInputStream.BUF_SIZE - bufOffset)
      bufOffset = 0
    }
  }

  @throws[IOException]
  private def peekAhead(values: Array[Int]): Unit = {
    prepareBuffer(values)
    System.arraycopy(buf, bufOffset, values, 0, values.length)
  }

  @throws[IOException]
  private def overrideBuffer(values: Array[Int]): Unit = {
    prepareBuffer(values)
    System.arraycopy(values, 0, buf, bufOffset, values.length)
  }

  @throws[IOException]
  private def fetchData(offset: Int): Unit = {
    var i = offset
    var ended = false
    while (!ended && i < ZipDecryptInputStream.BUF_SIZE) {
      buf(i) = delegate.read()
      if (buf(i) == -1) {
        ended = true
      }
      i += 1
    }
  }

  @throws[IOException]
  override def close(): Unit = {
    delegate.close()
    super.close()
  }

  private def initKeys(): Unit = {
    System.arraycopy(pwdKeys, 0, keys, 0, keys.length)
  }

  private def updateKeys(charAt: Byte): Unit = {
    ZipDecryptInputStream.updateKeyArray(charAt, keys)
  }

  private def decryptByte(): Byte = {
    val temp = keys(2) | 2
    ((temp * (temp ^ 1)) >>> 8).toByte
  }

}

private[io] object ZipDecryptInputStream {

  private final val BUF_SIZE = 8

  private[io] val CRC_TABLE: Array[Int] = {
    val table = new Array[Int](256)
    // compute the table
    // (could also have it pre-computed - see http://snippets.dzone.com/tag/crc32)
    var i = 0
    while (i < 256) {
      var r = i
      var j = 0
      while (j < 8) {
        if ((r & 1) == 1) {
          r = (r >>> 1) ^ 0xedb88320
        } else {
          r >>>= 1
        }
        j += 1
      }
      table(i) = r
      i += 1
    }
    table
  }

  //-------------------------------------------------------------------------
  // from com.alutam.ziputils.ZipUtil
  private final val DECRYPT_HEADER_SIZE = 12
  private[io] val LFH_SIGNATURE: Array[Int] = Array(0x50, 0x4b, 0x03, 0x04)
  private[io] val DD_SIGNATURE: Array[Int] = Array(0x50, 0x4b, 0x07, 0x08)

  private[io] def updateKeyArray(charAt: Byte, keys: Array[Int]): Unit = {
    keys(0) = crc32(keys(0), charAt)
    keys(1) += keys(0) & 0xff
    keys(1) = keys(1) * 134775813 + 1
    keys(2) = crc32(keys(2), (keys(1) >> 24).toByte)
  }

  private def crc32(oldCrc: Int, charAt: Byte): Int = {
    ((oldCrc >>> 8) ^ CRC_TABLE((oldCrc ^ charAt) & 0xff))
  }

  private[io] enum ZipState extends java.lang.Enum[ZipState] {
    case SIGNATURE, FLAGS, COMPRESSED_SIZE, FN_LENGTH, EF_LENGTH, HEADER, DATA, TAIL, CRC
  }

  private[io] enum ZipSection extends java.lang.Enum[ZipSection] {
    case FILE_HEADER, FILE_DATA, DATA_DESCRIPTOR
  }
}
