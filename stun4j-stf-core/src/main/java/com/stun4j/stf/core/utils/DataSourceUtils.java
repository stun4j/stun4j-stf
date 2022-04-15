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
package com.stun4j.stf.core.utils;

import static com.google.common.base.Strings.lenientFormat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

/** @author Jay Meng */
public abstract class DataSourceUtils {
  public static final String DB_VENDOR_ORACLE = "Oracle";
  public static final String DB_VENDOR_MY_SQL = "MySQL";
  public static final String DB_VENDOR_POSTGRE_SQL = "PostgreSQL";

  public static String getDatabaseProductName(DataSource ds) throws RuntimeException {
    try (Connection conn = ds.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();
      String dbVendor = metaData.getDatabaseProductName();
      if (DB_VENDOR_MY_SQL.equals(dbVendor) || DB_VENDOR_ORACLE.equals(dbVendor)
          || DB_VENDOR_POSTGRE_SQL.equals(dbVendor)) {
        return dbVendor;
      }
      throw new RuntimeException(
          lenientFormat("Not supported db[%s], only 'MySQL','PostgreSQL','Oracle' are supported", dbVendor));
    } catch (Exception e) {
      throw Exceptions.sneakyThrow(e);
    }
  }
}