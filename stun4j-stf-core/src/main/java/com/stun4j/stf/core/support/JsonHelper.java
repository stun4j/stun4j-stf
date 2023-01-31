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
package com.stun4j.stf.core.support;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.tuple.Pair;
import org.xerial.snappy.Snappy;

import com.github.luben.zstd.Zstd;
import com.stun4j.stf.core.serializer.Serializer;
import com.stun4j.stf.core.utils.Exceptions;

/** @author Jay Meng */
public abstract class JsonHelper {
  private static final Serializer DFT_SERIALIZER = Serializer.json();
  public static final Serializer NO_TYPING_SERIALIZER = Serializer.json(false);

  public static String toJson(Object obj) {
    return toJson(DFT_SERIALIZER, obj);
  }

  public static String toJson(Serializer serializer, Object obj) {
    return new String(serializer.serialize(obj), StandardCharsets.UTF_8);
  }

  public static Pair<Integer, byte[]> toJson(Object obj, CompressAlgorithmEnum compAlgo) {
    return toJson(DFT_SERIALIZER, obj, compAlgo);
  }

  public static Pair<Integer, byte[]> toJson(Serializer serializer, Object obj, CompressAlgorithmEnum compAlgo) {
    try {
      byte[] raw = serializer.serialize(obj);
      switch (compAlgo) {
        case ZSTD:
          return Pair.of(raw.length, Zstd.compress(raw));// TODO mj:lvl
        case SNAPPY:
          return Pair.of(raw.length, Snappy.compress(raw));
        default:
          return Pair.of(null, raw);
      }
    } catch (Throwable e) {
      throw Exceptions.sneakyThrow(e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    return fromJson(DFT_SERIALIZER, json, type);
  }

  public static <T> T fromJson(Serializer serializer, String json, Class<T> type) {
    if (json == null) return null;
    return serializer.deserialize(json.getBytes(StandardCharsets.UTF_8), type);
  }
}