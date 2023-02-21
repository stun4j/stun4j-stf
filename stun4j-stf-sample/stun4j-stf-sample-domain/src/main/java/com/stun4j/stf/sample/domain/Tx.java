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
package com.stun4j.stf.sample.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Jay Meng
 */
public class Tx extends Jsonable {
  private final Long id;
  private final String reqId;

  private final Long acctNoFrom;
  private final Long acctNoTo;
  private final BigDecimal amtDelta;
  private final StatusEnum st;
  private String remark;

  public Tx(Long id, String reqId) {
    this.id = id;
    this.reqId = reqId;
    this.acctNoFrom = null;
    this.acctNoTo = null;
    this.amtDelta = null;
    this.st = StatusEnum.I;
  }

  public Tx(Long id, String reqId, String acctNoFrom, String acctNoTo, String amt) {
    this.id = id;
    this.reqId = reqId;
    this.acctNoFrom = Long.parseLong(acctNoFrom);
    this.acctNoTo = Long.parseLong(acctNoTo);
    this.amtDelta = new BigDecimal(amt).divide(BigDecimal.ONE, 2, RoundingMode.HALF_UP);
    this.st = StatusEnum.I;
  }

  Tx() {
    this(null, null);
  }

  public long getId() {
    return id;
  }

  public String getReqId() {
    return reqId;
  }

  public Long getAcctNoFrom() {
    return acctNoFrom;
  }

  public Long getAcctNoTo() {
    return acctNoTo;
  }

  public BigDecimal getAmtDelta() {
    return amtDelta;
  }

  public StatusEnum getSt() {
    return st;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  @Override
  public String toString() {
    return "Tx [id=" + id + ", reqId=" + reqId + ", acctNoFrom=" + acctNoFrom + ", acctNoTo=" + acctNoTo + ", amtDelta="
        + amtDelta + ", st=" + st + ", remark=" + remark + "]";
  }

}