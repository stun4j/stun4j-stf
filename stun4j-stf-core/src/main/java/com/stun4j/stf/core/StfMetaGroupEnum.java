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
package com.stun4j.stf.core;

import java.util.stream.Stream;

/**
 * @author Jay Meng
 */
public enum StfMetaGroupEnum {
  CORE, DELAY;

  public static Stream<StfMetaGroupEnum> stream() {
    return Stream.of(StfMetaGroupEnum.values());
  }

  public static String[] namesLowerCase() {
    return stream().map(e -> e.name().toLowerCase()).toArray(String[]::new);
  }

  public String nameLowerCase() {
    return this.name().toLowerCase();
  }
}
