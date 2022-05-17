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
package com.stun4j.stf.core.support.actor;

import com.stun4j.stf.core.utils.executor.NamedThreadFactory;

/**
 * Why does actor framework(e.g. akka,quasar etc.) do so much:)
 * <p>
 * There is no doubt that JDK will eventually solve the core problem that actors are trying to solve.
 * @author Jay Meng
 */
public class ActorSystem {
  public static Thread newActor(String actorName, Runnable runnable) {
    return NamedThreadFactory.of(actorName).newThread(runnable);
  }
}