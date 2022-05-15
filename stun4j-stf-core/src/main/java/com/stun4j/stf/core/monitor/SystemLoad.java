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

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.guid.core.utils.Utils;
import com.stun4j.stf.core.support.BaseLifeCycle;
import com.sun.management.OperatingSystemMXBean;

/**
 * @author Jay Meng
 */
@SuppressWarnings("restriction")
public final class SystemLoad extends BaseLifeCycle {
  private static final int TIME_WINDOW_SECONDS = 60;
  public static final SystemLoad INSTANCE;

  private float highFactor = 0.8f;
  private final OperatingSystemMXBean os;
  private final int processorCnt;

  private double lastWindowSecondsTotal;
  private double lastWindowSecondsAvg;

  public double getLastWindowSecondsAvg() {
    return lastWindowSecondsAvg;
  }

  private int elapsedSeconds;
  private Thread thread;

  @Override
  public void doStart() {
    thread.start();
    LOG.debug("The stf-system-load monitor is successfully started");
  }

  @Override
  public void doShutdown() {
    thread.interrupt();
    LOG.debug("The stf-system-load monitor is successfully shut down");
  }

  public Pair<Boolean/* judgment result */, Double/* load */> isHigh() {
    boolean res = lastWindowSecondsAvg / processorCnt >= highFactor;
    return Pair.of(res, lastWindowSecondsAvg);
  }

  static {
    INSTANCE = new SystemLoad();
  }

  private SystemLoad() {
    this.os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    this.processorCnt = os.getAvailableProcessors();

    thread = new Thread(() -> {
      do {
        Utils.sleepSeconds(1);

        double load = os.getSystemLoadAverage();
        if (load < 0) continue;
        lastWindowSecondsTotal += load;
        if (++elapsedSeconds == TIME_WINDOW_SECONDS) {
          lastWindowSecondsAvg = lastWindowSecondsTotal / TIME_WINDOW_SECONDS;
          if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting system-load last{}SecondsAvg: {}", TIME_WINDOW_SECONDS, lastWindowSecondsAvg);
          }
          // reset
          lastWindowSecondsTotal = 0;
          elapsedSeconds = 0;
        }
      } while (!Thread.currentThread().isInterrupted());
    });
    thread.setDaemon(true);
  }

  public SystemLoad withHighFactor(float highFactor) {
    argument(highFactor > 0 && highFactor < 1, "The 'highFactor' must be greater than 0 and less than 1");
    this.highFactor = highFactor;
    return this;
  }
}
