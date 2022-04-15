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

import com.stun4j.stf.core.support.BaseEntity;

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
public class Stf extends BaseEntity<Long> {
  private Long id;

  private String type;
  private String body;
  private String st;
  private String isDead;
  private String isRunning;
  private int retryTimes;

  private long ctAt;
  private long upAt;

  @Override
  public Long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getBody() {
    return body;
  }

  public String getSt() {
    return st;
  }

  public String getIsDead() {
    return isDead;
  }

  public String getIsRunning() {
    return isRunning;
  }

  public int getRetryTimes() {
    return retryTimes;
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

  public void setType(String type) {
    this.type = type;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setSt(String st) {
    this.st = st;
  }

  public void setIsDead(String isDead) {
    this.isDead = isDead;
  }

  public void setIsRunning(String isRunning) {
    this.isRunning = isRunning;
  }

  public void setRetryTimes(int retryTimes) {
    this.retryTimes = retryTimes;
  }

  public void setCtAt(long ctAt) {
    this.ctAt = ctAt;
  }

  public void setUpAt(long upAt) {
    this.upAt = upAt;
  }
}
