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

import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.StfConsts.DFT_MIN_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.job.JobConsts.ALL_JOB_GROUPS;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfJobLoading;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfJobLoading;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.cluster.StfClusterMember;
import com.stun4j.stf.core.support.BaseLifecycle;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * Base class for loading stf-jobs into queue to speed up their retrieval
 * @author Jay Meng
 */
public abstract class BaseJobLoader extends BaseLifecycle {
  private static final int DFT_MIN_LOAD_SIZE = 100;
  private static final int DFT_MAX_LOAD_SIZE = 3000;
  private static final int DFT_LOAD_SIZE = 300;

  private static final int DFT_MIN_SCAN_FREQ_SECONDS = 3;
  private static final int DFT_SCAN_FREQ_SECONDS = 3;

  private static final double DFT_LOAD_FACTOR = 0.2;

  private final ConcurrentHashMap<String, JobQueue> queuesAllGrps;

  private final ScheduledExecutorService watcher;
  private final Map<String, ExecutorService> workers;

  private long jobTimeoutMs;

  private ScheduledFuture<?> sf;
  private int loadSize;
  private int scanFreqSeconds;
  private double loadFactor;

  @Override
  public void doStart() {
    int scanFreqSeconds;
    sf = watcher.scheduleWithFixedDelay(() -> {
      onSchedule();
    }, scanFreqSeconds = this.scanFreqSeconds, scanFreqSeconds, TimeUnit.SECONDS);

    LOG.debug("The stf-job-loader is successfully started");
  }

  @Override
  public void doShutdown() {
    try {
      if (sf != null) {
        sf.cancel(true);
      }
      watcher.shutdownNow();
      LOG.debug("Watcher is successfully shut down");
    } catch (Throwable e) {
      Exceptions.swallow(e, LOG, "An error occurred while shutting down watcher");
    }

    workers.forEach((grp, worker) -> {
      try {
        worker.shutdownNow();
        LOG.debug("Worker is successfully shut down [grp={}]", grp);
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while shutting down worker [grp={}]", grp);
      }
    });
    LOG.debug("The stf-job-loader is successfully shut down");
  }

  private void onSchedule() {
    StfClusterMember.sendHeartbeat();
    workers.forEach((jobGrp, worker) -> {
      worker.execute(() -> doLoadJobsToQueue(jobGrp));
    });
  }

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  protected abstract Stream<Stf> loadJobs(String jobGrp, int loadSize);

  Stf getJobFromQueue(String jobGrp, boolean blocking) throws InterruptedException {
    JobQueue queue = getOrCreateQueue(jobGrp);
    Stf job = blocking ? queue.take() : queue.poll();
    return job;
  }

  private void doLoadJobsToQueue(String jobGrp) {
    try {
      JobQueue queue = getOrCreateQueue(jobGrp);
      int queueSize = queue.size();
      if (!isAtLowWaterMark(queueSize)) {
        return;
      }
      int needLoadSize = loadSize + queueSize;
      int enqueued = 0;
      int rejected = 0;
      try (Stream<Stf> jobStream = loadJobs(jobGrp, needLoadSize)) {
        for (Iterator<Stf> iter = jobStream.iterator(); iter.hasNext();) {// This way non-blocking must be ensured
          Stf job = iter.next();
          int res;
          if ((res = queue.offer(job)) < 0) {
            break;
          }
          if (res > 0) {
            enqueued++;
          } else {
            rejected++;
          }
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Loading and try enqueuing stf-jobs [grp={}, enqueued={}, rejected={}, needLoadSize={}, queue-size before/after={}/{}]",
            jobGrp, enqueued, rejected, needLoadSize, queueSize, queue.size());
      }
    } catch (Throwable e) {
      Exceptions.swallow(e, LOG, "An error occurred while enqueuing stf-job");
    }
  }

  private JobQueue getOrCreateQueue(String jobGrp) {
    JobQueue queue = queuesAllGrps.computeIfAbsent(jobGrp, k -> new JobQueue(loadSize));
    return queue;
  }

  private boolean isAtLowWaterMark(int size) {
    return size / (loadSize * 1.0) < loadFactor;
  }

  BaseJobLoader() {
    jobTimeoutMs = DFT_JOB_TIMEOUT_SECONDS * 1000;

    loadSize = DFT_LOAD_SIZE;
    scanFreqSeconds = DFT_SCAN_FREQ_SECONDS;
    loadFactor = DFT_LOAD_FACTOR;

    queuesAllGrps = new ConcurrentHashMap<>();
    workers = Stream.of(ALL_JOB_GROUPS).reduce(new HashMap<String, ExecutorService>(), (map, grp) -> {
      map.put(grp, newWorkerOfJobLoading(grp));
      return map;
    }, (a, b) -> null);
    watcher = newWatcherOfJobLoading();
  }

  public long getJobTimeoutMs() {
    return jobTimeoutMs;
  }

  public void setLoadSize(int loadSize) {
    this.loadSize = loadSize < DFT_MIN_LOAD_SIZE ? DFT_MIN_LOAD_SIZE
        : (loadSize > DFT_MAX_LOAD_SIZE ? DFT_MAX_LOAD_SIZE : loadSize);
  }

  public void setScanFreqSeconds(int scanFreqSeconds) {
    this.scanFreqSeconds = scanFreqSeconds < DFT_MIN_SCAN_FREQ_SECONDS ? DFT_MIN_SCAN_FREQ_SECONDS : scanFreqSeconds;
  }

  public void setJobTimeoutSeconds(int jobTimeoutSeconds) {
    this.jobTimeoutMs = jobTimeoutSeconds < DFT_MIN_JOB_TIMEOUT_SECONDS ? DFT_MIN_JOB_TIMEOUT_SECONDS * 1000
        : jobTimeoutSeconds * 1000;
  }
}