/*
 * Copyright 2012-2019 the original author or authors.
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
package com.stun4j.stf.core.support.banner;

import java.io.PrintStream;

/**
 * Interface class for writing a banner programmatically.
 * 
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Jeremy Rickard
 * @author Jay Meng
 *         <p>
 *         From spring-boot:2.6.5
 * @since 1.2.0
 */
@FunctionalInterface
public interface Banner {

  /**
   * Print the banner to the specified print stream.
   * 
   * @param out the output print stream
   */
  void printBanner(PrintStream out);

  /**
   * An enumeration of possible values for configuring the Banner.
   */
  enum Mode {

    /**
     * Disable printing of the banner.
     */
    OFF,

    /**
     * Print the banner to System.out.
     */
    CONSOLE,

    /**
     * Print the banner to the log file.
     */
    LOG

  }
}