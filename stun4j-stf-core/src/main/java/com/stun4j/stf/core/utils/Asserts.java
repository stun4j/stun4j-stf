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

import static com.google.common.base.Strings.lenientFormat;

import java.util.function.Supplier;

import org.slf4j.Logger;

/** @author Jay Meng */
public abstract class Asserts {
  public static <T> void notNull(T obj) {
    if (obj == null)
      throw new IllegalArgumentException();
  }

  public static <T> void notNull(T obj, String errorMsg) {
    if (obj == null)
      throw new IllegalArgumentException(errorMsg);
  }

  public static <T> void notNull(T obj, Supplier<String> errorMsg) {
    if (obj == null)
      throw new IllegalArgumentException(errorMsg.get());
  }

  public static <T> void notNull(T obj, String errorMsgTemplate, Object... errorMsgArgs) {
    if (obj == null) {
      throw new IllegalArgumentException(lenientFormat(errorMsgTemplate, errorMsgArgs));
    }
  }

  public static <T> T requireNonNull(T obj) {
    notNull(obj);
    return obj;
  }

  public static <T> T requireNonNull(T obj, String errorMsg) {
    notNull(obj, errorMsg);
    return obj;
  }

  public static <T> T requireNonNull(T obj, Supplier<String> errorMsg) {
    notNull(obj, errorMsg);
    return obj;
  }

  public static void argument(boolean expression, String errorMsg) {
    if (!expression) {
      throw new IllegalArgumentException(errorMsg);
    }
  }

  public static void argument(boolean expression, Supplier<String> errorMsg) {
    if (!expression) {
      throw new IllegalArgumentException(errorMsg.get());
    }
  }

  public static void argument(boolean expression, String errorMsgTemplate, Object... errorMsgArgs) {
    if (!expression) {
      throw new IllegalArgumentException(lenientFormat(errorMsgTemplate, errorMsgArgs));
    }
  }

  public static void state(boolean expression, String errorMsg) {
    if (!expression) {
      throw new IllegalStateException(errorMsg);
    }
  }

  public static void state(boolean expression, String errorMsgTemplate, Object... errorMsgArgs) {
    if (!expression) {
      throw new IllegalStateException(lenientFormat(errorMsgTemplate, errorMsgArgs));
    }
  }

  public static void state(boolean expression, Supplier<String> errorMsg) {
    if (!expression) {
      throw new IllegalStateException(errorMsg.get());
    }
  }

  public static void state(boolean expression, Logger logger, String errorMsg) {
    if (!expression) {
      logger.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }
  }

  public static void state(boolean expression, Logger logger, String errorMsgTemplate, Object... errorMsgArgs) {
    if (!expression) {
      String msg;
      logger.error(msg = lenientFormat(errorMsgTemplate, errorMsgArgs));
      throw new IllegalStateException(msg);
    }
  }

  public static void state(boolean expression, Logger logger, Supplier<String> errorMsg) {
    if (!expression) {
      String msg;
      logger.error(msg = errorMsg.get());
      throw new IllegalStateException(msg);
    }
  }

  public static void raiseIllegalStateException(String errorMsg) {
    state(false, errorMsg);
  }

  public static void raiseIllegalStateException(String errorMsgTemplate, Object... errorMsgArgs) {
    state(false, errorMsgTemplate, errorMsgArgs);
  }

  public static void raiseIllegalStateException(Logger logger, String errorMsgTemplate, Object... errorMsgArgs) {
    state(false, logger, errorMsgTemplate, errorMsgArgs);
  }
}
