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
package com.stun4j.stf.core.support.registry;

import static com.stun4j.stf.core.utils.Asserts.argument;
import static com.stun4j.stf.core.utils.Asserts.notNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.utils.Exceptions;

/** @author Jay Meng */
public class StfDefaultPOJORegistry extends ConcurrentHashMap<Serializable, Object> implements StfRegistry {
  private static final long serialVersionUID = 1L;

  private final ConcurrentHashMap<String, Class<?>> map2;
  {
    map2 = new ConcurrentHashMap<>();
  }

  @Override
  public Object getObj(Serializable bizId) {
    return super.get(bizId);
  }

  @Override
  public synchronized void putObj(String bizId, Object bizObj, Class<?> specifiedClz) {
    notNull(bizId, "The bizId can't be null");
    notNull(bizObj, "The bizObj can't be null > In POJO scenarios, object-instance can't be null");
    Class<?> clz = null;
    try {
      super.put(bizId, bizObj);
      // the reverse index;
      if (specifiedClz != null) {// TODO mj:other $$EnhancerByXXX
        argument(specifiedClz.isAssignableFrom(bizObj.getClass()), "Class not matched [specifiedClz=%s, bizObjClz=%s]",
            specifiedClz, bizObj.getClass());
        clz = wipeOffEnhancement(bizObj, specifiedClz);
        super.put(specifiedClz, bizId);
        map2.put(bizId, clz);
        return;
      }
      clz = wipeOffEnhancement(bizObj);
      super.put(clz, bizId);
      map2.put(bizId, clz);
    } catch (Exception e) {
      // the rollback->
      super.remove(bizId);
      if (clz != null) {
        super.remove(clz);
      }
      map2.remove(bizId);
      // <-
      Exceptions.sneakyThrow(e);
    }
  }

  // wipe off 'specifiedClz',get rid of any enhancement
  private Class<?> wipeOffEnhancement(Object bizObj, Class<?> specifiedClz) throws ClassNotFoundException {
    String bizObjClzName = specifiedClz.getName();
    Class<?> clz;
    int tmpIdx;
    if ((tmpIdx = bizObjClzName.indexOf("$$EnhancerBySpringCGLIB")) != -1
        || (tmpIdx = bizObjClzName.indexOf("$$FastClassBySpringCGLIB")) != -1) {// TODO mj:other enhancement pattern
      bizObjClzName = bizObjClzName.substring(0, tmpIdx);
      clz = Class.forName(bizObjClzName, false, bizObj.getClass().getClassLoader());
    } else {
      clz = specifiedClz;
    }
    return clz;
  }

  // wipe off 'bizObj',get rid of any enhancement
  private Class<?> wipeOffEnhancement(Object bizObj) throws ClassNotFoundException {
    String bizObjClzName = bizObj.getClass().getName();
    int tmpIdx;
    if ((tmpIdx = bizObjClzName.indexOf("$$EnhancerBySpringCGLIB")) != -1
        || (tmpIdx = bizObjClzName.indexOf("$$FastClassBySpringCGLIB")) != -1) {// TODO mj:other enhancement pattern
      bizObjClzName = bizObjClzName.substring(0, tmpIdx);
    }
    Class<?> clz = Class.forName(bizObjClzName, false, bizObj.getClass().getClassLoader());
    return clz;
  }

  @Override
  public Class<?> getObjClass(String bizObjId) {
    // FIXME mj:Add loose bizObj acquisition strategy for DQ(check 'StfDefaultSpringRegistry')
    return map2.get(bizObjId);
  }
}