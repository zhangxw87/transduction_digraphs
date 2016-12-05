/**
 * NodeWriterTest.java
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
import java.io.StringWriter;
import java.util.Arrays;
import netkit.graph.*;

/**
 * NodeWriter Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class NodeWriterTest extends TestCase
{
    private Graph graph;

    private final String nodeInputMovie =
	"%name,genre,success\n" +
	"Terminator,Action,Blockbuster\n" +
	"Terminator2,Action,Blockbuster\n" +
	"Terminator3,Action,Blockbuster\n" +
	"KindergardenCop,Comedy,Average\n" +
	"LastActionHero,Comedy,Dud\n" +
	"TrueLies,Action,Blockbuster\n" +
	"Halloween,Horror,Blockbuster\n" +
	"BlueSteel,Drama,Dud\n" +
	"FreakyFriday,Comedy,Average\n" +
	"JonnyDangerously,Comedy,Dud\n" +
	"OtherPeoplesMoney,Comedy,Dud\n" +
	"GetShorty,Drama,Average\n" +
	"UnknownMovie,?,?\n" +
	"Twins,Comedy,Blockbuster\n";
    private final String nodeInputActor =
	"ArnoldSchwarzenegger,1947\n" +
	"JamieLeeCurtis,1958\n" +
	"DannyDeVito,1944\n";
    private final String nodeInputStudio =
	"WoodrowStudios,1.2\n" +
	"MJM,3.3333333333333333333333333333333333\n" +
	"Tinsletown,9.9999999999999999\n" +
	"Wubba,123.456\n" +
	"UnknownStudio,?\n";

    public NodeWriterTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	graph = new Graph();

	Attributes attr = new Attributes("Movie");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeFixedCategorical("Genre",
					       new FixedTokenSet(new String[] {"Comedy","Drama","Action","Horror" })));
	attr.add(new AttributeExpandableCategorical("Success"));
	graph.addAttributes(attr);
	NodeReader.readNodes(graph, attr.getName(), new StringReader(nodeInputMovie), false);

	attr = new Attributes("Actor");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeDiscrete("BirthYear"));
	graph.addAttributes(attr);
	NodeReader.readNodes(graph, attr.getName(), new StringReader(nodeInputActor), false);

	attr = new Attributes("Studio");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeContinuous("FPfield"));
	graph.addAttributes(attr);
	NodeReader.readNodes(graph, attr.getName(), new StringReader(nodeInputStudio), false);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	graph = null;
    }

    public void testWriteNodes() throws Exception
    {
	final Graph graph2 = new Graph();

	Attributes attr = new Attributes("Movie");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeFixedCategorical("Genre",
					       new FixedTokenSet(new String[] {"Comedy","Drama","Action","Horror" })));
	attr.add(new AttributeExpandableCategorical("Success"));
	graph2.addAttributes(attr);

	attr = new Attributes("Actor");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeDiscrete("BirthYear"));
	graph2.addAttributes(attr);

	attr = new Attributes("Studio");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeContinuous("FPfield"));
	graph2.addAttributes(attr);

	/* Output the Node data.  */
	final StringWriter swActor = new StringWriter(),
	    swMovie = new StringWriter(),
	    swStudio = new StringWriter();
	NodeWriter.writeNodes(graph.getNodes("Actor"), swActor);
	NodeWriter.writeNodes(graph.getNodes("Movie"), swMovie);
	NodeWriter.writeNodes(graph.getNodes("Studio"), swStudio);
	
	//	System.err.println("<"+swActor+">");
	//	System.err.println("<"+swMovie+">");
	//	System.err.println("<"+swStudio+">");

	/* Read back the Node data into graph2.  */
	NodeReader.readNodes(graph2, "Actor",
			     new StringReader(swActor.toString()), false);
	NodeReader.readNodes(graph2, "Movie",
			     new StringReader(swMovie.toString()), false);
	NodeReader.readNodes(graph2, "Studio",
			     new StringReader(swStudio.toString()), false);
	
	/* Now check that everything is the same.  */
	final Node[] nodes = graph.getNodes(), nodes2 = graph2.getNodes();
	Arrays.sort(nodes);
	Arrays.sort(nodes2);
	assertTrue(Arrays.equals(nodes, nodes2));
	/* Node.equals() does not validate the values, check them.  */
	for (int i=0; i<nodes.length; i++)
	    assertTrue(Arrays.equals(nodes[i].getValues(),nodes2[i].getValues()));
    }

    public static Test suite()
    {
        return new TestSuite(NodeWriterTest.class);
    }
}
