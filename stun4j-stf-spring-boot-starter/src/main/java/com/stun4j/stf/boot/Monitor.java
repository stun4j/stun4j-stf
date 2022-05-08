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
package com.stun4j.stf.boot;

/**
 * Stf monitor configuration
 * <p>
 * @author Jay Meng
 */
public class Monitor {
  /**
   * Is vm resource check enabled(vm-resource refers to cpu-rate, memory consumption, system-load, and so on)
   * <p>
   * Default: true
   */
  // The 'is-XXX' is not recommended, in part because javadoc is not displayed correctly in YML
  private boolean vmResCheckEnabled = true;

  /**
   * Whether system-load is taken into consider
   * <p>
   * Default: false
   */
  private boolean considerSysLoad = false;

  /**
   * Whether jvm-mem is taken into consider
   * <p>
   * Default: false
   */
  private boolean considerJvmMem = false;

  private JvmMem jvmMem = new JvmMem();
  private JvmCpu jvmCpu = new JvmCpu();
  private SysLoad sysLoad = new SysLoad();

  public boolean isVmResCheckEnabled() {
    return vmResCheckEnabled;
  }

  public void setVmResCheckEnabled(boolean vmResCheckEnabled) {
    this.vmResCheckEnabled = vmResCheckEnabled;
  }

  public boolean isConsiderSysLoad() {
    return considerSysLoad;
  }

  public void setConsiderSysLoad(boolean considerSysLoad) {
    this.considerSysLoad = considerSysLoad;
  }

  public boolean isConsiderJvmMem() {
    return considerJvmMem;
  }

  public void setConsiderJvmMem(boolean considerJvmMem) {
    this.considerJvmMem = considerJvmMem;
  }

  public JvmMem getJvmMem() {
    return jvmMem;
  }

  public void setJvmMem(JvmMem jvmMem) {
    this.jvmMem = jvmMem;
  }

  public JvmCpu getJvmCpu() {
    return jvmCpu;
  }

  public void setJvmCpu(JvmCpu jvmCpu) {
    this.jvmCpu = jvmCpu;
  }

  public SysLoad getSysLoad() {
    return sysLoad;
  }

  public void setSysLoad(SysLoad systemLoad) {
    this.sysLoad = systemLoad;
  }

  static class JvmMem {
    /**
     * Default: 0.85f
     */
    private float highFactor = 0.85f;
    /**
     * Whether non-heap memory is taken into consider
     * <p>
     * Default: false
     */
    private boolean includeNonHeap;

    /**
     * Default: true
     */
    private boolean checkEnabled = true;

    public float getHighFactor() {
      return highFactor;
    }

    public void setHighFactor(float highFactor) {
      this.highFactor = highFactor;
    }

    public boolean isIncludeNonHeap() {
      return includeNonHeap;
    }

    public void setIncludeNonHeap(boolean includeNonHeap) {
      this.includeNonHeap = includeNonHeap;
    }

    public boolean isCheckEnabled() {
      return checkEnabled;
    }

    public void setCheckEnabled(boolean checkEnabled) {
      this.checkEnabled = checkEnabled;
    }

  }

  static class JvmCpu {
    /**
     * Default: 0.65f
     */
    private float highFactor = 0.65f;

    public float getHighFactor() {
      return highFactor;
    }

    public void setHighFactor(float highFactor) {
      this.highFactor = highFactor;
    }

  }

  static class SysLoad {
    /**
     * Default: 0.8f
     */
    private float highFactor = 0.8f;

    public float getHighFactor() {
      return highFactor;
    }

    public void setHighFactor(float highFactor) {
      this.highFactor = highFactor;
    }

  }

}
