/**
 * Node.java
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

/** This class represents a node in the Graph object.  It holds it's
 * identifier name and the Attributes container for all its fields.
 * Nodes keep track of their Edge's and Node neighbors and all the
 * values for its Attributes.
 * @see Graph
 * @see Attributes
 * @see Edge
 * @see EdgeType
 * 
 * @author Kaveh R. Ghazi
 */
public final class Node implements Comparable<Node>
{
    private final String name;
    private final int index;
    private final Attributes attributes;
    private double values[];
    // The key is the edge type name and the values are submaps of
    // NeighborNode->ConnectingEdge.  We start with a low size to
    // conserve space for large graphs with few EdgeTypes.
    private final HashMap<String,HashMap<Node,Edge>> etMap
	= new HashMap<String,HashMap<Node,Edge>>(2);

    private transient int hash;
    private transient Node[] neighbors;
    private transient Edge[] edges;

    // Helper method to check the values array length against the
    // Attributes container length.  This ensures we don't try to
    // examine the values array if the field container has been
    // modified without resynchronizing by calling addValues() or
    // removeValue().
    private void checkLengths()
    {
	if (values.length != attributes.attributeCount())
	    throw new RuntimeException("values length <"+values.length
				       +"> does not match attribute count <"
				       +attributes.attributeCount()+">");
    }
    
    private Node(String name, Attributes attributes, int index, double[] values)
    {
      this.name = name;
      this.attributes = attributes;
      this.index = index;
      this.values = values;
     
    }
    
    /** The constructor must be provided with a name, an Attributes
     * container and an index.  The combination of the node name and
     * the Attributes name must be unique, however Nodes with
     * different Attributes can have the same name.  The Node's values
     * all default to "unknown" or NaN.
     * @param name a string value which, in combination with the Attributes, uniquely identifies this node.
     * @param attributes an attribute container which, in combination with the name, uniquely identifies this node.
     * @param index an index counter providing an offset into the
     * Graph Node array for quick lookups.  Set by the Graph class
     * when creating new Nodes and adding them to it's own containers.
     * Note, each new Node type should get a separate zero-based
     * index.
     */
    public Node(String name, Attributes attributes, int index)
    {
      this(name,attributes,index,new double[attributes.attributeCount()]);
     	// Every field starts out "unknown".
    	Arrays.fill(this.values, Double.NaN);
    
    	// Compute and cache the hash code value for this object.
    	hash = 37 * (37 * 17 + name.hashCode()) + attributes.hashCode();
    }

    /** The constructor must be provided with a name, an Attributes
     * container and an index.  The combination of the node name and
     * the Attributes name must be unique, however Nodes with
     * different Attributes can have the same name.  The Node's values
     * all default to "unknown" or NaN.
     * @param name a string value which, in combination with the Attributes, uniquely identifies this node.
     * @param attributes an attribute container which, in combination with the name, uniquely identifies this node.
     * @param index an index counter providing an offset into the
     * Graph Node array for quick lookups.  Set by the Graph class
     * when creating new Nodes and adding them to it's own containers.
     * Note, each new Node type should get a separate zero-based
     * index.
     */
    public Node copy(int index)
    {
      Node n = new Node(name,attributes,index,values);
      n.hash = hash;
      return n;
    }

    /** Get the name of this node.
     * @return the name of this node.
     */
    public String getName()
    {
	return name;
    }

    /** Get the index of this node.
     * @return the index of this node.
     */
    public int getIndex()
    {
	return index;
    }

    /** Get the type name of this node.
     * @return the type name of this node.
     */
    public String getType()
    {
	return attributes.getName();
    }
    
    /** Get the Attributes container detailing the attributes
     * contained within this node.
     * @return the Attributes container detailing the attributes
     * contained within this node.
     */
    public Attributes getAttributes()
    {
	return attributes;
    }

    /** If the Attributes container (AKA nodeType) has been increased
     * in size by adding one or more Attribute fields to it, this
     * method will resize the internal values array to match.  The new
     * value fields will be initialized to "unknown" or NaN.  If the
     * Attributes container is smaller than the values array, this
     * method will throw an exception.
     * @throws RuntimeException if the Attributes container holds less
     * Attribute fields than the current values array size.
     */
    public void addValues()
    {
	final int attrCount = attributes.attributeCount();
	// Don't extend by negative numbers, that's shrinking!
	if (values.length > attrCount)
	    throw new RuntimeException("Tried to extend by <"
				       +(attrCount-values.length)+">");
	else if (values.length < attrCount)
	{
	    final double[] newValues = new double[attrCount];
	    // Copy the existing values.
	    System.arraycopy(values, 0, newValues, 0, values.length);
	    // Set the new values to "unknown".  This assumes that the
	    // new values are at the end of the array, which they
	    // should be.
	    Arrays.fill(newValues, values.length, newValues.length, Double.NaN);
	    values = newValues;
	}
	checkLengths();
    }
    
    /** Removes the value from the values array at the specified index
     * and shrinks the values array accordingly.  This Node's
     * Attributes container (AKA nodeType) must be exactly one element
     * smaller than this Node's values array.
     * @param index an integer specifying which element of the values array to remove.
     * @throws RuntimeException if the user attemps to remove other
     * than exactly one element, this could happen if the Attributes
     * container is not exactly one element smaller than the values
     * array.  Also throws if the index is out of bounds.
     */
    public void removeValue(int index)
    {
	final int attrCount = attributes.attributeCount();
	if (values.length != attrCount+1)
	    throw new RuntimeException("Must remove one value at a time, tried to remove <"
				       +(values.length-attrCount)+">");
	if (index < 0 || index > values.length-1)
	    throw new RuntimeException("Tried to remove invalid index <"+index+">");
	if (index == attributes.getKeyIndex())
	    throw new RuntimeException("Tried to remove KEY index <"+index+">");
	final double[] newValues = new double[attrCount];
	// Copy the existing values before (and not including) index.
	System.arraycopy(values, 0, newValues, 0, index);
	// Copy the existing values after (and not including) index.
	System.arraycopy(values, index+1, newValues, index, newValues.length-index);
	values = newValues;
	checkLengths();
    }

    /** Get the integer index of a single value Attribute within this
     * container.
     * @param attribute the attribute name to lookup in this container.
     * @return the integer index of a single attribute within this container.
     * @throws RuntimeException if the attribute isn't contained
     * within this node.
     */
    public int getAttributeIndex(String attribute)
    {
	return attributes.getAttributeIndex(attribute);
    }

    /** Get the value associated with the attribute field at the
     * supplied index.
     * @param index the integer index representing the attribute field
     * to lookup.
     * @return the double value associated with the attribute field at
     * the supplied index.
     */
    public double getValue(int index)
    {
	checkLengths();
	return values[index];
    }

    /** Return whether the value associated with the attribute field
     * at the supplied index is missing.
     * @param index the integer index representing the attribute field
     * to lookup.
     * @return whether the value associated with the attribute field
     * at the supplied index is missing.
     */
    public boolean isMissing(int index)
    {
	return Double.isNaN(getValue(index));
    }

    /** Get the value associated with the named attribute field.
     * @param fieldName the String name representing the attribute
     * field to lookup.
     * @return the double value associated with the named attribute field.
     * @throws NullPointerException if the attribute isn't contained
     * within this node.
     */
    public double getValue(String fieldName)
    {
	return getValue(getAttributeIndex(fieldName));
    }

    /** Get whether the value associated with the named attribute
     * field is "missing".
     * @param attribute the String name representing the attribute
     * field to lookup.
     * @return the whether the value associated with the named
     * attribute field is missing.
     * @throws RuntimeException if the attribute isn't contained
     * within this node.
     */
    public boolean isMissing(String attribute)
    {
	return isMissing(getAttributeIndex(attribute));
    }

    /** Gets all of the values associated with this node.
     * @return all of the values associated with this node.
     */
    public double[] getValues()
    {
	checkLengths();
	return values.clone();
    }
    
    /** Sets the value associated with the attribute field at the
     * supplied index.  Note you can only change the key field if it's
     * currently unknown, i.e. NaN.
     * @param index the integer index of the attribute field.
     * @param v the double value to set.
     * @throws ArrayIndexOutOfBoundsException if the index is not
     * within the value array bounds.
     * @throws RuntimeException if the index is the key index and the
     * key field is not unknown, i.e. NaN.
     */
    public void setValue(int index, double v)
    {
	checkLengths();
	if (index == attributes.getKeyIndex()
	    && values[index] != v && !Double.isNaN(values[index]))
	    throw new RuntimeException("Cannot change Key field from "
				       + values[index] + " to " + v);
	values[index] = v;
    }
    
    /** Sets the value associated with the named attribute field.
     * Note you can only change the key field if it's currently
     * unknown, i.e. NaN.
     * @param attribute the name of the attribute field being set.
     * @param v the double value to set.
     * @throws RuntimeException if the attribute isn't contained within this node.
     * @throws RuntimeException if the index is the key index and the
     * key field is not unknown, i.e. NaN.
     */
    public void setValue(String attribute, double v)
    {
	setValue(getAttributeIndex(attribute), v);
    }
    
    /** Sets all of the values for this node.  Note you can only
     * change the key field if it's currently unknown, i.e. NaN.
     * @param v an array of double to be copied into this node's
     * values; the supplied array's size must match the number of
     * attributes in this node (not including the key attribute.)
     * @throws RuntimeException if the key field differs and the old
     * key field is not unknown, i.e. NaN.
     */
    public void setValues(double[] v)
    {
	checkLengths();
	final int keyIndex = attributes.getKeyIndex();
	if (values[keyIndex] != v[keyIndex] && !Double.isNaN(values[keyIndex]))
	    throw new RuntimeException("Cannot change Key field from "
				       + values[keyIndex]
				       + " to " + v[keyIndex]);
	System.arraycopy(v, 0, values, 0, values.length);
    }

    /** Adds the supplied edge to this node; the Edge must not already
     * exist.
     * @param newEdge the Edge to be added to this node.
     * @throws RuntimeException if the Edge already exists.
     * @throws RuntimeException if the Edge's source Node does not
     * equal this.
     */
    public void addEdge(Edge newEdge)
    {
	if (!this.equals(newEdge.getSource()))
	    throw new RuntimeException("Edge source did not equal this");
	final String edgeTypeName = newEdge.getEdgeType().getName();
	HashMap<Node,Edge> map = etMap.get(edgeTypeName);
	if (map == null)
	{
	    map = new HashMap<Node,Edge>();
	    etMap.put(edgeTypeName, map);
	}

	edges = null;
	neighbors = null;
	
	if (map.put(newEdge.getDest(), newEdge) != null)
	    throw new RuntimeException("Added duplicate neighbor edge");
    }

    /** Removes the Edge to the supplied destination Node via the
     * supplied EdgeType name.  The resulting Edge must exist.
     * @param edgeTypeName a String representing the EdgeType of the connection.
     * @param destNode the destination Node for the Edge to be removed.
     * @return a reference to the removed Edge.
     * @throws RuntimeException if the Edge does not exist.
     */
    public Edge removeEdge(String edgeTypeName, Node destNode)
    {
	final HashMap<Node,Edge> map = etMap.get(edgeTypeName);
	if (map == null)
	    throw new RuntimeException("No Edges of type <"+edgeTypeName+">");
	final Edge edge = map.remove(destNode);
	if (edge == null)
	    throw new RuntimeException("No Edge to Node <"+destNode.getName()+">");
	edges = null;
	neighbors = null;
	return edge;
    }
    
    /** Get all the edges of this Node irrespective of EdgeType; the
     * order is unspecified.
     * @return all the edges of this Node irrespective of EdgeType.
     */
    public Edge[] getEdges()
    {
	// Assumes that container preserves the insertion order.
        if (edges == null)
	{
	    edges = new Edge[numEdges()];
	    int i = 0;
	    for (final Map<Node,Edge> nMap : etMap.values())
		for (final Edge e : nMap.values())
		    edges[i++] = e;
	}
	return edges;
    }

    /** Get all the edges of this Node whose destination Node is the
     * supplied node; the order is unspecified.
     * @param destinationNode the destination Node.
     * @return the edges of this Node in an array whose destination
     * Node is the supplied node.
     */
    public Edge[] getEdgesToNeighbor(final Node destinationNode)
    {
	return getEdgesToNeighbor(destinationNode.getType(), new NodeFilter()
	    { public final boolean accept(Node n) { return n==destinationNode; }});
    }

    /** Get all the edges of this Node whose destination Node is of
     * the supplied type; the order is unspecified.
     * @param destinationNodeType a String representation of the
     * destination Node type.
     * @return the edges of this Node in an array whose destination
     * Node is of the supplied type.
     */
    public Edge[] getEdgesToNeighbor(String destinationNodeType)
    {
	return getEdgesToNeighbor(destinationNodeType, new NodeFilter()
	    { public final boolean accept(Node n) { return true; }});
    }

    /** Get all the edges of this Node whose destination Node is of
     * the supplied type and which matches the supplied NodeFilter;
     * the order is unspecified.
     * @param destinationNodeType a String representation of the
     * destination Node type.
     * @param nf a NodeFilter to match against.     
     * @return the edges of this Node in an array whose destination
     * Node is of the supplied type and which matches the supplied
     * NodeFilter.
     */
    public Edge[] getEdgesToNeighbor(String destinationNodeType, NodeFilter nf)
    {
	final ArrayList<Edge> elist = new ArrayList<Edge>();
	for (final Edge e : getEdges())
	    if (e.getDest().getType().equals(destinationNodeType)
		&& nf.accept(e.getDest()))
		elist.add(e);
	return elist.toArray(new Edge[elist.size()]);
    }

    /** Get the Edges of this Node whose EdgeType name is the supplied
     * parameter; the order is unspecified.
     * @param edgeTypeName a String representing the Edge's EdgeType name.
     * @return the Edges of this Node whose EdgeType name is the
     * supplied parameter.
     */
    public Edge[] getEdgesByType(String edgeTypeName)
    {
	return getEdgesByType(edgeTypeName, new NodeFilter()
	    { public final boolean accept(Node n) { return true; }});
    }
    
    /** Get the Edges of this Node whose EdgeType name is the supplied
     * parameter and whose destination Nodes match the supplied
     * NodeFilter; the order is unspecified.
     * @param edgeTypeName a String representing the Edge's EdgeType name.
     * @return the Edges of this Node whose EdgeType name is the
     * supplied parameter and whose destination Nodes match the
     * supplied NodeFilter.
     */
    public Edge[] getEdgesByType(String edgeTypeName, NodeFilter nf)
    {
	final ArrayList<Edge> elist = new ArrayList<Edge>();
	for (final Edge e : getEdges())
	    if (e.getEdgeType().getName().equals(edgeTypeName)
		&& nf.accept(e.getDest()))
		elist.add(e);
	return elist.toArray(new Edge[elist.size()]);
    }
    
    /** Get the number of outgoing Edges.
     * @return the number of outgoing Edges.
     */
    public int getUnweightedDegree()
    {
        return getEdges().length;
    }

    /** Get the number of Edges which connect to a destination Node
     * of the supplied node type.
     * @param nodeType the String type name of the matching destination Nodes.
     * @return the number of Edges which connect to a destination Node
     * of the supplied node type.
     */
    public int getUnweightedDegree(String nodeType)
    {
	int i=0;
	for (final Edge e : getEdges())
	    if (e.getDest().getType().equals(nodeType))
		i++;
	return i;
    }

    /** Get the number of outbound Edges of the supplied EdgeType.
     * @param edgeType an EdgeType to match against.
     * @return the number of outbound Edges of the supplied EdgeType.
     */
    public int getUnweightedDegree(EdgeType edgeType)
    {
	int i=0;
	
        for (final Edge e : getEdges())
            if (e.getEdgeType().equals(edgeType))
                i++;
        return i;
    }

    /** Get the sum of all weights of outgoing Edges.
     * @return the sum of all weights of outgoing Edges.
     */
    public double getWeightedDegree()
    {
        double d=0;
        for(Edge e : getEdges())
            d += e.getWeight();
        return d;
    }

    /** Get the sum of all weights of Edges which connect to a
     * destination Node of the supplied node type.
     * @param nodeType the String type name of the matching destination Nodes.
     * @return the sum of all weights of Edges which connect to a
     * destination Node of the supplied node type.
     */
    public double getWeightedDegree(String nodeType)
    {
	double d=0;
	for (final Edge e : getEdges())
	    if (e.getDest().getType().equals(nodeType))
		d += e.getWeight();
	return d;
    }
    
    /** Get the sum of all weights of outbound Edges of the supplied EdgeType.
     * @param edgeType an EdgeType to match against.
     * @return the sum of all weights of outbound Edges of the supplied EdgeType.
     */
    public double getWeightedDegree(EdgeType edgeType)
    {
	double d=0;
	for (final Edge e : getEdges())
            if (e.getEdgeType().equals(edgeType))
		d += e.getWeight();
        return d;
    }

    /** Gets the adjacent nodes connected to this node irrespective of
     * the EdgeType; the order is unspecified.
     * @return the adjacent nodes in an array irrespective of the
     * EdgeType.
     */
    public Node[] getNeighbors()
    {
	// Assumes that container preserves the insertion order.
        if (neighbors == null)
	{
	    neighbors = new Node[numEdges()];
	    int i = 0;
	    for (final Map<Node,Edge> nMap : etMap.values())
		for (final Node n : nMap.keySet())
		    neighbors[i++] = n;
	}
        return neighbors;
    }

    /** Gets the adjacent nodes connected to this node through an Edge
     * with the supplied EdgeType name; the order is unspecified.
     * @param edgeTypeName the name of the EdgeType connecting
     * neighbor Nodes.
     * @return the adjacent nodes connected to this node through an
     * Edge with the supplied EdgeType name.
     */
    public Node[] getNeighbors(String edgeTypeName)
    {
      final Map<Node,Edge> map = etMap.get(edgeTypeName);
      return ( (map==null) ? new Node[0] : map.keySet().toArray(new Node[map.size()]) );
    }

    /** Gets the edge connecting this node to a neighbor node.
     * @param edgeTypeName the String name of the EdgeType to consider.
     * @param neighbor the destination node for the connecting edge.
     * @return the Edge connecting this node to the supplied node, or
     * null if none exists.
     */
    public Edge getEdge(String edgeTypeName, Node neighbor)
    {
	final Map<Node,Edge> map = etMap.get(edgeTypeName);
	if (map != null)
	    return map.get(neighbor);
	return null;
    }

    /** Gets the total number of edges for this node.
     * @return the total number of edges for this node.
     */
    public int numEdges()
    {
	int s = 0;
	for (final Map<Node,Edge> map : etMap.values())
	    s += map.size();
	return s;
    }

    /** Gets the number of Edges to neighboring Nodes with the
     * supplied EdgeType name.
     * @param edgeTypeName the EdgeType name Edges must have to be
     * included in this count.
     * @return the number of Edges to neighboring Nodes with the
     * supplied EdgeType name.
     */
    public int numEdges(String edgeTypeName)
    {
	final Map<Node,Edge> map = etMap.get(edgeTypeName);
	if (map == null)
	    return 0;
	else
	    return map.size();
    }
    
    /** Returns a hash code value for this object.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
	return hash;
    }

    /** Indicates whether some other object is "equal to" this one;
     * Nodes are equal if they share the same name and type.
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the
     * argument; {@code false} otherwise.
     */
    public boolean equals(Object o)
    {
	if (this == o)
	    return true;
        if (o instanceof Node)
	{
	    final Node n = (Node)o;
            return name.equals(n.getName())
		&& getType().equals(n.getType());
	}
        return false;
    }

    /** Specifies a natural ordering for Nodes; compare node types
     * first, then node names.
     * @param n the Node to compare this object to.
     * @return a natural ordering for Nodes; compare node types
     * first, then node names.
     */
    public int compareTo(Node n)
    {
	if (!this.getType().equals(n.getType()))
	    return this.getType().compareTo(n.getType());
	else
	    return this.getName().compareTo(n.getName());
    }

    /** Returns a String representation for this object.
     * @return a String representation for this object.
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("[Node(name=");
        sb.append(name).append(",idx=").append(index).append(')');
        if(attributes != null)
            sb.append("::").append(attributes.toString());
        sb.append(" links-to:");
	for (final Edge e : getEdges())
            sb.append(" {").append(e.getDest().getName()).append(',').append(e.getWeight()).append('}');
        sb.append(']');
        return sb.toString();
    }

    /** Gets a List of Edges to neighboring Nodes based on the
     * supplied EdgeType path.  Neighbors are defined as Nodes that
     * can be reached by following the supplied EdgeType path through
     * Nodes and connecting Edges with matching respective EdgesTypes.
     * Intervening Nodes will be visited at most once.  The returned
     * List of Edges are newly created Edges which directly connect
     * the initial and final Nodes in the path and whose weight is
     * calculated by multiplying the weights of the actual intervening
     * Edges.  If a particular neighbor can be reached by more than
     * one Edge path, those paths are collapsed by adding the weights
     * of the respective Edges.  In all cases, these new Edges are not
     * contained in the associated Graph object.
     * @param edgeTypePath a List<EdgeType> which specifies the
     * EdgeTypes that the intervening Edges along the path must
     * contain.
     * @param nf a NodeFilter which is applied against the final Node
     * (or neighbor); this Node must be accepted by the NodeFilter
     * before the neighbor is accepted into the returned list.
     * @return a List<Edge> where each Edge is a newly created Edge
     * that directly connects the initial and final Nodes in the path
     * and whose weight is calculated by multiplying the weights of
     * the actual intervening Edges.
     * @throws RuntimeException if the supplied EdgeType path's first
     * element's source nodetype does not match the nodetype of this
     * Node.
     */
    public List<Edge> getNeighbors(List<EdgeType> edgeTypePath, NodeFilter nf)
    {
	// Consistency check: path length must be at least 1.
	if (edgeTypePath.size() < 1)
	    throw new RuntimeException("Size of path must be at least 1");
	
	final EdgeType firstEdgeType = edgeTypePath.get(0);

	// Consistency check: if our Attributes don't match the source
	// Attributes in the first element of the path, then punt.
	if (! getAttributes().getName().equals(firstEdgeType.getSourceType()))
	    throw new RuntimeException("Mismatch between this <"
				       + getAttributes().getName()
				       + "> and path first element <"
				       + firstEdgeType.getSourceType() +">");

	// Initialize testPaths with paths of length one.  Each test
	// path starts out corresponding to one of the outbound Edges
	// on this Node.
	final List<List<Edge>> testPaths = new ArrayList<List<Edge>>();
	for (final Edge edge : getEdgesByType(firstEdgeType.getName()))
	{
	    final ArrayList<Edge> newList = new ArrayList<Edge>();
	    newList.add(edge);
	    testPaths.add(newList);
	}

	// This is the list of paths kept and returned from this
	// method.  It's initially empty.
	final List<List<Edge>> keepPaths = new ArrayList<List<Edge>>();
	
	doRecurse(edgeTypePath, 1, keepPaths, testPaths, nf);
	
	// Now collapse each successful path in keepPaths into one
	// Edge that directly connects the initial and final
	// (neighboring) Nodes with a weight calculated by multiplying
	// the weights of the intervening actual Edges.  NOTE: these
	// new Edges are not in the Graph.

	// Create a suitable EdgeType (also not in the Graph.)
	final StringBuilder sb = new StringBuilder("TempEdgeType");
	for (final EdgeType et : edgeTypePath)
	    sb.append(':').append(et.getName());
	final EdgeType lastEdgeType = edgeTypePath.get(edgeTypePath.size()-1);
	final EdgeType newET = new EdgeType(sb.toString(),
					    firstEdgeType.getSourceType(),
					    lastEdgeType.getDestType());
	final List<Edge> resultingPaths = new ArrayList<Edge>();

	for (final List<Edge> path : keepPaths)
	{
	    double weight = 1.0;
	    for (final Edge e : path)
		weight *= e.getWeight();
	    final Node sourceNode = path.get(0).getSource();
	    final Node destNode = path.get(path.size()-1).getDest();
	    resultingPaths.add(new Edge(newET, sourceNode, destNode, weight));
	}
	
	// Now for paths that arrive at the same neighbor, collapse
	// these into one Edge by adding their respective weights.
	// Since the EdgeType and source Node are identical in all
	// entries, this sort will reorder Edges such that all those
	// with identical destination Nodes are adjacent.  Merge these
	// adjacent identical entries.
	Collections.sort(resultingPaths);
	Edge prevEdge = null;
	for (final Iterator<Edge> it = resultingPaths.iterator(); it.hasNext(); )
	{
	    final Edge currEdge = it.next();
	    if (currEdge.equals(prevEdge))
	    {
		// If the previous Edge and current Edge share the
		// same EdgeType, source and destination Nodes then
		// merge the weights and remove the current Edge.
		prevEdge.addWeight(currEdge.getWeight());
		it.remove();
	    }
	    else
		prevEdge = currEdge;
	}
	
	return resultingPaths;
    }
    
    /** Same as {@link #getNeighbors(List,NodeFilter)} except that the
     * NodeFilter always accepts Nodes, nothing is filtered out.
     */
    public List<Edge> getNeighbors(List<EdgeType> edgeTypePath)
    {
	return getNeighbors(edgeTypePath, new NodeFilter()
	    { public final boolean accept(Node n) { return true; }});
    }

    // Helper method for getNeighbors(List,NodeFilter).  Recurse
    // through the supplied EdgeType path creating actual instantiated
    // Edge paths to our neighbors.  On initial entry, keepPaths
    // should be empty, testPaths should contain a List of Edge paths
    // of length one corresponding to the outgoing Edges from the
    // start Node (i.e. "this") and edgeTypePathIndex should be 1.
    // Generated Edge paths are kept iff the final Node
    // (i.e. neighbor) is accepted by the NodeFilter.
    static private void doRecurse(List<EdgeType> edgeTypePath,
				  int edgeTypePathIndex,
				  List<List<Edge>> keepPaths,
				  List<List<Edge>> testPaths,
				  NodeFilter nf)
    {
	for (final List<Edge> edgePath : testPaths)
	{
	    if (edgeTypePathIndex < edgeTypePath.size())
	    {
		// Continue searching.  Build longer paths using the
		// existing path and the EdgeType at the current
		// index.  Then recurse using an index one greater.
		doRecurse(edgeTypePath, edgeTypePathIndex+1, keepPaths,
			  buildLongerActualPaths(edgePath,
						 edgeTypePath.get(edgeTypePathIndex)),
			  nf);
	    }
	    else
	    {
		// Otherwise we've successfully consumed all
		// EdgeTypes, so we've found a path to a neighbor.
		// Keep track of this path if the neighbor is accepted
		// by the NodeFilter.
		if (nf.accept(edgePath.get(edgePath.size()-1).getDest()))
		    keepPaths.add(edgePath);
	    }
	}
    }

    // Private helper method for doRecurse().  Given a List<Edge> (AKA
    // a path among Nodes) and given an EdgeType, find all paths one
    // element longer where the new (last) element matches the
    // supplied EdgeType.  Nodes are visited at most once in each Edge
    // path.
    static private List<List<Edge>> buildLongerActualPaths(List<Edge> pathSoFar,
							   EdgeType nextEdgeType)
    {
	final List<List<Edge>> returnList = new ArrayList<List<Edge>>();
	final Edge lastEdge = pathSoFar.get(pathSoFar.size()-1);

	// On the destination Node of the last Edge in pathSoFar, loop
	// over its the outgoing Edges which have a matching EdgeType.
	nextNewEdge:
	for (final Edge newEdge : lastEdge.getDest().getEdgesByType(nextEdgeType.getName()))
	{
	    final Node newDest = newEdge.getDest();
	    // If we've visited this Node already, skip it.
	    // FIXME: in theory prev.Dest == next.Source, so we could
	    // skip checking both sides except for the first or last
	    // Edge.  But as a sanity check we do both.
	    for (final Edge oldEdge : pathSoFar)
		if (newDest.equals(oldEdge.getSource())
		    || newDest.equals(oldEdge.getDest()))
		    continue nextNewEdge;

	    // Okay it's a new Node!  Copy pathSoFar, add the new Edge
	    // and add the new path to the return list.
	    final List<Edge> listCopy = new ArrayList<Edge>(pathSoFar);
	    listCopy.add(newEdge);
	    returnList.add(listCopy);
	}
	
	return returnList;
    }

}
