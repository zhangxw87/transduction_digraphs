/**
 * NodeTest.java
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

/**
 * Node Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class NodeTest extends TestCase
{
    private Node node0, node1, node2, node3, node4,
	node5, node6, node7, node8, node9;

    public NodeTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	final Attributes attrs1 = new Attributes("myAttributes1");
	attrs1.add(new AttributeKey("field0"));
	attrs1.add(new AttributeDiscrete("field1"));
	attrs1.add(new AttributeDiscrete("field2"));

	final Attributes attrs2 = new Attributes("myAttributes2");
	attrs2.add(new AttributeKey("Field0"));
	attrs2.add(new AttributeContinuous("Field1"));
	attrs2.add(new AttributeContinuous("Field2"));

	node0 = new Node("node0", attrs1, 0);
	node1 = new Node("node1", attrs1, 1);
	node2 = new Node("node2", attrs1, 2);
	node3 = new Node("node3", attrs1, 3);
	node4 = new Node("node4", attrs1, 4);
	node5 = new Node("node5", attrs2, 5);
	node6 = new Node("node6", attrs2, 6);
	node7 = new Node("node7", attrs2, 7);
	node8 = new Node("node8", attrs2, 8);
	node9 = new Node("node9", attrs2, 9);

	node0.setValues(new double[] { 0, 1, 2 });
	node1.setValues(new double[] { 1, 3, 4 });
	node2.setValues(new double[] { 2, 5, 6 });
	node3.setValues(new double[] { 3, 7, 8 });
	node4.setValues(new double[] { 4, 9, 10 });
	node5.setValues(new double[] { 5, 11, 12 });
	node6.setValues(new double[] { 6, 13, 14 });
	node7.setValues(new double[] { 7, 15, 16 });
	node8.setValues(new double[] { 8, 17, 18 });
	node9.setValues(new double[] { 9, 19, Double.NaN });

	final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	final EdgeType et2 = new EdgeType("myEdgeType2", "myAttributes1", "myAttributes2");
	final EdgeType et3 = new EdgeType("myEdgeType3", "myAttributes2", "myAttributes2");
	final EdgeType et4 = new EdgeType("myEdgeType4", "myAttributes2", "myAttributes2");

	node0.addEdge(new Edge(et1, node0, node1, 2.0));
	node1.addEdge(new Edge(et1, node1, node0, 2.0));

	node0.addEdge(new Edge(et1, node0, node2, 3.0));
	node2.addEdge(new Edge(et1, node2, node0, 3.0));

	node1.addEdge(new Edge(et1, node1, node3, 5.0));
	node3.addEdge(new Edge(et1, node3, node1, 5.0));

	node1.addEdge(new Edge(et1, node1, node4, 6.0));
	node4.addEdge(new Edge(et1, node4, node1, 6.0));
	
	node1.addEdge(new Edge(et2, node1, node6, 5.0));
	node3.addEdge(new Edge(et2, node3, node8, 8.0));
	node3.addEdge(new Edge(et2, node3, node9, 6.5));
	node4.addEdge(new Edge(et2, node4, node5, 3.0));
	node4.addEdge(new Edge(et2, node4, node7, 2.5));
	node4.addEdge(new Edge(et2, node4, node9, 3.5));

	node5.addEdge(new Edge(et3, node5, node7, 4.0));
	node7.addEdge(new Edge(et3, node7, node5, 4.0));
	
	node6.addEdge(new Edge(et3, node6, node7, 1.0));
	node7.addEdge(new Edge(et3, node7, node6, 1.0));
	
	node7.addEdge(new Edge(et3, node7, node8, 4.5));
	node8.addEdge(new Edge(et3, node8, node7, 4.5));

	node8.addEdge(new Edge(et3, node8, node9, 9.0));
	node9.addEdge(new Edge(et3, node9, node8, 9.0));

	node8.addEdge(new Edge(et4, node8, node9, 10.0));
	node9.addEdge(new Edge(et4, node9, node8, 10.0));
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	node0 = node1 = node2 = node3 = node4
	    = node5 = node6 = node7 = node8 = node9 = null;
    }

    public void testGetName() throws Exception
    {
	assertEquals("node0", node0.getName());
	assertEquals("node9", node9.getName());
    }

    public void testGetIndex() throws Exception
    {
	assertEquals(0, node0.getIndex());
	assertEquals(9, node9.getIndex());
    }

    public void testGetType() throws Exception
    {
	assertEquals("myAttributes1", node0.getType());
	assertEquals("myAttributes2", node9.getType());
    }

    public void testGetAttributes() throws Exception
    {
	assertEquals("myAttributes1", node0.getAttributes().getName());
	assertEquals("myAttributes2", node9.getAttributes().getName());
    }

    public void testGetAttributeIndex() throws Exception
    {
	assertEquals(0, node0.getAttributeIndex("field0"));
	assertEquals(1, node0.getAttributeIndex("field1"));
	assertEquals(2, node0.getAttributeIndex("field2"));
	try { // Invalid field
	    node0.getAttributeIndex("blah");
	    fail();
	} catch (RuntimeException success) { }

	assertEquals(0, node9.getAttributeIndex("Field0"));
	assertEquals(1, node9.getAttributeIndex("Field1"));
	assertEquals(2, node9.getAttributeIndex("Field2"));
	try { // Invalid field
	    node9.getAttributeIndex("blah");
	    fail();
	} catch (RuntimeException success) { }
    }

    public void testGetValue() throws Exception
    {
	assertEquals(0.0, node0.getValue(0));
	assertEquals(1.0, node0.getValue(1));
	assertEquals(2.0, node0.getValue(2));

	try { // Invalid index
	    node0.getValue(3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }

	assertEquals(9.0, node9.getValue(0));
	assertEquals(19.0, node9.getValue(1));
	assertEquals(Double.NaN, node9.getValue(2));

	try { // Invalid index
	    node9.getValue(3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }
    }

    public void testIsMissing() throws Exception
    {
	assertFalse(node0.isMissing(0));
	assertFalse(node0.isMissing(1));
	assertFalse(node0.isMissing(2));
	try { // Invalid index
	    node0.isMissing(3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }

	assertFalse(node9.isMissing(0));
	assertFalse(node9.isMissing(1));
	assertTrue(node9.isMissing(2));
	try { // Invalid index
	    node9.isMissing(3);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }
    }

    public void testGetValue1() throws Exception
    {
	assertEquals(0.0, node0.getValue("field0"));
	assertEquals(1.0, node0.getValue("field1"));
	assertEquals(2.0, node0.getValue("field2"));
	try { // Invalid field
            node0.getValue("blah");
            fail();
        } catch (RuntimeException success) { }

	assertEquals(9.0, node9.getValue("Field0"));
	assertEquals(19.0, node9.getValue("Field1"));
	assertEquals(Double.NaN, node9.getValue("Field2"));
	try { // Invalid field
            node9.getValue("blah");
            fail();
        } catch (RuntimeException success) { }
    }

    public void testIsMissing1() throws Exception
    {
	assertFalse(node0.isMissing("field0"));
	assertFalse(node0.isMissing("field1"));
	assertFalse(node0.isMissing("field2"));
	try { // Invalid index
	    node0.isMissing("blah");
	    fail();
	} catch (RuntimeException success) { }
	
	assertFalse(node9.isMissing("Field0"));
	assertFalse(node9.isMissing("Field1"));
	assertTrue(node9.isMissing("Field2"));
	try { // Invalid index
	    node9.isMissing("blah");
	    fail();
	} catch (RuntimeException success) { }
    }

    public void testGetValues() throws Exception
    {
	assertTrue(Arrays.equals(new double[] { 0, 1, 2 }, node0.getValues()));
	assertTrue(Arrays.equals(new double[] { 1, 3, 4 }, node1.getValues()));
	assertTrue(Arrays.equals(new double[] { 2, 5, 6 }, node2.getValues()));
	assertTrue(Arrays.equals(new double[] { 3, 7, 8 }, node3.getValues()));
	assertTrue(Arrays.equals(new double[] { 4, 9, 10 }, node4.getValues()));
	assertTrue(Arrays.equals(new double[] { 5, 11, 12 }, node5.getValues()));
	assertTrue(Arrays.equals(new double[] { 6, 13, 14 }, node6.getValues()));
	assertTrue(Arrays.equals(new double[] { 7, 15, 16 }, node7.getValues()));
	assertTrue(Arrays.equals(new double[] { 8, 17, 18 }, node8.getValues()));
	assertTrue(Arrays.equals(new double[] { 9, 19, Double.NaN }, node9.getValues()));
    }

    public void testSetValue() throws Exception
    {
	node0.setValue(1, 55);
	assertTrue(Arrays.equals(new double[] { 0, 55, 2 }, node0.getValues()));

	try { // Invalid index
	    node0.setValue(3, 120);
	    fail();
	} catch (ArrayIndexOutOfBoundsException success) { }

	try { // Reset the key field
	    node0.setValue(0, 999);
	    fail();
	} catch (RuntimeException success) { }

	// Ensure nothing changed.
	assertTrue(Arrays.equals(new double[] { 0, 55, 2 }, node0.getValues()));
    }

    public void testSetValue1() throws Exception
    {
	node0.setValue("field2", 99);
	assertTrue(Arrays.equals(new double[] { 0, 1, 99 }, node0.getValues()));

	try { // Invalid index
	    node0.setValue("blah", 120);
	    fail();
	} catch (RuntimeException success) { }

	try { // Reset the key field
	    node0.setValue("Field0", 999);
	    fail();
	} catch (RuntimeException success) { }

	// Ensure nothing changed.
	assertTrue(Arrays.equals(new double[] { 0, 1, 99 }, node0.getValues()));
    }

    public void testSetValues() throws Exception
    {
	node0.setValues(new double[] {0,101,102});
	assertTrue(Arrays.equals(new double[] { 0, 101, 102 }, node0.getValues()));

	try { // Reset the key field
	    node0.setValues(new double[] {999,101,102});
	    fail();
        } catch (RuntimeException success) { }

	// Ensure nothing changed.
	assertTrue(Arrays.equals(new double[] { 0, 101, 102 }, node0.getValues()));
    }

    public void testAddEdge() throws Exception
    {
	// The setup method already added valid cases.

	try { // Edge source != this
	    final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	    node0.addEdge(new Edge(et1, node2, node4, 2.0));
	    fail();
	} catch (RuntimeException success) { }
	
	try { // Invalid EdgeType
	    final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	    node0.addEdge(new Edge(et1, node0, node5, 2.0));
	    fail();
	} catch (RuntimeException success) { }
	
	try { // Invalid EdgeType
	    final EdgeType et2 = new EdgeType("myEdgeType2", "myAttributes1", "myAttributes2");
	    node9.addEdge(new Edge(et2, node9, node5, 2.0));
	    fail();
	} catch (RuntimeException success) { }
    }

    public void testGetEdges() throws Exception
    {
	Edge[] edges;

	Arrays.sort(edges = node0.getEdges());
	assertEquals(2, edges.length);
	assertEquals(edges[0].getDest(), node1);
	assertEquals(edges[1].getDest(), node2);

	Arrays.sort(edges = node1.getEdges());
	assertEquals(4, edges.length);
	assertEquals(edges[0].getDest(), node0);
	assertEquals(edges[1].getDest(), node3);
	assertEquals(edges[2].getDest(), node4);
	assertEquals(edges[3].getDest(), node6);
	
	Arrays.sort(edges = node2.getEdges());
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node0);

	Arrays.sort(edges = node3.getEdges());
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node1);
	assertEquals(edges[1].getDest(), node8);
	assertEquals(edges[2].getDest(), node9);

	Arrays.sort(edges = node4.getEdges());
	assertEquals(4, edges.length);
	assertEquals(edges[0].getDest(), node1);
	assertEquals(edges[1].getDest(), node5);
	assertEquals(edges[2].getDest(), node7);
	assertEquals(edges[3].getDest(), node9);

	Arrays.sort(edges = node5.getEdges());
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node7);

	Arrays.sort(edges = node6.getEdges());
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node7);

	Arrays.sort(edges = node7.getEdges());
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node5);
	assertEquals(edges[1].getDest(), node6);
	assertEquals(edges[2].getDest(), node8);

	Arrays.sort(edges = node8.getEdges());
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node7);
	assertEquals(edges[1].getDest(), node9);

	Arrays.sort(edges = node9.getEdges());
	assertEquals(2, edges.length);
	assertEquals(edges[0].getDest(), node8);
    }

    public void testGetEdgesToNeighbor() throws Exception
    {
	Edge[] edges;
	
	edges = node4.getEdgesToNeighbor("myAttributes1");
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node1);
	
	Arrays.sort(edges = node4.getEdgesToNeighbor("myAttributes2"));
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node5);
	assertEquals(edges[1].getDest(), node7);
	assertEquals(edges[2].getDest(), node9);
	
	edges = node4.getEdgesToNeighbor("blah");
	assertEquals(0, edges.length);
    }

    public void testGetEdgesToNeighbor1() throws Exception
    {
	final NodeFilter nf1 = new NodeFilter()
	    { public final boolean accept(Node n) 
		{ return n.getValue(0) > 3; }
	    };

	final NodeFilter nf2 = new NodeFilter()
	    { public final boolean accept(Node n) 
		{ return n.getValue(0) < 3; }
	    };

	Edge[] edges;
	
	edges = node1.getEdgesToNeighbor("myAttributes1", nf1);
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node4);

	edges = node1.getEdgesToNeighbor("myAttributes1", nf2);
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node0);
    }

    public void testGetEdgesByType() throws Exception
    {
	Edge[] edges;
	
	edges = node4.getEdgesByType("myEdgeType1");
	assertEquals(1, edges.length);
	assertEquals(edges[0].getDest(), node1);
	
	Arrays.sort(edges = node4.getEdgesByType("myEdgeType2"));
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node5);
	assertEquals(edges[1].getDest(), node7);
	assertEquals(edges[2].getDest(), node9);
	
	Arrays.sort(edges = node7.getEdgesByType("myEdgeType3"));
	assertEquals(3, edges.length);
	assertEquals(edges[0].getDest(), node5);
	assertEquals(edges[1].getDest(), node6);
	assertEquals(edges[2].getDest(), node8);

	edges = node7.getEdgesByType("blah");
	assertEquals(0, edges.length);
    }

    public void testGetEdgesByType1() throws Exception
    {
	final NodeFilter nf1 = new NodeFilter()
	    { public final boolean accept(Node n) 
		{ return n.getValue(0) > 5; }
	    };

	Edge[] edges;

	Arrays.sort(edges = node4.getEdgesByType("myEdgeType2", nf1));
	assertEquals(2, edges.length);
	assertEquals(edges[0].getDest(), node7);
	assertEquals(edges[1].getDest(), node9);
    }

    public void testGetUnweightedDegree() throws Exception
    {
	assertEquals(3, node1.getUnweightedDegree("myAttributes1"));
	assertEquals(1, node4.getUnweightedDegree("myAttributes1"));
	assertEquals(3, node4.getUnweightedDegree("myAttributes2"));
	assertEquals(0, node4.getUnweightedDegree("blah"));
    }

    public void testGetUnweightedDegree1() throws Exception
    {
	final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	final EdgeType et2 = new EdgeType("myEdgeType2", "myAttributes1", "myAttributes2");
	final EdgeType et3 = new EdgeType("myEdgeType3", "myAttributes2", "myAttributes2");
	final EdgeType et4 = new EdgeType("myEdgeType4", "myAttributes2", "myAttributes2");

	assertEquals(3, node1.getUnweightedDegree(et1));
	assertEquals(1, node4.getUnweightedDegree(et1));
	assertEquals(3, node4.getUnweightedDegree(et2));
	assertEquals(0, node4.getUnweightedDegree(et3));
	assertEquals(2, node8.getUnweightedDegree(et3));
	assertEquals(1, node8.getUnweightedDegree(et4));
    }

    public void testGetWeightedDegree() throws Exception
    {
	assertEquals(13.0, node1.getWeightedDegree("myAttributes1"));
	assertEquals(6.0, node4.getWeightedDegree("myAttributes1"));
	assertEquals(9.0, node4.getWeightedDegree("myAttributes2"));
	assertEquals(0.0, node4.getWeightedDegree("blah"));
    }

    public void testGetWeightedDegree1() throws Exception
    {
	final EdgeType et1 = new EdgeType("myEdgeType1", "myAttributes1", "myAttributes1");
	final EdgeType et2 = new EdgeType("myEdgeType2", "myAttributes1", "myAttributes2");
	final EdgeType et3 = new EdgeType("myEdgeType3", "myAttributes2", "myAttributes2");
	final EdgeType et4 = new EdgeType("myEdgeType4", "myAttributes2", "myAttributes2");

	assertEquals(13.0, node1.getWeightedDegree(et1));
	assertEquals(6.0, node4.getWeightedDegree(et1));
	assertEquals(9.0, node4.getWeightedDegree(et2));
	assertEquals(0.0, node4.getWeightedDegree(et3));
	assertEquals(13.5, node8.getWeightedDegree(et3));
	assertEquals(10.0, node8.getWeightedDegree(et4));
    }

    public void testGetNeighbors() throws Exception
    {
	Node[] nodes;
	
	Arrays.sort(nodes = node0.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node1, node2 }, nodes));
	Arrays.sort(nodes = node1.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node0, node3, node4, node6 }, nodes));
	Arrays.sort(nodes = node2.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node0 }, nodes));
	Arrays.sort(nodes = node3.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node1, node8, node9 }, nodes));
	Arrays.sort(nodes = node4.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node1, node5, node7, node9 }, nodes));
	Arrays.sort(nodes = node5.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node7 }, nodes));
	Arrays.sort(nodes = node6.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node7 }, nodes));
	Arrays.sort(nodes = node7.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node5, node6, node8 }, nodes));
	Arrays.sort(nodes = node8.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node7, node9, node9 }, nodes));
	Arrays.sort(nodes = node9.getNeighbors());
	assertTrue(Arrays.equals(new Node[] { node8, node8 }, nodes));
    }

    public void testGetNeighbors1() throws Exception
    {
	Node[] nodes;
	
	Arrays.sort(nodes = node4.getNeighbors("myEdgeType1"));
	assertTrue(Arrays.equals(new Node[] { node1 }, nodes));
	Arrays.sort(nodes = node4.getNeighbors("myEdgeType2"));
	assertTrue(Arrays.equals(new Node[] { node5, node7, node9 }, nodes));
	try { // Invalid EdgeType name
	    node4.getNeighbors("blah");
	    fail();
	} catch(NullPointerException success) { }
	Arrays.sort(nodes = node9.getNeighbors("myEdgeType3"));
	assertTrue(Arrays.equals(new Node[] { node8 }, nodes));
	Arrays.sort(nodes = node9.getNeighbors("myEdgeType4"));
	assertTrue(Arrays.equals(new Node[] { node8 }, nodes));
    }
    
    public void testGetEdge() throws Exception
    {
	assertEquals(9.0, node8.getEdge("myEdgeType3", node9).getWeight());
	assertEquals(10.0, node8.getEdge("myEdgeType4", node9).getWeight());
    }

    public void testNumEdges() throws Exception
    {
	assertEquals(2, node0.numEdges());
	assertEquals(4, node1.numEdges());
	assertEquals(1, node2.numEdges());
	assertEquals(3, node3.numEdges());
	assertEquals(4, node4.numEdges());
	assertEquals(1, node5.numEdges());
	assertEquals(1, node6.numEdges());
	assertEquals(3, node7.numEdges());
	assertEquals(3, node8.numEdges());
	assertEquals(2, node9.numEdges());
    }

    public void testNumEdges1() throws Exception
    {
	assertEquals(1, node4.numEdges("myEdgeType1"));
	assertEquals(3, node4.numEdges("myEdgeType2"));
	assertEquals(2, node8.numEdges("myEdgeType3"));
	assertEquals(1, node8.numEdges("myEdgeType4"));
    }
    
    public void testHashCode() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testEquals() throws Exception
    {
	assertTrue(node0.equals(node0));
	assertFalse(node0.equals(node1));	
    }

    public void testRemoveEdge() throws Exception
    {
	Node[] nodes;
	Edge[] edges;
	Edge e;

	e = node0.removeEdge("myEdgeType1", node1);
	assertEquals("node1", e.getDest().getName());
	Arrays.sort(edges = node0.getEdges());
	assertEquals(1, edges.length);
	assertEquals(node2, edges[0].getDest());
	Arrays.sort(nodes = node0.getNeighbors());
	assertEquals(1, nodes.length);
	assertEquals(node2, nodes[0]);

	e = node0.removeEdge("myEdgeType1", node2);
	assertEquals("node2", e.getDest().getName());
	Arrays.sort(edges = node0.getEdges());
	assertEquals(0, edges.length);

	try { // Invalid EdgeType
	    node1.removeEdge("blahblah", node6);
	    fail();
	} catch(RuntimeException success) { }
	Arrays.sort(edges = node1.getEdges());
	assertEquals(4, edges.length);

	try { // Missing Edge
	    node1.removeEdge("myEdgeType1", node6);
	    fail();
	} catch(RuntimeException success) { }
	Arrays.sort(edges = node1.getEdges());
	assertEquals(4, edges.length);

	e = node1.removeEdge("myEdgeType2", node6);
	assertEquals("node6", e.getDest().getName());
	Arrays.sort(edges = node1.getEdges());
	assertEquals(3, edges.length);
	assertEquals(node0, edges[0].getDest());
	assertEquals(node3, edges[1].getDest());
	assertEquals(node4, edges[2].getDest());
	Arrays.sort(nodes = node1.getNeighbors());
	assertEquals(3, nodes.length);
	assertEquals(node0, nodes[0]);
	assertEquals(node3, nodes[1]);
	assertEquals(node4, nodes[2]);
	
	e = node1.removeEdge("myEdgeType1", node3);
	assertEquals("node3", e.getDest().getName());
	Arrays.sort(edges = node1.getEdges());
	assertEquals(2, edges.length);
	assertEquals(node0, edges[0].getDest());
	assertEquals(node4, edges[1].getDest());
	Arrays.sort(nodes = node1.getNeighbors());
	assertEquals(2, nodes.length);
	assertEquals(node0, nodes[0]);
	assertEquals(node4, nodes[1]);
	
	e = node1.removeEdge("myEdgeType1", node4);
	assertEquals("node4", e.getDest().getName());
	Arrays.sort(edges = node1.getEdges());
	assertEquals(1, edges.length);
	assertEquals(node0, edges[0].getDest());
	Arrays.sort(nodes = node1.getNeighbors());
	assertEquals(1, nodes.length);
	assertEquals(node0, nodes[0]);
	
	e = node1.removeEdge("myEdgeType1", node0);
	assertEquals("node0", e.getDest().getName());
	Arrays.sort(edges = node1.getEdges());
	assertEquals(0, edges.length);
	Arrays.sort(nodes = node1.getNeighbors());
	assertEquals(0, nodes.length);
    }

    public void testAddValues()
    {
	final Attributes attrs = node0.getAttributes();

	// This should have no effect.
	node0.addValues(); 
	// Ensure we can still access values after synching above.
	node0.getValue(0);

	// Now add a field.
	attrs.add(new AttributeContinuous("field3"));

	// None of these should work yet...
	try {
	    node0.getValue(0);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue(3);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue("field1");
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue("field3");
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.isMissing(0);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValues();
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue(1, 1);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue(2, 1);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue("field1", 10);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue("field3", 29.3);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValues(new double[] { 0, 1, 99, 20.5});
	    fail();
	} catch (RuntimeException success) { }

	// Now resynch.
	node0.addValues();

	// All of these should work now.
	assertTrue(Arrays.equals(new double[] { 0, 1, 2, Double.NaN }, node0.getValues()));
	assertEquals(0.0, node0.getValue(0));
	assertEquals(Double.NaN, node0.getValue(3));
	assertEquals(1.0, node0.getValue("field1"));
	assertEquals(Double.NaN, node0.getValue("field3"));
	assertFalse(node0.isMissing(0));
	assertTrue(node0.isMissing(3));
	assertEquals(4, node0.getValues().length);
	node0.setValue(1, 4);
	node0.setValue(2, 5);
	node0.setValue(3, 33.5);
	assertTrue(Arrays.equals(new double[] { 0, 4, 5, 33.5 }, node0.getValues()));
	node0.setValue("field1", 11);
	node0.setValue("field2", 12);
	node0.setValue("field3", 29.3);
	assertTrue(Arrays.equals(new double[] { 0, 11, 12, 29.3 }, node0.getValues()));
	node0.setValues(new double[] { 0, 1, 99, 20.5});
	assertTrue(Arrays.equals(new double[] { 0, 1, 99, 20.5 }, node0.getValues()));
    }
    
    public void testRemoveValue() throws Exception
    {
	final Attributes attrs = node0.getAttributes();

	try { // Can't remove values until the nodeType has been shrunk.
	    node0.removeValue(1);
	    fail();
	} catch (RuntimeException success) { }

	// Remove a field from the nodeType.
	attrs.remove(1);

	try { // Can't remove KEY index.
	    node0.removeValue(0);
	    fail();
	} catch (RuntimeException success) { }
	    
	try { // Invalid index.
	    node0.removeValue(-1);
	    fail();
	} catch (RuntimeException success) { }
	    
	try { // Invalid index.
	    node0.removeValue(3);
	    fail();
	} catch (RuntimeException success) { }
	    
	// None of these should work yet...
	try {
	    node0.getValue(0);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue(1);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue("field0");
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValue("field1");
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.isMissing(0);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.getValues();
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue(1, 1);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValue("field1", 10);
	    fail();
	} catch (RuntimeException success) { }
	try {
	    node0.setValues(new double[] { 0, 1 });
	    fail();
	} catch (RuntimeException success) { }

	// Now remove a field.
	node0.removeValue(1);

	// All of these should work now.
	assertTrue(Arrays.equals(new double[] { 0, 2 }, node0.getValues()));
	assertEquals(0.0, node0.getValue(0));
	assertEquals(2.0, node0.getValue("field2"));
	assertFalse(node0.isMissing(0));
	assertEquals(2, node0.getValues().length);
	node0.setValue(1, 4);
	assertTrue(Arrays.equals(new double[] { 0, 4 }, node0.getValues()));
	node0.setValue("field2", 11);
	assertTrue(Arrays.equals(new double[] { 0, 11 }, node0.getValues()));
	node0.setValues(new double[] { 0, 100 });
	assertTrue(Arrays.equals(new double[] { 0, 100 }, node0.getValues()));
    }
    
    public void testToString() throws Exception
    {
        //TODO: Test goes here...
    }

    public static Test suite()
    {
        return new TestSuite(NodeTest.class);
    }
}
