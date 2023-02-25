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
package com.stun4j.stf.core.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

/**
 * The SPI interface connects Stf to user-side jdbc operations
 * 
 * @author Jay Meng
 */
public interface StfJdbcOps {
  <T> Stream<T> queryForStream(Enum<?> dataSourceKey, String sql, Object[] args, StfJdbcRowMapper<T> rowMapper);

  int update(Enum<?> dataSourceKey, String sql, Object... args);

  int[] batchUpdate(Enum<?> dataSourceKey, String sql, List<Object[]> batchArgs);

  /**
   * @param <T> the result type
   */
  @FunctionalInterface
  interface StfJdbcRowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
  }

  DataSource getDataSource(Enum<?> dsKey);

  Stream<Enum<?>> getAllDataSourceKeys();
}