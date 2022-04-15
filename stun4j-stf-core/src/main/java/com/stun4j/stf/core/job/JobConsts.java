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

public interface JobConsts {
  public static final String JOB_GROUP_TIMEOUT_WAITING_RUN = "TimeoutJobsOfWaitingRun";
  public static final String JOB_GROUP_TIMEOUT_RUNNING = "TimeoutJobsOfRunning";
  public static final String[] ALL_JOB_GROUPS = new String[]{JOB_GROUP_TIMEOUT_WAITING_RUN, JOB_GROUP_TIMEOUT_RUNNING};
}
