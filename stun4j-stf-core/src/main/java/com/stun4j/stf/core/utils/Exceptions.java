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
 * @author Jay Meng
 *         <p>
 *         <ul>
 *         <li>Made some simplifications</li>
 *         <li>Introduce silent handle strategy</li>
 *         <li>Introduce log mechanism</li>
 *         </ul>
 */
public final class Exceptions {
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
    swallow(t);
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  public static RuntimeException sneakyThrow(Throwable t, Logger logger, String msgOrFmt, Object... msgArgs) {
    swallow(t, logger, msgOrFmt, msgArgs);
    Exceptions.<RuntimeException> sneakyThrow0(t);
    return null;
  }

  public static void swallow(Throwable t) {
    swallow(t, null, null);
  }

  public static void swallow(Throwable t, Logger logger, String msgOrFmt, Object... msgArgs) {
    if (t == null) {
      return;
    }
    if (t instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    if (logger == null || msgOrFmt == null) {// TODO mj:sysout,printstack?
      return;
    }
    if (!ArrayUtils.isEmpty(msgArgs)) {
      if (msgArgs[msgArgs.length - 1] instanceof Throwable) {
        logger.error(msgOrFmt, msgArgs);
      } else {
        logger.error(msgOrFmt, ArrayUtils.add(msgArgs, t));
      }
    } else {
      logger.error(msgOrFmt, t);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
    throw (T)t;
  }

  private Exceptions() {
  }
}