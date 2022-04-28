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
package com.stun4j.stf.core.utils.executor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.stf.core.utils.ThreadPoolUtils;

/**
 * Help class to create (bounded)thread pools uniformly
 * @author Jay Meng
 */
public final class PoolExecutors {
  private static final Logger LOG = LoggerFactory.getLogger(PoolExecutors.class);
  public static final RejectedExecutionHandler BACK_PRESSURE_POLICY = new CallerRunsPolicy();
  public static final RejectedExecutionHandler DROP_WITH_EX_THROW_POLICY = new AbortPolicy();
  public static final RejectedExecutionHandler SILENT_DROP_POLICY = new DiscardPolicy();
  public static final RejectedExecutionHandler SILENT_DROP_OLDEST_POLICY = new DiscardOldestPolicy();

  public static ExecutorService defaultIoPrefer(String prefix) {
    return newDynamicIoPrefer(new LinkedBlockingQueue<>(1024), NamedThreadFactory.of(prefix), 60, true,
        BACK_PRESSURE_POLICY);
  }

  public static ExecutorService defaultWorkStealingPool(String prefix, boolean daemon) {
    return newWorkStealingPool(Runtime.getRuntime().availableProcessors(), prefix, daemon, null);
  }

  public static ExecutorService newDynamicIoPrefer(BlockingQueue<Runnable> queue, ThreadFactory threadFactory,
      int keepAliveTimeSeconds, boolean allowCoreThreadTimeOut, RejectedExecutionHandler reject) {
    ThreadPoolExecutor exec = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
        Math.min(ThreadPoolUtils.ioIntensivePoolSize(), 64), keepAliveTimeSeconds, TimeUnit.SECONDS, queue,
        threadFactory, reject);
    exec.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
    return exec;
  }

  public static ScheduledExecutorService newScheduler(int corePoolSize, String prefix, boolean daemon) {
    return newScheduler(corePoolSize, new NamedThreadFactory(prefix, daemon), DROP_WITH_EX_THROW_POLICY);
  }

  public static ScheduledExecutorService newScheduler(int corePoolSize, ThreadFactory threadFactory,
      RejectedExecutionHandler reject) {
    return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory, reject);
  }

  public static ScheduledExecutorService newSingleThreadScheduler(String prefix, boolean daemon) {
    return newScheduler(1, prefix, daemon);
  }

  public static ExecutorService newWorkStealingPool(int parallelism, String prefix, boolean daemon,
      UncaughtExceptionHandler handler) {
    return new ForkJoinPool(parallelism, new NamedForkJoinWorkerThreadFactory(prefix, daemon), handler, true);
  }

  /**
   * A handler for rejected tasks that silently discards the
   * rejected task.
   * @author Doug Lea
   * @author Jay Meng
   *         <p>
   *         Add log to silently discarded task
   */
  public static class DiscardPolicy implements RejectedExecutionHandler {
    /**
     * Creates a {@code DiscardPolicy}.
     */
    public DiscardPolicy() {
    }

    /**
     * Does nothing, which has the effect of discarding task r.
     * @param r the runnable task requested to be executed
     * @param e the executor attempting to execute this task
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
      // TODO mj:the flow-ctrl mechanism
      logOnSilentDrop(e);
    }
  }

  /**
   * A handler for rejected tasks that discards the oldest unhandled
   * request and then retries {@code execute}, unless the executor
   * is shut down, in which case the task is discarded.
   * @author Doug Lea
   * @author Jay Meng
   *         <ul>
   *         <li>Add log to silently discarded task.</li>
   *         <li>Fixed a bug that could cause a stack overflow.</li>
   *         <ul>
   */
  public static class DiscardOldestPolicy implements RejectedExecutionHandler {
    /**
     * Creates a {@code DiscardOldestPolicy} for the given executor.
     */
    public DiscardOldestPolicy() {
    }

    /**
     * Obtains and ignores the next task that the executor
     * would otherwise execute, if one is immediately available,
     * and then retries execution of task r, unless the executor
     * is shut down, in which case task r is instead discarded.
     * @param r the runnable task requested to be executed
     * @param e the executor attempting to execute this task
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
      if (!e.isShutdown()) {
        Runnable dropped = e.getQueue().poll();

        logOnSilentDrop(e);

        // mj:I think this is a bug, consider using a synchronous queue without any queued elements, which triggers
        // stack overflow, so simply add codes below to avoid it->
        if (dropped == null) {
          return;
        }
        // <-
        e.execute(r);
      }
    }
  }

  private static void logOnSilentDrop(ThreadPoolExecutor e) {
    if (e.getThreadFactory() instanceof NamedThreadFactory) {
      LOG.warn("Dropping task of {}", ((NamedThreadFactory)e.getThreadFactory()).getPrefix());
    }
  }

  private PoolExecutors() {
  }
}