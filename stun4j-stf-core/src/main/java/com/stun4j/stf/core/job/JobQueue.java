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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.stun4j.stf.core.Stf;

/**
 * @author Jay Meng
 */
public class JobQueue {
  private final int capacity;
  private final Map<Long/* jobId */, Long/* jobLastUpAt */> jobsLastUpAts;
  private final LinkedList<Stf> list;
  private final ReadWriteLock rwl;

  public boolean offer(Stf job) {
    argument(job != null, "The stf-job can't be null");
    if (list.size() >= capacity) {
      return false;
    }
    Lock lock;
    (lock = rwl.writeLock()).lock();
    if (list.size() >= capacity) {
      return false;
    }
    try {
      Long jobId = job.getId();
      Long jobLastUpAt = job.getUpAt();
      // if the job already exists and has been changed, log the last update time of the job and requeue it (to tail)
      // (simply reducing its priority) TODO mj:better implementation
      if (jobsLastUpAts.containsKey(job.getId())) {
        Long lastUpAt = jobsLastUpAts.get(jobId);
        if (lastUpAt != null && !lastUpAt.equals(jobLastUpAt)) {
          remove(job);
          list.add(job);
        }
      } else {
        list.add(job);
      }
      jobsLastUpAts.put(jobId, jobLastUpAt);
      return true;
    } finally {
      lock.unlock();
    }
  }

  public Stf poll() {
    Lock lock;
    (lock = rwl.writeLock()).lock();
    try {
      Stf job = list.pollFirst();
      if (job == null) return null;
      jobsLastUpAts.remove(job.getId());
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

  private void remove(Stf theJob) {
    for (ListIterator<Stf> iter = list.listIterator(); iter.hasNext();) {
      Stf job = iter.next();
      if (job.equals(theJob)) {
        iter.remove();
        jobsLastUpAts.remove(job.getId());
        return;
      }
    }
  }

  public JobQueue(int capacity) {
    argument(capacity > 0, "The stf-job-queue capacity must be greater than 0");
    this.capacity = capacity;
    this.list = new LinkedList<>();
    this.jobsLastUpAts = new HashMap<>();
    this.rwl = new ReentrantReadWriteLock();
  }

}