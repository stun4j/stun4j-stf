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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.stun4j.stf.boot.StfProperties;
import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.sample.boot.application.AppService;
import com.stun4j.stf.sample.domain.BizServiceOrphanStep;

/**
 * @author Jay Meng
 */
@Component
public class HelpStartUp implements ApplicationContextAware, ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(HelpStartUp.class);

  @Autowired
  private StfTxnOps txnOps;
  private ApplicationContext applicationContext;

  @Autowired
  private StfProperties props;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;

  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    boolean freshStart = args.containsOption("fresh-start")
        ? Boolean.valueOf(args.getOptionValues("fresh-start").get(0))
        : true;
    if (!freshStart) {
      return;
    }
    String coreDsBeanName = props.getCore().getDatasourceBeanName();
    String dlqDsBeanName = props.getDelayQueue().getDatasourceBeanName();
    // Clean business related datas(include mock datas),comment out the following code block if you
    // don't need it//->
    LOG.info("To give this example a fresh start, we are cleaning up the data associated with...");
    JdbcTemplate jdbc = applicationContext.getBean(JdbcTemplate.class);
    Stream.of(
        new String[]{"stn_stf", "stn_stf_delay", "stn_stf_sample_req", "stn_stf_sample_tx", "stn_stf_sample_acct_op"})
        .forEach((tbl) -> {
          if (tbl.equals("stn_stf")) {
            new JdbcTemplate(applicationContext.getBean(coreDsBeanName, DataSource.class)).update("delete from " + tbl);
          } else if (tbl.equals("stn_stf_delay")) {
            new JdbcTemplate(applicationContext.getBean(dlqDsBeanName, DataSource.class)).update("delete from " + tbl);
          } else {
            jdbc.update("delete from " + tbl);
          }
        });

    txnOps.rawExecuteWithoutResult(st -> {
      jdbc.update("delete from stn_stf_sample_mock");
      try {
        jdbc.update(String.format("insert into stn_stf_sample_mock (id, value) values ('%s', 3)", // 3
            AppService.class.getSimpleName()));
      } catch (DataAccessException e) {
      }
      try {
        jdbc.update(String.format("insert into stn_stf_sample_mock (id, value) values ('%s', 1)", // 1
            BizServiceOrphanStep.class.getSimpleName()));
      } catch (DataAccessException e) {
      }
    });
    // <-

  }

}
