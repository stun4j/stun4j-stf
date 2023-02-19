/*
 * Copyright 2015-2022 the original author or authors.
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

import static com.stun4j.stf.core.utils.Asserts.argument;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.lang3.ArrayUtils;

/** @author Jay Meng */
public final class Utils {

  private static final int NANOS_PER_MS = 1000 * 1000;
  private static final int NANOS_PER_SECONDS = 1000 * NANOS_PER_MS;

  public static void sleepMs(long ms) {
    LockSupport.parkNanos(ms * NANOS_PER_MS);
  }

  public static void sleepSeconds(int seconds) {
    LockSupport.parkNanos((long)seconds * NANOS_PER_SECONDS);
  }

  public static Class<?> pickGenericSuperTypeOf(Class<?> childClass, int typeIndex) {
    Type genericSuperType;
    if (!((genericSuperType = childClass.getGenericSuperclass()) instanceof ParameterizedType)) {
      return Object.class;
    }
    Type[] params = ((ParameterizedType)genericSuperType).getActualTypeArguments();
    argument(typeIndex >= 0 && typeIndex < params.length, "The generic type index is out of range");
    if (!(params[typeIndex] instanceof Class)) {
      return Object.class;
    }
    return (Class<?>)params[typeIndex];
  }

  public static Class<?> pickGenericSuperTypeOf(Class<?> childClass) {
    return pickGenericSuperTypeOf(childClass, 0);
  }

  public static int calculateNearestPowerOfTwo(int num) {
    if (num <= 0) {
      return 1;
    }
    int hb;
    return num <= (hb = Integer.highestOneBit(num)) ? hb : hb << 1;
  }

  public static int[] fibs(int first, int second, int estimatedMax) {
    Builder<Integer> builder = Stream.builder();
    builder.add(first);
    builder.add(second);
    fib(first, second, estimatedMax, builder);
    int[] fibs = ArrayUtils.toPrimitive(builder.build().toArray(Integer[]::new));
    return fibs;
  }

  public static int fib(int x, int y, int estimatedMax, Builder<Integer> builder) {
    int sum = x + y;
    builder.add(sum);
    if (sum >= estimatedMax) {
      return sum;
    }
    return fib(y, sum, estimatedMax, builder);
  }

  public static String getOSSignalType() {
    return System.getProperties().getProperty("os.name").toLowerCase().startsWith("win") ? "INT" : "TERM";
  }

  private Utils() {
  }
}
