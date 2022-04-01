/*
 * Copyright 2022 the original author or authors.
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
package com.stun4j.stf.boot;

import org.springframework.transaction.TransactionDefinition;

import com.stun4j.stf.core.support.BaseVo;

/**
 * Stf global transaction configuration
 * <p>
 * Doc from spring-tx:5.3.17 with some '@see XXX' commented out
 * @author Jay Meng
 */
public class Transaction extends BaseVo {

  /**
   * Default: default
   */
  private IsolationLevel isolationLevel = IsolationLevel.DEFAULT;

  /**
   * Default: required
   */
  private Propagation propagation = Propagation.REQUIRED;

  public IsolationLevel getIsolationLevel() {
    return isolationLevel;
  }

  public void setIsolationLevel(IsolationLevel isolationLevel) {
    this.isolationLevel = isolationLevel;
  }

  public Propagation getPropagation() {
    return propagation;
  }

  public void setPropagation(Propagation propagation) {
    this.propagation = propagation;
  }

  enum IsolationLevel {
    /**
     * Use the default isolation level of the underlying datastore.
     * All other levels correspond to the JDBC isolation levels.
     * @see java.sql.Connection
     */
    DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),
    /**
     * Indicates that dirty reads are prevented; non-repeatable reads and
     * phantom reads can occur.
     * <p>
     * This level only prohibits a transaction from reading a row
     * with uncommitted changes in it.
     * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
     */
    READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED), // same as
                                                                    // java.sql.Connection.TRANSACTION_READ_COMMITTED;
    /**
     * Indicates that dirty reads and non-repeatable reads are prevented;
     * phantom reads can occur.
     * <p>
     * This level prohibits a transaction from reading a row with uncommitted changes
     * in it, and it also prohibits the situation where one transaction reads a row,
     * a second transaction alters the row, and the first transaction re-reads the row,
     * getting different values the second time (a "non-repeatable read").
     * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
     */
    REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ);// same as
                                                                     // java.sql.Connection.TRANSACTION_REPEATABLE_READ;

    private final int value;

    private IsolationLevel(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

  }

  enum Propagation {
    /**
     * Support a current transaction; create a new one if none exists.
     * Analogous to the EJB transaction attribute of the same name.
     * <p>
     * This is typically the default setting of a transaction definition,
     * and typically defines a transaction synchronization scope.
     */
    REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
    /**
     * Support a current transaction; execute non-transactionally if none exists.
     * Analogous to the EJB transaction attribute of the same name.
     * <p>
     * <b>NOTE:</b> For transaction managers with transaction synchronization,
     * {@code PROPAGATION_SUPPORTS} is slightly different from no transaction
     * at all, as it defines a transaction scope that synchronization might apply to.
     * As a consequence, the same resources (a JDBC {@code Connection}, a
     * Hibernate {@code Session}, etc) will be shared for the entire specified
     * scope. Note that the exact behavior depends on the actual synchronization
     * configuration of the transaction manager!
     * <p>
     * In general, use {@code PROPAGATION_SUPPORTS} with care! In particular, do
     * not rely on {@code PROPAGATION_REQUIRED} or {@code PROPAGATION_REQUIRES_NEW}
     * <i>within</i> a {@code PROPAGATION_SUPPORTS} scope (which may lead to
     * synchronization conflicts at runtime). If such nesting is unavoidable, make sure
     * to configure your transaction manager appropriately (typically switching to
     * "synchronization on actual transaction").
     * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
     * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
     */
    SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),
    /**
     * Support a current transaction; throw an exception if no current transaction
     * exists. Analogous to the EJB transaction attribute of the same name.
     * <p>
     * Note that transaction synchronization within a {@code PROPAGATION_MANDATORY}
     * scope will always be driven by the surrounding transaction.
     */
    MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),
    /**
     * Create a new transaction, suspending the current transaction if one exists.
     * Analogous to the EJB transaction attribute of the same name.
     * <p>
     * <b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
     * on all transaction managers. This in particular applies to
     * {@link org.springframework.transaction.jta.JtaTransactionManager},
     * which requires the {@code javax.transaction.TransactionManager} to be
     * made available it to it (which is server-specific in standard Java EE).
     * <p>
     * A {@code PROPAGATION_REQUIRES_NEW} scope always defines its own
     * transaction synchronizations. Existing synchronizations will be suspended
     * and resumed appropriately.
     * //@see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
     */
    REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
    /**
     * Do not support a current transaction; rather always execute non-transactionally.
     * Analogous to the EJB transaction attribute of the same name.
     * <p>
     * <b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
     * on all transaction managers. This in particular applies to
     * {@link org.springframework.transaction.jta.JtaTransactionManager},
     * which requires the {@code javax.transaction.TransactionManager} to be
     * made available it to it (which is server-specific in standard Java EE).
     * <p>
     * Note that transaction synchronization is <i>not</i> available within a
     * {@code PROPAGATION_NOT_SUPPORTED} scope. Existing synchronizations
     * will be suspended and resumed appropriately.
     * //@see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
     */
    NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

    /**
     * Do not support a current transaction; throw an exception if a current transaction
     * exists. Analogous to the EJB transaction attribute of the same name.
     * <p>
     * Note that transaction synchronization is <i>not</i> available within a
     * {@code PROPAGATION_NEVER} scope.
     */
    NEVER(TransactionDefinition.PROPAGATION_NEVER),
    /**
     * Execute within a nested transaction if a current transaction exists,
     * behave like {@link #PROPAGATION_REQUIRED} otherwise. There is no
     * analogous feature in EJB.
     * <p>
     * <b>NOTE:</b> Actual creation of a nested transaction will only work on
     * specific transaction managers. Out of the box, this only applies to the JDBC
     * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
     * when working on a JDBC 3.0 driver. Some JTA providers might support
     * nested transactions as well.
     * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
     */
    NESTED(TransactionDefinition.PROPAGATION_NESTED);

    private final int value;

    private Propagation(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }
}
