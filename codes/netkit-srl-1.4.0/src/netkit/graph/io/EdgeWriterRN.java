/**
 * EdgeWriterRN.java
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

/** This class outputs Edge data in RN format.  The output format matches that read in by EdgeReaderRN.
 * @see EdgeReaderRN
 * @see SchemaReader
 * @see SchemaWriter
 * @see Graph
 * 
 * @author Kaveh R. Ghazi
 */
public final class EdgeWriterRN
{
  private static final String lineSeparator = System.getProperty("line.separator");

  /** Writes Edges to the supplied Writer.
   * @param edges an array of Edges to be output.  All of the
   * supplied Edges must share the same EdgeType reference.
   * @param writer a Writer object where the output will go.  The
   * Writer is flushed but not closed.
   * @throws RuntimeException if any constraints are violated.
   */
  public static void writeEdges(Edge[] edges, Writer writer)
  {
    final EdgeType et = edges[0].getEdgeType();
    try {
      for (final Edge edge : edges)
      {
        if (edge.getEdgeType() != et)
          throw new RuntimeException("EdgeType mismatch");

        writer.write(edge.getSource().getName());
        writer.write(',');
        writer.write(edge.getDest().getName());
        writer.write(',');
        writer.write(Double.toString(edge.getWeight()));
        writer.write(lineSeparator);
      }
      writer.flush();
    }
    catch(IOException ioe)
    {
      throw new RuntimeException("Error writing line?",ioe);
    }
  }
}
