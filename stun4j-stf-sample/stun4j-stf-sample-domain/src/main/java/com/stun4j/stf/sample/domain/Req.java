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
package com.stun4j.stf.sample.domain;

/**
 * @author Jay Meng
 */
public class Req extends Jsonable {
  private final String id;
  private final String acctNoFrom;
  private final String acctNoTo;
  private final String amt;

  public Req(String id, String acctNoFrom, String acctNoTo, String amt) {
    this.id = id;
    this.acctNoFrom = acctNoFrom;
    this.acctNoTo = acctNoTo;
    this.amt = amt;
  }

  public String getId() {
    return id;
  }

  public String getAcctNoFrom() {
    return acctNoFrom;
  }

  public String getAcctNoTo() {
    return acctNoTo;
  }

  public String getAmt() {
    return amt;
  }

  Req() {
    this(null, null, null, null);
  }

  @Override
  public String toString() {
    return "Req [id=" + id + ", acctNoFrom=" + acctNoFrom + ", acctNoTo=" + acctNoTo + ", amt=" + amt + "]";
  }

}