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
package com.stun4j.stf.core.monitor;

import static com.stun4j.guid.core.utils.Asserts.argument;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.apache.commons.lang3.tuple.Triple;

/**
 * @author Jay Meng
 */
public enum JvmMemory {
  INSTANCE;

  private float highFactor = 0.85f;
  private boolean includeNonHeap;
  private final MemoryMXBean MEM_BASIC;

  public MemoryUsage getHeapUsage() {
    return MEM_BASIC.getHeapMemoryUsage();
  }

  public MemoryUsage getNonHeapUsage() {
    return MEM_BASIC.getNonHeapMemoryUsage();
  }

  public Triple<Boolean/* judgment result */, MemoryUsage/* heap */, MemoryUsage/* nonHeap */> isScarce() {
    MemoryUsage heapUsage = getHeapUsage();
    MemoryUsage nonHeapUsage = null;
    // isMaxAvailable、max
    long max;
    boolean isMaxAvailable = (max = heapUsage.getMax()) > 0;
    if (includeNonHeap) {
      nonHeapUsage = getNonHeapUsage();
      isMaxAvailable &= (max = nonHeapUsage.getMax()) > 0;
    }
    // used
    long used = heapUsage.getUsed();
    if (includeNonHeap) {
      used += nonHeapUsage.getUsed();
    }
    // commited
    long commited = heapUsage.getCommitted();
    if (includeNonHeap) {
      commited += nonHeapUsage.getCommitted();
    }
    // critical level,almost to the point of error
    /*-
     * A memory allocation may fail if it attempts to increase the used memory
     * such that used > committed even if used <= max would still be true
     * (for example, when the system is low on virtual memory).
     */
    if (used >= commited) {
      if (isMaxAvailable && used >= max) {
        return Triple.of(true, heapUsage, nonHeapUsage);
      }
      return Triple.of(true, heapUsage, nonHeapUsage);
    }

    // extreme high level
    if (isMaxAvailable) {
      if (isAtHighWaterMark(commited, max) || isAtHighWaterMark(used, max)) {
        return Triple.of(true, heapUsage, nonHeapUsage);
      }
    }

    // high level
    if (isAtHighWaterMark(used, commited)) {
      return Triple.of(true, heapUsage, nonHeapUsage);
    }
    return Triple.of(false, heapUsage, nonHeapUsage);
  }

  private boolean isAtHighWaterMark(long low, long high) {
    return low / (high * 1.0) >= highFactor;
  }

  public JvmMemory withHighFactor(float highFactor) {
    argument(highFactor > 0 && highFactor < 1, "The 'highFactor' must be greater than 0 and less than 1");
    this.highFactor = highFactor;
    return this;
  }

  public JvmMemory withIncludeNonHeap(boolean includeNonHeap) {
    this.includeNonHeap = includeNonHeap;
    return this;
  }

  {
    MEM_BASIC = ManagementFactory.getMemoryMXBean();
  }

}
