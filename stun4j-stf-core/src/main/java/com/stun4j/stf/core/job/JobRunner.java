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

import static com.stun4j.stf.core.StfConsts.DFT_DATE_FMT;
import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.job.JobConsts.generateRetryBehaviorByPattern;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCall;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.JsonHelper;

/**
 * @author Jay Meng
 */
public class JobRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JobRunner.class);
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;

  private final Map<Integer, Integer> retryBehavior;
  private final LoadingCache<Integer, Map<Integer, Integer>> cachedRetryBehavior;
  private static JobRunner _instance;

  static void doHandleTimeoutJob(Stf job, StfCore stfCore) {
    // Determine job retry behavior
    Map<Integer, Integer> retryBehavior = _instance.determineJobRetryBehavior(calculateJobTimeoutSeconds(job));
    int retryMaxTimes = retryBehavior.size();

    // The job is declared dead if its retry-times exceeds the upper limit
    int lastRetryTimes = job.getRetryTimes();
    Long jobId = job.getId();
    if (lastRetryTimes >= retryMaxTimes) {
      stfCore.markDead(jobId, false);
      return;
    }
    // Calculate trigger time,if the time does not arrive, no execution is performed
    int expectedRetryTimes = lastRetryTimes == 0 ? 1 : lastRetryTimes + 1;
    Integer nextIntervalSecondsAllowReSend = retryBehavior.get(expectedRetryTimes);
    if (nextIntervalSecondsAllowReSend == null || job.getUpAt() <= 0) {
      LOG.error("Unexpected stf-job state, retrying was cancelled [expectedRetryTimes={}] |error: '{}'",
          expectedRetryTimes, "No 'nextIntervalSecondsAllowReSend' or missing job last update-time");
      return;
    }
    Date expectedTriggerTime = DateUtils.addSeconds(new Date(job.getUpAt()), nextIntervalSecondsAllowReSend);
    Date now = new Date();
    if (now.compareTo(expectedTriggerTime) < 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Retring stf-job#{} cancelled, the trigger time does not arrive [curTime={}, expectedRetryTimes={}, expectedTriggerTime={}]",
            jobId, now, expectedRetryTimes, expectedTriggerTime);
      }
      return;
    }
    logTriggerInformation(job, expectedRetryTimes, expectedTriggerTime, now, retryBehavior);
    // Retry the job
    String calleeInfo = null;
    Object[] methodArgs = null;
    try {
      StfCall callee = JsonHelper.fromJson(job.getBody(), StfCall.class);
      calleeInfo = toCallStringOf(callee);
      methodArgs = callee.getArgs();
    } catch (Throwable e) {
      // This will definitely result in an invoke error,just to increase the retry times
      LOG.error("Parsing calleeInfo of stf-job#{} error", jobId, e);
    }
    stfCore.reForward(jobId, lastRetryTimes, calleeInfo, true, methodArgs);
  }

  private static int calculateJobTimeoutSeconds(Stf job) {
    return (int)((job.getTimeoutAt() - job.getUpAt()) / 1000);// TODO mj:wot if negative?
  }

  private static void logTriggerInformation(Stf job, int curRetryTimes, Date curTriggerTime, Date now,
      Map<Integer, Integer> retryBehavior) {
    Integer nextIntervalSecondsAllowReSend = retryBehavior.get(curRetryTimes + 1);
    Date nextTriggerTime = nextIntervalSecondsAllowReSend != null
        ? DateUtils.addSeconds(now, nextIntervalSecondsAllowReSend)
        : null;
    LOG.info(
        "Retring stf-job#{} [curTime={}, expectedRetryTimes={}, expectedTriggerTime={}, lastTriggerTime={}, nextTriggerTime={}]",
        job.getId(), DFT_DATE_FMT.format(now), curRetryTimes, DFT_DATE_FMT.format(curTriggerTime),
        DFT_DATE_FMT.format(new Date(job.getUpAt())), DFT_DATE_FMT.format(nextTriggerTime));
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

  synchronized static JobRunner instance(Map<Integer, Integer> retryBehavior) {
    if (_instance != null) {
      return _instance;
    }
    return _instance = new JobRunner(retryBehavior);
  }

  private Map<Integer, Integer> determineJobRetryBehavior(int timeoutSeconds) {
    if (this.retryBehavior != null) {
      return this.retryBehavior;
    }
    if (timeoutSeconds == DFT_JOB_TIMEOUT_SECONDS) {
      return DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS;
    }
    return cachedRetryBehavior.getUnchecked(timeoutSeconds);
  }

  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = generateRetryBehaviorByPattern(DFT_JOB_TIMEOUT_SECONDS);
  }

  private JobRunner(Map<Integer, Integer> retryBehavior) {
    this.retryBehavior = retryBehavior;

    CacheLoader<Integer, Map<Integer, Integer>> loader = new CacheLoader<Integer, Map<Integer, Integer>>() {
      public Map<Integer, Integer> load(Integer timeoutSeconds) throws Exception {
        Map<Integer, Integer> map = generateRetryBehaviorByPattern(timeoutSeconds);
        return map;
      }
    };
    this.cachedRetryBehavior = CacheBuilder.newBuilder().maximumSize(100).weakKeys().weakValues().build(loader);
  }

}