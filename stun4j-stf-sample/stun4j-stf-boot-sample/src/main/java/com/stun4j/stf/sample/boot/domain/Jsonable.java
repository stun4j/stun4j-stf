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
package com.stun4j.stf.sample.boot.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.stun4j.stf.sample.boot.utils.JsonUtils;

/**
 * @author Jay Meng
 */
public abstract class Jsonable implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Map<String, ObjectMapper> PROFILES;

  @JsonFilter("filter")
  private interface FieldFilterMixIn {
  }

  static {
    PROFILES = new HashMap<>();
    // TODO mj:Hard-code temporarily
    PROFILES.put(Req.class.getName(), JsonUtils.DFT_MAPPER);
    PROFILES.put(Req.class.getName() + "-insert", mapper("id"));
    PROFILES.put(Tx.class.getName(), JsonUtils.DFT_MAPPER);
    PROFILES.put(Tx.class.getName() + "-insert", mapper("id", "reqId", "st"));
  }

  private static ObjectMapper mapper(String... excludeFields) {
    ObjectMapper mapper = new ObjectMapper();
    Set<String> filterSet = Stream.of(excludeFields).collect(Collectors.toSet());
    SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept(filterSet);
    SimpleFilterProvider filterProvider = new SimpleFilterProvider().addFilter("filter", filter);
    return mapper.setFilterProvider(filterProvider).addMixIn(Req.class, FieldFilterMixIn.class).addMixIn(Tx.class,
        FieldFilterMixIn.class);
  }

  private ObjectMapper profile(Class<?> clz, String profile) {
    return profile == null ? PROFILES.get(clz.getName()) : PROFILES.get(clz.getName() + "-" + profile);
  }

  public String toJson(String profile) {
    return JsonUtils.toJson(profile(this.getClass(), profile), this);
  }

  public String toJson() {
    return JsonUtils.toJson(profile(this.getClass(), null), this);
  }

  public byte[] toJsonBytes() {
    return JsonUtils.toJsonBytes(profile(this.getClass(), null), this);
  }
}