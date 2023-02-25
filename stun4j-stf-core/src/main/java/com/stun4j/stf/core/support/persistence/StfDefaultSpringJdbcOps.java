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

import static com.stun4j.stf.core.StfConsts.allDataSourceKeys;
import static com.stun4j.stf.core.StfHelper.newHashMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.SpringVersion;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.cluster.Heartbeat;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.utils.functions.TriFunction;

/** @author Jay Meng */
public class StfDefaultSpringJdbcOps implements StfJdbcOps {

  private final Map<Enum<?>, Pair<TriFunction<String, Object[], StfJdbcRowMapper<?>, Stream<?>>, JdbcTemplate>> qryFnAndOps;

  @SuppressWarnings("unchecked")
  @Override
  /**
   * @return the result Stream, containing stf objects, needing to be closed once fully processed
   *         (e.g. through a try-with-resources clause)
   */
  public <T> Stream<T> queryForStream(Enum<?> dsKey, String sql, Object[] args, StfJdbcRowMapper<T> rowMapper)
      throws DataAccessException {
    return (Stream<T>)qryFnAndOps.get(dsKey).getLeft().apply(sql, args, rowMapper);
  }

  @Override
  public int update(Enum<?> dsKey, String sql, Object... args) throws DataAccessException {
    return qryFnAndOps.get(dsKey).getRight().update(sql, args);
  }

  @Override
  public int[] batchUpdate(Enum<?> dsKey, String sql, List<Object[]> batchArgs) throws DataAccessException {
    return qryFnAndOps.get(dsKey).getRight().batchUpdate(sql, batchArgs);
  }

  @Override
  public DataSource getDataSource(Enum<?> dsKey) {
    return qryFnAndOps.get(dsKey).getRight().getDataSource();
  }

  public JdbcTemplate getRawOps(Enum<?> dsKey) {
    return qryFnAndOps.get(dsKey).getRight();
  }

  @SuppressWarnings("deprecation")
  public StfDefaultSpringJdbcOps(StfRegistry registry, Map<String, String> allDataSourceBeanNames) {
    // this.coreOps = coreOps;
    String springVer = SpringVersion.getVersion();
    boolean supportStreamQry = springVer.substring(0, 3).compareTo("5.3") >= 0;
    this.qryFnAndOps = newHashMap(allDataSourceKeys(), (map, type) -> {
      String typedDsBeanName = allDataSourceBeanNames.get(type);
      DataSource typedOrSharedDs = (DataSource)registry.getObj(typedDsBeanName);
      JdbcTemplate typedJdbcOps = new JdbcTemplate(typedOrSharedDs);

      TriFunction<String, Object[], StfJdbcRowMapper<?>, Stream<?>> qryFn = supportStreamQry
          ? (sql, args, rowMapper) -> typedJdbcOps.queryForStream(sql, (rs, rowNum) -> rowMapper.mapRow(rs, rowNum),
              args)
          : (sql, args, rowMapper) -> typedJdbcOps.query(sql, args, (rs, rowNum) -> rowMapper.mapRow(rs, rowNum))
              .stream();
      try {
        map.put(StfMetaGroup.valueOf(type.toUpperCase()), Pair.of(qryFn, typedJdbcOps));
      } catch (Exception e) {
        map.put(Heartbeat.SIGNAL, Pair.of(qryFn, typedJdbcOps));// TODO mj:not good
      }
      return map;
    });

  }

  @Override
  public Stream<Enum<?>> getAllDataSourceKeys() {
    return qryFnAndOps.keySet().stream();
  }

}