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
package com.stun4j.stf.sample.boot.persistence;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author Jay Meng
 */
@TableName("req")
public class ReqPo {
  private final String id;
  private final String body;
  private final Date ctAt;

  public ReqPo(String id, String body) {
    this(id, body, new Date());
  }

  public ReqPo(String id, String body, Date ctAt) {
    this.id = id;
    this.body = body;
    this.ctAt = ctAt;
  }

  public String getId() {
    return id;
  }

  public String getBody() {
    return body;
  }

  public Date getCtAt() {
    return ctAt;
  }

  public ReqPo(String body) {
    this(null, body, null);
  }

}
