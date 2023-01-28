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
import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
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
import com.stun4j.stf.core.StfMetaGroupEnum;
import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * @author Jay Meng
 */
class JobRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JobRunner.class);
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;

  private final Map<Integer, Integer> retryBehavior;
  private final LoadingCache<Integer, Map<Integer, Integer>> cachedRetryBehavior;
  private static JobRunner _instance;

  Pair<Boolean, Integer> checkWhetherTheJobCanRun(String jobGrp, Stf lockingJob, StfCore stfCore) {
    int minTimeoutSecs;
    Map<Integer, Integer> retryBehav = _instance
        .determineJobRetryBehavior(minTimeoutSecs = lockingJob.getTimeoutSecs());
    StfMetaGroupEnum metaGrp = H.determineMetaGroupBy(jobGrp);
    Pair<Boolean, Integer> canRun = doCheckAndMarkJobDeadIfNecessary(metaGrp, lockingJob, stfCore, retryBehav);
    if (!canRun.getKey()) {
      return Pair.of(false, null);
    }
    int lastRetryTimes = lockingJob.getRetryTimes();
    int curRetryTimes = lastRetryTimes + 1;
    Integer lastTimeoutSecs = Optional.ofNullable(retryBehav.get(lastRetryTimes)).orElse(minTimeoutSecs);
    Integer curTimeoutSecs = Optional.ofNullable(retryBehav.get(curRetryTimes)).orElse(lastTimeoutSecs);
    return Pair.of(true, curTimeoutSecs < minTimeoutSecs ? minTimeoutSecs : curTimeoutSecs);
  }

  static void doHandleTimeoutJob(StfMetaGroupEnum metaGrp, Stf lockedJob, StfCore stfCore) {
    Map<Integer, Integer> retryBehav = _instance.determineJobRetryBehavior(lockedJob.getTimeoutSecs());

    logTriggerInformation(metaGrp, lockedJob, retryBehav);

    // Retry the job
    Long jobId = lockedJob.getId();
    String calleeInfo = null;
    Object[] methodArgs = null;
    try {
      StfCall callee = JsonHelper.fromJson(lockedJob.getBody(), StfCall.class);
      calleeInfo = toCallStringOf(callee);
      methodArgs = callee.getArgs();
    } catch (Throwable t) {
      // This will definitely result in an invoke error,so fail-fast here
      Exceptions.sneakyThrow(t, LOG, "An error occurred while pre parsing calleeInfo of stf-job#{}", jobId);
    }
    stfCore.reForward(metaGrp, jobId, lockedJob.getRetryTimes(), calleeInfo, true, methodArgs);
  }

  private static Pair<Boolean, Integer> doCheckAndMarkJobDeadIfNecessary(StfMetaGroupEnum metaGrp, Stf job,
      StfCore stfCore, Map<Integer, Integer> retryBehav) {
    int retryMaxTimes = retryBehav.size();
    int lastRetryTimes = job.getRetryTimes();
    Long jobId = job.getId();
    if (lastRetryTimes >= retryMaxTimes) {
      stfCore.markDead(metaGrp, jobId, true);
      return Pair.of(false, null);
    }
    return Pair.of(true, null);
  }

  private static void logTriggerInformation(StfMetaGroupEnum metaGrp, Stf lockedJob, Map<Integer, Integer> retryBehav) {
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

  private static String toCallStringOf(StfCall call) {
    String type = call.getType();
    String bizObjId = call.getBizObjId();
    String method = call.getMethod();
    StringBuilder builder = new StringBuilder(type);
    builder.append(":").append(bizObjId).append(".").append(method);
    String calleeInfo = builder.toString();
    return calleeInfo;
  }

  synchronized static JobRunner init(Map<Integer, Integer> retryBehavior) {
    if (_instance != null) {
      return _instance;
    }
    return _instance = new JobRunner(retryBehavior);
  }

  Map<Integer, Integer> determineJobRetryBehavior(int timeoutSeconds) {
    if (this.retryBehavior != null) {
      return this.retryBehavior;
    }
    if (timeoutSeconds == DFT_JOB_TIMEOUT_SECONDS) {
      return DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;
    }
    return cachedRetryBehavior.getUnchecked(timeoutSeconds);
  }

  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = retryBehaviorByPattern(DFT_JOB_TIMEOUT_SECONDS);
  }

  private JobRunner(Map<Integer, Integer> retryBehavior) {
    this.retryBehavior = retryBehavior;

    CacheLoader<Integer, Map<Integer, Integer>> loader = new CacheLoader<Integer, Map<Integer, Integer>>() {
      public Map<Integer, Integer> load(Integer timeoutSeconds) throws Exception {
        Map<Integer, Integer> map = retryBehaviorByPattern(timeoutSeconds);
        return map;
      }
    };
    this.cachedRetryBehavior = CacheBuilder.newBuilder().maximumSize(100).weakKeys().weakValues().build(loader);
  }

}