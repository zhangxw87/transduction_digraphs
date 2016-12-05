/**
 * AggregatorImp.java
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

import netkit.graph.EdgeType;
import netkit.graph.Node;
import netkit.graph.Attribute;
import netkit.graph.AttributeCategorical;
import netkit.graph.Type;

/**
 * This should be the parent for any Aggregator class.
 * It sets up the basic structure for an Aggregator and defines most of the needed API.
 * The only thing needed to be implemented by any subclass is the getValue(Node, Estimate)
 * method.
 *
 * @see netkit.graph.Attribute
 * @see netkit.graph.Attributes
 * @see netkit.graph.Node
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public abstract class AggregatorImp implements Aggregator {
    // The final immutable name of this virtual attribute
    protected final String name;

    // This is a cache for aggregated values across a nodes in the graph such that we don't need to recompute every time.  Memory intensive.
    protected SharedNodeInfo aggregateCache = null;

    // This is a pointer to the attribute object that is being aggregated on
    protected final Attribute attribute;

    // The attribute type of this aggregator
    protected final Type type;

    // The name of the relation to follow to get to the neighbors of a node
    protected final EdgeType edgeType;

    // The index of the attribute into the Attributes vector for quick lookup
    protected int attribIdx = -1;

    /**
     * Helper constructor for AggregatorByValueImp - an aggregator for a specific attribute value.  We need
     * this here to correctly set the name of the Aggregator.  If this is not aggregating by a specific value,
     * then call it with a 'value' of Double.NaN.  The name of this virtual attribute is created by appending
     * the aggName, edgeType, and attribute name.  If the 'value' is not NaN, then append the value.
     * If the attribute is categorical, then this will be the token represented by the double.  It will otherwise
     * by the double value (cast as an integer if the attribute type is DISCRETE).
     *
     * @param aggName The prefix name of this aggregated attribute.  Normally the name of the aggregator (e.g., 'min' or 'max')
     * @param edgeType The name of the edge type to traverse to get at the neighboring nodes
     * @param attribute The attribute of the neighbor nodes to aggregate over
     * @param type The attribute type of this aggregator
     * @param value The value of the attribute to aggregate on (e.g., the double representing 'red').  Use Double.NaN if it is not aggregating by value.
     *
     * @see netkit.classifiers.aggregators.AggregatorImp#AggregatorImp(String, String, netkit.graph.Attribute, netkit.graph.Type)
     * @see java.lang.Double.NaN
     */
    protected AggregatorImp(final String aggName, final EdgeType edgeType, final Attribute attribute, final Type type, final double value) {
        this.attribute = attribute;
        this.type = type;
        this.edgeType = edgeType;

        if(Double.isNaN(value))
        {
            this.name = aggName+"_"+edgeType.getName()+"_"+attribute.getName();
        }
        else
        {
            switch(attribute.getType())
            {
            case CATEGORICAL:
                String token = ((AttributeCategorical)attribute).getToken((int)value).replaceAll(" ","_");
                this.name = aggName+"_"+edgeType.getName()+"_"+attribute.getName()+"_"+token;
                break;
            case DISCRETE:
                this.name = aggName+"_"+edgeType.getName()+"_"+attribute.getName()+"_"+((int)value);
                break;
            case CONTINUOUS:
                this.name = aggName+"_"+edgeType.getName()+"_"+attribute.getName()+"_"+value;
                break;
            default:
                throw new IllegalArgumentException("Unknown attribute type: "+attribute);
            }
        }
    }

    /**
     * Creates an aggregator that is not by value (it calls the more specific constructor with a 'value' of Double.NaN).
     * The name of this virtual attribute is created by appending
     * aggName, edgeType, and attribute name.
     *
     * @param aggName The prefix name of this aggregated attribute.  Normally the name of the aggregator (e.g., 'min' or 'max')
     * @param edgeType The name of the edge type to traverse to get at the neighboring nodes
     * @param attribute The attribute of the neighbor nodes to aggregate over
     * @param type The attribute type of this aggregator
     *
     * @see netkit.classifiers.aggregators.AggregatorImp#AggregatorImp(String, String, netkit.graph.Attribute, netkit.graph.Type, double)
     */
    public AggregatorImp(final String aggName, final EdgeType edgeType, final Attribute attribute, final Type type) {
        this(aggName,edgeType,attribute,type,Double.NaN);
    }

    /**
     * @return The name of this aggregated attribute
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the index of the attribute in the instance vector array--we need to go through a node to
     * get at this information.  This information tells us where in the instance vector array
     * to get the attribute value for the attribute that is to be aggregated on.
     *
     * @param nodeType The type of the node through which we can access the attribute array information
     * @return the index of the attribute in the instance vector array
     */
    protected final int getAttributeIndex(final String nodeType) {
    	
        if(attribIdx == -1)
            attribIdx = SharedNodeInfo.getAttributeIndex(nodeType,attribute);
        return attribIdx;
    }

    /**
     * What is the attribute that is being aggregated
     * @return the attribute that is being aggregated
     */
    public final Attribute getAttribute() {
        return attribute;
    }

    /** Gets the Type of the value stored in this object.
     * @return the Type of the value stored in this object.
     */
    public final Type getType()  {
        return type;
    }

    /**
     * What is the relation that should be used to get at the neighbors of an instance
     * @return the relation that should be used to get at the neighbors of an instance
     */
    public final EdgeType getEdgeType() {
        return edgeType;
    }

    /**
     * This is cached aggregation information about the node as is relevant to the relationship that
     * this aggregator uses.
     *
     * @param node The node whose cached aggregated information is needed.
     * @return
     */
    protected final SharedNodeInfo getNodeInfo(final Node node) {
        if(aggregateCache == null)
        {
            String nodeType = ( edgeType == null ) ? node.getType() : edgeType.getDestType();
            aggregateCache = SharedNodeInfo.getInfo(nodeType, getAttributeIndex(nodeType), edgeType);
        }
        return aggregateCache;
    }

    /**
     * Aggregate around the given node in the graph and return the result.
     *
     * @param n The node around which to aggregate
     * @return
     */
    public final double getValue(Node n) {
        return getValue(n,null);
    }

    public String toString() {
        return "Aggregator["+name+":"+type+"]";
    }
}
