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

import static com.stun4j.stf.core.StfConsts.allDataSourceKeys;
import static com.stun4j.stf.core.StfHelper.determinJobMetaGroup;
import static com.stun4j.stf.core.StfHelper.newHashMap;
import static com.stun4j.stf.core.StfHelper.registerGracefulShutdown;
import static com.stun4j.stf.core.StfRunMode.DEFAULT;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfJobManager;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfDelayQueueCore;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.StfRunMode;
import com.stun4j.stf.core.cluster.Heartbeat;
import com.stun4j.stf.core.cluster.HeartbeatHandler;
import com.stun4j.stf.core.cluster.StfClusterMember;
import com.stun4j.stf.core.monitor.StfMonitor;
import com.stun4j.stf.core.support.BaseLifecycle;
import com.stun4j.stf.core.support.event.StfEventBus;
import com.stun4j.stf.core.support.event.StfReceivedEvent;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.Utils;

/**
 * The core coordinator of the stf-job microkernel
 * <ul>
 * <li>In cluster deployment, only one active running instance is maintained for each stf-job</li>
 * <li>Try the best to prevent resource competition,OOM and other kinda problems,even when we have a
 * lot of jobs to handle</li>
 * <li>An intelligent, adaptive job handling mechanism is built-in</li>
 * <li>Graceful shutdown is supported</li>
 * </ul>
 * 
 * @author Jay Meng
 */
@SuppressWarnings("restriction")
public class JobManager extends BaseLifecycle {
  private static final int DFT_MIN_SCAN_FREQ_SECONDS = 3;
  private static final int DFT_SCAN_FREQ_SECONDS = 3;

  private static final int DFT_MIN_HANDLE_BATCH_SIZE = 1;
  private static final int DFT_MAX_HANDLE_BATCH_SIZE = 3000;
  private static final int DFT_HANDLE_BATCH_SIZE = 20;

  private final StfCore stfCore;
  private final StfRunMode runMode;
  private final JobLoader loader;
  private final JobRunners runners;
  private final JobRunner runner;
  private final JobMarkActor marker;
  private final boolean delayQueueEnabled;

  private JobDelayMarkActor delayMarker;

  private Map<String, Thread> watchers;

  private HeartbeatHandler heartbeatHandler;
  private int handleBatchSize;
  @Deprecated
  private int scanFreqSeconds;

  private boolean vmResCheckEnabled;
  private volatile boolean shutdown;

  @Override
  protected void doStart() {
    if (runMode == DEFAULT) {
      heartbeatHandler.doStart();
    }
    marker.doStart();
    if (this.delayQueueEnabled) {
      delayMarker.doStart();
    }

    if (runMode == DEFAULT) {
      loader.doStart();

      if (vmResCheckEnabled) {
        StfMonitor.INSTANCE.doStart();
      }

      watchers.forEach((type, watcher) -> {
        if (determinJobMetaGroup(type) == StfMetaGroup.DELAY && !this.delayQueueEnabled) {
          return;
        }
        watcher.start();
      });
    }

    LOG.info("The stf-job-manager({} mode, batch {}, dlq {}) is successfully started.", runMode.nameLowerCase(),
        this.handleBatchSize > 1 ? "enabled" : "disabled", this.delayQueueEnabled ? "enabled" : "disabled");
  }

  @Override
  protected void doShutdown() {
    shutdown = true;
    // TODO mj:extract close utility...
    watchers.forEach((type, watcher) -> {
      try {
        watcher.interrupt();
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while shutting down watcher [type={}]", type);
      }
    });

    runners.shutdown();
    loader.shutdown();

    StfMonitor.INSTANCE.shutdown();

    if (delayMarker != null) {
      delayMarker.shutdown();
    }
    marker.shutdown();
    heartbeatHandler.shutdown();

    LOG.info("The stf-job-manager is successfully shut down");
  }

  protected long lockJob(StfMetaGroup metaGrp, Long jobId, int timeoutSecs, int lastRetryTimes, long lastTimeoutAt) {
    long lockedAt;
    if ((lockedAt = stfCore.lockStf(metaGrp, jobId, timeoutSecs, lastRetryTimes, lastTimeoutAt)) == -1) {
      LOG.warn("Lock job#{} failed. It may be running [metaGrp={}]", jobId, metaGrp);
      return lockedAt;
    }
    return lockedAt;
  }

  private void shutdownGracefully() {
    // Never use 'System.exit' here!!!
    try {
      shutdown();
    } catch (Throwable e) {
      Exceptions.swallow(e, LOG, "[on jvm-shutdown] An error occurred while shutting down stf-job-manager");
    }
  }

  public JobManager(JobLoader loader, JobRunners runners) {
    this.scanFreqSeconds = DFT_SCAN_FREQ_SECONDS;
    this.handleBatchSize = DFT_HANDLE_BATCH_SIZE;
    this.vmResCheckEnabled = true;
    StfCore stfc;
    this.stfCore = stfc = runners.getStfCore();
    this.runMode = stfc.getRunMode();
    boolean delayQueueEnabled;
    this.delayQueueEnabled = delayQueueEnabled = ((StfDelayQueueCore)stfc).isDelayQueueEnabled();
    this.loader = loader;
    this.runners = runners;
    this.runner = runners.getRunner();
    StfEventBus.registerHandler(this.marker = new JobMarkActor(stfc, 16384));// TODO mj:to be configured
    if (delayQueueEnabled) {
      StfEventBus.registerHandler(this.delayMarker = new JobDelayMarkActor(stfc, 16384));
    }

    if (runMode != DEFAULT) {
      return;
    }
    this.watchers = newHashMap(allDataSourceKeys(), (map, dsKey) -> {
      Runnable runnable;
      if (dsKey.equals(Heartbeat.typeNameLowerCase())) {
        runnable = () -> {
          while (!Thread.currentThread().isInterrupted() && !shutdown) {
            try {
              Utils.sleepSeconds(scanFreqSeconds);
              StfClusterMember.sendHeartbeat();
            } catch (Throwable e) {
              Exceptions.swallow(e, LOG, "An error occurred while sending heartbeat");
            }
          }
          LOG.info("The {} seems going through a shutdown", Thread.currentThread().getName());
        };
      } else {
        StfMetaGroup metaGrp = StfMetaGroup.valueOf(dsKey.toUpperCase());
        if (metaGrp == StfMetaGroup.DELAY && !delayQueueEnabled) {
          return map;
        }
        runnable = () -> {
          int handleBatchSize = this.handleBatchSize;
          JobBatchLockAndRunActor batcher = null;
          if (handleBatchSize > 1) {
            batcher = new JobBatchLockAndRunActor(stfc, runners, 16384, metaGrp, handleBatchSize);
            batcher.start();// TODO mj:Cascade special #start
          }

          while (!Thread.currentThread().isInterrupted() && !shutdown) {
            if (vmResCheckEnabled) {// FIXME mj:try-catch
              Pair<Boolean, Map<String, Object>> resRpt;
              if ((resRpt = StfMonitor.INSTANCE.isVmResourceNotEnough()).getLeft()) {
                LOG.warn("Handling of stf-jobs is paused due to insufficient resources > Reason: {}",
                    resRpt.getRight());// TODO mj:log inhibition stuff
                Utils.sleepSeconds(scanFreqSeconds);// TODO mj:spin-blocking instead
                continue;
              }
            }

            // TODO mj: strategization,2 strategies: simple,adaptive batch
            if (batcher != null) {
              try {
                Stf job = loader.getJobFromQueue(metaGrp, true);
                if (job == null) {// Shouldn't happen TODO mj:tiny sleep
                  LOG.error("Unexpected null job found [metaGrp={}]", metaGrp);
                  continue;
                }
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Got job#{} from queue", job.getId());
                }

                Pair<Boolean, Integer> pair;
                if (!(pair = runner.checkWhetherTheJobCanRun(metaGrp, job, stfc)).getKey()) {
                  continue;
                }
                batcher.tell(new StfReceivedEvent(job, pair.getValue()));
              } catch (Throwable e) {
                Exceptions.swallow(e, LOG, "An error occurred while handling jobs [metaGrp={}]", metaGrp);
              }
              continue;
            }
            Stf job = null;
            try {
              job = loader.getJobFromQueue(metaGrp, true);
              if (job == null) {// Shouldn't happen TODO mj:tiny sleep
                LOG.error("Unexpected null job found [metaGrp={}]", metaGrp);
                continue;
              }
              if (LOG.isDebugEnabled()) {
                LOG.debug("Got job#{} from queue", job.getId());
              }

              Pair<Boolean, Integer> pair;
              if (!(pair = runner.checkWhetherTheJobCanRun(metaGrp, job, stfc)).getKey()) {/*-A double check here,meanwhile,pick up the dynamic timeout because we are using a
                 custom gradient retry mechanism*/
                continue;
              }

              long lockedAt;
              int dynaTimeoutSecs;
              if ((lockedAt = lockJob(metaGrp, job.getId(), dynaTimeoutSecs = pair.getValue(), job.getRetryTimes(),
                  job.getTimeoutAt())) <= 0) {
                continue;
              }
              job.partialUpdateInfoWhenLocked(lockedAt, dynaTimeoutSecs);

              runners.execute(metaGrp, job);
            } catch (Throwable e) {
              Exceptions.swallow(e, LOG, "An error occurred while handling the job#{}",
                  job != null ? job.getId() : "null");
            }
          }
          LOG.info("The {} seems going through a shutdown", Thread.currentThread().getName());
          if (batcher != null) {
            batcher.shutdown();
          }
        };
      }
      map.put(dsKey, newWatcherOfJobManager(dsKey, runnable));
      return map;
    });
  }

  public JobManager withHeartbeatHandler(HeartbeatHandler heartbeatHandler) {
    StfEventBus.registerHandler(heartbeatHandler);
    this.heartbeatHandler = heartbeatHandler;
    return this;
  }

  @Deprecated
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
    registerGracefulShutdown(LOG, () -> {
      shutdownGracefully();
      return null;
    });
  }
}