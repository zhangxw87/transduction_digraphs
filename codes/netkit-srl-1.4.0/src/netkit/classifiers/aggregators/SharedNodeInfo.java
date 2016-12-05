/**
 * SharedNodeInfo.java
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
 **/

/**
 * $Id$
 **/
package netkit.classifiers.aggregators;

import netkit.graph.*;
import netkit.util.HistogramDiscrete;
import netkit.util.VectorMath;
import netkit.classifiers.Estimate;

import java.util.HashMap;
import java.util.Map;

/**
 * The SharedNodeInfo class is used to cache aggregation statistics for a given node such that
 * multiple aggregators can use the same statistics without having to calculate them more than
 * once.
 * <P>
 * Currently, it is assumed that aggregators for a specific node is called in a succession
 * before aggregation for a different node in the graph is done.  Therefore, I only cache things
 * as long is we are aggregating on the same node.  This saves a lot of memory.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class SharedNodeInfo {
    // Key into the static lookup table to get cached SharedNodeInfo instances
    // These are keyed on nodetype and edgetype, so that is what this key uses
    private final static class SNIKey {
        public final String nodeType;
        public final String edgeType;
        private int hash = -1;
        public SNIKey(final String nt, final String et) {
            this.nodeType = nt;
            this.edgeType = ((et == null) ? "__null" : et);
        }
        public boolean equals(Object o) {
            if(o instanceof SNIKey) {
                SNIKey s = (SNIKey)o;
                return(nodeType.equals(s.nodeType) && edgeType.equals(s.edgeType));
            }
            return false;
        }
        public int hashCode() {
            if(hash == -1)
                hash = ("NodeType:"+nodeType+"-EdgeType:"+edgeType).hashCode();
            return hash;
        }
    }

    // lookup table to find a sharednode.  Currently I have one static node per nodetype per
    // attribute
    private static Map<SNIKey,SharedNodeInfo[]> info = new HashMap<SNIKey,SharedNodeInfo[]>();

    // The graph that is currently being used for aggregation.  I need this for certain lookups
    // when I create the sharednodeinfo classes.
    private static Graph g = null;

    // What was the last node info (array) that was asked for?  It is very likely that we will
    // want another SharedNodeInfo using the same nodetype and edgetype (but possibly a different
    // attribute).  This is just for quick lookup
    private static SNIKey lastKey = null;
    private static SharedNodeInfo[] lastArr = null;

    /**
     * Assume that we will be doing aggregation over this particular graph
     * until further notice
     * @param g The graph being aggregated over
     */
    public static void initialize(Graph g) {
        SharedNodeInfo.g = g;
    }
    
    public static int getAttributeIndex(final String nodeType, final Attribute attrib) {
        if(g == null)
            throw new IllegalStateException("SharedNode has not been initialized with a graph.");
        return g.getAttributes(nodeType).getAttributeIndex(attrib.getName());
    }

    /**
     * Get a SharedNodeInfo instance for a given node type, attribute and edge type.
     *
     * @param nodeType The node type of the neighbor nodes that will be aggregated over
     *                 (for example, if I want to create an aggregate result on node X of type XType
     *                  and I want to aggregate over attribute A on X's neighbors that are of type
     *                  YType, then nodeType is 'YType')
     * @param attribIdx What is the attribute index of the attribute to be aggregated over.
     * @param edgeType  What is the edge that is used to get from the source node (XType) to the
     *                  destination node (YType).  This is used to get the neighbors of a given
     *                  source node when computing the aggregation statistics.
     * @return A SharedNodeInfo instance that works for a given nodetype, attribute index and edgetype.
     */
    public static SharedNodeInfo getInfo(final String nodeType, final int attribIdx, EdgeType edgeType) {
        if(g == null)
            throw new IllegalStateException("SharedNode has not been initialized with a graph.");

        /** This first looks up the array of all cached information about a given nodetype and edgetype.
         * This is an array of sharednodeinfos across all attributes on that nodetype and we instantiate
         * only those that are needed.  We first look up to see if we got this array on the last query
         * to speed that up (this is admittedly a sille optimization, but that's how I am sometimes).
         */
        SNIKey key = new SNIKey(nodeType, edgeType.getName());
        SharedNodeInfo[] list = null;
        if(key.equals(lastKey))
            list = lastArr;
        else
        {
            list = info.get(key);
            if(list == null)
            {
                list = new SharedNodeInfo[g.getAttributes(nodeType).attributeCount()];
                java.util.Arrays.fill(list,null);
                info.put(key,list);
            }
            lastKey = key;
            lastArr = list;
        }
        SharedNodeInfo sni = list[attribIdx];
        if(sni == null)
        {
            sni = new SharedNodeInfo(nodeType, edgeType.getName(), attribIdx);
            list[attribIdx] = sni;
        }
        return sni;
    }

    /**
     * Helper function to get the node type at the other end of the given edgeType
     * @param edgeType The edgetype whose destination node type is requested
     * @return the node type at the other end of the given edgeType
     */
    public static String getDestinationNodeType(final String edgeType) {
        if(g == null)
            throw new IllegalStateException("SharedNode has not been initialized with a graph.");
        return ( (edgeType == null) ? null : g.getEdgeType(edgeType).getDestType() );
    }

    // specific information about the attribute that will be aggregated over
    private final String nodeType;
    private final String edgeType;
    private final int attribIdx;
    private final Attribute attribute;

    // various cached information
    private double[] count;
    private double min = Double.NaN;
    private double max = Double.NaN;
    private double mean = Double.NaN;
    private double sum = 0.0;
    private HistogramDiscrete histogram = null ;
    private Node lastNode = null;
    private Edge[] edges = null;

    /**
     * Constructor that can only be called by the 'getInfo' method.
     * @param nodeType The nodeType which is used to get neighbors of a specified node
     * @param edgeType The edgeType which is used to get neighbors of a specified edge
     * @param attribIdx
     * @see netkit.classifiers.aggregators.SharedNodeInfo#getInfo(String, int, EdgeType)
     */
    private SharedNodeInfo(final String nodeType, final String edgeType, final int attribIdx) {
        this.edgeType = edgeType;
        this.attribIdx = attribIdx;
        this.nodeType = nodeType;
        this.attribute = g.getAttributes(nodeType).getAttribute(attribIdx);

        if(attribute instanceof AttributeCategorical)
        {
            count = new double[((AttributeCategorical)attribute).size()];
            if(count.length > 0)
                count[0] = Double.NaN;
        }
        else
        {
            count = null;
        }
    }

    /**
     * Potentially resets the cached information if the new node is different from
     * the last node that was used.
     * @param n The new node to cache aggregate information about.
     */
    private void reset(Node n) {
    	if(n != lastNode) {
            if(!n.getType().equals(nodeType))
            	return;

            min = Double.NaN;
            max = Double.NaN;
            mean = Double.NaN;
            sum = 0.0;
            histogram = null ;
            lastNode = null;
            edges = null;
            if(count != null && count.length>0)
                count[0] = Double.NaN;
            lastNode = n;
        }
    }

    /**
     * Get numerical statistics on attributes that are discrete or continuous.  This includes
     * finding the max, min and mean.
     * @param n The node to aggregate on
     */
    private void getNumericStat(Node n) {
        reset(n);
        if(!Double.isNaN(min))
            return;

        if(attribute instanceof AttributeDiscrete)
        {
            HistogramDiscrete hist = getHistogram(n);
            if(hist == null)
            {
                min = Double.MIN_VALUE;
                max = Double.MAX_VALUE;
                mean = Double.NaN;
            }
            else
            {
                max = hist.getMaxValue();
                min = hist.getMinValue();
                mean = hist.getMeanValue();
            }
        }
        else if(attribute instanceof AttributeContinuous)
        {
            mean = Double.NaN;
            double total = 0;
            double num = 0;
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
            for(Edge e : getNeighborEdges(n))
            {
                Node dst = e.getDest();
                if(!dst.isMissing(attribIdx))
                {
                    double value = dst.getValue(attribIdx);
                    num++;
                    if(num==1)
                    {
                        min = value;
                        max = value;
                    }
                    else if(value < min)
                        min = value;
                    else if(value > max)
                        max = value;
                    total += value;
                }
            }
            if(num>0)
                mean = total / num;
        }

    }

    /**
     * Get the edges of a node to find the relevant neighbors.  Uses the edgeType that was
     * specified when the SharedNodeInfo was first acquired.
     * @param n The node whose edges to get
     * @return An array of edges
     */
    private Edge[] getNeighborEdges(Node n) {
        reset(n);
        if(edges == null)
        {
            edges = ((edgeType == null) ? n.getEdges() : n.getEdgesByType(edgeType));
            lastNode = n;
        }
        return edges;
    }

    /**
     * Count, for all relevant neighbors, how many of the neighboring attributes took on each
     * of the possible values (weighted by the edge weight).
     * If the prior is not null, then use the estimated values (for
     * categorical attributes) such that is the neighbor attribute is 'red' with a likelihood
     * of 60 percent (0.6), then count 0.6 towards 'red'.
     *
     * @param n The source node from which to find neighboring nodes
     * @param prior The prior estimations of the values of the attribute to be aggregated on (works only for categorical attributes)
     * @return A double array that has the count, for each possible value of the attribute, across all relevant neighbors
     *
     * @see netkit.classifiers.aggregators.SharedNodeInfo#getNeighborEdges(netkit.graph.Node)
     */
    public double[] countNeighbors(Node n, Estimate prior) {
        reset(n);
        if(count != null && count.length>0 && Double.isNaN(count[0]))
        {
            java.util.Arrays.fill(count,0);
            if(prior == null)
            {
                for (Edge e : getNeighborEdges(n))
                {
                    Node dest = e.getDest();
                    if(!dest.isMissing(attribIdx))
                    {
                        int value = (int)dest.getValue(attribIdx);
                        count[value] += e.getWeight();
                    }
                }
            }
            else
            {
               for (Edge e : getNeighborEdges(n))
                {
                    Node dest = e.getDest();
                    double wt = e.getWeight();

                    if(dest.isMissing(attribIdx))
                    {
                        double[] d = prior.getEstimate(dest);
                        if(d==null)
                            continue;
                        for(int v=0;v<d.length;v++)
                            count[v] += wt*d[v];
                    }
                    else
                    {
                        int value = (int)dest.getValue(attribIdx);
                        count[value] += wt;
                    }
                }
            }
            sum = VectorMath.sum(count);
        }
        return count;
    }

    /**
     * Get the (weighted) sum of all relevant neighbors.  Counts all neighbors and sums up the score.
     * This is useful to get a ratio rather than absolute count
     *
     * @param n The source node for the aggregation
     * @param prior The estimated priors of the attribute to be aggregated on
     * @return Get the (weighted) sum of all relevant neighbors.
     *
     * @see netkit.classifiers.aggregators.SharedNodeInfo#countNeighbors(netkit.graph.Node, netkit.classifiers.Estimate)
     */
    public double getSum(Node n, Estimate prior) {
        reset(n);
        if(sum == 0)
            countNeighbors(n,prior);
        return sum;
    }

    /**
     * Get the minium observed value (of the neighbors of the source node) of the discrete
     * or continuous attribute that is being aggregated over.
     *
     * @param n The source node to aggregate from
     * @return The minimum observed value of the attribute over relevant neighbors
     */
    public double getMin(Node n) {
        getNumericStat(n);
        return min;
    }

    /**
     * Get the maximum observed value (of the neighbors of the source node) of the discrete
     * or continuous attribute that is being aggregated over.
     *
     * @param n The source node to aggregate from
     * @return The maximum observed value of the attribute over relevant neighbors
     */
    public double getMax(Node n) {
        getNumericStat(n);
        return max;
    }

    /**
     * Get the mean observed value (of the neighbors of the source node) of the discrete
     * or continuous attribute that is being aggregated over.
     *
     * @param n The source node to aggregate from
     * @return The mean observed value of the attribute over relevant neighbors
     */
    public double getMean(Node n) {
        getNumericStat(n);
        return mean;
    }

    /**
     * Get the histogram of observed values (of the neighbors of the source node) of the discrete
     * attribute that is being aggregated over.
     *
     * @param n The source node to aggregate from
     * @return The histogram of observed value of the attribute over relevant neighbors
     */
    public HistogramDiscrete getHistogram(Node n) {
        reset(n);
        if(histogram == null)
        {
            try
            {
                histogram = new HistogramDiscrete(getNeighborEdges(n), (AttributeDiscrete)attribute, 1);
            }
            catch(Exception e)
            {
                // This happens if the were no data points
            }
        }
        return histogram;
    }
}
