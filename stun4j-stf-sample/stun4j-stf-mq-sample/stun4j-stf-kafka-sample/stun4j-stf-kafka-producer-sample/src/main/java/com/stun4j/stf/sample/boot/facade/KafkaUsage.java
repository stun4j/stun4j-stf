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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.sample.boot.application.AppServiceWithMq;
import com.stun4j.stf.sample.domain.Req;
import com.stun4j.stf.sample.utils.mock_data.Data;

/**
 * @author Jay Meng
 */
@RestController
@RequestMapping("kafka")
public class KafkaUsage {
  @Autowired
  private AppServiceWithMq svc;

  @RequestMapping
  String index() {
    Req req = Data.generateReq();
    svc.acceptReq(req);
    return req.getId();
  }
}
