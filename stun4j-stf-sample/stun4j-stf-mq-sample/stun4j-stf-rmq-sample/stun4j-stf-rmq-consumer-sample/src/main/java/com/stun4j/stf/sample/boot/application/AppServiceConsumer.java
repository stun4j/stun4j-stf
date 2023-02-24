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

import static com.stun4j.stf.core.StfThinContext.directCommitLastDone;
import static com.stun4j.stf.sample.boot.utils.mock_data.MockHelper.MockErrorTypeEnum.RETURN;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stun4j.stf.sample.boot.utils.mock_data.MockHelper;

//import org.apache.rocketmq.common.message.Message;
//import org.apache.rocketmq.common.message.MessageConst;

/**
 * @author Jay Meng
 */
@Component
@RocketMQMessageListener(consumerGroup = "foo", topic = "bar")
public class AppServiceConsumer implements RocketMQListener<MessageExt> {
  private static final Logger LOG = LoggerFactory.getLogger(AppServiceConsumer.class);

  @Override
  public void onMessage(MessageExt msg) {
    String reqId = msg.getUserProperty("bizReqId");
    if (mock.newError(this.getClass(), RETURN, LOG, "Notification of request#%s will be timed out...", reqId)
        .has()) {/*- Here we simply simulated multiple timeouts.You can clearly see the ladder of retry intervals. */
      return;
    }
    LOG.info("Received message: {}", msg);
    Long laStfId = Long.valueOf(msg.getUserProperty("__stn_laStfId"));
    directCommitLastDone(laStfId);
  }

  @Autowired
  MockHelper mock;
}