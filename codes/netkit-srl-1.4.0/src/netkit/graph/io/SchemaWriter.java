/**
 * SchemaWriter.java
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

import netkit.graph.*;

/** 
 * This class outputs schema information for a Graph.  The file format
 * for the schema is an extended ARFF format.  The extended ARFF file
 * format is further described in the SchemaReader class.
 *  
 * @see SchemaReader
 * @see NodeWriter
 * @see EdgeWriterRN
 * @see Graph
 * 
 * @author Kaveh R. Ghazi
 */

public final class SchemaWriter
{
    private static final String lineSeparator
	= System.getProperty("line.separator");
    
    /** Writes the Graph information in an extended ARFF format.  Note
     * the supplied Maps of filenames are simply output in the
     * resulting schema.  It is the responsibility of the caller to
     * ensure these files are actually created with the appropriate
     * names and data via other the Writer classes.
     * @param graph a Graph to be written.
     * @param writer a Writer into which to output the schema.  The
     * Writer is flushed but not closed.
     * @param nodeTypeFiles a Map of NodeType names to output filenames.
     * @param edgeTypeFiles a Map of EdgeType names to output filenames.
     * @throws RuntimeException if an I/O error occurs.
     * @throws NullPointerException if nodeTypeFiles does not have an
     * entry for every node type (AKA Attributes object) in the
     * supplied Graph.  Likewise for edgeTypeFiles and EdgeTypes in
     * the Graph.
     */
    public static void writeSchema(Graph graph, Writer writer,
				   Map<String,String> nodeTypeFiles,
				   Map<String,String> edgeTypeFiles)
    {
	if (nodeTypeFiles.size() != graph.getAllAttributes().length)
	    throw new RuntimeException("nodeTypeFiles (" + nodeTypeFiles.size()
				       + ") != number of attributes ("
				       + graph.getAllAttributes().length +")");
	if (edgeTypeFiles.size() != graph.getEdgeTypeNames().length)
	    throw new RuntimeException("edgeTypeFiles (" + edgeTypeFiles.size()
				       + ") != number of edge types ("
				       + graph.getEdgeTypeNames().length +")");

	try {
	    writer.write("# Node type data" + lineSeparator);
	    for (final Attributes attributes : graph.getAllAttributes())
	    {
		writer.write("@NODETYPE " + attributes.getName()
			     + lineSeparator);
		for (final Attribute attr : attributes)
		{
		    writer.write("@ATTRIBUTE " + attr.getName() + ' ');
		    switch (attr.getType())
		    {
		    case CATEGORICAL:
			if (attr instanceof AttributeKey)
			    writer.write("KEY" + lineSeparator);
			else if (attr instanceof AttributeExpandableCategorical)
			    writer.write("CATEGORICAL" + lineSeparator);
			else if (attr instanceof AttributeFixedCategorical)
			{
			    final String tokens[] =
				((AttributeFixedCategorical)attr).getTokens();
			    writer.write("{" + tokens[0]);

			    for (int i=1; i<tokens.length; i++)
				writer.write(", " + tokens[i]);
			    writer.write('}' + lineSeparator);
			}
			else throw new RuntimeException("Unknown CATEGORICAL class <"+attr+">");
			break;
		    case CONTINUOUS:
		    case DISCRETE:
			writer.write(attr.getType().toString()
				     + lineSeparator);
			break;
		    default:
			throw new RuntimeException("Unknown TYPE <"+attr.getType()+">");
		    }
		}
		writer.write("@NODEDATA "
			     + nodeTypeFiles.get(attributes.getName()).toLowerCase()
			     + lineSeparator + lineSeparator);
	    }
	    writer.write("# Edge type data" + lineSeparator);
	    for (final String edgeTypeName : graph.getEdgeTypeNames())
	    {
		final EdgeType et = graph.getEdgeType(edgeTypeName);
		writer.write("@EDGETYPE " + edgeTypeName
			     + ' ' + et.getSourceType()
			     + ' ' + et.getDestType() + lineSeparator);
		writer.write("@EDGEDATA "
			     + edgeTypeFiles.get(edgeTypeName).toLowerCase()
			     + lineSeparator + lineSeparator);
	    }
	    writer.flush();
	} catch(IOException ioe) {
	    throw new RuntimeException(ioe.getMessage());
	}
    }

    
    /** A test driver for the class.
     * @param args An array of Strings as the arguments for the test
     * driver.  Extactly one argument must be supplied, it contains
     * the name of the schema file to use in the test which is passed
     * to SchemaReader.readSchema() and then written back to
     * System.out via this class.
     */
    public static final void main(String args[])
    {
	if (args.length == 1)
	{
	    final File input = new File(args[0]);
	    final Graph graph = SchemaReader.readSchema(input);

	    final Map<String,String> nodeTypeFiles = new HashMap<String,String>();
	    for (final String nodeTypeName : graph.getNodeTypes())
		nodeTypeFiles.put(nodeTypeName, nodeTypeName+".csv");
	    final Map<String,String> edgeTypeFiles = new HashMap<String,String>();
	    for (final String edgeTypeName : graph.getEdgeTypeNames())
		edgeTypeFiles.put(edgeTypeName, edgeTypeName+".rn");

	    System.out.println("Writing SCHEMA: --------------");
	    SchemaWriter.writeSchema(graph, new PrintWriter(System.out), nodeTypeFiles, edgeTypeFiles);
	    for (final String attributes : graph.getNodeTypes())
	    {
		System.out.println("Writing NodeType <" + attributes + "> -------------");
		NodeWriter.writeNodes(graph.getNodes(attributes),
				      new PrintWriter(System.out));
		System.out.println();
	    }

	    for (final String edgeTypeName : graph.getEdgeTypeNames())
	    {
		System.out.println("Writing EdgeType <" + edgeTypeName + "> -------------");
		final EdgeType et = graph.getEdgeType(edgeTypeName);
		EdgeWriterRN.writeEdges(graph.getEdges(et),
					new PrintWriter(System.out));
		System.out.println();
	    }
	}
	else
	    throw new RuntimeException("Got "+args.length+" args");
    }
}
