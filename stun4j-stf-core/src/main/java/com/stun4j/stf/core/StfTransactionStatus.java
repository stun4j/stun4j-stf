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

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * To use {@link #commitLastDone()} for convenience and relative safety
 * 
 * @author Jay Meng
 */
public class StfTransactionStatus implements TransactionStatus {
  private TransactionStatus rawSt;

  @Override
  public boolean isNewTransaction() {
    return rawSt.isNewTransaction();
  }

  @Override
  public void setRollbackOnly() {
    rawSt.setRollbackOnly();
  }

  @Override
  public boolean isRollbackOnly() {
    return rawSt.isRollbackOnly();
  }

  @Override
  public boolean isCompleted() {
    return rawSt.isCompleted();
  }

  @Override
  public Object createSavepoint() throws TransactionException {
    return rawSt.createSavepoint();
  }

  @Override
  public void rollbackToSavepoint(Object savepoint) throws TransactionException {
    rawSt.rollbackToSavepoint(savepoint);
  }

  @Override
  public void releaseSavepoint(Object savepoint) throws TransactionException {
    rawSt.releaseSavepoint(savepoint);
  }

  @Override
  public boolean hasSavepoint() {
    return rawSt.hasSavepoint();
  }

  @Override
  public void flush() {
    rawSt.flush();
  }

  public void commitLastDone() {
    StfContext.commitLastDone();
  }

  StfTransactionStatus(TransactionStatus rawSt) {
    this.rawSt = rawSt;
  }

}
