/**
 * NodeWriter.java
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

import netkit.graph.*;

/** This class writes Node instance data to a Writer object.  The
 * output format corresponds to that read by the NodeReader class.
 * @see SchemaReader
 * @see NodeReader
 * @see SchemaWriter
 * @see EdgeWriterRN
 * @see Graph
 * 
 * @author Kaveh R. Ghazi
 */
public final class NodeWriter
{
    private static final String lineSeparator
	= System.getProperty("line.separator");

    /** This static method does the work of writing output data for the
     * class.
     * @param nodes an array of Node objects.  All of the supplied
     * Nodes must share the same Attributes object reference (AKA
     * their NodeType).
     * @param writer a Writer object where the Node output will go.
     * The Writer is flushed but not closed.
     * @throws RuntimeException if any constraints are violated.
     */
    public static void writeNodes(Node[] nodes, Writer writer)
    {
	final Attributes attributes = nodes[0].getAttributes();
	try {
	    for (final Node node : nodes)
	    {
		// Ensure all nodes share the same Attributes object.
		if (node.getAttributes() != attributes)
		    throw new RuntimeException("attributes mismatch");

		boolean first = true;
		for (final Attribute attr : attributes)
		{
		    if (!first)
			writer.write(',');
		    else
			first = false;
		    writer.write(attr.formatForOutput(node.getValue(attr.getName())));
		}
		writer.write(lineSeparator);
	    }
	    writer.flush();
	} catch(IOException ioe) {
            throw new RuntimeException("Error reading line?",ioe);
        }
    }
}
