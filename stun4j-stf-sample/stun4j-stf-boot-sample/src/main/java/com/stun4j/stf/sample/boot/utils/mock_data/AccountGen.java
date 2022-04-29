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

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Jay Meng
 */
public class AccountGen {
  public static void main(String[] args) throws Exception {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:mysql://localhost/test");
    ds.setUsername("root");
    ds.setPassword("1111");
    ds.setMaximumPoolSize(20);
    JdbcTemplate jdbcOps = new JdbcTemplate(ds);

    String sql = "insert into acct (no, amt, freeze_amt, st, up_at, ct_at) values(?, ?, ?, ?, ?, ?)";

    ThreadPoolExecutor E = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 40, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(1024), new ThreadPoolExecutor.CallerRunsPolicy());
    CountDownLatch latch = new CountDownLatch(Data.ACCT_NO_RANGE);
    for (int i = 0; i < Data.ACCT_NO_RANGE; i++) {
      if (i > 0 && i % 10000 == 0) {
        System.out.println(i);
      }
      int acctNoSuffix = i;
      E.execute(() -> {
        String acctNo = Data.ACCT_NO_PREFIX + acctNoSuffix + "";
        Date now = new Date();
        jdbcOps.update(sql, Long.parseLong(acctNo), 1000_0000, 0, "N", now, now);
        latch.countDown();
      });
    }
    latch.await();
    System.exit(1);
  }
}