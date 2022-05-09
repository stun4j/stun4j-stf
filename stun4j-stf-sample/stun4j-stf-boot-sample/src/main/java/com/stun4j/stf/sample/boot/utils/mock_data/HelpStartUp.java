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

import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.stun4j.stf.core.StfTxnOps;

/**
 * @author Jay Meng
 */
@Component
public class HelpStartUp implements ApplicationContextAware {

  @Autowired
  private StfTxnOps txnOps;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    // Clean business related datas,comment out the following code block if you don't need it//->
    JdbcTemplate jdbc = applicationContext.getBean(JdbcTemplate.class);
    Stream.of(new String[]{"stn_stf", "req", "tx", "acct_op"}).forEach((tbl) -> {
      jdbc.update("delete from " + tbl);
    });

    txnOps.rawExecuteWithoutResult(st -> {
      jdbc.update("delete from stn_sample_mock");
      try {
        jdbc.update("insert into stn_sample_mock (id, value) values ('cnt', 3)");
      } catch (DataAccessException e) {
      }
    });
    // <-

  }

}
