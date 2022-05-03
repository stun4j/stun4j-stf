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

/**
 * @author Jay Meng
 */
public class TxResult {
  private final Tx tx;
  private final Integer errorCode;

  static TxResult of(Tx tx) {
    return new TxResult(tx, null);
  }

  static TxResult error(Tx tx, Integer errorCode) {
    return new TxResult(tx, errorCode);
  }

  TxResult withTxRemark(String remark) {
    tx.setRemark(remark);
    return this;
  }

  TxResult(Tx tx, Integer errorCode) {
    this.tx = tx;
    this.errorCode = errorCode;
  }

  public Tx getTx() {
    return tx;
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  @Override
  public String toString() {
    return "TxResult [tx=" + tx + ", errorCode=" + errorCode + "]";
  }

}
