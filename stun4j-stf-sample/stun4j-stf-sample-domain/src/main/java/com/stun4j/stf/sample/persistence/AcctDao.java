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
package com.stun4j.stf.sample.persistence;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author Jay Meng
 */
public interface AcctDao {
  @Update("update stn_stf_sample_acct set amt = amt - abs(#{amtDt}), up_at = #{upAt} where no = #{acctNoFrom} and amt - abs(#{amtDt}) >= 0")
  int decreaseAcctAmt(@Param("amtDt") BigDecimal amtDt, @Param("acctNoFrom") Long acctNoFrom, @Param("upAt") Date upAt);

  @Update("update stn_stf_sample_acct set amt = amt + abs(#{amtDt}), up_at = #{upAt} where no = #{acctNoTo}")
  int increaseAcctAmt(@Param("amtDt") BigDecimal amtDt, @Param("acctNoTo") Long acctNoTo, @Param("upAt") Date upAt);

  default boolean decreaseAcctAmt(BigDecimal amtDt, Long acctNoFrom) {
    return decreaseAcctAmt(amtDt, acctNoFrom, new Date()) == 1;
  }

  default boolean increaseAcctAmt(BigDecimal amtDt, Long acctNoTo) {
    return increaseAcctAmt(amtDt, acctNoTo, new Date()) == 1;
  }
}
