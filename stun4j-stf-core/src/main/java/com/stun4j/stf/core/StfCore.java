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
 * @author JayMeng
 */
public interface StfCore extends StfBatchable {
  StfCore withRunMode(StfRunModeEnum runMode);

  @SuppressWarnings("unchecked")
  Long newStf(String bizObjId, String bizMethodName, Integer timeoutSeconds, Pair<?, Class<?>>... typedArgs);

  @SuppressWarnings("unchecked")
  default Long newStf(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    return newStf(bizObjId, bizMethodName, null, typedArgs);
  }

  long lockStf(String jobGrp, Long stfId, int timeoutSeconds, int lastRetryTimes, long lastTimeoutAt);

  void markDone(StfMetaGroupEnum metaGrp, Long stfId, boolean async);

  void markDead(StfMetaGroupEnum metaGrp, Long stfId, boolean async);

  void reForward(StfMetaGroupEnum metaGrp, Long stfId, int lastRetryTimes, String calleeInfo, boolean async,
      Object... calleeMethodArgs);

  StfRunModeEnum getRunMode();

  static StfCore empty() {
    return new StfCore() {
      static final String MODULE_ID = "stf-core";

      public StfCore withRunMode(StfRunModeEnum runMode) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Long newStf(String bizObjId, String bizMethodName, Integer timeoutSeconds,
          Pair<?, Class<?>>... typedArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public long lockStf(String jobGrp, Long stfId, int timeoutSeconds, int lastRetryTimes, long lastTimeoutAt) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return -1;
      }

      @Override
      public void markDone(StfMetaGroupEnum metaGrp, Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void markDead(StfMetaGroupEnum metaGrp, Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void reForward(StfMetaGroupEnum metaGrp, Long stfId, int lastRetryTimes, String calleeInfo, boolean async,
          Object... calleeMethodArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public List<Stf> batchLockStfs(String jobGrp, List<Object[]> preBatchArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public long fallbackToSingleLockStf(StfMetaGroupEnum metaGrp, Stf stf, int timeoutSeconds) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return -1;
      }

      @Override
      public int[] batchMarkDone(StfMetaGroupEnum metaGrp, List<Object[]> stfIdsInfo) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public boolean fallbackToSingleMarkDone(StfMetaGroupEnum metaGrp, Long stfId) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return false;
      }

      @Override
      public StfRunModeEnum getRunMode() {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

    };
  }

}