package netkit;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

import netkit.graph.Attributes;
import netkit.graph.Edge;
import netkit.graph.EdgeType;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.io.EdgeWriterRN;
import netkit.graph.io.SplitParser;
import netkit.util.NetKitEnv;

public class EdgeTransformer {
  private final Logger logger = NetKitEnv.getLogger(this);
  private final SplitParser parser = SplitParser.getParserCOMMA(3);
  
  // sort so that lowest scores are first
  private static final Comparator<Edge> minKcmp =  new Comparator<Edge>() {
    @Override
    public int compare(Edge e1, Edge e2) {
      double diff = e1.getWeight()-e2.getWeight();
      return (int)Math.signum(diff);
    }
  };

  // sort so that lowest scores are first
  private static final Comparator<Edge> topKcmp =  new Comparator<Edge>() {
    @Override
    public int compare(Edge e1, Edge e2) {
      double diff = e2.getWeight()-e1.getWeight();
      return (int)Math.signum(diff);
    }
  };


  private Reader input = new InputStreamReader(System.in);
  private PrintWriter output = new PrintWriter(System.out, true);
  private boolean reverse = false;
  private int topK = -1;
  private int minK = -1;
  private double pruneless = Double.NaN;
  private double prunemore = Double.NaN;
  private double reweight = Double.NaN;
  
  public EdgeTransformer(final String[] args) {
    setOptions(args);
  }
  
  public Graph readEdges(final Reader reader, final EdgeType edgeType) {
    final Graph g = new Graph();
    final String node1type = edgeType.getSourceType();
    final String node2type = edgeType.getDestType();
    Attributes as = new Attributes(node1type);
    g.addAttributes(as);
    g.addEdgeType(edgeType);
    EdgeType revEdgeType = null;
    
    if(!node1type.equals(node2type)) {
      as = new Attributes(node2type);
      g.addAttributes(as);
      if(reverse) {
        revEdgeType = new EdgeType(edgeType.getName()+"-reverse",node2type,node1type);
        g.addEdgeType(revEdgeType);
      }
    } else if(reverse)
      revEdgeType = edgeType;
    
    final LineNumberReader lnr = new LineNumberReader(reader);
    String s="";
    try
    {
      for (s = lnr.readLine(); s != null; s = lnr.readLine())
      {
        // Strip leading and trailing whitespace.
        s = s.trim();
  
        //  Skip % or # comments or blank (whitespace-only) lines.
        if (s.length() == 0 || s.charAt(0) == '%' || s.charAt(0) == '#')
          continue;
  
        final String[] tokens = parser.parseLine(s);
        if (tokens.length != 3)
          throw new RuntimeException("Invalid number of fields, expected 3, got " + tokens.length + " at line " + lnr.getLineNumber());
        
        final double weight = Double.parseDouble(tokens[2]);

        if(!Double.isNaN(pruneless) && weight < pruneless)
          continue;
        if(!Double.isNaN(prunemore) && weight > prunemore)
          continue;
        
        String name = tokens[0].toLowerCase();
        Node n1 = g.getNode(name, node1type);
        if(n1 == null)
          n1 = g.addNode(name, as);

        name = tokens[1].toLowerCase();
        Node n2 = g.getNode(name, node2type);
        if(n2 == null)
          n2 = g.addNode(name, as);

        g.addEdge(edgeType,n1,n2,weight);
        if(reverse)
          g.addEdge(revEdgeType,n2,n1,weight);
      }
    }
    catch(IOException ioe) {
      throw new RuntimeException("Error reading line?",ioe);
    }
    return g;
  }
  
  public static void pruneMinK(final Graph graph, final EdgeType edgeType, final int minK, final boolean reverse) {
    if(minK<1)
      return;
    pruneK(graph,edgeType,minK,-1,reverse);
  }
  
  public static void pruneTopK(final Graph graph, final EdgeType edgeType, final int topK, final boolean reverse) {
    if(topK<1)
      return;
    pruneK(graph,edgeType,-1,topK,reverse);
  }
  
  private static void pruneK(final Graph graph, final EdgeType edgeType, final int minK, final int topK, final boolean reverse) {
    if(topK<1 && minK<1)
      return;
    final int max = (topK<0) ? minK : topK;
    final Comparator<Edge> cmp = (topK<0) ? minKcmp : topKcmp;

    final String nodeType = edgeType.getSourceType();
    final String edgeName = edgeType.getName();
    // first prune edges
    for(final Node node : graph.getNodes(nodeType)) {
      final Edge[] edges = node.getEdgesByType(edgeName);
      if(edges.length<=max)
        continue;
      Arrays.sort(edges, cmp);
      for(int i=max+1;i<edges.length;i++)
        node.removeEdge(edgeName, edges[i].getDest());
    }
    
    if(!reverse)
      return;
    
    // complete the reverse edges as needed
    for(final Node node : graph.getNodes(nodeType)) {
      for(final Edge edge : node.getEdgesByType(edgeName)) {
        final Node dest = edge.getDest();
        if(graph.getEdge(edgeName, dest, node) == null)
          graph.addEdge(edgeType, dest, node, edge.getWeight());
      }
    }
  }
  
  public static void pruneLess(final Graph graph, final EdgeType edgeType, final double pruneless) {
    if(Double.isNaN(pruneless))
      return;
    pruneThreshold(graph,edgeType,pruneless,Double.NaN);
  }
  public static void pruneMore(final Graph graph, final EdgeType edgeType, final double prunemore) {
    if(Double.isNaN(prunemore))
      return;
    pruneThreshold(graph,edgeType,Double.NaN,prunemore);
  }

  private static void pruneThreshold(final Graph graph, final EdgeType edgeType, final double pruneless, final double prunemore) {
    final String edgeTypeName = edgeType.getName();
    for(final Edge edge : graph.getEdges(edgeType)) {
      final double weight = edge.getWeight();
      if(!Double.isNaN(pruneless) && weight >= pruneless)
        continue;
      if(!Double.isNaN(prunemore) && weight <= prunemore)
        continue;
      graph.removeEdge(edgeTypeName, edge.getSource(), edge.getDest());
    }
  }
  
  public static void reweight(final Graph graph, final EdgeType edgeType, final double reweight) {
    reweight(graph, graph.getEdges(edgeType), reweight);
  }
  
  public static void reweight(final Graph graph, final Edge[] edges, final double reweight) {
    if(Double.isNaN(reweight))
      return;
    if(edges == null || edges.length==0)
      return;
    
    final String nodeType = edges[0].getEdgeType().getSourceType();
    
    Node[] nodes = graph.getNodes(nodeType);
    double[] weights = new double[nodes.length];
    Arrays.fill(weights,0.0D);

    for(Edge edge : edges)
      weights[edge.getDest().getIndex()]+=edge.getWeight();
    
    for(Edge edge : edges) {
      double nodeWt = weights[edge.getDest().getIndex()];
      if(nodeWt == 0)
        continue;
      double newWt = reweight * ( edge.getWeight() / nodeWt );
      if(newWt <= 0)
          newWt = 0.000000000000000000001;
      edge.setWeight(newWt);
    }
  }
  
  
  public void run() {
    final EdgeType edgeType = new EdgeType("dummy","dummy","dummy");
    final Graph graph = readEdges(input,edgeType);
    transform(graph,edgeType);
    EdgeWriterRN.writeEdges(graph.getEdges(), output);
    output.close();
  }
  
  public void transform(final Graph graph, final EdgeType edgeType) {
    pruneThreshold(graph,edgeType,pruneless,prunemore);
    pruneK(graph,edgeType,minK,topK,reverse);
    reweight(graph,edgeType,reweight);
  }
  
  public void resetOptions() {
    output = new PrintWriter(System.out, true);
    reverse = false;
    topK = -1;
    minK = -1;
    pruneless = Double.NaN;
    prunemore = Double.NaN;
    reweight = Double.NaN;   
  }
  
  public double getReweight() { return reweight; }
  public void setReweight(final double w) {
    if(w<=0)
      throw new IllegalArgumentException("Invalid reweighting score: "+w+".  It must be a positive value.");
    reweight = w;
    logger.info("Reweighting edges to sum to "+reweight+" per node");
  }
  
  public double getPruneMore() { return prunemore; }
  public void setPruneMore(final double t) {
    if(!Double.isNaN(pruneless))
      throw new IllegalStateException("Cannot use prunemore if pruneless is set!");
    prunemore = t;
    logger.info("Pruning edges whose weight is more than "+prunemore);
  }
  
  public double getPruneLess() { return pruneless; }
  public void setPruneLess(final double t) {
    if(!Double.isNaN(prunemore))
      throw new IllegalStateException("Cannot use pruneless if prunemore is set!");
    pruneless = t;
    logger.info("Pruning edges whose weight is less than "+pruneless);
  }
  
  public int getMinK() { return minK; }
  public void setMinK(final int k) {
    if(k<1)
      throw new IllegalArgumentException("Invalid value for minK: "+k+".  It must be > 0!");
    if(topK>0)
      throw new IllegalStateException("Cannot set minK if topK is set!");
    minK = k;
    logger.info("Pruning edges with minK="+topK);
  }

  public int getTopK() { return topK; }
  public void setTopK(final int k) {
    if(k<1)
      throw new IllegalArgumentException("Invalid value for topK: "+k+".  It must be > 0!");
    if(minK>0)
      throw new IllegalStateException("Cannot set topK if minK is set!");
    topK = k;
    logger.info("Pruning edges with topK="+topK);
  }

  public boolean getReverse() { return reverse; }
  public void setReverse(final boolean r) {
    reverse = r;
    logger.info("Set edge reversal to true");    
  }

  public Reader getInput() { return input; }
  public void setInput(final Reader r) {
    input = r;
  }
  public void setInput(final String file) {
    if(file.equals("-"))
      setInput(new InputStreamReader(System.in));
    else {
      try {
        setInput(new FileReader(file));
      } catch(IOException ioe) {
        throw new IllegalArgumentException("Could not read from "+file+": "+ioe.getMessage());
      }
    }  
    logger.info("Reading edges from '"+file+"'");
  }

  public PrintWriter getOutput() { return output; }
  public void setOutput(final PrintWriter pw) {
    output = pw;   
  }
  public void setOutput(final String file) {
    if(file.equals("-"))
      setOutput(new PrintWriter(System.out, true));
    else {
      try {
        setOutput(new PrintWriter(new FileWriter(file), true));
      } catch(IOException ioe) {
        throw new IllegalArgumentException("Could not open file "+file);
      }
    }
    logger.info("Outputting new edges to '"+file+"'");
  }
  
  private void setOptions(String[] argv) {
    if (argv.length == 0)
      usage(null);

    int idx = 0;
    if(argv[idx].equalsIgnoreCase(Netkit.edgetransform))
      idx++;
    while (idx < argv.length && argv[idx].startsWith("-")) {
      String p = argv[idx].toLowerCase().substring(1);
      if (p.startsWith("h")) {
        usage(null);
      } else if (p.equals("log")) {
        final String filename = argv[++idx];
        NetKitEnv.setLogfile(filename);
        logger.info("Set log output to "+filename);
      } else if (p.equals("reverse")) {
        setReverse(true);
      } else if (p.equals("topk")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setTopK(Integer.parseInt(argv[idx]));
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.equals("mink")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setMinK(Integer.parseInt(argv[idx]));
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.equals("pruneless")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setPruneLess(Double.parseDouble(argv[idx]));
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.equals("prunemore")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setPruneMore(Double.parseDouble(argv[idx]));
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.equals("weight")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setReweight(Double.parseDouble(argv[idx]));
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.equals("output")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setOutput(argv[idx]);
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else {
        usage("Option "+p+" not recognized");
      }
      idx++;
    }
    if(idx+1 != argv.length)
      usage("Illegal number of arguments");
    
    try {
      setInput(argv[idx]);
    } catch(Exception ex) {
      usage(ex.getMessage());
    }
  }
  
  public static String[] getCommandLines() {
    String opt = Netkit.edgetransform;
    return new String[]{"usage: Netkit "+opt+" [-h] [OPTIONS] <edge-file>"};
  }
  
  public static void usage(String msg) {
    for(String cmd : getCommandLines())
      System.out.println(cmd);
    
    if (msg == null) {
      System.out.println();
      System.out.println("OPTIONS");
      System.out.println("  -h                 This help screen");
      System.out.println("  -log <filename>    Where to send logging information.");
      System.out.println("                       In logging.properties:");
      System.out.println("                         handlers property must be set to java.util.logging.FileHandler.");
      System.out.println("                       default out: see (java.util.logging.FileHandler.pattern) in logging.properties");
      System.out.println("   <edge-file>       Where to read edges from (in Netkit .rn format)");
      System.out.println("                       If you specify '-', then read from STDIN");
      System.out.println();
      System.out.println("EDGE TRANSFORM OPTIONS");
      System.out.println("  -reverse           Add reverse edges");
      System.out.println("  -topK K            Prune edges to only include K outgoing edges from a node with highest weight");
      System.out.println("                       Will add reverse edge if '-reverse' is set");
      System.out.println("                       Cannot be used with '-minK'");
      System.out.println("  -minK K            Prune edges to only include K outgoing edges from a node with lowest weight");
      System.out.println("                       Will add reverse edge if '-reverse' is set");
      System.out.println("                       Cannot be used with '-topK'");
      System.out.println("  -pruneless X       Prune edges whose weight is less than threshold");
      System.out.println("                       Cannot be used with '-prunemore'");
      System.out.println("  -prunemore X       Prune edges whose weight is more than threshold");
      System.out.println("                       Cannot be used with '-pruneless'");
      System.out.println("  -weight X          Re-weight edges such that total weight of outgoing");
      System.out.println("                       edges from a node sum to X");
      System.out.println("                       this reweighting will happen after any pruning is done");
      System.out.println("  -output (file|-)   Output new edges to this file.");
      System.out.println("                       If you specify '-', then write to STDOUT (default)");
    } else {
      System.out.println(msg);
    }
    System.exit(0);
  }
  
  public static void run(String[] argv) {
    EdgeTransformer et = new EdgeTransformer(argv);
    et.run();
  }

}
