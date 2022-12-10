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
package com.stun4j.stf.core;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StateEnum.F;
import static com.stun4j.stf.core.StateEnum.I;
import static com.stun4j.stf.core.StateEnum.P;
import static com.stun4j.stf.core.StateEnum.S;
import static com.stun4j.stf.core.StfConsts.DFT_CORE_TBL_NAME;
import static com.stun4j.stf.core.StfConsts.DFT_DELAY_TBL_NAME_SUFFIX;
import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
import static com.stun4j.stf.core.StfMetaGroupEnum.DELAY;
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.YesNoEnum.Y;
import static com.stun4j.stf.core.job.JobConsts.KEY_FEATURE_TIMEOUT_DELAY;
import static org.apache.commons.lang3.tuple.Pair.of;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.support.event.StfDelayTriggeredEvent;
import com.stun4j.stf.core.support.event.StfDoneEvent;
import com.stun4j.stf.core.support.event.StfEventBus;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.consumers.BaseConsumer;
import com.stun4j.stf.core.utils.consumers.PairConsumer;
import com.stun4j.stf.core.utils.consumers.QuadruConsumer;
import com.stun4j.stf.core.utils.consumers.SextuConsumer;
import com.stun4j.stf.core.utils.consumers.TriConsumer;

/**
 * The jdbc implementation of {@link BaseStfCore}
 * @author Jay Meng
 */
public class StfCoreJdbc extends BaseStfCore implements JdbcAware {
  private static final Pair<String, Object[]> EMPTY_CALLEE_PAIR = of(null, null);
  private final String INIT_SQL;
  private final String INIT_DELAY_SQL;
  private final String MARK_DEAD_SQL;
  private final String MARK_DEAD_DELAY_SQL;
  private final String MARK_DONE_SQL;
  private final String MARK_DONE_DELAY_SQL;
  private final String LOCK_SQL;
  private final String LOCK_DELAY_SQL;

  private final String DELAY_TRANSFER_SQL;
  private String coreTblName;

  private final StfJdbcOps jdbcOps;
  private final SextuConsumer<Pair<Long, StfMetaGroupEnum>, Pair<String, Object[]>, Integer, Boolean, Boolean, BaseConsumer<Long>> coreFn = (
      stfMeta, calleePair, lastRetryTimes, async, batch, bizFn) -> {
    Long stfId = stfMeta.getLeft();
    if (checkFail(stfId)) {
      return;
    }
    String calleeInfo = calleePair.getKey();
    Object[] calleeMethodArgs = calleePair.getValue();
    if (!async) {
      invokeConsumer(stfMeta, calleeInfo, calleeMethodArgs, lastRetryTimes, false,
          bizFn);/*- This 'false' is somewhat weird,meaning that ‘Sync calls’ must also be non-batch */
      return;
    }
    worker.execute(() -> invokeConsumer(stfMeta, calleeInfo, calleeMethodArgs, lastRetryTimes, batch, bizFn));
  };

  private final QuadruConsumer<Long, StfMetaGroupEnum, String, Object[]> reForward = (stfId, metaGrp, calleeInfo,
      calleeMethodArgs) -> {
    if (metaGrp == CORE) {
      invokeCall(stfId, calleeInfo, calleeMethodArgs);
    } else {
      try {
        if (!doDelayTransfer(stfId)) {
          return;
        }
      } catch (DuplicateKeyException e) {// Shouldn't happen
        try {
          H.tryCommitLaStfOnDup(LOG, stfId, coreTblName, (DuplicateKeyException)e,
              laStfDelayId -> this.fallbackToSingleMarkDone(DELAY, laStfDelayId));
        } catch (Throwable e1) {
          Exceptions.swallow(e1, LOG, "Unexpected error occurred while auto committing stf-delay");
        }
        return;
      }
      this.markDone(DELAY, stfId, true);// Stf internally using Stf itself:)
      invokeCall(stfId, calleeInfo, calleeMethodArgs);
    }
  };

  private final TriConsumer<Long, StfMetaGroupEnum, Boolean> markDone = (metaGrp, stfId, batch) -> {
    if (!doMarkDone(stfId, metaGrp, batch)) {
      LOG.error("The stf#{} can't be marked done", stfId);
    }
  };

  private final PairConsumer<Long, StfMetaGroupEnum> markDead = (metaGrp, stfId) -> {
    doMarkDead(stfId, metaGrp);
  };

  @Override
  public boolean lockStf(String jobGrp, Long stfId, int timeoutSecs, int curRetryTimes, long curTimeoutAt) {
    if (checkFail(stfId)) {
      return false;
    }
    StfMetaGroupEnum metaGrp = jobGrp.indexOf(KEY_FEATURE_TIMEOUT_DELAY) == -1 ? CORE : DELAY;
    return doLockStf(metaGrp, stfId, timeoutSecs, curRetryTimes, curTimeoutAt);
  }

  @Override
  public void markDone(StfMetaGroupEnum metaGrp, Long stfId, boolean async) {
    coreFn.accept(of(stfId, metaGrp), EMPTY_CALLEE_PAIR, null, async, true, markDone);
  }

  @Override
  public void markDead(StfMetaGroupEnum metaGrp, Long stfId, boolean async) {
    coreFn.accept(of(stfId, metaGrp), EMPTY_CALLEE_PAIR, null, async, false, markDead);
  }

  @Override
  public void reForward(StfMetaGroupEnum metaGrp, Long stfId, int lastRetryTimes, String calleeInfo, boolean async,
      Object... calleeMethodArgs) {
    coreFn.accept(of(stfId, metaGrp), of(calleeInfo, calleeMethodArgs), lastRetryTimes, async, false, reForward);
  }

  @Override
  protected void doNewStf(Long newStfId, StfCall callee, int timeoutSecs) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doNewStf", of("stf", newStfId));
      return;
    }
    String calleeJson = JsonHelper.toJson(callee);
    long now = System.currentTimeMillis();
    jdbcOps.update(INIT_SQL, newStfId, calleeJson, timeoutSecs, (now + timeoutSecs * 1000), now, now);
  }

  @Override
  protected void doNewStfDelay(Long newStfDelayId, StfCall callee, int timeoutSecs, int delaySecs) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doNewStfDelay", of("stfd", newStfDelayId));
      return;
    }
    String calleeJson = JsonHelper.toJson(callee);
    long now = System.currentTimeMillis();
    jdbcOps.update(INIT_DELAY_SQL, newStfDelayId, calleeJson, timeoutSecs, (now + delaySecs * 1000), now, now);
  }

  @Override
  protected boolean doLockStf(StfMetaGroupEnum metaGrp, Long stfId, int timeoutSecs, int curRetryTimes,
      long curTimeoutAt) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doLockStf", of("stf", stfId));
      return false;
    }
    long now;
    int cnt = jdbcOps.update(metaGrp == CORE ? LOCK_SQL : LOCK_DELAY_SQL,
        (now = System.currentTimeMillis()) + timeoutSecs * 1000, now, stfId, curRetryTimes, curTimeoutAt);
    return cnt == 1;
  }

  @Override
  protected int[] doBatchLockStfs(StfMetaGroupEnum metaGrp, List<Object[]> batchArgs) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doBatchLockStfs");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(metaGrp == CORE ? LOCK_SQL : LOCK_DELAY_SQL, batchArgs);
    return res;
  }

  @Override
  protected boolean doMarkDone(StfMetaGroupEnum metaGrp, Long stfId, boolean batch) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doMarkDone", of("stf", stfId));
      return false;
    }
    if (batch) {
      StfEventBus.post(metaGrp == CORE ? new StfDoneEvent(stfId) : new StfDelayTriggeredEvent(stfId));
      return true;
    }
    int cnt = jdbcOps.update(metaGrp == CORE ? MARK_DONE_SQL : MARK_DONE_DELAY_SQL, System.currentTimeMillis(), stfId);
    return cnt == 1;
  }

  @Override
  public int[] batchMarkDone(StfMetaGroupEnum metaGrp, List<Object[]> stfIdsInfo) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "batchMarkDone");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(metaGrp == CORE ? MARK_DONE_SQL : MARK_DONE_DELAY_SQL, stfIdsInfo);
    return res;
  }

  @Override
  protected void doMarkDead(StfMetaGroupEnum metaGrp, Long stfId) {
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doMarkDead", of("stf", stfId));
      return;
    }
    jdbcOps.update(metaGrp == CORE ? MARK_DEAD_SQL : MARK_DEAD_DELAY_SQL, System.currentTimeMillis(), stfId);
  }

  @Override
  protected boolean doDelayTransfer(Long stfDelayId) {
    long now;
    int cnt = jdbcOps.update(DELAY_TRANSFER_SQL, now = System.currentTimeMillis(), now, stfDelayId);
    return cnt == 1;
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps) {
    this(jdbcOps, DFT_CORE_TBL_NAME);
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps, String coreTblName) {
    StfHelper.init(jdbcOps);
    this.jdbcOps = jdbcOps;
    String delayTblName = (this.coreTblName = coreTblName) + DFT_DELAY_TBL_NAME_SUFFIX;

    String initSqlTpl = "insert into %s (id, callee, st, is_dead, retry_times, timeout_secs, timeout_at, ct_at, up_at) values(?, ?, '%s', '%s', %s, ?, ?, ?, ?)";
    INIT_SQL = lenientFormat(initSqlTpl, coreTblName, I.name(), N.name(), 0);
    INIT_DELAY_SQL = lenientFormat(initSqlTpl, delayTblName, I.name(), N.name(), 0);

    String lockSqlTpl = "update %s set st = '%s', retry_times = retry_times + 1, timeout_at = ?, up_at = ? where id = ? and retry_times = ? and timeout_at = ? and st in ('%s', '%s')";
    LOCK_SQL = lenientFormat(lockSqlTpl, coreTblName, P.name(), I.name(), P.name());
    LOCK_DELAY_SQL = lenientFormat(lockSqlTpl, delayTblName, P.name(), I.name(), P.name());

    String markDoneSqlTpl = "update %s set st = '%s', up_at = ? where id = ? and st != '%s'";
    MARK_DONE_SQL = lenientFormat(markDoneSqlTpl, coreTblName, S.name(), F.name());
    MARK_DONE_DELAY_SQL = lenientFormat(markDoneSqlTpl, delayTblName, S.name(), F.name());

    String markDeadSqlTpl = "update %s set is_dead = '%s', up_at = ? where id = ? and st != '%s'";
    MARK_DEAD_SQL = lenientFormat(markDeadSqlTpl, coreTblName, Y.name(), S.name());
    MARK_DEAD_DELAY_SQL = lenientFormat(markDeadSqlTpl, delayTblName, Y.name(), S.name());

    DELAY_TRANSFER_SQL = lenientFormat(
        "insert into %s (id, callee, st, is_dead, retry_times, timeout_secs, timeout_at, ct_at, up_at) select id, callee, '%s', '%s', %s, timeout_secs, timeout_at, ?, ? from %s where id = ? and st = '%s'",
        coreTblName, P.name(), N.name(), 0, delayTblName, P.name());
  }

  @Override
  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }

  private void invokeConsumer(Pair<Long, StfMetaGroupEnum> stfMeta, String calleeInfo, Object[] calleeMethodArgs,
      Integer curRetryTimes, boolean batch, BaseConsumer<Long> bizFn) {
    Long stfId = stfMeta.getLeft();
    StfMetaGroupEnum metaGrp = stfMeta.getRight();
    if (bizFn instanceof PairConsumer) {
      ((PairConsumer<Long, StfMetaGroupEnum>)bizFn).accept(stfId, metaGrp);
    } else if (bizFn instanceof TriConsumer) {
      ((TriConsumer<Long, StfMetaGroupEnum, Boolean>)bizFn).accept(stfId, metaGrp, batch);
    } else if (bizFn instanceof QuadruConsumer) {
      ((QuadruConsumer<Long, StfMetaGroupEnum, String, Object[]>)bizFn).accept(stfId, metaGrp, calleeInfo,
          calleeMethodArgs);
    }
  }

}