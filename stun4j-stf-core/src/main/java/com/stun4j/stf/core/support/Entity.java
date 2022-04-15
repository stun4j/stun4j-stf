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
public interface Entity<ID> {
  ID getId();

  default int hashCodeEntity() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((this.getId() == null) ? 0 : this.getId().hashCode());
    return result;
  }

  default boolean equalsEntity(Object obj) {
    @SuppressWarnings("unchecked")
    Entity<ID> other = (Entity<ID>)obj;
    if (other == this) {
      return true;
    }
    if (other == null || other.getClass() != other.getClass()) {
      return false;
    }
    return getId().equals(other.getId());
  }
}
