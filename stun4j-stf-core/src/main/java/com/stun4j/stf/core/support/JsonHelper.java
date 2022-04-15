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

import com.stun4j.stf.core.serializer.Serializer;

/** @author Jay Meng */
public abstract class JsonHelper {
  private static final Serializer DFT_SERIALIZER = Serializer.json();

  public static String toJson(Object obj) {
    return toJson(DFT_SERIALIZER, obj);
  }

  public static String toJson(Serializer serializer, Object obj) {
    return new String(serializer.serialize(obj), StandardCharsets.UTF_8);
  }

  public static <T> T fromJson(String json, Class<T> type) {
    return fromJson(DFT_SERIALIZER, json, type);
  }

  public static <T> T fromJson(Serializer serializer, String json, Class<T> type) {
    if (json == null) return null;
    return serializer.deserialize(json.getBytes(StandardCharsets.UTF_8), type);
  }
}