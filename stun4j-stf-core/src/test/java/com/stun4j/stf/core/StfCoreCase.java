package com.stun4j.stf.core;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.GenericContainer;

import com.google.common.base.Strings;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.job.JobScanner;
import com.stun4j.stf.core.utils.Utils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class StfCoreCase extends BaseContainerCase<StfCore> {

  static {
    LocalGuid.init(0, 0);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void _01_lockTwiceNotAllowed() {
    StfCore stfc = newStfCore();
    JobScanner scanner = newJobScanner(stfc);

    int timeoutSecs = 1;// given a shortest timeout
    Long stfId = stfc.newStf("foo", "bar", timeoutSecs);
    Utils.sleepSeconds(timeoutSecs);

    Stream<Stf> stfs = scanner.scanTimeoutCoreJobs(1);
    Stf stf = stfs.findFirst().get();
    long timeoutAt = stf.getTimeoutAt();

    // String grp = JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
    StfMetaGroup metaGrp = StfMetaGroup.CORE;
    long lockedAt = stfc.lockStf(metaGrp, stfId, timeoutSecs, 0, timeoutAt);
    assert lockedAt > 0 : "the timeout job should be locked";
    lockedAt = stfc.lockStf(metaGrp, stfId, timeoutSecs, 0, timeoutAt);
    assert lockedAt == -1 : "the job just locked shouldn't be locked again";
  }

  @Test
  public void _02_lockMoreTimesNotAllowedHighConcurrently() throws InterruptedException {
    StfCore stfc = newStfCore();
    JobScanner scanner = newJobScanner(stfc);

    int timeoutSecs = 1;// given a shortest timeout
    Long stfId = stfc.newStf("foo", "bar", timeoutSecs);
    Utils.sleepSeconds(timeoutSecs);

    Stream<Stf> stfs = scanner.scanTimeoutCoreJobs(2).filter(job -> job.getId().equals(stfId));
    Stf stf = stfs.findFirst().get();
    long timeoutAt = stf.getTimeoutAt();

    // String grp = JobConsts.JOB_GROUP_TIMEOUT_WAITING_RUN;
    StfMetaGroup metaGrp = StfMetaGroup.CORE;
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
          long lockedAt = stfc.lockStf(metaGrp, stfId, timeoutSecs, 0, timeoutAt);
          if (lockedAt > 0) {
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

  public static StfCall delegateNewStfCallee(StfMetaGroup metaGrp, String bizObjId, String bizMethodName,
      @SuppressWarnings("unchecked") Pair<?, Class<?>>... typedArgs) {
    StfCall callee = StfCall.newCallee(metaGrp, bizObjId, bizMethodName, typedArgs);
    return callee;
  }

  public StfCoreCase(GenericContainer db, String tblName) {
    super(db, tblName);
  }

  protected StfCoreCase(GenericContainer db) {
    super(db);
  }

}
