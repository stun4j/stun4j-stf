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

import java.util.stream.Stream;

/**
 * @author Jay Meng
 */
public enum CompressAlgorithm {
  NONE((byte)-1), ZSTD((byte)0), SNAPPY((byte)1);

  private final byte algorithm;

  private CompressAlgorithm(byte algorithm) {
    this.algorithm = algorithm;
  }

  public byte value() {
    return algorithm;
  }

  public static Stream<CompressAlgorithm> stream() {
    return Stream.of(CompressAlgorithm.values());
  }

  public static CompressAlgorithm valueOf(byte algorithm) {
    return stream().filter(a -> a.value() == algorithm).findFirst().orElse(NONE);
  }

}
