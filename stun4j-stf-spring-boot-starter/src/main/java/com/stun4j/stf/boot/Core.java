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
 * Stf Core global configuration
 * <p>
 * @author Jay Meng
 */
public class Core {
  @NestedConfigurationProperty
  private final Body body = new Body();

  @NestedConfigurationProperty
  private Datasource datasource = new Datasource(this);

  private final StfProperties parentBind;

  public Body getBody() {
    return body;
  }

  public String getDatasourceBeanName() {
    String coreDsBeanName = Optional.ofNullable(getDatasource().getBeanName())
        .orElse(parentBind.getDatasourceBeanName());/*-TODO mj:Misuse can lead to escape*/
    return coreDsBeanName;
  }

  public Datasource getDatasource() {
    return datasource;
  }

  Core(StfProperties parentBind) {
    this.parentBind = parentBind;
  }

}
