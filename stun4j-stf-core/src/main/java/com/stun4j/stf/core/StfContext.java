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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.stun4j.guid.core.LocalGuid;
import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.support.banner.Banner;
import com.stun4j.stf.core.support.banner.Banner.Mode;
import com.stun4j.stf.core.support.banner.Stun4jStfBannerPrinter;

/**
 * The core mediator with dozens of convenient static methods,which is responsible for interacting with core components
 * such as stf-registry, stf-core-ops, threadlocal-memo and so on
 * @author Jay Meng
 */
public final class StfContext {
  private static final Logger LOG = LoggerFactory.getLogger(StfContext.class);
  private static final TransmittableThreadLocal<Long> TTL;
  private static final ThreadLocal<Boolean> LAST_COMMITTED;
  private static final Map<?, LocalGuid> CACHED_GUID;// TODO mj:extract 'single value cache' utility class(not strict)

  private static StfCore stf;
  private static StfRegistry bizReg;

  /** must be called in the very beginning */
  public static void init(StfCore stfCore, StfRegistry bizReg) {
    StfContext.stf = requireNonNull(stfCore, "The stf-core can't be null");
    StfContext.bizReg = requireNonNull(bizReg, "The stf-biz-reg can't be null");
  }

  public static void commitLastDone(Long laStfId) {// TODO mj:build-in idempotent mechanism?
    markLastCommitted();
    stf.markDone(laStfId, true);
  }

  public static void commitLastDead(Long laStfId) {
    markLastCommitted();
    stf.markDead(laStfId, true);
  }

  public static void commitLastDone() {
    Long laStfId = laStfId();
    commitLastDone(laStfId);
  }

  public static void commitLastDead() {
    Long laStfId = laStfId();
    commitLastDead(laStfId);
  }

  @SafeVarargs
  public static Long preCommitNextStep(String bizObjId, String bizMethodName, Pair<?, Class<?>>... typedArgs) {
    return stf.init(bizObjId, bizMethodName, typedArgs);
  }

  public static Long laStfId() {
    return TTL.get();
  }

  public static Object getBizObj(String bizObjId) {
    return bizReg.getObj(bizObjId);
  }

  public static Class<?> getBizObjClass(String bizObjId) {
    return bizReg.getObjClass(bizObjId);
  }

  public static void putBizObj(String bizObjId, Object obj) {
    bizReg.putObj(bizObjId, obj);
  }

  public static void putBizObjClass(String bizObjId, Supplier<Class<?>> classProvider) {
    bizReg.putObjClass(bizObjId, classProvider.get());
  }

  static String getBizObjId(Class<?> bizClass) {
    return (String)bizReg.getObj(bizClass);
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
  }

  static void removeTTL() {
    TTL.remove();
  }

  static Long newStfId() {
    Long stfId = CACHED_GUID.computeIfAbsent(null, k -> LocalGuid.instance()).next();
    TTL.set(stfId);
    return stfId;
  }

  static void withStfId(Long stfId) {
    TTL.set(stfId);
  }

  static {
    printBanner();
    CACHED_GUID = new HashMap<>(1);// not very strict in high concurrency scenarios
    TTL = new TransmittableThreadLocal<>();
    LAST_COMMITTED = ThreadLocal.withInitial(() -> false);

    stf = StfCore.empty();
    bizReg = StfRegistry.empty();
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

  private StfContext() {
  }
}