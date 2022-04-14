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

import java.util.HashMap;
import java.util.Map;

/**
 * Stf global job configuration
 * <p>
 * @author Jay Meng
 */
public class Job {
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = new HashMap<>();
  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(1, 0);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(2, 1 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(3, 2 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(4, 5 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(5, 15 * 60);
  }
  /**
   * Default:
   * <ul>
   * <li>1: 0</li>
   * <li>2: 60</li>
   * <li>3: 120</li>
   * <li>4: 300</li>
   * <li>5: 900</li>
   * </ul>
   */
  private Map<Integer, Integer> retryIntervalSeconds = DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;

  public Map<Integer, Integer> getRetryIntervalSeconds() {
    return retryIntervalSeconds;
  }

  public void setRetryIntervalSeconds(Map<Integer, Integer> retryIntervalSeconds) {
    this.retryIntervalSeconds = retryIntervalSeconds;
  }

}