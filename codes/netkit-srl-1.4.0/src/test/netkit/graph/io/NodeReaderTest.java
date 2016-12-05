/**
 * NodeReaderTest.java
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

package netkit.graph.io;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import java.io.StringReader;
import java.util.Arrays;
import netkit.graph.*;

/**
 * NodeReader Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class NodeReaderTest extends TestCase
{
    private Graph graph;

    public NodeReaderTest(String name)
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
	attrs1.add(new AttributeContinuous("field2"));
	final FixedTokenSet fts = new FixedTokenSet(new String[] {"Low","Medium","High" });
	attrs1.add(new AttributeFixedCategorical("field3", fts));
	attrs1.add(new AttributeExpandableCategorical("field4"));
	graph.addAttributes(attrs1);

	final Attributes attrs2 = new Attributes("myAttributes2");
	attrs2.add(new AttributeKey("Field0"));
        attrs2.add(new AttributeDiscrete("Field1"));
        attrs2.add(new AttributeContinuous("Field2"));
	graph.addAttributes(attrs2);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	graph = null;
    }

    public void testReadNodes() throws Exception
    {
	// This input should pass through successfully.
	final String testString1 = 
	    "# This is a comment\n" +
	    "% This is another comment\n" +
	    " \t # This is a comment with leading whitespace\n" +
	    " \t % This is another comment with leading whitespace\n" +

	    // Test a blank line which should be skipped
	    "\n" +

	    // Test a line with only spaces and tabs which should be skipped
	    "  \t\t   \t \t \t \t  \t\t\t \n" +

	    // Here is some real instance data.
	    "node0,10,20.0,High,foo\n" +

	    // Test leading and trailing whitespace.
	    " \t node1,11,21.0,Medium,bar   \t\t\n" +

	    // Another regular line
	    "node2,12,22.0,Low,baz\n" +

	    // Test "unknown" field values.
	    "node3,?,?,?,?\n";
	NodeReader.readNodes(graph, "myAttributes1", new StringReader(testString1), false);

	final String testString2 =
	    // A first line to skip
	    "Name,value1,value2\n" +

	    // Here's the data
	    "Node0,100,200.0\n" +
	    "Node1,101,201.0\n" +
	    "Node2,102,202.0\n" +
	    "Node3,?,?\n";
	NodeReader.readNodes(graph, "myAttributes2", new StringReader(testString2), true);

	final Node[] nodes = graph.getNodes();
	assertEquals(8, nodes.length);

	assertEquals("node0", nodes[0].getName());
	assertEquals("node1", nodes[1].getName());
	assertEquals("node2", nodes[2].getName());
	assertEquals("node3", nodes[3].getName());
	assertTrue(Arrays.equals(new double[] { 0,10,20,2,0 }, nodes[0].getValues()));
	assertTrue(Arrays.equals(new double[] { 1,11,21,1,1 }, nodes[1].getValues()));
	assertTrue(Arrays.equals(new double[] { 2,12,22,0,2 }, nodes[2].getValues()));
	assertTrue(Arrays.equals(new double[] { 3,Double.NaN,Double.NaN,Double.NaN,Double.NaN },
				 nodes[3].getValues()));

	assertEquals("Node0", nodes[4].getName());
	assertEquals("Node1", nodes[5].getName());
	assertEquals("Node2", nodes[6].getName());
	assertEquals("Node3", nodes[7].getName());
	assertTrue(Arrays.equals(new double[] { 0,100,200 }, nodes[4].getValues()));
	assertTrue(Arrays.equals(new double[] { 1,101,201 }, nodes[5].getValues()));
	assertTrue(Arrays.equals(new double[] { 2,102,202 }, nodes[6].getValues()));
	assertTrue(Arrays.equals(new double[] { 3,Double.NaN,Double.NaN },
				 nodes[7].getValues()));
    }

    public void testReadNodes1() throws Exception
    {
	try { // An extra field
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10,20.0,High,foo,0\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }

    public void testReadNodes2() throws Exception
    {
	try { // A missing field
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10,20.0,High\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }

    public void testReadNodes3() throws Exception
    {
	try { // Bad data for DISCRETE "10.0"
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10.0,20.0,High,foo\n"),
				 false);
	    fail();
	} catch (NumberFormatException success) { }
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }

    public void testReadNodes4() throws Exception
    {
	try { // Bad data for CONTINUOUS "X"
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10,X,High,foo\n"),
				 false);
	    fail();
	} catch (NumberFormatException success) { }
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }

    public void testReadNodes5() throws Exception
    {
	try { // Bad data for CATEGORICAL "blah"
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10,20.0,blah,foo\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Parsed invalid token"));
	}
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }

    public void testReadNodes6() throws Exception
    {
	try { // Bad node type "blah".
	    NodeReader.readNodes(graph, "blah",
				 new StringReader("node0,10,20.0,High,foo\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("No Attributes matching node type"));
	}
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }
    
    public void testReadNodes7() throws Exception
    {
	try { // Duplicate node name.
	    NodeReader.readNodes(graph, "myAttributes1",
				 new StringReader("node0,10,20.0,High,foo\n" +
						  "node0,11,21.0,Low,bar\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("already contains"));
	}
	// Make sure only the first Node got in.
	assertEquals(1, graph.numNodes());
	final Node[] nodes = graph.getNodes();
	assertEquals(1, nodes.length);
	assertEquals("node0", nodes[0].getName());
	assertTrue(Arrays.equals(new double[] { 0,10,20,2,0 }, nodes[0].getValues()));
    }
    
    public void testReadNodes8() throws Exception
    {
	final Attributes attrs3 = new Attributes("myAttributes3");
        attrs3.add(new AttributeDiscrete("F1"));
        attrs3.add(new AttributeContinuous("F2"));
	graph.addAttributes(attrs3);

	try { // Attributes container is missing the key field.
	    NodeReader.readNodes(graph, "myAttributes3",
				 new StringReader("10,20.0\n"),
				 false);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("No key Attribute in node type"));
	}
	// Make sure no Nodes sneaked in.
	assertEquals(0, graph.numNodes());
    }
    
    public static Test suite()
    {
        return new TestSuite(NodeReaderTest.class);
    }
}
