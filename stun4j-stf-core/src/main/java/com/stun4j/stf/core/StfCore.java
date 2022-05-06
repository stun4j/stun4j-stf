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

import org.apache.commons.lang3.tuple.Pair;

/**
 * Stf's core operations,which basically covers the whole life cycle of Stf
 * @author JayMeng
 */
public interface StfCore {

  @SuppressWarnings("unchecked")
  Long init(String bizObjId, String bizMethodName, Integer timeoutSeconds, Pair<?, Class<?>>... typedArgs);

  @SuppressWarnings("unchecked")
  default Long init(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    return init(bizObjId, bizMethodName, null, typedArgs);
  }

  @Deprecated
  void forward(Long stfId, String calleeInfo, boolean async, Object... calleeMethodArgs);

  boolean tryLockStf(Long stfId, int timeoutSecs, int curRetryTimes);

  void markDone(Long stfId, boolean async);

  void markDead(Long stfId, boolean async);

  void reForward(Long stfId, int lastRetryTimes, String calleeInfo, boolean async, Object... calleeMethodArgs);

  static StfCore empty() {
    return new StfCore() {
      static final String MODULE_ID = "stf-core";

      @Override
      public void forward(Long stfId, String calleeInfo, boolean async, Object... calleeMethodArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public boolean tryLockStf(Long stfId, int timeoutSecs, int curRetryTimes) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return false;
      }

      @Override
      public void markDone(Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void markDead(Long stfId, boolean async) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public void reForward(Long stfId, int curRetryTimes, String calleeInfo, boolean async,
          Object... calleeMethodArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @SuppressWarnings("unchecked")
      @Override
      public Long init(String bizObjId, String bizMethodName, Integer timeoutSeconds, Pair<?, Class<?>>... typedArgs) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }
    };
  }

}