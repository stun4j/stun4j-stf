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

import java.util.List;
import java.util.stream.Collectors;

import com.stun4j.stf.core.StfBatchable;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.support.actor.BaseActor;

/**
 * Base class helps job batch commit
 * @author Jay Meng
 */
abstract class BaseJobBatchMarkActor extends BaseActor<Long> {
  private final StfBatchable core;

  @Override
  protected void onMsgs(List<Long> stfIds) throws InterruptedException {
    if (stfIds.size() == 1) {
      Long stfId = stfIds.get(0);
      core.fallbackToSingleMarkDone(metaGroup(), stfId);
      return;
    }
    long now = System.currentTimeMillis();
    List<Object[]> stfIdsInfo = stfIds.stream().map(stfId -> {
      return new Object[]{now, stfId};
    }).collect(Collectors.toList());
    core.batchMarkDone(metaGroup(), stfIdsInfo);
  }

  BaseJobBatchMarkActor(String name, int baseCapacity, StfBatchable core) {
    super(name, baseCapacity);
    this.core = core;
  }

  protected abstract StfMetaGroup metaGroup();
}
