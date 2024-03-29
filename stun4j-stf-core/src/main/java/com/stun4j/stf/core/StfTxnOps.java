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

import static com.stun4j.stf.core.StfHelper.H;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.stun4j.stf.core.utils.Exceptions;

/**
 * Stf's core transaction operation, which delegates to Spring's transaction operation
 * <ul>
 * <li>The 'out' parameter is an object that will be the outgoing parameter of the current method
 * and the incoming parameter of the downstream method.This is the typical, recommended way to make
 * Stf work well</li>
 * <li>Be care of the case where you passing 'out' as {@code null} and it is generally accepted that
 * the downstream method does not need an input. In this case, do not pass {@code null} and instead
 * use '#executeXXX' methods that have no 'out' parameter</li>
 * <li>Note that the 'out' object is at risk of becoming dirty. Changes made to 'out' inside the
 * closure of the '#executeXXX' method will be persisted and correctly passed to downstream methods,
 * while changes made to 'out' outside the closure will become dirty, in which case the persistent
 * data will not be consistent with the in-memory version.</li>
 * <li>The better option is to use methods like '#executeWithFinalXXX' or '#executeWithNonFinalXXX',
 * which minimize the side effects of the problem above</li>
 * </ul>
 * 
 * @author Jay Meng
 */
public class StfTxnOps {
  private static final Logger LOG = LoggerFactory.getLogger(StfTxnOps.class);

  private final TransactionOperations rawTxnOps;
  private final String coreTblName;

  // Low side-effect apis - - - - - - - - - - - - - - - - -->
  public <OUT> OUT executeWithFinalResult(Supplier<OUT> outInitSupplier,
      Function<OUT, Consumer<StfTransactionStatus>> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    OUT out = outInitSupplier.get();
    doExecuteWithoutResult(out, action.apply(out), callStacks, null, null);
    return out;
  }

  public <OUT> OUT executeWithNonFinalResult(Supplier<OUT> outInitSupplier,
      Function<OUT, Function<StfTransactionStatus, OUT>> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    OUT out = outInitSupplier.get();
    return doExecute(out, action.apply(out), callStacks, null);
  }

  public <OUT> OUT executeWithFinalResult(Supplier<OUT> outSupplier, Consumer<StfTransactionStatus> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    OUT out = outSupplier.get();
    doExecuteWithoutResult(out, action, callStacks, null, null);
    return out;
  }

  public <OUT> OUT executeWithNonFinalResult(Supplier<OUT> outSupplier,
      Function<OUT, Consumer<StfTransactionStatus>> action, Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    OUT out = outSupplier.get();
    doExecuteWithoutResult(out, action.apply(out), callStacks, null, txnOpsProvider);
    return out;
  }

  public <OUT> OUT executeWithFinalResult(Supplier<OUT> outSupplier, Consumer<StfTransactionStatus> action,
      Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    OUT out = outSupplier.get();
    doExecuteWithoutResult(out, action, callStacks, null, txnOpsProvider);
    return out;
  }

  public <T> T execute(Function<StfTransactionStatus, T> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    return doExecute(null, action, callStacks, null);
  }

  public <T> T execute(Function<StfTransactionStatus, T> action, Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    return doExecute(null, action, callStacks, txnOpsProvider);
  }

  public <T> void executeWithoutResult(Consumer<StfTransactionStatus> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(null, action, callStacks, null, null);
  }

  public <T> void executeWithoutResult(Consumer<StfTransactionStatus> action,
      Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(null, action, callStacks, null, txnOpsProvider);
  }

  public <T> void executeWithoutResult(Consumer<StfTransactionStatus> action,
      BiFunction<Throwable, StfTransactionStatus, Boolean> onError) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(null, action, callStacks, onError, null);
  }

  public <T> void executeWithoutResult(Consumer<StfTransactionStatus> action,
      BiFunction<Throwable, StfTransactionStatus, Boolean> onError, Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(null, action, callStacks, onError, txnOpsProvider);
  }

  // High side-effect apis - - - - - - - - - - - - - - - - -->

  // TODO mj:give a check on out shouldn't be null('callee has param' is a good start),null leads
  // unknown
  // error,currently
  public <T> T execute(T out, Function<StfTransactionStatus, T> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    return doExecute(out, action, callStacks, null);
  }

  public <T> T execute(T out, Function<StfTransactionStatus, T> action,
      Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    return doExecute(out, action, callStacks, txnOpsProvider);
  }

  public <T> void executeWithoutResult(T out, Consumer<StfTransactionStatus> action) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(out, action, callStacks, null, null);
  }

  public <T> void executeWithoutResult(T out, Consumer<StfTransactionStatus> action,
      Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(out, action, callStacks, null, txnOpsProvider);
  }

  public <T> void executeWithoutResult(T out, Consumer<StfTransactionStatus> action,
      BiFunction<Throwable, StfTransactionStatus, Boolean> onError) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(out, action, callStacks, onError, null);
  }

  public <T> void executeWithoutResult(T out, Consumer<StfTransactionStatus> action,
      BiFunction<Throwable, StfTransactionStatus, Boolean> onError, Supplier<TransactionOperations> txnOpsProvider) {
    StackTraceElement[] callStacks = Thread.currentThread().getStackTrace();
    doExecuteWithoutResult(out, action, callStacks, onError, txnOpsProvider);
  }

  private <T> T doExecute(T out, Function<StfTransactionStatus, T> action, StackTraceElement[] callStacks,
      Supplier<TransactionOperations> txnOpsProvider) {
    Pair<String, String> callerInfo = determineCallerInfo(callStacks);
    return doExecute0(out, action, callerInfo.getLeft(), callerInfo.getRight(), txnOpsProvider);
  }

  private <T> T doExecute0(T out, Function<StfTransactionStatus, T> action, String callerClassName,
      String callerMethodName, Supplier<TransactionOperations> txnOpsProvider) {
    Long laStfId = StfContext.safeGetLaStfIdValue();
    return determineTxnOps(txnOpsProvider).execute(StfTxnCallback.of(rawSt -> {
      StfTransactionStatus st = new StfTransactionStatus(rawSt);
      try {
        return action.apply(st);
      } catch (Throwable e) {
        if (e instanceof DuplicateKeyException) {
          try {
            H.tryCommitLaStfOnDup(LOG, laStfId, coreTblName, (DuplicateKeyException)e,
                stfId -> StfContext.commitLastDone(stfId));// StfContext.commitLastDone
          } catch (Throwable e1) {
            Exceptions.swallow(e1, LOG, "An error occurred while auto committing stf");
            // Silently move forward current transaction by swallowing any exception
          }
        }
        // TODO mj:error handling,consider the same mechanism like #doExecuteWithoutResult->
        // if (onError != null) {
        // boolean isSwallowAnyError = onError.apply(e, st);
        // if (isSwallowAnyError) {
        // return null;
        // }
        // }
        // <-
        throw e;
      }
    }, callerClassName, callerMethodName, out));
  }

  private <T> void doExecuteWithoutResult(T out, Consumer<StfTransactionStatus> action, StackTraceElement[] callStacks,
      BiFunction<Throwable, StfTransactionStatus, Boolean> onError, Supplier<TransactionOperations> txnOpsProvider) {
    Pair<String, String> callerInfo = determineCallerInfo(callStacks);
    doExecuteWithoutResult0(out, action, callerInfo.getLeft(), callerInfo.getRight(), onError, txnOpsProvider);
  }

  private <T> void doExecuteWithoutResult0(T out, Consumer<StfTransactionStatus> action, String callerClassName,
      String callerMethodName, BiFunction<Throwable, StfTransactionStatus, Boolean> onError,
      Supplier<TransactionOperations> txnOpsProvider) {
    Long laStfId = StfContext.safeGetLaStfIdValue();
    determineTxnOps(txnOpsProvider).execute(StfTxnCallback.of(rawSt -> {
      StfTransactionStatus st = new StfTransactionStatus(rawSt);
      try {
        action.accept(st);
      } catch (Throwable e) {
        if (e instanceof DuplicateKeyException) {
          try {
            H.tryCommitLaStfOnDup(LOG, laStfId, coreTblName, (DuplicateKeyException)e,
                stfId -> StfContext.commitLastDone(stfId));
          } catch (Throwable e1) {
            Exceptions.swallow(e1, LOG, "An error occurred while auto committing stf");
            // Silently move forward current transaction by swallowing any exception
          }
        }
        if (onError != null) {
          boolean isSwallowAnyError = onError.apply(e, st);
          if (isSwallowAnyError) {
            return null;
          }
        }
        throw e;
      }
      return null;
    }, callerClassName, callerMethodName, out));
  }
  // <-- - - - - - - - - - - - - - -

  private Pair<String, String> determineCallerInfo(StackTraceElement[] callStacks) {
    Pair<String, String> callerInfo = null;
    for (int i = 2; i < callStacks.length; i++) {
      StackTraceElement callStack = callStacks[i];
      String callerMethodName = callStack.getMethodName();
      String callerClzName = callStack.getClassName();
      if (callerClzName.indexOf("$$FastClassBySpringCGLIB") != -1
          || callerClzName.indexOf("$$EnhancerBySpringCGLIB") != -1) {
        continue;
      }
      String fileName = callStack.getFileName();
      if (fileName.indexOf("<generated>") != -1) {
        continue;
      }
      String pack = "org.springframework";
      if (callerClzName.indexOf(pack) != -1) {
        continue;
      }
      callerInfo = Pair.of(callerClzName, callerMethodName);
      break;
    }
    return callerInfo;
  }

  /**
   * @see org.springframework.transaction.support.TransactionOperations#executeWithoutResult
   */
  public void rawExecuteWithoutResult(Consumer<TransactionStatus> action) {
    rawExecuteWithoutResult(action, null);
  }

  public void rawExecuteWithoutResult(Consumer<TransactionStatus> action,
      Supplier<TransactionOperations> txnOpsProvider) {
    determineTxnOps(txnOpsProvider).executeWithoutResult(action);
  }

  /**
   * @see org.springframework.transaction.support.TransactionOperations#execute
   */
  public <T> T rawExecute(TransactionCallback<T> action) {
    return rawExecute(action, null);
  }

  public <T> T rawExecute(TransactionCallback<T> action, Supplier<TransactionOperations> txnOpsProvider) {
    return determineTxnOps(txnOpsProvider).execute(action);
  }

  private TransactionOperations determineTxnOps(Supplier<TransactionOperations> txnOpsProvider) {
    return txnOpsProvider == null ? rawTxnOps : txnOpsProvider.get();
  }

  public StfTxnOps(TransactionOperations rawTxnOps) {
    this(rawTxnOps, StfConsts.DFT_CORE_TBL_NAME);
  }

  public StfTxnOps(TransactionOperations rawTxnOps, String coreTblName) {
    this.rawTxnOps = rawTxnOps;
    this.coreTblName = coreTblName;
  }

  public String getCoreTblName() {
    return coreTblName;
  }
}