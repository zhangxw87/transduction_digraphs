/**
 * SchemaWriterTest.java
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
import java.util.*;
import netkit.graph.*;

/**
 * SchemaWriter Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class SchemaWriterTest extends TestCase
{
    public SchemaWriterTest(String name)
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

    public void testWriteSchema() throws Exception
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
	    "@attribute FPfield CONTINUOUS\n" +
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

	/* Output the schema.  */
	final Map<String,String> nodeTypeFiles = new HashMap<String,String>();
	for (final String nodeTypeName : graph.getNodeTypes())
	    nodeTypeFiles.put(nodeTypeName, devnull);
	final Map<String,String> edgeTypeFiles = new HashMap<String,String>();
	for (final String edgeTypeName : graph.getEdgeTypeNames())
	    edgeTypeFiles.put(edgeTypeName, devnull);
	final StringWriter swSchema = new StringWriter();
	SchemaWriter.writeSchema(graph, swSchema, nodeTypeFiles, edgeTypeFiles);
	
	//System.err.println(swSchema);

	/* Read back the schema into graph2.  */
	final Graph graph2 
	    = SchemaReader.readSchema(new StringReader(swSchema.toString()), null);
	
	/* Now check that everything is the same.  */
	final Attributes[] attrs = graph.getAllAttributes(),
	    attrs2 = graph2.getAllAttributes();
	assertTrue(Arrays.equals(attrs, attrs2));
	for (int i=0; i<attrs.length; i++)
	{
	    assertEquals(attrs[i].attributeCount(), attrs2[i].attributeCount());
	    for (int j=0; j<attrs[i].attributeCount(); j++)
	    {
		final Attribute attr = attrs[i].getAttribute(j),
		    attr2 = attrs2[i].getAttribute(j);
		assertEquals(attr.getName(), attr2.getName());
		assertEquals(attr.getType(), attr2.getType());
		if (attr instanceof AttributeCategorical)
		    assertTrue(Arrays.equals(((AttributeCategorical)attr).getTokens(),
					     ((AttributeCategorical)attr2).getTokens()));
	    }
	}

	final String[] etNames = graph.getEdgeTypeNames(),
	    etNames2 = graph2.getEdgeTypeNames();
	assertTrue(Arrays.equals(etNames, etNames2));
	for (int i=0; i<etNames.length; i++)
	{
	    final EdgeType et = graph.getEdgeType(etNames[i]),
		et2 = graph2.getEdgeType(etNames2[i]);
	    assertEquals(et.getSourceType(), et2.getSourceType());
	    assertEquals(et.getDestType(), et2.getDestType());
	}
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
        return new TestSuite(SchemaWriterTest.class);
    }
}
