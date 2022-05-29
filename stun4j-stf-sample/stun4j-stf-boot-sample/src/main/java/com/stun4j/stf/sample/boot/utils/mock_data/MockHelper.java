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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
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

  Long decrementAndGet(Class<?> sampleClz) {
    String sampleId = sampleClz.getSimpleName();
    Long cur = jdbc.queryForObject(String.format("select value from stn_stf_sample_mock where id = '%s'", sampleId),
        Long.class);
    int res = jdbc.update(
        String.format("update stn_stf_sample_mock set value = value - 1 where id = '%s' and value = ?", sampleId), cur);
    return res == 1 ? cur - 1 : decrementAndGet(sampleClz);
  }

  public MockError newError(Class<?> sampleClz) {
    return newError(sampleClz, MockErrorTypeEnum.RETURN, null, null);
  }

  public MockError newError(Class<?> sampleClz, MockErrorTypeEnum errorType, Logger logger, String fmt,
      Object... params) {
    try {
      return CompletableFuture.supplyAsync(() -> {
        if (decrementAndGet(sampleClz) < 0) {
          return new MockError(false, null);
        }
        String msg = null;
        if (logger != null && fmt != null) {
          msg = String.format(fmt, params);
          logger.error(msg);
        }
        String finalMsg = msg;
        return new MockError(true, () -> {
          if (errorType == MockErrorTypeEnum.RETURN) {
            return finalMsg;
          } else if (errorType == MockErrorTypeEnum.THROW_EX) {
            throw new RuntimeException(Optional.ofNullable(finalMsg).orElse("Unexpected error occured!"));
          }
          System.exit(-1);
          return null;
        });
      }).get();
    } catch (Exception e) {
      return new MockError(false, null);
    }
  }

  public class MockError {
    Pair<Boolean, Supplier<?>> pair;

    public boolean has() {
      if (pair.getKey()) {
        returnOrThrow().get();
        return true;
      }
      return false;
    }

    public Supplier<?> returnOrThrow() {
      return pair.getValue();
    }

    public MockError(boolean error, Supplier<?> errRtn) {
      this.pair = Pair.of(error, errRtn);
    }

  }

  public enum MockErrorTypeEnum {
    THROW_EX, SYS_EXIT, RETURN
  }
}
