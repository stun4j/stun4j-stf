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

import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
import static com.stun4j.stf.core.StfMetaGroupEnum.DELAY;
import static com.stun4j.stf.core.job.JobConsts.ALL_JOB_GROUPS;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_IN_PROGRESS;
import static com.stun4j.stf.core.job.JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfJobRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.BaseLifecycle;
import com.stun4j.stf.core.utils.Exceptions;

/** @author Jay Meng */
public class JobRunners extends BaseLifecycle {
  private final StfCore stfCore;
  private final Map<String, ThreadPoolExecutor> workers;
  private final JobRunner runner;

  @Override
  public void doShutdown() {
    workers.forEach((grp, worker) -> {
      try {
        worker.shutdown();
        worker.awaitTermination(30, TimeUnit.SECONDS);
        LOG.debug("Worker is successfully shut down [grp={}]", grp);
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "Unexpected error occurred while shutting down worker [grp={}]", grp);
      }
    });
  }

  public void execute(String jobGrp, Stf job) {
    workers.get(jobGrp).execute(() -> {
      Long jobId = job.getId();
      try {
        switch (jobGrp) {
          case JOB_GROUP_TIMEOUT_WAITING_RUN:
          case JOB_GROUP_TIMEOUT_IN_PROGRESS:
            JobRunner.doHandleTimeoutJob(CORE, job, stfCore);
            break;

          case JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN:
          case JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS:
            JobRunner.doHandleTimeoutJob(DELAY, job, stfCore);
            break;

          default:
            break;
        }
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while handing stf-job#{} [grp={}]", jobId, jobGrp);
      }
    });
  }

  public int getAvailablePoolSize(String jobGrp) {
    return workers.get(jobGrp).getMaximumPoolSize() - workers.get(jobGrp).getActiveCount();
  }

  public JobRunners(StfCore stfCore) {
    this(stfCore, null);
  }

  public JobRunners(StfCore stfCore, Map<Integer, Integer> retryIntervalSeconds) {
    this.stfCore = stfCore;
    this.runner = JobRunner.init(retryIntervalSeconds);
    this.workers = Stream.of(ALL_JOB_GROUPS).reduce(new HashMap<>(), (map, grp) -> {
      map.put(grp, newWorkerOfJobRunner(grp));
      return map;
    }, (a, b) -> null);
  }

  public StfCore getStfCore() {
    return stfCore;
  }

  JobRunner getRunner() {
    return runner;
  }

}