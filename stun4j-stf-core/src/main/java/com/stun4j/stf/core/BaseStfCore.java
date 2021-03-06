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

import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfStfCore;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;

/**
 * Base class for the core operations of Stf.
 * @author JayMeng
 */
abstract class BaseStfCore implements StfCore, StfDelayQueueCore {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  protected final ExecutorService worker;
  private StfRunModeEnum runMode;
  private boolean delayQueueEnabled;

  @Override
  public Long newStf(String bizObjId, String bizMethodName, Integer timeoutSeconds,
      @SuppressWarnings("unchecked") Pair<?, Class<?>>... typedArgs) {
    int timeoutSecs = Optional.ofNullable(timeoutSeconds).orElse(StfConfigs.getActionTimeout(bizObjId, bizMethodName));
    StfCall callee = newCallee(bizObjId, bizMethodName, typedArgs);
    StfId newStfId = StfContext.newStfId(bizObjId, bizMethodName);
    Long idVal;
    doNewStf(idVal = newStfId.getValue(), callee, timeoutSecs);
    return idVal;
  }

  @Override
  public Long newStfDelay(StfCall callee, int timeoutSeconds, int delaySeconds) {
    Long stfDelayId;
    doNewStfDelay(stfDelayId = LocalGuid.instance().next(), callee, timeoutSeconds, delaySeconds);
    return stfDelayId;
  }

  @Override
  public List<Stf> batchLockStfs(String jobGrp, List<Object[]> preBatchArgs) {
    long now = System.currentTimeMillis();
    List<Stf> jobs = new ArrayList<>();
    List<Object[]> batchArgs = preBatchArgs.stream().map(arg -> {
      int timeoutSeconds = (Integer)arg[1];
      Stf job = (Stf)arg[0];
      jobs.add(job);
      arg[0] = now + timeoutSeconds * 1000;
      arg[1] = now;
      return arg;
    }).collect(Collectors.toList());
    int finalBatchSize = batchArgs.size();
    StfMetaGroupEnum metaGrp = H.determineMetaGroupBy(jobGrp);
    int[] res = doBatchLockStfs(metaGrp, batchArgs);
    if (LOG.isDebugEnabled()) {
      LOG.debug("The batch-lock image of stfs: {}", res);
    }
    if (res == null || ArrayUtils.indexOf(res, 1) < 0) {
      return null;// TODO mj:Somewhat rude here
    }
    res = ArrayUtils.subarray(res, 0, finalBatchSize);
    int idx = 0;
    for (ListIterator<Stf> iter = jobs.listIterator(); iter.hasNext();) {
      iter.next();
      if (res[idx++] != 1) {
        iter.remove();
      }
    }
    return jobs;
  }

  StfCall newCallee(String bizObjId, String bizMethodName,
      @SuppressWarnings("unchecked") Pair<?, Class<?>>... typedArgs) {
    if (ArrayUtils.isNotEmpty(typedArgs)) {
      StfCall callee = StfCall.ofInJvm(bizObjId, bizMethodName, typedArgs.length);
      int argIdx = 0;
      for (Pair<?, Class<?>> arg : typedArgs) {
        Class<?> argType = arg.getRight();
        Object argVal = arg.getLeft();
        if (Primitives.isPrimitive(argType)) {
          callee.withPrimitiveArg(argIdx++, argVal, argType);
        } else {
          callee.withArg(argIdx++, argVal);
        }
      }
      return callee;
    }
    return StfCall.ofInJvm(bizObjId, bizMethodName);
  }

  protected boolean checkFail(Long stfId) {
    if (stfId == null || stfId <= 0) {
      LOG.error("The id of stf#{} must be greater than 0", stfId);
      return true;
    }
    return false;
  }

  protected void invokeCall(Long stfId, String callInfo, Object... callMethodArgs) {
    try {
      StfInvoker.invoke(stfId, callInfo, callMethodArgs);
    } catch (Throwable e) {
      Exceptions.sneakyThrow(e, LOG, "An error occurred while invoking stf#{}", stfId);
    }
  }

  protected abstract void doNewStf(Long newStfId, StfCall callee, int timeoutSeconds);

  protected abstract void doNewStfDelay(Long delayStfId, StfCall callee, int timeoutSeconds, int delaySeconds);

  protected abstract boolean doLockStf(StfMetaGroupEnum metaGrp, Long stfId, int timeoutSecs, int curRetryTimes);

  protected abstract int[] doBatchLockStfs(StfMetaGroupEnum metaGrp, List<Object[]> batchArgs);

  @Override
  public boolean fallbackToSingleMarkDone(StfMetaGroupEnum metaGrp, Long stfId) {
    return doMarkDone(metaGrp, stfId, false);
  }

  protected abstract boolean doMarkDone(StfMetaGroupEnum metaGrp, Long stfId, boolean batch);

  protected abstract void doMarkDead(StfMetaGroupEnum metaGrp, Long stfId);

  protected abstract boolean doDelayTransfer(Long delayStfId);

  {
    worker = newWorkerOfStfCore();
  }

  @Override
  public StfRunModeEnum getRunMode() {
    return runMode;
  }

  @Override
  public StfCore withRunMode(StfRunModeEnum runMode) {
    this.runMode = runMode;
    return this;
  }

  @Override
  public boolean isDelayQueueEnabled() {
    return delayQueueEnabled;
  }

  @Override
  public StfDelayQueueCore withDelayQueueEnabled(boolean enabled) {
    this.delayQueueEnabled = enabled;
    return this;
  }
}