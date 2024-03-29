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

import static com.stun4j.stf.core.StfConsts.NOT_INITIALIZED_THROW;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Stf's core operations,which basically covers the whole life cycle of Stf
 * 
 * @author JayMeng
 */
public interface StfCore extends StfBatchable {
  StfCore withRunMode(StfRunMode runMode);

  @SuppressWarnings("unchecked")
  Long newStf(String bizObjId, String bizMethodName, Integer timeoutSecs, Pair<?, Class<?>>... typedArgs);

  @SuppressWarnings("unchecked")
  default Long newStf(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    return newStf(bizObjId, bizMethodName, null, typedArgs);
  }

  long lockStf(StfMetaGroup metaGrp, Long stfId, int timeoutSecs, int lastRetryTimes, long lastTimeoutAt);

  void forward(StfMetaGroup metaGrp, Stf lockedStf, StfCall calleePreEval, boolean async);

  void markDone(StfMetaGroup metaGrp, Long stfId, boolean async);

  void markDead(StfMetaGroup metaGrp, Long stfId, boolean async);

  StfRunMode getRunMode();

  static StfCore empty() {
    return new StfCore() {
      static final String MODULE_ID = "stf-core";

      public StfCore withRunMode(StfRunMode runMode) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Long newStf(String bizObjId, String bizMethodName, Integer timeoutSecs, Pair<?, Class<?>>... typedArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public long lockStf(StfMetaGroup metaGrp, Long stfId, int timeoutSecs, int lastRetryTimes, long lastTimeoutAt) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return -1;
      }

      @Override
      public void forward(StfMetaGroup metaGrp, Stf lockedStf, StfCall calleePreEval, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void markDone(StfMetaGroup metaGrp, Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void markDead(StfMetaGroup metaGrp, Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public List<Stf> batchLockStfs(StfMetaGroup metaGrp, List<Object[]> preBatchArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public long fallbackToSingleLockStf(StfMetaGroup metaGrp, Stf stf, int timeoutSecs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return -1;
      }

      @Override
      public int[] batchMarkDone(StfMetaGroup metaGrp, List<Object[]> stfIdsInfo) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public boolean fallbackToSingleMarkDone(StfMetaGroup metaGrp, Long stfId) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return false;
      }

      @Override
      public StfRunMode getRunMode() {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

    };
  }

}