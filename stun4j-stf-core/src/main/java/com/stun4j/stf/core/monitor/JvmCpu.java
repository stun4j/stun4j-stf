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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.guid.core.utils.Utils;
import com.stun4j.stf.core.support.BaseLifeCycle;
import com.stun4j.stf.core.utils.executor.NamedThreadFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * @author Jay Meng
 */
@SuppressWarnings("restriction")
public final class JvmCpu extends BaseLifeCycle {
  private static final int TIME_WINDOW_SECONDS = 30;

  public static final JvmCpu INSTANCE;

  private float highFactor = 0.65f;

  private final RuntimeMXBean runtime;
  private final OperatingSystemMXBean os;
  private final List<GarbageCollectorMXBean> gcms;
  private final int processorCnt;
  private final float highFactorHalfProcessors;

  private long lastUpTime;
  private long lastProcessCpuTime;
  private long lastProcessGcTime;

  private double lastWindowSecondsTotal;
  private double lastWindowSecondsAvgRate;

  private int elapsedSeconds;
  private final Thread worker;
  private volatile boolean shutdown;

  @Override
  public void doStart() {
    worker.start();
    LOG.debug("The stf-jvm-cpu monitor is successfully started");
  }

  @Override
  public void doShutdown() {
    shutdown = true;
    worker.interrupt();
    LOG.debug("The stf-jvm-cpu monitor is successfully shut down");
  }

  public Pair<Boolean/* judgment result */, Double/* rate */> isHigh() {
    boolean res = lastWindowSecondsAvgRate >= highFactorHalfProcessors;
    return Pair.of(res, lastWindowSecondsAvgRate);
  }

  private Pair<Float/* cpu */, Float/* gc */> calculateCpuRealtimeRate() {
    long processCpuTime = os.getProcessCpuTime();
    long upTime = runtime.getUptime();

    long upTimeDelta = upTime - lastUpTime;
    if (upTimeDelta <= 0) {
      return Pair.of(0f, 0f);
    }
    long cpuTimeDelta = processCpuTime - lastProcessCpuTime;
    // double cpuRate = cpuTimeDelta * 100.0 / 1000000 / processorCnt / upTimeDelta;//another way saying the
    // cpu-rate(multi-core)
    float cpuRate = cpuTimeDelta / (upTimeDelta * 10000f);

    long processGcTime = getTotalGCTime(gcms);
    long processGcTimeDelta = processGcTime - lastProcessGcTime;
    // double gcRate = processGcTimeDelta * 100.0 / 1000000 / processorCnt / upTimeDelta;
    float gcRate = processGcTimeDelta / (upTimeDelta * 10000f);

    this.lastProcessCpuTime = processCpuTime;
    this.lastUpTime = upTime;
    this.lastProcessGcTime = processGcTime;
    return Pair.of(cpuRate, gcRate);
  }

  protected static long getTotalGCTime(List<GarbageCollectorMXBean> gcms) {
    long total = -1L;
    for (GarbageCollectorMXBean gcm : gcms) {
      total += gcm.getCollectionTime();
    }
    return total;
  }

  static {
    INSTANCE = new JvmCpu();
  }

  private JvmCpu() {
    this.runtime = ManagementFactory.getRuntimeMXBean();
    this.os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    this.gcms = ManagementFactory.getGarbageCollectorMXBeans();
    this.processorCnt = os.getAvailableProcessors();
    this.highFactorHalfProcessors = (processorCnt / 2) * 100 * highFactor;
    this.worker = new NamedThreadFactory("stf-jvm-cpu mon worker", true).newThread(() -> {
      do {
        this.lastUpTime = runtime.getUptime();
        this.lastProcessCpuTime = os.getProcessCpuTime();
        this.lastProcessGcTime = getTotalGCTime(gcms);
        Utils.sleepSeconds(1);

        Pair<Float, Float> info = calculateCpuRealtimeRate();
        lastWindowSecondsTotal += info.getLeft();
        if (++elapsedSeconds == TIME_WINDOW_SECONDS) {
          lastWindowSecondsAvgRate = lastWindowSecondsTotal / TIME_WINDOW_SECONDS;
          if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting jvm-cpu last{}SecondsAvgRate: {}", TIME_WINDOW_SECONDS, lastWindowSecondsAvgRate);
          }
          // reset
          lastWindowSecondsTotal = 0;
          elapsedSeconds = 0;
        }
      } while (!Thread.currentThread().isInterrupted() && !shutdown);
    });
  }

  public double getLastWindowSecondsAvgRate() {
    return lastWindowSecondsAvgRate;
  }

  public JvmCpu withHighFactor(float highFactor) {
    argument(highFactor > 0 && highFactor < 1, "The 'highFactor' must be greater than 0 and less than 1");
    this.highFactor = highFactor;
    return this;
  }
}
