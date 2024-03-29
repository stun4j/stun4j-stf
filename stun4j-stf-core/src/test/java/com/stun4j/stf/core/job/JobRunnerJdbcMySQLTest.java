package com.stun4j.stf.core.job;

import static com.stun4j.stf.core.support.SchemaFileHelper.cleanup;
import static com.stun4j.stf.core.utils.DataSourceUtils.DB_VENDOR_MY_SQL;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;

@SuppressWarnings("rawtypes")
public class JobRunnerJdbcMySQLTest extends JobRunnerCase {
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

  @Override
  public void _02_delayJobTransfer() throws FileNotFoundException {
    super._02_delayJobTransfer();
  }

  public JobRunnerJdbcMySQLTest() {
    super(DB, TBL_NAME);
  }

  @AfterClass
  public static void afterClass() {
    cleanup(SCHEMA_FILE_WITH_TBL_NAME_CHANGED);
    DB.close();
  }
}
