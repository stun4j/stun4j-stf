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
package com.stun4j.stf.core;

import java.util.function.Consumer;

import org.apache.commons.lang3.time.FastDateFormat;

/** @author Jay Meng */
public interface StfConsts {
  String DFT_CONF_SUFFIX = ".conf";
  String DFT_CORE_TBL_NAME = "stn_stf";
  String DFT_DELAY_TBL_NAME_SUFFIX = "_delay";
  String DFT_CLUSTER_MEMBER_TBL_NAME = "stn_stf_cluster_member";
  FastDateFormat DFT_DATE_FMT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSSZ");
  int DFT_MIN_JOB_TIMEOUT_SECONDS = 5;
  int DFT_JOB_TIMEOUT_SECONDS = 20;

  @SuppressWarnings("unused")
  static final Consumer<String> NOT_INITIALIZED_THROW = moduleId -> {
    if (true)
      throw new UnsupportedOperationException("Has the " + moduleId + " been initialized yet,in the very begining?");
    return;
  };
}
