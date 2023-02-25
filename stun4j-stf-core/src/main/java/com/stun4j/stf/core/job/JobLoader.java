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
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.cluster.StfClusterMembers;

/**
 * @author Jay Meng
 */
public class JobLoader extends BaseJobLoader {
  private final JobScanner scanner;

  @Override
  protected Stream<Stf> loadJobs(StfMetaGroup metaGrp, int loadSize) {
    int pageNo = StfClusterMembers.determineBlockToTakeOver();
    // TODO mj:Downgrade solution,consider the underlying FJP:
    // StfInternalExecutors#newWorkerOfJobLoading
    switch (metaGrp) {
      case CORE:
        return scanner.scanTimeoutCoreJobs(loadSize, pageNo);
      case DELAY:
        return scanner.scanTimeoutDelayJobs(loadSize, pageNo);
      default:
        return Stream.empty();
    }
  }

  public JobLoader(JobScanner scanner) {
    this.scanner = scanner;
  }

}