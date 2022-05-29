package com.stun4j.stf.sample.boot.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.sample.boot.application.AppServiceTraditional;
import com.stun4j.stf.sample.boot.domain.Req;
import com.stun4j.stf.sample.boot.utils.mock_data.Data;

@RestController
@RequestMapping("nested")
public class NestedTxUsage {

  @Autowired
  private AppServiceTraditional svcTraditional;

  @RequestMapping("type1")
  String type1() {
    Req req = Data.generateReq();
    svcTraditional.syncInvokeWithNestedTransactionType1(req);
    return req.getId();
  }

  @RequestMapping("type2")
  String type2() {
    Req req = Data.generateReq();
    svcTraditional.syncInvokeWithNestedTransactionType2(req);
    return req.getId();
  }

  @RequestMapping("type3")
  String type3() {
    Req req = Data.generateReq();
    svcTraditional.syncInvokeWithNestedTransactionType3(req);
    return req.getId();
  }
}
