/**
 * EdgeType.java
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

/** This class represents an edge type for the Edge class.  EdgeTypes
 * specify the acceptable types for the source and destination Nodes
 * of an Edge.  This class is immutable.
 * @see Graph
 * @see Node
 * @see Edge
 * @see Attributes
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public final class EdgeType
{
    // The name field is enough to ensure uniqueness and will be used
    // for hashCode() and equals().
    private final String name;

    private final String sourceType;
    private final String destType;
    
    /** The constructor requires a name, source type and destination type.
     * @param name a string name uniquely identifying this edge type.
     * @param sourceType a string representing the type of the source node.
     * @param destType a string representing the type of the destination node.
     */
    public EdgeType(String name, String sourceType, String destType)
    {
        this.name = name;
        this.sourceType = sourceType;
        this.destType = destType;
    }

    /** Get the name of this object.
     * @return the name of this object.
     */
    public String getName()
    {
	return name;
    }
    
    /** Get the source type of this object.
     * @return the source type of this object.
     */
    public String getSourceType()
    {
	return sourceType;
    }
    
    /** Get the destination type of this object.
     * @return the destination type of this object.
     */
    public String getDestType()
    {
	return destType;
    }
    
    /** Returns a hash code value for this object.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
    	return name.hashCode();
    }

    /** Indicates whether some other object is "equal to" this one;
     * note this method assumes that each instance has a unique name
     * and uses the name for equality purposes.  The constraint on
     * name uniqueness is enforced by the Graph class which holds
     * EdgeTypes.
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the
     * argument; {@code false} otherwise.
     * @see Graph#addEdgeType(EdgeType)
     */
    public boolean equals(Object o)
    {
	if (this == o)
	    return true;
        if (o instanceof EdgeType)
            return name.equals(((EdgeType)o).getName());
        return false;
    }

    /** Returns a String representation for this object.
     * @return a String representation for this object.
     */
    public String toString()
    {
    	return "[EdgeType(name=<"+name+">,sourceType=<"+sourceType+">,destType=<"+destType+">)]";
    }
}
