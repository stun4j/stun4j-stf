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
package com.stun4j.stf.sample.boot.facade;

import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAmount;
import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAcctNos;

import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.sample.boot.application.AppService;

/**
 * @author Jay Meng
 */
@RestController
@RequestMapping("test")
public class TestRs {
  @Autowired
  AppService svc;

  @RequestMapping
  String index() {
    String[] acctNos = generateAcctNos();
    String amt = generateAmount();
    String reqId = svc.acceptReq(acctNos[0], acctNos[1], amt);
    return reqId;
  }
}
