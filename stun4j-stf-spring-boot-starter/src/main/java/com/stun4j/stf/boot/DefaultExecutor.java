package com.stun4j.stf.boot;

import static com.stun4j.stf.boot.DefaultExecutor.RejectPolicy.BACK_PRESSURE;

public class DefaultExecutor {

  /**
   * This is the maximum time that excess idle threads in the thread pool will wait for new tasks before
   * terminating.<br>
   * 0: threads(might include core threads) always stay alive, <0: illegal
   * <p>
   * Default: 60
   */
  private int threadKeepAliveTimeSeconds = 60;
  /**
   * The queue to use for holding tasks before they are executed.
   * This queue will hold only the {@code Runnable}
   * <p>
   * Default: 1024
   */
  private int taskQueueSize = 1024;

  /**
   * If false, core threads stay alive even when idle. If true, core threads use keepAliveTime to time out
   * waiting for work.
   * <p>
   * Default: false
   */
  private boolean allowCoreThreadTimeOut = false;
  /**
   * Default: back-pressure
   * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
   * @see java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
   * @see com.stun4j.stf.core.utils.executor.PoolExecutors.DiscardPolicy
   * @see com.stun4j.stf.core.utils.executor.PoolExecutors.DiscardOldestPolicy
   */
  private RejectPolicy threadRejectPolicy = BACK_PRESSURE;

  public enum RejectPolicy {
    BACK_PRESSURE, DROP_WITH_EX_THROW, SILENT_DROP, SILENT_DROP_OLDEST
  }

  public int getThreadKeepAliveTimeSeconds() {
    return threadKeepAliveTimeSeconds;
  }

  public void setThreadKeepAliveTimeSeconds(int threadKeepAliveTimeSeconds) {
    this.threadKeepAliveTimeSeconds = threadKeepAliveTimeSeconds;
  }

  public boolean isAllowCoreThreadTimeOut() {
    return allowCoreThreadTimeOut;
  }

  public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
    this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
  }

  public RejectPolicy getThreadRejectPolicy() {
    return threadRejectPolicy;
  }

  public void setThreadRejectPolicy(RejectPolicy threadRejectPolicy) {
    this.threadRejectPolicy = threadRejectPolicy;
  }

  public int getTaskQueueSize() {
    return taskQueueSize;
  }

  public void setTaskQueueSize(int taskQueueSize) {
    this.taskQueueSize = taskQueueSize;
  }

}
