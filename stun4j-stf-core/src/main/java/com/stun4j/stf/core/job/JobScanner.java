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

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  Stream<Stf> scanTimeoutCoreJobsWaitingRun(int limit, int pageNo);

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  Stream<Stf> scanTimeoutCoreJobsInProgress(int limit, int pageNo);

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  default Stream<Stf> scanTimeoutCoreJobsWaitingRun(int limit) {
    return scanTimeoutCoreJobsWaitingRun(limit, 0);
  }

  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  default Stream<Stf> scanTimeoutCoreJobsInProgress(int limit) {
    return scanTimeoutCoreJobsInProgress(limit, 0);
  }

  /**
   * @return the result Stream, containing delay-stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  Stream<Stf> scanTimeoutDelayJobsWaitingRun(int limit, int pageNo);

  /**
   * @return the result Stream, containing delay-stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  Stream<Stf> scanTimeoutDelayJobsInProgress(int limit, int pageNo);

  /**
   * @return the result Stream, containing delay-stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  default Stream<Stf> scanTimeoutDelayJobsWaitingRun(int limit) {
    return scanTimeoutDelayJobsWaitingRun(limit, 0);
  }

  /**
   * @return the result Stream, containing delay-stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  default Stream<Stf> scanTimeoutDelayJobsInProgress(int limit) {
    return scanTimeoutDelayJobsInProgress(limit, 0);
  }

}