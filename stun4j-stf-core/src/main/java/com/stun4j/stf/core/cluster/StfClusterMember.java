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
package com.stun4j.stf.core.cluster;

import static com.stun4j.stf.core.StfHelper.H;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.support.BaseEntity;
import com.stun4j.stf.core.support.event.StfEventBus;

/**
 * @author JayMeng
 */
public class StfClusterMember extends BaseEntity<String> implements Comparable<StfClusterMember> {
  private String id;
  private long upAt;

  public static void sendHeartbeat() {
    StfEventBus.post(Heartbeat.SIGNAL);
  }

  public StfClusterMember(String id, long upAt) {
    this.id = id;
    this.upAt = upAt;
  }

  @Override
  public int compareTo(StfClusterMember otherMember) {
    return id.compareTo(otherMember.id);
  }

  @Override
  public String getId() {
    return id;
  }

  public long getUpAt() {
    return upAt;
  }

  public static String calculateId() {/*-TODO mj:trace all the ids which might be generated during the runtime(e.g. for a more thorough cleanup?but be care of the rarely happened reuse problem)*/
    LocalGuid guid;
    synchronized (guid = LocalGuid.instance()) {// H.cachedGuid();
      return guid.getDatacenterId() + "-" + guid
          .getWorkerId();/*-TODO mj:This may lead to a 'thundering herd', but what if that's the desired effect?Meanwhile,also a slight performance advantage here*/
    }
  }
}
