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
package com.stun4j.stf.core.support.executor;

import static com.stun4j.stf.core.utils.executor.PoolExecutors.defaultIoPrefer;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.defaultWorkStealingPool;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newDynamicIoPrefer;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newScheduler;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newSingleThreadScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.stun4j.stf.core.utils.executor.NamedThreadFactory;

/** @author Jay Meng */
public final class StfInternalExecutors {
  public static ThreadPoolExecutor newWorkerOfStfCore() {
    return (ThreadPoolExecutor)defaultIoPrefer("stf-core-worker");
  }

  public static ThreadPoolExecutor newWorkerOfJobRunner(String jobGrp) {
    return (ThreadPoolExecutor)defaultIoPrefer("stf-job-" + jobGrp + "-runner");
  }

  @Deprecated
  public static ScheduledExecutorService newWatcherOfRunningJobTimeoutFixer() {// TODO mj:prove 2 works?
    return newScheduler(2, "stf-job-running-timeout-fix-watcher", true);
  }

  public static ScheduledExecutorService newWatcherOfJobLoading() {
    return newSingleThreadScheduler("stf-job-load-watcher", true);
  }

  public static ExecutorService newWorkerOfJobLoading() {
    return defaultWorkStealingPool("stf-job-load-worker", true);
  }

  public static ScheduledExecutorService newWatcherOfJobManager() {
    return newSingleThreadScheduler("stf-job-mngr-watcher", true);
  }

  public static ThreadPoolExecutor newWorkerOfJobManager(String jobGrp) {
    return (ThreadPoolExecutor)defaultIoPrefer("stf-job-" + jobGrp + "-mngr-worker");
  }

  public static StfExecutorService newDefaultExec(int threadKeepAliveTimeSeconds, int taskQueueSize,
      RejectedExecutionHandler rejectPolicy, boolean allowCoreThreadTimeOut) {
    return new StfExecutorService(
        newDynamicIoPrefer(new LinkedBlockingQueue<>(taskQueueSize),
        NamedThreadFactory.of("stf-dft-exec"), threadKeepAliveTimeSeconds, allowCoreThreadTimeOut, rejectPolicy));
  }

  private StfInternalExecutors() {
  }
}