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
package com.stun4j.stf.core.spi;

import static com.stun4j.stf.core.StfConsts.NOT_INITIALIZED_THROW;

import java.io.Serializable;

/**
 * The SPI interface connects Stf to user-side services, systems, or containers(e.g. Spring,Guava) etc.
 * @author Jay Meng
 */
public interface StfRegistry {
  default void putObj(String bizObjId, Object bizObj) {
    putObj(bizObjId, bizObj, null);
  }

  void putObj(String bizObjId, Object bizObj, Class<?> specifiedBizObjClz);

  /**
   * Suitable for scenario using 'Spring'-like container (compare to {@link #putObj(String, Object)} ,you just need to
   * register biz-obj-class to make it work)
   */
  default void putObjClass(String bizObjId, Class<?> specifiedBizObjClz) {
    putObj(bizObjId, null, specifiedBizObjClz);
  }

  Object getObj(Serializable bizKey);

  Class<?> getObjClass(String bizObjId);

  static StfRegistry empty() {
    return new StfRegistry() {
      static final String MODULE_ID = "stf-biz-reg";

      @Override
      public void putObj(String bizObjId, Object bizObj, Class<?> specifiedClz) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
      }

      @Override
      public Object getObj(Serializable bizKey) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

      @Override
      public Class<?> getObjClass(String bizObjId) {
        NOT_INITIALIZED_THROW.accept(MODULE_ID);
        return null;
      }

    };
  };
}