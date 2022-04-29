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
package com.stun4j.stf.sample.boot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Jay Meng
 */
public class Tx extends Jsonable {
  private static final long serialVersionUID = 1L;
  final Long id;
  final String reqId;

  final Long acctNoFrom;
  final Long acctNoTo;
  final BigDecimal amtDelta;
  final StatusEnum st;
  String remark;

  public Tx(Long id, String reqId) {
    this(id, reqId, -1L, null);
  }

  public Tx(Long id, String reqId, Long acctNoTo, BigDecimal amtDelta) {
    this.id = id;
    this.reqId = reqId;
    this.acctNoFrom = -1L;
    this.acctNoTo = acctNoTo;
    this.amtDelta = amtDelta;
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

  private Tx() {
    this(-1L, null, -1L, null);
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

}