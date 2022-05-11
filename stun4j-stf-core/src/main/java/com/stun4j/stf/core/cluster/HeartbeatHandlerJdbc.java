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
package com.stun4j.stf.core.cluster;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.job.JobHelper.isDataSourceClose;
import static com.stun4j.stf.core.job.JobHelper.tryGetDataSourceCloser;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_POSTGRE_SQL;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.stun4j.stf.core.StfConsts;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.utils.DataSourceUtils;

/**
 * @author JayMeng
 */
public class HeartbeatHandlerJdbc extends HeartbeatHandler {
  private final String HB_UPSERT_SQL;
  private final String HB_KEEP_ALIVE_SQL;
  private final String HB_ALL_SQL;
  private final String HB_DELETE_SQL;

  private final StfJdbcOps jdbcOps;
  private final String dbVendor;
  private final Method dsCloser;

  @Override
  protected void onStartup() {
    registerSelf();
    refreshAllMembers();
  }

  @Override
  protected void onShutdown(String memberId) {
    deregisterSelf(memberId);
  }

  @Override
  protected void doSendHeartbeat() {
    if (isDataSourceClose(dsCloser, jdbcOps.getDataSource())) {
      LOG.warn("[doSendHeartbeat] The dataSource has been closed and the operation is cancelled.");
      return;
    }
    String memberId;
    long now;
    int cnt = jdbcOps.update(HB_KEEP_ALIVE_SQL, now = System.currentTimeMillis(),
        memberId = StfClusterMember.calculateId());
    if (cnt != 1) {
      LOG.warn("Found invalid heartbeat sending [memberId={}] > Stf member-id changes?", memberId);
      registerSelf();
    }
    localMemberTracingMemo.put(memberId, now);
    refreshAllMembers();
  }

  private void refreshAllMembers() {
    try (Stream<StfClusterMember> members = jdbcOps.queryForStream(HB_ALL_SQL, new Object[]{1024},
        (rs, arg) -> {/*-TODO mj:1024,to be configured*/
          StfClusterMember member = new StfClusterMember(rs.getString("id")/* mayTheReuseMemberId */,
              rs.getLong("up_at"));
          localMemberTracingMemo.computeIfPresent(member.getId(), (mayTheReuseMemberId, v) -> member.getUpAt());
          return member;
        })) {
      StfClusterMembers.replaceWith(members, this.getTimeoutMs());
    }
  }

  private void registerSelf() {
    String memberId;
    long now;
    jdbcOps.update(HB_UPSERT_SQL, memberId = StfClusterMember.calculateId(), now = System.currentTimeMillis(), now);
    localMemberTracingMemo.put(memberId, now);
  }

  private void deregisterSelf(String memberId) {
    if (isDataSourceClose(dsCloser, jdbcOps.getDataSource())) {
      LOG.warn("[deregisterSelf] The dataSource has been closed and the operation is cancelled.");
      return;
    }
    localMemberTracingMemo.forEach((everGeneratedMemberIdOfCurrentProcess, lastUpAt) -> {
      if (Objects.equals(memberId, everGeneratedMemberIdOfCurrentProcess)) {// Check for safe deregister
        jdbcOps.update(HB_DELETE_SQL, memberId);
      } else {
        if (System.currentTimeMillis() - lastUpAt > this.getTimeoutMs()) {// Just expect relative safe here
          jdbcOps.update(HB_DELETE_SQL, everGeneratedMemberIdOfCurrentProcess);
        }
      }
    });
  }

  public static HeartbeatHandlerJdbc of(StfJdbcOps jdbcOps) {
    return new HeartbeatHandlerJdbc(jdbcOps, StfConsts.DFT_CLUSTER_MEMBER_TBL_NAME);
  }

  public HeartbeatHandlerJdbc(StfJdbcOps jdbcOps, String tblName) {
    this.jdbcOps = jdbcOps;
    DataSource ds;
    this.dbVendor = DataSourceUtils.getDatabaseProductName(ds = jdbcOps.getDataSource());
    this.dsCloser = tryGetDataSourceCloser(ds);

    HB_KEEP_ALIVE_SQL = lenientFormat("update %s set up_at = ? where id = ?", tblName);
    HB_ALL_SQL = lenientFormat("select id, up_at from %s limit ?",
        tblName);/*-TODO mj:what is the upper limit,this may change*/
    HB_DELETE_SQL = lenientFormat("delete from %s where id =?", tblName);

    if (DB_VENDOR_MY_SQL.equals(dbVendor)) {
      HB_UPSERT_SQL = lenientFormat(
          "insert into %s (id, ct_at, up_at) values (?, ?, ?) on duplicate key update ct_at = values(ct_at), up_at = values(up_at)",
          tblName);
    } else if (DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
      HB_UPSERT_SQL = lenientFormat(
          "insert into %s (id, ct_at, up_at) values (?, ?, ?) on conflict (id) do update set ct_at = excluded.ct_at, up_at = excluded.up_at",
          tblName);
    } else {
      // FIXME mj:oracle implementation
      HB_UPSERT_SQL = null;
    }
  }

}
