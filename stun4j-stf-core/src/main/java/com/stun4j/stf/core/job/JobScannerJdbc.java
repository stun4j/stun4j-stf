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

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StateEnum.I;
import static com.stun4j.stf.core.StateEnum.P;
import static com.stun4j.stf.core.StfConsts.DFT_DELAY_TBL_NAME_SUFFIX;
import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
import static com.stun4j.stf.core.StfMetaGroupEnum.DELAY;
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_ORACLE;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_POSTGRE_SQL;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.StateEnum;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfConsts;
import com.stun4j.stf.core.StfMetaGroupEnum;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JdbcAware;

/**
 * The jdbc implementation of {@link JobScanner}
 * <ul>
 * <li>All the concerned stf-jobs will be scanned for specific subsequent processing.</li>
 * <li>MySQL,Oracle,PostgreSQL are supported out of the box.</li>
 * </ul>
 * @author Jay Meng
 */
public class JobScannerJdbc implements JobScanner, JdbcAware {
  private static final Logger LOG = LoggerFactory.getLogger(JobScannerJdbc.class);
  private final StfJdbcOps jdbcOps;

  final String SQL_CORE_MYSQL;
  final String SQL_CORE_POSTGRES;
  final String SQL_CORE_ORACLE;

  final String SQL_DELAY_MYSQL;
  final String SQL_DELAY_POSTGRES;
  final String SQL_DELAY_ORACLE;

  private int includeHowManyDaysAgo;

  @Override
  public Stream<Stf> scanTimeoutCoreJobsWaitingRun(int limit, int pageNo) {
    return doScanStillAlive(CORE, I, limit, pageNo);
  }

  @Override
  public Stream<Stf> scanTimeoutCoreJobsInProgress(int limit, int pageNo) {
    return doScanStillAlive(CORE, P, limit, pageNo);
  }

  @Override
  public Stream<Stf> scanTimeoutDelayJobsWaitingRun(int limit, int pageNo) {
    return doScanStillAlive(DELAY, I, limit, pageNo);
  }

  @Override
  public Stream<Stf> scanTimeoutDelayJobsInProgress(int limit, int pageNo) {
    return doScanStillAlive(DELAY, P, limit, pageNo);
  }

  public Stream<Stf> doScanStillAlive(StfMetaGroupEnum metaGrp, StateEnum st, int limit, int pageNo,
      String... includeFields) {/*-pageNo start with 0*/
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doScanStillAlive");
      return Stream.empty();
    }
    long now = System.currentTimeMillis();
    LocalGuid guid;
    long idEnd = (guid = H.cachedGuid()).from(now);
    long idStart = guid.from(now - TimeUnit.DAYS.toMillis(includeHowManyDaysAgo));

    String sql;
    String dbVendor;
    if (metaGrp == CORE) {
      if (DB_VENDOR_MY_SQL.equals(dbVendor = H.getDbVendor())) {
        sql = SQL_CORE_MYSQL;
      } else if (DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
        sql = SQL_CORE_POSTGRES;
      } else {
        sql = SQL_CORE_ORACLE;
      }
    } else {
      if (DB_VENDOR_MY_SQL.equals(dbVendor = H.getDbVendor())) {
        sql = SQL_DELAY_MYSQL;
      } else if (DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
        sql = SQL_DELAY_POSTGRES;
      } else {
        sql = SQL_DELAY_ORACLE;
      }
    }
    Object[] args;
    if (pageNo <= 0) {
      if (!DB_VENDOR_ORACLE.equals(dbVendor)) {
        args = new Object[]{idStart, idEnd, now, N.name(), st.name(), limit};
      } else {
        int lastPageRowNum;
        args = new Object[]{idStart, idEnd, now, N.name(), st.name(), (lastPageRowNum = pageNo * limit) + limit,
            lastPageRowNum};
      }
    } else {
      if (!DB_VENDOR_ORACLE.equals(dbVendor)) {
        sql = sql.substring(0, sql.indexOf("limit ?"));
        if (DB_VENDOR_MY_SQL.equals(dbVendor)) {
          sql += lenientFormat("limit %s, %s", pageNo * limit, limit);
        } else {
          sql += lenientFormat("limit %s offset %s", limit, pageNo * limit);
        }
        args = new Object[]{idStart, idEnd, now, N.name(), st.name()};
      } else {
        int lastPageRowNum;
        args = new Object[]{idStart, idEnd, now, N.name(), st.name(), (lastPageRowNum = pageNo * limit) + limit,
            lastPageRowNum};
      }
    }

    MutableBoolean checkFields = new MutableBoolean(false);
    if (includeFields != null && includeFields.length > 0) {
      sql = sql.substring(sql.indexOf("*") + 1);
      sql = "select " + StringUtils.join(includeFields, ",") + sql;
      checkFields.setValue(true);
    }
    Stream<Stf> stfs = jdbcOps.queryForStream(sql, args, (rs, arg) -> {
      Stf stf = new Stf();
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "id")) {
        stf.setId(rs.getLong("id"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "callee")) {
        stf.setBody(rs.getString("callee"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "st")) {
        stf.setSt(rs.getString("st"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "is_dead")) {
        stf.setIsDead(rs.getString("is_dead"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "retry_times")) {
        stf.setRetryTimes(rs.getInt("retry_times"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "timeout_secs")) {
        stf.setTimeoutSecs(rs.getInt("timeout_secs"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "timeout_at")) {
        stf.setTimeoutAt(rs.getLong("timeout_at"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "ct_at")) {
        stf.setCtAt(rs.getLong("ct_at"));
      }
      if (!checkFields.getValue() || ArrayUtils.contains(includeFields, "up_at")) {
        stf.setUpAt(rs.getLong("up_at"));
      }
      return stf;
    });

    /*
     * Sort by create time,instead of 'ctAt',we use id because it is generated by using classic-snowflake
     * algorithm,which means,the default job-queue order would be the create-time order of stfs
     */
    return stfs.sorted(Comparator.comparingLong(stf -> stf.getId()));
  }

  public static JobScanner of(StfJdbcOps jdbcOps) {
    return new JobScannerJdbc(jdbcOps, StfConsts.DFT_CORE_TBL_NAME);
  }

  public JobScannerJdbc(StfJdbcOps jdbcOps, String tblName) throws RuntimeException {
    this.jdbcOps = jdbcOps;
    this.includeHowManyDaysAgo = DFT_INCLUDE_HOW_MANY_DAYS_AGO;

    String mainSqlTpl = "select * from %s where id in (select id from %s where id between ? and ? and timeout_at <= ? and is_dead = ? and st = ?";
    String coreSql = lenientFormat(mainSqlTpl, tblName, tblName);
    SQL_CORE_MYSQL = lenientFormat("%s%s limit ?", coreSql, " order by timeout_at)");
    SQL_CORE_POSTGRES = lenientFormat("%s%s limit ?", coreSql,
        ") order by timeout_at");/*-Get certain order when using postgres*/
    String oracleSqlInner = lenientFormat("%s%s", coreSql, ") order by timeout_at");
    SQL_CORE_ORACLE = lenientFormat(
        "select * from (select t_temp.*, rownum rn from (%s) t_temp where rownum <= ?) where rn > ?", oracleSqlInner);

    String delayCoreSql = lenientFormat(mainSqlTpl, tblName + DFT_DELAY_TBL_NAME_SUFFIX,
        tblName + DFT_DELAY_TBL_NAME_SUFFIX);
    SQL_DELAY_MYSQL = lenientFormat("%s%s limit ?", delayCoreSql, " order by timeout_at)");
    SQL_DELAY_POSTGRES = lenientFormat("%s%s limit ?", delayCoreSql,
        ") order by timeout_at");/*-Get certain order when using postgres*/
    oracleSqlInner = lenientFormat("%s%s", delayCoreSql, ") order by timeout_at");
    SQL_DELAY_ORACLE = lenientFormat(
        "select * from (select t_temp.*, rownum rn from (%s) t_temp where rownum <= ?) where rn > ?", oracleSqlInner);
  }

  @Override
  @Deprecated
  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }

  public void setIncludeHowManyDaysAgo(int includeHowManyDaysAgo) {
    this.includeHowManyDaysAgo = includeHowManyDaysAgo < 0 ? DFT_INCLUDE_HOW_MANY_DAYS_AGO
        : (includeHowManyDaysAgo > DFT_MAX_INCLUDE_HOW_MANY_DAYS_AGO ? DFT_MAX_INCLUDE_HOW_MANY_DAYS_AGO
            : includeHowManyDaysAgo);
  }

}