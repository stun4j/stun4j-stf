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

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StfMetaGroupEnum.CORE;
import static com.stun4j.stf.core.StfMetaGroupEnum.DELAY;
import static com.stun4j.stf.core.job.JobConsts.KEY_FEATURE_TIMEOUT_DELAY;

import java.lang.reflect.Method;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.utils.DataSourceUtils;

/** @author Jay Meng */
public class StfHelper {
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

  void tryCommitLaStfOnDup(Logger logger, Long laStfId, String dupOccurredTblName, DuplicateKeyException e,
      Consumer<Long> commitLastDoneFn) {
    Throwable rootCause;
    if ((rootCause = e.getRootCause()) != null && rootCause instanceof SQLIntegrityConstraintViolationException) {
      String ex;
      // TODO mj:be care of the accuracy of this feature recognition
      if ((ex = e.getMessage()) != null && (ex.indexOf("Duplicate") != -1 && ex.indexOf("PRIMARY") != -1)
          && (ex.indexOf("insert".toLowerCase()) != -1 || ex.indexOf("insert".toUpperCase()) != -1)
          && (ex.indexOf(dupOccurredTblName.toLowerCase()) != -1
              || ex.indexOf(dupOccurredTblName.toUpperCase()) != -1)) {
        logger.warn(
            "It seems that the stf#{} has been saved successfully, we are now trying to commit last stf or stf-delay...",
            laStfId, e);
        commitLastDoneFn.accept(laStfId);
      }
    }
  }

  public void logOnDataSourceClose(Logger logger, String title) {
    logOnDataSourceClose(logger, title, null);
  }

  public void logOnDataSourceClose(Logger logger, String title, Pair<String, Object> opTarget) {
    if (logger == null || !logger.isDebugEnabled()) {
      return;
    }
    String msgTpl = opTarget == null ? " " : lenientFormat(" on %s:%s ", opTarget.getKey(), opTarget.getValue());
    logger.debug("[{}] The dataSource has been closed and the operation{}is cancelled.", title, msgTpl);
  }

  public StfMetaGroupEnum determineMetaGroupBy(String jobGroup) {
    if (jobGroup.indexOf(KEY_FEATURE_TIMEOUT_DELAY) == -1) {
      return CORE;
    }
    return DELAY;
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
}
