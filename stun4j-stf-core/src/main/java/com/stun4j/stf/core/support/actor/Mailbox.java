/*
 * Copyright 2015-2022 the original author or authors.
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
package com.stun4j.stf.core.support.actor;

import static com.stun4j.stf.core.utils.Utils.calculateNearestPowerOfTwo;
import static com.stun4j.stf.core.utils.Utils.pickGenericSuperTypeOf;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Actor mailbox implementation
 * @author Jay Meng
 */
public class Mailbox<T> {
  private final T[] msgs;
  private final int fixedLength;
  private final int fixedLengthMinusOne;
  private final Lock lock;
  private final Condition ifFull;
  private final Condition ifEmpty;

  private int writeIdx;
  private int readIdx;
  private int availableCnt;

  Mailbox<T> deliver(T msg) throws InterruptedException {
    if (msg == null) return this;
    lock.lockInterruptibly();
    try {
      while (availableCnt == fixedLength) {
        ifFull.await();
      }
      msgs[writeIdx++] = msg;
      if (writeIdx == fixedLength) {
        writeIdx = 0;
      }
      availableCnt++;
      ifEmpty.signal();
      return this;
    } finally {
      lock.unlock();
    }
  }

  int drainTo(Collection<? super T> col, int maxNumToTake) throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (availableCnt == 0) {
        ifEmpty.await();
      }
      return doDrainTo(col, maxNumToTake);
    } finally {
      lock.unlock();
    }
  }

  int await() throws InterruptedException {
    return await(40);
  }

  int await(int numToSpinFirst) throws InterruptedException {
    for (int i = 0; i < numToSpinFirst && availableCnt == 0; i++) {
      Thread.yield();
    }
    if (availableCnt == 0) {
      blockingAwait();
    }
    return availableCnt;
  }

  void blockingAwait() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (availableCnt == 0) {
        ifEmpty.await();
      }
    } finally {
      lock.unlock();
    }
  }

  private int doDrainTo(Collection<? super T> col, int maxNumToTake) {
    int actualNumToTake;
    if ((actualNumToTake = Math.min(availableCnt, maxNumToTake)) == 0) {
      return actualNumToTake;
    }
    int readIdxCopy = readIdx;
    for (int i = 0; i < actualNumToTake; i++) {
      int ringIdx = toRingIdx(readIdxCopy + i);
      col.add(msgs[ringIdx]);
      msgs[ringIdx] = null;
    }
    moveReadIdxBy(readIdxCopy, actualNumToTake);
    availableCnt -= actualNumToTake;
    ifFull.signalAll();
    return actualNumToTake;
  }

  private void moveReadIdxBy(int currentReadIdx, int numToTake) {
    currentReadIdx = toRingIdx(currentReadIdx + numToTake);
    this.readIdx = currentReadIdx;
  }

  private int toRingIdx(int movingIdx) {
    return movingIdx & fixedLengthMinusOne;
  }

  @SuppressWarnings("unchecked")
  Mailbox(int baseCapacity) {
    msgs = (T[])Array.newInstance(pickGenericSuperTypeOf(getClass()), calculateNearestPowerOfTwo(baseCapacity));
    fixedLengthMinusOne = (fixedLength = msgs.length) - 1;
    ifFull = (lock = new ReentrantLock()).newCondition();
    ifEmpty = lock.newCondition();
  }
}
