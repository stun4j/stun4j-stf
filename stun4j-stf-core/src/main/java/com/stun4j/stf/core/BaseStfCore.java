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

import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;

/**
 * Base class for the core operations of Stf
 * @author JayMeng
 */
public abstract class BaseStfCore implements StfCore {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  protected final ExecutorService worker;

  @SuppressWarnings("unchecked")
  @Override
  public Long init(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    StfCall callee = newCallee(bizObjId, bizMethodName, typedArgs);
    Long newStfId = StfContext.newStfId();
    doInit(newStfId, callee);
    return newStfId;
  }

  @SuppressWarnings("unchecked")
  StfCall newCallee(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
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
      Exceptions.sneakyThrow(e, LOG, "The invoke of stf#{} error", stfId);
    }
  }

  protected abstract void doInit(Long newStfId, StfCall callee);

  public abstract boolean doForward(Long stfId);

  protected abstract boolean doTryLockStf(Long stfId, long lastUpAtMs);

  protected abstract boolean doMarkDone(Long stfId);

  protected abstract void doMarkDead(Long stfId);

  protected abstract boolean doReForward(Long stfId, int curRetryTimes);

  {
    worker = newWorkerOfStfCore();
  }
}