/**
 * EdgeReaderGDA.java
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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import netkit.graph.*;
import netkit.util.NetKitEnv;

/** This class reads in Edge data in the GDA format.  This format
 * specifies that the input is a two column comma separated table.
 * The first line is the table header.  In subsequent lines, column 1
 * is the link or Edge identifier and column 2 is one of the (possibly
 * many) Nodes this Edge refers to.  GDA Edge indentifiers represent
 * collapsed meta-Edges where every Node within an indentifier group
 * is connected to every other Node within that group.  All Edges are
 * undirected.  Lines may contain comments which are lines where a '%'
 * or '#' appears as the first character.  Comments are terminated by
 * the end-of-line.
 * @see SchemaReader
 * @see Graph
 * @see Node
 * @see Edge
 * @see EdgeType
 * 
 * @author Kaveh R. Ghazi
 */

public final class EdgeReaderGDA
{
    private static final Logger log = NetKitEnv.getLogger(EdgeReaderGDA.class.getName());

    /** Reads Edges from the supplied Reader and creates the
     * corresponding Edges in the Graph; Edges are validated by the
     * supplied EdgeType which must have identical source and
     * destination Node types, and the Nodes these Edges refer to must
     * already exist in the Graph.
     * @param reader a Reader object containing Edge instances for the graph.
     * @param graph a Graph object into which Edges will be inserted.
     * @param et an EdgeType for Edges read from the Reader.
     * @throws RuntimeException if any of the format constraints of
     * the input are violated.
     */
    public static void readEdges(Reader reader, Graph graph, EdgeType et)
    {
	// GDA EdgeTypes must have the same type for source and dest.
	if (!et.getSourceType().equals(et.getDestType()))
	    throw new RuntimeException("GDA supports only one node type. Got <"
				       + et.getSourceType() + "><"
				       + et.getDestType() + ">");

	// This map holds the edges.  It puts the nodes each edge
	// connects to into an ArrayList keyed by the edge name from
	// the GDA input.
        final Map<String,ArrayList<Node>> edges = new HashMap<String,ArrayList<Node>>();
	final SplitParser parser = SplitParser.getParserCOMMA(2);
	final String nodeType = et.getSourceType();
	final LineNumberReader lr = new LineNumberReader(reader);
	
        log.info("EdgeReaderGDA parsing Edges");
        try
        {
            lr.readLine(); // skip first line.  Not needed by GDA
            for (String s = lr.readLine(); s != null; s = lr.readLine())
            {
		// Strip leading and trailing whitespace.
		s = s.trim();
		
		// Skip % or # comments or blank (whitespace-only) lines.
		if (s.length() == 0
		    || s.charAt(0) == '%' || s.charAt(0) == '#')
		    continue;
		
		final String[] tokens = parser.parseLine(s);
		if(tokens.length != 2)
		    throw new RuntimeException("Line:" + lr.getLineNumber()
					       + " - Invalid-lineformat ("
					       + tokens.length + " found) line: [" + s + "]");
		final String eName = tokens[0];
		final Node n = graph.getNode(tokens[1], nodeType);
		if (n == null)
		    throw new RuntimeException("Couldn't find node <"+tokens[1]+':'+nodeType
					       +"> line " + lr.getLineNumber());
		if (log.isLoggable(Level.FINE))
		    log.fine("Parsed Link["+eName+"] Node["+n.getName()+":"+n.getType()+"]");

		ArrayList<Node> l = edges.get(eName);
		if(l == null)
		    edges.put(eName,l=new ArrayList<Node>());
		l.add(n);
            }

	    // Close the input.
	    lr.close();
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Error reading line?",ioe);
        }

        log.info("EdgeReaderGDA creating Edges");
	for (final Iterator<Map.Entry<String,ArrayList<Node>>> it = edges.entrySet().iterator();
	     it.hasNext(); )
        {
	    final Map.Entry<String,ArrayList<Node>> me = it.next();
	    final ArrayList<Node> nodeList = me.getValue();
	    if (nodeList.size() == 1)
		throw new RuntimeException("Dangling link identifier <" + me.getKey() +">");
	    
	    for (int idx1=nodeList.size(); --idx1>=0; )
            {
		final Node n1 = nodeList.get(idx1);
		for (int idx2=idx1; --idx2>=0; )
		{
		    final Node n2 = nodeList.get(idx2);
		    graph.addEdge(et, n1, n2, 1.0);
		    graph.addEdge(et, n2, n1, 1.0);
		    if (log.isLoggable(Level.FINE))
		    {
			log.fine("Connected         [" + n1.getName() + "]["
				 + n2.getName() + "] weight="
				 + graph.getEdge(et.getName(),n1,n2).getWeight()
				 + " edgeType=" + et.getName());
                        log.fine("Connected REVERSED[" + n2.getName() + "]["
				 + n1.getName() + "] weight="
				 + graph.getEdge(et.getName(),n2,n1).getWeight()
				 + " edgeType=" + et.getName());
		    }
		}
            }
	    it.remove(); // Free up memory.
        }
    }
}
