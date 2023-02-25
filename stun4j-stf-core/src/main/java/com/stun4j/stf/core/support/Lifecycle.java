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
package com.stun4j.stf.core.support;

/**
 * The life cycle abstraction
 * <p>
 * If you are not simply creating an object, but starting some 'heavy' resource or waiting or
 * polling service, you should consider implementing {@link #start()}. <br>
 * If you provide a start implementation, you should provide {@link #shutdown()} equivalently (and
 * implement stop as elegantly as possible), but not vice versa.
 * 
 * @author Jay Meng
 */
public interface Lifecycle {
  void start();

  void shutdown();
}