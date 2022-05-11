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
package com.stun4j.stf.core.support;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.utils.DataSourceUtils;

/** @author Jay Meng */
public class StfHelper {
  private static final Map<?, LocalGuid> CACHED_GUID;
  private final DataSource ds;
  private final String dbVendor;
  private final Method dsCloser;

  public static StfHelper H;

  public static StfHelper init(StfJdbcOps jdbcOps) {
    return H = new StfHelper(jdbcOps);
  }

  public String getDbVendor() {
    return dbVendor;
  }

  public Method getDsCloser() {
    return dsCloser;
  }

  public boolean isDataSourceClose() {
    if (dsCloser == null || ds == null) {
      return false;
    }
    try {
      Boolean isClosed = (Boolean)dsCloser.invoke(ds);
      return isClosed;
    } catch (Exception e) {
      return false;
    }
  }

  public LocalGuid cachedGuid() {
    // TODO mj:extract 'single null-value cache' utility?or upgrade LocalGuid by applying empty-object pattern?
    return CACHED_GUID.computeIfAbsent(null, k -> LocalGuid.instance());
  }

  private Method tryGetDataSourceCloser() {
    Method dsCloser;
    try {
      dsCloser = MethodUtils.getAccessibleMethod(ds.getClass(), "isClosed");
      @SuppressWarnings("unused")
      Boolean isDsClosed = (Boolean)dsCloser.invoke(ds);
    } catch (Throwable e) {
      dsCloser = null;
    }
    return dsCloser;
  }

  public StfHelper(StfJdbcOps jdbcOps) {
    this.dbVendor = DataSourceUtils.getDatabaseProductName(this.ds = jdbcOps.getDataSource());
    this.dsCloser = tryGetDataSourceCloser();
  }

  static {
    CACHED_GUID = new HashMap<>(1);// not very strict in high concurrency scenarios,dosen't matter
  }

}
