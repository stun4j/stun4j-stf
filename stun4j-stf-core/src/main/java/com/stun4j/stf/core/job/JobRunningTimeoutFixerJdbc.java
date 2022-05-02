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
package com.stun4j.stf.core.job;

import static com.google.common.base.Strings.lenientFormat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.stun4j.stf.core.StateEnum;
import com.stun4j.stf.core.Stf;
import com.stun4j.stf.core.StfConsts;
import com.stun4j.stf.core.YesNoEnum;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.support.JdbcAware;

/**
 * The jdbc implementation of the base class
 * @author Jay Meng
 */
public class JobRunningTimeoutFixerJdbc extends BaseJobRunningTimeoutFixer implements JdbcAware {
  private final StfJdbcOps jdbcOps;
  final String RESET_SQL;

  @Override
  protected void doCheckAndFix(Stream<Stf> jobsMayTimeout) {
    List<Object[]> args = jobsMayTimeout
        .map((jobMayTimeout) -> new Object[]{ /* jobMayTimeout.getUpAt(), */ jobMayTimeout.getId()})
        .collect(Collectors.toList());
    jdbcOps.batchUpdate(RESET_SQL, args);
  }

  public static JobRunningTimeoutFixerJdbc of(StfJdbcOps jdbc, JobScanner scanner, JobRunners runners) {
    return new JobRunningTimeoutFixerJdbc(jdbc, scanner, runners, StfConsts.DFT_TBL_NAME);
  }

  JobRunningTimeoutFixerJdbc(StfJdbcOps jdbcOps, JobScanner scanner, JobRunners runners, String tblName) {
    super(scanner, runners);
    this.jdbcOps = jdbcOps;
    RESET_SQL = lenientFormat(
        "update %s set is_locked = '%s' where id = ? and is_locked = '%s' and st in ('%s', '%s')", tblName,
        YesNoEnum.N.name(), YesNoEnum.Y.name(), StateEnum.I.name(), StateEnum.P.name());
  }

  @Override
  public StfJdbcOps getJdbcOps() {
    return jdbcOps;
  }
}