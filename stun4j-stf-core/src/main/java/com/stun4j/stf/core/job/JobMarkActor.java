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

import static com.stun4j.stf.core.StfMetaGroup.CORE;

import com.google.common.eventbus.Subscribe;
import com.stun4j.stf.core.StfBatchable;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.support.event.StfDoneEvent;

/**
 * An ad-hoc actor used to batch commit the results of stf-jobs.
 * 
 * @author Jay Meng
 */
class JobMarkActor extends BaseJobBatchMarkActor {

  @Subscribe
  void on(StfDoneEvent evt) {
    super.tell(evt.getStfId());
  }

  JobMarkActor(StfBatchable stfc, int baseCapacity) {
    super("stf-grp-" + StfMetaGroup.CORE.nameLowerCase() + "-job-mark-actor", baseCapacity, stfc);
  }

  @Override
  protected StfMetaGroup metaGroup() {
    return CORE;
  }
}
