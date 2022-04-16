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

import com.alibaba.ttl.TtlRunnable;

/** @author Jay Meng */
public final class StfRunnable implements Runnable {
  private final TtlRunnable enhanced;

  public static StfRunnable of(Runnable runnable) {
    return new StfRunnable(TtlRunnable.get(runnable, false, true));// TODO mj:autorelease set 'false',also check
                                                                   // StfContext#removeTTL for the performance
  }

  @Override
  public void run() {
    enhanced.run();
  }

  StfRunnable(TtlRunnable enhanced) {
    this.enhanced = enhanced;
  }

}