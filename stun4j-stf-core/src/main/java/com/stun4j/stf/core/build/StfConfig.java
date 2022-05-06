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
import static com.stun4j.stf.core.build.BuildingBlockEnum.ACTS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.ARGS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.CLZ;
import static com.stun4j.stf.core.build.BuildingBlockEnum.FWDS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.GLB;
import static com.stun4j.stf.core.build.BuildingBlockEnum.LVS;
import static com.stun4j.stf.core.build.BuildingBlockEnum.M;
import static com.stun4j.stf.core.build.BuildingBlockEnum.OID;
import static com.stun4j.stf.core.build.BuildingBlockEnum.O_IN;
import static com.stun4j.stf.core.build.BuildingBlockEnum.ROOT;
import static com.stun4j.stf.core.build.BuildingBlockEnum.TIMEOUT;
import static com.stun4j.stf.core.build.BuildingBlockEnum.TO;
import static com.stun4j.stf.core.build.BuildingBlockEnum.U_IN;
import static com.stun4j.stf.core.utils.Asserts.argument;
import static com.stun4j.stf.core.utils.Asserts.notNull;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.stun4j.stf.core.utils.Exceptions;
import com.stun4j.stf.core.utils.shaded.guava.common.primitives.Primitives;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;

/**
 * The parser of single stf-flow configuration
 * <p>
 * Strong type checking is performed and the stf-config object is obtained for subsequent processing.
 * The stf-config object contains valid stf-actions and stf-action-forwards
 * @author Jay Meng
 */
public class StfConfig {
  static final Map<String, Class<?>> SUPPORTED_PRIMITIVE_KEYWORDS;
  private final Map<String/* current */, String/* next-to */> FORWARDS = new HashMap<>();
  private final Map<String/* method-name */, Map<String, Object>/* the args */> ACTIONS = new HashMap<>();
  private final Map<String/* global-key */, Object/* global-value */> GLOBAL = new HashMap<>();
  static {
    String prefix = "java.lang.";
    String[] keywords = new String[]{//
        "Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short", "String"};
    SUPPORTED_PRIMITIVE_KEYWORDS = new HashMap<>();
    try {
      for (String keyword : keywords) {
        Class<?> wrappedClz;
        SUPPORTED_PRIMITIVE_KEYWORDS.put(keyword, wrappedClz = Class.forName(prefix + keyword));
        if (!"String".equals(keyword)) {
          SUPPORTED_PRIMITIVE_KEYWORDS.put(keyword.toLowerCase(), Primitives.unwrap(wrappedClz));
        }
      }
    } catch (Throwable e) {
      Exceptions.sneakyThrow(e);
    }
  }

  public static StfConfig load(URL stfFlowFileUrlBasename) {
    return new StfConfig(ConfigFactory.parseURL(stfFlowFileUrlBasename));
  }

  public static StfConfig load(URL stfFlowFileUrlBasename, ClassLoader classLoader) {
    return new StfConfig(
        ConfigFactory.parseURL(stfFlowFileUrlBasename, ConfigParseOptions.defaults().setClassLoader(classLoader)),
        classLoader);
  }

  public static StfConfig load(String stfFlowFileResourceBasename) {
    return new StfConfig(ConfigFactory.parseResources(stfFlowFileResourceBasename));
  }

  public static StfConfig load(String stfFlowFileResourceBasename, ClassLoader classLoader) {
    return new StfConfig(ConfigFactory.parseResources(stfFlowFileResourceBasename,
        ConfigParseOptions.defaults().setClassLoader(classLoader)), classLoader);
  }

  public static StfConfig load(File stfFlowFileBasename) {
    return new StfConfig(ConfigFactory.parseFile(stfFlowFileBasename));
  }

  public static StfConfig load(File stfFlowFileBasename, ClassLoader classLoader) {
    return new StfConfig(
        ConfigFactory.parseFile(stfFlowFileBasename, ConfigParseOptions.defaults().setClassLoader(classLoader)),
        classLoader);
  }

  private StfConfig(Config cfg) {
    this(cfg, ConfigParseOptions.defaults().getClassLoader());
  }

  private StfConfig(Config cfgRaw, ClassLoader classLoader) {
    String rootPath;
    String rootPathPre;
    String parsingKey;
    Config cfg;
    cfgRaw = cfgRaw.withOnlyPath(rootPath = ROOT.key());
    if (cfgRaw.hasPath((rootPathPre = rootPath + ".") + (parsingKey = LVS.key()))) {
      Config vars = cfgRaw.getConfig(rootPathPre + parsingKey);
      if (!vars.isEmpty()) {
        for (Entry<String, ConfigValue> e : vars.entrySet()) {
          String key = e.getKey();
          ConfigValue value = e.getValue();
          cfgRaw = cfgRaw.withValue(key, value);
        }
        cfg = cfgRaw.resolve();
      } else {
        cfg = cfgRaw;
      }
    } else {
      cfg = cfgRaw;
    }
    String fileName;
    notNull(fileName = Optional.ofNullable(cfg.origin().filename()).orElse(cfg.origin().url().getFile()),
        () -> cfg.origin().description());
    argument(cfg.root().containsKey(rootPath), "Illegal root-element (use '%s{' to start?)", rootPath);
    // parse global
    // try parse global oid base on file->
    String tmp;
    if ((tmp = cfg.origin().resource()) == null) {
      fileName = determineFilename(fileName);
    } else {
      fileName = determineFilename(tmp);
    }
    int dashIdx = fileName.indexOf("-");
    if (dashIdx != -1) {
      GLOBAL.put(OID.key(), fileName.substring(0, dashIdx));
    }
    // <-
    if (cfg.hasPath(rootPathPre + (parsingKey = GLB.key()))) {
      Config global = cfg.getConfig(rootPathPre + parsingKey);
      global.entrySet().forEach(g -> {
        GLOBAL.put(g.getKey(), g.getValue().unwrapped());
      });
    }

    // parse actions
    if (cfg.hasPath(rootPathPre + (parsingKey = ACTS.key()))) {
      Config actions = cfg.getConfig(rootPathPre + parsingKey);
      actions.entrySet().forEach(act -> {
        String actionInfo = act.getKey();
        String action = actionInfo.substring(0, actionInfo.indexOf("."));
        String parsingActElmtKey;
        // parse action-oid
        if (actionInfo.indexOf(action + "." + (parsingActElmtKey = OID.key())) != -1) {
          String objId = act.getValue().unwrapped().toString();
          ACTIONS.computeIfAbsent(action, k -> new HashMap<>()).put(parsingActElmtKey, objId);
        }
        // parse action-args
        if (actionInfo.indexOf(action + "." + (parsingActElmtKey = ARGS.key())) != -1) {
          @SuppressWarnings("unchecked")
          List<Map<String, Map<String, String>>> args = (List<Map<String, Map<String, String>>>)act.getValue()
              .unwrapped();
          List<Function<?, Pair<?, Class<?>>>> argPairs = args.stream().map(arg -> {
            Map<String, String> argInfo;
            // arg-type:'invoke-on-in'
            if ((argInfo = arg.get(O_IN.key())) != null) {
              String clzStr = argInfo.get(CLZ.key());
              String method = argInfo.get(M.key());
              try {
                Class<?> clz = determineClass(clzStr, classLoader);
                Function<?, Pair<?, Class<?>>> argPair = out -> {
                  try {
                    return Pair.of(MethodUtils.invokeExactMethod(out, method), clz);
                  } catch (Throwable e) {
                    Exceptions.sneakyThrow(e);
                  }
                  return null;
                };
                return argPair;
              } catch (Throwable e1) {
                Exceptions.sneakyThrow(e1);
              }
            } else if ((argInfo = arg.get(U_IN.key())) != null) {
              String clzStr = argInfo.get(CLZ.key());
              try {
                Class<?> clz = determineClass(clzStr, classLoader);
                Function<?, Pair<?, Class<?>>> argPair = out -> {
                  try {
                    return Pair.of(out, clz);
                  } catch (Throwable e) {
                    Exceptions.sneakyThrow(e);
                  }
                  return null;
                };
                return argPair;
              } catch (Throwable e1) {
                Exceptions.sneakyThrow(e1);
              }
            } // TODO mj:else,to raise unsupported-ops error
            return null;
          }).collect(Collectors.toList());
          ACTIONS.computeIfAbsent(action, k -> new HashMap<>()).put(parsingActElmtKey, argPairs);
        }
        // parse action-timeout
        if (actionInfo.indexOf(action + "." + (parsingActElmtKey = TIMEOUT.key())) != -1) {
          String timeoutStr = act.getValue().unwrapped().toString();
          int idx;
          String msg;
          argument((idx = timeoutStr.lastIndexOf("s")) != -1,
              msg = ("The 'timeout' of action[%s] can only be set to seconds, the wrong value is '%s'"), action,
              timeoutStr);
          Integer timeoutSeconds;
          try {
            timeoutSeconds = Integer.valueOf(timeoutStr.substring(0, idx));
            argument(timeoutStr.substring(idx + 1).length() == 0, msg);
            argument(timeoutSeconds > 0,
                msg = "The timeout-seconds of action[%s] must be greater than 0, the wrong value is '%s'", action,
                timeoutStr);
            argument(timeoutSeconds <= 8388607,
                msg = "The timeout-seconds of action[%s] must be less than or equal to 8388607, the wrong value is '%s'", action,
                timeoutStr);
          } catch (Exception e) {
            throw new RuntimeException(lenientFormat(msg, action, timeoutStr), e);
          }
          ACTIONS.computeIfAbsent(action, k -> new HashMap<>()).put(parsingActElmtKey, timeoutSeconds);
        }
      });
    }
    // each action must have an 'oid'
    String globalOid = (String)GLOBAL.get(OID.key());
    ACTIONS.entrySet().forEach(action -> {
      Object specifiedOid;
      Object oid = Optional.ofNullable(specifiedOid = action.getValue().get(OID.key())).orElse(globalOid);
      notNull(oid,
          "The action[%s] must have an 'oid' > 'oid', aka '(biz-)obj-id' and the 'biz-obj' is something like 'spring-bean'",
          action.getKey());
      // make 'global-override' happen
      if (specifiedOid == null) {
        ACTIONS.get(action.getKey()).put(OID.key(), oid);
      }
    });

    // parse forwards
    if (cfg.hasPath(rootPathPre + (parsingKey = FWDS.key()))) {
      Config fowards = cfg.getConfig(rootPathPre + parsingKey);
      fowards.entrySet().forEach(forward -> {
        String actionInfo = forward.getKey();
        int idx;
        if ((idx = actionInfo.indexOf("." + TO.key())) != -1) {
          String action = actionInfo.substring(0, idx);
          // try auto-fill current action with oid,if not existed
          ACTIONS.computeIfAbsent(action, k -> new HashMap<>()).putIfAbsent(OID.key(), globalOid);

          String actionForwadTo = (String)forward.getValue().unwrapped();
          // try auto-fill current action-forward-to with oid,if not existed
          ACTIONS.computeIfAbsent(actionForwadTo, k -> new HashMap<>()).putIfAbsent(OID.key(), globalOid);

          FORWARDS.put(action, actionForwadTo);
        }
      });
    }
  }

  Map<String/* current */, String/* next-to */> getForwards() {
    return Collections.unmodifiableMap(FORWARDS);
  }

  Map<String/* method-name */, Map<String, Object>/* the args */> getActions() {
    return Collections.unmodifiableMap(ACTIONS);
  }

  public static String determineFilename(String fileName) {
    int tmpIdx;
    return (tmpIdx = fileName.lastIndexOf(File.separator)) != -1 ? fileName.substring(tmpIdx + 1) : fileName;
  }

  private static Class<?> determineClass(String clzStr, ClassLoader classLoader) throws Exception {
    Class<?> clz = SUPPORTED_PRIMITIVE_KEYWORDS.get(clzStr);
    if (clz != null) {
      return clz;
    }
    return Class.forName(clzStr, false, classLoader);
  }
}
