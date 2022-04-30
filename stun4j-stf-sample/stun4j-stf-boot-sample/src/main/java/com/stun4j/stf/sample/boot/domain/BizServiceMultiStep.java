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

import static com.stun4j.stf.core.StfContext.commitLastDone;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.sample.boot.persistence.AcctDao;
import com.stun4j.stf.sample.boot.persistence.AcctOpDao;
import com.stun4j.stf.sample.boot.persistence.ReqDao;
import com.stun4j.stf.sample.boot.persistence.ReqPo;
import com.stun4j.stf.sample.boot.persistence.TxDao;
import com.stun4j.stf.sample.boot.persistence.TxPo;
import com.stun4j.stf.sample.boot.utils.JsonUtils;

/**
 * @author Jay Meng
 */
@Service("bizMultiStep")
public class BizServiceMultiStep {
  private final LoadingCache<String, String> cacheReq;

  @Autowired
  private ReqDao reqDao;
  @Autowired
  private TxDao txDao;
  @Autowired
  private AcctOpDao acctOpDao;
  @Autowired
  private AcctDao acctDao;

  @Autowired
  private StfTxnOps txnOps;
  @Autowired
  private LocalGuid guid;

  public Tx acceptReq(Req req) {
    String reqId = req.getId();
    String reqBody = req.toJson("insert");
    return txnOps.executeWithFinalResult(() -> new Tx(guid.next(), reqId), out -> st -> {
      reqDao.insert(new ReqPo(reqId, reqBody));
    });
  }

  public Tx step1Tx(Long txId, String reqId) {
    String reqBody = cacheReq.getUnchecked(reqId);
    Req req = JsonUtils.fromJson(reqBody, Req.class);
    return txnOps.executeWithFinalResult(
        () -> new Tx(txId, reqId, req.getAcctNoFrom(), req.getAcctNoTo(), req.getAmt()), out -> st -> {
          String txBody = out.toJson("insert");
          txDao.insert(new TxPo(txId, reqId, txBody));
        });
  }

  public Tx step2Tx(Tx tx) {
    Long txId = tx.getId();
    String reqId = tx.getReqId();
    Long acctNoFrom = tx.getAcctNoFrom();
    Long acctNoTo = tx.getAcctNoTo();
    BigDecimal amtDt = tx.getAmtDelta();

    Long acctOpSeqNo = guid.next();
    return txnOps.executeWithNonFinalResult(() -> new Tx(txId, reqId, acctNoTo, amtDt), out -> st -> {
      if (!acctDao.decreaseAcctAmt(amtDt, acctNoFrom)) {
        /*-
         * 1.The Stf framework guarantees retry, but sometimes the business side may need to stop retry early.The
         * StfContext#commitLastDone method is used to do this.
         * 
         * 2.Here are a few of these scenarios, e.g. insufficient balance for debits, non-existent accounts.
         * And when these occur, we also need to mark the business-transfer-transaction as a failure in a JDBC
         * transaction.
         */
        txDao.markTxFail(txId);
        commitLastDone();
        return null;
      }
      acctOpDao.insertAcctOpOfDecrement(acctOpSeqNo, acctNoFrom, amtDt, txId);
      out.setRemark("This is the right place modifing the 'out' if necessary");// Pay attention to this
      return out;
    });
  }

  public void endTx(Tx tx) {
    Long txId = tx.getId();
    Long acctNoTo = tx.getAcctNoTo();
    BigDecimal amtDt = tx.getAmtDelta();

    Long acctOpSeqNo = guid.next();
    /*-
     * 1.In conjunction with the outer control of AppService, this demonstrates another way of implicitly passing
     * parameters between the Stf framework and downstream methods(To clarify, the downstream method
     * AppService#sendNotification takes a string as an input and the current method returns a void).
     * 
     * 2.Although the Stf framework has designed some apis to avoid the side effects of object modification, we still always
     * recommend using immutable objects as output parameters. On the other hand, we do not want any changes to the
     * downstream output parameters after the Stf specific transaction is executed, which can cause potential
     * inconsistencies.
     */
    txnOps.executeWithoutResult(tx.getReqId(), st -> {
      if (!txDao.markTxSuccess(txId)) {
        commitLastDone();
        return;
      }
      if (!acctDao.increaseAcctAmt(amtDt, acctNoTo)) {
        commitLastDone();
        return;
      }
      acctOpDao.insertAcctOpOfIncrement(acctOpSeqNo, acctNoTo, amtDt, txId);
    });
  }

  {
    CacheLoader<String, String> loader = new CacheLoader<String, String>() {
      public String load(String reqId) throws Exception {
        ReqPo req = reqDao.selectOne(new QueryWrapper<ReqPo>().select("body").eq("id", reqId));
        return req.getBody();
      }
    };
    this.cacheReq = CacheBuilder.newBuilder().maximumSize(1000).weakKeys().weakValues()
        .expireAfterWrite(30, TimeUnit.SECONDS).build(loader);
  }
}
