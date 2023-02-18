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

import static com.stun4j.stf.core.StfConsts.allDataSourceKeys;
import static com.stun4j.stf.core.StfHelper.newHashMap;

import java.io.File;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.stun4j.stf.core.cluster.Heartbeat;
import com.stun4j.stf.core.job.JobScanner;
import com.stun4j.stf.core.job.JobScannerJdbc;
import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.SchemaFileHelper;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;
import com.stun4j.stf.core.support.registry.StfDefaultPOJORegistry;
import com.stun4j.stf.core.utils.DataSourceUtils;

public abstract class BaseContainerCase<BEAN_TYPE> {
  protected final GenericContainer db;
  protected final String tblName;
  protected final byte containerType;

  /** Focus on core biz bean assemble */
  public BEAN_TYPE bizBean() {
    return null;
  }

  public StfMetaGroup dftDsKey() {
    return StfMetaGroup.CORE;
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
    }
    // else if (DataSourceUtils.DB_VENDOR_ORACLE.equals(dbVendor)) {
    // // gvenzl/oracle-xe:11(not work on my mac)
    // // gvenzl/oracle-xe:11-slim-faststart(not work on my mac)
    // // gvenzl/oracle-xe:21-slim-faststart(not work on my mac)
    // // oracleinanutshell/oracle-xe-11g(not work under recent testcontainers)
    // db = new OracleContainer("gvenzl/oracle-xe:11")
    // .withInitScript(SchemaFileHelper.classpath(dbVendor, roundId)).withPrivilegedMode(true)
    // .withDatabaseName("testDB").withUsername("testUser").withPassword("testPassword")
    // .withConnectTimeoutSeconds(300).withStartupTimeoutSeconds(300).withStartupAttempts(300);
    // }
    else {
      throw new RuntimeException("Not supported DB verdor: " + dbVendor);
    }
    return Triple.of(tblName, schemaFileWithTblNameChanged, db);
  }

  // TODO mj:consider using generics for refactoring
  public JdbcTemplate extractNativeJdbcOps(JdbcAware inst) {
    JdbcTemplate jdbc = null;
    if (inst.getJdbcOps() instanceof StfDefaultSpringJdbcOps) {
      jdbc = ((StfDefaultSpringJdbcOps)inst.getJdbcOps()).getRawOps(StfMetaGroup.CORE);
    }
    if (jdbc == null) {
      jdbc = newJdbcTemplate(db);
    }
    return jdbc;
  }

  protected DataSource newDataSource(GenericContainer db) {
    JdbcDatabaseContainer jdbcDb = (JdbcDatabaseContainer)db;
    String url = jdbcDb.getJdbcUrl();
    String username = jdbcDb.getUsername();
    String password = jdbcDb.getPassword();
    return new DriverManagerDataSource(url, username, password);
  }

  protected JdbcTemplate newJdbcTemplate(GenericContainer db) {
    JdbcTemplate jdbcOps = new JdbcTemplate(newDataSource(db));
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

  protected Pair<StfRegistry, Map<String, String>> prepareInit() {
    String dftDsBeanName = "dataSource";
    String coreDsBeanName = dftDsBeanName;
    String delayDsBeanName = dftDsBeanName;
    String hbDsBeanName = dftDsBeanName;// TODO mj:seperate&cfg
    Map<String, String> allDataSourceBeanNames = newHashMap(allDataSourceKeys(), (map, type) -> {
      if (StfMetaGroup.CORE.nameLowerCase().equals(type)) {
        map.put(type, coreDsBeanName);
      } else if (StfMetaGroup.DELAY.nameLowerCase().equals(type)) {
        map.put(type, delayDsBeanName);
      } else if (Heartbeat.typeNameLowerCase().equals(type)) {
        map.put(type, hbDsBeanName);
      } else {
        throw new RuntimeException("Not supported datasource key '" + type + "'");
      }
      return map;
    });
    StfRegistry bizReg = new StfDefaultPOJORegistry();
    bizReg.putObj(dftDsBeanName, newDataSource(db));
    return Pair.of(bizReg, allDataSourceBeanNames);
  }

  protected StfCore newStfCore(Object biz) {
    if (biz instanceof JdbcAware) {
      return new StfCoreJdbc(((JdbcAware)biz).getJdbcOps(), tblName);
    }
    // JdbcTemplate jdbcOps = newJdbcTemplate(db);
    Pair<StfRegistry, Map<String, String>> pair = prepareInit();
    return new StfCoreJdbc(new StfDefaultSpringJdbcOps(pair.getLeft(), pair.getRight()), tblName);
  }

  protected JobScanner newJobScanner(Object biz) {
    if (biz instanceof JdbcAware) {
      return new JobScannerJdbc(((JdbcAware)biz).getJdbcOps(), tblName);
    }
    // JdbcTemplate jdbcOps = newJdbcTemplate(db);
    Pair<StfRegistry, Map<String, String>> pair = prepareInit();
    return new JobScannerJdbc(new StfDefaultSpringJdbcOps(pair.getLeft(), pair.getRight()), tblName);
  }

  protected boolean isContainerTypeJdbc() {
    return this.containerType == TestConsts.CONTAINER_TYPE_JDBC;
  }
}