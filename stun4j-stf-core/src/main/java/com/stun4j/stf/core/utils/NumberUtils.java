/*
 * Copyright 2022-? the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.stf.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class NumberUtils {

  public static boolean isCreatable(final Class<?> targetClass) {
    if (Byte.class == targetClass || byte.class == targetClass)
      return true;
    if (Short.class == targetClass || short.class == targetClass)
      return true;
    if (Integer.class == targetClass || int.class == targetClass)
      return true;
    if (Long.class == targetClass || long.class == targetClass)
      return true;
    if (Float.class == targetClass || float.class == targetClass)
      return true;
    if (Double.class == targetClass || double.class == targetClass)
      return true;
    if (BigInteger.class == targetClass)
      return true;
    if (BigDecimal.class == targetClass || Number.class == targetClass) {
      return true;
    }

    return false;
  }

  // all the following code comes from spring 5.3.16
  /**
   * Parse the given {@code text} into a {@link Number} instance of the given target class, using the
   * corresponding {@code decode} / {@code valueOf} method.
   * <p>
   * Trims all whitespace (leading, trailing, and in between characters) from the input {@code String}
   * before attempting to parse the number.
   * <p>
   * Supports numbers in hex format (with leading "0x", "0X", or "#") as well.
   * 
   * @param text        the text to convert
   * @param targetClass the target class to parse into
   * @return the parsed number
   * @throws IllegalArgumentException if the target class is not supported (i.e. not a standard Number
   *                                  subclass as included in the JDK)
   * @see Byte#decode
   * @see Short#decode
   * @see Integer#decode
   * @see Long#decode
   * @see #decodeBigInteger(String)
   * @see Float#valueOf
   * @see Double#valueOf
   * @see java.math.BigDecimal#BigDecimal(String)
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
    Asserts.notNull(text, "Text can't be null");
    Asserts.notNull(targetClass, "Target class can't be null");
    // String trimmed = StringUtils.trimAllWhitespace(text);

    if (Byte.class == targetClass) {
      return (T)(isHexNumber(text) ? Byte.decode(text) : Byte.valueOf(text));
    } else if (Short.class == targetClass) {
      return (T)(isHexNumber(text) ? Short.decode(text) : Short.valueOf(text));
    } else if (Integer.class == targetClass) {
      return (T)(isHexNumber(text) ? Integer.decode(text) : Integer.valueOf(text));
    } else if (Long.class == targetClass) {
      return (T)(isHexNumber(text) ? Long.decode(text) : Long.valueOf(text));
    } else if (BigInteger.class == targetClass) {
      return (T)(isHexNumber(text) ? decodeBigInteger(text) : new BigInteger(text));
    } else if (Float.class == targetClass) {
      return (T)Float.valueOf(text);
    } else if (Double.class == targetClass) {
      return (T)Double.valueOf(text);
    } else if (BigDecimal.class == targetClass || Number.class == targetClass) {
      return (T)new BigDecimal(text);
    } else {
      throw new IllegalArgumentException(
          "Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
    }
  }

  /**
   * Determine whether the given {@code value} String indicates a hex number, i.e. needs to be passed
   * into {@code Integer.decode} instead of {@code Integer.valueOf}, etc.
   */
  private static boolean isHexNumber(String value) {
    int index = (value.startsWith("-") ? 1 : 0);
    return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
  }

  /**
   * Decode a {@link java.math.BigInteger} from the supplied {@link String} value.
   * <p>
   * Supports decimal, hex, and octal notation.
   * 
   * @see BigInteger#BigInteger(String, int)
   */
  private static BigInteger decodeBigInteger(String value) {
    int radix = 10;
    int index = 0;
    boolean negative = false;

    // Handle minus sign, if present.
    if (value.startsWith("-")) {
      negative = true;
      index++;
    }

    // Handle radix specifier, if present.
    if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      index += 2;
      radix = 16;
    } else if (value.startsWith("#", index)) {
      index++;
      radix = 16;
    } else if (value.startsWith("0", index) && value.length() > 1 + index) {
      index++;
      radix = 8;
    }

    BigInteger result = new BigInteger(value.substring(index), radix);
    return (negative ? result.negate() : result);
  }
}