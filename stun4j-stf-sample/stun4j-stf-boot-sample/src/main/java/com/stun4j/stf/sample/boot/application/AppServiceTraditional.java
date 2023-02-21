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
package com.stun4j.stf.sample.boot.application;

import static com.stun4j.stf.core.utils.Asserts.raiseIllegalStateException;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.core.support.executor.StfExecutorService;
import com.stun4j.stf.sample.domain.BizServiceMultiStep;
import com.stun4j.stf.sample.domain.Req;
import com.stun4j.stf.sample.domain.Tx;
import com.stun4j.stf.sample.domain.TxResult;
import com.stun4j.stf.sample.utils.mock_data.MockHelper;

/**
 * @author Jay Meng
 */
@Service("bizAppTraditional")
public class AppServiceTraditional {
  private static final Logger LOG = LoggerFactory.getLogger(AppServiceTraditional.class);

  @Autowired
  private BizServiceMultiStep svc;

  @Autowired
  private StfTxnOps txnOps;

  @Autowired
  private AppService appSvc;

  @Autowired
  private StfExecutorService stfExec;

  public void syncInvoke(Req req) {
    Tx txBegin = svc.acceptReq(req);
    Long txId = txBegin.getId();
    String reqId = req.getId();

    Tx tx = svc.step1Tx(txId, reqId);

    TxResult step2TxRes = svc.step2Tx(tx);

    if (Objects.equals(step2TxRes.getErrorCode(), 1)) {
      raiseIllegalStateException(LOG, "Insufficient balance of account#%s [reqId=%s]",
          step2TxRes.getTx().getAcctNoFrom(), reqId);
    }
    Tx txToEnd = step2TxRes.getTx();
    svc.endTx(txToEnd);

    // If you are currently running in an transaction,
    // For operations that are asynchronous in nature, you must keep in mind, as follows:->
    // This is not correct!!!Never do this!!!->
    // stfExec.execute(() -> {
    // appSvc.sendNotification(reqId);
    // });
    // <-

    // This is not correct too!!!->
    // appSvc.sendNotification(reqId);
    // <-
    // <-
  }

  public void syncInvokeWithNestedTransactionType1(Req req) {
    txnOps.rawExecuteWithoutResult(st -> {
      syncInvoke(req);
    });

    appSvc.sendNotification(req.getId());
  }

  public void syncInvokeWithNestedTransactionType2(Req req) {
    txnOps.executeWithoutResult(st -> {
      syncInvoke(req);
    });

    stfExec.execute(() -> {
      appSvc.sendNotification(req.getId());
    });
  }

  @Transactional
  public void syncInvokeWithNestedTransactionType3(Req req) {
    syncInvoke(req);// Note that appSvc#sendNotification will be called implicitly after a while
  }

  public void syncInvokeWithNestedTransactionType4(Req req) {
    txnOps.executeWithoutResult(st -> {
      syncInvoke(req);
    });
  }

  @Autowired
  MockHelper mock;
}
