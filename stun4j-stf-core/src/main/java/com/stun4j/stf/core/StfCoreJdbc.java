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
import static com.stun4j.stf.core.State.F;
import static com.stun4j.stf.core.State.I;
import static com.stun4j.stf.core.State.P;
import static com.stun4j.stf.core.State.S;
import static com.stun4j.stf.core.StfConsts.DFT_CORE_TBL_NAME;
import static com.stun4j.stf.core.StfConsts.DFT_DELAY_TBL_NAME_SUFFIX;
import static com.stun4j.stf.core.StfConsts.StfDbField.ALL_FIELD_NAMES_LOWER_CASE;
import static com.stun4j.stf.core.StfConsts.StfDbField.CALLEE_BYTES;
import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.StfMetaGroup.CORE;
import static com.stun4j.stf.core.StfMetaGroup.DELAY;
import static com.stun4j.stf.core.YesNo.N;
import static com.stun4j.stf.core.YesNo.Y;
import static org.apache.commons.lang3.RegExUtils.removeFirst;
import static org.apache.commons.lang3.tuple.Pair.of;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.event.StfDelayTriggeredEvent;
import com.stun4j.stf.core.support.event.StfDoneEvent;
import com.stun4j.stf.core.support.event.StfEventBus;

/**
 * The jdbc implementation of {@link BaseStfCore}
 * 
 * @author Jay Meng
 */
public class StfCoreJdbc extends BaseStfCore implements JdbcAware {
  private final String INIT_SQL_FULL;
  private final String INIT_SQL_NO_CALLEE_BYTES;
  private final String INIT_DELAY_SQL_FULL;
  private final String INIT_DELAY_SQL_NO_CALLEE_BYTES;
  private final String MARK_DEAD_SQL;
  private final String MARK_DEAD_DELAY_SQL;
  private final String MARK_DONE_SQL;
  private final String MARK_DONE_DELAY_SQL;
  private final String LOCK_SQL;
  private final String LOCK_DELAY_SQL;

  private final String DELAY_TRANSFER_FULL_SQL;

  private final StfJdbcOps jdbcOps;

  private String coreTblName;

  @Override
  public long lockStf(StfMetaGroup metaGrp, Long stfId, int timeoutSecs, int lastRetryTimes, long lastTimeoutAt) {
    if (checkFail(stfId)) {
      return -1;
    }
    return doLockStf(metaGrp, stfId, timeoutSecs, lastRetryTimes, lastTimeoutAt);
  }

  @Override
  public void markDone(StfMetaGroup metaGrp, Long stfId, boolean async) {
    coreFn.accept(of(metaGrp, stfId), null, null, async, true, markDone);
  }

  @Override
  public void markDead(StfMetaGroup metaGrp, Long stfId, boolean async) {
    coreFn.accept(of(metaGrp, stfId), null, null, async, false, markDead);
  }

  @Override
  public void forward(StfMetaGroup metaGrp, Stf lockedStf, StfCall calleePreEval, boolean async) {
    coreFn.accept(of(metaGrp, lockedStf.getId()), lockedStf, calleePreEval, async, false, forward);
  }

  @Override
  protected void doNewStf(Long stfId, StfCall callee, int timeoutSecs) {
    StfMetaGroup dsKey;
    if (H.isDataSourceClose(dsKey = CORE)) {
      H.logOnDataSourceClose(LOG, "doNewStf", of("stf", stfId));
      return;
    }
    Pair<String, byte[]> dynaCallee = callee.toBytesIfNecessary();
    String metaOrBody = dynaCallee.getKey();
    byte[] bytesOrNull = dynaCallee.getValue();
    long now = System.currentTimeMillis();
    if (bytesOrNull != null) {
      jdbcOps.update(dsKey, INIT_SQL_FULL, stfId, metaOrBody, bytesOrNull, timeoutSecs, (now + timeoutSecs * 1000), now,
          now);
    } else {
      jdbcOps.update(dsKey, INIT_SQL_NO_CALLEE_BYTES, stfId, metaOrBody, timeoutSecs, (now + timeoutSecs * 1000), now,
          now);
    }
  }

  @Override
  protected void doNewDelayStf(Long stfId, StfCall callee, int timeoutSecs, int delaySecs) {
    StfMetaGroup dsKey;
    if (H.isDataSourceClose(dsKey = DELAY)) {
      H.logOnDataSourceClose(LOG, "doNewStfDelay", of("stfd", stfId));
      return;
    }
    Pair<String, byte[]> dynaCallee = callee.toBytesIfNecessary();
    String metaOrBody = dynaCallee.getKey();
    byte[] bytesOrNull = dynaCallee.getValue();
    long now = System.currentTimeMillis();
    if (bytesOrNull != null) {
      jdbcOps.update(dsKey, INIT_DELAY_SQL_FULL, stfId, metaOrBody, bytesOrNull, timeoutSecs, (now + delaySecs * 1000),
          now, now);
    } else {
      jdbcOps.update(dsKey, INIT_DELAY_SQL_NO_CALLEE_BYTES, stfId, metaOrBody, timeoutSecs, (now + delaySecs * 1000),
          now, now);
    } // TODO mj:possibility batch insert?
  }

  @Override
  protected long doLockStf(StfMetaGroup metaGrp, Long stfId, int timeoutSecs, int lastRetryTimes, long lastTimeoutAt) {
    if (H.isDataSourceClose(metaGrp)) {
      H.logOnDataSourceClose(LOG, "doLockStf", of("stf", stfId));
      return -1;
    }
    long now;
    int cnt = jdbcOps.update(metaGrp, metaGrp == CORE ? LOCK_SQL : LOCK_DELAY_SQL,
        (now = System.currentTimeMillis()) + timeoutSecs * 1000, now, stfId, lastRetryTimes, lastTimeoutAt);
    return cnt == 1 ? now : -1;
  }

  @Override
  protected int[] doBatchLockStfs(StfMetaGroup metaGrp, List<Object[]> batchArgs) {
    if (H.isDataSourceClose(metaGrp)) {
      H.logOnDataSourceClose(LOG, "doBatchLockStfs");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(metaGrp, metaGrp == CORE ? LOCK_SQL : LOCK_DELAY_SQL, batchArgs);
    return res;
  }

  @Override
  protected boolean doMarkDone(StfMetaGroup metaGrp, Long stfId, boolean batch) {
    if (H.isDataSourceClose(metaGrp)) {
      H.logOnDataSourceClose(LOG, "doMarkDone", of("stf", stfId));
      return false;
    }
    if (batch) {
      StfEventBus.post(metaGrp == CORE ? new StfDoneEvent(stfId) : new StfDelayTriggeredEvent(stfId));
      return true;
    }
    int cnt = jdbcOps.update(metaGrp, metaGrp == CORE ? MARK_DONE_SQL : MARK_DONE_DELAY_SQL, System.currentTimeMillis(),
        stfId);
    return cnt == 1;
  }

  @Override
  public int[] batchMarkDone(StfMetaGroup metaGrp, List<Object[]> stfIdsInfo) {
    if (H.isDataSourceClose(metaGrp)) {
      H.logOnDataSourceClose(LOG, "batchMarkDone");
      return null;
    }
    int[] res = jdbcOps.batchUpdate(metaGrp, metaGrp == CORE ? MARK_DONE_SQL : MARK_DONE_DELAY_SQL, stfIdsInfo);
    return res;
  }

  @Override
  protected void doMarkDead(StfMetaGroup metaGrp, Long stfId) {
    if (H.isDataSourceClose(metaGrp)) {
      H.logOnDataSourceClose(LOG, "doMarkDead", of("stf", stfId));
      return;
    }
    jdbcOps.update(metaGrp, metaGrp == CORE ? MARK_DEAD_SQL : MARK_DEAD_DELAY_SQL, System.currentTimeMillis(), stfId);
  }

  @Override
  protected boolean doDelayTransfer(Stf lockedDelayStf, StfCall delayCalleePreEval) {
    Long stfId = lockedDelayStf.getId();
    StfMetaGroup dsKey;
    if (H.isDataSourceClose(dsKey = CORE)) {
      H.logOnDataSourceClose(LOG, "doDelayTransfer", of("stf", stfId));
      return false;
    }

    Pair<String, byte[]> calleeMayGotBodyFormatChanged = super.doDelayTransfer0(lockedDelayStf, delayCalleePreEval);
    int timeoutSecs = lockedDelayStf.getTimeoutSecs();

    long now;
    String metaOrBody = calleeMayGotBodyFormatChanged.getLeft();
    byte[] bytesOrNull = calleeMayGotBodyFormatChanged.getRight();
    int cnt = jdbcOps.update(dsKey, DELAY_TRANSFER_FULL_SQL, stfId, metaOrBody, bytesOrNull, timeoutSecs,
        (now = System.currentTimeMillis()) + timeoutSecs * 1000, now, now);
    return cnt == 1;
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps) {
    this(jdbcOps, DFT_CORE_TBL_NAME);
  }

  public StfCoreJdbc(StfJdbcOps jdbcOps, String coreTblName) {
    StfHelper.init(jdbcOps);
    this.jdbcOps = jdbcOps;
    String delayTblName = (this.coreTblName = coreTblName) + DFT_DELAY_TBL_NAME_SUFFIX;

    String initSqlTpl = "insert into %s (%s) values (?, ?, ?, '%s', '%s', %s, ?, ?, ?, ?)";
    INIT_SQL_FULL = lenientFormat(initSqlTpl, coreTblName, ALL_FIELD_NAMES_LOWER_CASE, I.name(), N.name(), 0);
    String calleeBytesFldSeg = ", " + CALLEE_BYTES.nameLowerCase();
    INIT_SQL_NO_CALLEE_BYTES = removeFirst(removeFirst(INIT_SQL_FULL, calleeBytesFldSeg), ", \\?");
    INIT_DELAY_SQL_FULL = lenientFormat(initSqlTpl, delayTblName, ALL_FIELD_NAMES_LOWER_CASE, I.name(), N.name(), 0);
    INIT_DELAY_SQL_NO_CALLEE_BYTES = removeFirst(removeFirst(INIT_DELAY_SQL_FULL, calleeBytesFldSeg), ", \\?");

    String lockSqlTpl = "update %s set st = '%s', retry_times = retry_times + 1, timeout_at = ?, up_at = ? where id = ? and retry_times = ? and timeout_at = ? and st in ('%s', '%s')";
    LOCK_SQL = lenientFormat(lockSqlTpl, coreTblName, P.name(), I.name(), P.name());
    LOCK_DELAY_SQL = lenientFormat(lockSqlTpl, delayTblName, P.name(), I.name(), P.name());

    String markDoneSqlTpl = "update %s set st = '%s', up_at = ? where id = ? and st != '%s'";
    MARK_DONE_SQL = lenientFormat(markDoneSqlTpl, coreTblName, S.name(), F.name());
    MARK_DONE_DELAY_SQL = lenientFormat(markDoneSqlTpl, delayTblName, S.name(), F.name());

    String markDeadSqlTpl = "update %s set is_dead = '%s', up_at = ? where id = ? and st != '%s'";
    MARK_DEAD_SQL = lenientFormat(markDeadSqlTpl, coreTblName, Y.name(), S.name());
    MARK_DEAD_DELAY_SQL = lenientFormat(markDeadSqlTpl, delayTblName, Y.name(), S.name());

    DELAY_TRANSFER_FULL_SQL = lenientFormat(initSqlTpl, coreTblName, ALL_FIELD_NAMES_LOWER_CASE, I.name(), N.name(), 0);
  }

  @Override
  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }

  @Override
  protected String getCoreTblName() {
    return coreTblName;
  }

}