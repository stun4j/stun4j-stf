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

import com.google.common.collect.Maps;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.BaseContainerCase;
import com.stun4j.stf.core.BaseStfCore;
import com.stun4j.stf.core.StateEnum;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseJobScannerCase extends BaseContainerCase<JobScanner> {
  static final Map<Class<? extends BaseContainerCase>, JobScanner> JOB_SCANNER_COMPONENTS = Maps.newConcurrentMap();

  static {
    LocalGuid.init(0, 0);
  }

  @Override
  public JobScanner bizBean() {
    return JOB_SCANNER_COMPONENTS.computeIfAbsent(this.getClass(), (k) -> {
      if (this.isContainerTypeJdbc()) {
        JdbcTemplate jdbcOps = newJdbcTemplate(db);
        return new JobScannerJdbc(new StfDefaultSpringJdbcOps(jdbcOps), tblName);
      }
      throw new RuntimeException("biz bean init error");
    });
  }

  @Test
  public void _01_basicConnectivity_optRtnFields() {
    _01_06_template(true);
  }

  @Test
  public void _02_timeout_scanTimeoutJobsWaitingRun() {
    _02_03_07_08_template(StateEnum.I, true);
  }

  private void _01_06_template(boolean isNormal) {
    JobScanner biz = bizBean();
    BaseStfCore stf = newStfCore(biz);

    long theVeryBegining = System.currentTimeMillis();
    if (isNormal) {
      stf.init("foo", "bar");
    }
    if (isNormal) {
      try (Stream<Stf> stfs = biz.scanTimeoutJobsWaitingRun(0, 1, false)) {
        assert stfs.count() == 1 : "should find 1 job";
      }
    }

    if (isNormal) {
      try (Stream<Stf> stfs = biz.scanTimeoutJobsStillAlive(0, 1, false, "id", "up_at")) {
        Stf[] stfArray = stfs.toArray(Stf[]::new);
        assert stfArray.length == 1 : "should find 1 job";
        Stf obj = stfArray[0];

        // getID==StfContext.laStfId()It also means that the correct value of id has been obtained
        // upAt >begin Indicates that the correct upAt value is obtained. RT of db is taken into
        // account，This value also asserts >=theVeryBegining
        assert obj.getId().equals(StfContext.laStfId()) && obj.getSt() == null
            && obj.getUpAt() >= theVeryBegining : "should job contains specified fields only";
      }
    }

    // Only sql programmar is tested
    if (isNormal) {
      try (Stream<Stf> stfs = biz.scanTimeoutJobsRunning(0, 1, false)) {
      }
    }
  }

  private void _02_03_07_08_template(StateEnum jobType, boolean isNormal) {
    JobScanner biz = bizBean();
    BaseStfCore stf = newStfCore(biz);
    // Initialize two pieces of data, both in initial state 'I'
    if (isNormal) {
      stf.init("foo", "bar");
      stf.init("foo2", "bar2");
    }
    // Change the state of 1 data item to 'P'
    Long stfId = StfContext.laStfId();
    // This changes the initial state of Stf: I->P, and the following process is the same!
    if (isNormal) {
      stf.doForward(stfId);
    }

    // tricky help test
    Map<String, Object> obj = null;
    if (biz instanceof JdbcAware) {
      JdbcTemplate jdbc = extractNativeJdbcOps((JdbcAware)biz);
      if (isNormal) {
        obj = jdbc.queryForMap("select * from " + tblName + " where st=? limit 1", jobType.name());
      }
    }
    long realDbUpAt = safeGetUpAt(obj);
    // do the real test
    long start = System.currentTimeMillis();
    // System.out.println("time(ms) now: " + start);
    // Minimum timeout period for query results(This value is derived inversely from db, so it is used as the threshold
    // for the timeout)
    long shortestTimeout = start - realDbUpAt;
    Stream<Stf> stfs = null;
    try {
      if (StateEnum.I == jobType) {
        if (isNormal) {
          stfs = biz.scanTimeoutJobsWaitingRun(shortestTimeout, 1, false);
        }
      } else {
        if (isNormal) {
          stfs = biz.scanTimeoutJobsRunning(shortestTimeout, 1, false);
        }
      }
      Stf[] stfArray = stfs.toArray(Stf[]::new);
      assert stfArray.length == 1
          && jobType.name().equals(stfArray[0].getSt()) : "should find 1 timeout job when timeout just happened";
    } finally {
      if (stfs != null) {
        stfs.close();
      }
    }

    long now = System.currentTimeMillis();
    // System.out.println("time(ms) passed: " + (now - start));
    // time passed by,So shortestTimeout is recalculated and then incremented by 1ms.(Deliberately extended the timeout
    // threshold just a little bit, so you can't expect to find anything)
    shortestTimeout = now - realDbUpAt;
    Stream<Stf> stfs2 = null;
    try {
      if (StateEnum.I == jobType) {
        if (isNormal) {
          stfs2 = biz.scanTimeoutJobsWaitingRun(shortestTimeout + 1, 1, false);
        }
      } else {
        if (isNormal) {
          stfs2 = biz.scanTimeoutJobsRunning(shortestTimeout + 1, 1, false);
        }
      }
      assert stfs2.count() == 0 : "should find 0 timeout job when timeout just not-happened";
    } finally {
      if (stfs2 != null) {
        stfs2.close();
      }
    }

    now = System.currentTimeMillis();
    shortestTimeout = now - realDbUpAt;
    Stream<Stf> stfs3 = null;
    try {
      if (StateEnum.I == jobType) {
        if (isNormal) {
          stfs3 = biz.scanTimeoutJobsWaitingRun(0, 1, true);
        }
      } else {
        if (isNormal) {
          stfs3 = biz.scanTimeoutJobsRunning(0, 1, true);
        }
      }
      assert stfs3.count() == 0 : "should find 0 timeout running job,because we never create any of this kinda job";
    } finally {
      if (stfs3 != null) {
        stfs3.close();
      }
    }
  }

  long safeGetUpAt(Map<String, Object> obj) {
    long realDbUpAt = ((Number)obj.get("up_at")).longValue();
    return realDbUpAt;
  }

  public BaseJobScannerCase(GenericContainer db, String tblName) {
    super(db, tblName);
  }

  public BaseJobScannerCase(GenericContainer db) {
    super(db);
  }
}