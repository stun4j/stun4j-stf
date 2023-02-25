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

import static com.stun4j.stf.core.build.StfConfigs.actionPathBy;

import com.stun4j.stf.core.support.BaseVo;

/**
 * Sometimes we need id value carry with the identity information
 * 
 * @author Jay Meng
 */
public class StfId extends BaseVo {
  private final Long value;
  private final String identity;
  private static final StfId EMPTY;

  public StfId(Long value, String oid, String methodName) {
    this(value, actionPathBy(oid, methodName));
  }

  public StfId(Long value, String identity) {
    this.value = value;
    this.identity = identity;
  }

  static {
    EMPTY = new StfId(null, null);
  }

  public Long getValue() {
    return value;
  }

  public String getIdentity() {
    return identity;
  }

  static StfId empty() {
    return EMPTY;
  }

}
