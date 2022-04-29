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
package com.stun4j.stf.sample.boot.utils.mock_data;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Jay Meng
 */
public class Data {
  static long ACCT_NO_PREFIX = 1000_0000_0000_0000L;
  static int ACCT_NO_RANGE = 10_0000;

  public static String generateAccountNo() {
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    return ACCT_NO_PREFIX + rand.nextLong(ACCT_NO_RANGE) + "";
  }

  public static String generateAmount() {
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    return rand.nextInt(10) < 7 ? rand.nextInt(1, 5_0000) + "" : rand.nextInt(1, 5_0000) + "." + rand.nextInt(100);
  }

  public static String[] generateAcctNos() {
    String acctNoFrom = Data.generateAccountNo();
    String acctNoTo = Data.generateAccountNo();
    if (!acctNoFrom.equals(acctNoTo)) {
      return new String[]{acctNoFrom, acctNoTo};
    }
    return generateAcctNos();
  }
}