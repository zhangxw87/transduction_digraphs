/**
 * GraphView.java
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
 */
package netkit.util;

import netkit.EdgeTransformer;
import netkit.graph.*;
import netkit.graph.edgecreator.*;

import java.util.*;
import java.util.logging.Logger;

public final class GraphView {
  private Logger logger = NetKitEnv.getLogger(this);

  private final Graph graph;
  private Map<EdgeType,double[]> edgeWeights = new HashMap<EdgeType,double[]>();
  //private Map<String,Node[]> nodes = null;
  private List<Edge> newEdges = new ArrayList<Edge>();
  private List<EdgeType> newEdgeTypes = new ArrayList<EdgeType>();
  
  public GraphView(final Graph graph) {
    this.graph = graph;
    for(EdgeType et : graph.getEdgeTypes())
      getOrigEdges(et);
  }
  
  private void getOrigEdges(EdgeType et) {
    if(edgeWeights.containsKey(et))
        return;

    final Edge[] edges = graph.getEdges(et);
    final double[] weights = new double[edges.length];
    for(int i=0;i<edges.length;i++)
      weights[i] = edges[i].getWeight();
    edgeWeights.put(et,weights);  
  }
 
  public void reset() {
    removeNewEdges();
    resetWeights();
  }
  
  public Graph getGraph() {
    return graph;
  }
  
  public void resetWeights() {
    for(EdgeType et: graph.getEdgeTypes())
    {
      Edge[] edges = graph.getEdges(et);
      double[] weights = edgeWeights.get(et);
      
      // If weights are null, then they are 'new' and we won't reset them
      if(weights == null)
        continue; 

      for(int i=0;i<edges.length;i++)
        edges[i].setWeight(weights[i]);
    }   
  }
  
  public void removeNewEdges() {
    for(Edge e : newEdges) {
      try {
      graph.removeEdge(e.getEdgeType().getName(), e.getSource(), e.getDest());
      } catch(Exception ex) {} // in case we try to remove the same edge twice... could happen
    }
    for(EdgeType et : newEdgeTypes)
    {
      graph.removeEdgeType(et.getName(), true);
      edgeWeights.remove(et);
    }
    newEdges.clear();
    newEdgeTypes.clear();
  }

  /**
   * Assumes EdgeType is same for all edges.  This is not checked.
   * @param edges
   */
  public void addEdges(Edge[] edges) {
    if(edges == null || edges.length == 0)
      return;
    
    final EdgeType et = edges[0].getEdgeType();
    boolean newET = false;
    if(graph.getEdgeType(et.getName()) == null)
    {
      graph.addEdgeType(et);
      newEdgeTypes.add(et);
      newET = true;
    }
    for(Edge e : edges)
    {
      graph.addEdge(et, e.getSource(), e.getDest(), e.getWeight()); 
      if(!newET && e.getSource().getEdge(e.getEdgeType().getName(), e.getDest()) == null)
        newEdges.add(e);
    }
  }
  
  public void enhanceGraphWithAttributeEdges(String nodeType, EdgeCreator[] edgeCreators, Map<EdgeType,Double> weightsByEdgeType, boolean mergeEdges) {
    EdgeType mergedET = null;
    boolean newET = false;
    if(mergeEdges)
    {
      mergedET = new EdgeType("mergedEdge-"+nodeType+"-to-"+nodeType,nodeType,nodeType);
      if(graph.getEdgeType(mergedET.getName()) == null)
      {
        graph.addEdgeType(mergedET);
        newEdgeTypes.add(mergedET);
        newET=true;
      }
    }

    for(EdgeCreator ec : edgeCreators)
    {
      Edge[] edges = ec.createEdges();
      logger.info("Created "+((edges==null)?0:edges.length)+" edges using "+ec.getEdgeType().getName());
      if(edges == null || edges.length == 0)
        continue;
      
      EdgeType et = ec.getEdgeType();
      if(weightsByEdgeType.containsKey(et))
        EdgeTransformer.reweight(graph, edges, weightsByEdgeType.get(et));
      
      if(mergeEdges)
      {
        for(Edge e : edges) {
          graph.addEdge(mergedET, e.getSource(), e.getDest(), e.getWeight());
          if(!newET && e.getSource().getEdge(mergedET.getName(), e.getDest()) == null)
            newEdges.add(e);
        }
      }
      else
      {
        addEdges(edges);
      }
    }
  }
}
