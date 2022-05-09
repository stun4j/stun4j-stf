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
package com.stun4j.stf.core.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Lifecycle management
 * <ul>
 * <li>Ensure start or shutdown only once</li>
 * <li>Swallow any error during start or shutdown</li>
 * </ul>
 * @author Jay Meng
 */
public class BaseLifeCycle implements LifeCycle {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private AtomicBoolean startOnce = new AtomicBoolean(false);
  private AtomicBoolean shutdownOnce = new AtomicBoolean(false);

  @Override
  public final void start() {
    if (!startOnce.compareAndSet(false, true)) {
      return;
    }
    try {
      doStart();
    } catch (Throwable e) {
      LOG.error("Unexpected start error, now try shutting down", e);
      // TODO mj:terminate current jvm in the very-beginning,instead of throwing error(support more strategy)
      shutdown();
      System.exit(-1);// TODO mj:Not good!!!
    }

  }

  @Override
  public final void shutdown() {
    if (!shutdownOnce.compareAndSet(false, true)) {
      return;
    }
    try {
      doShutdown();
    } catch (Throwable e) {
      LOG.error("Unexpected shutdown error", e);
    }
  }

  protected void doStart() {
  }

  protected void doShutdown() {
  }

}