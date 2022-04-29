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

import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAcctNos;
import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAmount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.core.support.executor.StfRunnable;
import com.stun4j.stf.sample.boot.domain.BizServiceOrphanStep;
import com.stun4j.stf.sample.boot.domain.Req;
import com.stun4j.stf.sample.boot.persistence.ReqDao;

/**
 * @author Jay Meng
 */
@RestController("entry")
@RequestMapping("test2")
public class TestRs2 {
  @Autowired
  LocalGuid guid;
  @Autowired
  BizServiceOrphanStep svc;
  @Autowired
  StfTxnOps txnOps;
  @Autowired
  ReqDao reqMapper;

  @RequestMapping
  public String index() {
    String[] acctNos = generateAcctNos();
    String acctNoFrom = acctNos[0];
    String acctNoTo = acctNos[1];
    String amt = generateAmount();
    String reqId = LocalGuid.uuid();

    /*-
     * Several points are demonstrated here, as follows:
     * 1.Sometimes a method has neither upstream nor downstream, and it is an orphan
     * method(BizServiceOrphanStep#handle). Stf also supports retry on orphan.
     * 
     * 2.In addition to thread pool, Stf also supports independent thread to complete asynchronous workflows.
     */
    Req req = txnOps.executeWithFinalResult(() -> new Req(reqId, acctNoFrom, acctNoTo, amt), st -> {
    });
    new Thread(StfRunnable.of(() -> {
      svc.handle(req);
    })).start();

    return reqId;
  }
}
