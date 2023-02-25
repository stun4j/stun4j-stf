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
package com.stun4j.stf.sample.boot;

import org.springframework.messaging.Message;
//import org.springframework.lang.Nullable;
import org.springframework.messaging.support.MessageBuilder;

import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.utils.Asserts;

/**
 * @author Jay Meng
 */
public final class StfMessageBuilder<T> {
  public static final String LA_STF_ID_KEY = "__stn_laStfId";
  private final MessageBuilder<T> builder;

  /**
   * Set the value for the given header name. If the provided value is {@code null}, the header will
   * be removed.
   */
  public StfMessageBuilder<T> setHeader(String headerName, Object headerValue) {// @Nullable Object headerValue
    Asserts.argument(!LA_STF_ID_KEY.equalsIgnoreCase(headerName), "'%s' is reserved，not allowed to use", headerName);
    builder.setHeader(headerName, headerValue);
    return this;
  }

  public Message<T> build() {
    return builder.build();
  }

  /**
   * Create a new builder for a message with the given payload.
   * 
   * @param payload the payload
   */
  public static <T> StfMessageBuilder<T> withPayload(T payload) {
    return new StfMessageBuilder<>(MessageBuilder.withPayload(payload));
  }

  public StfMessageBuilder(MessageBuilder<T> builder) {
    Long laStfId = StfContext.safeGetLaStfIdValue();
    builder.setHeader(LA_STF_ID_KEY, laStfId);
    this.builder = builder;
  }

}
