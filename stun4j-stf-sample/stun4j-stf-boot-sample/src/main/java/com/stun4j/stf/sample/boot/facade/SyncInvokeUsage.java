package com.stun4j.stf.sample.boot.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.sample.boot.application.AppService;
import com.stun4j.stf.sample.boot.application.AppServiceTraditional;
import com.stun4j.stf.sample.domain.Req;
import com.stun4j.stf.sample.utils.mock_data.Data;

@RestController
@RequestMapping("sync")
public class SyncInvokeUsage {

  @Autowired
  private AppServiceTraditional svcTraditional;

  @Autowired
  private AppService appSvc;

  @RequestMapping
  String index() {
    Req req = Data.generateReq();
    svcTraditional.syncInvoke(req);

    String reqId;
    appSvc.sendNotification(reqId = req.getId());
    return reqId;
  }
}
