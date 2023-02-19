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

import java.lang.reflect.Method;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.utils.DataSourceUtils;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.Utils;

import sun.misc.Signal;

/** @author Jay Meng */
@SuppressWarnings("restriction")
public class StfHelper {
  private final StfJdbcOps jdbcOps;

  private final Map<Enum<?>, Pair<String, Method>> map;

  public static StfHelper H;

  public static StfHelper init(StfJdbcOps jdbcOps) {
    return H = new StfHelper(jdbcOps);
  }

  public String getDbVendor(Enum<?> dsKey) {
    return map.get(dsKey).getLeft();
  }

  public Method getDsCloser(Enum<?> dsKey) {
    return map.get(dsKey).getRight();
  }

  public boolean isDataSourceClose(Enum<?> dsKey) {
    Method dsCloser = getDsCloser(dsKey);
    DataSource ds;
    if (dsCloser == null || (ds = jdbcOps.getDataSource(dsKey)) == null) {
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

  public static void registerGracefulShutdown(Logger logger, Supplier<?> shutdownGracefully) {
    try {
      Signal.handle(new Signal(Utils.getOSSignalType()), s -> {
        // Never use 'System.exit' within shutdownGracefully!!!
        shutdownGracefully.get();
      });
    } catch (Throwable e) {
      Exceptions.swallow(e, logger, e.getMessage());
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Never use 'System.exit' within shutdownGracefully!!!
      shutdownGracefully.get();
    }));
  }

  public static StfMetaGroup determinJobMetaGroup(String type) {
    int idx;
    if ((idx = ArrayUtils.indexOf(StfMetaGroup.namesLowerCase(), type)) == StfMetaGroup.CORE.ordinal()) {
      return StfMetaGroup.CORE;
    }
    if (idx == StfMetaGroup.DELAY.ordinal()) {
      return StfMetaGroup.DELAY;
    }
    return null;
  }

  public static <T, RT, RV> Map<RT, RV> newHashMap(T[] inArray, BiFunction<Map<RT, RV>, T, Map<RT, RV>> biFn) {
    return newHashMap(Stream.of(inArray), biFn);
  }

  public static <T, RT, RV> Map<RT, RV> newMap(T[] inArray, BiFunction<Map<RT, RV>, T, Map<RT, RV>> biFn,
      Supplier<Map<RT, RV>> resultInitiator) {
    return newMap(Stream.of(inArray), biFn, resultInitiator);
  }

  public static <ST, RT, RV> Map<RT, RV> newHashMap(Stream<ST> inStream,
      BiFunction<Map<RT, RV>, ST, Map<RT, RV>> biFn) {
    return newMap(inStream, biFn, HashMap::new);
  }

  public static <ST, RT, RV> Map<RT, RV> newMap(Stream<ST> inStream, BiFunction<Map<RT, RV>, ST, Map<RT, RV>> biFn,
      Supplier<Map<RT, RV>> resultInitiator) {
    try {
      return inStream.reduce(resultInitiator.get(), (resMap, streamType) -> {
        return biFn.apply(resMap, streamType);
      }, (a, b) -> null);
    } catch (Exception e) {
      throw Exceptions.sneakyThrow(e);
    }
  }

  private Method tryGetDataSourceCloser(Enum<?> dsKey) {
    DataSource ds = jdbcOps.getDataSource(dsKey);
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
    this.jdbcOps = jdbcOps;

    this.map = newHashMap(jdbcOps.getAllDataSourceKeys(), (map, dsKey) -> {
      String dbVendor = DataSourceUtils.getDatabaseProductName(jdbcOps.getDataSource(dsKey));
      Method dsCloser = tryGetDataSourceCloser(dsKey);
      map.put(dsKey, Pair.of(dbVendor, dsCloser));
      return map;
    });
  }
}
