/**
 * NodeReader.java
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

/** This class reads Node instance data from a Reader object.  The
 * input format is line oriented and organized into columns
 * corresponding to Node instance data fields.  Column delimiters are
 * commas, and no decorative whitespace is allowed in between values
 * and the delimiters.  Each line represents the instance data for one
 * Node.  The order of the values must match the order of the fields
 * in the Attributes container of the node type.  Data is always
 * converted into java double types.  The conversion for CONTINUOUS
 * and DISCRETE values is straightforward, except that DISCRETE values
 * must be a valid integer type before conversion to double.
 * CATEGORICAL types are first converted into int values coresponding
 * to the order they appear defined in the TokenSet of the
 * AttributeCategorical they are from.  The KEY field additionally
 * gets inserted into the name field for each Node and must be unique
 * across each node type with a single Graph.  Lines may contain
 * comments, which are lines where a '%' or '#' is the first
 * character.  Comments are terminated by the end-of-line.
 * 
 * @see SchemaReader
 * @see SchemaWriter
 * @see NodeWriter
 * @see Attributes
 * @see Node
 * @see Graph
 * @see TokenSet
 * @see AttributeCategorical
 * 
 * @author Kaveh R. Ghazi
 */
public final class NodeReader
{
    // The Logger for this class.
    private static final Logger log = NetKitEnv.getLogger(NodeReader.class.getName());

    // Helper method for readNodes().  This method iterates over the
    // Attribute classes in the Attributes container.  For each
    // Attribute, it parses a value from the values parameter and puts
    // its parsed double value into the results parameter.  The number
    // of Attribute classes must equal the length of the values and
    // results arrays.  Finally a Node object is created with the
    // "resulting" results array and inserted into the graph parameter
    // object.
    private static Node getAllValues(String values[], double[] results, Attributes attrs, Graph graph)
    {
	if (results.length != attrs.attributeCount())
	    throw new RuntimeException("Incorrect size for results, expected "
				       + attrs.attributeCount() + ", got "
				       + results.length);
	if (values.length != results.length)
	    throw new RuntimeException("Incorrect number of values, expected "
				       + results.length + ", got "
				       + values.length);

	String nodeName = null;
	int i = 0;

	for (final Attribute a : attrs)
	{
	    if (a instanceof AttributeKey)
		nodeName = values[i];
	    results[i] = a.parseAndInsert(values[i]);
	    i++;
	}
	
	// We only insert the Node if we were able to successfully
	// parse all the values above.
	final Node n = graph.addNode(nodeName, attrs);
	n.setValues(results);
	if (log.isLoggable(Level.FINE))
	    log.fine("Created node <"+n+">");
	return n;
    }
    
    /** This static method does the work of reading input data for the
     * class.  Given a Graph object, a node type name and an input
     * Reader object, it will read in the Node data and instantiate
     * Nodes as part of the supplied Graph with the appropriate node
     * type Attributes container.  This assumes that the graph object
     * contains an appropraite Attributes object with a corresponding
     * nodeType.  The Attributes object must contain a key Attribute.
     * @param graph the Graph object to insert Nodes into.
     * @param nodeType the node type name for these Nodes.
     * @param reader the Reader object to read instance data from; the
     * Reader will be closed when this method completes.
     * @param skipFirstLine true if the reader should skip the first
     * line of input; appropriate if the first line contains column
     * headers as in GDA files.
     * @throws RuntimeException if any of the input format constraints
     * are violated or the input cannot be read.
     */
    public static void readNodes(Graph graph, String nodeType, Reader reader, boolean skipFirstLine)
    {
	final Attributes attrs = graph.getAttributes(nodeType);
	if (attrs == null)
	    throw new RuntimeException("No Attributes matching node type <"+nodeType+">");
	if (attrs.getKey() == null)
	    throw new RuntimeException("No key Attribute in node type <"+nodeType+">");
	final double[] results = new double[attrs.attributeCount()];
//	final SplitParser parser = SplitParser.getParserCOMMA(attrs.attributeCount());
    final SplitParser parser = SplitParser.getParserCSV(attrs.attributeCount());
	final LineNumberReader lr = new LineNumberReader(reader);

	log.config("Parsing Node instance data using regex: " + parser.getRegex());
	
        try
        {
	    // For GDA format skip the first line, it's a column header.
	    if (skipFirstLine)
		lr.readLine();
            for (String s = lr.readLine(); s != null; s = lr.readLine())
            {
		// Strip leading and trailing whitespace.
		s = s.trim();
		
		// Skip % or # comments or blank (whitespace-only) lines.
		if (s.length() == 0
		    || s.charAt(0) == '%' || s.charAt(0) == '#')
		    continue;
		
		final String[] data = parser.parseLine(s);
		if (log.isLoggable(Level.FINE))
		{
		    final StringBuilder logString
			= new StringBuilder("Parsed instance data:");
		    for (final String d : data)
			logString.append('<' + d + '>');
		    log.fine(logString.toString());
		}
		// Process one line of instance data.
		getAllValues(data, results, attrs, graph);
	    }

	    // Close the input.
	    lr.close();
	}
        catch(IOException ioe)
        {
            throw new RuntimeException("Error reading line?",ioe);
        }
    }
}
