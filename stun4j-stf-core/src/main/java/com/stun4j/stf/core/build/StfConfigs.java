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
package com.stun4j.stf.core.build;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.StfConsts.DFT_JOB_TIMEOUT_SECONDS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.ARGS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.OID;
import static com.stun4j.stf.core.build.BuildingBlockEnum.TIMEOUT;
import static com.stun4j.stf.core.utils.Asserts.argument;
import static com.stun4j.stf.core.utils.Asserts.requireNonNull;
import static com.stun4j.stf.core.utils.Asserts.state;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.build.Chain.Node;
import com.stun4j.stf.core.utils.CollectionUtils;

/**
 * The aggregation of multiple stf-configs
 * <ul>
 * <li>This will be the core stf-actions register, which will guide all the action-forwards of user (biz-)system</li>
 * <li>More strong type checkings are performed on stf-action-methods,which will ensure their existence</li>
 * <li>In Stf,the 'action-forking' is not supported</li>
 * <ul>
 * @author Jay Meng
 */
public class StfConfigs {
  private static final Chain<String> CHAIN;
  static final Map<String/* path */, Map<String, Object>/* oid && args etc. */> FULL_PATH_ACTIONS;
  static final String ACTION_FULL_PATH_SEPARATOR = "#";// '#' make split easy works,instead of '.'

  public static boolean existForwardsFromOf(String actionOid, String actionMethodName) {
    String actionPath = actionPathBy(actionOid, actionMethodName);
    Node<String> actionNode = CHAIN.get(actionPath);
    if (actionNode == null) {
      return false;
    }
    return !CollectionUtils.isEmpty(actionNode.getIncomingNodes());
  }

  public static Pair<String/* oid */, String/* forward */> determineForwardToOf(String actionOid,
      String actionMethodName) {
    String fromActionPath = actionPathBy(actionOid, actionMethodName);
    Node<String> fromNode = CHAIN.get(fromActionPath);
    if (fromNode == null) {
      return null;
    }
    Iterator<Node<String>> iter = fromNode.getOutgoingNodes().iterator();
    if (!iter.hasNext()) {
      return null;
    }
    Node<String> toNode = iter.next();
    state(!iter.hasNext(), "Action forking is not supported");// this shouldn't happen
    if (toNode == null) {
      // FIXME mj:pay attention to orphan node whose next should be itself, and also be aware of its
      // 'retry-times',currently,its not tested,so we don't know what's the behavior of an orphan node
      return null;
    }
    String[] tmp;
    return Pair.of((tmp = toNode.getId().split(ACTION_FULL_PATH_SEPARATOR))[0], tmp[1]);
  }

  @SuppressWarnings("unchecked")
  public static Pair<?, Class<?>>[] determineActionMethodArgs(String actionOid, String actionMethodName, Object out) {
    Map<String, Object> actionDetail = getActionDetail(actionOid, actionMethodName);
    List<Function<Object, Pair<?, Class<?>>>> argsInfo = (List<Function<Object, Pair<?, Class<?>>>>)actionDetail
        .get(ARGS.key());
    // support no-arg method invoke
    if (CollectionUtils.isEmpty(argsInfo)) {
      return null;
    }

    Pair<?, Class<?>>[] args = argsInfo.stream().map(f -> {
      Pair<?, Class<?>> arg = f.apply(out);
      return arg;
    }).toArray(Pair[]::new);
    return args;
  }

  public static int getActionTimeout(String actionOid, String actionMethodName) {
    Integer timeoutSecs = null;
    Map<String, Object> actionDetail = getActionDetail(actionOid, actionMethodName);
    if (actionDetail != null) {
      timeoutSecs = (Integer)actionDetail.get(TIMEOUT.key());
    }

    timeoutSecs = Optional.ofNullable(timeoutSecs).orElse(DFT_JOB_TIMEOUT_SECONDS);
    return timeoutSecs;
  }

  public StfConfigs addConfigs(File... cfgFileBasenames) {
    for (File cfgFile : cfgFileBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(ClassLoader classLoader, File... cfgFileBasenames) {
    for (File cfgFile : cfgFileBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile, classLoader);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(String... cfgFileResourceBasenames) {
    for (String cfgFile : cfgFileResourceBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(ClassLoader classLoader, String... cfgFileResourceBasenames) {
    for (String cfgFile : cfgFileResourceBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile, classLoader);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(URL... cfgFileUrlBasenames) {
    for (URL cfgFile : cfgFileUrlBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(ClassLoader classLoader, URL... cfgFileUrlBasenames) {
    for (URL cfgFile : cfgFileUrlBasenames) {
      StfConfig cfg = StfConfig.load(cfgFile, classLoader);
      doMerge(cfg);
    }
    return this;
  }

  public StfConfigs addConfigs(StfConfig... cfgs) {
    for (StfConfig cfg : cfgs) {
      doMerge(cfg);
    }
    return this;
  }

  // This is especially friendly for application using 'Spring Framework'-like
  public StfConfigs autoRegisterBizObjClasses(Function<String, Class<?>> clzProvider) {
    FULL_PATH_ACTIONS.keySet().forEach(path -> {
      String oid = path.substring(0, path.indexOf(ACTION_FULL_PATH_SEPARATOR));
      StfContext.putBizObjClass(oid, () -> clzProvider.apply(oid));
    });
    return this;
  }

  @SuppressWarnings("unchecked")
  public StfConfigs autoRegisterBizObj(Pair<String/* oid */, Object/* the obj */>... objEntry)
      throws IllegalArgumentException {
    Map<String, Object> map = new HashMap<>();
    for (Pair<String, Object> pair : objEntry) {
      map.put(pair.getKey(), pair.getValue());
    }
    doAutoRegister(map);
    return this;
  }

  public StfConfigs registerBizObjs(Object... objIdAndObjOneByOne) throws IllegalArgumentException {
    Map<String, Object> map = new HashMap<>();
    String oid = null;
    for (int i = 0; i < objIdAndObjOneByOne.length; i++) {
      if (i % 2 == 0) {
        argument(objIdAndObjOneByOne[i] instanceof String,
            lenientFormat("Must be a string representing the id of an object [arg-pos=%s]", i));
        argument(ArrayUtils.isArrayIndexValid(objIdAndObjOneByOne, i + 1),
            "It must conform to the format 'an object-id followed by its object-instance'");
        oid = (String)objIdAndObjOneByOne[i];
        continue;
      }
      map.put(oid, requireNonNull(objIdAndObjOneByOne[i],
          lenientFormat("The object-instance can't be null [object-id='%s']", oid)));
    }
    doAutoRegister(map);
    return this;
  }

  private static Map<String, Object> getActionDetail(String actionOid, String actionMethodName) {
    String actionPath = actionPathBy(actionOid, actionMethodName);
    Map<String, Object> actionDetail = FULL_PATH_ACTIONS.get(actionPath);
    return actionDetail;
  }

  private void doAutoRegister(Map<String, Object> map) {
    FULL_PATH_ACTIONS.keySet().forEach(path -> {
      String oid = path.substring(0, path.indexOf(ACTION_FULL_PATH_SEPARATOR));
      StfContext.putBizObj(oid,
          requireNonNull(map.get(oid), lenientFormat("Missing object-instance with object-id '%s'", oid)));
    });
  }

  public void check() {
    new CycleChecker<String>().check(CHAIN);
    new ActionMethodChecker().check(CHAIN);
  }

  private static void doMerge(StfConfig cfg) {
    cfg.getForwards().entrySet().forEach(forward -> {
      String fromAction = forward.getKey();
      Map<String, Object> fromActionDetail;
      String fromActionOid = (String)(fromActionDetail = cfg.getActions().get(fromAction)).get(OID.key());
      String fromActionPath = actionPathBy(fromActionOid, fromAction);
      doMerge(fromActionPath, fromActionDetail);

      String toAction = forward.getValue();
      Map<String, Object> toActionDetail;
      String toActionOid = (String)(toActionDetail = cfg.getActions().get(toAction)).get(OID.key());
      String toActionPath = actionPathBy(toActionOid, toAction);
      doMerge(toActionPath, toActionDetail);
      CHAIN.addForward(fromActionPath, toActionPath);
    });
  }

  static String actionPathBy(String actionOid, String actionMethodName) {
    return actionOid + ACTION_FULL_PATH_SEPARATOR + actionMethodName;// the full path
  }

  private static void doMerge(String actionPath, Map<String, Object> childActionDetail) {
    FULL_PATH_ACTIONS.merge(actionPath, childActionDetail, (k, parentActionDetail) -> {
      // FIXME mj:this overrider may not correct in no-arg scenario
      if (childActionDetail != null) {
        return childActionDetail;
      }
      return parentActionDetail;
    });
  }

  static {
    CHAIN = new Chain<>();
    FULL_PATH_ACTIONS = new HashMap<>();
  }

}
