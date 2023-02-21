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

import java.util.Date;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stun4j.stf.sample.domain.StatusEnum;

/**
 * @author Jay Meng
 */
public interface TxDao extends BaseMapper<TxPo> {
  @Update("update stn_stf_sample_tx set st = #{st}, up_at = #{upAt} where id= #{txId} and st not in (#{excludeSt0}, #{excludeSt1})")
  int markTxEnd(@Param("txId") Long txId, @Param("st") StatusEnum st, @Param("upAt") Date upAt,
      @Param("excludeSt0") StatusEnum excludeSt0, @Param("excludeSt1") StatusEnum excludeSt1);

  default boolean markTxFail(Long txId) {
    return markTxEnd(txId, StatusEnum.F, new Date(), StatusEnum.S, StatusEnum.F) == 1;
  }

  default boolean markTxSuccess(Long txId) {
    return markTxEnd(txId, StatusEnum.S, new Date(), StatusEnum.S, StatusEnum.F) == 1;
  }
}
