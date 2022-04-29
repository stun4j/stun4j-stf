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
package com.stun4j.stf.sample.boot.persistence;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.stun4j.stf.sample.boot.domain.StatusEnum;

/**
 * @author Jay Meng
 */
@TableName("tx")
public class TxPo {
  private final Long id;
  private final String reqId;
  private final String body;
  private final String st;
  private final Date ctAt;
  private final Date upAt;
  private String remark;

  public TxPo(Long id, String reqId, String body) {
    this(id, reqId, body, StatusEnum.I.name(), new Date());
  }

  public TxPo(Long id, String reqId, String body, String st, Date ctAt) {
    this.id = id;
    this.reqId = reqId;
    this.body = body;
    this.st = st;
    this.ctAt = ctAt;
    this.upAt = ctAt;
  }

  public Long getId() {
    return id;
  }

  public String getReqId() {
    return reqId;
  }

  public String getBody() {
    return body;
  }

  public String getSt() {
    return st;
  }

  public Date getCtAt() {
    return ctAt;
  }

  public Date getUpAt() {
    return upAt;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

}
