/**
 * HistogramDiscrete.java
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

import netkit.graph.AttributeDiscrete;
import netkit.graph.Node;
import netkit.graph.Edge;

import java.util.*;

/** This class represents a histogram on Node field values which have
 * DISCRETE type.  The histogram keeps track of unique values and a
 * running of how many times each value appeared.
 * @see netkit.graph.AttributeDiscrete
 * @see netkit.graph.Node
 * 
 * @author Kaveh R. Ghazi
 */

public final class HistogramDiscrete extends Histogram
{
    private double mean = Double.NaN;
    private double median = Double.NaN;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;
    private final AttributeDiscrete attribute;

    /** This constructor is a convenience for accepting all values
     * without any minimum occurance.
     * @param values the array of double values for this object.
     * @param attribute the attribute describing the field type.
     */
    public HistogramDiscrete(double[] values, AttributeDiscrete attribute)
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
    public HistogramDiscrete(double[] values, AttributeDiscrete attribute, int minOccurance)
    {
	super(values, attribute, minOccurance);
	this.attribute = attribute;
    }
    
    /** This constructor is a convenience for accepting all node
     * values without any minimum occurance.
     * @param nodes the array of Nodes from which to get values.
     * @param attribute the attribute describing which field in the
     * Node to get values from.
     */
    public HistogramDiscrete(Node[] nodes, AttributeDiscrete attribute)
    {
	super(nodes, attribute, 1);
	this.attribute = attribute;
    }
    
    /** This constructor is a convenience for accepting all edge
     * values without any minimum occurance.
     * @param edges the array of Edges from which to get values.
     * @param attribute the attribute describing which field in the
     * Node to get values from.
     */
    public HistogramDiscrete(Edge[] edges, AttributeDiscrete attribute)
    {
	super(edges, attribute, 1);
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
    public HistogramDiscrete(Node[] nodes, AttributeDiscrete attribute, int minOccurance)
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
    public HistogramDiscrete(Edge[] edges, AttributeDiscrete attribute, int minOccurance)
    {
	super(edges, attribute, minOccurance);
	this.attribute = attribute;
    }

    /** Gets the attribute associated with this histogram.
     * @return the attribute associated with this histogram.
     */
    public AttributeDiscrete getAttribute()
    {
	return attribute;
    }

    /** Gets the maximum value stored in this object.
     * @return the maximum value stored in this object.
     */
    public int getMaxValue()
    {
        if(max == Integer.MIN_VALUE)
            max = Collections.max(cMap.keySet());
	// The "values" are actually stored in the key.
	return max;
    }

    /** Gets the minimum value stored in this object.
     * @return the minimum value stored in this object.
     */
    public int getMinValue()
    {
        if(min == Integer.MAX_VALUE)
            min = Collections.min(cMap.keySet());
	// The "values" are actually stored in the key.
	return min;
    }

    /** Gets the mean (average) value of the values in this object.
     * The mean is weighted by the count of each value.
     * @return the mean (average) value of the values in this object.
     */
    public double getMeanValue()
    {
        if(Double.isNaN(mean))
        {
            int result = 0;
            for (final Map.Entry<Integer,Double> me : cMap.entrySet())
                result += (me.getKey() * me.getValue());
            mean = (double)result / getTotalCount();
        }
        return mean;
    }

    /** Gets the median value of the values in this object.  The
     * median is weighted by the count of each value.
     * @return the median value of the values in this object.
     * @throws RuntimeException if there are no values in this object.
     */
    public double getMedianValue()
    {
        if(Double.isNaN(median))
        {
            @SuppressWarnings("unchecked")
            final Map.Entry<Integer, Double>[] entries = cMap.entrySet().toArray(new Map.Entry[cMap.size()]);

            Arrays.sort(entries, new Comparator<Map.Entry<Integer,Double>>() {
              public int compare(Map.Entry<Integer,Double> e1, Map.Entry<Integer,Double> e2)
              {
                return e1.getKey().compareTo(e2.getKey());
              }
            });
            
            final double totalCount = getTotalCount();
            final double medianCount = totalCount/2.0;

            double count = 0;
            int i=0;
            for(i=0;i<entries.length && count<medianCount;i++)
            {
              count += entries[i].getValue();
            }
            i--;

            if(count==medianCount)
              median = (entries[i].getKey()+entries[i+1].getKey())/2.0;
            else
              median = entries[i].getKey();
        }
        return median;
    }
}
