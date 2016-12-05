/**
 * AbstractAttributeMetaInfo.java
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

package netkit.graph;

import java.util.*;

/** This abstract class is used to persist certain information across
 * an entire Graph related to a particular Attribute.
 * 
 * @see Attribute
 * @see Graph
 * @see Node
 * 
 * @author Kaveh R. Ghazi
 */

public abstract class AbstractAttributeMetaInfo
{
    protected final Graph graph;
    private final String fieldName;
    private final String nodeTypeName;

    private final double mean;
    private final double median;
    private final double max;
    private final double min;
    
    /** Construct an object of this type.
     * @param attrib the Attribute for this meta info object.
     * @param attributes the Attributes container that attrib lives in.
     * @param graph the Graph object that the attributes lives in.
     */
    AbstractAttributeMetaInfo(Attribute attrib, Attributes attributes, Graph graph)
    {
	this.graph = graph;
	fieldName = attrib.getName();
	nodeTypeName = attributes.getName();
	
	final double vals[] = graph.getValues(nodeTypeName, fieldName);
	Arrays.sort(vals);
	
	// After sorting, min and max are the first and last elements
	min = vals[0];
	max = vals[vals.length-1];
	
	// Calculate the mean
	double sum = 0;
	for (final double d : vals)
	    sum += d;
	mean = sum/vals.length;

	// Get the median
	if (vals.length % 2 == 0)
	    median = (vals[vals.length/2]+vals[vals.length/2-1])/2;
	else
	    median = vals[vals.length/2];
    }

    /** Get the nodeTypeName for the Attributes container used for
     * this object.
     * @return a String representing the Attributes container used for
     * this object.
     */
    public String getNodeTypeName()
    {
	return nodeTypeName;
    }
    
    /** Get the field name for the Attribute used for this object.
     * @return a String representing the field name for the Attribute
     * used for this object.
     */
    public String getFieldName()
    {
	return fieldName;
    }
    
    /** Get the maximum value this Graph has for this field.
     * @return the maximum value this Graph has for this field.
     */
    public double getMax()
    {
	return max;
    }
    
    /** Get the minimum value this Graph has for this field.
     * @return the minimum value this Graph has for this field.
     */
    public double getMin()
    {
	return min;
    }
    
    /** Get the mean value this Graph has for this field.
     * @return the mean value this Graph has for this field.
     */
    public double getMean()
    {
	return mean;
    }
    
    /** Get the median value this Graph has for this field.
     * @return the median value this Graph has for this field.
     */
    public double getMedian()
    {
	return median;
    }
}
