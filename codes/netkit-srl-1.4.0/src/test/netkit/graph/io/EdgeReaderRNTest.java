/**
 * EdgeReaderRNTest.java
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
 * EdgeReaderRN Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class EdgeReaderRNTest extends TestCase
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

    public EdgeReaderRNTest(String name)
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
	    "# This is a comment\n" +
	    "% This is another comment\n" +
	    " \t # This is a comment with leading whitespace\n" +
	    " \t % This is another comment with leading whitespace\n" +

	    // Test a blank line which should be skipped
	    "\n" +

	    // Test a line with only spaces and tabs which should be skipped
	    "  \t\t   \t \t \t \t  \t\t\t \n" +

	    // Here is some real instance data.
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

	Edge[] edges;
	Arrays.sort(edges = graph.getEdges(graph.getEdgeType("HasActor")));
	assertEquals(15, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("HasActor", e.getEdgeType().getName());

	int i = 0;
	assertEquals("BlueSteel", edges[i].getSource().getName());
	assertEquals("JamieLeeCurtis", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("FreakyFriday", edges[i].getSource().getName());
	assertEquals("JamieLeeCurtis", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("GetShorty", edges[i].getSource().getName());
	assertEquals("DannyDeVito", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Halloween", edges[i].getSource().getName());
	assertEquals("JamieLeeCurtis", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JonnyDangerously", edges[i].getSource().getName());
	assertEquals("DannyDeVito", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("KindergardenCop", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("LastActionHero", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("OtherPeoplesMoney", edges[i].getSource().getName());
	assertEquals("DannyDeVito", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator2", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator3", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("TrueLies", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("TrueLies", edges[i].getSource().getName());
	assertEquals("JamieLeeCurtis", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Twins", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("Twins", edges[i].getSource().getName());
	assertEquals("DannyDeVito", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);



	Arrays.sort(edges = graph.getEdges(graph.getEdgeType("ActsIn")));
	assertEquals(15, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("ActsIn", e.getEdgeType().getName());

	i = 0;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("KindergardenCop", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("LastActionHero", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("Terminator", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("Terminator2", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("Terminator3", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("TrueLies", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("Twins", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("DannyDeVito", edges[i].getSource().getName());
	assertEquals("GetShorty", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("DannyDeVito", edges[i].getSource().getName());
	assertEquals("JonnyDangerously", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("DannyDeVito", edges[i].getSource().getName());
	assertEquals("OtherPeoplesMoney", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("DannyDeVito", edges[i].getSource().getName());
	assertEquals("Twins", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JamieLeeCurtis", edges[i].getSource().getName());
	assertEquals("BlueSteel", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JamieLeeCurtis", edges[i].getSource().getName());
	assertEquals("FreakyFriday", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JamieLeeCurtis", edges[i].getSource().getName());
	assertEquals("Halloween", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JamieLeeCurtis", edges[i].getSource().getName());
	assertEquals("TrueLies", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);



	Arrays.sort(edges = graph.getEdges(graph.getEdgeType("ProducedBy")));
	assertEquals(13, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("ProducedBy", e.getEdgeType().getName());

	i = 0;
	assertEquals("BlueSteel", edges[i].getSource().getName());
	assertEquals("Wubba", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("FreakyFriday", edges[i].getSource().getName());
	assertEquals("MJM", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("GetShorty", edges[i].getSource().getName());
	assertEquals("Wubba", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Halloween", edges[i].getSource().getName());
	assertEquals("Tinsletown", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("JonnyDangerously", edges[i].getSource().getName());
	assertEquals("MJM", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("KindergardenCop", edges[i].getSource().getName());
	assertEquals("MJM", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("LastActionHero", edges[i].getSource().getName());
	assertEquals("WoodrowStudios", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("OtherPeoplesMoney", edges[i].getSource().getName());
	assertEquals("Tinsletown", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator", edges[i].getSource().getName());
	assertEquals("WoodrowStudios", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator2", edges[i].getSource().getName());
	assertEquals("WoodrowStudios", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Terminator3", edges[i].getSource().getName());
	assertEquals("WoodrowStudios", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("TrueLies", edges[i].getSource().getName());
	assertEquals("WoodrowStudios", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Twins", edges[i].getSource().getName());
	assertEquals("MJM", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);


	
	Arrays.sort(edges = graph.getEdges(graph.getEdgeType("Produces")));
	assertEquals(13, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("Produces", e.getEdgeType().getName());

	i = 0;
	assertEquals("MJM", edges[i].getSource().getName());
	assertEquals("FreakyFriday", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("MJM", edges[i].getSource().getName());
	assertEquals("JonnyDangerously", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("MJM", edges[i].getSource().getName());
	assertEquals("KindergardenCop", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("MJM", edges[i].getSource().getName());
	assertEquals("Twins", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Tinsletown", edges[i].getSource().getName());
	assertEquals("Halloween", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Tinsletown", edges[i].getSource().getName());
	assertEquals("OtherPeoplesMoney", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("WoodrowStudios", edges[i].getSource().getName());
	assertEquals("LastActionHero", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("WoodrowStudios", edges[i].getSource().getName());
	assertEquals("Terminator", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("WoodrowStudios", edges[i].getSource().getName());
	assertEquals("Terminator2", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("WoodrowStudios", edges[i].getSource().getName());
	assertEquals("Terminator3", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("WoodrowStudios", edges[i].getSource().getName());
	assertEquals("TrueLies", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Wubba", edges[i].getSource().getName());
	assertEquals("BlueSteel", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	i++;
	assertEquals("Wubba", edges[i].getSource().getName());
	assertEquals("GetShorty", edges[i].getDest().getName());
	assertEquals(2.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);



	Arrays.sort(edges = graph.getEdges(graph.getEdgeType("ActsWith")));
	assertEquals(4, edges.length);

	// Check all the EdgeTypes.
	for (final Edge e : edges)
	    assertEquals("ActsWith", e.getEdgeType().getName());

	i = 0;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("DannyDeVito", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("ArnoldSchwarzenegger", edges[i].getSource().getName());
	assertEquals("JamieLeeCurtis", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("DannyDeVito", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	i++;
	assertEquals("JamieLeeCurtis", edges[i].getSource().getName());
	assertEquals("ArnoldSchwarzenegger", edges[i].getDest().getName());
	assertEquals(1.0, edges[i].getWeight());

	// Make sure we checked all of them.
	assertEquals(edges.length-1, i);
    }

    public void testReadEdges1() throws Exception
    {
	try { // EdgeTypes with mismatched source/dest node types.
	    EdgeReaderRN.readEdges(new StringReader("%comment\n"), graph,
				   graph.getEdgeType("HasActor"),
				   graph.getEdgeType("ActsWith"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Invalid reversed EdgeType"));
	}

	try { // EdgeTypes with mismatched source/dest node types.
	    EdgeReaderRN.readEdges(new StringReader("%comment\n"), graph,
				   graph.getEdgeType("ActsIn"),
				   graph.getEdgeType("ActsWith"));
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Invalid reversed EdgeType"));
	}

	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }
    
    public void testReadEdges2() throws Exception
    {
	try { // Test Edge with unknown source Node.
	    EdgeReaderRN.readEdges(new StringReader("blah,blah,1\n"), graph,
				   graph.getEdgeType("ActsWith"), null);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find node1"));
	}

	try { // Test Edge with unknown destination Node.
	    EdgeReaderRN.readEdges(new StringReader("DannyDeVito,blah,1\n"), graph,
				   graph.getEdgeType("ActsWith"), null);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Couldn't find node2"));
	}

	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }
    
    public void testReadEdges3() throws Exception
    {
	try { // Test an extra field.
	    EdgeReaderRN.readEdges(new StringReader("blah,blah,1,0\n"), graph,
				   graph.getEdgeType("ActsWith"), null);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}

	try { // Test a missing field.
	    EdgeReaderRN.readEdges(new StringReader("blah,blah\n"), graph,
				   graph.getEdgeType("ActsWith"), null);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Didn't find a match"));
	}

	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }
    
    public void testReadEdges4() throws Exception
    {
	try { // Test a bad weight value.
	    EdgeReaderRN.readEdges(new StringReader("ArnoldSchwarzenegger,DannyDeVito,blah\n"), graph,
				   graph.getEdgeType("ActsWith"), null);
	    fail();
	} catch (RuntimeException success) {
	    assertTrue(success.getMessage().contains("Illegal weight"));
	}

	// Make sure no Edges sneaked in.
	assertEquals(0, graph.numEdges());
    }
    
    public static Test suite()
    {
        return new TestSuite(EdgeReaderRNTest.class);
    }
}
