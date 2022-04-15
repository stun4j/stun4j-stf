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

/** @author Jay Meng */
public abstract class BaseEntity<ID> extends BaseVo implements Entity<ID> {
  public abstract ID getId();

  @Override
  public int hashCode() {
    return hashCodeEntity();
  }

  @Override
  public boolean equals(Object obj) {
    return equalsEntity(obj);
  }
}
