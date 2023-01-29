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

import static com.stun4j.stf.core.StfHelper.partialUpdateJobInfoWhenLocked;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfBatchable;
import com.stun4j.stf.core.StfMetaGroupEnum;
import com.stun4j.stf.core.support.actor.BaseActor;
import com.stun4j.stf.core.support.event.StfReceivedEvent;

/**
 * @author JayMeng
 */
class JobBatchLockAndRunActor extends BaseActor<StfReceivedEvent> {
  private final StfBatchable core;
  private final JobRunners runners;
  private final StfMetaGroupEnum metaGrp;
  private final int handleBatchSize;

  @Override
  protected void onMsgs(List<StfReceivedEvent> msgs) throws InterruptedException {
    int msgSize;
    if ((msgSize = msgs.size()) == 1) {
      StfReceivedEvent eve = msgs.get(0);
      Stf job = eve.getStf();
      int curTimeoutSecs = eve.getCurTimeoutSecs();

      long lockedAt;
      if ((lockedAt = core.fallbackToSingleLockStf(metaGrp, job, curTimeoutSecs)) <= 0) {
        return;
      }
      partialUpdateJobInfoWhenLocked(job, lockedAt, curTimeoutSecs);
      runners.execute(metaGrp, job);
      return;
    }
    int batchSize;
    int loop = msgSize % (batchSize = determineHandleBatchSize()) == 0 ? msgSize / batchSize : msgSize / batchSize + 1;
    if (LOG.isDebugEnabled()) {
      LOG.debug("[onMsgs] Loop:{} [msgSize={}, batchSize={}, metaGrp={}]", loop, msgSize, batchSize, metaGrp);
    }
    Iterator<StfReceivedEvent> iter = msgs.iterator();
    for (int i = 1; i <= loop; i++) {
      if (i == loop) {
        batchSize = msgSize - batchSize * (loop - 1);
      }

      List<Object[]> batchArgs = new ArrayList<>(batchSize);
      for (int j = 0; j < batchSize; j++) {
        StfReceivedEvent eve = iter.next();
        Stf job = eve.getStf();
        Integer curTimeoutSecs = eve.getCurTimeoutSecs();
        batchArgs.add(new Object[]{job, curTimeoutSecs, job.getId(), job.getRetryTimes(), job.getTimeoutAt()});
      }
      List<Stf> lockedJobs = core.batchLockStfs(metaGrp, batchArgs);
      for (Stf lockedJob : lockedJobs) {
        runners.execute(metaGrp, lockedJob);
      }
    }
  }

  private int determineHandleBatchSize() {
    // int availableThreadNum = runners.getAvailablePoolSize(jobGrp);
    // int enlargedThreadNum = availableThreadNum * batchMultiplyingFactor;
    // int loop = enlargedThreadNum % batchSize == 0 ? enlargedThreadNum / batchSize : enlargedThreadNum / batchSize +
    // 1;
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("[takeJobsAndRun] Loop:{} [availableThread={}, normalBatchSize={}, jobGrp={}]", loop,
    // availableThreadNum, batchSize, jobGrp);
    // }
    return handleBatchSize;
  }

  @Override
  public int getMailBoxMaxDrainNum() {
    return super.getMailBoxMaxDrainNum();
  }

  JobBatchLockAndRunActor(StfBatchable core, JobRunners runners, int baseCapacity, StfMetaGroupEnum metaGrp,
      int handleBatchSize) {
    super("stf-grp-" + metaGrp.nameLowerCase() + "-job-batch-lockrun-actor", baseCapacity);
    this.core = core;
    this.runners = runners;
    this.metaGrp = metaGrp;
    this.handleBatchSize = handleBatchSize;
  }

}
