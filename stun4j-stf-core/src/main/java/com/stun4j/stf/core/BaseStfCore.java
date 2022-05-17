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

import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;

/**
 * Base class for the core operations of Stf.
 * @author JayMeng
 */
abstract class BaseStfCore implements StfCore, StfBatchable, StfDelayQueueCore {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  protected final ExecutorService worker;

  @Override
  public Long newStf(String bizObjId, String bizMethodName, Integer timeoutSeconds,
      @SuppressWarnings("unchecked") Pair<?, Class<?>>... typedArgs) {
    int timeoutSecs = Optional.ofNullable(timeoutSeconds).orElse(StfConfigs.getActionTimeout(bizObjId, bizMethodName));
    StfCall callee = newCallee(bizObjId, bizMethodName, timeoutSecs, typedArgs);
    StfId newStfId = StfContext.newStfId(bizObjId, bizMethodName);
    Long idVal;
    doNewStf(idVal = newStfId.getValue(), callee, timeoutSecs);
    return idVal;
  }

  @Override
  public StfCall newCallee(String bizObjId, String bizMethodName, Integer timeoutSeconds,
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

  @Override
  public List<Stf> batchLockStfs(List<Object[]> preBatchArgs) {
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
    int[] res = doBatchLockStfs(batchArgs);
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
      Exceptions.sneakyThrow(e, LOG, "The invoke of stf#{} error", stfId);
    }
  }

  @Override
  public abstract void doNewStf(Long newStfId, StfCall callee, int timeoutSecs);

  @Deprecated
  protected abstract boolean doForward(Long stfId);

  @Deprecated
  protected abstract boolean doReForward(Long stfId, int curRetryTimes);

  protected abstract boolean doLockStf(Long stfId, int timeoutSecs, int curRetryTimes);

  protected abstract int[] doBatchLockStfs(List<Object[]> batchArgs);

  @Override
  public boolean fallbackToSingleMarkDone(Long stfId) {
    return doMarkDone(stfId, false);
  }

  protected abstract boolean doMarkDone(Long stfId, boolean batch);

  protected abstract void doMarkDead(Long stfId);

  {
    worker = newWorkerOfStfCore();
  }
}