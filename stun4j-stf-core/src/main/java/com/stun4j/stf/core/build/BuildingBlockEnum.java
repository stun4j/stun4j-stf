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
package com.stun4j.stf.core.build;

/**
 * Syntax elements of the stf-flow configuration file
 * @author Jay Meng
 */
public enum BuildingBlockEnum {
  ROOT("stfs"), //
  GLB("global"), ACTS("actions"), FWDS("forwards"), // lvl-2
  OID("oid"), // lvl-3 or 4
  ARGS("args"), TO("to"), // lvl-4
  O_IN("invoke-on-in"), U_IN("use-in"), // lvl-5,in means last-out
  M("method"), CLZ("clz");

  private final String key;

  private BuildingBlockEnum(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

}