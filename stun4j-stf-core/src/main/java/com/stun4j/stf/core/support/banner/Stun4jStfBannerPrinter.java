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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;

/**
 * Class used to print the application banner.
 * @author Phillip Webb
 * @author Jay Meng
 *         <p>
 *         Inspired from spring-boot,simplified
 */
public class Stun4jStfBannerPrinter {

  public static final Stun4jStfBannerPrinter INSTANCE = new Stun4jStfBannerPrinter();
  private static final Banner DEFAULT_BANNER = new Stun4jStfBanner();

  public Banner print(Logger logger) {
    Banner banner = getBanner();
    try {
      logger.info(createStringFromBanner(banner));
    } catch (UnsupportedEncodingException ex) {
      logger.warn("Failed to create String for banner", ex);
    }
    return new PrintedBanner(banner);
  }

  public Banner print(PrintStream out) {
    Banner banner = getBanner();
    banner.printBanner(out);
    return new PrintedBanner(banner);
  }

  private String createStringFromBanner(Banner banner) throws UnsupportedEncodingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    banner.printBanner(new PrintStream(baos));
    return baos.toString("UTF-8");
  }

  private Banner getBanner() {
    return DEFAULT_BANNER;
  }

  /**
   * Decorator that allows a {@link Banner} to be printed again without needing to specify the source class.
   */
  private static class PrintedBanner implements Banner {

    private final Banner banner;

    PrintedBanner(Banner banner) {
      this.banner = banner;
    }

    @Override
    public void printBanner(PrintStream out) {
      this.banner.printBanner(out);
    }

  }

  private Stun4jStfBannerPrinter() {
  }

}