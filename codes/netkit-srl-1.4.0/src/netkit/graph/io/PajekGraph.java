package netkit.graph.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import netkit.classifiers.Classification;
import netkit.classifiers.Estimate;
import netkit.graph.Attribute;
import netkit.graph.AttributeCategorical;
import netkit.graph.AttributeExpandableCategorical;
import netkit.graph.AttributeKey;
import netkit.graph.Attributes;
import netkit.graph.Edge;
import netkit.graph.EdgeType;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.Type;
import netkit.util.GraphMetrics;
import netkit.util.NetKitEnv;

public class PajekGraph {
  private static final Logger log = NetKitEnv.getLogger("netkit.graph.io.PajekGraph");
  private static final String lineSeparator = System.getProperty("line.separator");

  private static final Pattern pajekNodePattern = Pattern.compile("(\\d+)\\s+([\\\"\\\']([^\\\"\\\']+)|(\\s+))");
  private static final Pattern pajekEdgePattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s*(\\S+)?");
  
  // look for node shapes and assign them to different nodetypes
  private static final Pattern pajekNodeTypePattern = Pattern.compile("\\s(ellipse|box|diamond|cross|triangle|empty)(\\s|$)",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
  
  // look for node colors and assign them to different classes
  private static final Pattern pajekNodeClassPattern = Pattern.compile("\\sbc\\s(\\S+)(\\s|$)",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
  
  // color names have to be upper-cased to be understood by Pajek
  private static final String[][] pajekColors = new String[][]{
    {"Blue","NavyBlue","CornflowerBlue","LightCyan","LSkyBlue","Gray10"},
    {"BrickRed","Bittersweet","Red","RedOrange","LightOrange","Gray10"},
    {"OliveGreen","PineGreen","Green","LightGreen","LFadedGreen","Gray10"},
    {"Purple","Orchid","Thistle","LightPurple","LightPurple","Gray10"},
    {"GoldenRod","Yellow","Canary","LightYellow","LightYellow","Gray10"},
    {"Orange","YellowOrange","Dandelion","Apricot","LightOrange","Gray10"},
    {"Brown","RawSienna","Tan","Apricot","Apricot","Gray10"},
    {"RedViolet","Mulberry","VioletRed","CarnationPink","Pink","Gray10"},
    {"Gray","Gray45","Gray40","Gray30","Gray20","Gray10"},
  };
  
  // convert node type to a shape
  private static final String[] pajekShapes = new String[]{
    "Ellipse","Box","Diamond","Cross","Triangle"
  };
  
  /** Overloaded entry point for {@link #readGraph(Reader)}
   * @param pajekFile a File containing the Pajek formatted graph data.
   * @return the constructed Graph object.
   * @throws RuntimeException if any of the file format constraints
   * are violated or the files cannot be read.
   */
  public static Graph readGraph(File pajekFile)
  {
    try {
      return readGraph(new FileReader(pajekFile));
    }
    catch(FileNotFoundException fnfe)
    {
      throw new RuntimeException(fnfe.getMessage());
    }
  }
  
  private static Attributes getAttributes(final Graph graph, final String nodeType) {
    Attributes attr = graph.getAttributes(nodeType);
    if(attr == null) {
      attr = new Attributes(nodeType);
      final AttributeKey key = new AttributeKey("Key");
      final AttributeExpandableCategorical label = new AttributeExpandableCategorical("Label");
      final AttributeExpandableCategorical cls = new AttributeExpandableCategorical("Class");
      attr.add(key);
      attr.add(label);
      attr.add(cls);
      graph.addAttributes(attr);
      log.info("Constructed new Pajek Attributes container" + lineSeparator + "Got: " + attr);
    }
    return attr;
  }
  
  private static EdgeType getEdgeType(final Graph graph, final Node n1, final Node n2) {
    final String nt1 = n1.getType();
    final String nt2 = n2.getType();
    final String en = nt1+"-"+nt2;
    EdgeType et = graph.getEdgeType(en);
    if(et == null) {
      et = new EdgeType(en, nt1, nt2);
      graph.addEdgeType(et);
      log.info("Constructed new EdgeType container" + lineSeparator + "Got: " + et);
    }
    return et;
  }

  /** Reads the Graph information from Pajek formatted input,
   * constructs the data structures and instantiates all of the
   * instance data.  This method is provided for compatability with
   * the Pajek file format.
   * @param pajekReader a Reader object containing the Pajek formatted graph data.
   * @return the constructed Graph object.
   * @throws RuntimeException if any of the file format constraints
   * are violated or the files cannot be read.
   */
  public static Graph readGraph(final Reader pajekReader)
  {
    final Graph graph = new Graph();

    try 
    {
      log.info("Reading pajek data");
      final int NODE=1;
      final int EDGE=2;
      final int ARC=3;
      int numVertices = 0;
      int readVertices = 0;
      List<String> nodeTypeMap = new ArrayList<String>(); // for each vertex, which node type is it.
      int state = 0;
      final double[] results = new double[3];

      final LineNumberReader lr = new LineNumberReader(pajekReader);
      for (String s = lr.readLine(); s != null; s = lr.readLine())
      {
        // Strip leading and trailing whitespace.
        s = s.trim();
        
        // Pajek seems to stop when it sees a blank line, so let's do the same
        if(s.length()==0)
          break;
        
        if(s.startsWith("*")) {
          if(s.toLowerCase().startsWith("*network")) {
            // we output this, so let us at least allow the input of it as well
            // however, we ignore it since there is no NetKit equivalent
          } else if(s.toLowerCase().startsWith("*vertices")) {
            state = NODE;
            log.info("Parsing '*Vertices' header: \""+s+"\"");
            String[] tokens = s.split("\\s+");
            numVertices = Integer.parseInt(tokens[1]);
          } else if (s.toLowerCase().startsWith("*arcs")) {
            log.info("Parsing '*Arcs' header: \""+s+"\"");
            state = ARC;
            if(readVertices != numVertices)
              log.warning("Expected to read "+numVertices+" vertices, but got "+readVertices+" instead!");
          } else if (s.toLowerCase().startsWith("*edges")) {
            log.info("Parsing '*Edges' header: \""+s+"\"");
            state = EDGE;
            if(readVertices != numVertices)
              log.warning("Expected to read "+numVertices+" vertices, but got "+readVertices+" instead!");
          } else {
            throw new IllegalStateException("Reading header "+s+" - unknown Pajek directive --- NetKit only understands '*Vertices', '*Edges', and '*Arcs'");
          }
        } else if(state == 0) {
          // do nothing ... this means we have yet to see a directory, so just ignore
          log.warning("Saw line \""+s+"\" before *Vertices");
        } else {
          switch(state) {
          case NODE:
            Matcher m = pajekNodePattern.matcher(s);
            if(!m.find())
              throw new IOException("["+lr.getLineNumber()+"] Tried to get node, but could not parse \""+s+"\"");
            
            String id = m.group(1);
            String lbl = m.group(3);
            log.finest("Parsed node ["+id+","+lbl+"] from \""+s+"\"");
            
            m = pajekNodeTypePattern.matcher(s);
            String nodeType = "PajekNode";
            if(m.find())
              nodeType = m.group(1);

            m = pajekNodeClassPattern.matcher(s);
            String clsVal = "?";
            if(m.find())
              clsVal = m.group(1);

            Attributes attr = getAttributes(graph,nodeType);
            Attribute key = attr.getAttribute(0);
            AttributeExpandableCategorical label = (AttributeExpandableCategorical)attr.getAttribute(1);
            AttributeExpandableCategorical cls = (AttributeExpandableCategorical)attr.getAttribute(2);
            
            results[0] = key.parseAndInsert(id);
            results[1] = label.parseAndInsert(lbl);
            results[2] = cls.parseAndInsert(clsVal);
                        
            final Node n = graph.addNode(id, attr);
            n.setValues(results);
            if (log.isLoggable(Level.FINE))
                log.fine("Created node <"+n+">");
            readVertices++;
            
            int idx = Integer.parseInt(id);
            while(nodeTypeMap.size()<=idx)
              nodeTypeMap.add(null);
            nodeTypeMap.set(idx, nodeType);
            break;
            
          case ARC:
          case EDGE:
            m = pajekEdgePattern.matcher(s);
            if(!m.find())
              throw new IOException("[line "+lr.getLineNumber()+"] Tried to get arc/edge, but could not parse \""+s+"\"");
            
            String id1 = m.group(1);
            String id2 = m.group(2);
            String wtS = (m.group(3)==null) ? "1" : m.group(3);
            log.finest("Parsed arc/edge ["+id1+","+id2+","+wtS+"] from \""+s+"\"");
            
            int idx1 = Integer.parseInt(id1);
            int idx2 = Integer.parseInt(id2);
            Node n1 = graph.getNode(id1, nodeTypeMap.get(idx1));
            Node n2 = graph.getNode(id2, nodeTypeMap.get(idx2));
            double wt = Double.parseDouble(wtS); 
            if(wt<=0)
              throw new IOException("[line "+lr.getLineNumber()+"] Edge weight cannot be negative in \""+s+"\"");
            
            EdgeType edgeType = getEdgeType(graph, n1, n2);
            graph.addEdge(edgeType,n1,n2,wt);

            if(state == EDGE) {
              edgeType = getEdgeType(graph, n2, n1);
              graph.addEdge(edgeType,n2,n1,wt);
            }
            break;
          }
        }
      }
      lr.close();
    }
    catch(IOException ioe)
    {
      throw new RuntimeException(ioe.getMessage());
    }
    if (log.isLoggable(Level.CONFIG))
    {
      final StringBuilder logString = new StringBuilder("Expandable CATEGORICAL fields:");
      logString.append(lineSeparator);
      for (final Attributes attr : graph.getAllAttributes()) {
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
    }

    log.info("SUCCESS: Graph got " + graph.numNodes()
        + " Nodes and " + graph.numEdges() + " (actual) Edges");
    
    return graph;
  }
  
  private static String getTruthColor(final Node node, final Classification truth) {
    if(truth == null || !node.getType().equalsIgnoreCase(truth.getNodeType()))
      return null;

    if(truth.isUnknown(node))
      return null;
    
    final int idx = truth.getClassValue(node);
    
    return pajekColors[idx % pajekColors.length][0];
  }
  
  // ad hoc scaling of prediction to get a proper color
  private static String getPredictionColor(final Node node, final Estimate pred) {
    if(pred == null || !node.getType().equalsIgnoreCase(pred.getNodeType()))
      return "White";
  
    final double[] p = pred.getEstimate(node);
    if(p==null)
      return "White";
    
    final int idx = pred.getClassification(node);
    
    // ad hoc scaling of colors from 0 (very sure) to final color (not sure at all),
    // depending on how close the 'predicted' class is to 1
    final double length = 1.0D - (1.0D/(double)p.length);
    
    // how far are we from 1 towards "don't know"
    final double confidence = (1-0D-p[idx])/length;
    
    final double scale = 1.0D/(1.0D+Math.exp(-(4.0D*confidence)+1.75)) * (double)(p.length+1);
    
    final int cIdx = (int)scale; // this will lie in range [0:p.length-1];
    return pajekColors[idx % pajekColors.length][cIdx];
  }
  
  public static void saveGraph(final Graph graph, final PrintWriter pw, final Classification truth, final Estimate pred, final String labelAttribute) {
    final GraphMetrics metrics = graph.getMetrics();
    final String[] nodeTypes = graph.getNodeTypes();

    final NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    
    // ad hoc scaling of the shapes as they are being output
    final double numNodes = graph.numNodes();
    double xfact = 9.0D-Math.log(numNodes);
    if(xfact<1) xfact = 1.0D;
    if(xfact>6) xfact = 6.0D;
    double yfact = xfact * 0.8;
    if(yfact<1) yfact = 1.0D;
    final String xf = nf.format(xfact);
    final String yf = nf.format(yfact);
    
    final Map<String,String> nodeShapeMap = new HashMap<String,String>();
    for(int i=0;i<nodeTypes.length;i++) {
      final int sIdx = (i % pajekShapes.length);
      nodeShapeMap.put(nodeTypes[i], pajekShapes[sIdx]);
    }

    pw.println("*Network PajekGraph");
    pw.println("*Vertices "+graph.numNodes());
    for(final Node node : graph.getNodes()) {
      final String shape = nodeShapeMap.get(node.getType());
      String label = node.getName();
      final Attribute attr = (labelAttribute == null) ? null : node.getAttributes().getAttribute(labelAttribute);
      if(attr != null) {
        final int aIdx = node.getAttributeIndex(labelAttribute);
        final double val = node.getValue(aIdx);
        if(attr.getType() == Type.CATEGORICAL) {
          label = ((AttributeCategorical)attr).getToken((int)val);
        } else {
          label = ""+val;
        }
      }
      pw.print((metrics.getNodeIndex(node)+1)+" \""+label+"\" "+shape);
      if(xfact>1.0D) pw.print(" x_fact "+xf);
      if(yfact>1.0D) pw.print(" y_fact "+yf);
      
      String insideColor = (pred != null) ? getPredictionColor(node, pred) : getTruthColor(node, truth);
      if(insideColor != null)
        pw.print(" ic "+insideColor);
      if(pred != null) {
        String truthColor = getTruthColor(node, truth);
        if(truthColor != null)
          pw.print(" bc "+truthColor);
      }
      pw.println();
    }

    boolean ep=false;
    for(Edge edge : graph.getEdges()) {
      final Node src = edge.getSource();
      final Node dest = edge.getDest();
      
      // if this is not undirected
      if(graph.getEdge(edge.getEdgeType().getName(),src,dest) == null)
        continue;

      if(!ep) {
        pw.println("*Edges");
        ep=true;
      }
      
      final int srcID = metrics.getNodeIndex(src)+1;
      final int destID = metrics.getNodeIndex(dest)+1;
      final double wt = 1+Math.log(edge.getWeight());
      pw.println(srcID+" "+destID+" "+wt+" w "+wt);
    }

    boolean ap=false;
    for(Edge edge : graph.getEdges()) {
      final Node src = edge.getSource();
      final Node dest = edge.getDest();

      // if this is undirected
      if(graph.getEdge(edge.getEdgeType().getName(),src,dest) != null)
        continue;

      if(!ap) {
        pw.println("*Arcs");
        ap=true;
      }
      
      final int srcID = metrics.getNodeIndex(src);
      final int destID = metrics.getNodeIndex(dest);
      final double wt = 1+Math.log(edge.getWeight());
      pw.println(srcID+" "+destID+" "+wt+" w "+wt);
    }
  }
  
  public static void saveGraph(final Graph graph, final PrintWriter pw) {
    saveGraph(graph, pw, null, null, null);
  }
  public static void saveGraph(final Graph graph, final String file) {
    saveGraph(graph,file,null,null,null);
  }
 
  /**
   * Save the given graph as a pajek graph, to the given file, using the classification and estimate and label.
   * The nodes in the Pajek graph will be colored based on the truth and pred input variables.  If there is a pred
   * object, then the inside of the pajek nodes will be colored based on the prediction and the border of the nodes
   * will be colored based on the truth.   If there is no pred object, then the inside of the pajek node will be
   * colored based on the truth.  If an attribute name is provided, then that will be used as the label of the
   * nodes.  If that attribute is not available, then the id of the node is used instead.  
   * 
   * @param graph The NetKit graph to save in Pajek format
   * @param file The file to save it to
   * @param truth The true labels of nodes (can be null, otherwise used to color the node)
   * @param pred The predictions of labels of nodes (can be null)
   * @param labelAttribute The name of the attribute to use as the label of the nodes (if null, then the node ID is used).
   */
  public static void saveGraph(final Graph graph, final String file, final Classification truth, final Estimate pred, final String labelAttribute) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(file);
    saveGraph(graph,pw,truth,pred, labelAttribute);
    pw.println();
    pw.close();
  }
}
