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
package com.stun4j.stf.boot;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StfHelper.newHashMap;
import static com.stun4j.stf.core.StfHelper.registerGracefulShutdown;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.base.CaseFormat;
import com.stun4j.stf.core.support.JsonHelper;
import com.stun4j.stf.core.utils.Exceptions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Jay Meng
 */
class DelayQueueDataSourceBeanRegister {
  private static final Logger LOG = LoggerFactory.getLogger(DelayQueueDataSourceBeanRegister.class);

  private final Environment env;
  private final ApplicationContext appCtx;

  private final StfProperties props;

  private DataSource dlqDs;

  void tryRegister() {
    String coreDsBeanName = props.getCore().getDatasourceBeanName();
    DelayQueue dlqCfg;
    String dlqDsBeanName = (dlqCfg = props.getDelayQueue()).getDatasourceBeanName();
    if (dlqDsBeanName.equals(coreDsBeanName)) {
      if (dlqCfg.getDatasource().hasAnyBasicJdbcPropertyConfigured()) {
        LOG.warn(
            "Any datasource property(e.g. url,password,username etc.) defined for delay-queue will be ignored > Consider using a different datasource name than Stf-core's(current using: '{}')",
            coreDsBeanName);
      }
      return;
    }
    if (!dlqCfg.getDatasource().isAutoCreateEnabled()) {
      if (dlqCfg.getDatasource().hasAnyBasicJdbcPropertyConfigured()) {
        LOG.warn(
            "Any datasource property(e.g. url,password,username etc.) defined for delay-queue will be ignored > Consider set 'stun4j.stf.delay-queue.auto-create-enabled' to true",
            coreDsBeanName);
      }
      return;
    }
    GenericApplicationContext ctx = (GenericApplicationContext)appCtx;// TODO mj:Support other type ctx
    ctx.registerBean(dlqDsBeanName, DataSource.class, () -> {
      DataSource coreDs = ctx.getBean(coreDsBeanName, DataSource.class);
      return dlqDs = newDlqDs(coreDs, appCtx);
    });
  }

  @SuppressWarnings("unchecked")
  private DataSource newDlqDs(DataSource tplDs, ApplicationContext appCtx) {
    String prefix = "stun4j.stf.delay-queue.datasource";
    Datasource dsCfg = props.getDelayQueue().getDatasource();
    BindResult<?> res;
    try {
      // If connection-pool-vendor is specified,try using it
      if ((res = Binder.get(env).bind(prefix + ".hikari", HashMap.class)).isBound()) {
        Map<String, Object> newDsCfgRaw = (Map<String, Object>)res.get();

        Map<String, Object> camleCased = newHashMap(newDsCfgRaw.keySet().stream(), (map, rawKey) -> {
          String camleCasedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, rawKey);
          map.put(camleCasedKey, newDsCfgRaw.get(rawKey));
          return map;
        });
        String jsonClzId;
        String fooStr = JsonHelper.toJson(camleCased);
        fooStr = fooStr.replaceFirst(lenientFormat("\"%s\":\"%s\"", jsonClzId = "@class", HashMap.class.getName()),
            lenientFormat("\"%s\":\"%s\\$%s\"", jsonClzId, this.getClass().getName(),
                SafeHikariConfig.class.getSimpleName()));

        HikariConfig newDsCfg = JsonHelper.fromJson(fooStr, SafeHikariConfig.class);// TODO mj:try also properties

        copyBasicDataSourcePropertiesTo(dsCfg, newDsCfg);
        return new HikariDataSource(newDsCfg);
      }
      if ((res = Binder.get(env).bind(prefix + ".druid", Properties.class)).isBound()) {
        Properties newDsCfgRaw = (Properties)res.get();
        Properties camleCased = new Properties();
        newDsCfgRaw.forEach((k, v) -> {
          camleCased.put("druid." + k, v);
        });
        DruidDataSource newDsCfg = new DruidDataSource();
        newDsCfg.configFromPropety(camleCased);

        copyBasicDataSourcePropertiesTo(dsCfg, newDsCfg);
        return newDsCfg;// TODO mj:jvm shutdown hook?
      }
    } catch (Exception e) {
      Exceptions.sneakyThrow(e);
    }

    // If no connection-pool-vendor is specified,try using template datasource's
    if (tplDs.getClass().getSimpleName().indexOf("Hikari") != -1) {
      HikariConfig tplDsCfg = ((HikariConfig)tplDs);
      HikariConfig newDsCfg;
      tplDsCfg.copyStateTo(newDsCfg = new HikariConfig());
      copyBasicDataSourcePropertiesTo(tplDsCfg, newDsCfg);
      return new HikariDataSource(newDsCfg);
    }

    if (tplDs.getClass().getSimpleName().indexOf("Druid") != -1) {
      DruidDataSource tplDsCfg = ((DruidDataSource)tplDs);
      DruidDataSource newDs;
      copyBasicDataSourcePropertiesTo(tplDsCfg, newDs = new DruidDataSource());
      copyAdvancedDataSourcePropertiesTo(tplDsCfg, newDs);
      return newDs;
    }
    // Using plain-datasource if found no connection-pool-vendor
    String url = dsCfg.getUrl();
    String username = dsCfg.getUsername();
    String password = dsCfg.getPassword();
    return new DriverManagerDataSource(url, username, password);
  }

  private void copyBasicDataSourcePropertiesTo(Object fromDsCfg, Object toDsCfg) {
    try {
      String fromUsername = (String)MethodUtils.invokeMethod(fromDsCfg, "getUsername");
      String fromPassword = (String)MethodUtils.invokeMethod(fromDsCfg, "getPassword");
      String fromDriverClassName = (String)MethodUtils.invokeMethod(fromDsCfg, "getDriverClassName");
      String fromUrl;

      if (toDsCfg instanceof HikariConfig) {
        fromUrl = (String)MethodUtils.invokeMethod(fromDsCfg, "getJdbcUrl");
        MethodUtils.invokeMethod(toDsCfg, "setJdbcUrl", fromUrl);
      } else {
        fromUrl = (String)MethodUtils.invokeMethod(fromDsCfg, "getUrl");
        MethodUtils.invokeMethod(toDsCfg, "setUrl", fromUrl);
      }

      MethodUtils.invokeMethod(toDsCfg, "setUsername", fromUsername);
      MethodUtils.invokeMethod(toDsCfg, "setPassword", fromPassword);
      MethodUtils.invokeMethod(toDsCfg, "setDriverClassName", fromDriverClassName);
    } catch (Exception e) {
      Exceptions.sneakyThrow(e);
    }
  }

  private void copyAdvancedDataSourcePropertiesTo(DruidDataSource fromDsCfg, DruidDataSource toDsCfg) {
    // based on druid:1.2.15
    String tmpStr;
    boolean tmpBool;
    int tmpInt;
    long tmpLong;
    tmpStr = fromDsCfg.getName();// druid.name
    toDsCfg.setName(tmpStr);

    // Should be already done->
    // fromDsCfg.getUrl();// druid.url
    // fromDsCfg.getUsername();// druid.username
    // fromDsCfg.getPassword();// druid.password
    // <-

    tmpBool = fromDsCfg.isTestWhileIdle();// druid.testWhileIdle
    toDsCfg.setTestWhileIdle(tmpBool);

    tmpBool = fromDsCfg.isTestOnBorrow();// druid.testOnBorrow
    toDsCfg.setTestOnBorrow(tmpBool);

    tmpStr = fromDsCfg.getValidationQuery();// druid.validationQuery
    toDsCfg.setValidationQuery(tmpStr);

    tmpBool = fromDsCfg.isUseGlobalDataSourceStat();// druid.useGlobalDataSourceStat
    toDsCfg.setUseGlobalDataSourceStat(tmpBool);

    // druid.useGloalDataSourceStat// compatible for early versions mj:typo problem not exist during direct copy from
    // normalized template ds

    tmpBool = fromDsCfg.isAsyncInit();// druid.asyncInit// compatible for early versions
    toDsCfg.setAsyncInit(tmpBool);

    // druid.filters->
    List<Filter> filters;
    filters = fromDsCfg.getProxyFilters();
    toDsCfg.setProxyFilters(filters);
    // <-

    tmpBool = fromDsCfg.isClearFiltersEnable();// druid.clearFiltersEnable
    toDsCfg.setClearFiltersEnable(tmpBool);

    tmpBool = fromDsCfg.isResetStatEnable();// druid.resetStatEnable
    toDsCfg.setResetStatEnable(tmpBool);

    tmpInt = fromDsCfg.getNotFullTimeoutRetryCount();// druid.notFullTimeoutRetryCount
    toDsCfg.setNotFullTimeoutRetryCount(tmpInt);

    tmpLong = fromDsCfg.getTimeBetweenEvictionRunsMillis();// druid.timeBetweenEvictionRunsMillis
    toDsCfg.setTimeBetweenEvictionRunsMillis(tmpLong);

    tmpInt = fromDsCfg.getMaxWaitThreadCount();// druid.maxWaitThreadCount
    toDsCfg.setMaxWaitThreadCount(tmpInt);

    tmpLong = fromDsCfg.getMaxWait();// druid.maxWait
    toDsCfg.setMaxWait(tmpLong);

    tmpBool = fromDsCfg.isFailFast();// druid.failFast
    toDsCfg.setFailFast(tmpBool);

    tmpLong = fromDsCfg.getPhyTimeoutMillis();// druid.phyTimeoutMillis
    toDsCfg.setPhyTimeoutMillis(tmpLong);

    tmpLong = fromDsCfg.getPhyMaxUseCount();// druid.phyMaxUseCount
    toDsCfg.setPhyMaxUseCount(tmpLong);

    tmpLong = fromDsCfg.getMinEvictableIdleTimeMillis();// druid.minEvictableIdleTimeMillis
    toDsCfg.setMinEvictableIdleTimeMillis(tmpLong);

    tmpLong = fromDsCfg.getMaxEvictableIdleTimeMillis();// druid.maxEvictableIdleTimeMillis
    toDsCfg.setMaxEvictableIdleTimeMillis(tmpLong);

    tmpLong = fromDsCfg.getKeepAliveBetweenTimeMillis();// druid.keepAliveBetweenTimeMillis
    toDsCfg.setKeepAliveBetweenTimeMillis(tmpLong);

    tmpBool = fromDsCfg.isPoolPreparedStatements();// druid.poolPreparedStatements
    toDsCfg.setPoolPreparedStatements(tmpBool);

    tmpBool = fromDsCfg.isInitVariants();// druid.initVariants
    toDsCfg.setInitVariants(tmpBool);

    tmpBool = fromDsCfg.isInitGlobalVariants();// druid.initGlobalVariants
    toDsCfg.setInitGlobalVariants(tmpBool);

    tmpBool = fromDsCfg.isUseUnfairLock();// druid.useUnfairLock
    toDsCfg.setUseUnfairLock(tmpBool);

    // Should be already done->
    // tmp = fromDsCfg.getDriverClassName();// druid.driverClassName
    // toDsCfg.setDriverClassName(tmp);
    // <-

    tmpInt = fromDsCfg.getInitialSize();// druid.initialSize
    toDsCfg.setInitialSize(tmpInt);

    tmpInt = fromDsCfg.getMinIdle();// druid.minIdle
    toDsCfg.setMinIdle(tmpInt);

    tmpInt = fromDsCfg.getMaxActive();// druid.maxActive
    toDsCfg.setMaxActive(tmpInt);

    tmpBool = fromDsCfg.isKillWhenSocketReadTimeout();// druid.killWhenSocketReadTimeout
    toDsCfg.setKillWhenSocketReadTimeout(tmpBool);

    Properties props = fromDsCfg.getConnectProperties();// druid.connectProperties
    toDsCfg.setConnectProperties(props);

    tmpInt = fromDsCfg.getMaxPoolPreparedStatementPerConnectionSize();// druid.maxPoolPreparedStatementPerConnectionSize
    toDsCfg.setMaxPoolPreparedStatementPerConnectionSize(tmpInt);

    Collection<String> sqls = fromDsCfg.getConnectionInitSqls();// druid.initConnectionSqls
    toDsCfg.setConnectionInitSqls(sqls);

    // druid.load.spifilter.skip->
    Field fld;
    try {
      tmpBool = (fld = FieldUtils.getField(DruidDataSource.class, "loadSpifilterSkip", true)).getBoolean(fromDsCfg);
      fld.setBoolean(toDsCfg, tmpBool);
    } catch (Exception e) {
      Exceptions.sneakyThrow(e);
    }
    // <-

    tmpBool = fromDsCfg.isCheckExecuteTime();// druid.checkExecuteTime
    toDsCfg.setCheckExecuteTime(tmpBool);
  }

  DelayQueueDataSourceBeanRegister(Environment env, ApplicationContext appCtx, StfProperties props) {
    this.env = env;
    this.appCtx = appCtx;
    this.props = props;

    registerGracefulShutdown(LOG, () -> {
      if (dlqDs instanceof HikariDataSource) {
        ((HikariDataSource)dlqDs).close();
      } else if (dlqDs instanceof DruidDataSource) {
        ((DruidDataSource)dlqDs).close();
      }
      return null;
    });
  }

  private static class SafeHikariConfig extends HikariConfig {
  }

}
