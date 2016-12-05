  /**
   * SchemaReader.java
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
import java.util.regex.Pattern;
import java.lang.management.*;
import java.util.logging.*;

import netkit.graph.*;
import netkit.util.NetKitEnv;
  
  /** 
   * This class reads schema type information for a Graph and builds the
   * data structures to describe completely a network of Nodes and
   * Edges, all of their type information and the instance data.  The
   * file format for the schema is an extended ARFF format.  The
   * extended ARFF file format is line oriented.  Valid lines may
   * contain comments, whitespace or tags.  Comments are lines where a
   * '%' or '#' appears as the first non-whitespace character and they
   * are terminated by the end-of-line.<P>
   * 
   * Tags are directives beginning with an '@' character in column one.
   * There are six primary tags and they are case-insensitive:<P>
   * 
   * {@literal @NODETYPE}<BR>
   * {@literal @ATTRIBUTE}<BR>
   * {@literal @NODEDATA}<BR>
   * {@literal @EDGETYPE}<BR>
   * {@literal @REVERSIBLE}<BR>
   * {@literal @EDGEDATA}<P>
   * 
   * The first three tags are used for describing Nodes and the latter
   * three are for Edges.  Because Edges state and enforce the type of
   * Node they may connect to, the underlying Nodes for each EdgeType
   * must be declared first. <P>
   * 
   * A Node type is defined by the following sequence, exactly one line
   * with a {@literal @NODETYPE} tag, followed by any number of lines
   * with {@literal @ATTRIBUTE} tags, followed by exactly one line with
   * a {@literal @NODEDATA } tag. <P>
  
   * The {@literal @NODETYPE} tag names the node's type or attributes
   * container.  It is used to uniquely identify a Node type and
   * distinguish it from other types.  The line declaring it appears
   * like this: <P>
   *
   * {@literal @NODETYPE} <I>name</I><P>
   * 
   * There can be any number of {@literal @ATTRIBUTE} tags per Node
   * type.  These lines begin with {@literal @ATTRIBUTE} followed by the
   * name of the attribute field and the third token on that line is the
   * type of that attribute.  The name of each attribute identifies that
   * field among the attributes in one Node and must be unique in that
   * context. <P>
   * 
   * There are four valid attribute types.  They can be KEY, CONTINUOUS,
   * DISCRETE or CATEGORICAL.  These field types are case insensitive.
   * REAL is a synonym for CONTINUOUS and INT is a synonym for DISCRETE.
   * If any of the first three types appear in the third field on an
   * attribute line, that describes the type of that field.  For
   * example:<P>
   * 
   * {@literal @ATTRIBUTE} <I>field1</I> KEY<BR>
   * {@literal @ATTRIBUTE} <I>field2</I> CONTINUOUS<BR>
   * {@literal @ATTRIBUTE} <I>field3</I> REAL<BR>
   * {@literal @ATTRIBUTE} <I>field4</I> DISCRETE<BR>
   * {@literal @ATTRIBUTE} <I>field5</I> INT<P>
   * 
   * The KEY type can appear only once per Node type and is the unique
   * identifier for a particular Node instance.  It corresponds in value
   * to the java String type and each KEY value may appear exactly once.
   * The CONTINUOUS type corresponds to the java double type.  The
   * DISCRETE type corresponds to the java double type, but takes on
   * whole number (int) values.<P>
   * 
   * The last type, CATEGORICAL, can take on a fixed set of token
   * values.  To declare a CATEGORICAL type the line looks like this:<P>
   * {@literal @ATTRIBUTE} <I>field6</I>
   * {token1,token2,token3,etc...}<P>
   * 
   * The curly brace enclosed list of tokens represents the set of
   * values that this type of field can take on.  These values are
   * converted to doubles internally.  They are assigned whole number
   * values starting at zero and incrementing by one for each extra
   * token.<P>
   * 
   * The final tag for node types is the {@literal @NODEDATA} tag.  This
   * tag declares that the node type is finalized and no more attribute
   * fields will be added.  It also specifies the filename from which
   * the instance data for Nodes of this type will be read.  The
   * filename supplied may be a hard or relative path.  If a relative
   * path is supplied, it is relative to the location of the schema file
   * being read.  The {@literal @NODEDATA} line looks like this: <P>
   * 
   * {@literal @NODEDATA} <I>filename</I> <P>
   * 
   * Note {@literal @RELATION} is an alias for {@literal @NODETYPE} and
   * {@literal @DATA} is an alias for {@literal @NODEDATA}. <P>
   * 
   * The latter three tags describe an EdgeType and Edge connections.
   * Lines with the {@literal @EDGETYPE} tag supply the EdgeType name in
   * the second token.  The third token is the Edge's source Node type
   * and the fourth token is the Edge's destination Node type.  Node
   * types correspond to the {@literal @NODETYPE} tag defined above.
   * These lines appear like so: <P>
   * 
   * {@literal @EDGETYPE} <I>typeName</I> <I>sourceNodeType</I> <I>destinationNodeType</I><P>
   * 
   * The {@literal @REVERSIBLE} tag by it's absence indicates that the
   * Graph is directed.  If this line appears, then each added Edge also
   * implies another Edge in the opposite direction must be added.  If
   * the {@literal @REVERSIBLE} tag appears by itself, then the same
   * EdgeType is used for the reversed connection.  This implies that
   * the Nodes have the same type.  If the {@literal @REVERSIBLE}
   * includes an optional name in the second token, then this name is
   * used for the reversed EdgeType and is added to the list of
   * EdgeType's in the Graph.  In this case, the Nodes do not need to
   * have the same type since they get their own connection type.  These
   * lines look like this:<P>
   * 
   * {@literal @REVERSIBLE}<BR>
   * or<BR>
   * {@literal @REVERSIBLE} <I>name</I><P>
   * 
   * Finally, the {@literal @EDGEDATA} tag lists the file from which to
   * read Edge data.  Like the {@literal @NODEDATA} tag, this tag
   * accepts one extra filename token which specifies where to get the
   * Edges for this EdgeType.  And similarly, the filename may be a hard
   * or relative path.  The {@literal @EDGEDATA} line looks like
   * this:<P>
   * 
   * {@literal @EDGEDATA} <I>filename</I> <P>
   *  
   * @see Attribute
   * @see Attributes
   * @see TokenSet
   * @see ExpandableTokenSet
   * @see FixedTokenSet
   * @see EdgeType
   * @see Edge
   * @see Node
   * @see NodeReader
   * @see EdgeReaderRN
   * @see EdgeReaderGDA
   * @see SchemaWriter
   * @see NodeWriter
   * @see EdgeWriterRN
   * 
   * @author Kaveh R. Ghazi
   * @author Sofus A. Macskassy
   */
  
  public final class SchemaReader
  {
    private static final Logger log = NetKitEnv.getLogger("netkit.graph.io.SchemaReader");
    private static final String lineSeparator = System.getProperty("line.separator");
  
    private static final boolean STRESS_DEBUG = false;
  
    /** Overloaded entry point for {@link #readGDASchema(Reader,Reader)}
     * @param nodeFile a File containing the GDA formatted Node
     * instance data.
     * @param edgeFile a File containing the GDA formatted Edge
     * instance data.
     * @return the constructed Graph object.
     * @throws RuntimeException if any of the file format constraints
     * are violated or the files cannot be read.
     */
    public static Graph readGDASchema(File nodeFile, File edgeFile)
    {
      try {
        return readGDASchema(new FileReader(nodeFile),
            new FileReader(edgeFile));
      }
      catch(FileNotFoundException fnfe)
      {
        throw new RuntimeException(fnfe.getMessage());
      }
    }
  
    /** Reads the Node and Edge information from GDA formatted input,
     * constructs the data structures and instantiates all of the
     * instance data.  This method is provided for backwards
     * compatability with GDA file format.
     * @param nodeReader a Reader object containing the GDA formatted
     * Node instance data.
     * @param edgeReader a Reader object containing the GDA formatted
     * Edge instance data.
     * @return the constructed Graph object.
     * @throws RuntimeException if any of the file format constraints
     * are violated or the files cannot be read.
     */
    public static Graph readGDASchema(Reader nodeReader, Reader edgeReader)
    {
      final Graph graph = new Graph();
      final Attributes attr = new Attributes("GDA");
  
      try 
      {
        final BufferedReader br = new BufferedReader(nodeReader);
        final String[] tokens = br.readLine().trim().split(",");
  
        attr.add(new AttributeKey(tokens[0]));
        for (int i=1; i<tokens.length; i++)
          attr.add(new AttributeExpandableCategorical(tokens[i]));
        // We're done creating the field container
        log.info("Constructed GDA Attributes container"
            + lineSeparator + "Got: " + attr);
  
        // We're done creating the field container, add it to the
        // graph here after it's fully constructed.
        graph.addAttributes(attr);
  
        log.info("Reading Node instance data");
  
        // We already read the first line, don't skip it.
        NodeReader.readNodes(graph, attr.getName(), br, false);
        // The readNodes() method closed the Reader.
      }
      catch(IOException ioe)
      {
        throw new RuntimeException(ioe.getMessage());
      }
      if (log.isLoggable(Level.CONFIG))
      {
        final StringBuilder logString
        = new StringBuilder("Expandable CATEGORICAL fields:");
        logString.append(lineSeparator);
        for (final Attribute a : attr)
          if (a instanceof AttributeExpandableCategorical)
          {
            final String[] toks = ((AttributeCategorical)a).getTokens();
            logString.append(a.getClass().getSimpleName() + " field <"
                + a.getName() + "> saw " + toks.length
                + " tokens: ");
            for (final String tok : toks)
              logString.append('<' + tok + '>');
            logString.append(lineSeparator);
          }
        log.config(logString.toString());
      }
  
      final EdgeType edgeType = new EdgeType("ConnectsTo", "GDA", "GDA");
      graph.addEdgeType(edgeType);
      log.info("Reading Edge instance data");
      EdgeReaderGDA.readEdges(edgeReader, graph, edgeType);
      log.info("SUCCESS: Graph got " + graph.numNodes()
          + " Nodes and " + graph.numEdges() + " (actual) Edges");
      return graph;
    }
  
    /** Overloaded entry point for {@link #readSchema(Reader,String)}
     * @param file a File from which the schema extended ARFF
     * description is read; the parent directory of this File is where
     * instance data files are searched for if relative paths are
     * supplied in the schema.
     * @return the constructed Graph object.
     * @throws RuntimeException if any of the file format constraints
     * are violated or the file cannot be read.
     */
    public static Graph readSchema(File file)
    {
      try {
        return readSchema(new FileReader(file),
            file.getParent());
      }
      catch(FileNotFoundException fnfe)
      {
        throw new RuntimeException(fnfe.getMessage());
      }
    }
  
    /** Reads the Graph information from a schema file, constructs the
     * data structures and instantiates all of the instance data.
     * @param reader a Reader object from which the schema extended
     * ARFF description is read.
     * @param parentDirectory a String representing the directory in
     * which to search for instance data files if the filenames
     * supplied in the schema are relative paths.
     * @return the constructed Graph object.
     * @throws RuntimeException if any of the file format constraints
     * are violated or the file cannot be read.
     */
    public static Graph readSchema(Reader reader, String parentDirectory)
    {
      final Graph graph = new Graph();
      Attributes attr = null;
      EdgeType edge1=null, edge2=null;
      boolean gotNodeType=false;
      // This pattern is a comma surrounded by any amount of whitespace.
      final Pattern commaPattern = Pattern.compile("\\s*,\\s*");
      // This pattern is one or more whitespace characters.
      final Pattern spacePattern = Pattern.compile("\\s+");
  
  
  
      log.info("SchemaReader reading schema");
      final StringBuilder logString = new StringBuilder(lineSeparator);
      try
      {
        final LineNumberReader lr = new LineNumberReader(reader);
        for (String s = lr.readLine(); s != null; s = lr.readLine())
        {
          // Strip leading and trailing whitespace.
          s = s.trim();
  
          // Skip % or # comments or blank (whitespace-only) lines.
          if (s.length() == 0
              || s.charAt(0) == '%' || s.charAt(0) == '#')
            continue;
  
          // Check for an @ tag.
          if (s.startsWith("@"))
          {
            /*
  		      This line should be "TAG NAME TYPE"
  		      TAG -  is case-insensitive and is one of
  		      @NODETYPE, @ATTRIBUTE or @NODEDATA.
  		      NAME - is any String.
  		      TYPE - is case-insensitive and is one of "DISCRETE",
  		      "CONTINUOUS" or a curly bracket-enclosed comma-separated
  		      list of possible String values.
             */
            final String[] tokens = spacePattern.split(s,3);
            final String tag = tokens[0];
  
            if (tag.equalsIgnoreCase("@NODETYPE")
                || tag.equalsIgnoreCase("@RELATION"))
            {
              // Parse @NODETYPE tag.
              if (tokens.length != 2)
                throw new RuntimeException("Got " + tokens.length
                    + " tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              if (gotNodeType)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag out of order on line "
                    + lr.getLineNumber());
              // Process @NODETYPE tag.
              gotNodeType = true;
              final String nodeType = tokens[1].intern();
              attr = new Attributes(nodeType);
              logString.append("Parsed @NODETYPE <" + nodeType + '>'
                  + lineSeparator);
            }
            else if (tag.equalsIgnoreCase("@ATTRIBUTE"))
            {
              // Parse @ATTRIBUTE tag.
              if (tokens.length != 3)
                throw new RuntimeException("Got " + tokens.length
                    + " tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              if (!gotNodeType)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag out of order on line "
                    + lr.getLineNumber());
              final String attributeName = tokens[1];
              logString.append("Parsed " + tag.toUpperCase()
                  + " <" + attributeName + '>');
  
              final String attributeType = tokens[2];
              if (attributeType.equalsIgnoreCase("continuous")
                  || attributeType.equalsIgnoreCase("real"))
              {
                logString.append(" with type <" + attributeType
                    + '>' + lineSeparator);
                // Process @ATTRIBUTE tag.
                attr.add(new AttributeContinuous(attributeName));
              }
              else if (attributeType.equalsIgnoreCase("discrete")
                  || attributeType.equalsIgnoreCase("int"))
              {
                logString.append(" with type <" + attributeType
                    + '>' + lineSeparator);
                // Process @ATTRIBUTE tag.
                attr.add(new AttributeDiscrete(attributeName));
              }
              else if (attributeType.equalsIgnoreCase("key"))
              {
                logString.append(" with type <" + attributeType
                    + '>' + lineSeparator);
                // Process @ATTRIBUTE tag.
                attr.add(new AttributeKey(attributeName));
              }
              else if (attributeType.equalsIgnoreCase("ignore"))
              {
                logString.append(" with type <" + attributeType
                    + ">, skipping..."
                    + lineSeparator);
                // Process @ATTRIBUTE tag.
                attr.add(new AttributeIgnore(attributeName));
                continue;
              }
              else if (attributeType.equalsIgnoreCase("categorical"))
              {
                logString.append(" with type <" + attributeType
                    + '>' + lineSeparator);
                // Process @ATTRIBUTE tag.
                attr.add(new AttributeExpandableCategorical(attributeName));
              }
              else if (attributeType.charAt(0) == '{'
                && attributeType.charAt(attributeType.length()-1) == '}')
              {
                final String[] validValues
                = commaPattern.split(tokens[2].substring(1,attributeType.length()-1).trim());
                logString.append(" with " + validValues.length
                    + " valid tokens:");
                for (final String v : validValues)
                  logString.append('<'+v+'>');
                logString.append(lineSeparator);
                // Process @ATTRIBUTE tag.
                final FixedTokenSet idSet
                = new FixedTokenSet(validValues);
                attr.add(new AttributeFixedCategorical(attributeName,
                    idSet));
              }
              else
                throw new RuntimeException("Got invalid attribute type <"
                    + attributeType
                    + "> on line "
                    + lr.getLineNumber());
            }
            else if (tag.equalsIgnoreCase("@NODEDATA")
                || tag.equalsIgnoreCase("@DATA"))
            {
              if (tokens.length != 2)
                throw new RuntimeException("Got " + tokens.length
                    + " tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              if (!gotNodeType)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag too early on line "
                    + lr.getLineNumber());
              // Process @NODEDATA tag.
              final String dataFileName = tokens[1];
              // We're done creating the field container,
              // add it to the graph here after it's fully
              // constructed so we know which field is last
              // for class index purposes.
              graph.addAttributes(attr);
              logString.append("Constructed Attributes container: "
                  + attr + lineSeparator);
              gotNodeType = false; // Prepare to start another...
  
              // Read nodes!!!
              File nodeInput = new File(dataFileName);
              if (!nodeInput.isAbsolute())
                nodeInput = new File(parentDirectory,dataFileName);
              log.info("Reading Node instance data from: "
                  + nodeInput.getAbsolutePath());
              try
              {
                NodeReader.readNodes(graph, attr.getName(),
                    new FileReader(nodeInput),
                    dataFileName.endsWith(".gda"));
              }
              catch(FileNotFoundException fnfe)
              {
                throw new RuntimeException(fnfe.getMessage());
              }
              for (final Attribute a : attr)
                if (a instanceof AttributeExpandableCategorical)
                {
                  final String[] toks
                  = ((AttributeCategorical)a).getTokens();
                  logString.append(a.getClass().getSimpleName()
                      +" field <"+a.getName()
                      +"> saw "+toks.length
                      +" tokens: ");
                  for (final String tok : toks)
                    logString.append('<'+tok+'>');
                  logString.append(lineSeparator);
                }
            }
            else if (tag.equalsIgnoreCase("@EDGETYPE"))
            {
              // Re-split the line with unlimited tokens.
              final String[] toks = spacePattern.split(s);
              if (toks.length != 4)
                throw new RuntimeException("Got " + toks.length
                    + " tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              if (edge1 != null || edge2 != null)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag out of order on line "
                    + lr.getLineNumber());
              edge1 = new EdgeType(toks[1], toks[2], toks[3]);
              graph.addEdgeType(edge1);
            }
            else if (tag.equalsIgnoreCase("@REVERSIBLE"))
            {
              if (edge1 == null || edge2 != null)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag out of order on line "
                    + lr.getLineNumber());
              switch (tokens.length)
              {
              case 1:
                if (!edge1.getSourceType().equals(edge1.getDestType()))
                  throw new RuntimeException("Got unnamed "
                      + tag.toUpperCase()
                      + " tag with different types on line "
                      + lr.getLineNumber());
                edge2 = edge1;
                break;
              case 2:
                edge2 = new EdgeType(tokens[1],
                    edge1.getDestType(),
                    edge1.getSourceType());
                graph.addEdgeType(edge2);
                break;
              default:
                throw new RuntimeException("Got " + tokens.length
                    + "tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              }
            }
            else if (tag.equalsIgnoreCase("@EDGEDATA"))
            {
              if (tokens.length != 2)
                throw new RuntimeException("Got " + tokens.length
                    + " tokens for "
                    + tag.toUpperCase()
                    + " tag on line "
                    + lr.getLineNumber());
              if (edge1 == null)
                throw new RuntimeException("Got "+tag.toUpperCase()
                    + " tag out of order on line "
                    + lr.getLineNumber());
  
              final String edgeFilename = tokens[1];
              File edgeFile = new File(edgeFilename);
              if (!edgeFile.isAbsolute())
                edgeFile = new File(parentDirectory, edgeFilename);
              log.info("Reading Edge instance data from <"
                  + edgeFile.getAbsolutePath()+'>');
              try
              {
                final FileReader edgeReader
                = new FileReader(edgeFile);
                // Auto-detect file format based on
                // filename extension.
                if (edgeFilename.endsWith(".rn")
                    || edgeFilename.equals("/dev/null")) // Hack for testsuite
                  EdgeReaderRN.readEdges(edgeReader, graph,
                      edge1, edge2);
                else if (edgeFilename.endsWith(".gda"))
                  EdgeReaderGDA.readEdges(edgeReader, graph,
                      edge1);
                else
                  throw new RuntimeException("Cannot determine Edge reader for file <"
                      + edgeFile.getAbsolutePath() + '>');
                edgeReader.close();
              }
              catch (FileNotFoundException fnfe)
              {
                throw new RuntimeException(fnfe.getMessage());
              }
  
              // Reset edge type info.
              edge1 = null;
              edge2 = null;
            }
            else
              throw new RuntimeException("Got unknown "
                  + tag.toUpperCase()
                  + " tag on line "
                  + lr.getLineNumber());
          }
          else
          {
            throw new RuntimeException("Got unrecognized input <" + s
                + "> on line "
                + lr.getLineNumber());
          }
        }
  
        // Close the file.
        lr.close();
      }
      catch(IOException ioe)
      {
        throw new RuntimeException("Error reading file?", ioe);
      }
  
      logString.append("Complete set of Attributes:" + lineSeparator);
      for (final Attributes a : graph.getAllAttributes())
        logString.append(a+lineSeparator);
      log.config(logString.toString());
      log.info("SUCCESS: Graph got " + graph.numNodes() + " Nodes and "
          + graph.numEdges() + " (actual) Edges");
  
      return graph;
    }
  
    private static long[] getMemory(MemoryMXBean mbean)
    {
      return new long[] { mbean.getHeapMemoryUsage().getUsed(),
          mbean.getNonHeapMemoryUsage().getUsed() };
    }
  
    private static void cleanMem(MemoryMXBean mbean)
    {
      for (int i=0; i<0; i++) // Has no effect regardless of loop max...
        mbean.gc();
    }
  
    private static String printableBytes(long bytes)
    {
      final long kb = 1024;
      final long mb = kb * kb;
      final long gb = mb * kb;
      if (bytes >= gb)
        return String.format("%.2fG", (double)bytes/gb);
      else if (bytes >= mb)
        return String.format("%.2fM", (double)bytes/mb);
      else if (bytes >= kb)
        return String.format("%.2fK", (double)bytes/kb);
      else
        return Long.toString(bytes);
    }
  
  
    /** This method conducts a stress test by creating a set of random
     * nodes and edges and performs busy-work accessing the node and
     * edge information.
     * @param numFields determines how many fields are created in each Node.
     * @param numNodes determines how many Nodes to create in the Graph.
     * @param numEdges determines how many Edges to create in the Graph.
     * @return the constructed Graph object.
     */
    @SuppressWarnings("unused")
	public static Graph stressTest(int numFields, int numNodes, int numEdges)
    {
      final MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
      final Random random = new Random(System.currentTimeMillis());
      final Graph graph = new Graph();
      final Attributes attr = new Attributes("TestNodeType");
      final EdgeType et = new EdgeType("TestEdgeType", "TestNodeType",
      "TestNodeType");
      long timer;
      long[] mem, mem2;
  
      // Create the Attributes container.
      cleanMem(mbean);
      mem=getMemory(mbean);
      timer = System.currentTimeMillis();
      attr.add(new AttributeKey("Name"));
      for (int f=0; f<numFields; f++)
        attr.add(new AttributeContinuous("Field"+f));
      // We're done creating the field container, add it to the
      // graph here after it's fully constructed so we know which
      // field is last for class index purposes.
      graph.addAttributes(attr);
      graph.addEdgeType(et);
      mem2=getMemory(mbean);
      log.info("Constructed Attributes container: " + attr
          + lineSeparator + "Attribute setup took: "
          + (System.currentTimeMillis()-timer)/1000.0 + " secs"
          + lineSeparator
          + "Memory change heap: " + printableBytes(mem2[0]-mem[0])
          + " nonheap: " + printableBytes(mem2[1]-mem[1]));
  
      // Create all of the Nodes.
      cleanMem(mbean);
      mem=getMemory(mbean);
      timer = System.currentTimeMillis();
      final double[] results = new double[attr.attributeCount()];
      for (int n=0; n<numNodes; n++)
      {
        final Node node = graph.addNode("Node"+n, attr);
        for (int r=0; r<results.length; r++)
          results[r] = random.nextDouble();
        node.setValues(results);
        if (STRESS_DEBUG) System.err.println ("Created node <"+node+'>');
        if (n%200000==0 && n>0)
          log.config("Created " + n + " Nodes in "
              + (System.currentTimeMillis()-timer)/1000.0
              + " secs");
      }
      mem2=getMemory(mbean);
      log.info("Created "+numNodes+" Nodes in "
          + (System.currentTimeMillis()-timer)/1000.0
          + " secs" + lineSeparator
          + "Memory change heap: " + printableBytes(mem2[0]-mem[0])
          + " nonheap: " + printableBytes(mem2[1]-mem[1]));
  
      // Create all of the Edges.
      cleanMem(mbean);
      mem=getMemory(mbean);
      timer = System.currentTimeMillis();
      for (int e=0; e<numEdges; e++)
      {
        final Node n1 = graph.getNode(random.nextInt(numNodes));
        final Node n2 = graph.getNode(random.nextInt(numNodes));
        graph.addEdge(et, n1, n2, 1.0);
        if (STRESS_DEBUG) System.err.println("Connected         ["
            + n1.getName() + "]["
            + n2.getName()
            + "] weight+=1.0"
            + " edgeType="
            + et.getName());
        if (e%250000==0 && e>0)
          log.config("Created " + e + " Edges in "
              + (System.currentTimeMillis()-timer)/1000.0
              + " secs");
      }
      mem2=getMemory(mbean);
      log.info("Created " + numEdges + " Edges in "
          + (System.currentTimeMillis()-timer)/1000.0
          + " secs" + lineSeparator
          + "Memory change heap: " + printableBytes(mem2[0]-mem[0])
          + " nonheap: " + printableBytes(mem2[1]-mem[1]));
  
      // Create an array to hold the random indexes for Nodes.
      final int[] nodeIndexes = new int[numNodes];
      // Seed each slot with an index. 
      for (int i=0; i<nodeIndexes.length; i++)
        nodeIndexes[i] = i;
      // Shuffle all the indexes.
      for (int i=nodeIndexes.length; --i>0;)
      {
        final int swapidx = random.nextInt(i+1);
        final int tmp = nodeIndexes[i];
        if (false)
          System.err.println("Swapping nodeIndexes[" + i + "]="
              + nodeIndexes[i] + " and NodeIndexes["
              + swapidx + "]=" + nodeIndexes[swapidx]);
        nodeIndexes[i] = nodeIndexes[swapidx];
        nodeIndexes[swapidx] = tmp;
      }
      if (false)
      {
        System.err.print("Getting Nodes in order:");
        for (int i : nodeIndexes)
          System.err.print(" "+i);
        System.err.println();
      }
  
      cleanMem(mbean);
      mem=getMemory(mbean);
      timer = System.currentTimeMillis();
      final int maxIter = 100;
      for (int count=0; count < maxIter; count++)
      {
        for (int i=0; i<numNodes; i++)
        {
          // Get a node randomly through the shuffled nodeIndexes[].
          final Node node = graph.getNode(nodeIndexes[i]);
          final Edge[] neighborEdges = node.getEdges();
  
          for (int f=0; f<numFields; f++)
          {
            // Get the node's value for field f.
            double result = node.getValue(f);
  
            // Iterate over each neighbor.
            for (final Edge edge : neighborEdges)
            {
              // Access field f for each neighbor.
              result += edge.getDest().getValue(f);
              // Access Edge weight for connection to each neighbor.
              result += edge.getWeight();
            }
          }
        }
        log.config("Completed loop " + count + " in "
            + (System.currentTimeMillis()-timer)/1000.0 + " secs");
      }
  
      mem2=getMemory(mbean);
      log.info("Completed (" + maxIter + "x) accessing "
          + numNodes + " Nodes in "
          + (System.currentTimeMillis()-timer)/1000.0
          + " secs" + lineSeparator
          + "Memory change heap: " + printableBytes(mem2[0]-mem[0])
          + " nonheap: " + printableBytes(mem2[1]-mem[1]));
  
      log.info("SUCCESS: Graph got " + graph.numNodes() + " Nodes and "
          + graph.numEdges() + " (actual) Edges");
      return graph;
    }
  
    // Helper method for main().
    private static void printNodes(Node[] nodes)
    {
      // Print out the Node values.
      System.err.println("Printing nodes");
      for (final Node n : nodes)
      {
        System.err.printf("Values for Node %-20s are:", n.getName());
        for (final double d : n.getValues())
          System.err.printf(" %f", d);
        System.err.println();
      }
    }
  
    /** A test driver for the class.
     * @param args An array of Strings as the arguments for the test
     * driver.  If one argument is supplied, it contains the name of
     * the schema file to use in the test which is passed to
     * readSchema.  If two arguments are supplied, they contain the
     * names of the GDA formatted Node instance data and Edge instance
     * data which are passed to readGDASchema.  If three arguments are
     * supplied, they are integer values representing a stress test
     * inputs for the driver which are passed to stressTest.  It will
     * create nodes in memory using number-of-fields-per-node = arg0,
     * number-of-nodes = arg1, number-of-edges = arg2.  All Graph
     * elements are populated randomly in the latter case.
     */
    public static final void main(String args[])
    {
      if (args.length == 1)
      {
        final File input = new File(args[0]);
        final Graph graph = SchemaReader.readSchema(input);
        printNodes(graph.getNodes());
      }
      else if (args.length == 2)
      {
        final File nodeFile = new File(args[0]);
        final File edgeFile = new File(args[1]);
        final Graph graph = readGDASchema(nodeFile, edgeFile);
        printNodes(graph.getNodes());
     }
      else if (args.length == 3)
        SchemaReader.stressTest(Integer.valueOf(args[0]),
            Integer.valueOf(args[1]),
            Integer.valueOf(args[2]));
      else
        throw new RuntimeException("Got "+args.length+" args");
    }
  }
