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
package com.stun4j.stf.core.job;

import static com.stun4j.guid.core.utils.Asserts.argument;
import static com.stun4j.guid.core.utils.Asserts.state;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.stun4j.stf.core.Stf;

/**
 * @author Jay Meng
 */
class JobQueue {
  private final int capacity;
  private final Map<Long/* jobId */, Long/* jobUpAt */> jobsUpAts;// Be treated as indexes & terms
  private final LinkedList<Stf> list;
  private final ReadWriteLock rwl;
  private final Condition ifEmpty;

  public int offer(Stf job) {
    argument(job != null && job.getId() != null, "The stf-job(and its id) can't be null");
    long curJobUpAt;
    state((curJobUpAt = job.getUpAt()) > 0, "The stf-job's upAt must be greater than 0");
    Lock lock;
    (lock = rwl.writeLock()).lock();
    try {
      if (list.size() >= capacity) {
        return -1;
      }
      Long jobId;
      if (jobsUpAts.containsKey(jobId = job.getId())) {
        Long existJobUpAt = jobsUpAts.get(jobId);
        // May be the same 'term'
        if (existJobUpAt.equals(curJobUpAt)) {
          return 0;
        }
        // Try reorder term(shouldn‘t happen)
        if (existJobUpAt.compareTo(curJobUpAt) > 0) {
          // If the job already exists and has more recent 'term', we simply requeue it (to tail)
          // TODO mj:Better implementation
          // TODO mj:Priority implementation
          Stf existJob = remove(job);
          list.add(job);
          if (existJob != null) {// Shouldn't happen
            list.add(existJob);
          }
          jobsUpAts.put(jobId, existJobUpAt);
          ifEmpty.signalAll();
          return 1;
        }
      }
      // Normal case: job not exists or curJobUpAt > existJobUpAt
      list.add(job);
      jobsUpAts.put(jobId, curJobUpAt);
      ifEmpty.signalAll();
      return 1;
    } finally {
      lock.unlock();
    }
  }

  public Stf poll() {
    Lock lock;
    (lock = rwl.writeLock()).lock();
    try {
      return doPollFirst();
    } finally {
      lock.unlock();
    }
  }

  public Stf take() throws InterruptedException {
    Lock lock;
    (lock = rwl.writeLock()).lock();
    try {
      Stf job;
      while ((job = doPollFirst()) == null) {
        ifEmpty.await();
      }
      return job;
    } finally {
      lock.unlock();
    }
  }

  public int size() {
    Lock lock;
    (lock = rwl.readLock()).lock();
    try {
      return list.size();
    } finally {
      lock.unlock();
    }
  }

  private Stf doPollFirst() {
    Stf job = list.pollFirst();
    if (job == null)
      return null;
    jobsUpAts.remove(job.getId());
    return job;
  }

  private Stf remove(Stf theJob) {
    for (ListIterator<Stf> iter = list.listIterator(); iter.hasNext();) {
      Stf job = iter.next();
      if (job.equals(theJob)) {
        iter.remove();
        jobsUpAts.remove(job.getId());
        return job;
      }
    }
    return null;
  }

  public JobQueue(int capacity) {
    argument(capacity > 0, "The stf-job-queue capacity must be greater than 0");
    this.capacity = capacity;
    this.list = new LinkedList<>();
    this.jobsUpAts = new HashMap<>();
    ifEmpty = (this.rwl = new ReentrantReadWriteLock()).writeLock().newCondition();
  }

}