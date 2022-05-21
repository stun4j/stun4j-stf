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
package com.stun4j.stf.core.job;

import java.util.HashMap;
import java.util.Map;

public interface JobConsts {
  static final String KEY_FEATURE_TIMEOUT_DELAY = "TimeoutDelay";
  static final String JOB_GROUP_TIMEOUT_WAITING_RUN = "TimeoutWaitingRun";
  static final String JOB_GROUP_TIMEOUT_IN_PROGRESS = "TimeoutInProgress";
  static final String JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN = KEY_FEATURE_TIMEOUT_DELAY + "WaitingRun";
  static final String JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS = KEY_FEATURE_TIMEOUT_DELAY + "InProgress";
  static final String[] ALL_JOB_GROUPS = new String[]{JOB_GROUP_TIMEOUT_WAITING_RUN, JOB_GROUP_TIMEOUT_IN_PROGRESS,
      JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN, JOB_GROUP_TIMEOUT_DELAY_IN_PROGRESS};

  static Map<Integer, Integer> generateRetryBehaviorByPattern(
      int timeoutSeconds) {/*- TODO mj:Open this block to make it configurable*/
    Map<Integer, Integer> map = new HashMap<>();
    map.put(1, 0);
    map.put(2, 1 * timeoutSeconds);
    map.put(3, 2 * timeoutSeconds);
    map.put(4, 3 * timeoutSeconds);
    map.put(5, 5 * timeoutSeconds);
    return map;
  }
}
