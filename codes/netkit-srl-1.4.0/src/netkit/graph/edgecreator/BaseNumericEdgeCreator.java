/**
 * BaseNumericEdgeCreator.java
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This class creates edges based on numeric distribution of the graph as
 * a whole.  It will select the K nearest neighbors to a given node, based
 * on the node's numeric value 
 */
package netkit.graph.edgecreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import netkit.graph.Attribute;
import netkit.graph.Edge;
import netkit.graph.Node;
import netkit.graph.Type;

public class BaseNumericEdgeCreator extends EdgeCreatorImp {
  protected NbrEntry[] nodes = null; 
  protected Map<Node,Integer> nat = new HashMap<Node,Integer>(); // lookup map to find where a node is in the list

  @Override
  public String getName() {
    return "baseNumericEdgeCreator";
  }
  
  protected double getValue(Node n) {
    return n.getValue(attributeIndex);
  }
  
  protected double getWeightFast(Node src, Node dest) {
    double w1 = getValue(src);
    double w2 = getValue(dest);
    double diff = Math.abs(w1-w2);
    return 1.0D/(1.0D + diff);
  }
  
  @Override
  public double getWeight(Node src, Node dest) {
    if(attributeIndex == -1)
      throw new IllegalStateException("EdgeCreator["+getName()+"] has not yet been initialized!");
    
    if(src.isMissing(attributeIndex) || dest.isMissing(attributeIndex))
      return Double.NaN;
    
    return getWeightFast(src,dest);
  }

  @Override
  public boolean canHandle(Attribute attribute) {
    return (attribute.getType() != Type.CATEGORICAL);
  }
  
  protected void buildNodeArray()  {
    if(nodes!=null)
      return;
    List<NbrEntry> ne = new ArrayList<NbrEntry>();
    for(Node n : graph.getNodes(nodeType)) {
      if(n.isMissing(attributeIndex))
        continue;
      double v = n.getValue(attributeIndex);
      ne.add(new NbrEntry(null,n,v));
    }
    Collections.sort(ne,Collections.reverseOrder()); // sort in increasing value
    nodes = ne.toArray(new NbrEntry[0]);
    for(int i=0;i<nodes.length;i++)
      nat.put(nodes[i].dest,i);
  }
  
  @Override
  /**
   * Get edges to all nodes who share the same attribute value as this node
   * (no cutoff is done as we don't know where to cut off, other than sampling
   * perhaps).  If this is a by-attribute-value edge creator, then the incoming
   * node must have the correct value of its attribute.  
   * 
   * @param node The node 
   * @return
   */
  public Edge[] getEdgesToNearestNeighbors(final Node node) {
    if(graph == null)
      throw new IllegalArgumentException("EdgeCreator has not yet been initialized!");
    double v = node.getValue(attributeIndex);
    if(Double.isNaN(v))
      return new Edge[0];
    
    buildNodeArray();
    
    Integer idx = nat.get(node);
    if(idx==null)
      return new Edge[0];
    
    // now, let's find maxEdges nbrs
    Edge[] edges = null;
    int e = 0;
    if(maxEdges == -1 || nodes.length < maxEdges-1) {
      edges = new Edge[nodes.length-1];
      for(int i=0;i<nodes.length;i++)
        if(i!=idx)
          edges[e++] = nodes[i].asEdge(node);
    } else {
      int l = idx-1;
      int r = idx+1;
      edges = new Edge[maxEdges];
      if(l<0 || (r<nodes.length && nodes[r].score > nodes[l].score)) {
        edges[e++] = nodes[r].asEdge(node);
        r++;
      } else {
        edges[e++] = nodes[l].asEdge(node);
        l--;
      }
    }
    
    return edges;
  }
  
  
  @Override
  protected void buildEdges() {
    if(edges != null)
      return;
    if(graph == null)
      throw new IllegalArgumentException("EdgeCreator has not yet been initialized!");
    buildNodeArray();

    int l=0;
    int r=((maxEdges==-1) ? nodes.length-1 : maxEdges+1);
    if(r>=nodes.length) r = nodes.length-1;

    int[] right = new int[nodes.length];
    Arrays.fill(right,-1);
    List<Edge> edgelist = new ArrayList<Edge>();
    for(int i=0;i<nodes.length;i++) {
      Node src = nodes[i].dest;
      for(int j=l;j<r;j++) {
        if(i==j || j < right[j])
          continue;
        Node dest = nodes[j].dest;
        double weight = getWeightFast(src, dest);
        // we create edges both ways
        if(weight>0) {
          edgelist.add(new Edge(edgetype, src, dest, weight));
          edgelist.add(new Edge(edgetype, dest, src, weight));
        }
      }
      // we keep track of the rightmost node for prior nodes to make sure we
      // do not create edges twice
      right[i] = r;
      if(r+1 < nodes.length) {
        double w1 = this.getWeightFast(nodes[i].dest, nodes[l].dest);
        double w2 = this.getWeightFast(nodes[i].dest, nodes[r+1].dest);
        if(w2>w1) {
          l++;
          r++;
        }
      }
    }
    edges = edgelist.toArray(new Edge[0]);
  }
}
