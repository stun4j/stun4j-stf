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

import static com.stun4j.stf.core.support.SchemaFileHelper.cleanup;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;

import java.io.File;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;

@SuppressWarnings("rawtypes")
public class JobScannerJdbcMySQLTest extends JobScannerCase {
  @ClassRule
  public static final JdbcDatabaseContainer DB;
  static final String TBL_NAME;
  static final File SCHEMA_FILE_WITH_TBL_NAME_CHANGED;

  static {
    Triple<String, File, JdbcDatabaseContainer> rtn = determineJdbcMeta(DB_VENDOR_MY_SQL);
    TBL_NAME = rtn.getLeft();
    SCHEMA_FILE_WITH_TBL_NAME_CHANGED = rtn.getMiddle();
    DB = rtn.getRight();
  }

  public JobScannerJdbcMySQLTest() {
    super(DB, TBL_NAME);
  }

  @AfterClass
  public static void afterClass() {
    cleanup(SCHEMA_FILE_WITH_TBL_NAME_CHANGED);
    DB.close();
  }
}