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
package com.stun4j.stf.sample.boot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jay Meng
 */
public abstract class JsonUtils {
  public static final ObjectMapper DFT_MAPPER = new ObjectMapper();

  public static String toJson(ObjectMapper mapper, Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException("json error", e);
    }
  }

  public static byte[] toJsonBytes(ObjectMapper mapper, Object obj) {
    try {
      return mapper.writeValueAsBytes(obj);
    } catch (Exception e) {
      throw new RuntimeException("json error", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    return fromJson(DFT_MAPPER, json, type);
  }

  public static <T> T fromJson(byte[] json, Class<T> type) {
    return fromJson(DFT_MAPPER, json, type);
  }

  public static <T> T fromJson(ObjectMapper mapper, String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new RuntimeException("json error", e);
    }
  }

  public static <T> T fromJson(ObjectMapper mapper, byte[] json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new RuntimeException("json error", e);
    }
  }
}