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

import java.io.File;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.stun4j.stf.core.job.JobScanner;
import com.stun4j.stf.core.job.JobScannerJdbc;
import com.stun4j.stf.core.spi.StfJdbcOps.StfJdbcRowMapper;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.SchemaFileHelper;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;

import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.CALLEE;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.CT_AT;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.ID;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.IS_DEAD;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.RETRY_TIMES;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.ST;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.TIMEOUT_AT;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.TIMEOUT_SECS;
import static com.stun4j.stf.core.StfConsts.StfDbFieldEnum.UP_AT;
import com.stun4j.stf.core.utils.DataSourceUtils;

public abstract class BaseContainerCase<BEAN_TYPE> {
  protected final GenericContainer db;
  protected final String tblName;
  protected final byte containerType;

  public static final StfJdbcRowMapper<Stf> STF_ROW_MAPPER = (rs, arg) -> {
    Stf stf = new Stf();
    stf.setId(rs.getLong(ID.nameLowerCase()));
    stf.setBody(rs.getString(CALLEE.nameLowerCase()));
    stf.setSt(rs.getString(ST.nameLowerCase()));
    stf.setIsDead(rs.getString(IS_DEAD.nameLowerCase()));
    stf.setRetryTimes(rs.getInt(RETRY_TIMES.nameLowerCase()));
    stf.setTimeoutSecs(rs.getInt(TIMEOUT_SECS.nameLowerCase()));
    stf.setTimeoutAt(rs.getLong(TIMEOUT_AT.nameLowerCase()));
    stf.setCtAt(rs.getLong(CT_AT.nameLowerCase()));
    stf.setUpAt(rs.getLong(UP_AT.nameLowerCase()));
    return stf;
  };

  /** Focus on core biz bean assemble */
  public BEAN_TYPE bizBean() {
    return null;
  }

  @SuppressWarnings("resource")
  protected static Triple<String, File, JdbcDatabaseContainer> determineJdbcMeta(String dbVendor) {
    Triple<String, File, Long> rtn = SchemaFileHelper.extracted(dbVendor);
    String tblName = rtn.getLeft();
    File schemaFileWithTblNameChanged = rtn.getMiddle();
    long roundId = rtn.getRight();
    JdbcDatabaseContainer db;
    if (DataSourceUtils.DB_VENDOR_MY_SQL.equals(dbVendor)) {
      db = new MySQLContainer("mysql:5.7").withInitScript(SchemaFileHelper.classpath(dbVendor, roundId));
    } else if (DataSourceUtils.DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
      db = new PostgreSQLContainer("postgres").withInitScript(SchemaFileHelper.classpath(dbVendor, roundId));
    } else {
      throw new RuntimeException("Not supported DB verdor: " + dbVendor);
    }
    return Triple.of(tblName, schemaFileWithTblNameChanged, db);
  }

  // TODO mj:consider using generics for refactoring
  public JdbcTemplate extractNativeJdbcOps(JdbcAware inst) {
    JdbcTemplate jdbc = null;
    if (inst.getJdbcOps() instanceof StfDefaultSpringJdbcOps) {
      jdbc = ((StfDefaultSpringJdbcOps)inst.getJdbcOps()).getRawOps();
    }
    if (jdbc == null) {
      jdbc = newJdbcTemplate(db);
    }
    return jdbc;
  }

  protected JdbcTemplate newJdbcTemplate(GenericContainer db) {
    JdbcDatabaseContainer jdbcDb = (JdbcDatabaseContainer)db;
    String url = jdbcDb.getJdbcUrl();
    String username = jdbcDb.getUsername();
    String password = jdbcDb.getPassword();
    JdbcTemplate jdbcOps = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
    return jdbcOps;
  }

  protected BaseContainerCase(GenericContainer db) {
    this(db, StfConsts.DFT_CORE_TBL_NAME);
  }

  protected BaseContainerCase(GenericContainer db, String tblName) {
    this.db = db;
    this.tblName = tblName;

    // if (db instanceof JdbcDatabaseContainer) {
    // this.containerType = TestConsts.CONTAINER_TYPE_JDBC;
    // }
    this.containerType = TestConsts.CONTAINER_TYPE_JDBC;
  }

  protected StfCore newStfCore() {
    return newStfCore(null);
  }

  protected StfDelayQueueCore newStfDelayQueueCore() {
    return (StfDelayQueueCore)newStfCore();
  }

  protected StfDelayQueueCore newStfDelayQueueCore(StfCore stfc) {
    return (StfDelayQueueCore)stfc;
  }

  protected StfCore newStfCore(Object biz) {
    if (biz instanceof JdbcAware) {
      return new StfCoreJdbc(((JdbcAware)biz).getJdbcOps(), tblName);
    }
    JdbcTemplate jdbcOps = newJdbcTemplate(db);
    return new StfCoreJdbc(new StfDefaultSpringJdbcOps(jdbcOps), tblName);
  }

  protected JobScanner newJobScanner(Object biz) {
    if (biz instanceof JdbcAware) {
      return new JobScannerJdbc(((JdbcAware)biz).getJdbcOps(), tblName);
    }
    JdbcTemplate jdbcOps = newJdbcTemplate(db);
    return new JobScannerJdbc(new StfDefaultSpringJdbcOps(jdbcOps), tblName);
  }

  protected boolean isContainerTypeJdbc() {
    return this.containerType == TestConsts.CONTAINER_TYPE_JDBC;
  }
}