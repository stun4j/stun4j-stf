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

import static com.stun4j.stf.boot.DefaultExecutor.RejectPolicy.DROP_WITH_EX_THROW;
import static com.stun4j.stf.boot.DefaultExecutor.RejectPolicy.SILENT_DROP;
import static com.stun4j.stf.boot.DefaultExecutor.RejectPolicy.SILENT_DROP_OLDEST;
import static com.stun4j.stf.core.StfConsts.DFT_CONF_SUFFIX;
import static com.stun4j.stf.core.StfConsts.allDataSourceKeys;
import static com.stun4j.stf.core.StfHelper.newHashMap;
import static com.stun4j.stf.core.StfRunMode.CLIENT;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.BACK_PRESSURE_POLICY;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.DROP_WITH_EX_THROW_POLICY;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.SILENT_DROP_OLDEST_POLICY;
import static com.stun4j.stf.core.utils.executor.PoolExecutors.SILENT_DROP_POLICY;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.stun4j.guid.boot.GuidAutoConfigure;
import com.stun4j.guid.core.utils.Asserts;
import com.stun4j.guid.core.utils.Strings;
import com.stun4j.stf.boot.Transaction.IsolationLevel;
import com.stun4j.stf.boot.Transaction.Propagation;
import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.StfCore;
import com.stun4j.stf.core.StfCoreJdbc;
import com.stun4j.stf.core.StfDelayQueue;
import com.stun4j.stf.core.StfDelayQueueCore;
import com.stun4j.stf.core.StfMetaGroup;
import com.stun4j.stf.core.StfRunMode;
import com.stun4j.stf.core.StfTxnOps;
import com.stun4j.stf.core.build.StfConfig;
import com.stun4j.stf.core.build.StfConfigs;
import com.stun4j.stf.core.cluster.Heartbeat;
import com.stun4j.stf.core.cluster.HeartbeatHandlerJdbc;
import com.stun4j.stf.core.job.JobLoader;
import com.stun4j.stf.core.job.JobManager;
import com.stun4j.stf.core.job.JobRunners;
import com.stun4j.stf.core.job.JobScanner;
import com.stun4j.stf.core.job.JobScannerJdbc;
import com.stun4j.stf.core.monitor.JvmCpu;
import com.stun4j.stf.core.monitor.JvmMemory;
import com.stun4j.stf.core.monitor.StfMonitor;
import com.stun4j.stf.core.monitor.SystemLoad;
import com.stun4j.stf.core.spi.StfJdbcOps;
import com.stun4j.stf.core.spi.StfRegistry;
import com.stun4j.stf.core.support.executor.StfExecutorService;
import com.stun4j.stf.core.support.executor.StfInternalExecutors;
import com.stun4j.stf.core.support.persistence.StfDefaultSpringJdbcOps;
import com.stun4j.stf.core.support.registry.StfDefaultSpringRegistry;
import com.stun4j.stf.core.utils.Exceptions;

/**
 * Responsible for loading, starting, and initializing Stf from its boot configuration files.
 * 
 * @author Jay Meng
 */
@Configuration
@EnableConfigurationProperties(StfProperties.class)
public class StfAutoConfigure implements BeanClassLoaderAware, EnvironmentAware, ApplicationContextAware {
  private static final Logger LOG = LoggerFactory.getLogger(StfAutoConfigure.class);

  private final StfProperties props;

  private BiFunction<Object, TreeMap<Integer, Object>, Boolean> flowConfFilterAndSortFn;

  private ClassLoader classLoader;
  private Environment environment;
  private ApplicationContext applicationContext;

  @Bean
  StfTxnOps stfTxnOps() {
    DataSource dataSource = applicationContext.getBean(props.getCore().getDatasourceBeanName(), DataSource.class);
    IsolationLevel txIsolationLvl = props.getTransaction().getIsolationLevel();
    Propagation txPropagation = props.getTransaction().getPropagation();
    TransactionTemplate rawTxnOps = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    rawTxnOps.setIsolationLevel(txIsolationLvl.getValue());
    rawTxnOps.setPropagationBehavior(txPropagation.getValue());
    // TODO mj:timeout
    return new StfTxnOps(rawTxnOps);
  }

  @Bean
  StfExecutorService stfDftExec() {
    DefaultExecutor cfg = props.getDefaultExecutor();
    RejectedExecutionHandler rejectPolicy = BACK_PRESSURE_POLICY;
    if (DROP_WITH_EX_THROW == cfg.getThreadRejectPolicy()) {
      rejectPolicy = DROP_WITH_EX_THROW_POLICY;
    } else if (SILENT_DROP == cfg.getThreadRejectPolicy()) {
      rejectPolicy = SILENT_DROP_POLICY;
    } else if (SILENT_DROP_OLDEST == cfg.getThreadRejectPolicy()) {
      rejectPolicy = SILENT_DROP_OLDEST_POLICY;
    }
    return StfInternalExecutors.newDefaultExec(cfg.getThreadKeepAliveTimeSeconds(), cfg.getTaskQueueSize(),
        rejectPolicy, cfg.isAllowCoreThreadTimeOut());
  }

  @Bean
  @ConditionalOnProperty(prefix = "stun4j.stf.delay-queue", name = "enabled", havingValue = "true", matchIfMissing = true)
  StfDelayQueue stfDelayQueue() {
    return new StfDelayQueue(StfContext.delayQueueCore());
  }

  private void doEarlyInitialize() {
    // configure global
    Body coreBodyCfg;
    Body dlqBodyCfg;
    StfMetaGroup.CORE.withGlobalStfBodyBytesEnabled((coreBodyCfg = props.getCore().getBody()).isBytesEnabled())
        .withGlobalStfBodyCompress(coreBodyCfg.getCompressAlgorithm());
    StfMetaGroup.DELAY.withGlobalStfBodyBytesEnabled((dlqBodyCfg = props.getDelayQueue().getBody()).isBytesEnabled())
        .withGlobalStfBodyCompress(dlqBodyCfg.getCompressAlgorithm());

    // configure core
    String coreDsBeanName = props.getCore().getDatasourceBeanName();
    String delayDsBeanName = props.getDelayQueue().getDatasourceBeanName();
    String hbDsBeanName = coreDsBeanName;// TODO mj:seperate&cfg
    Map<String, String> allDataSourceBeanNames = newHashMap(allDataSourceKeys(), (map, type) -> {
      if (StfMetaGroup.CORE.nameLowerCase().equals(type)) {
        map.put(type, coreDsBeanName);
      } else if (StfMetaGroup.DELAY.nameLowerCase().equals(type)) {
        map.put(type, delayDsBeanName);
      } else if (Heartbeat.typeNameLowerCase().equals(type)) {
        map.put(type, hbDsBeanName);
      } else {
        Asserts.argument(false, "Not supported datasource-type: '%s'", type);
      }
      return map;
    });

    StfRegistry bizReg = new StfDefaultSpringRegistry(applicationContext);
    StfJdbcOps jdbcOps = new StfDefaultSpringJdbcOps(bizReg, allDataSourceBeanNames);
    StfRunMode runMode;
    StfCore stfc = new StfCoreJdbc(jdbcOps).withRunMode(runMode = props.getRunMode());

    boolean dlqEnabled = props.getDelayQueue().isEnabled();
    ((StfDelayQueueCore)stfc).withDelayQueueEnabled(dlqEnabled);

    StfContext.init(stfc, bizReg);

    // load, sort, and validate the stf-flow configuration
    if (runMode != CLIENT) {
      String confPath = props.getConfRootPath();

      DefaultResourceLoader resLoader;
      (resLoader = new FileSystemResourceLoader()).setClassLoader(classLoader);
      Resource dirRes = resLoader.getResource(confPath);
      Asserts.state(dirRes.exists(), "The root path of stf-flow configurations must exist [resource='%s']", dirRes);
      URL dirUrl = null;
      String resProtocol = null;
      try {
        resProtocol = (dirUrl = dirRes.getURL()).getProtocol();
      } catch (IOException e) {
        Exceptions.sneakyThrow(e);
      }
      String dirName = dirRes.getFilename();
      StfConfigs cfgs = new StfConfigs();
      if ("jar".equals(resProtocol)) {
        loadFlowConfsFromJar(confPath, resLoader, dirUrl, dirName, cfgs);
      } else {
        loadFlowConfsFromFile(dirRes, cfgs);
      }
    }

    // stf start
    JobScanner scanner = JobScannerJdbc.of(jdbcOps);
    JobLoader loader = new JobLoader(scanner);
    JobRunners runners = new JobRunners(stfc);
    JobManager jobMngr = new JobManager(loader, runners).withHeartbeatHandler(HeartbeatHandlerJdbc.of(jdbcOps));

    // configure loader
    com.stun4j.stf.boot.Job.JobLoader loaderCfg;
    loader.setLoadSize((loaderCfg = props.getJob().getLoader()).getLoadSize());
    loader.setScanFreqSeconds(loaderCfg.getScanFreqSecs());

    // configure manager
    com.stun4j.stf.boot.Job.JobManager mngrCfg;
    jobMngr.setHandleBatchSize((mngrCfg = props.getJob().getManager()).getHandleBatchSize());
    jobMngr.setScanFreqSeconds(mngrCfg.getScanFreqSecs());

    // configure monitor->
    Monitor monCfg;
    jobMngr.setVmResCheckEnabled((monCfg = props.getMonitor()).isVmResCheckEnabled());
    StfMonitor.INSTANCE.withConsiderSystemLoad(monCfg.isConsiderSysLoad())
        .withConsiderJvmMemory(monCfg.isConsiderJvmMem());
    JvmMemory.INSTANCE.withHighFactor(monCfg.getJvmMem().getHighFactor())
        .withIncludeNonHeap(monCfg.getJvmMem().isIncludeNonHeap());
    JvmCpu.INSTANCE.withHighFactor(monCfg.getJvmCpu().getHighFactor());
    SystemLoad.INSTANCE.withHighFactor(monCfg.getSysLoad().getHighFactor());
    // <-
    jobMngr.start();
  }

  private void loadFlowConfsFromFile(Resource resource, StfConfigs cfgs) {
    File dir = null;
    try {
      dir = resource.getFile();
    } catch (IOException e) {
      Exceptions.sneakyThrow(e);
    }
    Asserts.state(dir.isDirectory(), "The root path of stf-flow configurations must be a directory [path='%s']",
        dir.getAbsolutePath());
    String[] fullLoadOrder = props.getConfFullLoadOrder();
    TreeMap<Integer, Object> filesOfSpecifizedLoadOrder = fullLoadOrder != null ? new TreeMap<>() : null;
    File[] files = dir.listFiles(f -> flowConfFilterAndSortFn.apply(f, filesOfSpecifizedLoadOrder));
    // post-sorting
    if (fullLoadOrder != null) {
      files = filesOfSpecifizedLoadOrder.values().stream().toArray(File[]::new);
    } else {
      Arrays.sort(files, Comparator.comparing(file -> file.getName(), String.CASE_INSENSITIVE_ORDER));
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stf-flow configurations via files: {}", Arrays.toString(files));
    }

    if (ArrayUtils.isEmpty(files)) {
      LOG.warn(
          "No stf-flow configuration was found under path '{}' > You can't benefit from Stf, even though you have Stf enabled",
          dir);
      return;
    }
    // add to stf-configs and perform final validation
    cfgs.addConfigs(classLoader, files).autoRegisterBizObjClasses((oid) -> applicationContext.getType(oid)).check();
  }

  private void loadFlowConfsFromJar(String confPath, ResourceLoader resLoader, URL dirUrl, String dirName,
      StfConfigs cfgs) {
    String confDirPath = dirUrl.getPath();
    // path of the jar where the stf-flow configurations are located
    String jarFilePath = confDirPath.substring(5, confDirPath.indexOf("!"));
    try (JarFile jar = new JarFile(URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8.name()))) {
      Enumeration<JarEntry> entries = jar.entries();
      String classpathTrait = Strings.lenientFormat("classes%s%s%s", File.separator, dirName, File.separator);
      boolean isFound = false;
      String[] fullLoadOrder = props.getConfFullLoadOrder();
      TreeMap<Integer, Object> filesOfSpecifizedLoadOrder = fullLoadOrder != null ? new TreeMap<>() : null;
      // pick only the stf-flow configurations, will skip all subsequent files once the relevant fragment
      // is found
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        int idx;
        if ((idx = name.indexOf(classpathTrait)) != -1 && //
            !name.endsWith(dirName + File.separator)) {// skip the empty dir
          isFound = true;
          String resPathRestored = name.substring(idx);
          resPathRestored = resPathRestored.substring("classes".length());
          String confFilename = resPathRestored.substring(dirName.length() + 2);
          Resource confRes = resLoader.getResource(confPath + File.separator + confFilename);
          flowConfFilterAndSortFn.apply(confRes.getURL(), filesOfSpecifizedLoadOrder);
        } else {
          if (isFound) {
            break;
          }
        }
      }
      // post-sorting
      URL[] flowConfUrls = null;
      if (fullLoadOrder != null) {
        flowConfUrls = filesOfSpecifizedLoadOrder.values().stream().map(o -> (URL)o).toArray(URL[]::new);
      } else {
        // sort the file in the same way as ordinary files
        Arrays.sort(flowConfUrls,
            Comparator.comparing(url -> StfConfig.determineFilename(url.getFile()), String.CASE_INSENSITIVE_ORDER));
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Loading stf-flow configurations via urls: {}", Arrays.toString(flowConfUrls));
      }

      if (ArrayUtils.isEmpty(flowConfUrls)) {
        LOG.warn(
            "No stf-flow configuration was found under path '{}' > You can't benefit from Stf, even though you have Stf enabled",
            confDirPath);
        return;
      }
      // add to stf-configs and perform final validation
      cfgs.addConfigs(classLoader, flowConfUrls).autoRegisterBizObjClasses((oid) -> applicationContext.getType(oid))
          .check();
    } catch (IOException e) {
      Exceptions.sneakyThrow(e);
    }
  }

  StfAutoConfigure(StfProperties props) {
    this.props = props;

    if (props.getRunMode() == CLIENT) {
      return;
    }
    // remove the useless suffix
    String[] fullLoadOrder = props.getConfFullLoadOrder();
    if (fullLoadOrder != null) {// has side effect,but doesn't matter
      for (int i = 0; i < fullLoadOrder.length; i++) {
        if (fullLoadOrder[i].endsWith(DFT_CONF_SUFFIX)) {
          fullLoadOrder[i] = fullLoadOrder[i].substring(0, fullLoadOrder[i].indexOf(DFT_CONF_SUFFIX));
        }
      }
    }

    // core function definition for configuring filtering and sorting for stf-flow
    this.flowConfFilterAndSortFn = (res, filesOfSpecifizedLoadOrder) -> {
      String name = null;
      if (res instanceof File) {
        name = ((File)res).getName();
      } else if (res instanceof URL) {
        name = StfConfig.determineFilename(((URL)res).getFile());
      }
      if (!name.endsWith(DFT_CONF_SUFFIX)) {
        return false;
      }
      String[] excludes;
      if ((excludes = props.getConfExcludeFilenames()) != null) {
        for (String exclude : excludes) {
          if (name.contains(exclude)) {
            return false;
          }
        }
      }
      if (fullLoadOrder != null) {
        int sortOrder = ArrayUtils.indexOf(fullLoadOrder, name.substring(0, name.indexOf(DFT_CONF_SUFFIX)));
        if (sortOrder == -1) {
          return false;
        }
        filesOfSpecifizedLoadOrder.put(sortOrder, res);
      }
      return true;
    };
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
    applicationContext.getBean(GuidAutoConfigure.class);// The stun4j-guid module must be initialized first

    if (props.getDelayQueue().isEnabled()) {
      new DelayQueueDataSourceBeanRegister(environment, applicationContext, props).tryRegister();
    }

    doEarlyInitialize();
  }

}
