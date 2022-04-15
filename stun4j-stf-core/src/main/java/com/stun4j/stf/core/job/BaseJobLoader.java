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

import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfJobLoading;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfJobLoading;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.support.BaseLifeCycle;

/**
 * Base class for loading stf-jobs into queue to speed up their retrieval
 * @author Jay Meng
 */
public abstract class BaseJobLoader extends BaseLifeCycle {
  private static final int DFT_MIN_LOAD_SIZE = 100;
  private static final int DFT_MAX_LOAD_SIZE = 2000;
  private static final int DFT_LOAD_SIZE = 300;

  private static final int DFT_MIN_SCAN_FREQ_SECONDS = 3;
  private static final int DFT_SCAN_FREQ_SECONDS = 3;

  private static final double DFT_LOAD_FACTOR = 0.2;

  private final Set<String> allGrpsLoadingSignal;
  private final ConcurrentHashMap<String, JobQueue> queuesAllGrps;

  private final ScheduledExecutorService watcher;
  private final ExecutorService worker;

  private ScheduledFuture<?> sf;
  private int loadSize;
  private int scanFreqSeconds;
  private double loadFactor;

  @Override
  public void doStart() {
    int scanFreqSeconds;
    sf = watcher.scheduleWithFixedDelay(() -> {
      smartLoadJobsToQueue();
    }, scanFreqSeconds = this.scanFreqSeconds, scanFreqSeconds, TimeUnit.SECONDS);

    LOG.debug("The stf-job-fetcher is successfully started");
  }

  @Override
  public void doShutdown() {
    try {
      watcher.shutdownNow();
      if (sf != null) {
        sf.cancel(true);
      }
      LOG.info("Watcher is successfully shut down");
    } catch (Throwable e) {
      LOG.error("Unexpected watcher shutdown error", e);
    }

    try {
      worker.shutdownNow();
      LOG.info("Worker is successfully shut down");
    } catch (Throwable e) {
      LOG.error("Unexpected worker shut down error", e);
    }
  }

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  protected abstract Stream<Stf> loadJobs(String jobGrp, int loadSize);

  Stf getJobFromQueue(String jobGrp) {
    JobQueue queue = getOrCreateQueue(jobGrp);
    Stf job = queue.poll();
    return job;
  }

  void signalToLoadJobs(String... grpsToLoad) {
    for (String grpToLoad : grpsToLoad) {
      allGrpsLoadingSignal.add(grpToLoad);
    }
  }

  private void smartLoadJobsToQueue() {
    for (String jobGrp : allGrpsLoadingSignal) {
      worker.execute(() -> doLoadJobsToQueue(jobGrp));
    }
  }

  private void doLoadJobsToQueue(String jobGrp) {
    try {
      JobQueue queue = getOrCreateQueue(jobGrp);
      int queueSize = queue.size();
      if (!isAtLowWaterMark(queueSize)) {
        return;
      }
      int needLoadSize = loadSize + queueSize;
      try (Stream<Stf> loadedJobStream = loadJobs(jobGrp, needLoadSize)) {
        List<Stf> loadedJobs = loadedJobStream.collect(Collectors.toList());
        if (loadedJobs.size() == 0) {
          return;
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Loaded and try enqueuing stf-jobs [grp={}, loaded={}, queue size before={}]", jobGrp,
              loadedJobs.size(), queueSize);
        }
        for (Stf job : loadedJobs) {
          if (!queue.offer(job)) {
            break;
          }
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("The stf-jobs are enqueued [grp={}, queue size after={}]", jobGrp, queue.size());
      }
    } catch (Throwable e) {
      LOG.error("Enqueue stf-job error", e);
    } finally {
      allGrpsLoadingSignal.remove(jobGrp);
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
    loadSize = DFT_LOAD_SIZE;
    scanFreqSeconds = DFT_SCAN_FREQ_SECONDS;
    loadFactor = DFT_LOAD_FACTOR;

    allGrpsLoadingSignal = ConcurrentHashMap.newKeySet();
    queuesAllGrps = new ConcurrentHashMap<>();
    worker = newWorkerOfJobLoading();
    watcher = newWatcherOfJobLoading();
  }

  public void setLoadSize(int loadSize) {
    this.loadSize = loadSize < DFT_MIN_LOAD_SIZE ? DFT_MIN_LOAD_SIZE
        : (loadSize > DFT_MAX_LOAD_SIZE ? DFT_MAX_LOAD_SIZE : loadSize);
  }

  public void setScanFreqSeconds(int scanFreqSeconds) {
    this.scanFreqSeconds = scanFreqSeconds < DFT_MIN_SCAN_FREQ_SECONDS ? DFT_MIN_SCAN_FREQ_SECONDS : scanFreqSeconds;
  }
}