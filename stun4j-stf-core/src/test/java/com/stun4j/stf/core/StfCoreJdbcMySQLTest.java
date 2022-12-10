package com.stun4j.stf.core;

import java.io.File;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

import com.stun4j.stf.core.support.SchemaFileHelper;
import com.stun4j.stf.core.utils.DataSourceUtils;

public class StfCoreJdbcMySQLTest extends BaseStfCoreCase {
  @ClassRule
  public static final JdbcDatabaseContainer DB;
  static final String TBL_NAME;
  static final File SCHEMA_FILE_WITH_TBL_NAME_CHANGED;

  static {
    String dbVendor = DataSourceUtils.DB_VENDOR_MY_SQL;
    Triple<String, File, Long> rtn = SchemaFileHelper.extracted(dbVendor);
    TBL_NAME = rtn.getLeft();
    SCHEMA_FILE_WITH_TBL_NAME_CHANGED = rtn.getMiddle();
    long roundId = rtn.getRight();
    DB = new MySQLContainer("mysql:5.7").withInitScript(SchemaFileHelper.classpath(dbVendor, roundId));
  }

  public StfCoreJdbcMySQLTest() {
    super(DB, TBL_NAME);
  }

  @AfterClass
  public static void afterClass() {
    SchemaFileHelper.cleanup(SCHEMA_FILE_WITH_TBL_NAME_CHANGED);
    DB.close();
  }
}
