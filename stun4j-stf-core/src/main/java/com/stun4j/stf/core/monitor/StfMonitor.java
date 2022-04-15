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

import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.stun4j.stf.core.support.BaseLifeCycle;

/**
 * @author Jay Meng
 */
public class StfMonitor extends BaseLifeCycle {
  public static final StfMonitor INSTANCE;
  private final JvmCpu jvmCpu;
  private final SystemLoad systemLoad;
  private boolean considerSystemLoad;

  @Override
  public void doStart() {
    jvmCpu.doStart();
    if (StfMonitor.INSTANCE.considerSystemLoad) {
      systemLoad.doStart();
    }

    LOG.info("The stf-monitor is successfully started");
  }

  @Override
  public void doShutdown() {
    jvmCpu.shutdown();
    if (StfMonitor.INSTANCE.considerSystemLoad) {
      systemLoad.shutdown();
    }
    LOG.info("The stf-monitor is successfully shut down");
  }

  public Pair<Boolean, Map<String, Object>> isVmResourceNotEnough() {
    Triple<Boolean/* judgment result */, MemoryUsage/* heap */, MemoryUsage/* nonHeap */> mem;
    Pair<Boolean/* judgment result */, Double/* rate */> cpuRate;
    Pair<Boolean/* judgment result */, Double/* load */> sysLoad = null;
    boolean isMemScarce = (mem = JvmMemory.INSTANCE.isScarce(true)).getLeft();
    boolean isCpuRateHigh = (cpuRate = JvmCpu.INSTANCE.isHigh()).getLeft();
    boolean isSysLoadHigh = considerSystemLoad && (sysLoad = systemLoad.isHigh()).getLeft();
    boolean isNotEnough = isMemScarce || isCpuRateHigh || isSysLoadHigh;
    Map<String, Object> rpt = null;
    if (isNotEnough) {
      rpt = new HashMap<>();
      rpt.put("mem", mem);
      rpt.put("cpuRate", cpuRate);
      rpt.put("sysLoad", sysLoad);
    }
    return Pair.of(isNotEnough, rpt);
  }

  static {
    INSTANCE = new StfMonitor();
  }

  private StfMonitor() {
    jvmCpu = JvmCpu.INSTANCE;
    systemLoad = SystemLoad.INSTANCE;
  }

  public void setConsiderSystemLoad(boolean considerSystemLoad) {
    this.considerSystemLoad = considerSystemLoad;
  }

}
