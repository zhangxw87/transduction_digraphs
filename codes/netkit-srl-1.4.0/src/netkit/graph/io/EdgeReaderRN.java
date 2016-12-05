/**
 * EdgeReaderRN.java
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
import java.util.logging.*;

import netkit.util.NetKitEnv;
import netkit.graph.*;

/** This class reads in Edge data in RN format.  The input format is
 * line oriented and organized into columns.  There are three comma
 * separated column elements per line.  The first two elements are
 * names of Nodes in the Graph object which are to be connected via an
 * Edge.  The third element is a weight for the connecting Edge.  In
 * directed Graphs, all Edges in the file use the same single
 * EdgeType.  However undirected Graphs have two EdgeTypes, one for
 * each Edge direction.  Lines may contain comments which are lines
 * where a '%' or '#' is the first character.  Comments are terminated
 * by the end-of-line.
 * @see SchemaReader
 * @see EdgeWriterRN
 * @see Graph
 * @see Node
 * @see Edge
 * @see EdgeType
 * 
 * @author Kaveh R. Ghazi
 */
public final class EdgeReaderRN
{
    private static final Logger log = NetKitEnv.getLogger(EdgeReaderRN.class.getName());

    /** Reads Edges from the supplied Reader and creates the
     * corresponding Edges in the Graph; Edges are validated by the
     * the supplied EdgeTypes and the Nodes these Edges refer to must
     * already exist in the Graph.
     * @param reader a Reader object containing Edge instances for the graph.
     * @param graph a Graph object into which Edges will be inserted.
     * @param et1 an EdgeType for Edges read from the Reader.
     * @param et2 if not null, an EdgeType for reversed Edges read
     * from Reader; used in undirected graphs.
     * @throws RuntimeException if any of the format constraints of
     * the input are violated.
     */
    public static void readEdges(Reader reader, Graph graph, EdgeType et1, EdgeType et2)
    {
	final SplitParser parser = SplitParser.getParserCOMMA(3);
	final String nodeType1 = et1.getSourceType();
	final String nodeType2 = et1.getDestType();
	if (et2 != null
	    && (!nodeType1.equals(et2.getDestType())
		|| !nodeType2.equals(et2.getSourceType())))
	    throw new RuntimeException ("Invalid reversed EdgeType <"+et1+"><"+et2+">");

	log.info("EdgeReaderRN parsing and creating edges");
	final LineNumberReader lnr = new LineNumberReader(reader);
        try
        {
	    String s="";
	    try
	    {
		for (s = lnr.readLine(); s != null; s = lnr.readLine())
		{
		    // Strip leading and trailing whitespace.
		    s = s.trim();
		
		    // Skip % or # comments or blank (whitespace-only) lines.
		    if (s.length() == 0
			|| s.charAt(0) == '%' || s.charAt(0) == '#')
			continue;

		    final String[] tokens = parser.parseLine(s);
		    if (tokens.length != 3)
			throw new RuntimeException("Invalid number of fields, expected 3, got "
						   + tokens.length + " at line " + lnr.getLineNumber());
		    final Node n1 = graph.getNode(tokens[0], nodeType1);
		    if (n1 == null)
			throw new RuntimeException("Couldn't find node1 <"+tokens[0]+':'
						   +nodeType1+"> at line: " + lnr.getLineNumber());
		    final Node n2 = graph.getNode(tokens[1], nodeType2);
		    if (n2 == null)
			throw new RuntimeException("Couldn't find node2 <"+tokens[1]+':'
						   +nodeType2+"> at line: " + lnr.getLineNumber());
		    final double weight = Double.parseDouble(tokens[2]);
		    graph.addEdge(et1,n1,n2,weight);
		    if (log.isLoggable(Level.FINE))
			log.fine("Connected         [" + n1.getName() + "]["
				 + n2.getName() + "] weight=" + weight
				 + " edgeType=" + et1.getName());
		    if (et2 != null)
		    {
			graph.addEdge(et2,n2,n1,weight);
			if (log.isLoggable(Level.FINE))
			    log.fine("Connected REVERSED[" + n2.getName() + "]["
				     + n1.getName() + "] weight=" + weight
				     + " edgeType=" + et2.getName());
		    }
		}
	    }
	    catch(IllegalArgumentException iae)
	    {
		throw new RuntimeException("Line:"+lnr.getLineNumber()+" - Illegal weight [line: '"+s+"']",iae);
	    }
	    finally
	    {
		// Close the input.
		lnr.close();
	    }
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Error reading line?",ioe);
        }
    }
}
