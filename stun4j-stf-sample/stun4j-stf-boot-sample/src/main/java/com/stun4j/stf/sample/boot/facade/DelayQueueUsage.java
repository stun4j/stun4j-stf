package com.stun4j.stf.sample.boot.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stun4j.stf.core.StfDelayQueue;
import com.stun4j.stf.sample.boot.utils.mock_data.Data;

@RestController
@RequestMapping("dlq")
public class DelayQueueUsage {

  @Autowired
  private StfDelayQueue queue;

  @RequestMapping
  public Long index() {
    /*-
     * 1.If you just need a simple DelayQueue interface that doesn't require any configuration and doesn't have any
     * upstream and downstream implications.Check this sample.
     * 
     * 2.Stf DelayQueue is distributed, highly-reliable and can support any delay time with accuracy of seconds.
     * 
     * 3.Stf DelayQueue has high-throughput, high-availability and good horizontal-scaling-capability
     */
    Long taskNo = queue.offer("bizOrphanStep", "handle", 10, 30, Data.generateReq());
    return taskNo;
  }
}
