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
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.job.JobHelper.isDataSourceClose;
import static com.stun4j.stf.core.job.JobHelper.tryGetDataSourceCloser;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_POSTGRE_SQL;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.StateEnum;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfConsts;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.utils.DataSourceUtils;

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
  private final String dbVendor;
  private final LocalGuid guid;
  private final Method dsCloser;

  // final String SQL_WITH_ALIVE_ST;
  final String SQL_WITH_SINGLE_ST;
  // final String SQL_WITH_ALIVE_ST_MYSQL;
  final String SQL_WITH_SINGLE_ST_MYSQL;
  // final String SQL_WITH_ALIVE_ST_ORACLE;
  final String SQL_WITH_SINGLE_ST_ORACLE;

  private int includeHowManyDaysAgo;

  @Override
  public Stream<Stf> scanTimeoutJobsWaitingRun(int limit) {
    return doScanStillAlive(I, limit);
  }

  @Override
  public Stream<Stf> scanTimeoutJobsInProgress(int limit) {
    return doScanStillAlive(P, limit);
  }

  @Deprecated
  @Override
  public Stream<Stf> scanTimeoutJobsStillAlive(int limit, boolean locked, String... includeFields) {
    return doScanStillAlive(null, limit, includeFields);
  }

  public Stream<Stf> doScanStillAlive(StateEnum st, int limit, String... includeFields) {
    if (isDataSourceClose(dsCloser, jdbcOps.getDataSource())) {
      LOG.warn("The dataSource has been closed and the scan is cancelled.");
      return Stream.empty();
    }
    long now = System.currentTimeMillis();
    long idEnd = guid.from(now);
    long idStart = guid.from(now - TimeUnit.DAYS.toMillis(includeHowManyDaysAgo));

    String sql;
    Object[] args;
    // YesNoEnum yesNo = locked ? Y : N;
    // if (st != null) {
    if (DB_VENDOR_MY_SQL.equals(dbVendor) || DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
      sql = SQL_WITH_SINGLE_ST_MYSQL;
    } else {
      sql = SQL_WITH_SINGLE_ST_ORACLE;
    }
    args = new Object[]{idStart, idEnd, now, N.name(), st.name(), limit};
    // } else {
    // if (DB_VENDOR_MY_SQL.equals(dbVendor) || DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
    // sql = SQL_WITH_ALIVE_ST_MYSQL;
    // } else {
    // sql = SQL_WITH_ALIVE_ST_ORACLE;
    // }
    // args = new Object[]{idStart, idEnd, now, yesNo.name(), N.name(), limit};
    // }

    MutableBoolean checkFields = new MutableBoolean(false);
    if (includeFields != null && includeFields.length > 0) {
      sql = sql.replaceFirst("select \\*", "select " + StringUtils.join(includeFields, ","));
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
     * sort by create time,instead of 'ctAt',we use id because it is generated by using classic-snowflake
     * algorithm,which means,the default job-queue order would be the create-time order of stfs
     */
    return stfs.sorted(Comparator.comparingLong(stf -> stf.getId()));
  }

  public static JobScannerJdbc of(StfJdbcOps jdbcOps) {
    return new JobScannerJdbc(jdbcOps, StfConsts.DFT_TBL_NAME);
  }

  public JobScannerJdbc(StfJdbcOps jdbcOps, String tblName) throws RuntimeException {
    this.jdbcOps = jdbcOps;
    DataSource ds;
    this.dbVendor = DataSourceUtils.getDatabaseProductName(ds = jdbcOps.getDataSource());
    this.guid = LocalGuid.instance();
    this.includeHowManyDaysAgo = DFT_INCLUDE_HOW_MANY_DAYS_AGO;
    this.dsCloser = tryGetDataSourceCloser(ds);

    // SQL_WITH_ALIVE_ST = lenientFormat(
    // "select * from %s where id in (select id from %s where id between ? and ? and timeout_at <= ? and is_locked = ?
    // and is_dead = ? and st in ('%s', '%s') order by timeout_at) ",
    // tblName, tblName, I.name(), P.name());

    SQL_WITH_SINGLE_ST = lenientFormat(
        "select * from %s where id in (select id from %s where id between ? and ? and timeout_at <= ? and is_dead = ? and st = ? order by timeout_at) ",
        tblName, tblName);

    // SQL_WITH_ALIVE_ST_MYSQL = lenientFormat("%s limit ?", SQL_WITH_ALIVE_ST);
    SQL_WITH_SINGLE_ST_MYSQL = lenientFormat("%s limit ?", SQL_WITH_SINGLE_ST);
    // SQL_WITH_ALIVE_ST_ORACLE = lenientFormat(
    // "select * from (select t_temp.*, rownum rn from (%s) t_temp where rownum <= ?) where rn > 0",
    // SQL_WITH_ALIVE_ST);
    SQL_WITH_SINGLE_ST_ORACLE = lenientFormat(
        "select * from (select t_temp.*, rownum rn from (%s) t_temp where rownum <= ?) where rn > 0",
        SQL_WITH_SINGLE_ST);
  }

  public String getDbVendor() {
    return dbVendor;
  }

  @Override
  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }

  public void setIncludeHowManyDaysAgo(int includeHowManyDaysAgo) {
    this.includeHowManyDaysAgo = includeHowManyDaysAgo < 0 ? DFT_INCLUDE_HOW_MANY_DAYS_AGO
        : (includeHowManyDaysAgo > DFT_MAX_INCLUDE_HOW_MANY_DAYS_AGO ? DFT_MAX_INCLUDE_HOW_MANY_DAYS_AGO
            : includeHowManyDaysAgo);
  }

}