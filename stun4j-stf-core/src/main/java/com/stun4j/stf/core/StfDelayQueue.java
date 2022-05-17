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

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.support.StfHelper.H;
import static com.stun4j.stf.core.utils.Asserts.notNull;
import static com.stun4j.stf.core.utils.Asserts.requireNonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.build.ActionMethodChecker;

/**
 * This is a convenience class that simply encapsulates the internal capabilities of Stf, hence the so-called DelayQueue
 * functionality.
 * @author Jay Meng
 */
public final class StfDelayQueue {
  private final StfDelayQueueCore core;
  private final Map<Class<?>, Object> CONSTRUCTOR_CHECK_MEMO;
  private final Map<String, Boolean> TASK_METHOD_HEAD_MEMO;

  public final Long offer(String taskObjId, String taskMethodName, int delaySeconds, Object... taskParams) {
    Stream<Pair<?/* arg-value */, Class<?>/* arg-type */>> pairStream = Stream.of(taskParams)
        .map(p -> Pair.of(p, p.getClass()));
    return offer(taskObjId, taskMethodName, delaySeconds, pairStream);
  }

  @SuppressWarnings("unchecked")
  public final Long offer(String taskObjId, String taskMethodName, int delaySeconds,
      Stream<Pair<?/* arg-value */, Class<?>/* arg-type */>> taskParams) {
    // Inspired from ActionMethodChecker->
    Pair<?, Class<?>>[] taskParamsPair = taskParams.toArray(Pair[]::new);
    Class<?>[] taskMethodArgClzs = Stream.of(taskParamsPair).map(argPair -> {// re-stream:(
      Class<?> clz = argPair.getRight();
      return clz;
    }).toArray(Class[]::new);
    String theCacheKey = Stream.of(taskMethodArgClzs).map(c -> c.getName()).reduce(taskObjId + "#" + taskMethodName,
        (a, b) -> a + "-" + b);
    TASK_METHOD_HEAD_MEMO.computeIfAbsent(theCacheKey, k -> {
      Class<?> taskObjClz = StfContext.getBizObjClass(taskObjId);
      notNull(taskObjClz,
          "The taskObjClass corresponding to the taskObjId '%s' can't be null > A task method can only be invoked if it exists, forgot to register taskObjClass with the oid '%s'?",
          taskObjId, taskObjId);

      // Ensure that each arg class has a default constructor to avoid potential serialization error
      ActionMethodChecker.ensureDefaultConstructor(CONSTRUCTOR_CHECK_MEMO, taskMethodArgClzs);
      Method matchedMethod = MethodUtils.getAccessibleMethod(taskObjClz, taskMethodName, taskMethodArgClzs);
      notNull(matchedMethod, () -> lenientFormat(
          "No matched task method was found [method-signature=%s#%s%s] > Please check your task register mechanism(e.g. Spring,POJO Registry,Guava) ",
          taskObjClz, taskMethodName,
          Arrays.toString(taskMethodArgClzs).replaceFirst("\\[", "(").replaceFirst("\\]", ")")));
      return true;
    });
    // <-

    StfCall callee = core.newCallee(taskObjId, taskMethodName, delaySeconds, taskParamsPair);
    Long stfId = H.cachedGuid().next();
    core.doNewStf(stfId, callee, delaySeconds);
    return stfId;
  }

  public StfDelayQueue(StfDelayQueueCore core) {
    this.core = requireNonNull(core, "The stf-delayqueue-core can't be null");
    CONSTRUCTOR_CHECK_MEMO = new ConcurrentHashMap<>();
    TASK_METHOD_HEAD_MEMO = new ConcurrentHashMap<>();
  }

}