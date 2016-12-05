/**
 * Graph.java
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

package netkit.graph;

import netkit.util.GraphMetrics;
import netkit.util.HistogramCategorical;

import java.util.*;

/** This class represents the relational network in memory.  It
 * contains Node objects connected via Edge objects.  The Graph object
 * also keeps track of valid node types represented by Attributes
 * containers and valid edge types represented by the EdgeType class.
 * @see Node
 * @see Edge
 * @see Attributes
 * @see EdgeType
 * @see netkit.graph.io.SchemaReader
 * @see netkit.graph.io.SchemaWriter
 * 
 * @author Kaveh R. Ghazi
 */
public final class Graph implements Cloneable
{
    // Helper class to encapsulate keeping track of Attributes and
    // related bits for this Graph.
    private final class NodeTypeHolder
    {
	// This is the Attributes container (AKA nodeType).
	private final Attributes attributes;
	
	// We use a LinkedHashMap to preserve the order in which
	// elements were added to this container.  The key in this map
	// is the node name and the value is the actual Node object.
	// This let's us easily split up node types.
	private final Map<String,Node> nodeMap = new LinkedHashMap<String,Node>();
	
	// We use this to store the meta info objects keyed on the
	// Attribute (or field) name within the Attributes container.
	private final Map<String,AbstractAttributeMetaInfo> amiMap
	    = new HashMap<String,AbstractAttributeMetaInfo>();
	

	// Construct/initialize an object of this type.
	private NodeTypeHolder(Attributes a)
	{
	    this.attributes = a;
	}

	// Return a hash code for this object.
	public int hashCode()
	{
	    return attributes.hashCode();
	}
	
	// Gets the Attributes object for this node type.
	private Attributes getAttributes()
	{
	    return attributes;
	}

	// Gets all Nodes of this node type.
	private Collection<Node> getNodes()
	{
	    return nodeMap.values();
	}

	// Gets the Node with the supplied node name.
	private Node getNode(String nodeName)
	{
	    return nodeMap.get(nodeName);
	}
	
	// Gets the AMI object for a supplied fieldName.  Create and
	// cache it if it doesn't already exist, otherwise return the
	// cached reference.
	private AbstractAttributeMetaInfo getAMI(String fieldName)
	{
	    AbstractAttributeMetaInfo ami = amiMap.get(fieldName);
	    if (ami == null)
	    {
		final Attribute attr = attributes.getAttribute(fieldName);
		if (attr instanceof AttributeCategorical)
		    ami = new AttributeCategoricalMetaInfo((AttributeCategorical)attr, attributes, Graph.this);
		else
		    ami = new AttributeMetaInfo(attr, attributes, Graph.this);
		    
		amiMap.put(fieldName, ami);
	    }
	    return ami;
	}
	
	// Return the number of Nodes of this node type.
	private int numNodes()
	{
	    return nodeMap.size();
	}
	
	// Create and save a Node of this node type.
	private Node addNode(String nodeName)
	{
	    final Node n = new Node(nodeName, attributes, numNodes());
	    if (nodeMap.put(nodeName, n) != null)
		throw new RuntimeException("Node <"+n+"> already exists!");
	    return n;
	}
	
	// Clear the cache of meta info objects for this node type.
	private void clearMetaInfo()
	{
	    amiMap.clear();
	}
	
	// Clear the cached meta info for the supplied fieldName.
	private void clearMetaInfo(String fieldName)
	{
	    amiMap.remove(fieldName);
	}
	
	// Clear out all Nodes (and meta info) for this node type.
	private void clearNodes()
	{
	    clearMetaInfo();
	    nodeMap.clear();
	}
    }

    // Helper class to encapsulate keeping track of EdgeTypes, Edges
    // and related bits for this Graph.
    private static final class EdgeTypeHolder
    {
	private final EdgeType edgeType;
	private final Set<Edge> eSet = new HashSet<Edge>();
	
	// Construct/initialize an object of this type.
	private EdgeTypeHolder(EdgeType et)
	{
	    this.edgeType = et;
	}
	
	// Return a name for this edge type.
	private String getName()
	{
	    return edgeType.getName();
	}
	
	// Return a hash code for this object.
	public int hashCode()
	{
	    return edgeType.hashCode();
	}

	// Gets the EdgeType object.
	private EdgeType getEdgeType()
	{
	    return edgeType;
	}

	// Gets all Edges of this EdgeType.
	private Collection<Edge> getEdges()
	{
	    return eSet;
	}
	
	// Return the number of Edges of this EdgeType.
	private int numEdges()
	{
	    return eSet.size();
	}
	
	// Add an Edge to this holder object.  If the Edge already
	// exists then throw.
	private Edge addEdge(Node source, Node dest, double weight)
	{
	    final Edge newEdge = new Edge(edgeType, source, dest, weight);
	    if (!eSet.add(newEdge))
		throw new RuntimeException("Edge already exists!");
	    source.addEdge(newEdge);
	    return newEdge;
	}

	// Remove all edges of this EdgeType from the Graph.
	private void removeEdges()
	{
	    final String edgeTypeName = edgeType.getName();
	    for (final Edge e : eSet)
		e.getSource().removeEdge(edgeTypeName, e.getDest());
	    eSet.clear();
	}

	// Remove the Edge connecting the supplied Nodes.  The Edge
	// must exist.
	private Edge removeEdge(Node source, Node dest)
	{
	    final Edge e = source.removeEdge(edgeType.getName(), dest);
	    if (!eSet.remove(e))
		throw new RuntimeException("No Edge from Node <"+source.getName()
					   +"> to Node <"+dest.getName()+">");
	    return e;
	}
    }
    
    // We use a LinkedHashMap to preserve the order in which elements
    // were added to this container.  The key in this map is a string
    // naming the node type, the value is a NodeTypeHolder class.
    private final Map<String,NodeTypeHolder> ntMap
	= new LinkedHashMap<String,NodeTypeHolder>();
    
    // We use a LinkedHashMap to preserve the order in which elements
    // were added to this container.  The key in this map is a string
    // representing the name of the valid EdgeTypes in this graph.
    // The values are EdgeTypeHolder classes.
    private final Map<String,EdgeTypeHolder> ethMap
	= new LinkedHashMap<String,EdgeTypeHolder>();


    private GraphMetrics metrics = null;

    private transient Node[] nodes;
    private transient Edge[] edges;
    
    public Graph clone() {
      Graph newG = new Graph();
      newG.edges = this.edges;
      newG.nodes = this.nodes;
      newG.metrics = metrics;
      newG.ntMap.putAll(ntMap);
      newG.ethMap.putAll(ethMap);
      
      return newG;
    }

    /**
     * Create a sub-graph consisting only of the given nodes
     * and the edges between those nodes.  The new sub-graph
     * will have a new set of nodes and edges, but internal
     * node attribute-vectors are all the same as are graph
     * meta information such as attributes and edgeType objects
     * 
     * @param nodeSet
     * @return new Graph.
     */
    public Graph subGraph(final Collection<Node> nodeSet) {
      final Graph newG = new Graph();
      newG.metrics = null;
      
      for(Attributes a : this.getAllAttributes())
        newG.addAttributes(a);
      for(EdgeType et : this.getEdgeTypes())
        newG.addEdgeType(et);
      
      final Map<Node,Node> map = new HashMap<Node,Node>();
      
      for(final Node n : nodeSet)
      {
        final NodeTypeHolder nth = newG.ntMap.get(n.getAttributes().getName());
        final Node newN = n.copy(nth.numNodes());
        final String nodeName = newN.getName();
        nth.nodeMap.put(nodeName, newN);
        map.put(n,newN);
      }
      for(final Map.Entry<Node,Node> pair : map.entrySet())
      {
        for(final Edge e : pair.getKey().getEdges())
        {
          final Node dst = map.get(e.getDest());
          if(dst != null) {
            newG.addEdge(e.getEdgeType(),pair.getValue(),dst,e.getWeight());
          }
        }
      }
      
      return newG;
    }

    /** Get the metrics encapsulating statistics about this graph.
     * @return a GraphMetrics object which contains graph metrics about this graph.
     */
    public GraphMetrics getMetrics()
    {
    if(metrics == null)
        metrics = new GraphMetrics(this);
    return metrics;
    }

    /** Adds the supplied Attributes container (or node type) to this
     * Graph; Attributes must be added to the Graph before Nodes that
     * utilize them can be added.
     * @param a an Attributes container to add.
     * @throws RuntimeException if an Attributes container with the
     * same name has already been added to this Graph.
     */
    public void addAttributes(Attributes a)
    {
	final String name = a.getName();
	if (ntMap.containsKey(name))
	    throw new RuntimeException("Graph already contains <"+name+">");
	ntMap.put(name, new NodeTypeHolder(a));
    }
    
    /** Gets an array of Attributes containing the node types in this
     * graph.
     * @return an array of Attributes containing the node types in
     * this graph.
     */
    public Attributes[] getAllAttributes() 
    {
	final Attributes[] array = new Attributes[ntMap.size()];
	int i=0;
	for (final NodeTypeHolder nt : ntMap.values())
	    array[i++] = nt.getAttributes();
	return array;
    }
    
    /** Get the Attributes container matching the string provided.
     * @param nodeType the string name identifying the node type to search for.
     * @return the Attributes container matching the provided string
     * node type or null if no Attributes container with a matching
     * name is found.
     */
    public Attributes getAttributes(String nodeType)
    {
	final NodeTypeHolder nt = ntMap.get(nodeType);
	return (nt == null) ? null : nt.getAttributes();
    }

    /** Adds an Attribute to the Attributes container represented by
     * the supplied nodeType name.  Synchronize any existing Nodes by
     * extending their internal values array.
     * @param nodeType a String representing the Attributes container
     * to be extended.
     * @param a an Attribute field to be added to the container.
     * @throws NullPointerException if the specified nodeType doesn't
     * exist in this Graph.
     */
    public void addAttribute(String nodeType, Attribute a)
    {
	final NodeTypeHolder nt = ntMap.get(nodeType);
	nt.getAttributes().add(a);
	
	// Synchronize all Nodes of this type to have the new field.
	for (final Node node : nt.getNodes())
	    node.addValues();
    }

    /** Remove an Attribute at the supplied index from the Attributes
     * container represented by the supplied nodeType name.
     * Synchronize Nodes in this Graph by removing the value at the
     * supplied index.  Also invalidate any cached AttributeMetaInfo
     * objects for the supplied Attribute.  Note you cannot remove a
     * KEY Attribute field.
     * @param nodeType a String representing the Attributes container.
     * @param index an integer field index to be removed.
     * @throws NullPointerException if the index is out of bounds or
     * if the supplied nodeType doesn't exist.
     * @throws RuntimeException if the supplied index represents the
     * KEY field.
     */
    public void removeAttribute(String nodeType, int index)
    {
	final NodeTypeHolder nt = ntMap.get(nodeType);
	final Attributes attrs = nt.getAttributes();
	final String fieldName = attrs.getAttribute(index).getName();

	attrs.remove(index);

	// Synchronize all Nodes of this type to remove the field.
	for (final Node node : nt.getNodes())
	    node.removeValue(index);

	// Now remove AttributeMetaInfo entries (if any) for the
	// specified Attribute.
	nt.clearMetaInfo(fieldName);
    }

    /** Gets an array of String containing the list of names of the
     * node types in this graph.
     * @return an array of String containing the list of names of the
     * node types in this graph.
     */
    public String[] getNodeTypes()
    {
	return ntMap.keySet().toArray(new String[ntMap.size()]);
    }
    
    /** Gets an array of String containing the list of names of all EdgeTypes.
     * @return an array of String containing the list of names of all
     * the EdgeTypes.
     */
    public String[] getEdgeTypeNames()
    {
	return ethMap.keySet().toArray(new String[ethMap.size()]);
    }

    /** Gets an array of String containing the list of names of the
     * EdgeTypes whose source node type is the supplied parameter.
     * @param sourceNodeType the String name of the source node type.
     * @return an array of String containing the list of names of the
     * EdgeTypes whose source node type is the supplied parameter.
     */
    public String[] getEdgeTypeNames(String sourceNodeType)
    {
	final ArrayList<String> alist = new ArrayList<String>();
	for (final EdgeTypeHolder eth : ethMap.values())
	    if (eth.getEdgeType().getSourceType().equals(sourceNodeType))
		alist.add(eth.getName());
	return alist.toArray(new String[alist.size()]);
    }

    /** Gets an array of EdgeTypes containing the list of the
     * EdgeTypes whose source node type is the supplied parameter.
     * @param sourceNodeType the String name of the source node type.
     * @return an array of EdgeTypes containing the list of the
     * EdgeTypes whose source node type is the supplied parameter.
     */
    public EdgeType[] getEdgeTypes(String sourceNodeType)
    {
	final ArrayList<EdgeType> alist = new ArrayList<EdgeType>();
	for (final EdgeTypeHolder eth : ethMap.values())
	    if (eth.getEdgeType().getSourceType().equals(sourceNodeType))
		alist.add(eth.getEdgeType());
	return alist.toArray(new EdgeType[alist.size()]);
    }

    /** Gets an array of EdgeType containing the list of 
     * EdgeTypes whose source and destination node types are the
     * supplied parameters.
     * @param destNodeType the String name of the destination node type.
     * @return an array of EdgeType containing the list of
     * EdgeTypes whose source and destination node types are the
     * supplied parameters.
     */
    public EdgeType[] getEdgeTypes(String sourceNodeType, String destNodeType)
    {
  final ArrayList<EdgeType> alist = new ArrayList<EdgeType>();
  for (final EdgeTypeHolder eth : ethMap.values())
      if (eth.getEdgeType().getSourceType().equals(sourceNodeType)
    && eth.getEdgeType().getDestType().equals(destNodeType))
    alist.add(eth.getEdgeType());
  return alist.toArray(new EdgeType[alist.size()]);
    }
    

    /** Gets an array of String containing the list of names of the
     * EdgeTypes whose source and destination node types are the
     * supplied parameters.
     * @param destNodeType the String name of the destination node type.
     * @return an array of String containing the list of names of the
     * EdgeTypes whose source and destination node types are the
     * supplied parameters.
     */
    public String[] getEdgeTypeNames(String sourceNodeType, String destNodeType)
    {
  final ArrayList<String> alist = new ArrayList<String>();
  for (final EdgeTypeHolder eth : ethMap.values())
      if (eth.getEdgeType().getSourceType().equals(sourceNodeType)
    && eth.getEdgeType().getDestType().equals(destNodeType))
    alist.add(eth.getName());
  return alist.toArray(new String[alist.size()]);
    }
    
    /** Get the EdgeType matching the provided edge type name.
     * @param edgeType the string edge type name to lookup.
     * @return the EdgeType matching the provided string, or null if not found.
     */
    public EdgeType getEdgeType(String edgeType)
    {
  final EdgeTypeHolder eth = ethMap.get(edgeType);
  return (eth == null) ? null : eth.getEdgeType();
    }
    
    /** Gets an array of EdgeType containing the list of EdgeTypes
     * in this graph.
     * @return an array of EdgeType containing the list of EdgeTypes
     * in this graph.
     */
    public EdgeType[] getEdgeTypes()
    {
      final ArrayList<EdgeType> alist = new ArrayList<EdgeType>();
      for (final EdgeTypeHolder eth : ethMap.values())
          alist.add(eth.getEdgeType());
      return alist.toArray(new EdgeType[alist.size()]);
    }

    /** Add the supplied EdgeType to this Graph; EdgeTypes must be
     * added to the Graph before Edges that utilize them can be added.
     * @param et the EdgeType to add.
     * @throws RuntimeException if an EdgeType with the same name has
     * already been added to this Graph; or if the EdgeType's source
     * or destination node types are invalid for this Graph.
     */
    public void addEdgeType(EdgeType et)
    {
	final String edgeTypeName = et.getName();
	if (ethMap.containsKey(edgeTypeName))
	    throw new RuntimeException("Duplicate EdgeType <" + edgeTypeName + "> added!");
	if (!ntMap.containsKey(et.getSourceType()))
	    throw new RuntimeException("Invalid EdgeType source <" +et.getSourceType());
	if (!ntMap.containsKey(et.getDestType()))
	    throw new RuntimeException("Invalid EdgeType destination <" +et.getDestType());
	ethMap.put(edgeTypeName, new EdgeTypeHolder(et));
    }
    
    /** Remove the supplied EdgeType from this Graph.  If force is
     * false and Edges using the supplied EdgeType exist, then throw
     * an exception.  Otherwise, remove the offending components.
     * @param edgeTypeName a String representing the EdgeType to be
     * removed.
     * @param force if false and Edges using the supplied EdgeType
     * exist, then throw.
     * @throws NullPointerException if the supplied EdgeType does not
     * exist in this Graph.
     * @throws RuntimeException if force is false and Edges using the
     * supplied EdgeType exist.
     */
    public void removeEdgeType(String edgeTypeName, boolean force)
    {
	final EdgeTypeHolder eth = ethMap.get(edgeTypeName);
	
	// If edges using the supplied EdgeType exist, then ...
	if (eth.numEdges() > 0)
	    // If we're forcing then remove them, otherwise, throw.
	    if (force)
	    {
		// Now remove the matching Edges from this Graph and clear the cache.
		eth.removeEdges();
		// Invalidate the Edge array cache.
		edges = null;
	    }
	    else
		throw new RuntimeException("Edges exist with EdgeType <"+edgeTypeName+">");


	// Assert that we were able to remove all the appropriate Edges.
	if (eth.numEdges() > 0)
	    throw new RuntimeException("Couldn't remove all matching Edges!");
	
	// Now remove the supplied EdgeType from this Graph.
	ethMap.remove(edgeTypeName);
    }

    /** Gets the total number of nodes in this graph.
     * @return the total number of nodes in this graph.
     */
    public int numNodes()
    {
	int s = 0;
	for (final NodeTypeHolder nt : ntMap.values())
	    s += nt.numNodes();
	return s;
    }

    /** Gets the number of nodes in the graph for the supplied node type.
     * @return the number of nodes in the graph for the supplied node type.
     * @throws NullPointerException if the supplied node type doesn't exist.
     */
    public int numNodes(String nodeType)
    {
	return ntMap.get(nodeType).numNodes();
    }
    
    /** Gets the total number of edges in this graph.
     * @return the total number of edges in this graph.
     */
    public int numEdges()
    {
	int s = 0;
	for (final EdgeTypeHolder eth : ethMap.values())
	    s += eth.numEdges();
	return s;
    }

    /** Gets the number of Edges in this graph for the supplied
     * EdgeType name.
     * @param edgeTypeName the EdgeType name Edges must have to be
     * included in this count.
     * @return the number of Edges in this graph for the supplied
     * EdgeType name.
     */
    public int numEdges(String edgeTypeName)
    {
	final EdgeTypeHolder eth = ethMap.get(edgeTypeName);
	return (eth == null) ? 0 : eth.numEdges();
    }

    /** Adds a new Node to the Graph using the supplied node name and
     * Attributes container.
     * @param nodeName the string name for the new Node.
     * @param a the Attributes container for the new Node.
     * @return the newly created Node.
     * @throws NullPointerException if the supplied Attributes
     * container doesn't exist in the Graph.
     * @throws RuntimeException if a Node matching the supplied
     * parameters already exists in this Graph.
     */
    public Node addNode(String nodeName, Attributes a) 
    {
	nodes = null;
	return ntMap.get(a.getName()).addNode(nodeName);
    }

    /** Remove all Nodes in this Graph whose Attributes container (AKA
     * nodeType) matches the supplied nodeType.  If force is false and
     * Edges connecting Nodes using the supplied nodeType exist, then
     * throw an exception.  Otherwise when force is true remove the
     * offending components as well.  Also remove any cached
     * AttributeMetaInfo objects for the supplied nodeType.
     * @param nodeType a String representing the nodeType of the Nodes
     * to be removed.
     * @param force if false and components using the supplied
     * nodeType exist, then throw.  Otherwise, remove the components.
     * @throws RuntimeException if the supplied nodeType does not
     * exist in this Graph.  Also throws if force is false and
     * components using the supplied nodeType exist.
     */
    public void removeNodes(String nodeType, boolean force)
    {
	final NodeTypeHolder nt = ntMap.get(nodeType);
	
	// If the nodeType doesn't exist, then throw.
	if (nt == null)
	    throw new RuntimeException("Can't find nodeType <"+nodeType+">");

	// Look for EdgeTypes connecting Nodes of type nodeType.
	for (final EdgeTypeHolder eth : ethMap.values())
	{
	    final EdgeType et = eth.getEdgeType();
	    if (et.getSourceType().equals(nodeType)
		|| et.getDestType().equals(nodeType))
	    {
		// If we find any Edges matching this EdgeType...
		if (eth.numEdges() > 0)
		    // If we're forcing then remove them, otherwise throw.
		    if (force)
		    {
			// Only remove the Edges, not the EdgeType itself!
			eth.removeEdges();
			// Invalidate the Edge array cache.
			edges = null;
		    }
		    else
			throw new RuntimeException("Edges already exist which connect Nodes of type <"
						   +nodeType+">");
	    }
	}
	// Now clear out matching Nodes, not the nodeType itself!
	nt.clearNodes();
	// Invalidate the nodes array cache.
	nodes = null;
    }

    /** Remove the supplied Attributes container (AKA nodeType) from
     * this Graph.  If force is false and components using the
     * supplied nodeType exist, then throw an Exception.  Otherwise
     * when force is true remove the offending Graph components.
     * These components might include Nodes, Edges, EdgeTypes and
     * AttributeMetaInfo objects.
     * @param nodeType a String representing the nodeType to be removed.
     * @param force if false and components using the supplied
     * nodeType exist, then throw.  Otherwise, remove the components.
     * @throws NullPointerException if the supplied nodeType does not
     * exist in this Graph.
     * @throws RuntimeException if force is false and components using
     * the supplied nodeType exist.
     */
    public void removeAttributes(String nodeType, boolean force)
    {
	final NodeTypeHolder nt = ntMap.get(nodeType);
	
	// If nodes using the supplied node type exist, then...
	if (nt.numNodes() > 0)
	    // If we're forcing then remove them, otherwise, throw.
	    if (force)
		removeNodes(nodeType, force);
	    else
		throw new RuntimeException("Nodes already exist of type <"
					   +nodeType+">");
	
	// Look for EdgeTypes connecting Nodes of type nodeType.
	for (final String edgeTypeName : getEdgeTypeNames())
	{
	    final EdgeType et = getEdgeType(edgeTypeName);
	    if (et.getSourceType().equals(nodeType)
		|| et.getDestType().equals(nodeType))
	    {
		// If we're not forcing and we find any EdgeTypes
		// using the supplied nodeType, then throw.
		if (!force)
		    throw new RuntimeException("EdgeTypes already exist which connect Nodes of type <"
					       +nodeType+">");
		// Now remove the matching EdgeType from this Graph.
		removeEdgeType(edgeTypeName, force);
	    }
	}

	// Assert that we were able to remove all the appropriate Nodes.
	if (nt.numNodes() > 0)
	    throw new RuntimeException("Couldn't remove all matchines Nodes!");
	
	// Now remove the nodeType itself from this Graph.
	ntMap.remove(nodeType);
    }
    
    /** Gets the node corresponding to the supplied int index.
     * @param index an int indicating which node is being requested.
     * @return a Node object corresponding to the supplied int index.
     * @throws ArrayIndexOutOfBoundsException if the index is invalid.
     */
    public Node getNode(int index)
    {
	if (nodes == null)
	    getNodes();
        return nodes[index];
    }

    /** Gets the node coresponding to the supplied node name and node type.
     * @param nodeName the name of the Node to lookup.
     * @param nodeType the type of the Node to lookup.
     * @return the Node matching the supplied parameters, returns null
     * if the supplied node name doesn't exist.
     * @throws NullPointerException if the supplied type doesn't exist.
     */
    public Node getNode(String nodeName, String nodeType)
    {
	return ntMap.get(nodeType).getNode(nodeName);
    }
    
    /** Gets all of the nodes in the graph; the order is unspecified.
     * @return all of the nodes in the graph in an array ordered as
     * they were inserted into the Graph container.
     */
    public Node[] getNodes()
    {
        if(nodes == null)
	{
	    nodes = new Node[numNodes()];
	    int i = 0;
	    for (final NodeTypeHolder nt : ntMap.values())
		for (final Node node : nt.getNodes())
		    nodes[i++] = node;
	}
        return nodes.clone();
    }

    /** Gets all of the Nodes matching the supplied node type.
     * @param type a string node type name to match against.
     * @return an array of Nodes whose node type matches the supplied parameter.
     * @throws NullPointerException if the supplied node type doesn't exist.
     */
    public Node[] getNodes(String type)
    {
	return getNodes(type, new NodeFilter()
	    { public final boolean accept(Node n) { return true; }});
    }
    
    /** Gets all of the Nodes matching the supplied node type and
     * which also are accepted by the supplied NodeFilter.
     * @param nodeType a String node type name to match against.
     * @param nf a NodeFilter to match against.
     * @return an array of Nodes whose node type matches the supplied parameters.
     * @throws NullPointerException if the supplied node type doesn't exist.
     */
    public Node[] getNodes(String nodeType, NodeFilter nf)
    {
        final ArrayList<Node> nodes = new ArrayList<Node>();
        for (final Node n : ntMap.get(nodeType).getNodes())
            if (nf.accept(n))
                nodes.add(n);
        return nodes.toArray(new Node[nodes.size()]);
    }
    
    /** Gets all of the Nodes whose Attributes container matches the supplied parameter.
     * @param attrs the Attributes container to match against.
     * @return an array of Nodes whose node type matches the supplied parameter.
     * @throws NullPointerException if the Attributes is not in this Graph.
     */
    public Node[] getNodes(Attributes attrs)
    {
	final String type = attrs.getName();
	return getNodes(type);
    }
    
    /** Add the supplied edge to the graph; if the edge's nodes are
     * already connected, then simply add the edge's weight to the
     * existing one.
     * @param e the edge to be added to the graph.
     * @deprecated Use of this method is deprecated, use {@link
     * #addEdge(EdgeType,Node,Node,double)} instead
     */
    public @Deprecated void addEdge(Edge e)
    {
	addEdge(e.getEdgeType(), e.getSource(), e.getDest(), e.getWeight());
    }

    /** Add an edge created from the supplied parameters to the graph;
     * if the nodes are already connected, then simply add the weight
     * to the existing Edge.  Note the Edge's EdgeType must already
     * have been inserted into the Graph.
     * @param et the EdgeType of the Edge.
     * @param source the source Node for the Edge.
     * @param dest the destination Node for the Edge.
     * @param weight the weight for the Edge.
     * @throws NullPointerException if the Edge's EdgeType hasn't
     * already been added to the Graph.
     * @return The created edge
     */
    public Edge addEdge(EdgeType et, Node source, Node dest, double weight)
    {
	final String edgeTypeName = et.getName();
        Edge existingEdge = getEdge(edgeTypeName, source, dest);
        if (existingEdge == null)
        {
            existingEdge = ethMap.get(edgeTypeName).addEdge(source, dest, weight);
            edges = null;
        }
        else
            existingEdge.addWeight(weight);
        return existingEdge;
    }

    /** Removes the Edge connecting the supplied source Node and
     * destination Node through the supplied EdgeType.  The resulting
     * Edge must exist.
     * @param edgeTypeName a String representing the EdgeType for the
     * connecting Edge to be removed.
     * @param source the source Node for the connecting Edge.
     * @param dest the destination Node for the connecting Edge.
     * @throws NullPointerException if the supplied EdgeType name
     * doesn't exist.
     * @throws RuntimeException if there is no connecting edge for the
     * supplied Nodes.
     */
    public void removeEdge(String edgeTypeName, Node source, Node dest)
    {
	final EdgeTypeHolder eth = ethMap.get(edgeTypeName);
	if(eth!=null) eth.removeEdge(source, dest);
	// Invalidate the Edge array cache.
	edges = null;
    }
    
    /** Removes all Edges from this Graph sharing the supplied EdgeType.
     * @param edgeTypeName a String representing the EdgeType for
     * Edges to be removed.
     * @throws NullPointerException if the supplied EdgeType name
     * doesn't exist.
     */
    public void removeEdges(String edgeTypeName)
    {
      final EdgeTypeHolder eth = ethMap.get(edgeTypeName);
      if(eth!=null) eth.removeEdges();
	// Invalidate the Edge array cache.
	edges = null;
    }
    
    /** Gets the edge connecting two nodes in the graph; if the nodes
     * aren't connected return null.
     * @param edgeTypeName the EdgeType name of the Edge being sought.
     * @param source the source Node for the Edge being sought.
     * @param dest the destination Node for the Edge being sought.
     * @return an Edge object connecting two nodes in the graph; if
     * the nodes aren't connected return null.
     */
    public Edge getEdge(String edgeTypeName, Node source, Node dest)
    {
	return source.getEdge(edgeTypeName, dest);
    }

    /** Gets all of the edges in the graph, irrespective of the
     * EdgeType; the order is unspecified.
     * @return all of the edges in the graph in an array.
     */
    public Edge[] getEdges()
    {
        if(edges == null)
	{
	    edges = new Edge[numEdges()];
	    int i = 0;
	    for (final EdgeTypeHolder eth : ethMap.values())
		for (final Edge e : eth.getEdges())
		    edges[i++] = e;
	}
        return edges.clone();
    }

    /** Gets all the of the edges in the graph having a particular
     * EdgeType; the order is unspecified.
     * @param et an EdgeType for the returned Edges.
     * @return all the of the edges in the graph having a particular
     * EdgeType.
     * @throws NullPointerException if the supplied EdgeType doesn't
     * exist in the Graph.
     */
    public Edge[] getEdges(EdgeType et)
    {
	return getEdges(et.getName());
    }
    
    /** Gets all the of the edges in the graph having a particular
     * EdgeType; the order is unspecified.
     * @param edgeTypeName a String representing the EdgeType name for
     * the returned Edges.
     * @return all the of the edges in the graph having a particular
     * EdgeType.
     * @throws NullPointerException if the supplied EdgeType doesn't
     * exist in the Graph.
     */
    public Edge[] getEdges(String edgeTypeName)
    {
      final EdgeTypeHolder eth = ethMap.get(edgeTypeName);
      if(eth==null)
        return new Edge[0];
      final Collection<Edge> col = eth.getEdges();
      return col.toArray(new Edge[col.size()]);
    }
    
    /** Gets a double array of values from Nodes in this Graph
     * matching the supplied node type; the values are obtained from
     * the Attribute at the supplied index offset into the Node's fields.
     * @param nodeType a String representation of the node type.
     * @param index the field index to lookup in each Node.
     * @return a double array of values from Nodes in this Graph
     * matching the supplied node type; the values are obtained from
     * the Attribute at the supplied index offset into the Node's fields.
     * @throws NullPointerException if the supplied node type doesn't exist.
     * @throws ArrayIndexOutOfBoundsException if the supplied index is
     * outside the bounds of the values in the Nodes.
     */
    public double[] getValues(String nodeType, int index)
    {
	return getValues(nodeType, new NodeFilter()
	    { public final boolean accept(Node n) { return true; }},
			 index);
    }
    
    /** Gets a double array of values from Nodes in this Graph
     * matching the supplied node type and which also are accepted by
     * the supplied NodeFilter; the values are obtained from the
     * Attribute at the supplied index offset into the Node's fields.
     * @param nodeType a String representation of the node type.
     * @param nf a NodeFilter to match against.
     * @param index the field index to lookup in each Node.
     * @return a double array of values from Nodes in this Graph
     * matching the supplied node type; the values are obtained from
     * the Attribute at the supplied index offset into the Node's fields.
     * @throws NullPointerException if the supplied node type doesn't exist.
     * @throws ArrayIndexOutOfBoundsException if the supplied index is
     * outside the bounds of the values in the Nodes.
     */
    public double[] getValues(String nodeType, NodeFilter nf, int index)
    {
	final Node[] nodes = getNodes(nodeType, nf);
	final double[] result = new double[nodes.length];

	for (int i=0; i<nodes.length; i++)
	    result[i] = nodes[i].getValue(index);
	return result;
    }
    
    /** Gets a double array of values from Nodes in this Graph
     * matching the supplied node type; the values are obtained from
     * the Attribute matching the supplied field name.
     * @param nodeType a String representation of the node type.
     * @param fieldName the field name to lookup in each Node.
     * @return a double array of values from Nodes in this Graph
     * matching the supplied node type; the values are obtained from
     * the Attribute matching the supplied field name.
     * @throws NullPointerException if the supplied node type doesn't exist.
     * @throws RuntimeException if the supplied field name doesn't exist.
     */
    public double[] getValues(String nodeType, String fieldName)
    {
	final int index = getAttributes(nodeType).getAttributeIndex(fieldName);
	return getValues(nodeType, index);
    }
    
    /** Gets a double array of values from Nodes in this Graph
     * matching the supplied node type and which also are accepted by
     * the supplied NodeFilter; the values are obtained from
     * the Attribute matching the supplied field name.
     * @param nodeType a String representation of the node type.
     * @param nf a NodeFilter to match against.
     * @param fieldName the field name to lookup in each Node.
     * @return a double array of values from Nodes in this Graph
     * matching the supplied node type and which also are accepted by
     * the supplied NodeFilter; the values are obtained from the
     * Attribute matching the supplied field name.
     * @throws NullPointerException if the supplied node type doesn't exist.
     * @throws RuntimeException if the supplied field name doesn't exist.
     */
    public double[] getValues(String nodeType, NodeFilter nf, String fieldName)
    {
	final int index = getAttributes(nodeType).getAttributeIndex(fieldName);
	return getValues(nodeType, nf, index);
    }
    
    // Helper method for doRecurse().  Given a List<EdgeType> (also
    // known as a "path") create all possible paths whose lengths are
    // one longer by appending EdgeTypes whose source node type
    // matches the destination node type of the last element in the
    // original List.
    private List<List<EdgeType>> buildLongerPaths(List<EdgeType> pathSoFar)
    {
	final List<List<EdgeType>> returnList
	    = new ArrayList<List<EdgeType>>();
	final String nextSourceNodeType
	    = pathSoFar.get(pathSoFar.size()-1).getDestType();
	
	for (final EdgeType et : getEdgeTypes(nextSourceNodeType))
	{
	    final List<EdgeType> listCopy = new ArrayList<EdgeType>(pathSoFar);
	    listCopy.add(et);
	    returnList.add(listCopy);
	}
	return returnList;
    }
    
    // Helper method for getPaths().  Check all paths in the testPaths
    // List, if any path match our constraints then add it to the
    // keepPaths List.  Otherwise, if we haven't reached our maximum
    // length build longer paths and recurse.
    private void doRecurse(String destNodeType, List<List<EdgeType>> keepPaths,
			   List<List<EdgeType>> testPaths, int maxLength)
    {
	for (final List<EdgeType> path : testPaths)
	{
	    // If the last element in this path has a matching
	    // destNodeType, then we found a suitable path.  Add it to
	    // the "keep" list.
	    if (path.get(path.size()-1).getDestType().equals(destNodeType))
		keepPaths.add(path);
	    else if (maxLength > 0)
		// We don't have a match.  If we haven't reached our
		// maximum length, recurse one level and keep looking.
		doRecurse(destNodeType, keepPaths,
			  buildLongerPaths(path), maxLength-1);
	}
    }
    
    /** Gets all paths in this Graph from the supplied source node
     * type to the supplied destination node type that fit within the
     * supplied maximum length.  A path is defined as a
     * {@code List<EdgeType>} which order EdgeType
     * elements by matching the predecessor's destination node type to
     * the successor's source node type.  A suitable path must also
     * have it's first element's source node type and it's last
     * element's destination node type match the supplied parameters.
     * If the source node type equals the destination node type then
     * the resulting paths are loops.
     * @param sourceNodeType a String name representing the source
     * node type of the path.
     * @param destNodeType a String name representing the destination
     * node type of the path.
     * @param maxLength an int for the maximum path length to clamp
     * the recursive search.
     * @return all paths in this Graph from the supplied source node
     * type to the supplied destination node type that fit within the
     * supplied maximum length.
     */
    public List<List<EdgeType>> getPaths(String sourceNodeType,
					 String destNodeType, int maxLength)
    {
	final List<List<EdgeType>> testPaths = new ArrayList<List<EdgeType>>();
	// Initialize testPaths to lists of length one, each
	// containing one starting EdgeType that match our
	// sourceNodeType.
	for (final EdgeType et : getEdgeTypes(sourceNodeType))
	{
	    final List<EdgeType> alist = new ArrayList<EdgeType>();
	    alist.add(et);
	    testPaths.add(alist);
	}

	// This List holds the paths that satisfy our conditions.
	final List<List<EdgeType>> keepPaths = new ArrayList<List<EdgeType>>();

	doRecurse(destNodeType, keepPaths, testPaths, maxLength-1);

	return keepPaths;
    }

    // Private helper method for getClassReferenceVector() and
    // getUnconditionalReferenceVector().
    private HistogramCategorical getReferenceVector(Node[] startNodes,
						    AttributeCategorical destAttrib,
						    List<EdgeType> path,
						    boolean normalized)
    {
	final double[] results = new double[destAttrib.size()];
	Arrays.fill(results, 0);
	
	for (final Node node : startNodes)
	{
	    final int index = node.getAttributeIndex(destAttrib.getName());
	    for (final Edge edge : node.getNeighbors(path))
	    {
		final int resultIndex = (int) edge.getDest().getValue(index);
		results[resultIndex] += edge.getWeight();
	    }
	}

	if (normalized)
	{
	    double sum = 0;
	    for (final double d : results)
		sum += d;
	    for (int i=0; i<results.length; i++)
		results[i] /= sum;
	}
	
	return new HistogramCategorical(results, destAttrib);
    }

    /** Gets a class reference vector for the supplied parameters;
     * returns a histogram on the results.
     * @param sourceAttrib the source attribute on which to restrict
     * the possible starting Nodes.
     * @param sourceAttribValue the value that the starting Nodes must
     * have for the source attribute.
     * @param destAttrib the  destination attribute for the vector.
     * @param path an EdgeType path for the vector.
     * @param normalized true if the resulting histogram should be
     * normalized.
     * @return a histogram on the resulting vector.
     */
    public HistogramCategorical getClassReferenceVector(AttributeCategorical sourceAttrib,
							final int sourceAttribValue,
							AttributeCategorical destAttrib,
							List<EdgeType> path,
							boolean normalized)
    {
	final String startNodeType = path.get(0).getSourceType();
	final String sourceAttribName = sourceAttrib.getName();
	final Node[] startNodes = getNodes(startNodeType, new NodeFilter()
	    { public final boolean accept(Node n) 
		{ return n.getValue(sourceAttribName) == sourceAttribValue; }
	    });

	return getReferenceVector(startNodes, destAttrib, path, normalized);
    }

    /** Gets an unconditional reference vector for the supplied
     * parameters; returns a histogram on the results.
     * @param destAttrib the  destination attribute for the vector.
     * @param path an EdgeType path for the vector.
     * @param normalized true if the resulting histogram should be
     * normalized.
     * @return a histogram on the resulting vector.
     */
    public HistogramCategorical getUnconditionalReferenceVector(AttributeCategorical destAttrib,
								List<EdgeType> path,
								boolean normalized)
    {
	final String startNodeType = path.get(0).getSourceType();
	final Node[] startNodes = getNodes(startNodeType);
	
	return getReferenceVector(startNodes, destAttrib, path, normalized);
    }

    /** Factory method which gets the meta info object for the
     * supplied nodeType name and attribute.  If the method is called
     * more than once, the same object is always returned.
     * @param nodeTypeName a String representing the nodeType (AKA Attributes object).
     * @param attribute an Attribute object within the supplied nodeType.
     * @return an AttributeMetaInfo object based on the supplied parameters.
     * @throws NullPointerException if the supplied nodeTypeName does
     * not exist within this Graph.
     * @throws RuntimeException if the supplied attribute does not
     * exist within the supplied nodeType.
     */
    public AttributeMetaInfo getAttributeMetaInfo(String nodeTypeName, Attribute attribute)
    {
	final NodeTypeHolder nt = ntMap.get(nodeTypeName);
	final Attributes attrs = nt.getAttributes();
	final String attributeName = attribute.getName();
	// Check if the attribute exists in the nodeType.
	if (attrs.getAttribute(attributeName) == null)
	    throw new RuntimeException("Attribute <"+attributeName
				       +"> does not exist in nodeType <"
				       +nodeTypeName+"!");
	
	return (AttributeMetaInfo) nt.getAMI(attributeName);
    }
    
    /** Factory method which gets the meta info object for the
     * supplied nodeType name and attribute.  If the method is called
     * more than once, the same object is always returned.
     * @param nodeTypeName a String representing the nodeType (AKA Attributes object).
     * @param attribute an Attribute object within the supplied nodeType.
     * @return an AttributeCategoricalMetaInfo object based on the supplied parameters.
     * @throws NullPointerException if the supplied nodeTypeName does
     * not exist within this Graph.
     * @throws RuntimeException if the supplied attribute does not
     * exist within the supplied nodeType.
     */
    public AttributeCategoricalMetaInfo getAttributeMetaInfo(String nodeTypeName, AttributeCategorical attribute)
    {
	final NodeTypeHolder nt = ntMap.get(nodeTypeName);
	final Attributes attrs = nt.getAttributes();
	final String attributeName = attribute.getName();
	// Check if the attribute exists in the nodeType.
	if (attrs.getAttribute(attributeName) == null)
	    throw new RuntimeException("Attribute <"+attributeName
				       +"> does not exist in nodeType <"
				       +nodeTypeName+"!");

	return (AttributeCategoricalMetaInfo) nt.getAMI(attributeName);
    }
    
    public static final void testGetPaths(final int maxLength)
    {
      final Graph graph = new Graph();
      graph.addAttributes(new Attributes("attr1"));
      graph.addAttributes(new Attributes("attr2"));
      graph.addAttributes(new Attributes("attr3"));
      graph.addAttributes(new Attributes("attr4"));
      graph.addAttributes(new Attributes("attr5"));
      graph.addAttributes(new Attributes("attr6"));
      graph.addAttributes(new Attributes("attr7"));
      graph.addAttributes(new Attributes("attr8"));
      graph.addEdgeType(new EdgeType("et1", "attr1","attr2"));
      graph.addEdgeType(new EdgeType("et2", "attr1","attr3"));
      graph.addEdgeType(new EdgeType("et3", "attr1","attr4"));
      graph.addEdgeType(new EdgeType("et4", "attr2","attr3"));
      graph.addEdgeType(new EdgeType("et5", "attr2","attr4"));
      graph.addEdgeType(new EdgeType("et6", "attr3","attr2"));
      graph.addEdgeType(new EdgeType("et7", "attr3","attr5"));
      graph.addEdgeType(new EdgeType("et8", "attr4","attr4"));
      graph.addEdgeType(new EdgeType("et9", "attr4","attr5"));
      graph.addEdgeType(new EdgeType("et10", "attr5","attr2"));
      graph.addEdgeType(new EdgeType("et11", "attr5","attr6"));
      graph.addEdgeType(new EdgeType("et12", "attr6","attr1"));
      graph.addEdgeType(new EdgeType("et13", "attr6","attr5"));
      graph.addEdgeType(new EdgeType("et14", "attr6","attr7"));
      graph.addEdgeType(new EdgeType("et15", "attr7","attr8"));
      graph.addEdgeType(new EdgeType("et16", "attr8","attr7"));

      final List<List<EdgeType>> paths = graph.getPaths("attr1", "attr7", maxLength);

      for (final List<EdgeType> path : paths)
      {
          System.err.println("Path length: "+path.size());
          for (final EdgeType et : path)
        System.err.println("\t\t<"+et+">");
      }
    }

    public static final void main(String args[])
    {
    	if(args.length == 0 || args[0].equalsIgnoreCase("testPaths"))
      {
        final int maxLength = (args.length > 1) ? Integer.parseInt(args[1]) : 5;
        testGetPaths(maxLength);
      }
	
    	else if(args[0].equalsIgnoreCase("testSubGraph"))
      {
    	  if(args.length < 3)
    	  {
    	    System.err.println("usage: testSubGraph schemafile nodetype:node1 ... nodetype:nodeK");
    	    System.exit(1);
    	  }
    	  Graph graph = netkit.graph.io.SchemaReader.readSchema(new java.io.File(args[1]));

        System.out.println("=======================================");
        System.out.println("Original graph:");
        System.out.println("Nodes:");
        for(Node n : graph.getNodes())
          System.out.println(n);
        System.out.println("Edges:");
        for(Edge e : graph.getEdges())
          System.out.println(e);

        Collection<Node> nodeSet = new ArrayList<Node>();
        for(int i=2;i<args.length;i++){
          String[] info = args[i].split(":");
          System.out.println("Extracing node ("+info[0]+":"+info[1]+")");
          nodeSet.add(graph.getNode(info[1],info[0]));
        }

        System.out.println("=======================================");
        System.out.println("Subgraph:");
        System.out.println("Nodes:");
        Graph newGraph = graph.subGraph(nodeSet);
        for(Node n : newGraph.getNodes())
          System.out.println(n);
        System.out.println("Edges:");
        for(Edge e : newGraph.getEdges())
          System.out.println(e);
        System.out.println("=======================================");
      }
    	
    	else
    	{
    	  System.err.println("Invalid option: "+args[0]);
        System.err.println("usage:");
        System.err.println("   Graph testPaths [maxLength]");
        System.err.println("   Graph testSubGraph schemafile nodetype:node1 ... nodetype:nodeK");
    	}
  }
}
