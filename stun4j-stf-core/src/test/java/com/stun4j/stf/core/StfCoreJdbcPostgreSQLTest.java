package com.stun4j.stf.core;

import java.io.File;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.stun4j.stf.core.support.SchemaFileHelper;
import com.stun4j.stf.core.utils.DataSourceUtils;

public class StfCoreJdbcPostgreSQLTest extends BaseStfCoreCase {
  @ClassRule
  public static final JdbcDatabaseContainer DB;
  static final String TBL_NAME;
  static final File SCHEMA_FILE_WITH_TBL_NAME_CHANGED;

  static {
    String dbVendor = DataSourceUtils.DB_VENDOR_POSTGRE_SQL;
    Triple<String, File, Long> rtn = SchemaFileHelper.extracted(dbVendor);
    TBL_NAME = rtn.getLeft();
    SCHEMA_FILE_WITH_TBL_NAME_CHANGED = rtn.getMiddle();
    long roundId = rtn.getRight();
    DB = new PostgreSQLContainer("postgres").withInitScript(SchemaFileHelper.classpath(dbVendor, roundId));
  }

  public StfCoreJdbcPostgreSQLTest() {
    super(DB, TBL_NAME);
  }

  @AfterClass
  public static void afterClass() {
    SchemaFileHelper.cleanup(SCHEMA_FILE_WITH_TBL_NAME_CHANGED);
    DB.close();
  }
}
