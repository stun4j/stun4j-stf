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

import java.util.stream.Stream;

import com.stun4j.stf.core.Stf;

/**
 * @author Jay Meng
 */
public interface JobScanner {
  int DFT_INCLUDE_HOW_MANY_DAYS_AGO = 1;
  int DFT_MAX_INCLUDE_HOW_MANY_DAYS_AGO = 100;

  Stream<Stf> scanTimeoutCoreJobs(int limit, int pageNo);

  Stream<Stf> scanTimeoutDelayJobs(int limit, int pageNo);

  default Stream<Stf> scanTimeoutCoreJobs(int limit) {
    return scanTimeoutCoreJobs(limit, 0);
  }

  default Stream<Stf> scanTimeoutDelayJobs(int limit) {
    return scanTimeoutDelayJobs(limit, 0);
  }

}