/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect

import com.google.common.base.CharMatcher

/**
 * Useful Guava character matchers.
 */
final class CharMatchers private ()

object CharMatchers {

  private val UPPER_LETTERS: CharMatcher = CharMatcher.inRange('A', 'Z')
  private val LOWER_LETTERS: CharMatcher = CharMatcher.inRange('a', 'z')
  private val LETTERS: CharMatcher = UPPER_LETTERS.or(LOWER_LETTERS)
  private val DIGITS: CharMatcher = CharMatcher.inRange('0', '9')
  private val LETTERS_DIGITS: CharMatcher = LETTERS.or(DIGITS)
  private val UPPER_HEX: CharMatcher = DIGITS.or(CharMatcher.inRange('A', 'F'))
  private val LOWER_HEX: CharMatcher = DIGITS.or(CharMatcher.inRange('a', 'f'))
  private val HEX: CharMatcher = UPPER_HEX.or(LOWER_HEX)

  //-------------------------------------------------------------------------
  /**
   * Returns the matcher for ASCII upper case letters.
   *
   * @return the matcher for ASCII upper case letters
   */
  def upperLetters(): CharMatcher = {
    UPPER_LETTERS
  }

  /**
   * Returns the matcher for ASCII upper case letters.
   *
   * @return the matcher for ASCII upper case letters
   */
  def lowerLetters(): CharMatcher = {
    LOWER_LETTERS
  }

  /**
   * Returns the matcher for ASCII letters.
   *
   * @return the matcher for ASCII letters
   */
  def letters(): CharMatcher = {
    LETTERS
  }

  /**
   * Returns the matcher for ASCII digits.
   *
   * @return the matcher for ASCII digits
   */
  def digits(): CharMatcher = {
    DIGITS
  }

  /**
   * Returns the matcher for ASCII letters and digits.
   *
   * @return the matcher for ASCII letters and digits
   */
  def lettersAndDigits(): CharMatcher = {
    LETTERS_DIGITS
  }

  /**
   * Returns the matcher for ASCII upper case hex characters.
   *
   * @return the matcher for ASCII upper case hex characters
   */
  def upperHex(): CharMatcher = {
    UPPER_HEX
  }

  /**
   * Returns the matcher for ASCII upper case hex characters.
   *
   * @return the matcher for ASCII upper case hex characters
   */
  def lowerHex(): CharMatcher = {
    LOWER_HEX
  }

  /**
   * Returns the matcher for ASCII hex characters.
   *
   * @return the matcher for ASCII hex characters
   */
  def hex(): CharMatcher = {
    HEX
  }

}
