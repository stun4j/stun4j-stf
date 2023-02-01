package com.stun4j.stf.core.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.guid.core.utils.Utils;
import com.stun4j.stf.core.BaseContainerCase;
import com.stun4j.stf.core.State;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfCall;
import com.stun4j.stf.core.StfConsts;
import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfCoreCase;
import com.stun4j.stf.core.StfDelayQueueCore;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.YesNo;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.spi.StfJdbcOps.StfJdbcRowMapper;
import com.stun4j.stf.core.support.registry.StfDefaultPOJORegistry;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class JobRunnerCase extends BaseContainerCase<JobRunner> {

  static {
    LocalGuid.init(0, 0);
  }

  public static final StfJdbcRowMapper<Stf> STF_ROW_MAPPER = JobScannerJdbc.STF_ROW_MAPPER_FN.apply(false,
      ArrayUtils.EMPTY_STRING_ARRAY);

  @Test
  public void _01_jobMaxRetryTillDead() {
    int timeoutSecs = 2;
    Map<Integer, Integer> retryBehav = customeRetryBehavior(timeoutSecs);
    JobRunner jobr = JobRunner.init(retryBehav);

    // job got timeout for the first time->
    StfCore stfc = newStfCore(null);
    JobScanner scanner = newJobScanner(null);
    @SuppressWarnings("unchecked")
    Long stfId = stfc.newStf("foo", "bar", timeoutSecs);
    assert !scanner.scanTimeoutCoreJobs(1).findFirst().isPresent() : "new job shouldn't timeout";
    Utils.sleepSeconds(timeoutSecs);
    // <-

    // 3 times retry following timeout
    StfMetaGroup metaGrp = StfMetaGroup.CORE;
    for (int curRetryTimes = 1; curRetryTimes <= 3; curRetryTimes++) {
      Stf job = scanner.scanTimeoutCoreJobs(1).findFirst().get();
      assert stfId.equals(job.getId()) : "should find 1 timeout job";
      if (curRetryTimes == 1) {
        assert job.getSt().equals(State.I.name()) : "should be an waiting-run job";
      } else {
        assert job.getSt().equals(State.P.name()) : "should be an in-progress job";
      }
      int curTimeoutSecs = mockCheckAndLock(jobr, retryBehav, stfc, scanner, stfId, job, metaGrp, curRetryTimes)
          .getLeft();

      // job got timeout
      Utils.sleepSeconds(curTimeoutSecs);
    }

    // retry over 3 times,then assert 'dead'
    Stf job = scanner.scanTimeoutCoreJobs(1).findFirst().get();
    assert stfId.equals(job.getId()) : "should be the same job";
    assert job.getSt().equals(State.P.name()) : "should be an in-progress job";

    int curTimeoutSecs = mockCheckAndLock(jobr, retryBehav, stfc, scanner, stfId, job, metaGrp, 4).getLeft();
    assert curTimeoutSecs == -1 : "shouldn't have timeout info";

    Utils.sleepSeconds(timeoutSecs);// give async markdead-op a little while
    assert !scanner.scanTimeoutCoreJobs(1).findFirst().isPresent() : "shouldn't still be alive";

    StfJdbcOps jdbcOps = ((JobScannerJdbc)scanner).getJdbcOps();
    String sql = "select * from " + this.tblName + " where id = ?";
    Stream<Stf> stfs = jdbcOps.queryForStream(sql, new Object[]{stfId}, STF_ROW_MAPPER);
    Stf deadJob = stfs.findFirst().get();
    assert YesNo.Y.name().equals(deadJob.getIsDead()) : "should be dead job";
    int maxRetryTimes = retryBehav.size();
    assert deadJob.getRetryTimes() == maxRetryTimes : "should exceeded max retry-times";
  }

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  @SuppressWarnings("unchecked")
  @Test
  public void _02_delayJobTransfer() throws FileNotFoundException {
    StfCore stfc = newStfCore();
    StfDelayQueueCore sftDlqc = newStfDelayQueueCore(stfc);
    JobScanner jobsc = newJobScanner(stfc);
    String mockBizObjId = "foo2";
    StfCall callee = StfCoreCase.delegateNewStfCallee(StfMetaGroup.DELAY, mockBizObjId, "toString");

    int timeoutSecs = 2;
    int delaySecs = 3;
    // found delay-job need to be triggered
    Long stfId = sftDlqc.newDelayStf(callee, timeoutSecs, delaySecs);
    assert !jobsc.scanTimeoutDelayJobs(1).findFirst().isPresent() : "new delay-job shouldn't being get transferred";
    Utils.sleepSeconds(delaySecs);// mock delay

    Stf delayJob = jobsc.scanTimeoutDelayJobs(1).findFirst().get();
    assert stfId.equals(delayJob.getId()) : "should find 1 delay-job need to be triggered";

    // delay-job got successfully locked
    Map<Integer, Integer> retryBehav = customeRetryBehavior(timeoutSecs);
    JobRunner jobr = JobRunner.init(retryBehav);
    // String jobGrp = JobConsts.JOB_GROUP_TIMEOUT_DELAY_WAITING_RUN;
    StfMetaGroup metaGrp = StfMetaGroup.DELAY;
    int curRetryTimes = 1;
    Pair<Integer, Long> lockedInfo = mockCheckAndLock(jobr, retryBehav, stfc, jobsc, stfId, delayJob, metaGrp,
        curRetryTimes);
    int lockedDelayJobTimeoutSecs = lockedInfo.getLeft();
    long delayJobLockedAt = lockedInfo.getRight();

    StfJdbcOps jdbcOps = ((JobScannerJdbc)jobsc).getJdbcOps();
    String sql = "select * from " + this.tblName + StfConsts.DFT_DELAY_TBL_NAME_SUFFIX + " where id = ?";
    Stream<Stf> stfDls = jdbcOps.queryForStream(sql, new Object[]{stfId}, STF_ROW_MAPPER);
    Stf lockedDelayJob = stfDls.findFirst().get();
    //@formatter:off
    assert State.P == State.valueOf(lockedDelayJob.getSt()) : "locked delay-job's status should be 'P";//TODO mj: move to '#mockCheckAndLock'
    assert (delayJobLockedAt + lockedDelayJobTimeoutSecs * 1000) == lockedDelayJob
        .getTimeoutAt() : "locked delay-job's timeoutAt should be 'lockedAt + timeoutSecs * 1000'";//TODO mj: move to '#mockCheckAndLock'
    //@formatter:on

    // now start transferring delay-job and do the final trigger
    // do some prepare->
    StfContext.init(stfc, new StfDefaultPOJORegistry());
    String signalMsg = mockBizObjId + "#toString got invoked...";
    StfContext.putBizObj(mockBizObjId, new Object() {
      @Override
      public String toString() {
        LOG.info(signalMsg);
        // StfContext.commitLastDoneWithoutTx();TODO mj:for this work,we need to init StfEventBus
        return super.toString();
      }
    });
    // <-
    // the real transfer&trigger happens here->
    JobRunner.doHandleTimeoutJob(StfMetaGroup.DELAY, lockedDelayJob, stfc);
    // <-
    Utils.sleepSeconds(2);
    try (Scanner scanner = new Scanner(new File("logs/info.log")).useDelimiter("\n")) {
      int times = 0;
      while (scanner.hasNext()) {
        String curLine;// the log file is shared,so we need to do the distinguish via class name(TODO mj:may changed...)
        if ((curLine = scanner.next()).contains(signalMsg) && curLine.contains(this.getClass().getSimpleName())) {
          times++;
        }
      }
      assert times == 1 : "mocked delay-callee '" + mockBizObjId + "#toString' shoule be invoked";
    }
    sql = "select * from " + this.tblName + " where id = ?";
    Stream<Stf> stfs = jdbcOps.queryForStream(sql, new Object[]{stfId}, STF_ROW_MAPPER);
    Stf transAndTriggeredJob = stfs.findFirst().get();
    assert State.I == State
        .valueOf(transAndTriggeredJob.getSt()) : "transferred delay-job(became a stf)'s init-status should be 'I";
    assert 0 == transAndTriggeredJob
        .getRetryTimes() : "normal triggered delay-job(became a stf)'s retry-times should be 0";
    // key timeline assert
    long transferredAt = transAndTriggeredJob.getCtAt();
    assert delayJobLockedAt < transferredAt : "delayJobLockedAt < transferredAt(the corresponding stf's ctAt)";
    System.out.printf("stfId:%s\n delayJobLockedTime: %s\n transferredTime:    %s\n", stfId,
        StfConsts.WITH_MS_DATE_FMT.format(delayJobLockedAt), StfConsts.WITH_MS_DATE_FMT.format(transferredAt));

  }

  static Map<Integer, Integer> customeRetryBehavior(int timeoutSecs) {
    Map<Integer, Integer> map = new HashMap<>();
    map.put(1, 0);// will be fixed to min-timeout-secs
    map.put(2, (int)(1.5 * timeoutSecs));
    map.put(3, 2 * timeoutSecs);
    return map;
  }

  static Pair<Integer, Long> mockCheckAndLock(JobRunner jobr, Map<Integer, Integer> retryBehav, StfCore stfc,
      JobScanner scanner, Long stfId, Stf job, StfMetaGroup metaGrp, int curRetryTimes) {
    // pre-check before lock->
    Pair<Boolean, Integer> curRunInfo = jobr.checkWhetherTheJobCanRun(metaGrp, job, stfc);
    if (curRetryTimes <= 3) {
      assert curRunInfo.getKey() : "should be able to run";
    } else {
      assert !curRunInfo.getKey() : "shouldn't be able to run";
      assert curRunInfo.getValue() == null : "shouldn't have timeout info";
      return Pair.of(-1, null);
    }
    int curTimeoutSecs = curRunInfo.getValue();
    int minTimeoutSecs = job.getTimeoutSecs();
    // @formatter:off
    if (curRetryTimes == 1) {
      assert curTimeoutSecs == minTimeoutSecs : "retry-times:timeout not matched";// retry-times:timeout map.put(1, 0); 0 should be fixed to min-timeout-secs
    } else if (curRetryTimes == 2) {
      assert curTimeoutSecs == 1.5 * minTimeoutSecs : "retry-times:timeout not matched";// retry-times:timeout map.put(2, 1 * minTimeoutSecs);
    } else if (curRetryTimes == 3) {
      assert curTimeoutSecs == 2 * minTimeoutSecs : "retry-times:timeout not matched";// retry-times:timeout map.put(3, 1.5 * minTimeoutSecs);
    }
    // @formatter:on
    // <-

    // lock the job->
    int lastRetryTimes = job.getRetryTimes();
    if (curRetryTimes == 1) {
      assert lastRetryTimes == 0 : "job lastRetryTimes should be 0";
    } else if (curRetryTimes == 2) {
      assert lastRetryTimes == 1 : "job lastRetryTimes should be 1";
    } else if (curRetryTimes == 3) {
      assert lastRetryTimes == 2 : "job lastRetryTimes should be 2";
    }
    long lastTimeoutAt = job.getTimeoutAt();
    long lockedAt = stfc.lockStf(metaGrp, stfId, curTimeoutSecs, lastRetryTimes, lastTimeoutAt);
    assert lockedAt > 0 : "should lock the job";
    assert !scanner.scanTimeoutCoreJobs(1).findFirst().isPresent() : "job shouldn't timeout immediately after the lock";
    return Pair.of(curTimeoutSecs, lockedAt);
    // <-
  }

  protected JobRunnerCase(GenericContainer db) {
    super(db);
  }

  public JobRunnerCase(GenericContainer db, String tblName) {
    super(db, tblName);
  }

}