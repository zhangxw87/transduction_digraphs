/**
 * Edge.java
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

/** The Edge class represents an edge in the Graph object.  It
 * connects a source and destination Node with a given weight.  Edges
 * also have an EdgeType which specifies which kinds of Nodes can be
 * connected by this Edge.
 * @see Graph
 * @see Node
 * @see EdgeType
 * @see Attributes
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public final class Edge implements Comparable<Edge>
{
    private final EdgeType edgeType;
    private final Node source;
    private final Node dest;

    private float weight;
    
    /** The constructor requires an EdgeType, a source and destination Node and a weight.
     * @param edgeType an edge type describing the valid node types of
     * the source and destination nodes.
     * @param source a node whose type must be valid for the EdgeType.
     * @param dest a node whose type must be valid for the EdgeType.
     * @param weight a double value representing the weight of this edge.
     * @throws RuntimeException if the source or dest Node's node type
     * is not valid for the supplied EdgeType.
     * @throws IllegalArgumentException if the supplied weight
     * increment is less than zero.
     */
    public Edge(EdgeType edgeType, Node source, Node dest, double weight)
    {
	if (!edgeType.getSourceType().equals(source.getType()))
	    throw new RuntimeException("Source Node type mismatch, expected <"
				       + edgeType.getSourceType() + "> got <"
				       + source.getType() + ">");
	if (!edgeType.getDestType().equals(dest.getType()))
	    throw new RuntimeException("Destination Node type mismatch, expected <"
				       + edgeType.getDestType() + "> got <"
				       + dest.getType() + ">");
	
        this.edgeType = edgeType;
        this.source = source;
        this.dest = dest;
	addWeight(weight);
    }

    /** Copy constructor
     * @param e an Edge to copy into a new object.
     */
    public Edge(Edge e)
    {
	this(e.edgeType, e.source, e.dest, e.weight);
    }
    
    /** Increments the weight field of this Edge.
     * @param weight the amount by which the weight is incremented.
     * @throws IllegalArgumentException if the supplied weight
     * increment is less than zero.
     */
    public void addWeight(double weight)
    {
        if(weight <= 0)
            throw new IllegalArgumentException("Edge weight cannot be < 0!");
        this.weight += weight;
    }

    /** Sets the weight field of this Edge.
     * @param weight the new weight of the edge.
     * @throws IllegalArgumentException if the supplied weight
     * is less than zero.
     */
    public void setWeight(double weight)
    {
        if(weight <= 0)
            throw new IllegalArgumentException("Edge weight cannot be <= 0!");
        this.weight = (float)weight;
    }

    /** Get the weight field of this object.
     * @return the weight field of this object.
     */
    public double getWeight()
    {
        return weight;
    }

    /** Get the source node of this object.
     * @return the source node of this object.
     */
    public Node getSource()
    {
	return source;
    }
    
    /** Get the destination node of this object.
     * @return the destination node of this object.
     */
    public Node getDest()
    {
	return dest;
    }
    
    /** Get the edge type of this object.
     * @return the edge type of this object.
     */
    public EdgeType getEdgeType()
    {
	return edgeType;
    }
    
    /** Returns a hash code value for this object.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
	return 37 * (37 * (37 * 17 + edgeType.hashCode()) + source.hashCode()) + dest.hashCode();
    }

    /** Indicates whether some other object is "equal to" this one. 
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the
     * argument; {@code false} otherwise.
     */
    public boolean equals(Object o)
    {
	if (this == o)
	    return true;
        if (o instanceof Edge)
        {
            final Edge e = (Edge)o;
            return e.getEdgeType().equals(edgeType)
		&& e.getSource().equals(source)
		&& e.getDest().equals(dest);
        }
        return false;
    }

    /** Specifies a natural ordering for Edges; compare EdgeTypes
     * first, then the source Node and finally the destination Node.
     * @param e the Edge to compare this object to.
     * @return a natural ordering for Edges; compare EdgeTypes first,
     * then the source Node and finally the destination Node.
     */
    public int compareTo(Edge e)
    {
	if (!this.getEdgeType().equals(e.getEdgeType()))
	    return this.getEdgeType().getName().compareTo(e.getEdgeType().getName());
	else if (!this.getSource().equals(e.getSource()))
	    return this.getSource().compareTo(e.getSource());
	else
	    return this.getDest().compareTo(e.getDest());
    }

    /** Returns a String representation for this object.
     * @return a String representation for this object.
     */
    public String toString()
    {
    	return "[Edge("+source.getName()+","+dest.getName()+","+weight+")]";
    }
}
