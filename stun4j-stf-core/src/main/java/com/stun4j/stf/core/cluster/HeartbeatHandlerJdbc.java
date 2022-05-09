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

import static com.stun4j.stf.core.job.JobHelper.isDataSourceClose;
import static com.stun4j.stf.core.job.JobHelper.tryGetDataSourceCloser;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

import com.stun4j.stf.core.spi.StfJdbcOps;

/**
 * @author JayMeng
 */
public class HeartbeatHandlerJdbc extends HeartbeatHandler {
  private final String HB_UPSERT_SQL;
  private final String HB_KEEP_ALIVE_SQL;
  private final String HB_ALL_SQL;
  private final String HB_DELETE_SQL;
  private final StfJdbcOps jdbcOps;
  private final Method dsCloser;

  @Override
  protected void onStartup(String memberId) {
    register(memberId);
    refreshAllMembers();
  }

  @Override
  protected void onShutdown(String memberId) {
    deregister(memberId);
  }

  @Override
  protected void doSendHeartbeat(String memberId) {
    if (isDataSourceClose(dsCloser, jdbcOps.getDataSource())) {
      LOG.warn("[doSendHeartbeat] The dataSource has been closed and the operation is cancelled.");
      return;
    }
    long now;
    int cnt = jdbcOps.update(HB_KEEP_ALIVE_SQL, now = System.currentTimeMillis(), memberId);
    if (cnt != 1) {
      LOG.warn("Found invalid heartbeat sending [memberId={}] > Stf member-id changes?", memberId);
      register(memberId);
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

  private void register(String memberId) {
    long now;
    jdbcOps.update(HB_UPSERT_SQL, memberId, now = System.currentTimeMillis(), now);
    localMemberTracingMemo.put(memberId, now);
  }

  private void deregister(String memberId) {
    localMemberTracingMemo.forEach((everGeneratedMemberIdOfCurrentProcess, lastUpAt) -> {
      if (Objects.equals(memberId, everGeneratedMemberIdOfCurrentProcess)) {// Check for the safe deregister
        jdbcOps.update(HB_DELETE_SQL, memberId);
      } else {
        if (System.currentTimeMillis() - lastUpAt > this
            .getTimeoutMs()) {/*- Hope this is the relatively safe check for the deregister */
          jdbcOps.update(HB_DELETE_SQL, everGeneratedMemberIdOfCurrentProcess);
        }
      }
    });
  }

  public HeartbeatHandlerJdbc(StfJdbcOps jdbcOps) {
    this.jdbcOps = jdbcOps;
    this.dsCloser = tryGetDataSourceCloser(jdbcOps.getDataSource());

    HB_KEEP_ALIVE_SQL = "update stn_stf_cluster_member set up_at = ? where id = ?";
    HB_UPSERT_SQL = "insert into stn_stf_cluster_member(id, ct_at, up_at) values (?, ?, ?) on duplicate key update ct_at = values(ct_at), up_at = values(up_at)";
    HB_ALL_SQL = "select id, up_at from stn_stf_cluster_member limit ?";/*-TODO mj:what is the upper limit,this may change*/
    HB_DELETE_SQL = "delete from stn_stf_cluster_member where id =?";
  }

}
