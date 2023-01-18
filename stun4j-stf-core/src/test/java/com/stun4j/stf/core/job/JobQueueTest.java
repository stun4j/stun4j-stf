package com.stun4j.stf.core.job;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stun4j.guid.core.utils.Exceptions;
import com.stun4j.guid.core.utils.Utils;
import com.stun4j.stf.core.Stf;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobQueueTest {
  private static final Logger LOG = LoggerFactory.getLogger(JobQueueTest.class);

  @Test
  public void _01_basic() throws InterruptedException {
    JobQueue queue = new JobQueue(2);
    int res = queue.offer(newJob(1));
    assert res == 1 : "should be offered";
    assert queue.size() == 1 : "size not matched";
    res = queue.offer(newJob(2));// put a different job
    assert res == 1 : "should be offered";
    res = queue.offer(newJob(3));// queue is full
    assert res == -1 : "can't be offered,because it's full";

    Map<Long, Long> idx = indexOf(queue);
    assert idx.toString().equals("{1=1, 2=2}") : "idx image not matched";

    Stf job = queue.poll();
    assert job.getId().equals(1L) : "fifo get #1";
    job = queue.poll();
    assert job.getId().equals(2L) : "fifo get #2";
    assert queue.size() == 0 : "size not matched";

    idx = indexOf(queue);
    assert idx.size() == 0 : "idx size not matched";
  }

  @Test
  public void _02_advanced_termAware() throws InterruptedException {
    JobQueue queue = new JobQueue(10);
    Stf job1Term1 = newJob(1, 1);
    Stf job2Term1 = newJob(2, 1);
    Stf job2Term2 = newJob(2, 2);

    // term auto adjust->
    queue.offer(job1Term1);
    queue.offer(job2Term2);// term:2
    queue.offer(job2Term1);// term:1
    assert queue.size() == 3 : "size not matched";

    Map<Long, Long> idx = indexOf(queue);
    assert idx.toString().equals("{1=1, 2=2}") : "idx image not matched";// index keeps the max term of the same job

    assert queue.poll().getId().equals(1L) : "basic fifo not matched";

    Stf job2 = queue.poll();
    assert job2.getId().equals(2L) : "basic fifo not matched";
    assert job2.getUpAt() == 1 : "term auto-adjust not work";

    job2 = queue.poll();
    assert job2.getId().equals(2L) : "basic fifo not matched";
    assert job2.getUpAt() == 2 : "term auto-adjust not work";
    // <-

    assert queue.size() == 0 : "size not matched";// ensure a fresh round

    // same term reject->
    queue.offer(job1Term1);
    assert queue.size() == 1 : "size not matched";
    Stf anotherJob1Term1 = newJob(1, 1);
    int res = queue.offer(anotherJob1Term1);
    assert res == 0 : "job with same term can't be offered";
    assert queue.size() == 1 : "after reject offering,size should be the same";

    idx = indexOf(queue);
    assert idx.toString().equals("{1=1}") : "idx image not matched";
    // <-

    // do a reset&ensure a fresh round->
    queue.poll();
    assert queue.size() == 0 : "size not matched";
    // <-

    // normal term offering
    queue.offer(job1Term1);// #1 with term:1
    queue.offer(job2Term1);// #2 with term:1
    queue.offer(job2Term2);// #2 with term:2

    assert queue.size() == 3 : "size not matched";
    idx = indexOf(queue);
    assert idx.toString().equals("{1=1, 2=2}") : "idx image not matched";// index keeps the max term of the same job
  }

  @Test
  public void _03_blockingTake() throws InterruptedException {
    JobQueue queue = new JobQueue(2);
    LOG.info("queue size: {}", queue.size());

    Long jobId = 1L;
    int sleepSecs = 3;
    // prepare a delay task for the purpose to break the expected blocking
    new Thread(() -> {
      Utils.sleepSeconds(sleepSecs);
      LOG.info("try offering jobs...");
      Stf job = newJob(1);
      queue.offer(job);// should't be blocked

      Stf job2 = newJob(2);
      queue.offer(job2);
    }).start();

    // check non-blocking #size works during the blocking...
    AtomicInteger cnt = new AtomicInteger();
    new Thread(() -> {
      while (true) {
        Utils.sleepMs(500);
        cnt.incrementAndGet();
        LOG.info("queue size: {}", queue.size());// should't be blocked
      }
    }).start();

    LOG.info("try taking the job but blocked...");
    long blockTakingAt = System.currentTimeMillis();
    Stf job = queue.take();
    long gotAt = System.currentTimeMillis();
    //@formatter:off //-50:Allow for a slight time-cost 
    assert (gotAt - blockTakingAt) >= sleepSecs * 1000 - 20 : "blocking works not correct, gotAt=" + gotAt + ", blockTakingAt="+blockTakingAt;
    //@formatter:on
    assert cnt.get() >= 4 : "non-blocking works not correct";// at least 2 seconds non-blocking window
    LOG.info("got the job {}", job);
    assert job.getId().equals(jobId) : "should get the same job#1";
    assert queue.size() == 1 : "should remain 1 job";
    assert queue.take().getId().equals(2L) : "should get job#2";

    // checking another non-blocking #poll works during the blocking...
    assert queue.size() == 0 : "size not matched";
    // prepare a delay task invoking #poll
    AtomicBoolean assert1 = new AtomicBoolean();
    AtomicBoolean assert2 = new AtomicBoolean();
    new Thread(() -> {
      Utils.sleepSeconds(sleepSecs);
      assert1.set(queue.size() == 0);
      assert2.set(queue.poll() == null);
      queue.offer(newJob(99));
    }).start();
    blockTakingAt = System.currentTimeMillis();
    job = queue.take();// blocking happens again
    gotAt = System.currentTimeMillis();
    //@formatter:off 
    assert (gotAt - blockTakingAt) >= sleepSecs * 1000 - 20 : "blocking works not correct, gotAt=" + gotAt + ", blockTakingAt="+blockTakingAt;
    //@formatter:on
    assert assert1.get() : "size not matched, when #take blocks";
    assert assert2.get() : "poll works correct, when #take blocks";
  }

  @Test
  public void _04_concurrent_basic() throws InterruptedException {
    int cap = 10;
    JobQueue queue = new JobQueue(cap);
    int conLvl = 50;
    Thread[] threads = new Thread[conLvl];
    CyclicBarrier bar = new CyclicBarrier(conLvl);
    AtomicInteger sucCnt = new AtomicInteger();
    AtomicInteger failCnt = new AtomicInteger();
    for (int i = 0; i < threads.length; i++) {
      int idx = i + 1;
      threads[i] = new Thread(() -> {
        try {
          bar.await();
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
          Exceptions.sneakyThrow(e);
        }
        int res = queue.offer(newJob(idx));
        if (res == 1) {
          sucCnt.incrementAndGet();
        } else if (res == -1) {
          failCnt.incrementAndGet();
        }
      });
      threads[i].start();
    }
    for (Thread thread : threads) {
      thread.join();
    }
    assert queue.size() == cap : "size not matched :" + queue.size();
    assert sucCnt.get() == cap : "offer success-cnt not matched :" + sucCnt;
    assert failCnt.get() == (conLvl - sucCnt.get()) : "offer fail-cnt not matched :" + failCnt;
  }

  @SuppressWarnings("unchecked")
  public Map<Long, Long> indexOf(JobQueue queue) {
    try {
      Map<Long, Long> map = (Map<Long, Long>)FieldUtils.getField(JobQueue.class, "jobsUpAts", true).get(queue);
      return map;
    } catch (Exception e) {
      Exceptions.sneakyThrow(e);
    }
    return null;
  }

  private Stf newJob(long id) {
    return newJob(id, id);
  }

  private Stf newJob(long id, long term) {
    Stf job = new Stf();
    job.setId(id);
    job.setUpAt(term);
    return job;
  }
}
