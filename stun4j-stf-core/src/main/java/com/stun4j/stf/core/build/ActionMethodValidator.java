package com.stun4j.stf.core.build;

import static com.google.common.base.Strings.lenientFormat;
import static com.stun4j.stf.core.build.BuildingBlockEnum.ARGS;
import static com.stun4j.stf.core.build.StfConfigs.ACTION_FULL_PATH_SEPARATOR;
import static com.stun4j.stf.core.build.StfConfigs.FULL_PATH_ACTIONS;
import static com.stun4j.stf.core.build.StfConfigs.actionPathBy;
import static com.stun4j.stf.core.utils.Asserts.notNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.ReflectionUtils;

import com.stun4j.stf.core.StfContext;
import com.stun4j.stf.core.utils.CollectionUtils;

/**
 * Do strongly typed stf-action-method checks
 * @author Jay Meng
 */
public class ActionMethodValidator implements Validator<String> {

  @Override
  public void validate(Chain<String> chain) throws RuntimeException {
    // forking check
    chain.getAllNodes().stream().filter(n -> n.getOutgoingNodes().size() > 1).findAny().ifPresent(n -> {
      throw new RuntimeException(lenientFormat(
          "Action#%s forking is not supported > In stf,one caller cannot have different callees", n.getId()));
    });

    // method exist check
    chain.getAllNodes().forEach(n -> {
      String[] tmp = n.getId().split(ACTION_FULL_PATH_SEPARATOR);
      String actionOid = tmp[0];
      String actionMethodName = tmp[1];
      Class<?> bizObjClz = StfContext.getBizObjClass(actionOid);
      notNull(bizObjClz,
          "The bizObjClass corresponding to the actionOid '%s' can't be null > An action method can only be invoked if it exists, forgot to register bizObjClass with the oid '%s'?",
          actionOid, actionOid);
      // <-

      String actionPath = actionPathBy(actionOid, actionMethodName);
      Map<String, Object> actionDetail = FULL_PATH_ACTIONS.get(actionPath);
      @SuppressWarnings("unchecked")
      List<Function<Object, Pair<?, Class<?>>>> argPairs = (List<Function<Object, Pair<?, Class<?>>>>)actionDetail
          .get(ARGS.key());
      if (CollectionUtils.isEmpty(argPairs)) {
        Method matchedMethod = MethodUtils.getAccessibleMethod(bizObjClz, actionMethodName);
        notNull(matchedMethod, () -> lenientFormat(
            "No matched action method was found, please check your biz-flow configuration [method-signature=%s#%s()]",
            bizObjClz, actionMethodName));
        return;
      }

      // search from back(special 'arg$' expression on java.util.function.Function object)
      Class<?>[] actionMethodArgClzs = argPairs.stream().map(argPair -> {
        Field fld = Optional.ofNullable(ReflectionUtils.findField(argPair.getClass(), "arg$2"))
            .orElse(ReflectionUtils.findField(argPair.getClass(), "arg$1"));
        fld.setAccessible(true);
        Class<?> clz = (Class<?>)ReflectionUtils.getField(fld, argPair);
        return clz;
      }).toArray(Class[]::new);

      /*
       * #getAccessibleMethod strictly distinguishes types (able to identify wrapped/unwrapped types), but
       * #getMatchingAccessibleMethod not
       */
      Method matchedMethod = MethodUtils.getAccessibleMethod(bizObjClz, actionMethodName, actionMethodArgClzs);
      notNull(matchedMethod,
          () -> lenientFormat(
              "No matched action method was found, please check your biz-flow configuration [method-signature=%s#%s%s]",
              bizObjClz, actionMethodName,
              Arrays.toString(actionMethodArgClzs).replaceFirst("\\[", "(").replaceFirst("\\]", ")")));
    });
  }
}
