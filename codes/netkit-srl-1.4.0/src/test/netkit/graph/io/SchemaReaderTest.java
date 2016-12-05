/**
 * SchemaReaderTest.java
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
import netkit.graph.*;

/**
 * SchemaReader Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class SchemaReaderTest extends TestCase
{
    public SchemaReaderTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testReadGDASchema() throws Exception
    {
	final String nodeInput =
	    "Name,Class,Gender,HairColor\n" +
	    "personA,Good,Male,Brown\n" +
	    "personB,Good,Male,Blond\n" +
	    "personC,Good,Female,Red\n" +
	    "personD,Bad,Male,Brown\n" +
	    "personE,Bad,Female,Brown\n" +
	    "personF,Good,Female,Blond\n" +
	    "personG,Good,Male,Brown\n" +
	    "personH,Good,Female,Brown\n";
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
	final Graph graph = SchemaReader.readGDASchema(new StringReader(nodeInput),
						       new StringReader(edgeInput));
	// Verify we got exactly one.
	final Attributes[] attributes = graph.getAllAttributes();
	assertEquals(1, attributes.length);

	// Verify it was constructed correctly.
	final Attributes attrs = attributes[0];
	assertEquals("GDA", attrs.getName());
	assertEquals(4, attrs.attributeCount());
	assertEquals("Name", attrs.getAttribute(0).getName());
	assertEquals("Class", attrs.getAttribute(1).getName());
	assertEquals("Gender", attrs.getAttribute(2).getName());
	assertEquals("HairColor", attrs.getAttribute(3).getName());
	assertEquals("Name", attrs.getKey().getName());
	for (final Attribute a : attrs)
	    assertTrue(a instanceof AttributeExpandableCategorical);

	// Verify we got exactly one.
	final String[] edgeTypeNames = graph.getEdgeTypeNames();
	assertEquals(1, edgeTypeNames.length);
	// Verify it was named correctly.
	assertEquals("ConnectsTo", edgeTypeNames[0]);
	assertEquals("GDA", graph.getEdgeType(edgeTypeNames[0]).getSourceType());
	assertEquals("GDA", graph.getEdgeType(edgeTypeNames[0]).getDestType());

	// Verify we got the correct number of Nodes and Edges.
	assertEquals(8, graph.numNodes());
	assertEquals(24, graph.numEdges());
    }

    public void testReadSchema() throws Exception
    {
	final String devnull = "/dev/null";
	final String schemaInput =
	    "@nodetype Movie\n" +
	    "@attribute Name KEY\n" +
	    "@attribute Genre {Comedy, Drama, Action, Horror}\n" +
	    "@attribute Oscars IGNORE\n" +
	    "# The `Success' field should have these tokens in the instance file: {Dud, Average, Blockbuster}\n" +
	    "@attribute Success CATEGORICAL\n" +
	    "@nodedata " + devnull + "\n" +
	    "\n" +
	    "@nodetype Actor\n" +
	    "@attribute Name KEY\n" +
	    "@attribute Age IGNORE\n" +
	    "@attribute BirthYear INT\n" +
	    "@nodedata " + devnull + "\n" +
	    "\n" +
	    "% Test @relation as an alias for @nodetype.\n" +
	    "@relation Studio\n" +
	    "@attribute Name KEY\n" +
	    "@attribute Budget IGNORE\n" +
	    "% Test a @data tag\n" +
	    "@data " + devnull + "\n" +
	    "\n" +
	    "@edgetype HasActor Movie Actor\n" +
	    "@Reversible ActsIn\n" +
	    "@edgedata " + devnull + "\n" +
	    "\n" +
	    "@edgetype ProducedBy Movie Studio\n" +
	    "@Reversible Produces\n" +
	    "@edgedata " + devnull + "\n" +
	    "\n" +
	    "% Test an unnamed @reversible tag.\n" +
	    "@edgetype ActsWith Actor Actor\n" +
	    "@reversible\n" +
	    "@edgedata " + devnull + "\n";
	final Graph graph
	    = SchemaReader.readSchema(new StringReader(schemaInput), null);

	// Verify Attributes
	final Attributes[] attributes = graph.getAllAttributes();
	assertEquals(3, attributes.length);

	Attributes attrs = attributes[0];
	assertEquals("Movie", attrs.getName());
	assertEquals(3, attrs.attributeCount());
	assertEquals("Name", attrs.getAttribute(0).getName());
	assertTrue(attrs.getAttribute(0) instanceof AttributeKey);
	assertEquals("Genre", attrs.getAttribute(1).getName());
	assertTrue(attrs.getAttribute(1) instanceof AttributeFixedCategorical);
	assertEquals("Success", attrs.getAttribute(2).getName());
	assertTrue(attrs.getAttribute(2) instanceof AttributeExpandableCategorical);
	assertEquals("Name", attrs.getKey().getName());

	attrs = attributes[1];
	assertEquals("Actor", attrs.getName());
        assertEquals(2, attrs.attributeCount());
        assertEquals("Name", attrs.getAttribute(0).getName());
        assertTrue(attrs.getAttribute(0) instanceof AttributeKey);
        assertEquals("BirthYear", attrs.getAttribute(1).getName());
        assertTrue(attrs.getAttribute(1) instanceof AttributeDiscrete);
	assertEquals("Name", attrs.getKey().getName());

	attrs = attributes[2];
	assertEquals("Studio", attrs.getName());
        assertEquals(1, attrs.attributeCount());
        assertEquals("Name", attrs.getAttribute(0).getName());
        assertTrue(attrs.getAttribute(0) instanceof AttributeKey);
	assertEquals("Name", attrs.getKey().getName());

	// Verify EdgeTypes.
	final String[] edgeTypeNames = graph.getEdgeTypeNames();
	assertEquals(5, edgeTypeNames.length);

	int i = 0;
	assertEquals("HasActor", edgeTypeNames[i]);
	assertEquals("Movie", graph.getEdgeType(edgeTypeNames[i]).getSourceType());
	assertEquals("Actor", graph.getEdgeType(edgeTypeNames[i]).getDestType());
	i++;
	assertEquals("ActsIn", edgeTypeNames[i]);
	assertEquals("Actor", graph.getEdgeType(edgeTypeNames[i]).getSourceType());
	assertEquals("Movie", graph.getEdgeType(edgeTypeNames[i]).getDestType());
	i++;
	assertEquals("ProducedBy", edgeTypeNames[i]);
	assertEquals("Movie", graph.getEdgeType(edgeTypeNames[i]).getSourceType());
	assertEquals("Studio", graph.getEdgeType(edgeTypeNames[i]).getDestType());
	i++;
	assertEquals("Produces", edgeTypeNames[i]);
	assertEquals("Studio", graph.getEdgeType(edgeTypeNames[i]).getSourceType());
	assertEquals("Movie", graph.getEdgeType(edgeTypeNames[i]).getDestType());
	i++;
	assertEquals("ActsWith", edgeTypeNames[i]);
	assertEquals("Actor", graph.getEdgeType(edgeTypeNames[i]).getSourceType());
	assertEquals("Actor", graph.getEdgeType(edgeTypeNames[i]).getDestType());

	// Verify we got zero Nodes and Edges.
	assertEquals(0, graph.numNodes());
	assertEquals(0, graph.numEdges());
    }

    public void testStressTest() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testMain() throws Exception
    {
        //TODO: Test goes here...
    }

    public static Test suite()
    {
        return new TestSuite(SchemaReaderTest.class);
    }
}
