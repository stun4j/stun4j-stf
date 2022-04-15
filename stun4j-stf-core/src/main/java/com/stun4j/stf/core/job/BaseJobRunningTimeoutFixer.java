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

import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfRunningJobTimeoutFixer;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.support.BaseLifeCycle;

/**
 * Base class for finding timeout jobs and getting them to run again
 * @author Jay Meng
 */
public abstract class BaseJobRunningTimeoutFixer extends BaseLifeCycle {
  private static final int DFT_MIN_TIMEOUT_SECONDS = 10;
  private static final int DFT_MAX_TIMEOUT_SECONDS = 5 * 60;
  private static final int DFT_TIMEOUT_SECONDS = 30;

  private static final int DFT_MIN_LIMIT_PER_SCAN = 50;
  private static final int DFT_MAX_LIMIT_PER_SCAN = 5000;
  private static final int DFT_LIMIT_PER_SCAN = 1000;

  private static final int DFT_MIN_CHECK_FIX_FREQ_SECONDS = 5;
  private static final int DFT_MAX_CHECK_FIX_FREQ_SECONDS = 5 * 60;
  private static final int DFT_SCAN_FREQ_SECONDS = 10;

  protected final JobScanner scanner;
  private final JobRunners runners;

  private final ScheduledExecutorService watcher;

  protected int scanFreqSeconds;
  protected long timeoutMs;
  protected int limitPerScan;
  private ScheduledFuture<?> sf;

  @Override
  public void doStart() {
    int scanFreqSeconds;
    sf = watcher.scheduleWithFixedDelay(() -> {
      try {
        this.checkAndFix();
      } catch (Throwable e) {
        LOG.error("[on schedule] Check&fix timeout running stf-jobs error", e);
      }
    }, scanFreqSeconds = this.scanFreqSeconds, scanFreqSeconds, TimeUnit.SECONDS);
    LOG.debug("The stf-job-running-timeout-fixer is successfully started");
  }

  @Override
  public void doShutdown() {
    try {
      watcher.shutdown();
      watcher.awaitTermination(15, TimeUnit.SECONDS);

      if (sf != null) {
        sf.cancel(false);// the best effort for the completion of underlying operations
      }
      LOG.info("Watcher is successfully shut down");
    } catch (Throwable e) {
      LOG.error("Unexpected watcher shutdown error", e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void checkAndFix() {
    try (Stream<Stf> jobsMayTimeoutStream = scanner.scanTimeoutJobsStillAlive(timeoutMs, limitPerScan, true, "id")) {
      List<Stf> jobsMayTimeout = jobsMayTimeoutStream.collect(Collectors.toList());
      if (jobsMayTimeout.size() == 0) {
        return;
      }
      doCheckAndFix(runners.getNotRunning(jobsMayTimeout));
    }
  }

  protected abstract void doCheckAndFix(Stream<Stf> jobsMayTimeout);

  BaseJobRunningTimeoutFixer(JobScanner scanner, JobRunners runners) {
    this.scanner = scanner;
    this.runners = runners;
    this.watcher = newWatcherOfRunningJobTimeoutFixer();

    this.scanFreqSeconds = DFT_SCAN_FREQ_SECONDS;
    this.timeoutMs = DFT_TIMEOUT_SECONDS * 1000;
    this.limitPerScan = DFT_LIMIT_PER_SCAN;
  }

  public void setScanFreqSeconds(int scanFreqSeconds) {
    this.scanFreqSeconds = scanFreqSeconds < DFT_MIN_CHECK_FIX_FREQ_SECONDS ? DFT_MIN_CHECK_FIX_FREQ_SECONDS
        : (scanFreqSeconds > DFT_MAX_CHECK_FIX_FREQ_SECONDS ? DFT_MAX_CHECK_FIX_FREQ_SECONDS : scanFreqSeconds);
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutMs = timeoutSeconds < DFT_MIN_TIMEOUT_SECONDS ? DFT_MIN_TIMEOUT_SECONDS * 1000
        : (timeoutSeconds > DFT_MAX_TIMEOUT_SECONDS ? DFT_MAX_TIMEOUT_SECONDS * 1000 : timeoutSeconds * 1000);
  }

  public void setLimitPerScan(int limitPerScan) {
    this.limitPerScan = limitPerScan < DFT_MIN_LIMIT_PER_SCAN ? DFT_MIN_LIMIT_PER_SCAN
        : (limitPerScan > DFT_MAX_LIMIT_PER_SCAN ? DFT_MAX_LIMIT_PER_SCAN : limitPerScan);
  }

}