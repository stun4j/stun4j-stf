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

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
//import org.apache.rocketmq.common.message.Message;
//import org.apache.rocketmq.common.message.MessageConst;

/**
 * @author Jay Meng
 */
@Component
public class AppServiceConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(AppServiceConsumer.class);

  @Bean
  public NewTopic topic() {
    return TopicBuilder.name("bar").build();
  }

  @KafkaListener(groupId = "foo", topics = "bar")
  public void onMessage(Message<?> msg) {
    // String reqId = msg.getUserProperty("bizReqId");
    // if (mock.newError(this.getClass(), RETURN, LOG, "Notification of request#%s will be timed out...", reqId)
    // .has()) {/*- Here we simply simulated multiple timeouts.You can clearly see the ladder of retry intervals. */
    // return;
    // }
    LOG.info("Received message: {}", msg);
    byte[] laStfIdBytes = msg.getHeaders().get("__stn_laStfId", byte[].class);
    // ByteBuffer buffer = ByteBuffer.wrap(laStfIdBytes, 0, 8);
    // long laStfId = buffer.getLong();
    long laStfId = Long.parseLong(new String(laStfIdBytes, StandardCharsets.UTF_8));
    // long laStfId = buffer.order(ByteOrder.nativeOrder()).getLong();
    // System.out.println(laStfId);
    // Long laStfId = msg.getHeaders().get("__stn_laStfId", Long.class);
    directCommitLastDone(laStfId);
  }

  // @Autowired
  // MockHelper mock;
}