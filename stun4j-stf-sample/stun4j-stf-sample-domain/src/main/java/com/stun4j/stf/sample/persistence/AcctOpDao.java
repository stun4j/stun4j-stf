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

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stun4j.stf.sample.domain.DecrIncrEnum;

/**
 * @author Jay Meng
 */
public interface AcctOpDao extends BaseMapper<Void> {
  @Insert("insert into stn_stf_sample_acct_op (id, acct_no, amt_dt, decr_incr, tx_id, ct_at) values(#{acctOpSeqNo}, #{acctNoFrom}, abs(#{amtDt}), #{op}, #{txId}, #{ctAt})")
  void insertAcctOp(@Param("acctOpSeqNo") Long acctOpSeqNo, @Param("acctNoFrom") Long acctNoFrom,
      @Param("amtDt") BigDecimal amtDt, @Param("op") DecrIncrEnum op, @Param("txId") Long txId,
      @Param("ctAt") Date ctAt);

  default void insertAcctOpOfDecrement(Long acctOpSeqNo, Long acctNoFrom, BigDecimal amtDt, Long txId) {
    insertAcctOp(acctOpSeqNo, acctNoFrom, amtDt, DecrIncrEnum.D, txId, new Date());
  }

  default void insertAcctOpOfIncrement(Long acctOpSeqNo, Long acctNoFrom, BigDecimal amtDt, Long txId) {
    insertAcctOp(acctOpSeqNo, acctNoFrom, amtDt, DecrIncrEnum.I, txId, new Date());
  }
}
