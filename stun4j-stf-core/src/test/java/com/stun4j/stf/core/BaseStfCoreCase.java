package com.stun4j.stf.core;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import com.google.common.collect.Maps;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.utils.Strings;
import com.stun4j.guid.core.utils.Utils;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfCoreJdbc;
import com.stun4j.stf.core.job.JobConsts;
import com.stun4j.stf.core.job.JobScanner;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseStfCoreCase extends BaseContainerCase<StfCore> {
  static final Map<Class<? extends BaseContainerCase>, StfCore> STF_CORE_COMPONENTS = Maps.newConcurrentMap();

  static {
    LocalGuid.init(0, 0);
  }

  @Override
  public StfCore bizBean() {
    return STF_CORE_COMPONENTS.computeIfAbsent(this.getClass(), (k) -> {
      if (this.isContainerTypeJdbc()) {
        JdbcTemplate jdbcOps = newJdbcTemplate(db);
        return new StfCoreJdbc(new StfDefaultSpringJdbcOps(jdbcOps), tblName);
      }
      throw new RuntimeException("biz bean init error");
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void _01_lockTwiceNotAllowed() {
    StfCore stfc = bizBean();
    JobScanner scanner = newJobScanner(stfc);

    int timeoutSecs = 1;// given a shortest timeout
    Long stfId = stfc.newStf("foo", "bar", timeoutSecs);
    Utils.sleepSeconds(timeoutSecs);

    Stream<Stf> stfs = scanner.scanTimeoutCoreJobsWaitingRun(1);
    Stf stf = stfs.findFirst().get();
    long timeoutAt = stf.getTimeoutAt();

    String grp = JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
    boolean locked = stfc.lockStf(grp, stfId, timeoutSecs, 0, timeoutAt);
    assert locked : "the timeout job should be locked";
    locked = stfc.lockStf(grp, stfId, timeoutSecs, 0, timeoutAt);
    assert !locked : "the job just locked shouldn't be locked again";
  }

  @Test
  public void _02_lockMoreTimesNotAllowedHighConcurrently() throws InterruptedException {
    StfCore stfc = bizBean();
    JobScanner scanner = newJobScanner(stfc);

    int timeoutSecs = 1;// given a shortest timeout
    Long stfId = stfc.newStf("foo", "bar", timeoutSecs);
    Utils.sleepSeconds(timeoutSecs);

    Stream<Stf> stfs = scanner.scanTimeoutCoreJobsWaitingRun(1);
    Stf stf = stfs.findFirst().get();
    long timeoutAt = stf.getTimeoutAt();

    String grp = JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
    int n = 50;
    AtomicInteger cnt = new AtomicInteger();
    CyclicBarrier gate = new CyclicBarrier(n);
    Thread[] threads = new Thread[n];
    for (int i = 0; i < n; i++) {
      threads[i] = new Thread(() -> {
        try {
          gate.await();
          System.out
              .println(Strings.lenientFormat("Thread[%s] trying lock the job...", Thread.currentThread().getName()));
          boolean locked = stfc.lockStf(grp, stfId, timeoutSecs, 0, timeoutAt);
          if (locked) {
            cnt.incrementAndGet();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
      threads[i].start();
    }
    for (int i = 0; i < n; i++) {
      threads[i].join();
    }
    assert cnt.get() == 1 : "exactly 1 job should be locked";
  }

  public BaseStfCoreCase(GenericContainer db, String tblName) {
    super(db, tblName);
  }

  protected BaseStfCoreCase(GenericContainer db) {
    super(db);
  }

}
