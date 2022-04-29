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

import static com.stun4j.stf.core.StfContext.commitLastDone;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.support.executor.StfExecutorService;
import com.stun4j.stf.sample.boot.domain.BizServiceMultiStep;
import com.stun4j.stf.sample.boot.domain.Req;
import com.stun4j.stf.sample.boot.domain.Tx;

/**
 * @author Jay Meng
 */
@Service("bizApp")
public class AppService {
  private static final Logger LOG = LoggerFactory.getLogger(AppService.class);

  @Autowired
  private LocalGuid guid;

  @Autowired
  private StfExecutorService stfExec;

  @Autowired
  private BizServiceMultiStep svc;

  public String acceptReq(String acctNoFrom, String acctNoTo, String amt) {
    Req req = new Req(guid.next() + "", acctNoFrom, acctNoTo, amt);
    Tx txBegin = svc.acceptReq(req);
    Long txId = txBegin.getId();
    String reqId = txBegin.getReqId();
    stfExec.execute(() -> {
      /*-
       * Several points are demonstrated here, as follows:
       * 1.Stf supports basic type parameter transmission.
       * 
       * 2.Performance tips: In Stf, you should always consider minimizing the serialized size of Stf state object in
       * the underlying storage.
       */
      Tx txToStep2 = svc.step1Tx(txId, reqId);
      stfExec.execute(() -> {
        Tx txToEnd = svc.step2Tx(txToStep2);
        if (txToEnd == null) {
          return;
        }
        stfExec.execute(() -> {
          svc.endTx(txToEnd);

          this.sendNotification(reqId);
        });
      });
    });
    return req.getId();
  }

  public void sendNotification(String reqId) {// Here we simply simulated a timeout
    if (once.compareAndSet(false, true)) {
      LOG.error("Notification of request#{} has timed out...", reqId);
      return;
    }
    LOG.info("Notification of request#{} was sent successfully", reqId);
    commitLastDone();

    /*
     * An equivalent piece of code is written as follows:
     * (More transparent, but the code above is better in performance)
     */
    // if (once.compareAndSet(false, true)) {
    // LOG.error("Notification of request#{} has timed out...", reqId);
    // return;
    // }
    // txnOps.executeWithoutResult(st -> {
    // LOG.info("Notification of request#{} was sent successfully", reqId);
    // });
  }

  private AtomicBoolean once = new AtomicBoolean();// just for the time out mock
}
