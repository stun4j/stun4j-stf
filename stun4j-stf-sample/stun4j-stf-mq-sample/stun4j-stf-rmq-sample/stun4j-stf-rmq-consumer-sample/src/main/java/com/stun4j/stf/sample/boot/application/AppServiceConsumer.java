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

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//import org.apache.rocketmq.common.message.Message;
//import org.apache.rocketmq.common.message.MessageConst;

/**
 * @author Jay Meng
 */
@Component
@RocketMQMessageListener(topic = "bar", consumerGroup = "foo")
public class AppServiceConsumer implements RocketMQListener<MessageExt> {
  private static final Logger LOG = LoggerFactory.getLogger(AppServiceConsumer.class);

  @Override
  public void onMessage(MessageExt msg) {
    LOG.info("received message: {}", msg);
    Long laStfId = Long.valueOf(msg.getUserProperty("__stn_laStfId"));
    directCommitLastDone(laStfId);
  }

}