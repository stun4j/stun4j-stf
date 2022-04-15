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
package com.stun4j.stf.core.job;

import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_RUNNING;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;

import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;

/**
 * @author Jay Meng
 */
public class JobLoader extends BaseJobLoader {
  private static final int DFT_MIN_JOB_WAITING_RUN_TIMEOUT_SECONDS = 10;
  private static final int DFT_JOB_WAITING_RUN_TIMEOUT_SECONDS = 30;

  private static final int DFT_MIN_JOB_RUNNING_TIMEOUT_SECONDS = 10;
  private static final int DFT_JOB_RUNNING_TIMEOUT_SECONDS = 30;

  private long jobWaitingRunTimeoutMs = DFT_JOB_WAITING_RUN_TIMEOUT_SECONDS * 1000;
  private long jobRunningTimeoutMs = DFT_JOB_RUNNING_TIMEOUT_SECONDS * 1000;
  private final JobScanner scanner;

  @Override
  protected Stream<Stf> loadJobs(String jobGrp, int loadSize) {
    switch (jobGrp) {
      case JOB_GROUP_TIMEOUT_WAITING_RUN:
        return scanner.scanTimeoutJobsWaitingRun(jobWaitingRunTimeoutMs, loadSize, false);
      case JOB_GROUP_TIMEOUT_RUNNING:
        return scanner.scanTimeoutJobsRunning(jobRunningTimeoutMs, loadSize, false);
      default:
        return Stream.empty();
    }
  }

  public JobLoader(JobScanner scanner) {
    this.scanner = scanner;
  }

  public void setJobWaitingRunTimeoutSeconds(int jobWaitingRunTimeoutSeconds) {
    this.jobWaitingRunTimeoutMs = jobWaitingRunTimeoutSeconds < DFT_MIN_JOB_WAITING_RUN_TIMEOUT_SECONDS
        ? DFT_MIN_JOB_WAITING_RUN_TIMEOUT_SECONDS * 1000
        : jobWaitingRunTimeoutSeconds * 1000;
  }

  public void setJobRunningTimeoutSeconds(int jobRunningTimeoutSeconds) {
    this.jobRunningTimeoutMs = jobRunningTimeoutSeconds < DFT_MIN_JOB_RUNNING_TIMEOUT_SECONDS
        ? DFT_MIN_JOB_RUNNING_TIMEOUT_SECONDS * 1000
        : jobRunningTimeoutSeconds * 1000;
  }

}