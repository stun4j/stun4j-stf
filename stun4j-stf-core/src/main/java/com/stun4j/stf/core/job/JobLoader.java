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

import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_IN_PROGRESS;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS;

import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.cluster.StfClusterMembers;

/**
 * @author Jay Meng
 */
public class JobLoader extends BaseJobLoader {
  private final JobScanner scanner;

  @Override
  protected Stream<Stf> loadJobs(String jobGrp, int loadSize) {
    int pageNo = StfClusterMembers.determineBlockToTakeOver();
    switch (jobGrp) {
      case JOB_GROUP_TIMEOUT_WAITING_RUN:
        return scanner.scanTimeoutCoreJobsWaitingRun(loadSize, pageNo);
      case JOB_GROUP_TIMEOUT_IN_PROGRESS:
        return scanner.scanTimeoutCoreJobsInProgress(loadSize, pageNo);
      case JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN:
        return scanner.scanTimeoutDelayJobsWaitingRun(loadSize, pageNo);
      case JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS:
        return scanner.scanTimeoutDelayJobsInProgress(loadSize, pageNo);
      default:
        return Stream.empty();
    }
  }

  public JobLoader(JobScanner scanner) {
    this.scanner = scanner;
  }

}