/**
 * EdgeWriterRNTest.java
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
 * EdgeWriterRN Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class EdgeWriterRNTest extends TestCase
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
	"Twins,Comedy,Blockbuster\n";
    private final String nodeInputActor =
	"ArnoldSchwarzenegger,1947\n" +
	"JamieLeeCurtis,1958\n" +
	"DannyDeVito,1944\n";
    private final String nodeInputStudio =
	"WoodrowStudios\n" +
	"MJM\n" +
	"Tinsletown\n" +
	"Wubba\n";

    public EdgeWriterRNTest(String name)
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
	graph.addAttributes(attr);
	NodeReader.readNodes(graph, attr.getName(), new StringReader(nodeInputStudio), false);

	graph.addEdgeType(new EdgeType("HasActor", "Movie", "Actor"));
	graph.addEdgeType(new EdgeType("ActsIn", "Actor", "Movie"));
	graph.addEdgeType(new EdgeType("ProducedBy", "Movie", "Studio"));
	graph.addEdgeType(new EdgeType("Produces", "Studio", "Movie"));
	graph.addEdgeType(new EdgeType("ActsWith", "Actor", "Actor"));
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	graph = null;
    }

    public void testReadEdges() throws Exception
    {
	final String mov2act =
	    "Terminator,ArnoldSchwarzenegger,1\n" +
	    "Terminator2,ArnoldSchwarzenegger,1\n" +
	    "Terminator3,ArnoldSchwarzenegger,1\n" +
	    "KindergardenCop,ArnoldSchwarzenegger,1\n" +
	    "LastActionHero,ArnoldSchwarzenegger,1\n" +
	    "TrueLies,ArnoldSchwarzenegger,1\n" +
	    "TrueLies,JamieLeeCurtis,1\n" +
	    "Halloween,JamieLeeCurtis,1\n" +
	    "BlueSteel,JamieLeeCurtis,1\n" +
	    "FreakyFriday,JamieLeeCurtis,1\n" +
	    "JonnyDangerously,DannyDeVito,1\n" +
	    "OtherPeoplesMoney,DannyDeVito,1\n" +
	    "GetShorty,DannyDeVito,1\n" +
	    "Twins,DannyDeVito,1\n" +
	    "Twins,ArnoldSchwarzenegger,1\n";
	final String mov2std =
	    "Terminator,WoodrowStudios,2\n" +
	    "Terminator2,WoodrowStudios,2\n" +
	    "Terminator3,WoodrowStudios,2\n" +
	    "KindergardenCop,MJM,2\n" +
	    "LastActionHero,WoodrowStudios,2\n" +
	    "TrueLies,WoodrowStudios,2\n" +
	    "Halloween,Tinsletown,2\n" +
	    "BlueSteel,Wubba,2\n" +
	    "FreakyFriday,MJM,2\n" +
	    "JonnyDangerously,MJM,2\n" +
	    "OtherPeoplesMoney,Tinsletown,2\n" +
	    "GetShorty,Wubba,2\n" +
	    "Twins,MJM,2";
	final String act2act =
	    "ArnoldSchwarzenegger,JamieLeeCurtis,1\n" +
	    "ArnoldSchwarzenegger,DannyDeVito,1\n";

	EdgeReaderRN.readEdges(new StringReader(mov2act), graph,
			       graph.getEdgeType("HasActor"),
			       graph.getEdgeType("ActsIn"));
	EdgeReaderRN.readEdges(new StringReader(mov2std), graph,
			       graph.getEdgeType("ProducedBy"),
			       graph.getEdgeType("Produces"));
	EdgeReaderRN.readEdges(new StringReader(act2act), graph,
			       graph.getEdgeType("ActsWith"),
			       graph.getEdgeType("ActsWith"));
	assertEquals(20, graph.numNodes());
	assertEquals(60, graph.numEdges());

	/* Setup the second Graph.  */
	final Graph graph2 = new Graph();
	Attributes attr = new Attributes("Movie");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeFixedCategorical("Genre",
					       new FixedTokenSet(new String[] {"Comedy","Drama","Action","Horror" })));
	attr.add(new AttributeExpandableCategorical("Success"));
	graph2.addAttributes(attr);
	NodeReader.readNodes(graph2, attr.getName(), new StringReader(nodeInputMovie), false);

	attr = new Attributes("Actor");
	attr.add(new AttributeKey("Name"));
	attr.add(new AttributeDiscrete("BirthYear"));
	graph2.addAttributes(attr);
	NodeReader.readNodes(graph2, attr.getName(), new StringReader(nodeInputActor), false);

	attr = new Attributes("Studio");
	attr.add(new AttributeKey("Name"));
	graph2.addAttributes(attr);
	NodeReader.readNodes(graph2, attr.getName(), new StringReader(nodeInputStudio), false);

	graph2.addEdgeType(new EdgeType("HasActor", "Movie", "Actor"));
	graph2.addEdgeType(new EdgeType("ActsIn", "Actor", "Movie"));
	graph2.addEdgeType(new EdgeType("ProducedBy", "Movie", "Studio"));
	graph2.addEdgeType(new EdgeType("Produces", "Studio", "Movie"));
	graph2.addEdgeType(new EdgeType("ActsWith", "Actor", "Actor"));
	

	/* Output the Edge data.  */
	final StringWriter swM2A = new StringWriter(),
	    swA2M = new StringWriter(),
	    swM2S = new StringWriter(),
	    swS2M = new StringWriter(),
	    swA2A = new StringWriter();
	EdgeWriterRN.writeEdges(graph.getEdges(graph.getEdgeType("HasActor")), swM2A);
	EdgeWriterRN.writeEdges(graph.getEdges(graph.getEdgeType("ActsIn")), swA2M);
	EdgeWriterRN.writeEdges(graph.getEdges(graph.getEdgeType("ProducedBy")), swM2S);
	EdgeWriterRN.writeEdges(graph.getEdges(graph.getEdgeType("Produces")), swS2M);
	EdgeWriterRN.writeEdges(graph.getEdges(graph.getEdgeType("ActsWith")), swA2A);
	
	/* Read back the Edge data into graph2.  */
	EdgeReaderRN.readEdges(new StringReader(swM2A.toString()), graph2,
			       graph2.getEdgeType("HasActor"), null);
	EdgeReaderRN.readEdges(new StringReader(swA2M.toString()), graph2,
			       graph2.getEdgeType("ActsIn"), null);
	EdgeReaderRN.readEdges(new StringReader(swM2S.toString()), graph2,
			       graph2.getEdgeType("ProducedBy"), null);
	EdgeReaderRN.readEdges(new StringReader(swS2M.toString()), graph2,
			       graph2.getEdgeType("Produces"), null);
	EdgeReaderRN.readEdges(new StringReader(swA2A.toString()), graph2,
			       graph2.getEdgeType("ActsWith"), null);

	/* Now check that everything is the same.  */
	final Edge[] edges = graph.getEdges(), edges2 = graph2.getEdges();
	Arrays.sort(edges);
	Arrays.sort(edges2);
	assertTrue(Arrays.equals(edges, edges2));
	/* Edge.equals() does not validate the weight, check it.  */
	for (int i=0; i<edges.length; i++)
	    assertEquals(edges[i].getWeight(),edges2[i].getWeight());
    }

    public static Test suite()
    {
        return new TestSuite(EdgeWriterRNTest.class);
    }
}
