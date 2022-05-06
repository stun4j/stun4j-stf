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

import static com.stun4j.stf.core.job.JobConsts.ALL_JOB_GROUPS;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfJobManager;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfJobManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.monitor.StfMonitor;
import com.stun4j.stf.core.support.BaseLifeCycle;

import sun.misc.Signal;

/**
 * The core coordinator of the stf-job microkernel
 * <ul>
 * <li>In cluster deployment, only one active running instance is maintained for each stf-job</li>
 * <li>Try the best to prevent OOM,even when we have a lot of jobs to handle</li>
 * <li>An intelligent, adaptive job handling mechanism is built-in</li>
 * <li>Graceful shutdown is supported</li>
 * </ul>
 * @author Jay Meng
 */
public class JobManager extends BaseLifeCycle {
  private static final int DFT_MIN_SCAN_FREQ_SECONDS = 3;
  private static final int DFT_SCAN_FREQ_SECONDS = 3;

  private static final int DFT_MIN_HANDLE_BATCH_SIZE = 5;
  private static final int DFT_MAX_HANDLE_BATCH_SIZE = 5000;
  private static final int DFT_HANDLE_BATCH_SIZE = 20;

  private final ConcurrentHashMap<String, AtomicBoolean> handlings;
  private final StfCore stfCore;
  private final JobLoader loader;
  private final JobRunners runners;
  private final JobRunner runner;

  private final ScheduledExecutorService watcher;
  private final Map<String, ThreadPoolExecutor> workers;

  private ScheduledFuture<?> sf;

  private int scanFreqSeconds;
  private int handleBatchSize;

  private boolean vmResCheckEnabled;

  @Override
  protected void doStart() {
    loader.doStart();

    if (vmResCheckEnabled) {
      StfMonitor.INSTANCE.doStart();
    }

    int scanFreqSeconds;
    sf = watcher.scheduleWithFixedDelay(() -> {
      try {
        if (vmResCheckEnabled) {
          Pair<Boolean, Map<String, Object>> resRpt;
          if ((resRpt = StfMonitor.INSTANCE.isVmResourceNotEnough()).getLeft()) {
            LOG.warn("Handling of stf-jobs is paused due to insufficient resources > Reason: {}", resRpt.getRight());
            return;
          }
        }
        takeJobsAndRun();
      } catch (Throwable e) {
        LOG.error("[on schedule] Handle stf-jobs error", e);
      }
    }, scanFreqSeconds = this.scanFreqSeconds, scanFreqSeconds, TimeUnit.SECONDS);

    LOG.info("The stf-job-manager is successfully started");
  }

  @Override
  protected void doShutdown() {
    // TODO mj:extract close utility...
    try {
      watcher.shutdown();
      watcher.awaitTermination(15, TimeUnit.SECONDS);

      if (sf != null) {
        sf.cancel(false);
      }
      LOG.info("Watcher is successfully shut down");
    } catch (Throwable e) {
      LOG.error("Unexpected watcher shutdown error", e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
    workers.forEach((grp, worker) -> {
      try {
        worker.shutdown();
        worker.awaitTermination(15, TimeUnit.SECONDS);
        LOG.info("Worker is successfully shut down [grp={}]", grp);
      } catch (Throwable e) {
        LOG.error("Unexpected worker shutdown error [grp={}]", grp, e);
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    });
    runners.shutdown();
    loader.shutdown();

    if (vmResCheckEnabled) {
      StfMonitor.INSTANCE.shutdown();
    }
  }

  protected boolean tryLockJob(String jobGrp, Long jobId, int timeoutSecs, int curRetryTimes) {
    if (!stfCore.tryLockStf(jobId, timeoutSecs, curRetryTimes)) {
      LOG.warn("Try lock job#{} fail,it may be running [jobGrp={}]", jobId, jobGrp);
      return false;
    }
    return true;
  }

  public Stf takeUniqueJob(String jobGrp) {
    while (!Thread.currentThread().isInterrupted()) {
      Stf job = null;
      try {
        job = loader.getJobFromQueue(jobGrp);
        if (job == null) return null;

        Pair<Boolean, Integer> pair;
        if (!(pair = runner.checkWhetherTheJobCanRun(job, stfCore)).getKey()) {
          return null;
        }

        if (tryLockJob(jobGrp, job.getId(), pair.getValue(), job.getRetryTimes())) {
          // job.setExecutor(jobMayLocked.getExecutor());TODO mj:record who lock the stf-job if necessary
          return job;
        }
      } catch (Throwable e) {
        LOG.error("Try lock job#{} error", job != null ? job.getId() : "null", e);
      } finally {
        loader.signalToLoadJobs(jobGrp);
      }
    }
    return null;
  }

  private void takeJobsAndRun() {
    workers.forEach((jobGrp, worker) -> {
      worker.execute(() -> {
        takeJobsAndRun(jobGrp);
      });
    });
  }

  private void takeJobsAndRun(String jobGrp) {
    int batchSize = handleBatchSize;
    AtomicBoolean handling = handlings.computeIfAbsent(jobGrp, k -> new AtomicBoolean());
    if (handling.compareAndSet(false, true)) {
      try {
        int availableThread = runners.getAvailablePoolSize(jobGrp);
        int it = availableThread % batchSize == 0 ? availableThread / batchSize : availableThread / batchSize + 1;
        start: for (int i = 1; i <= it; i++) {
          int size = batchSize;
          if (i == it) {
            size = availableThread - batchSize * (it - 1);
          }
          int finalSize = size;
          for (int j = 0; j < finalSize; j++) {
            Stf job = takeUniqueJob(jobGrp);
            if (job == null) {
              break start;
            }
            runners.execute(jobGrp, job);
          }
        }
      } finally {
        handling.compareAndSet(true, false);
      }
    }
  }

  @SuppressWarnings("restriction")
  private void registerGracefulShutdown() {
    try {
      Signal.handle(new Signal(getOSSignalType()), s -> {
        shutdownGracefully();
      });
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      shutdownGracefully();
    }));
  }

  private void shutdownGracefully() {
    // Never use 'System.exit' here!!!
    try {
      shutdown();
      LOG.info("[on jvm-shutdown] The stf-job-manager is gracefully shut down");
    } catch (Throwable e) {
      LOG.error("[on jvm-shutdown] The stf-job-manager shutdown error", e);
    }
  }

  private static String getOSSignalType() {
    return System.getProperties().getProperty("os.name").toLowerCase().startsWith("win") ? "INT" : "TERM";
  }

  public JobManager(JobLoader loader, JobRunners runners) {
    this.scanFreqSeconds = DFT_SCAN_FREQ_SECONDS;
    this.handleBatchSize = DFT_HANDLE_BATCH_SIZE;
    this.vmResCheckEnabled = true;
    this.stfCore = runners.getStfCore();
    this.loader = loader;
    this.runners = runners;
    this.runner = runners.getRunner();
    this.handlings = new ConcurrentHashMap<>();
    this.workers = Stream.of(ALL_JOB_GROUPS).reduce(new HashMap<String, ThreadPoolExecutor>(), (map, grp) -> {
      map.put(grp, newWorkerOfJobManager(grp));
      return map;
    }, (a, b) -> null);
    this.watcher = newWatcherOfJobManager();
  }

  public void setScanFreqSeconds(int scanFreqSeconds) {
    this.scanFreqSeconds = scanFreqSeconds < DFT_MIN_SCAN_FREQ_SECONDS ? DFT_MIN_SCAN_FREQ_SECONDS : scanFreqSeconds;
  }

  public void setHandleBatchSize(int handleBatchSize) {
    this.handleBatchSize = handleBatchSize < DFT_MIN_HANDLE_BATCH_SIZE ? DFT_MIN_HANDLE_BATCH_SIZE
        : (handleBatchSize > DFT_MAX_HANDLE_BATCH_SIZE ? DFT_MAX_HANDLE_BATCH_SIZE : handleBatchSize);
  }

  public void setVmResCheckEnabled(boolean vmResCheckEnabled) {
    this.vmResCheckEnabled = vmResCheckEnabled;
  }

  {
    registerGracefulShutdown();
  }
}