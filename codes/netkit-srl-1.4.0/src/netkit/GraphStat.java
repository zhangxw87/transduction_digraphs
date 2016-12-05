package netkit;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import netkit.classifiers.Classification;
import netkit.graph.Attribute;
import netkit.graph.AttributeCategorical;
import netkit.graph.Attributes;
import netkit.graph.EdgeType;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.Type;
import netkit.graph.io.DotGraph;
import netkit.graph.io.NetkitGraph;
import netkit.graph.io.PajekGraph;
import netkit.util.GraphMetrics;
import netkit.util.ApproximateCentralities;
import netkit.util.HistogramDiscrete;
import netkit.util.ModularityClusterer;
import netkit.util.NetKitEnv;

public class GraphStat {
  private static final Logger logger = NetKitEnv.getLogger(GraphStat.class);
  
  private boolean saveDot = false;
  private boolean savePajek = false;
  private boolean saveNetkit = false;
  private String pajekLabel = null; // which attribute to use as the label of a pajek graph
  private String pajekColorStr = null; // [nt:]attr - which node type and attribute to use for coloring the nodes on the pajek graph
  private Classification pajekColor = null; // using pajekNT and pajekColor, create this classification object
  private boolean pruneSingletons = false;
  private boolean cluster = false;
  private boolean centrality = true;
  private boolean alphaCentrality = false;
  private boolean pagerank = false;
  private boolean degreeStat = true;
  private boolean coefficients = true;
  private boolean calcAssort = true;
  private boolean saveAttributes = false;
  private boolean appendStatistics = true;
  private String saveClusterStem = null;
  private String saveComponentStem = null;
  private String saveGraphStem = null;
  private String schemaFile = null;
  
  private String output = "-";
  private String globalOut = null;
  private String degreeOut = null;
  private String nodeOut = null;
   
  public GraphStat() {}
  
  public GraphStat(final String[] args) {
    setOptions(args);
  }
  
  private void cluster(final Graph graph) {
    if(cluster) {
      final GraphMetrics metrics = graph.getMetrics();
      metrics.calcClusterProgress();
      ModularityClusterer mc = metrics.getClusterer();
      if(mc.percentDone()!=1.0D) {
        logger.info("Performing clustering of graph (community detection) - O(N^3)");
        mc.startClustering();
      }
    }
  }
  
  private void printSubgraphSummary(final Graph origGraph, final Graph subGraph, final PrintWriter pw, final int id) {
    final StringBuilder sb = new StringBuilder();
    final double numNodes = subGraph.numNodes();
    sb.append(id).append(',');
    sb.append(numNodes/(double)origGraph.numNodes()).append(',');
    sb.append(subGraph.numNodes());

    final String[] ntNames = subGraph.getNodeTypes();
    if(ntNames.length > 1) {
      for(final String nt : ntNames) {
        sb.append(',').append(subGraph.numNodes(nt));
        sb.append(',').append(subGraph.numNodes(nt)/numNodes);
      }
    }
    final String[] etNames = subGraph.getEdgeTypeNames();
    sb.append(',').append(subGraph.numEdges());
    if(etNames.length>1) {
      for(final String en : subGraph.getEdgeTypeNames()) {
        sb.append(',').append(subGraph.numEdges(en));
        final double density = (double)subGraph.numEdges(en)/(numNodes*(numNodes-1));
        sb.append(',').append(density);
      }
    } else {
      final double density = (double)subGraph.numEdges()/(numNodes*(numNodes-1));
      sb.append(',').append(density);
    }
    pw.println(sb.toString());
  }
  
  private void printSummaryHeader(final Graph graph, final PrintWriter pw, final String sumtype) {
    final StringBuilder sb = new StringBuilder();
    sb.append(sumtype).append(" summaries (id,ratio-of-graph,num-nodes");
    final String[] ntNames = graph.getNodeTypes();
    if(ntNames.length > 1) {
      for(final String nt : ntNames){ sb.append(',').append("num-").append(nt).append(",ratio-").append(nt); }
    }
    final String[] etNames = graph.getEdgeTypeNames();
    sb.append(",num-edges");
    if(etNames.length>1) {
      for(final String en : graph.getEdgeTypeNames()) { sb.append(',').append("num-").append(en).append(",density-").append(en); }
    } else {
      sb.append(',').append("density");
    }
    sb.append(")");
    pw.println(sb.toString());
  }
  
  private void printSchemaSummary(final Graph graph, final String schemaFile, final String nodeFile) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(schemaFile);
    pw.println("@nodetype clusterSummary");
    pw.println("@attribute id KEY");
    pw.println("@attribute ratio-of-graph CONTINUOUS");
    pw.println("@attribute size DISCRETE");

    final String[] ntNames = graph.getNodeTypes();
    if(ntNames.length > 1) {
      for(final String nt : ntNames) {
        pw.print("@attribute num-");
        pw.print(nt);
        pw.println(" DISCRETE");
        pw.print("@attribute ratio-");
        pw.print(nt);
        pw.println(" CONTINUOUS");
      }
    }
    final String[] etNames = graph.getEdgeTypeNames();
    pw.println("@attribute num-edges DISCRETE");
    if(etNames.length>1) {
      for(final String en : graph.getEdgeTypeNames()) {
        pw.print("@attribute num-");
        pw.print(en);
        pw.println(" DISCRETE");
        pw.print("@attribute density-");
        pw.print(en);
        pw.println(" CONTINUOUS");
      }
    } else {
      pw.println("@attribute density CONTINUOUS");
    }
    pw.print("@nodedata ");
    pw.println(nodeFile);
    pw.close();
  }
  
  private void printNetKitClusterSummaries(final Graph graph, final PrintWriter pw) {
    cluster(graph);
    final ModularityClusterer mc = graph.getMetrics().getClusterer();
    int cnum=0;
    for(final Set<Node> nodeSet : mc.getConnectedClusterNodeSets()) {
      cnum++;
      final Graph subGraph = graph.subGraph(nodeSet);
      printSubgraphSummary(graph,subGraph,pw,cnum);
    }
  }
  
  private void printComponentSummaries(final Graph graph, final PrintWriter pw) {
    final GraphMetrics metrics = graph.getMetrics();
    List<List<Node>> nodeSets = new ArrayList<List<Node>>();
    for(int cnum=0;cnum<metrics.getNumComponents();cnum++)
      nodeSets.add(new ArrayList<Node>(metrics.getComponentSize(cnum)));
    for(Node node : graph.getNodes()) {
      final int cnum = metrics.getComponent(node);
      nodeSets.get(cnum).add(node);
    }
    int cnum=0;
    for(List<Node> nodeSet : nodeSets) {
      cnum++;
      final Graph subgraph = graph.subGraph(nodeSet);
      printSubgraphSummary(graph,subgraph,pw,cnum);
    }
  }
  
  public void printGlobalInfo(final Graph graph) {
    final GraphMetrics metrics = graph.getMetrics();
    
    PrintWriter globalPrint = NetKitEnv.getPrintWriter(getGlobalOutput());

    logger.info("Computing component statistics - O(N)");
    final int singletons = metrics.getNumSingletons();
    final int numComponents = metrics.getNumComponents();
    final int numNodes = graph.numNodes();
    final int numEdges = graph.numEdges();
    final double density = metrics.getGraphDensity();
    
    globalPrint.println("Graph-statistic,value");
    globalPrint.println("num-nodes,"+numNodes);
    for(final String nt : graph.getNodeTypes()) {
      final int numNT = graph.numNodes(nt);
      globalPrint.println("num-nodes["+nt+"],"+numNT);
      globalPrint.println("ratio-nodes["+nt+"],"+numNodes/(double)numNT);
    }
    globalPrint.println("num-edges,"+numEdges);
    for(final String et : graph.getEdgeTypeNames()) {
      final int numET = graph.numEdges(et);
      globalPrint.println("num-edges["+et+"],"+numET);
      globalPrint.println("ratio-edges["+et+"],"+numEdges/(double)numET);
    }
    globalPrint.println("num-components,"+numComponents);

    ModularityClusterer mc = null;
    if(cluster) {
      cluster(graph);
      mc = metrics.getClusterer();
      globalPrint.println("num-clusters,"+mc.getNumConnectedClusters());
    }
        
    globalPrint.println("num-singletons,"+singletons);
    globalPrint.println("graph-density,"+density);
    
    if(coefficients) {
      logger.info("Computing clustering coefficients, number triangles and non-triangle paths of length 2 - O(N^3)");
      final int path2 = metrics.getNumPath2();
      final int triangle = metrics.getNumtriangles();
      final double clusterCoeff = metrics.getGlobalClusterCoeff();
      final double localClusterCoeff = metrics.getLocalClusterCoeff();
      globalPrint.println("num-path-2,"+path2);
      globalPrint.println("num-triangles,"+triangle);
      globalPrint.println("global-cluster-coeff,"+clusterCoeff);
      globalPrint.println("local-cluster-coeff,"+localClusterCoeff);
    }
    
    if(centrality) {
      logger.info("Computing centrality statistics - O(N^3)");
      final double path = metrics.getCharacteristicPathLength();
      final double wPath = metrics.getWeightedCharacteristicPathLength();
      final double graphCentrality = metrics.getGraphCentrality();
      final double weightedGraphCentrality = metrics.getWeightedGraphCentrality();
      final double maxDist = metrics.getMaxDist();
      final double weightedMaxDist = metrics.getWeightedMaxDist();
      final double meanDist = metrics.getMeanDist();
      final double weightedMeanDist = metrics.getWeightedMeanDist();

      globalPrint.println("graph-centrality,"+graphCentrality);
      globalPrint.println("weighted-graph-centrality,"+weightedGraphCentrality);
      globalPrint.println("characteristic-path-length,"+path);
      globalPrint.println("weighted-characteristic-path-length,"+wPath);
      globalPrint.println("graph-diameter,"+maxDist);
      globalPrint.println("weighted-graph-diameter,"+weightedMaxDist);
      globalPrint.println("mean-node-dist,"+meanDist);
      globalPrint.println("weighted-mean-node-dist,"+weightedMeanDist);
    }

    if(alphaCentrality) {
	logger.info("Computing approximate alpha centralities");
	final ApproximateCentralities ac = metrics.getApproximateCentralities();
	ac.alphaCentrality.start();
	ac.weightedAlphaCentrality.start();
    }

    if(pagerank) {
	logger.info("Computing approximate pagerank centralities");
	final ApproximateCentralities ac = metrics.getApproximateCentralities();
	ac.pagerank.start();
	ac.weightedPagerank.start();
    }

    if(degreeStat) {
      logger.info("Computing degree statistics - O(N)");
      final double avgDegree = metrics.getMeanDegree();
      final HistogramDiscrete degreeDistribution = metrics.getDegreeDistribution();
      final int maxDegree = degreeDistribution.getMaxValue();
      final int minDegree = degreeDistribution.getMinValue();
      final double meanDegree = degreeDistribution.getMeanValue();
      final double medianDegree = degreeDistribution.getMedianValue();
      globalPrint.println("avg-degree,"+avgDegree);
      globalPrint.println("max-node-degree,"+maxDegree);
      globalPrint.println("mean-node-degree,"+meanDegree);
      globalPrint.println("median-node-degree,"+medianDegree);
      globalPrint.println("min-node-degree,"+minDegree);
    }

    
    if(calcAssort) {
      // if there are more values of a categorical than 25% of the nodes in the graph,
      // then do not calculate assortativity on that attribute
      int maxNodes = (int)((double)graph.numNodes()*0.25); 
      logger.info("Computing assortativity on all attributes - O(E)");
      for(String nodeType : graph.getNodeTypes()) {
        Attributes as = graph.getAttributes(nodeType);
        for(Attribute attr : as){
          if(attr.getType() != Type.CATEGORICAL || attr == as.getKey())
            continue;
          if(((AttributeCategorical)attr).size()>maxNodes)
            continue;
          
          Classification cls = new Classification(graph,nodeType,(AttributeCategorical)attr);
          EdgeType[] edgeTypes = graph.getEdgeTypes(nodeType);
          for(EdgeType edgeType : edgeTypes) {
            double[] eassort = GraphMetrics.calculateEdgeBasedAssortativityCoeff(cls,edgeType);
            double[] nassort = GraphMetrics.calculateNodeBasedAssortativityCoeff(cls,edgeType);
            globalPrint.println("unweighted-node-based-assortativity["+nodeType+","+edgeType.getName()+","+attr.getName()+"],"+nassort[1]);
            globalPrint.println("weighted-node-based-assortativity["+nodeType+","+edgeType.getName()+","+attr.getName()+"],"+nassort[1]);
            globalPrint.println("unweighted-edge-based-assortativity["+nodeType+","+edgeType.getName()+","+attr.getName()+"],"+eassort[1]);
            globalPrint.println("weighted-edge-based-assortativity["+nodeType+","+edgeType.getName()+","+attr.getName()+"],"+eassort[1]);
          }
          if(edgeTypes.length>1) {
            double[] eassort = GraphMetrics.calculateEdgeBasedAssortativityCoeff(cls);
            double[] nassort = GraphMetrics.calculateNodeBasedAssortativityCoeff(cls);
            globalPrint.println("unweighted-node-based-assortativity["+nodeType+","+attr.getName()+"],"+nassort[1]);
            globalPrint.println("weighted-node-based-assortativity["+nodeType+","+attr.getName()+"],"+nassort[1]);
            globalPrint.println("unweighted-edge-based-assortativity["+nodeType+","+attr.getName()+"],"+eassort[1]);
            globalPrint.println("weighted-edge-based-assortativity["+nodeType+","+attr.getName()+"],"+eassort[1]);
          }
        }
      }
    }
    
    if(saveComponentStem == null && metrics.getNumComponents()>1) {
      globalPrint.println();
      printSummaryHeader(graph,globalPrint,"Component");
      printComponentSummaries(graph,globalPrint);
    }
    
    if(cluster && saveClusterStem == null && metrics.getClusterer().getNumConnectedClusters()>1) {
      globalPrint.println();
      printSummaryHeader(graph,globalPrint,"Cluster");
      printNetKitClusterSummaries(graph,globalPrint);
    }
   
    if(globalPrint != NetKitEnv.systemOut && globalPrint != NetKitEnv.getStdOut())
      globalPrint.close();
  }
  
  public void printDegreeDistribution(final Graph graph) {
    if(!degreeStat)
      return;
    
    logger.info("Printing degree distribution..");
    final GraphMetrics metrics = graph.getMetrics();
    HistogramDiscrete degreeDistribution = metrics.getDegreeDistribution();
    final int maxDegree = degreeDistribution.getMaxValue();
    final int minDegree = degreeDistribution.getMinValue();
    
    PrintWriter degreePrint = NetKitEnv.getPrintWriter(getDegreeOutput());
    if(degreePrint == NetKitEnv.systemOut || degreePrint == NetKitEnv.getStdOut())
      degreePrint.println();
    degreePrint.println("Degree distribution:");   
    for(int i=minDegree;i<=maxDegree;i++)
      degreePrint.println(i+","+degreeDistribution.getCount(i));
    if(degreePrint != NetKitEnv.systemOut && degreePrint != NetKitEnv.getStdOut())
      degreePrint.close();
  }
  
  public void printNodeStatistics(final Graph graph) {
    if(getNodeOutput()==null)
      return;
    
    logger.info("Printing node statistics distribution..");

    final GraphMetrics metrics = graph.getMetrics();
    cluster(graph);

    PrintWriter nodePrint = NetKitEnv.getPrintWriter(getNodeOutput());
    if(nodePrint == NetKitEnv.systemOut || nodePrint == NetKitEnv.getStdOut())
      nodePrint.println();

    nodePrint.print("name,type,node-index");
    for(String name : NetkitGraph.getNodeStatNames(metrics)) {
      nodePrint.print(',');
      nodePrint.print(name);
    }
    nodePrint.println();

    for(Node node : graph.getNodes())
    {
      StringBuilder sb = new StringBuilder();
      sb.append(node.getName()).append(',');
      sb.append(node.getType()).append(',');
      sb.append(metrics.getNodeIndex(node)).append(',');
      sb.append(NetkitGraph.getNodeStatistics(metrics,node));
      nodePrint.println(sb.toString());
    }

    if(nodePrint != NetKitEnv.systemOut && nodePrint != NetKitEnv.getStdOut())
      nodePrint.close(); 
  }
  

  public void saveClusters(final Graph graph) {  
    if(saveClusterStem == null)
      throw new IllegalStateException("saveClusterStem is not set!");
    
    logger.info("Saving clusters using stem="+saveClusterStem);

    final GraphMetrics metrics = graph.getMetrics();
    
    if(saveNetkit || (!saveDot && !savePajek)) {
      final String arff = saveClusterStem+"-summary.arff";
      final String csv = saveClusterStem+"-summary.csv";
      printSchemaSummary(graph,arff,csv);
      PrintWriter pw = NetKitEnv.getPrintWriter(csv);
      printNetKitClusterSummaries(graph,pw);
      pw.close();
      
      int cnum=0;
      final ModularityClusterer mc = metrics.getClusterer();
      for(Set<Node> nodeSet : mc.getConnectedClusterNodeSets()) {
        final Graph subGraph = graph.subGraph(nodeSet);
        NetkitGraph.saveGraph(subGraph,saveClusterStem+"-"+cnum);
        cnum++;
      }

    }
    
    if(saveDot) {
      // TODO: make cluster summary
      // PrintWriter pw = NetKitEnv.getPrintWriter(saveClusterStem+"-summary.dot");
      // printDotClusterSummaries(graph,pw);
      // pw.close();
      
      int cnum=0;
      final ModularityClusterer mc = metrics.getClusterer();
      for(Set<Node> nodeSet : mc.getConnectedClusterNodeSets()) {
        final Graph subGraph = graph.subGraph(nodeSet);
        DotGraph.saveGraph(subGraph,saveClusterStem+"-"+cnum+".dot");
        cnum++;
      }
    }
    
    if(savePajek) {
      // TODO: make cluster summary
      // PrintWriter pw = NetKitEnv.getPrintWriter(saveClusterStem+"-summary.net");
      // printDotClusterSummaries(graph,pw);
      // pw.close();
      
      int cnum=0;
      final ModularityClusterer mc = metrics.getClusterer();
      for(Set<Node> nodeSet : mc.getConnectedClusterNodeSets()) {
        final Graph subGraph = graph.subGraph(nodeSet);
        PajekGraph.saveGraph(subGraph,saveClusterStem+"-"+cnum+".net", null, null, pajekLabel);
        cnum++;
      }
    }
  }
  
  public void saveComponents(final Graph graph) {
    if(saveComponentStem == null)
      throw new IllegalStateException("saveComponentStem is not set!");
    
    logger.info("Saving components using stem="+saveComponentStem);

    final GraphMetrics metrics = graph.getMetrics();
    final List<List<Node>> nodeSets = new ArrayList<List<Node>>();
    
    for(int cnum=0;cnum<metrics.getNumComponents();cnum++)
      nodeSets.add(new ArrayList<Node>(metrics.getComponentSize(cnum)));
    for(Node node : graph.getNodes())
      nodeSets.get(metrics.getComponent(node)).add(node);
    
    if(saveNetkit || (!saveDot && !savePajek)) {
      String arff = saveComponentStem+"-summary.arff";
      String csv = saveComponentStem+"-summary.csv";
      printSchemaSummary(graph,arff,csv);
      PrintWriter pw = NetKitEnv.getPrintWriter(csv);
      printComponentSummaries(graph,pw);
      pw.close();
    }
    if(saveDot) {
      // TODO: make component summary
      // PrintWriter pw = NetKitEnv.getPrintWriter(saveComponentStem+"-summary.dot");
      // printPajekComponentSummaries(graph,pw);
      // pw.close();
    }
      
    if(savePajek) {
      // TODO: make component summary
      // PrintWriter pw = NetKitEnv.getPrintWriter(saveComponentStem+"-summary.net");
      // printPajekComponentummaries(graph,pw);
      // pw.close();
    }
    
    int cnum=0;
    for(List<Node> nodeSet : nodeSets) {
      Graph subGraph = graph.subGraph(nodeSet);

      if(saveNetkit || (!saveDot && !savePajek))
        NetkitGraph.saveGraph(subGraph, saveComponentStem+"-"+cnum);
  
      if(saveDot)
        DotGraph.saveGraph(subGraph,saveComponentStem+"-"+cnum+".dot");
      
      if(savePajek)
        PajekGraph.saveGraph(subGraph,saveComponentStem+"-"+cnum+".net",null,null,pajekLabel);
      
      cnum++;
    }
  }
  
  public void saveGraph(final Graph graph) {
    if(saveGraphStem == null)
      throw new IllegalStateException("saveGraphStem is not set!");
    
    if(saveNetkit || (!saveDot && !savePajek)) {
      NetkitGraph.saveGraph(graph, saveGraphStem, saveAttributes, appendStatistics);
    }

    if(saveDot) {
      DotGraph.saveGraph(graph,saveGraphStem+".dot");
    }

    if(savePajek) {
      PajekGraph.saveGraph(graph,saveGraphStem+".net", pajekColor, null, pajekLabel);
    }
  }
   
  public void run(Graph graph) {
    logger.info("[GraphStat] running on graph with "+graph.numNodes()+" nodes and "+graph.numEdges()+" edges");
    if(pruneSingletons) {
      List<Node> keep = new ArrayList<Node>();
      for(Node node : graph.getNodes()) {
        if(node.getUnweightedDegree()==0)
          continue;
        keep.add(node);
      }
      graph = graph.subGraph(keep);
    }
    logger.info("[GraphStat] pruned to running on graph with "+graph.numNodes()+" nodes and "+graph.numEdges()+" edges");
   
    if(pajekColorStr != null)
      setPajekColor(graph);
    
    printGlobalInfo(graph);
    printDegreeDistribution(graph);
    printNodeStatistics(graph);
    
    if(saveClusterStem != null)
      saveClusters(graph);
    if(saveComponentStem != null)
      saveComponents(graph);
    if(saveGraphStem != null)
      saveGraph(graph);
}
  
  public boolean getPruneSingletons() { return pruneSingletons; }
  public void setPruneSingletons(final boolean p) {
    pruneSingletons = p;
    logger.info("Will"+(p?"":" NOT")+" prune singleton nodes away.");
  }

  public boolean getSaveAttributes() { return saveAttributes; }
  public void setSaveAttributes(final boolean sa) {
    saveAttributes = sa;
    logger.info("Will"+(sa?"":" NOT")+" save original node attributes where applicable.");
  }

  public boolean getAppendStatistics() { return appendStatistics; }
  public void setAppendStatistics(final boolean ss) {
    appendStatistics = ss;
    logger.info("Will"+(ss?"":" NOT")+" save node statistics where applicable.");
  }

  public boolean getSaveDot() { return saveDot; }
  public void setSaveDot(final boolean sd) {
    saveDot = sd;
    logger.info("Will"+(sd?"":" NOT")+" save relevant graph information in dot format [.dot].");
  }

  public boolean getSavePajek() { return savePajek; }
  public void setSavePajek(final boolean sp) {
    savePajek = sp;
    logger.info("Will"+(sp?"":" NOT")+" save relevant graph information in pajek format [.net].");
  }

  public boolean getSaveNetkit() { return saveNetkit; }
  public void setSaveNetkit(final boolean sn) {
    saveNetkit = sn;
    logger.info("Will"+(sn?"":" NOT")+" save relevant graph information in netkit format [.arff,.rn,.csv].");
  }
  
  public boolean getDoClustering() { return cluster; }
  public void setDoClustering(final boolean c) {
    cluster = c;
    logger.info("Will"+(c?"":" NOT")+" apply and save results of modularity clustering (community detection).");
  }
  
  public boolean getCalcAssort() { return calcAssort; }
  public void setCalcAssort(final boolean a) {
    calcAssort = a;
    logger.info("Will"+(a?"":" NOT")+" compute assortativity metrics.");
  }
  
  public boolean getDoCentralities() { return centrality; }
  public void setDoCentralities(final boolean c) {
    centrality = c;
    logger.info("Will"+(c?"":" NOT")+" compute centrality metrics.");
  }
  
  public boolean getDoAlphaCentralities() { return alphaCentrality; }
  public void setDoAlphaCentralities(final boolean c) {
    alphaCentrality = c;
    logger.info("Will"+(c?"":" NOT")+" compute approximate alpha centrality metrics.");
  }
  
  public boolean getDoPagerank() { return pagerank; }
  public void setDoPagerank(final boolean p) {
    pagerank = p;
    logger.info("Will"+(p?"":" NOT")+" compute approximate pagerank centrality metrics.");
  }
  
  public boolean getDoCoefficients() { return coefficients; }
  public void setDoCoefficients(final boolean c) {
    coefficients = c;
    logger.info("Will"+(c?"":" NOT")+" compute graph coefficients.");
  }
  
  public boolean getDoDegree() { return degreeStat; }
  public void setDoDegree(final boolean d) {
    degreeStat = d;
    logger.info("Will"+(d?"":" NOT")+" compute degree statistics.");
  }
  
  public String getSaveClusterStem() { return saveClusterStem; }
  public void setSaveClusterStem(final String stem) {
    saveClusterStem = stem;
    logger.info("Will save cluster information using filestem="+stem);
  }
  
  public String getSaveComponentStem() { return saveComponentStem; }
  public void setSaveComponentStem(final String stem) {
    saveComponentStem = stem;
    logger.info("Will save component information using filestem="+stem);
  }
  
  public String getSaveGraphStem() { return saveGraphStem; }
  public void setSaveGraphStem(final String stem) {
    saveGraphStem = stem;
    logger.info("Will save graph using filestem="+stem);
  }

  public String getOutput() { return output; }
  public void setOutput(final String file) {
    output = file;
    NetKitEnv.setStdOut(NetKitEnv.getPrintWriter(file));
    logger.info("Outputting to '"+file+"'");
  }

  public String getPajekLabel() { return pajekLabel; }
  public void setPajekLabel(final String pajekLabel) {
    this.pajekLabel = pajekLabel; 
    logger.info("Will label pajek nodes using attribute: "+((pajekLabel == null) ? "default" : pajekLabel)); 
    if(!getSavePajek()) setSavePajek(true);
  }

  public Classification getPajekColor() { return pajekColor; }
  private void setPajekColor(final Graph graph) {
    if(pajekColorStr == null)
      return;
    String nt = null;
    String attr = null;
    if(pajekColorStr.contains(":")) {
      final String[] tokens = pajekColorStr.split(":");
      nt = tokens[0];
      attr = tokens[1];
    } else {
      final String[] nts = graph.getNodeTypes();
      if(nts.length!=1)
        throw new IllegalArgumentException("PajekColor["+pajekColorStr+"] does not specify a node type and the graph has more than 1 nodetype!");
      nt = nts[0];
      attr = pajekColorStr;
    }
    Attributes attrs = graph.getAttributes(nt);
    if(attrs == null)
      throw new IllegalArgumentException("PajekColor["+nt+":"+attr+"] does not specify a valid node type.");
    Attribute na = attrs.getAttribute(attr);
    if(na == null)
      throw new IllegalArgumentException("PajekColor["+nt+":"+attr+"] node type does not contain that particular attribute.");
    if(na.getType() != Type.CATEGORICAL)
      throw new IllegalArgumentException("PajekColor["+nt+":"+attr+"] attribute is not a categorical.");
    pajekColor = new Classification(graph, nt, (AttributeCategorical)na);
  }
  public String getPajekColorStr() { return pajekColorStr; }
  public void setPajekColorStr(final String pajekColorStr) {
    this.pajekColorStr = pajekColorStr;
    if(pajekColorStr.contains(":")) {
      final String[] tokens = pajekColorStr.split(":");
      if(tokens.length!=2)
        throw new IllegalArgumentException("Invalid pajekColor string: '"+pajekColorStr+"'.  It must be in format [nt:]attr");
    }
    logger.info("Will color pajek nodes using attribute "+pajekColorStr); 
    if(!getSavePajek()) setSavePajek(true);
  }

  public String getNodeOutput() { return nodeOut; }
  public void setNodeOutput(final String file) {
    nodeOut = file;
    logger.info("Outputting nodes to '"+file+"'");
  }

  public String getDegreeOutput() { return degreeOut; }
  public void setDegreeOutput(final String file) {
    degreeOut = file;
    logger.info("Outputting degree histogram to '"+file+"'");
  }

  public String getGlobalOutput() { return globalOut; }
  public void setGlobalOutput(final String file) {
    globalOut = file;
    logger.info("Outputting global graph statistics to '"+file+"'");
  }
  

  private void setOptions(String[] argv) {
    if (argv.length == 0)
      usage(null);

    int idx = 0;
    if(argv[idx].equalsIgnoreCase(Netkit.graphstat))
      idx++;
    while (idx < argv.length && argv[idx].startsWith("-")) {
      String p = argv[idx].toLowerCase().substring(1);
      if (p.startsWith("h")) {
        usage(null);
      } else if (p.startsWith("l")) {
        final String filename = argv[++idx];
        NetKitEnv.setLogfile(filename);
        logger.info("Set log output to "+filename);
      } else if (p.startsWith("alpha")) {
        setDoAlphaCentralities(true);
      } else if (p.startsWith("c")) {
        setDoClustering(true);
      } else if (p.startsWith("noa")) {
        setCalcAssort(false);
      } else if (p.startsWith("noce")) {
        setDoCentralities(false);
      } else if (p.startsWith("noco")) {
        setDoCoefficients(false);
      } else if (p.startsWith("nod")) {
        setDoDegree(false);
      } else if (p.startsWith("nos")) {
        setAppendStatistics(false);
      } else if (p.startsWith("o")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setOutput(argv[idx]);
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.startsWith("pajekc")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setPajekColorStr(argv[idx]);
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.startsWith("pajekl")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        try {
          setPajekLabel(argv[idx]);
        } catch(Exception ex) {
          usage(ex.getMessage());
        }
      } else if (p.startsWith("page")) {
	setDoPagerank(true);
      } else if (p.startsWith("prune")) {
        setPruneSingletons(true);
      } else if (p.startsWith("savea")) {
        setSaveAttributes(true);
      } else if (p.startsWith("savecl")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setSaveClusterStem(argv[idx]);
        setDoClustering(true);
      } else if (p.startsWith("saveco")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setSaveComponentStem(argv[idx]);
      } else if (p.startsWith("savede")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setDegreeOutput(argv[idx]);
      } else if (p.startsWith("savedo")) {
        setSaveDot(true);
      } else if (p.startsWith("savegl")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setGlobalOutput(argv[idx]);
      } else if (p.startsWith("savegr")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setSaveGraphStem(argv[idx]);
      } else if (p.startsWith("savene")) {
        setSaveNetkit(true);
      } else if (p.startsWith("saveno")) {
        idx++;
        if(idx == argv.length)
          usage("No value specified for "+p);
        setNodeOutput(argv[idx]);
      } else if (p.startsWith("savepa")) {
        setSavePajek(true);
      } else {
        usage("Option "+p+" not recognized");
      }
      idx++;
    }
    if(idx+1 != argv.length)
      usage("Illegal number of arguments");
    schemaFile = argv[idx];
  }
  
  public static String[] getCommandLines() {
    String opt = Netkit.graphstat;
    return new String[]{"usage: Netkit "+opt+" [-h] [OPTIONS] <schema-file|pajek-net-file>"};
  }
  
  public static void usage(String msg) {
    for(String cmd : getCommandLines())
      System.out.println(cmd);
    
    if (msg == null) {
      System.out.println("Note that some of these computations are O(N^3) and this may take");
      System.out.println("a while on large graphs");
      System.out.println();
      System.out.println("OPTIONS");
      System.out.println("  -h                   This help screen");
      System.out.println("  -log <filename>      Where to send logging information.");
      System.out.println("                         In logging.properties:");
      System.out.println("                           handlers property must be set to java.util.logging.FileHandler.");
      System.out.println("                         default out: see (java.util.logging.FileHandler.pattern) in logging.properties");
      System.out.println("   <schema-file>       What schema file to read graph from");
      System.out.println();
      System.out.println("GRAPH STAT OPTIONS");
      System.out.println("  -cluster             Compute clusters using a community detection algorithm.");
      System.out.println("  -noassort            Do not compute assortativity statistics.");
      System.out.println("  -nocentrality        Do not compute centralities (as this is expensive).");
      System.out.println("  -alphacentrality     Compute approximate alpha centralities.");
      System.out.println("  -pagegrank           Compute approximate pagerank centralities.");
      System.out.println("  -nocoeff             Do not compute cluster coefficients (as this is expensive).");
      System.out.println("  -nodegree            Do not compute degree statistics.");
      System.out.println("  -pruneSingletons     Remove singleton nodes before doing any computations"); 
      System.out.println();
      System.out.println("OUTPUT OPTIONS");
      System.out.println("  -output (file|-)     Output graph statistics to the given file.");
      System.out.println("                         If you specify '-', then write to STDOUT (default)");
      System.out.println("                         This output is used for all output not specifically handled by the options:");
      System.out.println("                            -saveDegrees");
      System.out.println("                            -saveGlobal");
      System.out.println("                            -saveNodes");
      System.out.println("  -saveDot             Save graph output (-saveClusters, -saveComponents, -saveGraph options below) in dot format");
      System.out.println("  -saveNetkit          Save graph output (-saveClusters, -saveComponents, -saveGraph options below) in netkit format");
      System.out.println("  -savePajek           Save graph output (-saveClusters, -saveComponents, -saveGraph options below) in pajek format");
      System.out.println("  -pajekLabel <attr>   What is the name of the attribute to use as the label for the pajek nodes?");
      System.out.println("                          This will turn on -savePajek.");
      System.out.println("                          Default: node id.  If attribute is not found, the it will use the default.");
      System.out.println("  -pajekColor [nt:]<attr> What is the attribute to use to color pajek nodes?  'nt' is the nodetype.");
      System.out.println("                          This is required if the schema contains more than one nodetype.");
      System.out.println("                          This will turn on -savePajek.");
      System.out.println("                          Default: no coloring.  This will only color nodes of the given nodetype.");
      System.out.println("  -savePajek           Save graph output (-saveClusters, -saveComponents, -saveGraph options below) in pajek format");
      System.out.println("  -saveDegrees <file>  Save the degree distribution to the given file");
      System.out.println("                         format: <degree>,<num-nodes-with-degree>");
      System.out.println("                         Degree is not otherwise sent to the output.");
      System.out.println("  -saveGlobal (file|-) Output global graph statistics to the given file.");
      System.out.println("                         If you specify '-', then write to STDOUT (default)");
      System.out.println("  -saveAttributes      Add original attributes to graph stat attributes when outputting");
      System.out.println("                         node information (used by -saveNodes, -saveClusters, -saveComponents, -saveGraph)");
      System.out.println("  -noSaveStatistics    Do not add statistics when outputting node informat");
      System.out.println("                         (used by -saveNodes, -saveClusters, -saveComponents, -saveGraph)");
      System.out.println("  -saveNodes <file>    Save statistics of all nodes to the given file in a comma-separated file");
      System.out.println("                         format: <instanceID>,<nodeType>,[node-statistics such as centralitites, ...]");
      System.out.println("  -saveClusters <stem> Output graph with cluster information [this will turn on clustering (-cluster)]");
      System.out.println("                         if neither -saveDotty or -saveNetkit is used, then defaults to -saveNetkit");
      System.out.println("                         for dotty output: 'stem.dotty', complete graph where nodes are colored by cluster.");
      System.out.println("                         for netkit output:");
      System.out.println("                                cluster-K.arff");
      System.out.println("                                cluster-K-<nodetype>.csv");
      System.out.println("                                cluster-K-edgetype.rn");
      System.out.println("                         If this is not specified and clusters are turned on (-cluster), then saves cluster");
      System.out.println("                         summaries to 'global' output.");
      System.out.println("  -saveComponents <stem> Output connected components in separate .csv and .rn files.");
      System.out.println("                         if neither -saveDotty or -saveNetkit is used, then defaults to -saveNetkit");
      System.out.println("                         for dotty output: 'stem.dotty', complete graph where nodes are colored by component.");
      System.out.println("                         for netkit output:");
      System.out.println("                                component-K.arff");
      System.out.println("                                component-K-<nodetype>.csv");
      System.out.println("                                component-K-edgetype.rn");
      System.out.println("                         If this is not specified then saves component summaries to 'global' output.");
      System.out.println("  -saveGraph <stem>    Output graph in netkit format, with graph statistics as attributes.");
      System.out.println("                         Will output to files:");
      System.out.println("                                stem.arff");
      System.out.println("                                stem-<nodetype>.csv");
      System.out.println("                                stem-edgetype.rn");
    } else {
      System.out.println(msg);
    }
    System.exit(0);
  }
  
  public static void run(String[] argv) {
    GraphStat gs = new GraphStat(argv);
    try {
      Graph g = null;
      if(gs.schemaFile.toLowerCase().endsWith(".net"))
        g = PajekGraph.readGraph(new File(gs.schemaFile));
      else 
        g = NetkitGraph.readGraph(new File(gs.schemaFile));
      gs.run(g);
    } catch(OutOfMemoryError oom) {
      System.err.println("Out of memory!");
      System.err.println("You may want to increase available memory by adding '-XmxKKKKM' to your java command"); 
      System.err.println("where KKKK is the amount of memory you want to allocate (in megabytes)");
      System.err.println("   e.g.: java -Xmx1024M -jar NetKit.jar ...."); 
    }
  }
}
