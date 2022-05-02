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
package com.stun4j.stf.core.store;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StateEnum.F;
import static com.stun4j.stf.core.StateEnum.I;
import static com.stun4j.stf.core.StateEnum.P;
import static com.stun4j.stf.core.StateEnum.S;
import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.StfConsts.DFT_TBL_NAME;
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.YesNoEnum.Y;

import java.util.Optional;
import java.util.function.Supplier;

import com.stun4j.stf.core.BaseStfCore;
import com.stun4j.stf.core.StfCall;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.utils.consumers.BaseConsumer;
import com.stun4j.stf.core.utils.consumers.Consumer;
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

  private final SextuConsumer<Long, String, Object[], Integer, Boolean, BaseConsumer<Long>> coreFn = (stfId, calleeInfo,
      calleeMethodArgs, lastRetryTimes, async, bizFn) -> {
    if (checkFail(stfId)) {
      return;
    }
    if (!async) {
      invokeConsumer(stfId, calleeInfo, calleeMethodArgs, lastRetryTimes, bizFn);
      return;
    }
    worker.execute(() -> invokeConsumer(stfId, calleeInfo, calleeMethodArgs, lastRetryTimes, bizFn));
  };

  private final TriConsumer<Long, String, Object[]> forward = (stfId, calleeInfo, calleeMethodArgs) -> {
    if (!doForward(stfId)) {
      LOG.error("The stf#{} can't be triggered forward", stfId);
      return;
    }
    invokeCall(stfId, calleeInfo, calleeMethodArgs);
  };

  private final QuadruConsumer<Long, String, Object[], Integer> reForward = (stfId, calleeInfo, calleeMethodArgs,
      lastRetryTimes) -> {
    if (!doReForward(stfId, lastRetryTimes)) {
      LOG.warn("The stf#{} can't be re-forward", stfId);
      return;
    }
    invokeCall(stfId, calleeInfo, calleeMethodArgs);
  };

  private final Consumer<Long> markDone = stfId -> {
    if (!doMarkDone(stfId)) {
      LOG.error("The stf#{} can't be marked done", stfId);
    }
  };

  private final Consumer<Long> markDead = stfId -> {
    doMarkDead(stfId);
  };

  @Override
  public void forward(Long stfId, String calleeInfo, boolean async, Object... calleeMethodArgs) {
    coreFn.accept(stfId, calleeInfo, calleeMethodArgs, null, async, forward);
  }

  @Override
  public boolean tryLockStf(Long stfId, long lastUpAtMs) {
    if (checkFail(stfId)) {
      return false;
    }
    return doTryLockStf(stfId, lastUpAtMs);
  }

  @Override
  public boolean doTryLockStf(Long stfId, long lastUpAtMs) {
    int cnt = jdbcOps.update(LOCK_SQL, stfId, lastUpAtMs);
    return cnt == 1;
  }

  @Override
  public void markDone(Long stfId, boolean async) {
    coreFn.accept(stfId, null, null, null, async, markDone);
  }

  @Override
  public void markDead(Long stfId, boolean async) {
    coreFn.accept(stfId, null, null, null, async, markDead);
  }

  @Override
  public void reForward(Long stfId, int lastRetryTimes, String calleeInfo, boolean async, Object... calleeMethodArgs) {
    coreFn.accept(stfId, calleeInfo, calleeMethodArgs, lastRetryTimes, async, reForward);
  }

  @Override
  protected void doInit(Long newStfId, StfCall callee) {
    String calleeJson = JsonHelper.toJson(callee);
    long now = System.currentTimeMillis();
    int timeoutSecs = callee.getTimeoutSecs();
    jdbcOps.update(INIT_SQL, newStfId, calleeJson, (now + timeoutSecs * 1000), now, now);
  }

  @Override
  public boolean doForward(Long stfId) {
    int cnt = jdbcOps.update(FORWARD_SQL, System.currentTimeMillis(), stfId);
    return cnt == 1;
  }

  @Override
  protected void doMarkDead(Long stfId) {
    jdbcOps.update(MARK_DEAD_SQL, System.currentTimeMillis(), stfId);
  }

  @Override
  public boolean doMarkDone(Long stfId) {
    int cnt = jdbcOps.update(MARK_DONE_SQL, System.currentTimeMillis(), stfId);
    return cnt == 1;
  }

  @Override
  protected boolean doReForward(Long stfId, int curRetryTimes) {
    int cnt = jdbcOps.update(RETRY_FORWARD_SQL, System.currentTimeMillis(), stfId, curRetryTimes);
    return cnt == 1;
  }

  public StfCoreJdbc(StfJdbcOps jdbc) {
    this(jdbc, DFT_TBL_NAME);
  }

  public StfCoreJdbc(StfJdbcOps jdbc, String tblName) {
    this.jdbcOps = jdbc;

    String initTemplateSql = lenientFormat(
        "insert into %s (id, callee, st, is_dead, is_locked, retry_times, timeout_at, ct_at, up_at) values(?, ?, '%s', '%s', '%s', %s, ?, ?, ?)",
        tblName);
    INIT_SQL = lenientFormat(initTemplateSql, I.name(), N.name(), N.name(), 0);
    FORWARD_SQL = lenientFormat("update %s set st = '%s', up_at = ? where id = ? and st in ('%s', '%s')", tblName,
        P.name(), I.name(), P.name());

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
    MARK_DEAD_SQL = lenientFormat("update %s set is_dead = '%s', up_at = ? where id = ? and st != '%s'", tblName,
        Y.name(), S.name());
    MARK_DONE_SQL = lenientFormat("update %s set st = '%s', up_at = ? where id = ? and st != '%s'", tblName, S.name(),
        F.name());

    LOCK_SQL = lenientFormat(
        "update %s set is_locked = '%s' where id = ? and is_locked = '%s' and up_at = ? and st not in ('%s', '%s')",
        tblName, Y.name(), N.name(), S.name(), F.name());// FIXME mj:Extend timeoutAt here?
  }

  private void invokeConsumer(Long stfId, String calleeInfo, Object[] calleeMethodArgs, Integer curRetryTimes,
      BaseConsumer<Long> bizFn) {
    if (bizFn instanceof Consumer) {
      ((Consumer<Long>)bizFn).accept(stfId);
    } else if (bizFn instanceof TriConsumer) {
      ((TriConsumer<Long, String, Object[]>)bizFn).accept(stfId, calleeInfo, calleeMethodArgs);
    } else if (bizFn instanceof QuadruConsumer) {
      ((QuadruConsumer<Long, String, Object[], Integer>)bizFn).accept(stfId, calleeInfo, calleeMethodArgs,
          curRetryTimes);
    }
  }

}