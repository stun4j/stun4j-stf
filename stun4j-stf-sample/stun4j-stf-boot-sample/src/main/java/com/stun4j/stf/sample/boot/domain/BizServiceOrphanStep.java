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
package com.stun4j.stf.sample.boot.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.sample.boot.persistence.ReqDao;
import com.stun4j.stf.sample.boot.persistence.ReqPo;
import com.stun4j.stf.sample.boot.utils.mock_data.MockHelper;

/**
 * @author Jay Meng
 */
@Service("bizOrphanStep")
public class BizServiceOrphanStep {
  private static final Logger LOG = LoggerFactory.getLogger(BizServiceOrphanStep.class);
  @Autowired
  private ReqDao reqDao;
  @Autowired
  private StfTxnOps txnOps;

  public void handle(Req req) {
    String reqId = req.getId();
    String reqBody = req.toJson("insert");

    boolean error = mock.anError(getClass());
    txnOps.executeWithoutResult(st -> {
      reqDao.insert(new ReqPo(reqId, reqBody));
      if (error) {/*- Here we simply simulated an error.You should clearly see the retry of this task after a while. */
        throw new RuntimeException("Sorry, DB is temporarily down!");
      }
    });
    LOG.info("The bizOrphanStep#handle is done [reqId={}]", reqId);
  }

  @Autowired
  MockHelper mock;
}
