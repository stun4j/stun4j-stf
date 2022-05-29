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
package com.stun4j.stf.boot;

import static com.stun4j.guid.core.utils.Asserts.argument;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import com.stun4j.stf.core.StfRunModeEnum;
import com.stun4j.stf.core.support.BaseVo;

/**
 * Base class for configuration of Stf.
 * @author Jay Meng
 */
@ConfigurationProperties("stun4j.stf")
public class StfProperties extends BaseVo {

  /**
   * The run mode of Stf
   * <p>
   * Default: default
   */
  private StfRunModeEnum runMode = StfRunModeEnum.DEFAULT;

  /**
   * The spring bean name of underlying sql datasource
   * <p>
   * Default: dataSource
   */
  private String datasourceBeanName = "dataSource";

  /**
   * The root path of stf-flow configurations, all of which are scanned and loaded by default
   * <p>
   * Default: classpath:stfs
   */
  private String confRootPath = "classpath:stfs";

  /**
   * The exclude file names of stf-flow configurations
   */
  private String[] confExcludeFilenames;

  /**
   * The full loading order of stf-flow configurations
   * <ul>
   * <li>
   * Note that if filename is not specified in this full-order-list, the file will also be excluded from loading,which
   * may cause unexpected loading error.
   * </li>
   * <li>The configuration loaded first is parent, which can be overridden by the configuration loaded later.</li>
   * <li>
   * Default(comparison behavior): Use comparator that orders String objects(will pick configuration's filename from
   * File or URL)
   * as by compareToIgnoreCase. Note that the Comparator does not
   * take locale into account, and will result in an unsatisfactory ordering for
   * certain locales. The java.text package provides Collators to allow locale-sensitive ordering.
   * </li>
   * </ul>
   * @see java.lang.String#CASE_INSENSITIVE_ORDER
   */
  private String[] confFullLoadOrder;

  @NestedConfigurationProperty
  private Transaction transaction = new Transaction();

  @NestedConfigurationProperty
  private Job job = new Job();

  @NestedConfigurationProperty
  private Monitor monitor = new Monitor();

  @NestedConfigurationProperty
  private DefaultExecutor defaultExecutor = new DefaultExecutor();

  @NestedConfigurationProperty
  private DelayQueue delayQueue = new DelayQueue();

  public void setConfRootPath(String confRootPath) {
    argument(confRootPath.indexOf("*") == -1, "'*' is not supported in root path of stf-flow configurations");
    if (confRootPath.endsWith("/")) {
      confRootPath = confRootPath.substring(0, confRootPath.length() - 1);
    }
    this.confRootPath = confRootPath;
  }

  public String getDatasourceBeanName() {
    return datasourceBeanName;
  }

  public void setDatasourceBeanName(String datasourceBeanName) {
    this.datasourceBeanName = datasourceBeanName;
  }

  public StfRunModeEnum getRunMode() {
    return runMode;
  }

  public void setRunMode(StfRunModeEnum runMode) {
    this.runMode = runMode;
  }

  public String getConfRootPath() {
    return confRootPath;
  }

  public String[] getConfExcludeFilenames() {
    return confExcludeFilenames;
  }

  public void setConfExcludeFilenames(String[] confExcludeFilenames) {
    this.confExcludeFilenames = confExcludeFilenames;
  }

  public String[] getConfFullLoadOrder() {
    return confFullLoadOrder;
  }

  public void setConfFullLoadOrder(String[] confFullLoadOrder) {
    this.confFullLoadOrder = confFullLoadOrder;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public Job getJob() {
    return job;
  }

  public Monitor getMonitor() {
    return monitor;
  }

  public DefaultExecutor getDefaultExecutor() {
    return defaultExecutor;
  }

  public DelayQueue getDelayQueue() {
    return delayQueue;
  }

}
