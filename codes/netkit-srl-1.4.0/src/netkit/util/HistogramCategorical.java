/**
 * HistogramCategorical.java
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

package netkit.util;

import netkit.graph.AttributeCategorical;
import netkit.graph.Node;
import netkit.graph.Edge;

/** This class represents a histogram on Node field values which have
 * CATEGORICAL type.  The histogram keeps track of unique values and a
 * running of how many times each value appeared.
 * @see netkit.graph.AttributeCategorical
 * @see netkit.graph.Node
 * 
 * @author Kaveh R. Ghazi
 */

public final class HistogramCategorical extends Histogram
{
    private final AttributeCategorical attribute;

    /** This constructor is a convenience for accepting all values
     * without any minimum occurance.
     * @param values the array of double values for this object.
     * @param attribute the attribute describing the field type.
     */
    public HistogramCategorical(double[] values, AttributeCategorical attribute)
    {
	super(values, attribute, 1);
	this.attribute = attribute;
    }

    /** This constructor creates a histogram object given an array of
     * values and an attribute type.  It also checks that values
     * appear at least minOccurance times before including them.
     * @param values the array of double values for this object.
     * @param attribute the attribute describing the field type.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    public HistogramCategorical(double[] values, AttributeCategorical attribute, int minOccurance)
    {
	super(values, attribute, minOccurance);
	this.attribute = attribute;
    }
    
    /** This constructor is a convenience for accepting all edge
     * values without any minimum occurance.
     * @param edges the array of Edges from which to get values.
     * @param attribute the attribute describing which field in the
     * Node to get values from.
     */
    public HistogramCategorical(Edge[] edges, AttributeCategorical attribute)
    {
	super(edges, attribute, 1);
	this.attribute = attribute;
    }

    /** This constructor is a convenience for accepting all node
     * values without any minimum occurance.
     * @param nodes the array of Nodes from which to get values.
     * @param attribute the attribute describing which field in the
     * Node to get values from.
     */
    public HistogramCategorical(Node[] nodes, AttributeCategorical attribute)
    {
	super(nodes, attribute, 1);
	this.attribute = attribute;
    }

    /** This constructor creates a histogram object given an array of
     * nodes and an attribute from which to get the values.  It also
     * checks that values appear at least minOccurance times before
     * including them.
     * @param nodes the array of Nodes from which to get values.
     * @param attribute the attribute describing the field in the Node
     * to get values from.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    public HistogramCategorical(Node[] nodes, AttributeCategorical attribute, int minOccurance)
    {
	super(nodes, attribute, minOccurance);
	this.attribute = attribute;
    }

    /** This constructor creates a histogram object given an array of
     * edges and an attribute from which to get the values.   Each edge
     * counts the destination-node's attribute edge-weight times. It also
     * checks that values appear at least minOccurance times before
     * including them.
     * @param edges the array of Edges from which to get values.
     * @param attribute the attribute describing the field in the Node
     * to get values from.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    public HistogramCategorical(Edge[] edges, AttributeCategorical attribute, int minOccurance)
    {
	super(edges, attribute, minOccurance);
	this.attribute = attribute;
    }

    /** Gets the attribute associated with this histogram.
     * @return the attribute associated with this histogram.
     */
    public AttributeCategorical getAttribute()
    {
	return attribute;
    }

    /** Gets the number of times a particular categorical token
     * appears in this histogram.
     * @param token the token to lookup in the histogram.
     * @return the number of times a particular categorical token
     * appears in this histogram.
     * @throws RuntimeException if the token isn't valid for this
     * histogram's attribute.
     * @throws NullPointerException if the token value doesn't exist
     * in this histogram.
     */
    public double getCount(String token)
    {
	return getCount(attribute.getValue(token));
    }

}
