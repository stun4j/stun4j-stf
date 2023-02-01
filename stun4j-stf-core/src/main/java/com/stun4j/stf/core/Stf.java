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
package com.stun4j.stf.core;

import static com.stun4j.stf.core.support.JsonHelper.NO_TYPING_SERIALIZER;
import static com.stun4j.stf.core.support.JsonHelper.fromJson;
import static com.stun4j.stf.core.utils.Asserts.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import com.github.luben.zstd.Zstd;
import com.stun4j.stf.core.support.BaseEntity;
import com.stun4j.stf.core.support.CompressAlgorithm;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * State transition
 * <ul>
 * <li>Forward only,that's part of how the 'Stf' got its name</li>
 * <li>This is a very simple anemic entity object,it will be persist in DB</li>
 * <li>The {@link #body} carries the core state(to be transited),which now is a flat json string.Though this is designed
 * to accommodate any business scenario,but large-state is always not recommended</li>
 * </ul>
 * @author Jay Meng
 */
public class Stf extends BaseEntity<Long> implements Cloneable {
  private static final Logger LOG = LoggerFactory.getLogger(Stf.class);
  private Long id;
  private String body;
  private byte[] bodyBytes;
  private String st;
  private String isDead;
  private int retryTimes;
  private int timeoutSecs;

  private long timeoutAt;
  private long ctAt;
  private long upAt;

  public StfCall toCallee() {
    String restored = this.evalBody().getBody();
    StfCall res = fromJson(restored, StfCall.class);
    return res;
  }

  private Stf evalBody() {
    // Uncompress and replace body if necessary
    String bodyOrMeta = requireNonNull(this.body, "The stf-body can't be null");
    byte[] bodyBytes;
    if ((bodyBytes = this.bodyBytes) == null) {
      return this;
    }
    try {
      StfCall meta = fromJson(NO_TYPING_SERIALIZER, bodyOrMeta, StfCall.class);
      CompressAlgorithm compAlgo = CompressAlgorithm.valueOf(meta.getCompAlgo());
      switch (compAlgo) {
        case ZSTD:
          int oriSize = Optional.ofNullable(meta.getZstdOriSize()).orElse(65536);
          bodyBytes = Zstd.decompress(bodyBytes, oriSize);
          break;
        case SNAPPY:
          bodyBytes = Snappy.uncompress(bodyBytes);
          break;
        default:
          break;
      }
      Stf copy = (Stf)this.clone();// a safe copy
      copy.setBody(new String(bodyBytes, StandardCharsets.UTF_8));// NONE or bytesEnabled
      return copy;
    } catch (Throwable e) {
      throw Exceptions.sneakyThrow(e, LOG, "An error occured while evaling stf-body [bodyMeta={}]", bodyOrMeta);
    }
  }

  Stf(String body, byte[] bodyBytes) {
    this.body = body;
    this.bodyBytes = bodyBytes;
  }

  public Stf() {
  }

  @Override
  public Long getId() {
    return id;
  }

  public String getBody() {
    return body;
  }

  public byte[] getBodyBytes() {
    return bodyBytes;
  }

  public String getSt() {
    return st;
  }

  public String getIsDead() {
    return isDead;
  }

  public int getRetryTimes() {
    return retryTimes;
  }

  public int getTimeoutSecs() {
    return timeoutSecs;
  }

  public long getTimeoutAt() {
    return timeoutAt;
  }

  public long getCtAt() {
    return ctAt;
  }

  public long getUpAt() {
    return upAt;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setBodyBytes(byte[] bodyBytes) {
    this.bodyBytes = bodyBytes;
  }

  public void setSt(String st) {
    this.st = st;
  }

  public void setIsDead(String isDead) {
    this.isDead = isDead;
  }

  public void setRetryTimes(int retryTimes) {
    this.retryTimes = retryTimes;
  }

  public void setTimeoutSecs(int timeoutSecs) {
    this.timeoutSecs = timeoutSecs;
  }

  public void setTimeoutAt(long timeoutAt) {
    this.timeoutAt = timeoutAt;
  }

  public void setCtAt(long ctAt) {
    this.ctAt = ctAt;
  }

  public void setUpAt(long upAt) {
    this.upAt = upAt;
  }
}
