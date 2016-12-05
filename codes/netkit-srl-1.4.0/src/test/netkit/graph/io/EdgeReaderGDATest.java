/**
 * EdgeReaderGDATest.java
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
import java.io.*;
import java.util.Arrays;
import netkit.graph.*;

/**
 * EdgeReaderGDA Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class EdgeReaderGDATest extends TestCase
{
    private Graph graph;

    private final String nodeInput =
	"Name,Class,Gender,HairColor\n" +
	"personA,Good,Male,Brown\n" +
	"personB,Good,Male,Blond\n" +
	"personC,Good,Female,Red\n" +
	"personD,Bad,Male,Brown\n" +
	"personE,Bad,Female,Brown\n" +
	"personF,Good,Female,Blond\n" +
	"personG,Good,Male,Brown\n" +
	"personH,Good,Female,Brown\n";

    public EdgeReaderGDATest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	graph = new Graph();
	final Attributes attr = new Attributes("GDA");
	final BufferedReader br = new BufferedReader(new StringReader(nodeInput));
	final String[] tokens = br.readLine().trim().split(",");
	attr.add(new AttributeKey(tokens[0]));
	for (int i=1; i<tokens.length; i++)
	    attr.add(new AttributeExpandableCategorical(tokens[i]));
	graph.addAttributes(attr);
	NodeReader.readNodes(graph, attr.getName(), br, false);
	final EdgeType edgeType = new EdgeType("ConnectsTo", "GDA", "GDA");
	graph.addEdgeType(edgeType);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	graph = null;
    }

    // Test successful insertion.
    public void testReadEdges() throws Exception
    {
	final String edgeInput =
	    "link,entity\n" +
	    "link1,personA\n" +
	    "link1,personB\n" +
	    "link1,personC\n" +
	    "link2,personA\n" +
	    "link2,personB\n" +
	    "link3,personA\n" +
	    "link3,personC\n" +
	    "link4,personA\n" +
	    "link4,personC\n" +
	    "link5,personD\n" +
	    "link5,personE\n" +
	    "link6,personE\n" +
	    "link6,personD\n" +
	    "link7,personF\n" +
	    "link7,personG\n" +
	    "link7,personH\n" +
	    "link8,personF\n" +
	    "link8,personH\n" +
	    "link9,personH\n" +
	    "link9,personG\n" +
	    "link10,personF\n" +
	    "link10,personG\n" +
	    "link11,personA\n" +
	    "link11,personB\n" +
	    "link11,personC\n" +
	    "link11,personF\n" +
	    "link12,personC\n" +
	    "link12,personD\n" +
	    "link12,personE\n";

	EdgeReaderGDA.readEdges(new StringReader(edgeInput), graph,
				graph.getEdgeType("ConnectsTo"));
	assertEquals(8, graph.numNodes());
	assertEquals(24, graph.numEdges());

	final Edge[] edges = graph.getEdges();
	Arrays.sort(edges);
	assertEquals(24, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("ConnectsTo", e.getEdgeType().getName());

	int i = 0;
	assertEquals("personA", edges[i].getSource().getName());
	assertEquals("personB", edges[i].getDest().getName());
	assertEquals(3.0, edges[i].getWeight());

	i++;
	assertEquals("personA", edges[i].getSource().getName());
	assertEquals("personC", edges[i].getDest().getName());
	assertEquals(4.0, edges[i].getWeight());

	i++;
	assertEquals("personA", edges[i].getSource().getName());
	assertEquals("personF", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personB", edges[i].getSource().getName());
	assertEquals("personA", edges[i].getDest().getName());
	assertEquals(3.0, edges[i].getWeight());

	i++;
	assertEquals("personB", edges[i].getSource().getName());
	assertEquals("personC", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personB", edges[i].getSource().getName());
	assertEquals("personF", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personC", edges[i].getSource().getName());
	assertEquals("personA", edges[i].getDest().getName());
	assertEquals(4.0, edges[i].getWeight());

	i++;
	assertEquals("personC", edges[i].getSource().getName());
	assertEquals("personB", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personC", edges[i].getSource().getName());
	assertEquals("personD", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personC", edges[i].getSource().getName());
	assertEquals("personE", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personC", edges[i].getSource().getName());
	assertEquals("personF", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personD", edges[i].getSource().getName());
	assertEquals("personC", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personD", edges[i].getSource().getName());
	assertEquals("personE", edges[i].getDest().getName());
	assertEquals(3.0, edges[i].getWeight());

	i++;
	assertEquals("personE", edges[i].getSource().getName());
	assertEquals("personC", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personE", edges[i].getSource().getName());
	assertEquals("personD", edges[i].getDest().getName());
	assertEquals(3.0, edges[i].getWeight());

	i++;
	assertEquals("personF", edges[i].getSource().getName());
	assertEquals("personA", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personF", edges[i].getSource().getName());
	assertEquals("personB", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personF", edges[i].getSource().getName());
	assertEquals("personC", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("personF", edges[i].getSource().getName());
	assertEquals("personG", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personF", edges[i].getSource().getName());
	assertEquals("personH", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personG", edges[i].getSource().getName());
	assertEquals("personF", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personG", edges[i].getSource().getName());
	assertEquals("personH", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personH", edges[i].getSource().getName());
	assertEquals("personF", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("personH", edges[i].getSource().getName());
	assertEquals("personG", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);
    }

    public void testReadEdges1() throws Exception
    {
	graph.addAttributes(new Attributes("GDA2"));
	final EdgeType edgeType = new EdgeType("bad", "GDA", "GDA2");
	graph.addEdgeType(edgeType);

	try { // EdgeType with different source/dest node types.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\n"),
				    graph, edgeType);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("only one node type"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges2() throws Exception
    {
	try { // An extra field.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1,personA,0\n"),
				    graph, graph.getEdgeType("ConnectsTo"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges3() throws Exception
    {
	try { // A missing field.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1\n"),
				    graph, graph.getEdgeType("ConnectsTo"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges4() throws Exception
    {
	try { // Unknown Node name, first Node.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1,personX\nlink1,personA\n"),
				    graph, graph.getEdgeType("ConnectsTo"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find node"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges5() throws Exception
    {
	try { // Unknown Node name, second Node.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1,personA\nlink1,personX\n"),
				    graph, graph.getEdgeType("ConnectsTo"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find node"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges6() throws Exception
    {
	final Attributes attr = new Attributes("GDA2");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeExpandableCategorical("Class"));
	attr.add(new AttributeExpandableCategorical("Gender"));
	attr.add(new AttributeExpandableCategorical("HairColor"));
	graph.addAttributes(attr);
	final EdgeType edgeType = new EdgeType("bad", "GDA2", "GDA2");
	graph.addEdgeType(edgeType);

	try { // Unknown Node type.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1,personA\nlink1,personB\n"),
				    graph, edgeType);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find node"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public void testReadEdges7() throws Exception
    {
	try { // Dangling link id.
	    EdgeReaderGDA.readEdges(new StringReader("link,entity\nlink1,personA\n"),
				    graph, graph.getEdgeType("ConnectsTo"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Dangling link identifier"));
	}
	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }

    public static Test suite()
    {
        return new TestSuite(EdgeReaderGDATest.class);
    }
}
