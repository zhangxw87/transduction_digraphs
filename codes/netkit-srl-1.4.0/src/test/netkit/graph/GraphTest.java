/**
 * GraphTest.java
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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import java.util.Arrays;
import java.util.List;

/**
 * Graph Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class GraphTest extends TestCase
{
    private Graph graph;
    
    public GraphTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	graph = new Graph();

	final Attributes attrs1 = new Attributes("myAttributes1");
	attrs1.add(new AttributeKey("field0"));
	attrs1.add(new AttributeDiscrete("field1"));
	attrs1.add(new AttributeDiscrete("field2"));

	final Attributes attrs2 = new Attributes("myAttributes2");
	attrs2.add(new AttributeKey("Field0"));
	attrs2.add(new AttributeContinuous("Field1"));
	attrs2.add(new AttributeContinuous("Field2"));

	graph.addAttributes(attrs1);
	graph.addAttributes(attrs2);
	
	final Node node0 = graph.addNode("node0", attrs1);
	node0.setValues(new double[] { 0, 1, 2 });
	final Node node1 = graph.addNode("node1", attrs1);
	node1.setValues(new double[] { 1, 3, 4 });
	final Node node2 = graph.addNode("node2", attrs1);
	node2.setValues(new double[] { 2, 5, 6 });
	final Node node3 = graph.addNode("node3", attrs1);
	node3.setValues(new double[] { 3, 7, 8 });
	final Node node4 = graph.addNode("node4", attrs1);
	node4.setValues(new double[] { 4, 9, 10 });
	final Node node5 = graph.addNode("node5", attrs2);
	node5.setValues(new double[] { 5, 11, 12 });
	final Node node6 = graph.addNode("node6", attrs2);
	node6.setValues(new double[] { 6, 13, 14 });
	final Node node7 = graph.addNode("node7", attrs2);
	node7.setValues(new double[] { 7, 15, 16 });
	final Node node8 = graph.addNode("node8", attrs2);
	node8.setValues(new double[] { 8, 17, 18 });
	final Node node9 = graph.addNode("node9", attrs2);
	node9.setValues(new double[] { 9, 19, Double.NaN });

	final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	final EdgeType et2 = new EdgeType("myEdgeType2", "myAttributes1", "myAttributes2");
	final EdgeType et3 = new EdgeType("myEdgeType3", "myAttributes2", "myAttributes2");
	final EdgeType et4 = new EdgeType("myEdgeType4", "myAttributes2", "myAttributes2");

	graph.addEdgeType(et1);
	graph.addEdgeType(et2);
	graph.addEdgeType(et3);
	graph.addEdgeType(et4);
	
	graph.addEdge(et1, node0, node1, 2.0);
	graph.addEdge(et1, node1, node0, 2.0);

	graph.addEdge(et1, node0, node2, 3.0);
	graph.addEdge(et1, node2, node0, 3.0);

	graph.addEdge(et1, node1, node3, 5.0);
	graph.addEdge(et1, node3, node1, 5.0);

	graph.addEdge(et1, node1, node4, 6.0);
	graph.addEdge(et1, node4, node1, 6.0);
	
	graph.addEdge(et2, node1, node6, 5.0);
	graph.addEdge(et2, node3, node8, 8.0);
	graph.addEdge(et2, node3, node9, 6.5);
	graph.addEdge(et2, node4, node5, 3.0);
	graph.addEdge(et2, node4, node7, 2.5);
	graph.addEdge(et2, node4, node9, 3.5);

	graph.addEdge(et3, node5, node7, 4.0);
	graph.addEdge(et3, node7, node5, 4.0);
	
	graph.addEdge(et3, node6, node7, 1.0);
	graph.addEdge(et3, node7, node6, 1.0);
	
	graph.addEdge(et3, node7, node8, 4.5);
	graph.addEdge(et3, node8, node7, 4.5);

	graph.addEdge(et3, node8, node9, 9.0);
	graph.addEdge(et3, node9, node8, 9.0);

	graph.addEdge(et4, node8, node9, 10.0);
	graph.addEdge(et4, node9, node8, 10.0);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	graph = null;
    }

    public void testGetMetrics() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testAddAttributes() throws Exception
    {
	// Adding valid Attributes was tested in the setup method.

	try { // Duplicate entry
	    graph.addAttributes(new Attributes("myAttributes1"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Graph already contains"));
	}
    }

    public void testGetAllAttributes() throws Exception
    {
	final Attributes[] a = graph.getAllAttributes();
	assertEquals(2, a.length);
	assertEquals("myAttributes1", a[0].getName());
	assertEquals("myAttributes2", a[1].getName());
    }

    public void testGetAttributes() throws Exception
    {
	assertEquals("myAttributes1", graph.getAttributes("myAttributes1").getName());
	assertEquals("myAttributes2", graph.getAttributes("myAttributes2").getName());
    }

    public void testGetNodeTypes() throws Exception
    {
	assertTrue(Arrays.equals(new String[] { "myAttributes1", "myAttributes2" },
				 graph.getNodeTypes()));
    }

    public void testGetEdgeTypeNames() throws Exception
    {
	assertTrue(Arrays.equals(new String[] {"myEdgeType1","myEdgeType2"},
				 graph.getEdgeTypeNames("myAttributes1")));
	assertTrue(Arrays.equals(new String[] {"myEdgeType3","myEdgeType4"},
				 graph.getEdgeTypeNames("myAttributes2")));
	assertTrue(Arrays.equals(new String[] { },
				 graph.getEdgeTypeNames("blah")));
    }

    public void testGetEdgeTypeNames1() throws Exception
    {
	assertTrue(Arrays.equals(new String[] { "myEdgeType1"},
				 graph.getEdgeTypeNames("myAttributes1","myAttributes1")));
	assertTrue(Arrays.equals(new String[] { "myEdgeType2"},
				 graph.getEdgeTypeNames("myAttributes1","myAttributes2")));
	assertTrue(Arrays.equals(new String[] { "myEdgeType3","myEdgeType4"},
				 graph.getEdgeTypeNames("myAttributes2","myAttributes2")));
	assertTrue(Arrays.equals(new String[] { },
				 graph.getEdgeTypeNames("blah","myAttributes2")));
	assertTrue(Arrays.equals(new String[] { },
				 graph.getEdgeTypeNames("myAttributes1","blah")));
    }

    public void testGetEdgeType() throws Exception
    {
	assertEquals("myEdgeType1", graph.getEdgeType("myEdgeType1").getName());
	assertEquals("myEdgeType2", graph.getEdgeType("myEdgeType2").getName());
	assertEquals("myEdgeType3", graph.getEdgeType("myEdgeType3").getName());
	assertEquals("myEdgeType4", graph.getEdgeType("myEdgeType4").getName());
	assertNull(graph.getEdgeType("blah"));
    }

    public void testAddEdgeType() throws Exception
    {
	// The setup method has already tested successful additions.

	try { // Duplicate entry
	    graph.addEdgeType(new EdgeType("myEdgeType1", "blah", "blah"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Duplicate EdgeType"));
	}

	try { // Bad source node type.
	    graph.addEdgeType(new EdgeType("myEdgeType5", "blah1", "blah2"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Invalid EdgeType source"));
	}

	try { // Bad destination node type.
	    graph.addEdgeType(new EdgeType("myEdgeType5", "myAttributes1", "blah2"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Invalid EdgeType destination"));
	}

	assertEquals(new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1"),
				  graph.getEdgeType("myEdgeType1"));
    }

    public void testNumNodes() throws Exception
    {
	assertEquals(10, graph.numNodes());
    }

    public void testNumNodes1() throws Exception
    {
	assertEquals(5, graph.numNodes("myAttributes1"));
	assertEquals(5, graph.numNodes("myAttributes2"));
	try {
	    graph.numNodes("blah");
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testNumEdges() throws Exception
    {
	assertEquals(24, graph.numEdges());
    }

    public void testNumEdges1() throws Exception
    {
	assertEquals(8, graph.numEdges("myEdgeType1"));
	assertEquals(6, graph.numEdges("myEdgeType2"));
	assertEquals(8, graph.numEdges("myEdgeType3"));
	assertEquals(2, graph.numEdges("myEdgeType4"));
	assertEquals(0, graph.numEdges("blah"));
    }

    public void testAddNode() throws Exception
    {
        // The setup method already tested valid additions

	try { // Invalid Attributes
	    graph.addNode("blahNode", new Attributes("blah"));
	    fail();
	} catch (NullPointerException success) { }
	
	try { // Duplicate node
	    graph.addNode("node0", graph.getAttributes("myAttributes1"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("already exists"));
	}

	assertEquals(10, graph.numNodes());
    }

    public void testGetNode() throws Exception
    {
	assertEquals("node0", graph.getNode(0).getName());
	assertEquals("node1", graph.getNode(1).getName());
	assertEquals("node2", graph.getNode(2).getName());
	assertEquals("node3", graph.getNode(3).getName());
	assertEquals("node4", graph.getNode(4).getName());
	assertEquals("node5", graph.getNode(5).getName());
	assertEquals("node6", graph.getNode(6).getName());
	assertEquals("node7", graph.getNode(7).getName());
	assertEquals("node8", graph.getNode(8).getName());
	assertEquals("node9", graph.getNode(9).getName());
	try { // Invalid index
	    graph.getNode(10);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }
    }

    public void testGetNode1() throws Exception
    {
	assertEquals("node0", graph.getNode("node0","myAttributes1").getName());
	assertEquals("node1", graph.getNode("node1","myAttributes1").getName());
	assertEquals("node2", graph.getNode("node2","myAttributes1").getName());
	assertEquals("node3", graph.getNode("node3","myAttributes1").getName());
	assertEquals("node4", graph.getNode("node4","myAttributes1").getName());
	assertEquals("node5", graph.getNode("node5","myAttributes2").getName());
	assertEquals("node6", graph.getNode("node6","myAttributes2").getName());
	assertEquals("node7", graph.getNode("node7","myAttributes2").getName());
	assertEquals("node8", graph.getNode("node8","myAttributes2").getName());
	assertEquals("node9", graph.getNode("node9","myAttributes2").getName());
	assertNull(graph.getNode("node55","myAttributes1"));
	try { // Invalid Attributes
	    graph.getNode("node0","blah");
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetNodes() throws Exception
    {
	final Node[] nodes = graph.getNodes();
	assertEquals(10, nodes.length);
	assertEquals("node0", nodes[0].getName());
	assertEquals("node1", nodes[1].getName());
	assertEquals("node2", nodes[2].getName());
	assertEquals("node3", nodes[3].getName());
	assertEquals("node4", nodes[4].getName());
	assertEquals("node5", nodes[5].getName());
	assertEquals("node6", nodes[6].getName());
	assertEquals("node7", nodes[7].getName());
	assertEquals("node8", nodes[8].getName());
	assertEquals("node9", nodes[9].getName());
    }

    public void testGetNodes1() throws Exception
    {
	Node[] nodes;

	nodes = graph.getNodes("myAttributes1");
	assertEquals(5, nodes.length);
	assertEquals("node0", nodes[0].getName());
	assertEquals("node1", nodes[1].getName());
	assertEquals("node2", nodes[2].getName());
	assertEquals("node3", nodes[3].getName());
	assertEquals("node4", nodes[4].getName());

	nodes = graph.getNodes("myAttributes2");
	assertEquals(5, nodes.length);
	assertEquals("node5", nodes[0].getName());
	assertEquals("node6", nodes[1].getName());
	assertEquals("node7", nodes[2].getName());
	assertEquals("node8", nodes[3].getName());
	assertEquals("node9", nodes[4].getName());

	try { // Invalid Attributes
	    graph.getNodes("blah");
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetNodes2() throws Exception
    {
	final NodeFilter nf = new NodeFilter()
	    {
		public final boolean accept(Node n) 
		{ return (n.getIndex() % 2) == 0; }
	    };

	Node[] nodes;

	nodes = graph.getNodes("myAttributes1", nf);
	assertEquals(3, nodes.length);
	assertEquals("node0", nodes[0].getName());
	assertEquals("node2", nodes[1].getName());
	assertEquals("node4", nodes[2].getName());

	nodes = graph.getNodes("myAttributes2", nf);
	assertEquals(3, nodes.length);
	assertEquals("node5", nodes[0].getName());
	assertEquals("node7", nodes[1].getName());
	assertEquals("node9", nodes[2].getName());

	try { // Invalid Attributes
	    graph.getNodes("blah", nf);
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetNodes3() throws Exception
    {
	Node[] nodes;

	nodes = graph.getNodes(graph.getAttributes("myAttributes1"));
	assertEquals(5, nodes.length);
	assertEquals("node0", nodes[0].getName());
	assertEquals("node1", nodes[1].getName());
	assertEquals("node2", nodes[2].getName());
	assertEquals("node3", nodes[3].getName());
	assertEquals("node4", nodes[4].getName());

	nodes = graph.getNodes(graph.getAttributes("myAttributes2"));
	assertEquals(5, nodes.length);
	assertEquals("node5", nodes[0].getName());
	assertEquals("node6", nodes[1].getName());
	assertEquals("node7", nodes[2].getName());
	assertEquals("node8", nodes[3].getName());
	assertEquals("node9", nodes[4].getName());

	try { // Invalid Attributes
	    graph.getNodes(new Attributes("blah"));
	    fail();
	} catch (NullPointerException success) { }
	
    }

    public void testAddEdge() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testAddEdge1() throws Exception
    {
	// Valid additions were already test in the setup method.
    }

    public void testGetEdge() throws Exception
    {
	for (final Edge e : graph.getEdges())
	    assertEquals(e, graph.getEdge(e.getEdgeType().getName(),
					  e.getSource(), e.getDest()));
    }

    public void testGetEdges() throws Exception
    {
	final Edge[] edges = graph.getEdges();
	Arrays.sort(edges);

	assertEquals(24, edges.length);

	assertEquals("myEdgeType1", edges[0].getEdgeType().getName());
	assertEquals("node0", edges[0].getSource().getName());
	assertEquals("node1", edges[0].getDest().getName());
	assertEquals(2.0, edges[0].getWeight());

	assertEquals("myEdgeType4", edges[23].getEdgeType().getName());
	assertEquals("node9", edges[23].getSource().getName());
	assertEquals("node8", edges[23].getDest().getName());
	assertEquals(10.0, edges[23].getWeight());
    }

    public void testGetEdges1() throws Exception
    {
	Edge[] edges;

	edges = graph.getEdges(graph.getEdgeType("myEdgeType1"));
	assertEquals(8, edges.length);

	edges = graph.getEdges(graph.getEdgeType("myEdgeType2"));
	assertEquals(6, edges.length);

	edges = graph.getEdges(graph.getEdgeType("myEdgeType3"));
	assertEquals(8, edges.length);

	edges = graph.getEdges(graph.getEdgeType("myEdgeType4"));
	assertEquals(2, edges.length);

	try { // Invalid EdgeType
	    graph.getEdges(new EdgeType("blah", "blah", "blah"));
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetValues() throws Exception
    {
	assertTrue(Arrays.equals(new double[] {0,1,2,3,4},
				 graph.getValues("myAttributes1", 0)));
	assertTrue(Arrays.equals(new double[] {5,6,7,8,9},
				 graph.getValues("myAttributes2", 0)));
	assertTrue(Arrays.equals(new double[] {1,3,5,7,9},
				 graph.getValues("myAttributes1", 1)));
	assertTrue(Arrays.equals(new double[] {11,13,15,17,19},
				 graph.getValues("myAttributes2", 1)));
	assertTrue(Arrays.equals(new double[] {2,4,6,8,10},
				 graph.getValues("myAttributes1", 2)));
	assertTrue(Arrays.equals(new double[] {12,14,16,18,Double.NaN},
				 graph.getValues("myAttributes2", 2)));

	try { // Invalid Index
	    graph.getValues("myAttributes1", 3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }
	try { // Invalid Attributes
	    graph.getValues("blah", 0);
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetValues1() throws Exception
    {
	final NodeFilter nf = new NodeFilter()
	    {
		public final boolean accept(Node n) 
		{ return (n.getIndex() % 2) == 0; }
	    };

	assertTrue(Arrays.equals(new double[] {0,2,4},
				 graph.getValues("myAttributes1", nf, 0)));
	assertTrue(Arrays.equals(new double[] {5,7,9},
				 graph.getValues("myAttributes2", nf, 0)));
	assertTrue(Arrays.equals(new double[] {1,5,9},
				 graph.getValues("myAttributes1", nf, 1)));
	assertTrue(Arrays.equals(new double[] {11,15,19},
				 graph.getValues("myAttributes2", nf, 1)));
	assertTrue(Arrays.equals(new double[] {2,6,10},
				 graph.getValues("myAttributes1", nf, 2)));
	assertTrue(Arrays.equals(new double[] {12,16,Double.NaN},
				 graph.getValues("myAttributes2", nf, 2)));

	try { // Invalid Index
	    graph.getValues("myAttributes1", nf, 3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }
	try { // Invalid Attributes
	    graph.getValues("blah", nf, 0);
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetValues2() throws Exception
    {
	assertTrue(Arrays.equals(new double[] {0,1,2,3,4},
				 graph.getValues("myAttributes1", "field0")));
	assertTrue(Arrays.equals(new double[] {5,6,7,8,9},
				 graph.getValues("myAttributes2", "Field0")));
	assertTrue(Arrays.equals(new double[] {1,3,5,7,9},
				 graph.getValues("myAttributes1", "field1")));
	assertTrue(Arrays.equals(new double[] {11,13,15,17,19},
				 graph.getValues("myAttributes2", "Field1")));
	assertTrue(Arrays.equals(new double[] {2,4,6,8,10},
				 graph.getValues("myAttributes1", "field2")));
	assertTrue(Arrays.equals(new double[] {12,14,16,18,Double.NaN},
				 graph.getValues("myAttributes2", "Field2")));

	try { // Invalid Index
	    graph.getValues("myAttributes1", "field3");
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find index for attribute"));
	}
	try { // Invalid Attributes
	    graph.getValues("blah", "field0");
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetValues3() throws Exception
    {
	final NodeFilter nf = new NodeFilter()
	    {
		public final boolean accept(Node n) 
		{ return (n.getIndex() % 2) == 0; }
	    };

	assertTrue(Arrays.equals(new double[] {0,2,4},
				 graph.getValues("myAttributes1",nf,"field0")));
	assertTrue(Arrays.equals(new double[] {5,7,9},
				 graph.getValues("myAttributes2",nf,"Field0")));
	assertTrue(Arrays.equals(new double[] {1,5,9},
				 graph.getValues("myAttributes1",nf,"field1")));
	assertTrue(Arrays.equals(new double[] {11,15,19},
				 graph.getValues("myAttributes2",nf,"Field1")));
	assertTrue(Arrays.equals(new double[] {2,6,10},
				 graph.getValues("myAttributes1",nf,"field2")));
	assertTrue(Arrays.equals(new double[] {12,16,Double.NaN},
				 graph.getValues("myAttributes2",nf,"Field2")));

	try { // Invalid Index
	    graph.getValues("myAttributes1",nf,"field3");
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find index for attribute"));
	}
	try { // Invalid Attributes
	    graph.getValues("blah",nf,"field0");
	    fail();
	} catch (NullPointerException success) { }
    }

    public void testGetPaths() throws Exception
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

	// Test a path.
	String start = "attr1";
	String end = "attr7";
	List<List<EdgeType>> paths = graph.getPaths(start, end, 10);
	assertEquals(203, paths.size());
	for (final List<EdgeType> path : paths)
	{
	    assertEquals(start, path.get(0).getSourceType());
	    for (int i=1; i<path.size()-1; i++)
		assertEquals(path.get(i).getDestType(), path.get(i+1).getSourceType());
	    assertEquals(end, path.get(path.size()-1).getDestType());
	}

	// Test a loop.
	start = "attr1";
	end = "attr1";
	paths = graph.getPaths(start, end, 10);
	assertEquals(154, paths.size());
	for (final List<EdgeType> path : paths)
	{
	    assertEquals(start, path.get(0).getSourceType());
	    for (int i=1; i<path.size()-1; i++)
		assertEquals(path.get(i).getDestType(), path.get(i+1).getSourceType());
	    assertEquals(end, path.get(path.size()-1).getDestType());
	}
    }
    
    public void testAddAttribute() throws Exception
    {
	try { // Duplicate field name.
	    graph.addAttribute("myAttributes2", new AttributeContinuous("Field0"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Already got field"));
	}

	try { // Invalid nodeType (AKA Attributes container) name.
	    graph.addAttribute("blahblah", new AttributeContinuous("Field3"));
	    fail();
	} catch (NullPointerException success) { }

	try { // Duplicate KEY.
	    graph.addAttribute("myAttributes2", new AttributeKey("Field3"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Got second key attribute"));
	}

	for (final Node node : graph.getNodes("myAttributes2"))
	{
	    assertEquals(3, node.getAttributes().attributeCount());
	    assertFalse(node.isMissing(1));
	    assertTrue(Double.NaN != node.getValue("Field1"));
	}

	graph.addAttribute("myAttributes2", new AttributeContinuous("Field3"));
	for (final Node node : graph.getNodes("myAttributes2"))
	{
	    assertEquals(4, node.getAttributes().attributeCount());
	    assertFalse(node.isMissing(1));
	    assertTrue(Double.NaN != node.getValue("Field1"));
	    assertTrue(node.isMissing(3));
	    assertEquals(Double.NaN, node.getValue("Field3"));
	}
    }
    
    public void testRemoveAttribute() throws Exception
    {
	try { // Invalid nodeType (AKA Attributes container) name.
	    graph.removeAttribute("blahblah", 2);
	    fail();
	} catch (NullPointerException success) { }

	try { // Can't remove KEY.
	    graph.removeAttribute("myAttributes2", 0);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Can't remove KEY field"));
	}

	try { // Invalid index.
	    graph.removeAttribute("myAttributes2", -1);
	    fail();
	} catch (NullPointerException success) { }

	try { // Invalid index.
	    graph.removeAttribute("myAttributes2", 3);
	    fail();
	} catch (NullPointerException success) { }

	for (final Node node : graph.getNodes("myAttributes2"))
	{
	    assertEquals(3, node.getAttributes().attributeCount());
	    assertFalse(node.isMissing(1));
	    assertTrue(Double.NaN != node.getValue("Field1"));
	}

	graph.removeAttribute("myAttributes2", 1);
	for (final Node node : graph.getNodes("myAttributes2"))
	{
	    assertEquals(2, node.getAttributes().attributeCount());
	    assertFalse(node.isMissing(0));
	    assertTrue(Double.NaN != node.getValue("Field0"));
	}
    }
    
    public void testRemoveEdge() throws Exception
    {
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	try { // Invalid EdgeType name.
	    graph.removeEdge("blahblah",
			     graph.getNode("node0", "myAttributes1"),
			     graph.getNode("node1", "myAttributes1"));
	    fail();
	} catch (NullPointerException success) { }

	try { // Invalid source Node.
	    graph.removeEdge("myEdgeType1",
			     graph.getNode("node99", "myAttributes1"),
			     graph.getNode("node0", "myAttributes1"));
	    fail();
	} catch (NullPointerException success) { }

	try { // Invalid dest Node.
	    graph.removeEdge("myEdgeType1",
			     graph.getNode("node0", "myAttributes1"),
			     graph.getNode("node99", "myAttributes1"));
	    fail();
	} catch (NullPointerException success) { }

	try { // No Edge connecting these two nodes.
	    graph.removeEdge("myEdgeType1",
			     graph.getNode("node0", "myAttributes1"),
			     graph.getNode("node3", "myAttributes1"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("No Edge to Node"));
	}

	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	graph.removeEdge("myEdgeType1",
			 graph.getNode("node0", "myAttributes1"),
			 graph.getNode("node1", "myAttributes1"));
	assertEquals (23, graph.getEdges().length);
	assertEquals (7, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdge("myEdgeType1",
			 graph.getNode("node1", "myAttributes1"),
			 graph.getNode("node0", "myAttributes1"));
	assertEquals (22, graph.getEdges().length);
	assertEquals (6, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdge("myEdgeType1",
			 graph.getNode("node3", "myAttributes1"),
			 graph.getNode("node1", "myAttributes1"));
	assertEquals (21, graph.getEdges().length);
	assertEquals (5, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdge("myEdgeType3",
			 graph.getNode("node7", "myAttributes2"),
			 graph.getNode("node8", "myAttributes2"));
	assertEquals (20, graph.getEdges().length);
	assertEquals (5, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (7, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
    }
    
    public void testRemoveEdges() throws Exception
    {
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	try { // Invalid EdgeType name.
	    graph.removeEdges("blahblah");
	    fail();
	} catch (NullPointerException success) { }

	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	graph.removeEdges("myEdgeType1");
	
	assertEquals (16, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdges("myEdgeType3");
	
	assertEquals (8, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdges("myEdgeType4");
	
	assertEquals (6, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (0, graph.getEdges("myEdgeType4").length);

	graph.removeEdges("myEdgeType4"); // Try it again.
	
	assertEquals (6, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (0, graph.getEdges("myEdgeType4").length);

	graph.removeEdges("myEdgeType2");
	
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (0, graph.getEdges("myEdgeType4").length);
    }
    
    public void testRemoveEdgeType() throws Exception
    {
	assertTrue(Arrays.equals(new String[] {"myEdgeType1", "myEdgeType2",
					       "myEdgeType3", "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	try { // Invalid EdgeType name, force=false.
	    graph.removeEdgeType("blahblah", false);
	    fail();
	} catch (NullPointerException success) { }
	
	try { // Invalid EdgeType name, force=true.
	    graph.removeEdgeType("blahblah", true);
	    fail();
	} catch (NullPointerException success) { }

	try { // Valid EdgeType name, but Edges exist.
	    graph.removeEdgeType("myEdgeType1", false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges exist with EdgeType"));
	}

	try { // Valid EdgeType name, but Edges exist.
	    graph.removeEdgeType("myEdgeType2", false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges exist with EdgeType"));
	}

	try { // Valid EdgeType name, but Edges exist.
	    graph.removeEdgeType("myEdgeType3", false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges exist with EdgeType"));
	}

	try { // Valid EdgeType name, but Edges exist.
	    graph.removeEdgeType("myEdgeType4", false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges exist with EdgeType"));
	}

	assertTrue(Arrays.equals(new String[] {"myEdgeType1", "myEdgeType2",
					       "myEdgeType3", "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	
	graph.removeEdgeType("myEdgeType1", true);
	assertTrue(Arrays.equals(new String[] {"myEdgeType2", "myEdgeType3",
					       "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (16, graph.getEdges().length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	// Remove all Edges so we can try having force=false.
	graph.removeEdges("myEdgeType2");
	assertTrue(Arrays.equals(new String[] {"myEdgeType2", "myEdgeType3",
					       "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (10, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdgeType("myEdgeType2", false);
	assertTrue(Arrays.equals(new String[] {"myEdgeType3", "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (10, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	try { // Removing an EdgeType a second time.
	    graph.removeEdgeType("myEdgeType2", true);
	    fail();
        } catch (NullPointerException success) { }
	assertTrue(Arrays.equals(new String[] {"myEdgeType3", "myEdgeType4" },
				 graph.getEdgeTypeNames()));
	assertEquals (10, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);

	graph.removeEdgeType("myEdgeType4", true);
	assertTrue(Arrays.equals(new String[] {"myEdgeType3" },
				 graph.getEdgeTypeNames()));
	assertEquals (8, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);

	graph.removeEdgeType("myEdgeType3", true);
	assertTrue(Arrays.equals(new String[] { },
				 graph.getEdgeTypeNames()));
	assertEquals (0, graph.getEdges().length);
    }
    
    public void testRemoveNodes() throws Exception
    {
	assertEquals (10, graph.getNodes().length);
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);

	try { // Invalid nodeType, force=false.
	    graph.removeNodes("blahblah", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Can't find nodeType"));
	}

	try { // Invalid nodeType, force=true.
	    graph.removeNodes("blahblah", true);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Can't find nodeType"));
	}

	try { // Valid nodeType but force=false.
	    graph.removeNodes("myAttributes1", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges already exist which connect Nodes of type"));
	}

	try { // Valid nodeType but force=false.
	    graph.removeNodes("myAttributes2", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges already exist which connect Nodes of type"));
	}

	assertEquals (10, graph.getNodes().length);
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);

	graph.removeNodes("myAttributes1", true);

	assertEquals (5, graph.getNodes().length);
	assertEquals (10, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
	
	try { // Valid nodeType but force=false.
	    graph.removeNodes("myAttributes2", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges already exist which connect Nodes of type"));
	}
	
	assertEquals (5, graph.getNodes().length);
	assertEquals (10, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
	
	graph.removeEdges("myEdgeType3");
	
	assertEquals (5, graph.getNodes().length);
	assertEquals (2, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
	
	try { // Valid nodeType but force=false.
	    graph.removeNodes("myAttributes2", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Edges already exist which connect Nodes of type"));
	}
	
	assertEquals (5, graph.getNodes().length);
	assertEquals (2, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
	
	graph.removeEdges("myEdgeType4");

	assertEquals (5, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (0, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
	
	// Now force=false should work.
	graph.removeNodes("myAttributes2", false);

	assertEquals (0, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (0, graph.getEdges("myEdgeType2").length);
	assertEquals (0, graph.getEdges("myEdgeType3").length);
	assertEquals (0, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);
    }

    public void testRemoveAttributes() throws Exception
    {
	assertEquals (10, graph.getNodes().length);
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);

	try { // Invalid nodeType, force=false.
	    graph.removeAttributes("blahblah", false);
	    fail();
        } catch (NullPointerException success) { }

	try { // Invalid nodeType, force=true.
	    graph.removeAttributes("blahblah", true);
	    fail();
        } catch (NullPointerException success) { }

	try { // Valid nodeType but Nodes etc of that type exist.
	    graph.removeAttributes("myAttributes2", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Nodes already exist of type"));
	}

	assertEquals (10, graph.getNodes().length);
	assertEquals (24, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (6, graph.getEdges("myEdgeType2").length);
	assertEquals (8, graph.getEdges("myEdgeType3").length);
	assertEquals (2, graph.getEdges("myEdgeType4").length);
	assertEquals (4, graph.getEdgeTypeNames().length);
	assertEquals (2, graph.getAllAttributes().length);

	graph.removeAttributes("myAttributes2", true);

	assertEquals (5, graph.getNodes().length);
	assertEquals (8, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	try { // Valid nodeType but Nodes etc of that type exist.
	    graph.removeAttributes("myAttributes1", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Nodes already exist of type"));
	}

	assertEquals (5, graph.getNodes().length);
	assertEquals (8, graph.getEdges().length);
	assertEquals (8, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	graph.removeEdges("myEdgeType1");
	
	assertEquals (5, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	try { // Valid nodeType but Nodes etc of that type exist.
	    graph.removeAttributes("myAttributes1", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Nodes already exist of type"));
	}

	assertEquals (5, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	graph.removeNodes("myAttributes1", true);

	assertEquals (0, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	try { // Valid nodeType but EdgeTypes using that NodeType exist.
	    graph.removeAttributes("myAttributes1", false);
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("EdgeTypes already exist which connect Nodes of type"));
	}

	assertEquals (0, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdges("myEdgeType1").length);
	assertEquals (1, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	graph.removeEdgeType("myEdgeType1", false);

	assertEquals (0, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdgeTypeNames().length);
	assertEquals (1, graph.getAllAttributes().length);

	graph.removeAttributes("myAttributes1", false);

	assertEquals (0, graph.getNodes().length);
	assertEquals (0, graph.getEdges().length);
	assertEquals (0, graph.getEdgeTypeNames().length);
	assertEquals (0, graph.getAllAttributes().length);
    }

    public void testGetAttributeMetaInfo() throws Exception
    {
	final Attributes attrs1 = graph.getAttributes("myAttributes1");
	final Attribute attrf1 = attrs1.getAttribute("field1");
	final Attributes attrs2 = graph.getAttributes("myAttributes2");
	final Attribute attrF1 = attrs2.getAttribute("Field1");
	
	try { // Invalid nodeType.
	    graph.getAttributeMetaInfo("blahblah", attrf1);
	    fail();
        } catch (NullPointerException success) { }

	try { // Invalid field.
	    graph.getAttributeMetaInfo("myAttributes1", new AttributeDiscrete("blah"));
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("does not exist in nodeType"));
	}

	// Try a simple AMI object.
	final AttributeMetaInfo amif1 = graph.getAttributeMetaInfo("myAttributes1", attrf1);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif1 == graph.getAttributeMetaInfo("myAttributes1", attrf1));
	assertEquals("myAttributes1", amif1.getNodeTypeName());
	assertEquals("field1", amif1.getFieldName());
	assertEquals(9.0, amif1.getMax());
	assertEquals(1.0, amif1.getMin());
	assertEquals(5.0, amif1.getMean());
	assertEquals(5.0, amif1.getMedian());
	
	// Try another simple AMI object.
	final AttributeMetaInfo amiF1 = graph.getAttributeMetaInfo("myAttributes2", attrF1);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amiF1 == graph.getAttributeMetaInfo("myAttributes2", attrF1));
	assertEquals("myAttributes2", amiF1.getNodeTypeName());
	assertEquals("Field1", amiF1.getFieldName());
	assertEquals(19.0, amiF1.getMax());
	assertEquals(11.0, amiF1.getMin());
	assertEquals(15.0, amiF1.getMean());
	assertEquals(15.0, amiF1.getMedian());

	// Try removing field1 and getting field2's AMI object.
	graph.removeAttribute("myAttributes1", 1);
	final Attribute attrf2 = graph.getAttributes("myAttributes1").getAttribute("field2");
	final AttributeMetaInfo amif2 = graph.getAttributeMetaInfo("myAttributes1", attrf2);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif2 == graph.getAttributeMetaInfo("myAttributes1", attrf2));
	assertEquals("myAttributes1", amif2.getNodeTypeName());
	assertEquals("field2", amif2.getFieldName());
	assertEquals(10.0, amif2.getMax());
	assertEquals(2.0, amif2.getMin());
	assertEquals(6.0, amif2.getMean());
	assertEquals(6.0, amif2.getMedian());
	
	// Try removing field2, then add it back with new values and
	// see if we get it recalculated.  Note "field2" is at index 1
	// because we removed "field1" above.
	graph.removeAttribute("myAttributes1", 1);
	graph.addAttribute("myAttributes1", new AttributeDiscrete("field2"));
	graph.getNode("node0", "myAttributes1").setValue("field2", 18);
	graph.getNode("node1", "myAttributes1").setValue("field2", 19);
	graph.getNode("node2", "myAttributes1").setValue("field2", 23);
	graph.getNode("node3", "myAttributes1").setValue("field2", 24);
	graph.getNode("node4", "myAttributes1").setValue("field2", 35);
	final Attribute attrf2b = graph.getAttributes("myAttributes1").getAttribute("field2");
	final AttributeMetaInfo amif2b = graph.getAttributeMetaInfo("myAttributes1", attrf2b);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif2b == graph.getAttributeMetaInfo("myAttributes1", attrf2b));
	assertTrue(amif2b != amif2);
	assertEquals("myAttributes1", amif2b.getNodeTypeName());
	assertEquals("field2", amif2b.getFieldName());
	assertEquals(35.0, amif2b.getMax());
	assertEquals(18.0, amif2b.getMin());
	assertEquals(23.8, amif2b.getMean());
	assertEquals(23.0, amif2b.getMedian());

	// Try removing all Nodes, adding others and making sure things are recalculated.
	graph.removeNodes("myAttributes1", true);
	final Node node0 = graph.addNode("Node0", attrs1);
	node0.setValues(new double[] { 10, 11 });
	final Node node1 = graph.addNode("Node1", attrs1);
	node1.setValues(new double[] { 11, 13 });
	final Node node2 = graph.addNode("Node2", attrs1);
	node2.setValues(new double[] { 12, 15 });
	final Node node3 = graph.addNode("Node3", attrs1);
	node3.setValues(new double[] { 23, 27 });
	final AttributeKey attrf0 = (AttributeKey)graph.getAttributes("myAttributes1").getAttribute("field0");
	final AbstractAttributeMetaInfo amif0 = graph.getAttributeMetaInfo("myAttributes1", attrf0);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif0 == graph.getAttributeMetaInfo("myAttributes1", attrf0));
	assertEquals("myAttributes1", amif0.getNodeTypeName());
	assertEquals("field0", amif0.getFieldName());
	assertEquals(23.0, amif0.getMax());
	assertEquals(10.0, amif0.getMin());
	assertEquals(14.0, amif0.getMean());
	assertEquals(11.5, amif0.getMedian());

	// Try removing the nodeType (AKA Attributes container).  Then
	// readd everything with new numbers and make sure it gets
	// recalculated.
	graph.removeAttributes("myAttributes1", true);
	final Attributes attrs1b = new Attributes("myAttributes1");
	attrs1b.add(new AttributeKey("field0"));
	attrs1b.add(new AttributeDiscrete("field1"));
	attrs1b.add(new AttributeDiscrete("field2"));
	graph.addAttributes(attrs1b);
	final Node node0b = graph.addNode("Node0", attrs1b);
	node0b.setValues(new double[] { 110, 111, 321 });
	final Node node1b = graph.addNode("Node1", attrs1b);
	node1b.setValues(new double[] { 111, 113, 322});
	final Node node2b = graph.addNode("Node2", attrs1b);
	node2b.setValues(new double[] { 112, 115, 327 });
	final Node node3b = graph.addNode("Node3", attrs1b);
	node3b.setValues(new double[] { 213, 127, 329 });
	final Attribute attrf2c = graph.getAttributes("myAttributes1").getAttribute("field2");
	final AttributeMetaInfo amif2c = graph.getAttributeMetaInfo("myAttributes1", attrf2c);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif2c == graph.getAttributeMetaInfo("myAttributes1", attrf2c));
	assertTrue(amif2c != amif2);
	assertTrue(amif2c != amif2b);
	assertEquals("myAttributes1", amif2c.getNodeTypeName());
	assertEquals("field2", amif2c.getFieldName());
	assertEquals(329.0, amif2c.getMax());
	assertEquals(321.0, amif2c.getMin());
	assertEquals(324.75, amif2c.getMean());
	assertEquals(324.5, amif2c.getMedian());
    }

    public void testGetAttributeMetaInfo1() throws Exception
    {
	final Attributes attrs1 = graph.getAttributes("myAttributes1");
	final FixedTokenSet fts = new FixedTokenSet(new String[] {"Low","Medium","High" });
	final AttributeFixedCategorical afc = new AttributeFixedCategorical("field3", fts);
	graph.addAttribute("myAttributes1", afc);
	graph.getNode("node0", "myAttributes1").setValue("field3", 0);
	graph.getNode("node1", "myAttributes1").setValue("field3", 1);
	graph.getNode("node2", "myAttributes1").setValue("field3", 2);
	graph.getNode("node3", "myAttributes1").setValue("field3", 2);
	graph.getNode("node4", "myAttributes1").setValue("field3", 2);

	try { // Invalid nodeType.
	    graph.getAttributeMetaInfo("blahblah", afc);
	    fail();
        } catch (NullPointerException success) { }

	try { // Invalid field.
	    graph.getAttributeMetaInfo("myAttributes1", new AttributeExpandableCategorical("blah"));
	    fail();
        } catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("does not exist in nodeType"));
	}

	// Try a Categorical AMI object.
	final AttributeCategoricalMetaInfo amif3
	    = graph.getAttributeMetaInfo("myAttributes1", afc);
	// If we reask for it, make sure we get the same reference.
	assertTrue(amif3 == graph.getAttributeMetaInfo("myAttributes1", afc));
	assertEquals("myAttributes1", amif3.getNodeTypeName());
	assertEquals("field3", amif3.getFieldName());
	assertEquals(2.0, amif3.getMax());
	assertEquals(0.0, amif3.getMin());
	assertEquals(1.4, amif3.getMean());
	assertEquals(2.0, amif3.getMedian());
	assertEquals(2, amif3.getMode());
    }

    public static Test suite()
    {
        return new TestSuite(GraphTest.class);
    }
}
