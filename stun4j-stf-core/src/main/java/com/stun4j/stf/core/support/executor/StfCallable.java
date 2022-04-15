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

import java.util.concurrent.Callable;

import com.alibaba.ttl.TtlCallable;

/** @author Jay Meng */
public final class StfCallable<V> implements Callable<V> {
  private final TtlCallable<V> enhanced;

  public static <V> StfCallable<V> of(Callable<V> runnable) {
    return new StfCallable<>(TtlCallable.get(runnable, false, true));// TODO mj:autorelease set 'false',also check
                                                                     // StfContext#removeTTL for the performance
  }

  @Override
  public V call() throws Exception {
    return enhanced.call();
  }

  StfCallable(TtlCallable<V> enhanced) {
    this.enhanced = enhanced;
  }

}