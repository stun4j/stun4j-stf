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

import java.util.Map;
import java.util.stream.Stream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.BaseContainerCase;
import com.stun4j.stf.core.State;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.utils.Utils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class JobScannerCase extends BaseContainerCase<JobScanner> {

  static {
    LocalGuid.init(0, 0);
  }

  @Test
  public void _01_basicConnectivity_optRtnFields() {
    JobScanner biz = newJobScanner(null);
    StfCore stfc = newStfCore(biz);
    stfc.newStf("foo", "bar", 1);// given a shortest timeout
    Utils.sleepSeconds(1);
    try (Stream<Stf> stfs = biz.scanTimeoutCoreJobs(1)) {
      assert stfs.count() == 1 : "should find 1 timeout job";
    }

    // Only sql programmar is tested
    try (Stream<Stf> stfs = biz.scanTimeoutDelayJobs(1)) {
    }
  }

  @Test
  public void _02_timeout_scanTimeoutJobsWaitingRun() {
    _02_template(State.I);
  }

  private void _02_template(State st) {
    JobScanner biz = newJobScanner(null);
    StfCore stfc = newStfCore(biz);
    // Initialize two pieces of data, both in initial state 'I'
    stfc.newStf("foo", "bar");
    stfc.newStf("foo2", "bar2");
    // Change the state of 1 data item to 'P'
    Long stfId = StfContext.safeGetLaStfIdValue();
    // This changes the initial state of Stf: I->P, and the following process is the same!
    // if (isNormal) {
    // stf.doForward(stfId);
    // }

    // tricky help test
    Map<String, Object> obj = null;
    JdbcTemplate jdbc = null;
    if (biz instanceof JdbcAware) {
      jdbc = extractNativeJdbcOps((JdbcAware)biz);
      obj = jdbc.queryForMap("select * from " + tblName + " where st=? limit 1", st.name());
    }
    // do the real test
    long now = System.currentTimeMillis();
    jdbc.update("update " + tblName + " set timeout_at = ?", now);
    Stream<Stf> stfs = null;
    try {
      stfs = biz.scanTimeoutCoreJobs(1);
      Stf[] stfArr = stfs.toArray(Stf[]::new);
      assert stfArr.length == 1 && st.name().equals(stfArr[0].getSt())
          : "should find 1 timeout job when timeout just happened";
    } finally {
      if (stfs != null) {
        stfs.close();
      }
    }

    now = System.currentTimeMillis();
    jdbc.update("update " + tblName + " set timeout_at = ?", now + 500);// +1000?
    Stream<Stf> stfs2 = null;
    try {
      stfs2 = biz.scanTimeoutCoreJobs(1);
      assert stfs2.count() == 0 : "should find 0 timeout job when timeout just not happened";
    } finally {
      if (stfs2 != null) {
        stfs2.close();
      }
    }
  }

  long safeGetUpAt(Map<String, Object> obj) {
    long realDbUpAt = ((Number)obj.get("up_at")).longValue();
    return realDbUpAt;
  }

  public JobScannerCase(GenericContainer db, String tblName) {
    super(db, tblName);
  }

  public JobScannerCase(GenericContainer db) {
    super(db);
  }
}