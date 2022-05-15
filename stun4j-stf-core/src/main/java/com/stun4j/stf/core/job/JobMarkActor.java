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

import com.google.common.eventbus.Subscribe;
import com.stun4j.stf.core.BaseStfCore;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.StfDoneEvent;
import com.stun4j.stf.core.support.actor.BaseActor;

/**
 * An ad-hoc actor used to batch commit the results of stf-jobs
 * @author Jay Meng
 */
public class JobMarkActor extends BaseActor<Long> {
  private final StfCore stfCore;

  @Subscribe
  public void onStfDone(StfDoneEvent eve) {
    super.tell(eve.getStfId());
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onMsgs(List<Long> stfIds) {
    if (stfIds.size() == 1) {
      Long stfId = stfIds.get(0);
      ((BaseStfCore)stfCore).doMarkDone(stfId, false);
      return;
    }
    long now = System.currentTimeMillis();
    List<Object[]> stfIdsInfo = stfIds.stream().map(stfId -> {
      return new Object[]{now, stfId};
    }).collect(Collectors.toList());
    stfCore.batchMarkDone(stfIdsInfo);
    // TODO mj:log stuff
  }

  public JobMarkActor(StfCore stfCore, int baseCapacity) {
    super("stf-job-mark-actor", baseCapacity);
    this.stfCore = stfCore;
  }
}
