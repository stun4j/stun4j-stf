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
import static com.stun4j.stf.core.StfHelper.partialUpdateJobInfoWhenLocked;
import static com.stun4j.stf.core.StfMetaGroup.CORE;
import static com.stun4j.stf.core.StfMetaGroup.DELAY;
import static com.stun4j.stf.core.support.executor.StfInternalExecutors.newWorkerOfStfCore;
import static org.apache.commons.lang3.tuple.Pair.of;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.support.CompressAlgorithm;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.consumers.BaseConsumer;
import com.stun4j.stf.core.utils.consumers.PairConsumer;
import com.stun4j.stf.core.utils.consumers.QuadruConsumer;
import com.stun4j.stf.core.utils.consumers.SextuConsumer;
import com.stun4j.stf.core.utils.consumers.TriConsumer;

/**
 * Base class for the core operations of Stf.
 * @author JayMeng
 */
abstract class BaseStfCore implements StfCore, StfDelayQueueCore {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  protected final ExecutorService worker;

  protected final SextuConsumer<Pair<StfMetaGroup, Long>, Stf, StfCall, Boolean, Boolean, BaseConsumer<StfMetaGroup>> coreFn;
  protected final QuadruConsumer<StfMetaGroup, Long/*-TODO mj:Bad design just to distinguish it from another generic 'tri'*/, Stf, StfCall/*-TODO mj:Bad design just for the special pre-eval purpose*/> forward;
  protected final TriConsumer<StfMetaGroup, Long, Boolean> markDone;
  protected final PairConsumer<StfMetaGroup, Long> markDead;

  private StfRunMode runMode;
  private boolean delayQueueEnabled;

  @Override
  public Long newStf(String bizObjId, String bizMethodName, Integer timeoutSeconds,
      @SuppressWarnings("unchecked") Pair<?, Class<?>>... typedArgs) {
    int timeoutSecs = Optional.ofNullable(timeoutSeconds).orElse(StfConfigs.getActionTimeout(bizObjId, bizMethodName));
    StfCall callee = StfCall.newCallee(CORE, bizObjId, bizMethodName, typedArgs);
    StfId stfId = StfContext.newStfId(bizObjId, bizMethodName);
    Long idVal;
    doNewStf(idVal = stfId.getValue(), callee, timeoutSecs);
    return idVal;
  }

  @Override
  public Long newDelayStf(StfCall callee, int timeoutSecs, int delaySecs) {
    Long stfId;
    doNewDelayStf(stfId = LocalGuid.instance().next(), callee, timeoutSecs, delaySecs);
    return stfId;
  }

  @Override
  public List<Stf> batchLockStfs(StfMetaGroup metaGrp, List<Object[]> preBatchArgs) {
    List<Stf> jobs = new ArrayList<>();
    long lockedAt = System.currentTimeMillis();
    List<Object[]> batchArgs = preBatchArgs.stream().map(arg -> {
      Stf job = (Stf)arg[0];
      int dynaTimeoutSecs = (Integer)arg[1];
      jobs.add(job);
      // a shallow clone to reduce side-effect modifying preBatchArgs,also for grabbing 'dynaTimeoutSecs' again
      arg = arg.clone();
      arg[0] = lockedAt + dynaTimeoutSecs * 1000;
      arg[1] = lockedAt;
      return arg;
    }).collect(Collectors.toList());
    int finalBatchSize = batchArgs.size();
    int[] res = doBatchLockStfs(metaGrp, batchArgs);
    if (LOG.isDebugEnabled()) {
      LOG.debug("The batch-lock image of stfs: {}", res);
    }
    if (res == null || ArrayUtils.indexOf(res, 1) < 0) {
      return Collections.emptyList();
    }
    res = ArrayUtils.subarray(res, 0, finalBatchSize);
    int idx = 0;
    for (ListIterator<Stf> iter = jobs.listIterator(); iter.hasNext();) {
      Stf job = iter.next();
      //@formatter:off
      // not locked
      if (res[idx] != 1) {
        iter.remove();
      // locked
      } else {
        int dynaTimeoutSecs = (Integer)preBatchArgs.get(idx)[1];
        partialUpdateJobInfoWhenLocked(job, lockedAt, dynaTimeoutSecs);
      }
      //@formatter:on
      idx++;
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

  protected void invokeCall(Stf lockedStf, StfCall calleePreEval) {
    Long stfId = lockedStf.getId();
    try {
      calleePreEval = Optional.ofNullable(calleePreEval).orElse(lockedStf.toCallee());

      Pair<String, Object[]> invokeMeta = calleePreEval.toInvokeMeta();
      String calleeSvcUri = invokeMeta.getKey();
      Object[] calleeMethodArgs = invokeMeta.getValue();
      StfInvoker.invoke(stfId, calleeSvcUri, calleeMethodArgs);
    } catch (Throwable e) {
      Exceptions.sneakyThrow(e, LOG, "An error occurred while invoking stf#{}", stfId);
    }
  }

  protected abstract void doNewStf(Long stfId, StfCall callee, int timeoutSecs);

  protected abstract void doNewDelayStf(Long stfId, StfCall callee, int timeoutSecs, int delaySecs);

  protected abstract long doLockStf(StfMetaGroup metaGrp, Long stfId, int timeoutSecs, int lastRetryTimes,
      long lastTimeoutAt);

  protected abstract int[] doBatchLockStfs(StfMetaGroup metaGrp, List<Object[]> batchArgs);

  @Override
  public boolean fallbackToSingleMarkDone(StfMetaGroup metaGrp, Long stfId) {
    return doMarkDone(metaGrp, stfId, false);
  }

  @Override
  public long fallbackToSingleLockStf(StfMetaGroup metaGrp, Stf stf, int timeoutSecs) {
    Long stfId = stf.getId();
    int lastRetryTimes = stf.getRetryTimes();
    long lastTimeoutAt = stf.getTimeoutAt();
    return doLockStf(metaGrp, stfId, timeoutSecs, lastRetryTimes, lastTimeoutAt);
  }

  protected abstract boolean doMarkDone(StfMetaGroup metaGrp, Long stfId, boolean batch);

  protected abstract void doMarkDead(StfMetaGroup metaGrp, Long stfId);

  protected abstract boolean doDelayTransfer(Stf lockedDelayStf, StfCall delayCalleePreEval);

  protected abstract String getCoreTblName();

  protected Pair<String, byte[]> doDelayTransfer0(Stf lockedDelayStf, StfCall delayCalleePreEval) {
    String delayBody = lockedDelayStf.getBody();
    byte[] delayBodyBytes = lockedDelayStf.getBodyBytes();

    // Compare delayCallee with runCallee bytes-store info, do the transformation if necessary
    boolean delayBytesEnabled = DELAY.isGlobalBodyBytesEnabled();// So far, we are reading global information
    CompressAlgorithm delayCompAlgo = DELAY.getGlobalBodyCompressAlgorithm();
    boolean coreBytesEnabled = CORE.isGlobalBodyBytesEnabled();
    CompressAlgorithm coreCompAlgo = CORE.getGlobalBodyCompressAlgorithm();

    Pair<String, byte[]> calleeMayGotBodyFormatChanged = of(delayBody, delayBodyBytes);
    // @formatter:off
    // Force follow core-meta
    if (delayBytesEnabled != coreBytesEnabled) {
      calleeMayGotBodyFormatChanged = doDelayBodyFormatToCore(delayBody, delayBodyBytes, coreBytesEnabled, coreCompAlgo);

    // Check compress-algorithm which may different
    } else {
      if (coreBytesEnabled == true) {
        if (delayCompAlgo != coreCompAlgo) {
          calleeMayGotBodyFormatChanged = doDelayBodyFormatToCore(delayBody, delayBodyBytes, coreBytesEnabled, coreCompAlgo);
        }
        // Do nothiing but follow delay's original body&bytes
        // else {
        //
        // }
      }
      // Do nothiing but follow delay's original body&bytes
      // else {
      //
      // }
    }
    // @formatter:on
    return calleeMayGotBodyFormatChanged;
  }

  private Pair<String, byte[]> doDelayBodyFormatToCore(String delayBody, byte[] delayBodyBytes,
      boolean coreBytesEnabled, CompressAlgorithm coreCompAlgo) {
    Stf restored = new Stf(delayBody, delayBodyBytes);
    StfCall callee = restored.toCallee();
    Pair<String, byte[]> res = callee.withBytes(coreBytesEnabled).withCompress(coreCompAlgo).toBytesIfNecessary();
    return res;
  }

  private void invokeConsumer(Pair<StfMetaGroup, Long> stfMeta, Stf stf, StfCall callee, boolean batch,
      BaseConsumer<StfMetaGroup> bizFn) {
    StfMetaGroup metaGrp = stfMeta.getLeft();
    Long stfId = stfMeta.getRight();
    if (bizFn instanceof PairConsumer) {
      ((PairConsumer<StfMetaGroup, Long>)bizFn).accept(metaGrp, stfId);
    } else if (bizFn instanceof TriConsumer) {
      ((TriConsumer<StfMetaGroup, Long, Boolean>)bizFn).accept(metaGrp, stfId, batch);
    } else if (bizFn instanceof QuadruConsumer) {
      ((QuadruConsumer<StfMetaGroup, Long, Stf, StfCall>)bizFn).accept(metaGrp, stfId, stf, callee);
    }
  }

  {
    worker = newWorkerOfStfCore();

    coreFn = (stfMeta, stf, callee, async, batch, bizFn) -> {
      Long stfId = stfMeta.getRight();
      if (checkFail(stfId)) {
        return;
      }
      if (!async) {
        invokeConsumer(stfMeta, stf, callee, false,
            bizFn);/*- This 'false' is somewhat weird,meaning that ‘Sync calls’ must also be non-batch */
        return;
      }
      worker.execute(() -> invokeConsumer(stfMeta, stf, callee, batch, bizFn));
    };

    forward = (metaGrp, stfId, lockedStf, calleePreEval) -> {
      if (metaGrp == CORE) {
        // Invoke callee to actually retry the job
        invokeCall(lockedStf, calleePreEval);
        return;
      }
      try {
        if (!doDelayTransfer(lockedStf, calleePreEval)) {
          return;
        }
      } catch (DuplicateKeyException e) {// Shouldn't happen
        try {
          H.tryCommitLaStfOnDup(LOG, stfId, getCoreTblName(), (DuplicateKeyException)e,
              laStfId -> this.fallbackToSingleMarkDone(DELAY, laStfId));
        } catch (Throwable e1) {
          Exceptions.swallow(e1, LOG, "An error occurred while auto committing stf-delay");
        }
        return;
      }
      this.markDone(DELAY, stfId, true);// Stf internally using Stf itself:)

      // Immediate trigger the invoke(this is not retry)
      invokeCall(lockedStf, calleePreEval);
    };

    markDone = (metaGrp, stfId, batch) -> {
      if (!doMarkDone(metaGrp, stfId, batch)) {
        LOG.error("The stf#{} can't be marked done", stfId);
      }
    };

    markDead = (metaGrp, stfId) -> {
      doMarkDead(metaGrp, stfId);
    };
  }

  @Override
  public StfRunMode getRunMode() {
    return runMode;
  }

  @Override
  public StfCore withRunMode(StfRunMode runMode) {
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