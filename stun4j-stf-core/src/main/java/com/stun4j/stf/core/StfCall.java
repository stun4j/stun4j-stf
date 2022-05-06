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

import static com.stun4j.stf.core.utils.Asserts.state;

import java.util.Collections;

import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;

/**
 * A 'call' carries enough meta-data of an action/cmd,which drives state move-forward,could be a caller or a callee
 * @author Jay Meng
 */
public class StfCall {
  private static final String TYPE_INJVM = "injvm";

  private final String type;
  private final String bizObjId;
  private final String method;
  private final Object[] args;

//  private int timeoutSecs;

  public static StfCall of(String type, String bizObjId, String method) {
    return new StfCall(type, bizObjId, method, null);
  }

  public static StfCall ofInJvm(String bizObjId, String method) {
    return new StfCall(TYPE_INJVM, bizObjId, method, null);
  }

  public static StfCall ofInJvm(String bizObjId, String method, int argsLength) {
    return new StfCall(TYPE_INJVM, bizObjId, method, new Object[argsLength]);
  }

  public static StfCall of(String type, String bizObjId, String method, int argsLength) {
    return new StfCall(type, bizObjId, method, new Object[argsLength]);
  }

  public StfCall withPrimitiveArg(int argIdx, Object argValue, Class<?> primitiveClz) {
    state(Primitives.isPrimitive(primitiveClz), "Not primitive type");
    // primitiveClz = Primitives.unwrap(primitiveClz);
    if (int.class == primitiveClz) {
      argValue = (int)argValue;
    } else if (long.class == primitiveClz) {
      // TODO mj:long is very special,more test for other primitive types
      // argValue = (long) argValue;
      argValue = String.valueOf(argValue);
    } else if (boolean.class == primitiveClz) {
      argValue = (boolean)argValue;
    } else if (float.class == primitiveClz) {
      argValue = (float)argValue;
    } else if (byte.class == primitiveClz) {
      argValue = (byte)argValue;
    } else if (short.class == primitiveClz) {
      argValue = (short)argValue;
    } else if (double.class == primitiveClz) {
      argValue = (double)argValue;
    } else if (char.class == primitiveClz) {
      argValue = (char)argValue;
    } else if (void.class == primitiveClz) {
      throw new IllegalStateException("Not supported primitive type");
    }
    args[argIdx] = Collections.singletonMap(primitiveClz.getName(), argValue);
    return this;
  }

  public StfCall withArg(int argIdx, Object argValue) {
    args[argIdx] = argValue;
    return this;
  }

//  public StfCall withTimeoutSeconds(int timeoutSeconds) {
//    this.timeoutSecs = timeoutSeconds;
//    return this;
//  }

  private StfCall(String type, String bizObjId, String method, Object[] args) {
    this.type = type;
    this.bizObjId = bizObjId;
    this.method = method;
    this.args = args;
  }

  StfCall() {
    this(null, null, null, null);
  }

  public String getType() {
    return type;
  }

  public String getBizObjId() {
    return bizObjId;
  }

  public String getMethod() {
    return method;
  }

  public Object[] getArgs() {
    return args;
  }

//  public int getTimeoutSecs() {
//    return timeoutSecs;
//  }

}
