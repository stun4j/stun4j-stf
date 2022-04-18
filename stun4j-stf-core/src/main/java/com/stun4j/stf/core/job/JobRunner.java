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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCall;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.JsonHelper;

/**
 * @author Jay Meng
 */
public class JobRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JobRunner.class);
  private static final Map<Integer, Integer> DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS = new HashMap<>();

  static {
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(1, 0);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(2, 1 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(3, 2 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(4, 5 * 60);
    DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS.put(5, 15 * 60);
  }

  private final Map<Integer, Integer> retryIntervalSeconds;
  private final int retryMaxTimes;
  private static JobRunner _instance;

  static void doHandleTimeoutJob(Stf job, StfCore stfCore) {
    // the job is declared dead if its retry-times exceeds the upper limit
    int lastRetryTimes = job.getRetryTimes();
    Long jobId = job.getId();
    if (lastRetryTimes >= _instance.retryMaxTimes) {
      stfCore.markDead(jobId, false);
      return;
    }
    // calculate trigger time,if the time does not arrive, no execution is performed
    int expectedRetryTimes = lastRetryTimes == 0 ? 1 : lastRetryTimes + 1;
    Integer nextIntervalSecondsAllowReSend = _instance.retryIntervalSeconds.get(expectedRetryTimes);
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
    logTriggerInformation(job, expectedRetryTimes, expectedTriggerTime, now);
    // retry the job
    String calleeInfo = null;
    Object[] methodArgs = null;
    try {
      StfCall callee = JsonHelper.fromJson(job.getBody(), StfCall.class);
      calleeInfo = toCallStringOf(callee);
      methodArgs = callee.getArgs();
    } catch (Throwable e) {
      // this will definitely result in an invoke error,just to increase the retry times
      LOG.error("Parsing calleeInfo of stf-job#{} error", jobId, e);
    }
    stfCore.reForward(jobId, lastRetryTimes, calleeInfo, true, methodArgs);
  }

  private static void logTriggerInformation(Stf job, int curRetryTimes, Date curTriggerTime, Date now) {
    Integer nextIntervalSecondsAllowReSend = _instance.retryIntervalSeconds.get(curRetryTimes + 1);
    Date nextTriggerTime = nextIntervalSecondsAllowReSend != null
        ? DateUtils.addSeconds(now, nextIntervalSecondsAllowReSend)
        : null;
    LOG.info(
        "Retring stf-job#{} [curTime={}, expectedRetryTimes={}, expectedTriggerTime={}, lastTriggerTime={}, nextTriggerTime={}]",
        job.getId(), now, curRetryTimes, curTriggerTime, new Date(job.getUpAt()), nextTriggerTime);
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

  synchronized static JobRunner instance() {
    return instance(null);
  }

  synchronized static JobRunner instance(Map<Integer, Integer> retryIntervalSeconds) {
    if (_instance != null) {
      return _instance;
    }
    return _instance = new JobRunner(
        Optional.ofNullable(retryIntervalSeconds).orElse(new HashMap<>(DFT_FIXED_JOB_RETRY_INTERVAL_SECONDS)));
  }

  private JobRunner(Map<Integer, Integer> retryIntervalSeconds) {
    this.retryIntervalSeconds = retryIntervalSeconds;
    this.retryMaxTimes = retryIntervalSeconds.size();
  }

}