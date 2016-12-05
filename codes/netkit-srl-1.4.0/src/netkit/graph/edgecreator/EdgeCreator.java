/**
 * GraphTransfomer.java
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
package netkit.graph.edgecreator;

import netkit.graph.*;
import netkit.classifiers.DataSplit;

public interface EdgeCreator {
  /**
   * Calculate the edgeweight from node src to node dest.  Assumes that
   * <code>initialize</code> has already been called, so that the creator
   * knows the graph, the class attribute index and the attribute index
   * 
   * @param src The source node
   * @param dest The destination node
   * @return the weight of the edge, Double.NaN if no edge should be created.
   */
   public double getWeight(final Node src, final Node dest);
   
   /**
    * The name of this edge-creator
    */
   public String getName();
          
   public EdgeType getEdgeType();
   
   /**
    * Which attribute is this based on.
    */
   public int getAttributeIndex();
   
   /**
    * How many edges should there be at maximum per node?
    */
   public int getMaxEdges();
   
   /**
    * Which attribute value is this based on.
    */
   public double getAttributeValue();
   
   /**
    * Is this edge creator by attribute or by instance as a whole
    */
   public boolean isByAttribute();
   
   /**
    * Is this edge creator by attribute value or by attribute as a whole
    */
   public boolean isByAttributeValue();
   
   /**
    * Initialize this creator.  This can only be done once
    * 
    * @param graph The graph over which to create edges
    * @param nodeType Which nodetype is the node to create edges over
    * @param attributeIndex The attribute index over which to generate edges (ignored for creators that cannot handle attributes)
    * @param attributeValue The attribute value over which to generate edges (ignored for creators that cannot handle values; otherwise ignored if value is Double.NaN)
    * @param maxEdges The maximum number of outgoing edges to create from any given node
    * @exception IllegalStateException if this creator has already been initialized
    */
    public void initialize(final Graph graph, final String nodeType, final int attributeIndex, final double attributeValue, final int maxEdges)
      throws IllegalStateException;
    
    /**
     * Queries the edge creator if it can handle (i.e., create edges for) the
     * given attribute.
     * 
     * @param attribute
     * @return true if this edge creator can create edges based on the given Attribute
     */
     public boolean canHandle(final Attribute attribute);
     
     /**
      * Queries the edge creator if it can handle (i.e., create edges for) the
      * given attribute using a specific attribute value.
      * 
      * @param attribute
      * @return true if this edge creator can create edges based on the given Attribute if given a specific attribute value
      */
      public boolean canHandleAttributeValue(final Attribute attribute);
      
      /**
       * Build a model of edge creation based on the data split.  Must be called before calling
       * any analytic methods.
       * 
       * @param split
       */
      public void buildModel(final DataSplit split);
      
      /**
       * Compute the node-based assortativity of this edge creator.
       * This is calculated by computing the assortativity of the nodes,
       * using only the edges that this edge creator would create (creating
       * no more then <i>maxEdges</i> outgoing edges per node if possible --
       * this is provided in the initialize method).
       * 
       * @param useTruth Should we compute assortativity based on the whole truth or just the training set
       * @exception IllegalStateException if the creator has not yet been built
       */
      public double getAssortativity(final boolean useTruth) throws IllegalStateException;
      
      /**
       * Create all the edges on the graph provided in the initialize method
       * (indirectly through the DataSplit object).  It will create no more than
       * <i>maxEdges</i> outgoing edges (if possible), as provided in the initialize
       * method.
       * 
       * @exception IllegalStateException if the creator has not yet been built
       */
      public Edge[] createEdges() throws IllegalStateException;
}
