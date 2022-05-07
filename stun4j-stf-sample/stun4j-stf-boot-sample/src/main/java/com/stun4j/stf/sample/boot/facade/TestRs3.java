package com.stun4j.stf.sample.boot.facade;

import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAcctNos;
import static com.stun4j.stf.sample.boot.utils.mock_data.Data.generateAmount;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.sample.boot.domain.Req;

@RestController("entry2")
@RequestMapping("test3")
public class TestRs3 {
  @Autowired
  private StfTxnOps txnOps;
  ExecutorService E = Executors.newWorkStealingPool();
  private static final Logger LOG = LoggerFactory.getLogger(TestRs3.class);

  @RequestMapping
  public String index() {
    for (int i = 0; i < 10_0000; i++) {
      String[] acctNos = generateAcctNos();
      String acctNoFrom = acctNos[0];
      String acctNoTo = acctNos[1];
      String amt = generateAmount();
      String reqId = LocalGuid.uuid();
      E.execute(() -> {
        // Req req = txnOps.executeWithFinalResult(() -> new Req(reqId, acctNoFrom, acctNoTo, amt), st -> {
        // });
        scheduleBatch(new Req(reqId, acctNoFrom, acctNoTo, amt));
      });
    }
    return "done";
  }

  public void scheduleBatch(Req req) {
    txnOps.executeWithoutResult(req, st -> {// TODO mj:Consider a separate api for delay task
    });
  }
}
