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
import static com.stun4j.stf.core.utils.Asserts.argument;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.stun4j.stf.core.build.Chain.Node;
import com.stun4j.stf.core.support.NullValue;

/**
 * Do action-chain cycle check
 * @author Jay Meng
 */
public class CycleChecker<ID> implements Checker<ID> {
  private Map<Node<ID>, Object> visitedNodes = new HashMap<>();
  private Map<Node<ID>, Object> onPathNodes = new HashMap<>();

  @Override
  public void check(Chain<ID> chain) {
    doProcess(chain.getAllNodes());
  }

  private void doProcess(Collection<Node<ID>> nodes) {
    argument(nodes != null && !nodes.isEmpty(), "Nodes can't be empty");
    for (Node<ID> node : nodes) {
      detectCycle(node);
    }
  }

  private void detectCycle(Node<ID> node) {
    visitedNodes.put(node, NullValue.INSTANCE);
    onPathNodes.put(node, NullValue.INSTANCE);
    doDepthFirstTraversal(node);
    onPathNodes.remove(node);
  }

  private void doDepthFirstTraversal(Node<ID> node) {
    for (Node<ID> outNode : node.getOutgoingNodes()) {
      if (!isAlreadyVisited(outNode)) {
        detectCycle(outNode);
      } else if (isOnPath(outNode)) {
        throw new IllegalArgumentException(lenientFormat("Cycle detected %s with %s", node, outNode));
      }
    }
  }

  private boolean isAlreadyVisited(Node<ID> node) {
    return this.visitedNodes.containsKey(node);
  }

  private boolean isOnPath(Node<ID> node) {
    return onPathNodes.containsKey(node);
  }

}
