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
  private boolean considerSystemLoad = false;

  public boolean isVmResCheckEnabled() {
    return vmResCheckEnabled;
  }

  public void setVmResCheckEnabled(boolean vmResCheckEnabled) {
    this.vmResCheckEnabled = vmResCheckEnabled;
  }

  public boolean isConsiderSystemLoad() {
    return considerSystemLoad;
  }

  public void setConsiderSystemLoad(boolean considerSystemLoad) {
    this.considerSystemLoad = considerSystemLoad;
  }

}
