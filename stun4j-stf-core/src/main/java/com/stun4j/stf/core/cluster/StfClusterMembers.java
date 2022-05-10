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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author JayMeng
 */
public abstract class StfClusterMembers {
  private static final Logger LOG = LoggerFactory.getLogger(StfClusterMembers.class);
  static final AtomicReference<String[]> availableMembers = new AtomicReference<>();

  public static void replaceWith(Stream<StfClusterMember> allMembers, int hbTimeoutMs) {
    long now = System.currentTimeMillis();
    String[] memberIds = allMembers.filter(m -> {
      return now - m.getUpAt() <= hbTimeoutMs;
    }).sorted().map(m -> m.getId()).toArray(String[]::new);
    availableMembers.set(memberIds);
  }

  public static int determineBlockToTakeOver() {
    String[] availableMemberIds = availableMembers.get();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Found availableMembers: {}", Arrays.toString(availableMemberIds));
    }
    String localMemberId = StfClusterMember.calculateId();
    if (availableMemberIds == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("The local-member#{} will take over block#{}", localMemberId, 0);
      }
      return 0;
    }
    int pos = ArrayUtils.indexOf(availableMemberIds, localMemberId);
    pos = pos < 0 ? 0 : pos;
    if (LOG.isDebugEnabled()) {
      LOG.debug("The local-member#{} will take over block#{}", localMemberId, pos);
    }
    return pos;
  }
}
