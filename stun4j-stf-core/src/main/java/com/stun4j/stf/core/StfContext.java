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

import static com.stun4j.stf.core.utils.Asserts.requireNonNull;
import static com.stun4j.stf.core.utils.Asserts.state;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.support.banner.Banner;
import com.stun4j.stf.core.support.banner.Banner.Mode;
import com.stun4j.stf.core.support.banner.Stun4jStfBannerPrinter;

/**
 * The core mediator with dozens of convenient static methods,which is responsible for interacting with core components
 * such as stf-registry, stf-core-ops, threadlocal-memo and so on.
 * @author Jay Meng
 */
public final class StfContext {
  private static final Logger LOG = LoggerFactory.getLogger(StfContext.class);
  private static final TransmittableThreadLocal<StfId> TTL;
  private static final ThreadLocal<Boolean> LAST_COMMITTED;

  private static StfCore _core;
  private static StfRegistry _bizReg;

  public static void commitLastDoneWithoutTx() {// Do not use this method in any active transaction!!!
    state(!isActualTransactionActive(), "Can't be called in an active transaction");
    Long laStfId = safeGetLaStfIdValue();
    try {
      _core.markDone(StfMetaGroupEnum.CORE, laStfId, true);
    } finally {
      removeTTL();
    }
  }

  public static Long safeGetLaStfIdValue() {
    return Optional.ofNullable(laStfId()).orElse(StfId.empty()).getValue();
  }

  public static StfId laStfId() {
    return TTL.get();
  }

  static void commitLastDone() {
    Long laStfId = safeGetLaStfIdValue();
    commitLastDone(laStfId);
  }

  static void commitLastDead() {
    Long laStfId = safeGetLaStfIdValue();
    commitLastDead(laStfId);
  }

  static void commitLastDone(Long laStfId) {
    commitLastDone(laStfId, true);
  }

  static void commitLastDone(Long laStfId, boolean async) {// TODO mj:Count as build-in idempotent mechanism?
    markLastCommitted();
    _core.markDone(StfMetaGroupEnum.CORE, laStfId, async);
  }

  static void commitLastDead(Long laStfId) {
    markLastCommitted();
    _core.markDead(StfMetaGroupEnum.CORE, laStfId, true);
  }

  @SafeVarargs
  static Long preCommitNextStep(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    return _core.newStf(bizObjId, bizMethodName, typedArgs);
  }

  static Object getBizObj(String bizObjId) {
    return _bizReg.getObj(bizObjId);
  }

  static String getBizObjId(Class<?> bizClass) {
    return (String)_bizReg.getObj(bizClass);
  }

  static void markLastCommitted() {
    LAST_COMMITTED.set(true);
  }

  static boolean isLastCommitted() {
    return LAST_COMMITTED.get();
  }

  static void removeLastCommitInfo() {
    // DO NOT REMOVE HERE!!!TTL.remove()
    LAST_COMMITTED.remove();
    TransactionResourceManager.TRANSACTION_RESOURCES.remove();
  }

  static void removeTTL() {
    TTL.remove();
  }

  static StfId newStfId(String oid, String methodName) {
    Long stfId = LocalGuid.instance().next();
    String identity = StfConfigs.actionPathBy(oid, methodName);
    StfId id;
    TTL.set(id = new StfId(stfId, identity));
    return id;
  }

  static void withStfId(StfId stfId) {
    TTL.set(stfId);
  }

  private static Banner printBanner() {
    if (BANNER_MODE == Banner.Mode.OFF) {
      return null;
    }
    Stun4jStfBannerPrinter bannerPrinter = Stun4jStfBannerPrinter.INSTANCE;
    if (BANNER_MODE == Mode.LOG) {
      return bannerPrinter.print(LOG);
    }
    return bannerPrinter.print(System.out);
  }

  static Banner.Mode BANNER_MODE = Banner.Mode.CONSOLE;

  static {
    printBanner();
    TTL = new TransmittableThreadLocal<>();
    LAST_COMMITTED = ThreadLocal.withInitial(() -> false);

    _core = StfCore.empty();
    _bizReg = StfRegistry.empty();
  }

  private StfContext() {
  }

  static class TransactionResourceManager {
    private static final ThreadLocal<Map<String, Object>> TRANSACTION_RESOURCES;

    static void unbindAll() {
      TRANSACTION_RESOURCES.remove();
    }

    static Object getResource(String key) {
      return TRANSACTION_RESOURCES.get().get(key);
    }

    static Map<String, Object> getResourceMap() {
      return TRANSACTION_RESOURCES.get();
    }

    static void bindResource(String key, Object value) {
      TRANSACTION_RESOURCES.get().put(key, value);
    }

    static {
      TRANSACTION_RESOURCES = ThreadLocal.withInitial(HashMap::new);
    }
  }

  /** Must be called in the very beginning */
  public static void init(StfCore stfCore, StfRegistry bizReg) {
    StfContext._core = requireNonNull(stfCore, "The stf-core can't be null");
    StfContext._bizReg = requireNonNull(bizReg, "The stf-biz-reg can't be null");
  }

  public static StfDelayQueueCore delayQueueCore() {
    return (StfDelayQueueCore)StfContext._core;
  }

  public static Class<?> getBizObjClass(String bizObjId) {
    return _bizReg.getObjClass(bizObjId);
  }

  public static void putBizObj(String bizObjId, Object obj) {
    _bizReg.putObj(bizObjId, obj);
  }

  public static void putBizObjClass(String bizObjId, Supplier<Class<?>> classProvider) {
    _bizReg.putObjClass(bizObjId, classProvider.get());
  }

  // static boolean isActualStfTransactionActive() {
  // return TransactionSynchronizationManager.isActualTransactionActive()
  // && TransactionResourceManager.getResourceMap().size() > 0;
  // }
}