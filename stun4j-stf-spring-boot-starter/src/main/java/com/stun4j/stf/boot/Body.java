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

import com.stun4j.stf.core.support.CompressAlgorithm;

/**
 * Stf body(aka. callee) configuration
 * <p>
 * @author Jay Meng
 */
public class Body {
  /**
   * Whether Stf-body's binary(aka. bytes) format is enabled
   * <p>
   * Default: false
   */
  private boolean bytesEnabled = false;
  /**
   * Whether Stf-body compression is enabled, and if so, which algorithm will be used
   * <p>
   * Default: none
   */
  private CompressAlgorithm compressAlgorithm = CompressAlgorithm.NONE;

  public boolean isBytesEnabled() {
    return bytesEnabled;
  }

  public void setBytesEnabled(boolean bytesEnabled) {
    this.bytesEnabled = bytesEnabled;
  }

  public CompressAlgorithm getCompressAlgorithm() {
    return compressAlgorithm;
  }

  public void setCompressAlgorithm(CompressAlgorithm compressAlgorithm) {
    this.compressAlgorithm = compressAlgorithm;

    if (compressAlgorithm != CompressAlgorithm.NONE) {
      setBytesEnabled(true);// force bytes-format to be enabled when using any compress algorithm
    }
  }

}
