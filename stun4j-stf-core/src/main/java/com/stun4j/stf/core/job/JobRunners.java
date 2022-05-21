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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Functions;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.BaseLifeCycle;
import com.stun4j.stf.core.support.NullValue;
import com.stun4j.stf.core.utils.Exceptions;

/** @author Jay Meng */
public class JobRunners extends BaseLifeCycle {
  private final StfCore stfCore;
  private final Map<String, ThreadPoolExecutor> workers;
  private final Map<String, Runnings> runnings;
  private final JobRunner runner;

  private class Runnings {
    private final ConcurrentMap<Long, Object> jobIds = new ConcurrentHashMap<>();

    public void register(Long jobId) {
      jobIds.putIfAbsent(jobId, NullValue.INSTANCE);
    }

    public void deregister(Long jobId) {
      jobIds.remove(jobId);
    }
  }

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
        JobRunners.this.runnings.get(jobGrp).register(jobId);
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
      } finally {
        JobRunners.this.runnings.get(jobGrp).deregister(jobId);
      }
    });
    // TODO mj:error handle
  }

  // prevent pool-running jobs to be picked
  Stream<Stf> getNotRunning(List<Stf> jobs) {
    Map<Long, ?> allRunningJobIds = runnings.values().stream().flatMap(runnings -> {
      return runnings.jobIds.keySet().stream();
    }).collect(Collectors.toMap(Functions.identity(), v -> NullValue.INSTANCE));
    return jobs.stream().filter(job -> job != null && !allRunningJobIds.containsKey(job.getId()));
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

    this.runnings = new HashMap<>();
    this.workers = new HashMap<>();
    Stream.of(ALL_JOB_GROUPS).forEach((jobGrp) -> {
      this.workers.put(jobGrp, newWorkerOfJobRunner(jobGrp));
      this.runnings.put(jobGrp, new Runnings());
    });
  }

  public StfCore getStfCore() {
    return stfCore;
  }

  JobRunner getRunner() {
    return runner;
  }

}