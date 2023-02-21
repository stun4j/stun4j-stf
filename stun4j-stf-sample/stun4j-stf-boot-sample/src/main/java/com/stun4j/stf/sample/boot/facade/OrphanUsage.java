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

import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.core.support.executor.StfRunnable;
import com.stun4j.stf.sample.domain.BizServiceOrphanStep;
import com.stun4j.stf.sample.domain.Req;
import com.stun4j.stf.sample.utils.mock_data.Data;

/**
 * @author Jay Meng
 */
@RestController("entry")
@RequestMapping("orphan")
public class OrphanUsage {
  @Autowired
  private BizServiceOrphanStep svc;
  @Autowired
  private StfTxnOps txnOps;

  @RequestMapping
  public String index() {
    /*-
     * 1.Sometimes a method has neither upstream nor downstream, and it is an orphan
     * method(BizServiceOrphanStep#handle). Stf also supports retry on orphan.
     * 
     * 2.In addition to thread pool, Stf also supports independent thread to complete asynchronous workflows.
     */
    Req req = txnOps.executeWithFinalResult(() -> Data.generateReq(), st -> {
    });

    /*-
     * 3.Another thing is that you can think of Stf as a compensatory workflow that runs behind the
     * scene.
     */
    // You can comment out the following code block and take a glance, Stf will take all the control->
    new Thread(StfRunnable.of(() -> {
      svc.handle(req);
    })).start();
    // <-

    return req.getId();
  }
}
