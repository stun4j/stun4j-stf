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

import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.job.JobConsts.retryBehaviorByPattern;

import java.util.Map;

/**
 * Stf global job configuration
 * <p>
 * 
 * @author Jay Meng
 */
public class Job {
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;
  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = retryBehaviorByPattern(DFT_JOB_TIMEOUT_SECONDS);
  }

  /**
   * Currently configuring this has no effect!!!
   * <p>
   * Default:
   * <ul>
   * <li>1: 20</li>
   * <li>2: 20</li>
   * <li>3: 40</li>
   * <li>4: 60</li>
   * <li>5: 100</li>
   * </ul>
   */
  private Map<Integer, Integer> retryIntervalSeconds = DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;/*- TODO mj:config the generate pattern,instead of hardcode*/
  private JobLoader loader = new JobLoader();
  private JobManager manager = new JobManager();

  public Map<Integer, Integer> getRetryIntervalSeconds() {
    return retryIntervalSeconds;
  }

  public void setRetryIntervalSeconds(Map<Integer, Integer> retryIntervalSeconds) {
    this.retryIntervalSeconds = retryIntervalSeconds;
  }

  public JobLoader getLoader() {
    return loader;
  }

  public void setLoader(JobLoader loader) {
    this.loader = loader;
  }

  public JobManager getManager() {
    return manager;
  }

  public void setManager(JobManager manager) {
    this.manager = manager;
  }

  static class JobLoader {
    /**
     * Default: 300
     */
    private int loadSize = 300;
    /**
     * Default: 3
     */
    private int scanFreqSecs = 3;

    public int getLoadSize() {
      return loadSize;
    }

    public void setLoadSize(int loadSize) {
      this.loadSize = loadSize;
    }

    public int getScanFreqSecs() {
      return scanFreqSecs;
    }

    public void setScanFreqSecs(int scanFreqSecs) {
      this.scanFreqSecs = scanFreqSecs;
    }

  }

  static class JobManager {
    /**
     * Default: 0
     */
    private int handleBatchSize = 0;
    /**
     * Default: 3
     */
    private int scanFreqSecs = 3;

    public int getHandleBatchSize() {
      return handleBatchSize;
    }

    public void setHandleBatchSize(int handleBatchSize) {
      this.handleBatchSize = handleBatchSize;
    }

    public int getScanFreqSecs() {
      return scanFreqSecs;
    }

    public void setScanFreqSecs(int scanFreqSecs) {
      this.scanFreqSecs = scanFreqSecs;
    }

  }

}