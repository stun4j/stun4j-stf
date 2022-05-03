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

import static com.stun4j.guid.core.utils.Strings.lenientFormat;
import static com.stun4j.stf.core.utils.Asserts.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stun4j.stf.core.StfContext.TransactionResourceManager;
import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * Stf's core transaction enhancement, which delegates to Spring's transaction operation
 * <ul>
 * <li>The 'out' parameter is an object that will be the outgoing parameter of the current method and the incoming
 * parameter of the downstream method.This is the typical, recommended way to make Stf work well</li>
 * <li>Be care of the case where you passing 'out' as {@code null} and it is generally accepted that the downstream
 * method does not need an input. In this case, do not pass {@code null} and instead use '#execute' or
 * '#executeWithoutResult' methods that have no 'out' parameter</li>
 * </ul>
 * @author Jay Meng
 */
@SuppressWarnings("deprecation")
public class StfTxnCallback<T> implements InvocationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(StfTxnCallback.class);

  private final TransactionCallback<T> rawTxnCb;
  private final Class<?> callerClass;
  private final String callerMethodName;
  private final T bizOut;

  @SuppressWarnings("unchecked")
  public static <T> TransactionCallback<T> of(TransactionCallback<T> txCb, String callerClassName,
      String callerMethodName, T bizOut) {
    ClassLoader bizClassLoader = requireNonNull((bizOut != null
        ? Optional.ofNullable(bizOut.getClass().getClassLoader())/* some class may not have classloader(e.g. String) */
            .orElse(Thread.currentThread().getContextClassLoader())
        : Thread.currentThread().getContextClassLoader()), "The biz-classloader can't be null");
    TransactionCallback<T> enhanced;
    try {
      enhanced = (TransactionCallback<T>)Proxy.newProxyInstance(bizClassLoader, new Class[]{TransactionCallback.class},
          new StfTxnCallback<>(txCb, callerClassName, callerMethodName, bizClassLoader, bizOut));
      return enhanced;
    } catch (Exception e) {
      Exceptions.sneakyThrow(e);
    }
    return null;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
        private StfId laStfId;

        @Override
        public void afterCompletion(int status) {
          StfContext.removeLastCommitInfo();
          TransactionResourceManager.unbindAll();
          TransactionSynchronizationManager.clear();
        }

        @Override
        public void beforeCompletion() {
          // support normal 'single' transaction
          boolean isLaStfCommitted = false;
          if (laStfId != null && !StfContext.isLastCommitted()) {
            String callerObjId = StfContext.getBizObjId(callerClass);
            String identity = StfConfigs.actionPathBy(callerObjId, callerMethodName);
            if (identity.equals(laStfId.getIdentity())) {
              StfContext.commitLastDone(laStfId.getValue());
              isLaStfCommitted = true;
            }
          }

          // support 'nested' transaction,meanwhile,try continue processing last-committed transaction,for last stfs to
          // be confirmed if any
          String callerObjId = null;
          try {
            callerObjId = StfContext.getBizObjId(callerClass);
            String callerKey = lineageKeyOf(callerObjId, callerMethodName);
            @SuppressWarnings("unchecked")
            MutablePair<Boolean, Long> nestedTxLaStfCommitInfo = (MutablePair<Boolean, Long>)TransactionResourceManager
                .getResource(callerKey);
            if (nestedTxLaStfCommitInfo != null) {
              if (!nestedTxLaStfCommitInfo.getLeft()) {
                Long laStfId = nestedTxLaStfCommitInfo.getRight();
                if (!laStfId.equals(this.laStfId.getValue()) || !isLaStfCommitted) {
                  StfContext.commitLastDone(laStfId);
                }
                nestedTxLaStfCommitInfo.setLeft(true);
              }
            }
          } catch (Throwable e) {
            Exceptions.sneakyThrow(e, LOG, "[{}#{}]Commit last stf |error: '{}'", callerObjId, callerMethodName,
                e.getMessage());
          }
        }

        @Override
        public void beforeCommit(boolean readOnly) throws RuntimeException {
          if (readOnly) {
            throw new RuntimeException("Read-only transaction is not supported by Stf");
          }

          if (StfContext.isLastCommitted()) {
            LOG.warn(
                "[{}]The last step is already committed,any pre-commit next-step is canceled > It seems that StfContext#commitLastXxx has been invoked before",
                callerMethodName);
            return;
          }

          /*
           * Assuming this is the last time call StfContext#laStfId,so this is the right time to remove TTL,for the
           * purpose preventing the resource leak.
           * Store 'laStfId' here,that's the way to support normal single transaction,but not the nested transaction.
           */// ->
          laStfId = StfContext.laStfId();
          StfContext.removeTTL();
          // <-

          String callerObjId = null;
          try {
            callerObjId = StfContext.getBizObjId(callerClass);
            Pair<String, String> calleeInfo = StfConfigs.determineForwardToOf(callerObjId, callerMethodName);
            if (calleeInfo == null) {
              if (!StfConfigs.existForwardsFromOf(callerObjId, callerMethodName)) {// TODO mj:using orphan do the
                                                                                   // judgment?
                LOG.warn(
                    "[{}]No matched callee > Using raw transaction-ops is recommended, e.g. Spring's TransactionTemplate, StfTxnOps#rawExecuteXxx",
                    callerMethodName);
              }
              return;
            }
            String calleeObjId = calleeInfo.getLeft();
            String calleeMethodName = calleeInfo.getRight();
            Pair<?, Class<?>>[] args = StfConfigs.determineActionMethodArgs(calleeObjId, calleeMethodName, bizOut);
            Long newStfId = StfContext.preCommitNextStep(calleeObjId, calleeMethodName, args);

            // support 'nested' transaction(found unexpected re-order confirming last stf in such scenario)->
            String calleeKey = lineageKeyOf(calleeObjId, calleeMethodName);
            TransactionResourceManager.bindResource(calleeKey, MutablePair.of(false, newStfId));
            // <-
          } catch (Throwable e) {
            Exceptions.sneakyThrow(e, LOG, "[{}#{}]Pre commit next-step |error: '{}'", callerObjId, callerMethodName,
                e.getMessage());
          }
        }
      });
    }
    return method.invoke(rawTxnCb, args);
  }

  public String lineageKeyOf(String oid, String methodName) {
    return lenientFormat("%s-%s-%s", StfTxnCallback.class.getSimpleName(), oid, methodName);
  }

  StfTxnCallback(TransactionCallback<T> rawTxCb, String callerClassName, String callerMethodName,
      ClassLoader bizClassLoader, T bizOut) throws ClassNotFoundException {
    this.rawTxnCb = rawTxCb;
    this.bizOut = bizOut;
    this.callerClass = Class.forName(callerClassName, true, bizClassLoader);
    this.callerMethodName = callerMethodName;
  }
}
