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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Enhance the {@link java.util.concurrent.ExecutorService} to gain the ability to track lineage
 * relationships between threads
 * 
 * @author Jay Meng
 */
public class StfExecutorService implements ExecutorService {
  private final ExecutorService exec;

  @Override
  public void shutdown() {
    exec.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return exec.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return exec.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    exec.execute(StfRunnable.of(command));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return exec.submit(StfCallable.of(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return exec.submit(StfRunnable.of(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return exec.submit(StfRunnable.of(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return exec.invokeAll(enhance(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return exec.invokeAll(enhance(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return exec.invokeAny(enhance(tasks));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return exec.invokeAny(enhance(tasks), timeout, unit);
  }

  @Override
  public boolean isShutdown() {
    return exec.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return exec.isTerminated();
  }

  private <T> Collection<StfCallable<T>> enhance(Collection<? extends Callable<T>> tasks)
      throws UnsupportedOperationException {
    if (tasks instanceof ArrayList) {
      return tasks.stream().map(task -> StfCallable.of(task)).collect(Collectors.toList());
    } else if (tasks instanceof HashSet) {
      return tasks.stream().map(task -> StfCallable.of(task)).collect(Collectors.toSet());
    }
    throw new UnsupportedOperationException("Only 'ArrayList' and 'HashSet' enhancement are supported");
  }

  public StfExecutorService(ExecutorService exec) {
    this.exec = exec;
  }
}
