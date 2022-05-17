package com.stun4j.stf.sample.boot.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.core.StfDelayQueue;
import com.stun4j.stf.sample.boot.utils.mock_data.Data;

@RestController
@RequestMapping("test3")
public class TestRs3 {

  @Autowired
  private StfDelayQueue queue;

  @RequestMapping
  public Long index() {
    Long taskNo = queue.offer("bizOrphanStep", "handle", 30, Data.generateReq());
    return taskNo;
  }
}
