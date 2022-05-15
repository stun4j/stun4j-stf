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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.stun4j.stf.core.support.JdbcAware;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;

public abstract class BaseContainerCase<BEAN_TYPE> {
  protected final GenericContainer db;
  protected final String tblName;
  protected final byte containerType;

  public abstract BEAN_TYPE bizBean();

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

  // push-down 1 level
  protected StfCore newStfCore(BEAN_TYPE biz) {
    if (biz instanceof JdbcAware) {
      return new StfCoreJdbc(((JdbcAware)biz).getJdbcOps(), tblName);
    }
    return null;
  }

  protected boolean isContainerTypeJdbc() {
    return this.containerType == TestConsts.CONTAINER_TYPE_JDBC;
  }
}