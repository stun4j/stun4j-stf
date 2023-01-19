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

import static com.stun4j.guid.core.utils.Asserts.state;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.utils.ClassUtils;
import com.stun4j.stf.core.utils.NumberUtils;
import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;

/**
 * Method invoker via reflection
 * @author Jay Meng
 */
abstract class StfInvoker {
  private final static Pattern PATTERN = Pattern.compile("(injvm):(\\w+)\\.(\\w+)");

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void invoke(Long stfId, String calleeInfo, Object... calleeMethodArgs) throws Exception {
    Matcher matcher = PATTERN.matcher(calleeInfo);
    if (!matcher.matches()) {
      return;
    }
    String type = matcher.group(1);
    switch (type) {
      case "injvm":
        String bizObjId = matcher.group(2);
        String methodName = matcher.group(3);
        Object bizObj = StfContext.getBizObj(bizObjId);
        state(bizObj != null && methodName != null, "The biz-obj or method can't be null [bizObjId=%s, methodName=%s]",
            bizObjId, methodName);
        boolean argsNotEmpty = ArrayUtils.isNotEmpty(calleeMethodArgs);
        Class<?>[] argTypes = argsNotEmpty ? new Class[calleeMethodArgs.length] : null;
        Object[] args = argsNotEmpty ? new Object[calleeMethodArgs.length] : null;
        if (argsNotEmpty) {
          int i = 0;
          for (Object arg : calleeMethodArgs) {
            Class argClz;
            Object argObj;
            if (!(arg instanceof Map)) {// FIXME mj:wot if arg is null?
              argClz = argTypes[i] = arg.getClass();
              argObj = arg;
            } else {
              Map<String, Object> map = (Map<String, Object>)arg;
              Entry<String, Object> entry = map.entrySet().iterator().next();
              String argType = entry.getKey();
              argObj = entry.getValue();
//              argClz = argTypes[i] = ClassUtils.getClass(argType);
              argClz = argTypes[i] = ClassUtils.resolveClassName(argType, ClassUtils.getDefaultClassLoader());
            }
            boolean isNum = NumberUtils.isCreatable(argClz);
            if (isNum) {
              argObj = NumberUtils.parseNumber(String.valueOf(argObj),
                  Primitives.isPrimitive(argClz) ? Primitives.wrap(argClz) : argClz);
            } else if (String.class != argClz) {
              // TODO mj:optimize,Date etc.
              String tmpJson = JsonHelper.toJson(argObj);
              argObj = JsonHelper.fromJson(tmpJson, argClz);
            }
            args[i] = argObj;
            argTypes[i++] = argClz;
          }
        }
        Method method;
        StfContext.withStfId(new StfId(stfId, bizObjId, methodName));
        if (argsNotEmpty) {
          method = MethodUtils.getAccessibleMethod(bizObj.getClass(), methodName, argTypes);
          method.invoke(bizObj, args);
        } else {
          method = MethodUtils.getAccessibleMethod(bizObj.getClass(), methodName);
          method.invoke(bizObj);
        }
        break;
      // TODO mj:others:mq?dubbo?http
      case "xxx":
        break;
    }
  }
}