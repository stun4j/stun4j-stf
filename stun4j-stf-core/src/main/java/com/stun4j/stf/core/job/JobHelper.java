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

import java.lang.reflect.Method;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.MethodUtils;

/** @author Jay Meng */
final class JobHelper {
  public static Method tryGetDataSourceCloser(DataSource dataSource) {
    Method dsCloser;
    try {
      dsCloser = MethodUtils.getAccessibleMethod(dataSource.getClass(), "isClosed");
      @SuppressWarnings("unused")
      Boolean isDsClosed = (Boolean)dsCloser.invoke(dataSource);
    } catch (Throwable e) {
      dsCloser = null;
    }
    return dsCloser;
  }

  public static boolean isDataSourceClose(Method dataSourceCloser, DataSource dataSource) {
    if (dataSourceCloser == null || dataSource == null) {
      return false;
    }
    try {
      Boolean isClosed = (Boolean)dataSourceCloser.invoke(dataSource);
      return isClosed;
    } catch (Exception e) {
      return false;
    }
  }

  private JobHelper() {
  }
}
