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
import java.util.concurrent.CompletableFuture;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.stun4j.stf.core.support.executor.StfExecutorService;
import com.stun4j.stf.sample.boot.StfMessageBuilder;
import com.stun4j.stf.sample.domain.BizServiceMultiStep;
import com.stun4j.stf.sample.domain.Req;
import com.stun4j.stf.sample.domain.Tx;
import com.stun4j.stf.sample.utils.mock_data.MockHelper;

/**
 * @author Jay Meng
 */
@Service("bizAppWithMq")
public class AppServiceWithMq {
  private static final Logger LOG = LoggerFactory.getLogger(AppServiceWithMq.class);

  @Autowired
  private StfExecutorService stfExec;

  @Autowired
  private BizServiceMultiStep svc;

  @Resource
  private KafkaTemplate<?, ?> mqOps;

  public void acceptReq(Req req) {
    Tx txBegin = svc.acceptReq(req);
    Long txId = txBegin.getId();
    String reqId = req.getId();
    /*-
    * 1.Each downstream method is guaranteed to be called, even if any error occurred such as system crash, timeout, etc.
    *
    * 2.Stf supports non-forking asynchronous task chain by using StfExecutorService or StfRunnable,StfCallable.
    *
    * 3.Stf supports basic type parameter transmission.
    *
    * 4.Performance tips: In Stf, you should always consider minimizing the serialized size of Stf state object in
    * the underlying storage.
    */
    CompletableFuture.supplyAsync(() -> svc.step1Tx(txId, reqId), stfExec).thenApplyAsync(svc::step2Tx, stfExec)
        .thenApply(step2TxRes -> {
          if (Objects.equals(step2TxRes.getErrorCode(), 1)) {
            raiseIllegalStateException(LOG, "Insufficient balance of account#%s [reqId=%s]",
                step2TxRes.getTx().getAcctNoFrom(), reqId);
          }
          return step2TxRes.getTx();// 5.Here is an example of a transformation where we convert 'step2TxRes' to the
                                    // input parameter of the downstream method
        }).thenAcceptAsync(txToEnd -> {
          svc.endTx(txToEnd);

          this.sendNotification(reqId);
        }, stfExec);
  }

  public void sendNotification(String reqId) {
    // if (mock.newError(this.getClass(), THROW_EX, LOG, "Notification of request#%s will be timed
    // out...", reqId)
    // .has()) {/*- Here we simply simulated multiple timeouts.You can clearly see the ladder of retry
    // intervals. */
    // return;
    // }
    LOG.info("Notification of request#{} is sending...]", reqId);
    Message<?> msg = StfMessageBuilder.withPayload("Hi-" + reqId).setHeader(KafkaHeaders.GROUP_ID, "foo")
        .setHeader(KafkaHeaders.TOPIC, "bar").setHeader("bizReqId", reqId).build();
    mqOps.send(msg);
  }

  @Autowired
  MockHelper mock;
}
