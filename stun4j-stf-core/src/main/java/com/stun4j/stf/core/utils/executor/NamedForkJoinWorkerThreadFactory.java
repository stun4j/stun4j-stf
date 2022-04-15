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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/** @author Jay Meng */
public class NamedForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
  private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

  private final AtomicInteger threadNum = new AtomicInteger(1);

  private final String prefix;

  private final boolean daemon;

  public static NamedForkJoinWorkerThreadFactory of(String prefix) {
    return new NamedForkJoinWorkerThreadFactory(prefix, false);
  }

  @Override
  public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
    String name = prefix + threadNum.getAndIncrement();
    ForkJoinWorkerThread thread = new NamedForkJoinWorkerThread(pool, name);
    thread.setDaemon(daemon);
    return thread;
  }

  public NamedForkJoinWorkerThreadFactory(String prefix, boolean daemon) {
    this.prefix = prefix + "-thread-";
    this.daemon = daemon;
  }

  NamedForkJoinWorkerThreadFactory() {
    this("pool-" + POOL_SEQ.getAndIncrement(), false);
  }

}