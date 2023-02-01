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

import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.StfConsts.WITH_MS_DATE_FMT;
import static com.stun4j.stf.core.StfMetaGroup.CORE;
import static com.stun4j.stf.core.job.JobConsts.retryBehaviorByPattern;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCall;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfMetaGroup;

/**
 * @author Jay Meng
 */
class JobRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JobRunner.class);
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;

  private final Map<Integer, Integer> retryBehavior;
  private final LoadingCache<Integer, Map<Integer, Integer>> cachedRetryBehavior;
  private static JobRunner _instance;

  Pair<Boolean, Integer> checkWhetherTheJobCanRun(StfMetaGroup metaGrp, Stf lockingJob, StfCore stfc) {
    int minTimeoutSecs;
    Map<Integer, Integer> retryBehav = _instance
        .determineJobRetryBehavior(minTimeoutSecs = lockingJob.getTimeoutSecs());
    Pair<Boolean, Integer> canRun = doCheckAndMarkJobDeadIfNecessary(metaGrp, lockingJob, stfc, retryBehav);
    if (!canRun.getKey()) {
      return Pair.of(false, null);
    }
    int lastRetryTimes = lockingJob.getRetryTimes();
    int curRetryTimes = lastRetryTimes + 1;
    Integer lastTimeoutSecs = Optional.ofNullable(retryBehav.get(lastRetryTimes)).orElse(minTimeoutSecs);
    Integer curTimeoutSecs = Optional.ofNullable(retryBehav.get(curRetryTimes)).orElse(lastTimeoutSecs);
    return Pair.of(true, curTimeoutSecs < minTimeoutSecs ? minTimeoutSecs : curTimeoutSecs);
  }

  static void doHandleTimeoutJob(StfMetaGroup metaGrp, Stf lockedJob, StfCore stfc) {
    Map<Integer, Integer> retryBehav = _instance.determineJobRetryBehavior(lockedJob.getTimeoutSecs());

    logTriggerInformation(metaGrp, lockedJob, retryBehav);

    StfCall callee = null;
    if (metaGrp == CORE) {
      callee = lockedJob.toCallee();
    }

    stfc.forward(metaGrp, lockedJob, callee,
        true);/*-TODO mj:1.consider set to false or we may encounter excessiving threadpool(or pre-choose tp via meta-gloabl?) 2.extract cpu-related jobs here(current choice)*/
  }

  private static Pair<Boolean, Integer> doCheckAndMarkJobDeadIfNecessary(StfMetaGroup metaGrp, Stf job, StfCore stfc,
      Map<Integer, Integer> retryBehav) {
    int retryMaxTimes = retryBehav.size();
    int lastRetryTimes = job.getRetryTimes();
    Long jobId = job.getId();
    if (lastRetryTimes >= retryMaxTimes) {
      LOG.info("Exceeded retryMaxTimes, now marking the job#{} dead [lastRetryTimes={}, retryMaxTimes={}]", jobId,
          lastRetryTimes, retryMaxTimes);
      stfc.markDead(metaGrp, jobId, true);
      return Pair.of(false, null);
    }
    return Pair.of(true, null);
  }

  private static void logTriggerInformation(StfMetaGroup metaGrp, Stf lockedJob, Map<Integer, Integer> retryBehav) {
    Long jobId = lockedJob.getId();
    int curRetryTimes = lockedJob.getRetryTimes();
    if (LOG.isDebugEnabled()) {
      long lockedAt = lockedJob.getUpAt();
      Date now = new Date();
      String title = metaGrp == CORE ? "Retring stf-job"
          : (curRetryTimes == 1 ? "Triggering stf-delay-job" : "Retring to trigger stf-delay-job");
      String tpl = "{}#{} [curTime={}, curRetryTimes={}, lockedTime={}, curTimeoutTime={}, lastTimeoutTime={}]";
      LOG.debug(tpl, title, jobId, WITH_MS_DATE_FMT.format(now), metaGrp == CORE ? curRetryTimes : "n/a",
          WITH_MS_DATE_FMT.format(lockedAt), WITH_MS_DATE_FMT.format(lockedAt + lockedJob.getTimeoutSecs() * 1000),
          WITH_MS_DATE_FMT.format(lockedJob.getTimeoutAt()));
      return;
    }
    if (LOG.isInfoEnabled()) {
      if (metaGrp == CORE) {
        LOG.info("Retring stf-job#{}", jobId);
        return;
      }
      if (curRetryTimes == 1) {
        LOG.info("Triggering stf-delay-job#{}", jobId);
      } else {
        LOG.info("Retring to trigger stf-delay-job#{}", jobId);
      }
      return;
    }
  }

  synchronized static JobRunner init(Map<Integer, Integer> retryBehavior) {
    if (_instance != null) {
      return _instance;
    }
    return _instance = new JobRunner(retryBehavior);
  }

  Map<Integer, Integer> determineJobRetryBehavior(int timeoutSecs) {
    if (this.retryBehavior != null) {
      return this.retryBehavior;
    }
    if (timeoutSecs == DFT_JOB_TIMEOUT_SECONDS) {
      return DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;
    }
    return cachedRetryBehavior.getUnchecked(timeoutSecs);
  }

  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = retryBehaviorByPattern(DFT_JOB_TIMEOUT_SECONDS);
  }

  private JobRunner(Map<Integer, Integer> retryBehavior) {
    this.retryBehavior = retryBehavior;

    CacheLoader<Integer, Map<Integer, Integer>> loader = new CacheLoader<Integer, Map<Integer, Integer>>() {
      public Map<Integer, Integer> load(Integer timeoutSecs) throws Exception {
        Map<Integer, Integer> map = retryBehaviorByPattern(timeoutSecs);
        return map;
      }
    };
    this.cachedRetryBehavior = CacheBuilder.newBuilder().maximumSize(100).weakKeys().weakValues().build(loader);
  }

}