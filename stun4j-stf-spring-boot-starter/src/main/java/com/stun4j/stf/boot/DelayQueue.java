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

import java.util.Optional;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Stf DelayQueue configuration
 * <p>
 * @author Jay Meng
 */
public class DelayQueue {
  /**
   * Whether Stf DelayQueue is enabled
   * <p>
   * Default: true
   */
  private boolean enabled = true;

  @NestedConfigurationProperty
  private final Body body = new Body();

  @NestedConfigurationProperty
  private Datasource datasource = new Datasource(this);

  private final Core parentBind;

  public String getDatasourceBeanName() {
    String dlqDsBeanName = Optional.ofNullable(getDatasource().getBeanName())
        .orElse(parentBind.getDatasourceBeanName());
    return dlqDsBeanName;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Body getBody() {
    return body;
  }

  public Datasource getDatasource() {
    return datasource;
  }

  public DelayQueue(Core parentBind) {
    this.parentBind = parentBind;
  }

}
