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
package com.stun4j.stf.core.cluster;

import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWatcherOfMemberHeartbeat;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfMemberHeartbeat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;
import com.stun4j.stf.core.support.BaseLifeCycle;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * @author JayMeng
 */
public abstract class HeartbeatHandler extends BaseLifeCycle {
  private static final int DFT_MIN_HB_TIMEOUT_SECONDS = 3;
  private static final int DFT_MAX_HB_TIMEOUT_SECONDS = 60;
  private static final int DFT_HB_TIMEOUT_SECONDS = 10;
  private final ExecutorService worker;

  private final AtomicInteger cnt;
  private final ScheduledExecutorService watcher;

  private ScheduledFuture<?> sf;
  private int timeoutMs;

  protected final Map<String, Long> localMemberTracingMemo;

  @Override
  public void doStart() {
    onStartup();

    sf = watcher.scheduleWithFixedDelay(() -> {
      onSchedule();
    }, 0, 5, TimeUnit.SECONDS);

    LOG.debug("The heartbeat-handler is successfully started");
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
      Exceptions.swallow(e, LOG, "Unexpected error occurred while shutting down watcher");
    }

    try {
      worker.shutdownNow();
      LOG.debug("Worker is successfully shut down");
    } catch (Throwable e) {
      Exceptions.swallow(e, LOG, "Unexpected error occurred while shutting down worker");
    }
    String memberId = null;
    try {
      onShutdown(memberId = StfClusterMember
          .calculateId());/*- Recalculate the latest local-member-id for more robust deregister */
    } catch (Throwable e) {
      Exceptions.swallow(e, LOG, "The local-member#{} deregister error with local-member-tracing-memo:{} ", memberId,
          localMemberTracingMemo);
    }

    LOG.debug("The heartbeat-handler is successfully shut down");
  }

  private void onSchedule() {
    onHeartbeat(Heartbeat.SIGNAL);
  }

  @Subscribe
  public void onHeartbeat(Heartbeat hb) {
    if (cnt.incrementAndGet() < 3) {
      return;
    }
    cnt.set(0);
    worker.execute(() -> {
      try {
        doSendHeartbeat();
      } catch (Throwable e) {
        Exceptions.swallow(e, LOG, "An error occurred while sending heartbeat");
      }
    });
  }

  protected abstract void onStartup();

  protected abstract void onShutdown(String memberId);

  protected abstract void doSendHeartbeat();

  {
    worker = newWorkerOfMemberHeartbeat();
    watcher = newWatcherOfMemberHeartbeat();
    cnt = new AtomicInteger();
    timeoutMs = DFT_HB_TIMEOUT_SECONDS * 1000;
    localMemberTracingMemo = new ConcurrentHashMap<>();
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutMs = timeoutSeconds < DFT_MIN_HB_TIMEOUT_SECONDS ? DFT_MIN_HB_TIMEOUT_SECONDS * 1000
        : (timeoutSeconds > DFT_MAX_HB_TIMEOUT_SECONDS ? DFT_MAX_HB_TIMEOUT_SECONDS * 1000 : timeoutSeconds * 1000);
  }

}
