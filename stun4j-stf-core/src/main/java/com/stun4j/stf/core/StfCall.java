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

import static com.stun4j.stf.core.support.JsonHelper.NO_TYPING_SERIALIZER;
import static com.stun4j.stf.core.support.JsonHelper.toJson;
import static com.stun4j.stf.core.utils.Asserts.state;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.support.CompressAlgorithm;
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

  // private int timeoutSecs;

  private boolean bytes = false;
  private byte compAlgo = CompressAlgorithm.NONE.value();// FIXME mj:to be configured
  private Integer zstdOriSize;

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

  public Pair<String, Object[]> toInvokeMeta() {
    // String type = this.type;
    // String bizObjId = this.getBizObjId();
    // String method = this.getMethod();
    StringBuilder builder = new StringBuilder(this.type).append(":").append(this.bizObjId).append(".")
        .append(this.method);
    String invokeInfo = builder.toString();
    return Pair.of(invokeInfo, this.args);
  }

  public Pair<String, byte[]> toBytesIfNecessary() {
    CompressAlgorithm algo;
    Pair<Integer, byte[]> bytesInfo = toJson(this, algo = CompressAlgorithm.valueOf(compAlgo));
    byte[] rtnBytes = bytesInfo.getValue();
    switch (algo) {
      case NONE:
        if (bytes) {
          String meta = toJson(NO_TYPING_SERIALIZER, new StfCall(algo).enableBytes());
          return Pair.of(meta, rtnBytes);
        }
        String callStr = new String(rtnBytes, StandardCharsets.UTF_8);
        return Pair.of(callStr, null);
      default:
        StfCall call = new StfCall(algo)
            .enableBytes();/*- force bytes-format to be enabled when using any compress algorithm*/
        if (algo == CompressAlgorithm.ZSTD) {
          call.withZstdOriSize(bytesInfo.getKey());
        }
        String meta = toJson(NO_TYPING_SERIALIZER, call);
        return Pair.of(meta, rtnBytes);
    }
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

  public StfCall enableBytes() {
    bytes = true;
    return this;
  }

  public StfCall enableCompress() {
    return enableCompress(CompressAlgorithm.ZSTD);
  }

  public StfCall enableCompress(CompressAlgorithm compAlgo) {
    this.compAlgo = compAlgo.value();
    if (compAlgo != CompressAlgorithm.NONE) {
      this.enableBytes();// force bytes-format to be enabled when using any compress algorithm
    }
    return this;
  }

  private StfCall(String type, String bizObjId, String method, Object[] args) {
    this.type = type;
    this.bizObjId = bizObjId;
    this.method = method;
    this.args = args;
  }

  private StfCall(CompressAlgorithm compAlgo) {
    this();
    this.enableCompress(compAlgo);
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

  public byte getCompAlgo() {
    return compAlgo;
  }

  public boolean isBytes() {
    return bytes;
  }

  public Integer getZstdOriSize() {
    return zstdOriSize;
  }

  public StfCall withZstdOriSize(Integer zstdOriSize) {
    this.zstdOriSize = zstdOriSize;
    return this;
  }

}
