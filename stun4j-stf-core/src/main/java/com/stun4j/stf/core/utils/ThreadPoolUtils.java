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
package com.stun4j.stf.core.utils;

/**
 * @author Jay Meng
 */
public abstract class ThreadPoolUtils {
  public static int cpuIntensivePoolSize() {
    return poolSize(0.1);
  }

  public static int ioIntensivePoolSize() {
    return poolSize(0.9);
  }

  public static int poolSize(double blockingFactor) {
    int numOfCore = Runtime.getRuntime().availableProcessors();
    int poolSize = (int)(numOfCore / (1 - blockingFactor));
    return poolSize;
  }
}