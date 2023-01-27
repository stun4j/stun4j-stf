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
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.CALLEE;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.CT_AT;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.ID;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.IS_DEAD;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.RETRY_TIMES;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.ST;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.TIMEOUT_AT;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.TIMEOUT_SECS;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.UP_AT;
import static com.stun4j.stf.core.StfHelper.H;
import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
import static com.stun4j.stf.core.StfMetaGroupEnum.DELAY;
import static com.stun4j.stf.core.YesNoEnum.N;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_ORACLE;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_POSTGRE_SQL;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  String SQL_CORE_MYSQL;
  String SQL_CORE_POSTGRES;
  String SQL_CORE_ORACLE;

  String SQL_DELAY_MYSQL;
  String SQL_DELAY_POSTGRES;
  String SQL_DELAY_ORACLE;

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
      String... includeFields) {// pageNo start with 0
    if (H.isDataSourceClose()) {
      H.logOnDataSourceClose(LOG, "doScanStillAlive");
      return Stream.empty();
    }

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
    long endAt = System.currentTimeMillis();
    long startAt = endAt - TimeUnit.DAYS.toMillis(includeHowManyDaysAgo);
    if (pageNo <= 0) {
      if (!DB_VENDOR_ORACLE.equals(dbVendor)) {
        args = new Object[]{startAt, endAt, st.name(), N.name(), limit};
      } else {
        int lastPageRowNum;
        args = new Object[]{startAt, endAt, st.name(), N.name(), (lastPageRowNum = pageNo * limit) + limit,
            lastPageRowNum};
      }
    } else {
      if (!DB_VENDOR_ORACLE.equals(dbVendor)) {
        String limitSep;
        int limitSepIdx = sql.indexOf(limitSep = "limit ?");
        String preSql = sql.substring(0, limitSepIdx);
        if (DB_VENDOR_MY_SQL.equals(dbVendor)) {
          String postSql = sql.substring(limitSepIdx + limitSep.length());
          sql = lenientFormat("%slimit %s, %s%s", preSql, pageNo * limit, limit, postSql);
        } else {
          sql = lenientFormat("%slimit %s offset %s", preSql, limit, pageNo * limit);
        }
        args = new Object[]{startAt, endAt, st.name(), N.name()};
      } else {
        int lastPageRowNum;
        args = new Object[]{startAt, endAt, st.name(), N.name(), (lastPageRowNum = pageNo * limit) + limit,
            lastPageRowNum};
      }
    }

    MutableBoolean checkFields = new MutableBoolean(false);
    if (includeFields != null && includeFields.length > 0) {
      if (!DB_VENDOR_ORACLE.equals(dbVendor)) {
        String[] includeFldsWithPrefix = Stream.of(includeFields).map(fld -> "a." + fld).toArray(String[]::new);
        sql = sql.substring(sql.indexOf("*") + 1);
        sql = "select " + StringUtils.join(includeFldsWithPrefix, ",") + sql;
      } else {
        int lastStarIdx;
        String preSql = sql.substring(0, lastStarIdx = sql.lastIndexOf("*"));
        sql = sql.substring(lastStarIdx + 1);
        sql = preSql + StringUtils.join(includeFields, ",") + sql;
      }
      checkFields.setValue(true);
    }
    Stream<Stf> stfs = jdbcOps.queryForStream(sql, args, (rs, arg) -> {
      Stf stf = new Stf();
      boolean checkFlds = checkFields.getValue();

      String curFld;
      if (Boolean.valueOf(curFld = ID.lowerCaseName()) || !checkFlds || ArrayUtils.contains(includeFields, curFld)) {
        stf.setId(rs.getLong(curFld));
      }
      if (Boolean.valueOf(curFld = CALLEE.lowerCaseName()) || !checkFlds
          || ArrayUtils.contains(includeFields, curFld)) {
        stf.setBody(rs.getString(curFld));
      }
      if (Boolean.valueOf(curFld = ST.lowerCaseName()) || !checkFlds || ArrayUtils.contains(includeFields, curFld)) {
        stf.setSt(rs.getString(curFld));
      }
      if (Boolean.valueOf(curFld = IS_DEAD.lowerCaseName()) || !checkFlds
          || ArrayUtils.contains(includeFields, curFld)) {
        stf.setIsDead(rs.getString(curFld));
      }
      if (Boolean.valueOf(curFld = RETRY_TIMES.lowerCaseName()) || !checkFlds
          || ArrayUtils.contains(includeFields, curFld)) {
        stf.setRetryTimes(rs.getInt(curFld));
      }
      if (Boolean.valueOf(curFld = TIMEOUT_SECS.lowerCaseName()) || !checkFlds
          || ArrayUtils.contains(includeFields, curFld)) {
        stf.setTimeoutSecs(rs.getInt(curFld));
      }
      if (Boolean.valueOf(curFld = TIMEOUT_AT.lowerCaseName()) || !checkFlds
          || ArrayUtils.contains(includeFields, curFld)) {
        stf.setTimeoutAt(rs.getLong(curFld));
      }
      if (Boolean.valueOf(curFld = CT_AT.lowerCaseName()) || !checkFlds || ArrayUtils.contains(includeFields, curFld)) {
        stf.setCtAt(rs.getLong(curFld));
      }
      if (Boolean.valueOf(curFld = UP_AT.lowerCaseName()) || !checkFlds || ArrayUtils.contains(includeFields, curFld)) {
        stf.setUpAt(rs.getLong(curFld));
      }
      return stf;
    });
    return stfs;
  }

  public static JobScanner of(StfJdbcOps jdbcOps) {
    return new JobScannerJdbc(jdbcOps, StfConsts.DFT_CORE_TBL_NAME);
  }

  public JobScannerJdbc(StfJdbcOps jdbcOps, String tblName) throws RuntimeException {
    this.jdbcOps = jdbcOps;
    this.includeHowManyDaysAgo = DFT_INCLUDE_HOW_MANY_DAYS_AGO;

    String mainSqlTpl = "select a.* from %s a inner join (select id from %s where timeout_at between ? and ? and st = ? and is_dead = ?%s) b on a.id = b.id";
    String orderSqlTpl = " order by %stimeout_at, %sid";// To get certain order
    Stream.of(tblName, tblName + DFT_DELAY_TBL_NAME_SUFFIX).forEach(tbl -> {
      String mysql = lenientFormat(mainSqlTpl, tbl, tbl, lenientFormat(orderSqlTpl + " limit ?", "", ""));
      String mainSqlNoLimit;
      String postgres = (mainSqlNoLimit = lenientFormat(mainSqlTpl, tbl, tbl, ""))
          + lenientFormat(orderSqlTpl + " limit ?", "a.", "a.");// To get certain order on PG
      String mainSqlNoLimitNoJoin = mainSqlNoLimit
          .substring(mainSqlNoLimit.indexOf("(") + 1, mainSqlNoLimit.indexOf(")"))
          .replaceFirst("select id", "select *");
      String oracle = lenientFormat(
          "select * from (select t_temp.*, rownum rn from (%s) t_temp where rownum <= ?) where rn > ?%s",
          mainSqlNoLimitNoJoin, lenientFormat(orderSqlTpl, "", ""));
      if (!tbl.endsWith(DFT_DELAY_TBL_NAME_SUFFIX)) {
        SQL_CORE_MYSQL = mysql;
        SQL_CORE_POSTGRES = postgres;
        SQL_CORE_ORACLE = oracle;
        return;
      }
      SQL_DELAY_MYSQL = mysql;
      SQL_DELAY_POSTGRES = postgres;
      SQL_DELAY_ORACLE = oracle;
    });
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