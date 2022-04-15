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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.stun4j.stf.core.support.BaseEntity;
import com.stun4j.stf.core.support.BaseVo;

/**
 * The abstraction of Stf's action chain
 * @author Jay Meng
 */
public class Chain<ID> extends BaseVo {
  private Map<ID, Node<ID>> nodes = new HashMap<ID, Node<ID>>();
  private Map<ID, Node<ID>> orphanNodes = new HashMap<ID, Node<ID>>();

  public Chain<ID> addOrphan(ID nodeId) {
    addOrGet(nodeId);
    return this;
  }

  public Chain<ID> addForward(ID fromNodeId, ID toNodeId) {
    Node<ID> fromNode = addOrGet(fromNodeId);
    Node<ID> toNode = addOrGet(toNodeId);

    addEdge(fromNode, toNode);

    this.orphanNodes.remove(fromNode.getId());
    this.orphanNodes.remove(toNode.getId());
    return this;
  }

  private void addEdge(Node<ID> fromNodeId, Node<ID> toNodeId) {
    fromNodeId.addOutgoingNode(toNodeId);
    toNodeId.addIncomingNode(fromNodeId);
  }

  private Node<ID> addOrGet(ID nodeId) {
    return this.nodes.computeIfAbsent(nodeId, k -> {
      Node<ID> node = new Node<>(nodeId);
      this.orphanNodes.put(nodeId, node);
      return node;
    });
  }

  public int size() {
    return this.nodes.size();
  }

  public Collection<Node<ID>> getAllNodes() {
    return this.nodes.values();
  }

  public Node<ID> get(ID id) {
    return this.nodes.get(id);
  }

  public Collection<Node<ID>> getOrphanNodes() {
    return this.orphanNodes.values();
  }

  /**
   * A node representation in this chain, each node may have set of incoming-nodes and outgoing-nodes, a node is
   * represented by an unique id
   * @author Jay Meng
   */
  static class Node<ID> extends BaseEntity<ID> {
    private ID id;
    private Set<Node<ID>> incomingNodes = new LinkedHashSet<Node<ID>>();
    private Set<Node<ID>> outgoingNodes = new LinkedHashSet<Node<ID>>();

    public void addIncomingNode(Node<ID> node) {
      this.incomingNodes.add(node);
    }

    public void addOutgoingNode(Node<ID> node) {
      this.outgoingNodes.add(node);
    }

    @Override
    public String toString() {
      return "Node [id=" + id + "]";
    }

    Node(ID id) {
      this.id = id;
    }

    public Set<Node<ID>> getIncomingNodes() {
      return this.incomingNodes;
    }

    public Set<Node<ID>> getOutgoingNodes() {
      return this.outgoingNodes;
    }

    public ID getId() {
      return this.id;
    }
  }
}
