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
package com.stun4j.stf.sample.boot.utils.mock_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Jay Meng
 */
@Component
public class MockHelper {

  @Autowired
  JdbcTemplate jdbc;

  Integer decrementAndGet(Class<?> sampleClz) {
    String sampleId = sampleClz.getSimpleName();
    Integer cur = jdbc.queryForObject(String.format("select value from stn_stf_sample_mock where id = '%s'", sampleId),
        Integer.class);
    int res = jdbc.update(
        String.format("update stn_stf_sample_mock set value = value - 1 where id = '%s' and value = ?", sampleId), cur);
    return res == 1 ? cur - 1 : decrementAndGet(sampleClz);
  }

  public boolean anError(Class<?> sampleClz) {
    boolean error = false;
    if (decrementAndGet(sampleClz) >= 0) {
      error = true;
    }
    return error;
  }
}
