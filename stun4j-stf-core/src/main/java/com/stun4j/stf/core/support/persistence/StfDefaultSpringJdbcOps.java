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
package com.stun4j.stf.core.support.persistence;

import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.core.SpringVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.utils.functions.TriFunction;

/** @author Jay Meng */
public class StfDefaultSpringJdbcOps implements StfJdbcOps {
  private final JdbcTemplate rawOps;
  private final TriFunction<String, Object[], StfJdbcRowMapper<?>, Stream<?>> qryFn;

  @Override
  public DataSource getDataSource() {
    return rawOps.getDataSource();
  }

  @SuppressWarnings("unchecked")
  @Override
  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed (e.g. through a
   *         try-with-resources clause)
   */
  public <T> Stream<T> queryForStream(String sql, Object[] args, StfJdbcRowMapper<T> rowMapper)
      throws DataAccessException {
    return (Stream<T>)qryFn.apply(sql, args, rowMapper);
  }

  @Override
  public int update(String sql, Object... args) throws DataAccessException {
    return rawOps.update(sql, args);
  }

  @Override
  public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
    return rawOps.batchUpdate(sql, batchArgs);
  }

  public StfDefaultSpringJdbcOps(DataSource ds) {
    this(new JdbcTemplate(ds));
  }

  @SuppressWarnings("deprecation")
  public StfDefaultSpringJdbcOps(JdbcTemplate jdbcOps) {
    this.rawOps = jdbcOps;
    String springVer = SpringVersion.getVersion();
    boolean supportStreamQry = springVer.substring(0, 3).compareTo("5.3") >= 0;
    qryFn = supportStreamQry
        ? (sql, args, rowMapper) -> rawOps.queryForStream(sql, (rs, rowNum) -> rowMapper.mapRow(rs, rowNum), args)
        : (sql, args, rowMapper) -> rawOps.query(sql, args, (rs, rowNum) -> rowMapper.mapRow(rs, rowNum)).stream();

  }

  public JdbcTemplate getRawOps() {
    return rawOps;
  }

}