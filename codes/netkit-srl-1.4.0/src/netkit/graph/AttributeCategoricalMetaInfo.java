/**
 * AttributeCategoricalMetaInfo.java
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
import netkit.util.HistogramCategorical;

/** This class is used to persist certain information across an entire
 * graph related to a particular AttributeCategorical.
 * 
 * @see AttributeCategorical
 * @see Graph
 * @see Node
 * 
 * @author Kaveh R. Ghazi
 */

public class AttributeCategoricalMetaInfo extends AbstractAttributeMetaInfo
{
    final AttributeCategorical attrib;
    final int mode;
    
    /** Construct an object of this type.
     * @param attrib the AttributeCategorical for this meta info object.
     * @param attributes the Attributes container that attrib lives in.
     * @param graph the Graph object that the attributes lives in.
     */
    public AttributeCategoricalMetaInfo(AttributeCategorical attrib,
					Attributes attributes, Graph graph)
    {
	super(attrib, attributes, graph);
	this.attrib = attrib;

	// Calculate the mode.
	// First figure out how many times each value appears.
	final Map<Integer,Integer> vMap = new HashMap<Integer,Integer>();
	final double vals[] = graph.getValues(getNodeTypeName(), getFieldName());
	for (final double d : vals)
	{
	    final Integer count = vMap.get((int)d);
	    vMap.put((int)d, (count == null) ? 1 : count+1);
	}

	// Then create a Comparator object on Map.Entrys.
	final Comparator<Map.Entry<Integer,Integer>> compValues
	    = new Comparator<Map.Entry<Integer,Integer>>()
	    {
		public final int compare(Map.Entry<Integer,Integer> o1,
					 Map.Entry<Integer,Integer> o2)
		{ return o1.getValue().compareTo(o2.getValue()); }
	    };

	// Then find the value that appears most often.
	// FIXME: this is deterministic, should it be stochastic???
	this.mode = Collections.max(vMap.entrySet(), compValues).getKey();
    }

    /** Get the class reference vector from the graph for this
     * attribute.
     * @param sourceAttrib the source attribute for the vector.
     * @param sourceAttribValue the value the source attribute must have.
     * @param path the list of EdgeTypes to follow in the path when
     * creating the vector.
     * @param normalized if true, the results will be normalized.
     * @return a HistogramCategorical containing the reference vector.
     */
    public HistogramCategorical getClassReferenceVector(AttributeCategorical sourceAttrib,
							final int sourceAttribValue,
							List<EdgeType> path,
							boolean normalized)
    {
	return graph.getClassReferenceVector(sourceAttrib, sourceAttribValue, attrib, path, normalized);
    }
    
    /** Get the unconditional reference vector from the graph for this
     * attribute.
     * @param path the list of EdgeTypes to follow in the path when
     * creating the vector.
     * @param normalized if true, the results will be normalized.
     * @return a HistogramCategorical containing the reference vector.
     */
    public HistogramCategorical getUnconditionalReferenceVector(List<EdgeType> path, boolean normalized)
    {
	return graph.getUnconditionalReferenceVector(attrib, path, normalized);
    }
    
    /** Get the mode value this Graph has for this field.
     * @return the mode value this Graph has for this field.
     */
    public int getMode()
    {
	return mode;
    }
}
