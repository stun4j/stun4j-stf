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

import static com.stun4j.stf.core.StfHelper.newHashMap;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfJobRunner;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.support.BaseLifecycle;
import com.stun4j.stf.core.utils.Exceptions;

/** @author Jay Meng */
public class JobRunners extends BaseLifecycle {
  private final StfCore stfCore;
  private final Map<StfMetaGroup, ThreadPoolExecutor> workers;
  private final JobRunner runner;

  @Override
  public void doShutdown() {
    workers.forEach((grp, worker) -> {
      try {
        worker.shutdown();
        worker.awaitTermination(30, TimeUnit.SECONDS);
        LOG.debug("Worker is successfully shut down [grp={}]", grp);
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while shutting down worker [grp={}]", grp);
      }
    });
  }

  public void execute(StfMetaGroup metaGrp, Stf job) {
    workers.get(metaGrp).execute(() -> {
      Long jobId = job.getId();
      try {
        JobRunner.doHandleTimeoutJob(metaGrp, job, stfCore);
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while handing stf-job#{} [metaGrp={}]", jobId, metaGrp);
      }
    });
  }

  public int getAvailablePoolSize(StfMetaGroup metaGrp) {
    return workers.get(metaGrp).getMaximumPoolSize() - workers.get(metaGrp).getActiveCount();
  }

  public JobRunners(StfCore stfCore) {
    this(stfCore, null);
  }

  public JobRunners(StfCore stfCore, Map<Integer, Integer> retryBehavior) {
    this.stfCore = stfCore;
    this.runner = JobRunner.init(retryBehavior);
    this.workers = newHashMap(StfMetaGroup.stream(), (map, metaGrp) -> {
      map.put(metaGrp, newWorkerOfJobRunner(metaGrp));
      return map;
    });
  }

  public StfCore getStfCore() {
    return stfCore;
  }

  JobRunner getRunner() {
    return runner;
  }

}