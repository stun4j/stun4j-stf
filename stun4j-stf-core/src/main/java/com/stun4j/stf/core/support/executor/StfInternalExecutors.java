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

import static com.stun4j.stf.core.utils.executor.PoolExecutors.SILENT_DROP_POLICY;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.defaultCpuPrefer;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.defaultIoPrefer;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.defaultWorkStealingPool;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newIoPrefer;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newSingleThreadPool;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.newSingleThreadScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.utils.executor.NamedThreadFactory;

/** @author Jay Meng */
public final class StfInternalExecutors {
  public static ThreadPoolExecutor newWorkerOfJobRunner(StfMetaGroup metaGrp) {
    return (ThreadPoolExecutor)defaultCpuPrefer("stf-grp-" + metaGrp.nameLowerCase() + "-job-runner");
  }

  public static ThreadPoolExecutor newWorkerOfStfCore() {
    return (ThreadPoolExecutor)defaultIoPrefer("stf-core-worker");
  }

  public static ScheduledExecutorService newWatcherOfJobLoading() {
    return newSingleThreadScheduler("stf-job-load-watcher", true);
  }

  public static ScheduledExecutorService newWatcherOfMemberHeartbeat() {
    return newSingleThreadScheduler("stf-member-hb-watcher", true);
  }

  public static ExecutorService newWorkerOfMemberHeartbeat() {
    return newSingleThreadPool(new LinkedBlockingQueue<>(1), new NamedThreadFactory("stf-member-hb-worker", true), 0,
        false, SILENT_DROP_POLICY);
  }

  public static ExecutorService newWorkerOfJobLoading(StfMetaGroup metaGrp) {
    // TODO mj:recheck this policy,for the purpose fulfill&little-bit-overload?
    return defaultWorkStealingPool(Math.min(Runtime.getRuntime().availableProcessors(), 16),
        "stf-grp-" + metaGrp.nameLowerCase() + "-job-load-worker", true);
  }

  public static StfExecutorService newDefaultExec(int threadKeepAliveTimeSeconds, int taskQueueSize,
      RejectedExecutionHandler rejectPolicy, boolean allowCoreThreadTimeOut) {
    return new StfExecutorService(newIoPrefer(new LinkedBlockingQueue<>(taskQueueSize),
        NamedThreadFactory.of("stf-dft-exec"), threadKeepAliveTimeSeconds, allowCoreThreadTimeOut, rejectPolicy));
  }

  public static Thread newWatcherOfJobManager(String type, Runnable runnable) {
    return new NamedThreadFactory("stf-type-" + type + "-mngr-watcher", true).newThread(runnable);
  }

  private StfInternalExecutors() {
  }
}