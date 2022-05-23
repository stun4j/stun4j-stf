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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.stf.core.support.BaseLifeCycle;
import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.Utils;
import com.stun4j.stf.core.utils.executor.NamedThreadFactory;

/**
 * Base class for actor implementation
 * @author Jay Meng
 */
public abstract class BaseActor<T> extends BaseLifeCycle implements Actor<T> {
  protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private static final int[] MSGS_SIZE_LADDER;
  private static final int MSGS_SIZE_LADDER_LENGTH;
  private static final int MSGS_SIZE_LADDER_LENGTH_MINUS_ONE;
  private final Mailbox<T> mailbox;
  private final Thread worker;
  private final String name;

  private volatile boolean shutdown;
  private int msgSizeScaleStep;
  private int msgsLastDrained;

  @Override
  public void doStart() {
    worker.start();
    LOG.debug("The {} is successfully started", name);
  }

  @Override
  public void doShutdown() {
    shutdown = true;
    worker.interrupt();
    LOG.debug("The {} is successfully shut down", name);
  }

  @Override
  public void tell(T msg) {
    try {
      mailbox.deliver(msg);
    } catch (InterruptedException e) {// TODO mj:log stuff
      Thread.currentThread().interrupt();
      LOG.info("[tell] The {} seems going through a shutdown", name);
    }
  }

  protected abstract void onMsgs(List<T> msgs) throws InterruptedException;

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted() && !shutdown) {
        mailbox.await();
        List<T> msgs;
        int drained = mailbox.drainTo(msgs = new ArrayList<>(MSGS_SIZE_LADDER[msgSizeScaleStep]),
            1000);/*-TODO mj:to be configured*/
        try {
          onMsgs(msgs);
        } catch (Throwable e) {
          Exceptions.swallow(e, LOG, "[onMsgs] An error occurred while handling msgs");
        }
        adjustMsgSizeScaleStepBy(drained);
      }
    } catch (InterruptedException e) {// TODO mj:log stuff
      Thread.currentThread().interrupt();
    }
    LOG.info("[run] The {} seems going through a shutdown", name);
  }

  private void adjustMsgSizeScaleStepBy(int drained) {// Just do a crude guess
    int lastDrained;
    if (drained > (lastDrained = this.msgsLastDrained)) {
      if (drained > (lastDrained * 2)) {
        if ((msgSizeScaleStep += 3) >= MSGS_SIZE_LADDER_LENGTH) {
          msgSizeScaleStep = MSGS_SIZE_LADDER_LENGTH_MINUS_ONE;
        }
      } else {
        if (++msgSizeScaleStep >= MSGS_SIZE_LADDER_LENGTH) {
          msgSizeScaleStep = MSGS_SIZE_LADDER_LENGTH_MINUS_ONE;
        }
      }
    } else if (drained < this.msgsLastDrained) {
      if (--msgSizeScaleStep <= MSGS_SIZE_LADDER_LENGTH) {
        msgSizeScaleStep = 0;
      }
    }
    this.msgsLastDrained = drained;
  }

  public BaseActor(String name, int baseCapacity) {
    this.mailbox = new Mailbox<>(baseCapacity);
    this.worker = NamedThreadFactory.of(this.name = name).newThread(this);
  }

  static {
    MSGS_SIZE_LADDER = Utils.fibs(10, 20, 500);
    MSGS_SIZE_LADDER_LENGTH = MSGS_SIZE_LADDER.length;
    MSGS_SIZE_LADDER_LENGTH_MINUS_ONE = MSGS_SIZE_LADDER_LENGTH - 1;
  }

}
