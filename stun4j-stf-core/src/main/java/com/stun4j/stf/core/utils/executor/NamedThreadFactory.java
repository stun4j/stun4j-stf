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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** @author Jay Meng */
public class NamedThreadFactory implements ThreadFactory {
  private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

  private final AtomicInteger threadNum = new AtomicInteger(1);

  private final String prefix;

  private final boolean daemon;

  private final ThreadGroup group;

  public static NamedThreadFactory of(String prefix) {
    return new NamedThreadFactory(prefix, false);
  }

  public Thread newThread(Runnable runnable) {
    String name = prefix + threadNum.getAndIncrement();
    Thread thread = new Thread(group, runnable, name, 0);
    thread.setDaemon(daemon);
    return thread;
  }

  NamedThreadFactory() {
    this("pool-" + POOL_SEQ.getAndIncrement(), false);
  }

  public NamedThreadFactory(String prefix, boolean daemon) {
    this.prefix = prefix + "-thread-";
    this.daemon = daemon;
    SecurityManager s = System.getSecurityManager();
    group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
  }

  public ThreadGroup getThreadGroup() {
    return group;
  }

  public String getPrefix() {
    return prefix;
  }

}