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
package com.stun4j.stf.core.utils.consumers;

@FunctionalInterface
/** @author Jay Meng */
public interface TriConsumer<T, U, V> extends BaseConsumer<T> {

  /**
   * Performs this operation on the given arguments.
   * @param t the first input argument
   * @param u the second input argument
   */
  void accept(T t, U u, V v);

  // /**
  // * Returns a composed {@code BiConsumer} that performs, in sequence, this
  // * operation followed by the {@code after} operation. If performing either
  // * operation throws an exception, it is relayed to the caller of the
  // * composed operation. If performing this operation throws an exception,
  // * the {@code after} operation will not be performed.
  // *
  // * @param after the operation to perform after this operation
  // * @return a composed {@code BiConsumer} that performs in sequence this
  // * operation followed by the {@code after} operation
  // * @throws NullPointerException if {@code after} is null
  // */
  // default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
  // Objects.requireNonNull(after);
  //
  // return (l, r) -> {
  // accept(l, r);
  // after.accept(l, r);
  // };
  // }
}