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

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.utils.Asserts.argument;
import static com.stun4j.stf.core.utils.Asserts.state;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;

import com.stun4j.stf.core.spi.StfRegistry;

/** @author Jay Meng */
public class StfDefaultSpringRegistry extends ConcurrentHashMap<Serializable, Object> implements StfRegistry {
  private static final long serialVersionUID = 1L;

  private final ApplicationContext applicationContext;

  private final ConcurrentHashMap<String, Class<?>> map2;
  {
    map2 = new ConcurrentHashMap<>();
  }

  @Override
  public Object getObj(Serializable bizKey) {// TODO mj:synchronized
    if (bizKey instanceof String) {
      return applicationContext.getBean((String)bizKey);
    } else if (bizKey instanceof Class<?>) {
      String[] beanNames = applicationContext.getBeanNamesForType((Class<?>)bizKey);
      /*
       * This is a very strict check to ensure that a unique bean exists.
       * If not,stf-flow will not run exactly as configured
       */
      state(ArrayUtils.getLength(beanNames) == 1, lenientFormat(
          "Found multiple biz-obj with same class [class=%s, bean-names=%s]", bizKey, Arrays.toString(beanNames)));
      return beanNames[0];
    }
    return null;
  }

  public StfDefaultSpringRegistry(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public synchronized void putObj(String bizId, Object bizObj, Class<?> specifiedClz) {
    argument(bizObj == null, "In Stf,to register an object-instance is not supported in 'Spring' scenarios");
    super.put(specifiedClz, bizId);
    map2.put(bizId, specifiedClz);
  }

  @Override
  public Class<?> getObjClass(String bizObjId) {
    // This is relatively loose bizObj acquisition strategy,it is friendly for using DelayQueue->
    Class<?> clz;
    if ((clz = map2.get(bizObjId)) != null) {
      return clz;
    }
    return applicationContext.getType(bizObjId);
    // <-
    // return map2.get(bizObjId);
  }
}