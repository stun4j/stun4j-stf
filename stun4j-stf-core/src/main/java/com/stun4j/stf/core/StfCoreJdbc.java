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
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.YesNoEnum.Y;
import static com.stun4j.stf.core.support.StfHelper.H;

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.support.StfDoneEvent;
import com.stun4j.stf.core.support.StfEventBus;
import com.stun4j.stf.core.support.StfHelper;
import com.stun4j.stf.core.utils.consumers.BaseConsumer;
import com.stun4j.stf.core.utils.consumers.Consumer;
import com.stun4j.stf.core.utils.consumers.PairConsumer;
import com.stun4j.stf.core.utils.consumers.QuadruConsumer;
import com.stun4j.stf.core.utils.consumers.SextuConsumer;
import com.stun4j.stf.core.utils.consumers.TriConsumer;

/**
 * The jdbc implementation of {@link BaseStfCore}
 * @author Jay Meng
 */
public class StfCoreJdbc extends BaseStfCore {
  private final String INIT_SQL;
  private final String FORWARD_SQL;
  private final String RETRY_FORWARD_SQL;
  private final String MARK_DEAD_SQL;
  private final String MARK_DONE_SQL;
  private final String LOCK_SQL;

  private final StfJdbcOps jdbcOps;
  private final SextuConsumer<Long, Pair<String, Object[]>, Integer, Boolean, Boolean, BaseConsumer<Long>> coreFn = (
      stfId, calleePair, lastRetryTimes, async, batch, bizFn) -> {
    if (checkFail(stfId)) {
      return;
    }
    String calleeInfo = calleePair.getKey();
    Object[] calleeMethodArgs = calleePair.getValue();
    if (!async) {
      invokeConsumer(stfId, calleeInfo, calleeMethodArgs, lastRetryTimes, batch, bizFn);
      return;
    }
    worker.execute(() -> invokeConsumer(stfId, calleeInfo, calleeMethodArgs, lastRetryTimes, batch, bizFn));
  };
  private static final Pair<String, Object[]> EMPTY_CALLEE_PAIR = Pair.of(null, null);

  @Deprecated
  private final TriConsumer<Long, String, Object[]> forward = (stfId, calleeInfo, calleeMethodArgs) -> {
    if (!doForward(stfId)) {
      LOG.error("The stf#{} can't be triggered forward", stfId);
      return;
    }
    invokeCall(stfId, calleeInfo, calleeMethodArgs);
  };

  private final QuadruConsumer<Long, String, Object[], Integer> reForward = (stfId, calleeInfo, calleeMethodArgs,
      lastRetryTimes) -> {
    // if (!doReForward(stfId, lastRetryTimes)) {
    // LOG.warn("The stf#{} can't be re-forward", stfId);
    // return;
    // }
    invokeCall(stfId, calleeInfo, calleeMethodArgs);
  };

  private final PairConsumer<Long, Boolean> markDone = (stfId, batch) -> {
    if (!doMarkDone(stfId, batch)) {
      LOG.error("The stf#{} can't be marked done", stfId);
    }
  };

  private final Consumer<Long> markDead = stfId -> {
    doMarkDead(stfId);
  };

  @Override
  public void forward(Long stfId, String calleeInfo, boolean async, Object... calleeMethodArgs) {
    coreFn.accept(stfId, Pair.of(calleeInfo, calleeMethodArgs), null, async, false, forward);
  }

  @Override
  public boolean lockStf(Long stfId, int timeoutSecs, int curRetryTimes) {
    if (checkFail(stfId)) {
      return false;
    }
    return doLockStf(stfId, timeoutSecs, curRetryTimes);
  }

  @Override
  public void markDone(Long stfId, boolean async) {
    coreFn.accept(stfId, EMPTY_CALLEE_PAIR, null, async, true, markDone);
  }

  @Override
  public void markDead(Long stfId, boolean async) {
    coreFn.accept(stfId, EMPTY_CALLEE_PAIR, null, async, false, markDead);
  }

  @Override
  public void reForward(Long stfId, int lastRetryTimes, String calleeInfo, boolean async, Object... calleeMethodArgs) {
    coreFn.accept(stfId, Pair.of(calleeInfo, calleeMethodArgs), lastRetryTimes, async, false, reForward);
  }

  @Override
  protected boolean doLockStf(Long stfId, int timeoutSecs, int curRetryTimes) {
    if (H.isDataSourceClose()) {
      LOG.warn("[doLockStf] The dataSource has been closed and the operation on stf#{} is cancelled.", stfId);
      return false;
    }
    long now;
    int cnt = jdbcOps.update(LOCK_SQL, (now = System.currentTimeMillis()) + timeoutSecs * 1000, now, stfId,
        curRetryTimes);
    return cnt == 1;
  }

  @Override
  public int[] doBatchLockStfs(List<Object[]> batchArgs) {
    if (H.isDataSourceClose()) {
      LOG.warn("[doBatchLockStfs] The dataSource has been closed and the operations on stfs is cancelled.");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(LOCK_SQL, batchArgs);
    return res;
  }

  @Override
  protected void doNewStf(Long newStfId, StfCall callee, int timeoutSecs) {
    if (H.isDataSourceClose()) {
      LOG.warn("[doInit] The dataSource has been closed and the operation on stf#{} is cancelled.", newStfId);
      return;
    }
    String calleeJson = JsonHelper.toJson(callee);
    long now = System.currentTimeMillis();
    jdbcOps.update(INIT_SQL, newStfId, calleeJson, timeoutSecs, (now + timeoutSecs * 1000), now, now);
  }

  @Override
  protected void doMarkDead(Long stfId) {
    if (H.isDataSourceClose()) {
      LOG.warn("[doMarkDead] The dataSource has been closed and the operation on stf#{} is cancelled.", stfId);
      return;
    }
    jdbcOps.update(MARK_DEAD_SQL, System.currentTimeMillis(), stfId);
  }

  @Override
  public boolean doMarkDone(Long stfId, boolean batch) {
    if (H.isDataSourceClose()) {
      LOG.warn("[doMarkDone] The dataSource has been closed and the operation on stf#{} is cancelled.", stfId);
      return false;
    }
    if (batch) {
      StfEventBus.post(new StfDoneEvent(stfId));
      return true;
    }
    int cnt = jdbcOps.update(MARK_DONE_SQL, System.currentTimeMillis(), stfId);
    return cnt == 1;
  }

  @Override
  public int[] batchMarkDone(List<Object[]> stfIdsInfo) {
    if (H.isDataSourceClose()) {
      LOG.warn("[batchMarkDone] The dataSource has been closed and the operations on stfs is cancelled.");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(MARK_DONE_SQL, stfIdsInfo);
    return res;
  }

  @Deprecated
  @Override
  public boolean doForward(Long stfId) {
    long now;
    int cnt = jdbcOps.update(FORWARD_SQL, now = System.currentTimeMillis(), now, stfId);
    return cnt == 1;
  }

  @Deprecated
  @Override
  protected boolean doReForward(Long stfId, int curRetryTimes) {
    long now;
    int cnt = jdbcOps.update(RETRY_FORWARD_SQL, now = System.currentTimeMillis(), now, stfId, curRetryTimes);
    return cnt == 1;
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps) {
    this(jdbcOps, DFT_CORE_TBL_NAME);
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps, String tblName) {
    StfHelper.init(jdbcOps);
    this.jdbcOps = jdbcOps;

    String initTemplateSql = lenientFormat(
        "insert into %s (id, callee, st, is_dead, retry_times, timeout_secs, timeout_at, ct_at, up_at) values(?, ?, '%s', '%s', %s, ?, ?, ?, ?)",
        tblName);
    INIT_SQL = lenientFormat(initTemplateSql, I.name(), N.name(), 0);
    // TODO mj:deprecated->
    FORWARD_SQL = lenientFormat(
        "update %s set st = '%s', timeout_at = ? + (timeout_at - up_at), up_at = ? where id = ? and st in ('%s', '%s')",
        tblName, P.name(), I.name(), P.name());

    RETRY_FORWARD_SQL = new Supplier<String>() {
      @Override
      public String get() {
        String[] tmp = FORWARD_SQL.split(" where ");
        String upPart = tmp[0];
        String condPart = tmp[1];
        upPart += ", retry_times = retry_times + 1 ";
        condPart += " and retry_times = ?";
        return upPart + " where " + condPart;
      }
    }.get();
    // <-
    MARK_DEAD_SQL = lenientFormat("update %s set is_dead = '%s', up_at = ? where id = ? and st != '%s'", tblName,
        Y.name(), S.name());
    MARK_DONE_SQL = lenientFormat("update %s set st = '%s', up_at = ? where id = ? and st != '%s'", tblName, S.name(),
        F.name());

    LOCK_SQL = lenientFormat(
        "update %s set st = '%s', retry_times = retry_times + 1, timeout_at = ?, up_at = ? where id = ? and retry_times = ? and st in ('%s', '%s')",
        tblName, P.name(), I.name(), P.name());
  }

  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }

  private void invokeConsumer(Long stfId, String calleeInfo, Object[] calleeMethodArgs, Integer curRetryTimes,
      boolean batch, BaseConsumer<Long> bizFn) {
    if (bizFn instanceof Consumer) {
      ((Consumer<Long>)bizFn).accept(stfId);
    } else if (bizFn instanceof PairConsumer) {
      ((PairConsumer<Long, Boolean>)bizFn).accept(stfId, batch);
    } else if (bizFn instanceof TriConsumer) {
      ((TriConsumer<Long, String, Object[]>)bizFn).accept(stfId, calleeInfo, calleeMethodArgs);
    } else if (bizFn instanceof QuadruConsumer) {
      ((QuadruConsumer<Long, String, Object[], Integer>)bizFn).accept(stfId, calleeInfo, calleeMethodArgs,
          curRetryTimes);
    }
  }

}