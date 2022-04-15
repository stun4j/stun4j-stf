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

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

/**
 * Inspired by lombok
 */
public class Exceptions {
  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it onwards. The exception
   * is still thrown - javac will just stop whining about it.
   * <p>
   * Example usage:
   * 
   * <pre>
   * public void run() {
   *   throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }
   * </pre>
   * <p>
   * NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does not know or care about
   * the concept of a 'checked exception'. All this method does is hide the act of throwing a checked exception from the
   * java compiler.
   * <p>
   * Note that this method has a return type of {@code RuntimeException}; it is advised you always call this method as
   * argument to the {@code throw} statement to avoid compiler errors regarding no return statement and similar
   * problems. This method won't of course return an actual {@code RuntimeException} - it never returns, it always
   * throws the provided exception.
   * @param t The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws an exception!
   */
  public static RuntimeException sneakyThrow(Throwable t) {
    if (t == null) throw new NullPointerException("t");
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
    throw (T)t;
  }

  public static RuntimeException sneakyThrow(Throwable t, Logger log, String msg) {
    if (t == null) throw new NullPointerException("t");
    log.error(msg, t);
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  public static RuntimeException sneakyThrow(Throwable t, Logger log, String format, Object arg1) {
    if (t == null) throw new NullPointerException("t");
    log.error(format, arg1, t);
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  public static RuntimeException sneakyThrow(Throwable t, Logger log, String format, Object arg1, Object arg2) {
    if (t == null) throw new NullPointerException("t");
    log.error(format, arg1, arg2, t);
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  public static RuntimeException sneakyThrow(Throwable t, Logger log, String format, Object... args) {
    if (t == null) throw new NullPointerException("t");
    if (!ArrayUtils.isEmpty(args)) {
      if (args[args.length - 1] instanceof Throwable) {
        log.error(format, args);
      } else {
        log.error(format, ArrayUtils.add(args, t));
      }
    } else {
      log.error(format, t);
    }
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }
}