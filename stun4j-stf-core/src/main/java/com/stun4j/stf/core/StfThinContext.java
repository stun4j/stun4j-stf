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
package com.stun4j.stf.core;

/**
 * A thin context for directly committing Stf
 * <p>
 * For example, it is typically used by MQ-consumers, remote service providers, and so on.
 * @author Jay Meng
 */
public final class StfThinContext {

  static StfCore _core;

  public static void directCommitLastDone(Long laStfId) {
    directCommitLastDone(laStfId, true);
  }

  public static void directCommitLastDone(Long laStfId, boolean async) {
    _core.markDone(StfMetaGroup.CORE, laStfId, async);
  }
}
