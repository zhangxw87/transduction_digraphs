package netkit.graph.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import netkit.graph.Attribute;
import netkit.graph.Attributes;
import netkit.graph.Edge;
import netkit.graph.EdgeType;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.util.GraphMetrics;
import netkit.util.ApproximateCentralities;
import netkit.util.ModularityClusterer;
import netkit.util.NetKitEnv;

public class NetkitGraph {
  public static String[] getNodeStatNames(final GraphMetrics metrics) {
    List<String> statNames = new ArrayList<String>();
    if(metrics.calcCentralityProgress()==1.0D) {
      statNames.add("Betweenness");
      statNames.add("WeightedBetweenness");
      statNames.add("Closeness");
      statNames.add("WeightedCloseness");
      statNames.add("GraphCentrality");
      statNames.add("WeightedGraphCentrality");
    }

    final ApproximateCentralities ac = metrics.getApproximateCentralities();
    for(ApproximateCentralities.ApproximateCentrality a : ac.getCentralities()) {
	if(a.progress()>0)
	    statNames.add(a.name());
    }

    statNames.add("DegreeCentrality");
    statNames.add("WeightedDegreeCentrality");
    statNames.add("ComponentNumber");

    final ModularityClusterer mc = metrics.getClusterer();
    if(mc.percentDone()==1.0D)
      statNames.add("ClusterNumber");
    return statNames.toArray(new String[0]);
  }

  private static void saveSchema(final Graph graph, final String prefix, final boolean saveAttributes, final boolean appendStatistics) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(prefix+".arff");
    for(final String nodeType : graph.getNodeTypes()) {
      pw.println("@nodetype "+nodeType);
      if(saveAttributes) {
        final Attributes as = graph.getAttributes(nodeType);
        for(final Attribute attrib : as){
          pw.print("@attribute ");
          pw.print(attrib.getName());
          pw.print(" ");
          if(as.getKey() == attrib)
            pw.println("KEY");
          else
            pw.println(attrib.getType());
        }
      } else {
        pw.println("@attribute id KEY");
      }
      if(appendStatistics) {
        for(String val : getNodeStatNames(graph.getMetrics())) {
          pw.print("@attribute ");
          pw.print(val);
          if(val.equals("ClusterNumber") || val.equals("ComponentNumber"))
            pw.println(" CATEGORICAL");
          else
            pw.println(" CONTINUOUS");
        }
      }
      pw.print("@nodedata ");
      pw.print(prefix);
      pw.print('-');
      pw.print(nodeType.toLowerCase());
      pw.println(".csv");
      pw.println();
    }

    for(final EdgeType edgeType : graph.getEdgeTypes()) {
      pw.println("@edgetype "+edgeType.getName()+" "+edgeType.getSourceType()+" "+edgeType.getDestType());
      pw.print("@edgedata ");
      pw.print(prefix);
      pw.print('-');
      pw.print(edgeType.getName().toLowerCase());
      pw.println(".rn");
      pw.println();
    }

    pw.close();
  }
  
  public static String getNodeStatistics(final GraphMetrics metrics, final Node node) {
    StringBuilder sb = new StringBuilder();
    if(metrics.calcCentralityProgress()==1.0D) {
      sb.append(metrics.getBetweennessCentrality(node)).append(',');
      sb.append(metrics.getWeightedBetweennessCentrality(node)).append(',');
      sb.append(metrics.getClosenessCentrality(node)).append(',');
      sb.append(metrics.getWeightedClosenessCentrality(node)).append(',');
      sb.append(metrics.getGraphCentrality(node)).append(',');
      sb.append(metrics.getWeightedGraphCentrality(node)).append(',');
    }

    final ApproximateCentralities ac = metrics.getApproximateCentralities();
    for(ApproximateCentralities.ApproximateCentrality a : ac.getCentralities()) {
	if(a.progress()>0)
	    sb.append(a.getCentrality(node)).append(',');
    }

    sb.append(node.getUnweightedDegree()).append(',');
    sb.append(node.getWeightedDegree()).append(',');
    sb.append(metrics.getComponent(node));
    
    final ModularityClusterer mc = metrics.getClusterer();
    if(mc.percentDone()==1.0D)
      sb.append(',').append(metrics.getCluster(node));
    return sb.toString();
  }
  


  private static void printNode(final GraphMetrics metrics, final PrintWriter pw, final Node node, final boolean saveAttributes, final boolean appendStatistics) {
    final StringBuilder sb = new StringBuilder();
    final Attributes as = node.getAttributes();
    if(saveAttributes) {
      sb.append(as.getAttribute(0).formatForOutput(node.getValue(0)));
      for(int i=1;i<as.attributeCount();i++)
        sb.append(',').append(as.getAttribute(i).formatForOutput(node.getValue(i)));
    } else {
      final int i = as.getKeyIndex();
      sb.append(as.getAttribute(i).formatForOutput(node.getValue(i)));
    }
    if(appendStatistics)
      sb.append(',').append(getNodeStatistics(metrics,node));
    pw.println(sb.toString());
  }
  

  public static void printNetKitNodes(final Graph graph, final PrintWriter pw, final String nodeType) {
    printNetKitNodes(graph, pw, nodeType, true, false);
  }
  
  public static void printNetKitNodes(final Graph graph, final PrintWriter pw, final String nodeType, final boolean saveAttributes, final boolean appendStatistics) {
    final GraphMetrics metrics = graph.getMetrics();
    for(final Node node : graph.getNodes(nodeType))
      printNode(metrics,pw,node,saveAttributes,appendStatistics);
  }
  
  public static void saveNetKitNodes(final Graph graph, final String prefix, final String nodeType) {
    saveNetKitNodes(graph,prefix,nodeType,true,false);
  }

  public static void saveNetKitNodes(final Graph graph, final String prefix, final String nodeType, final boolean saveAttributes, final boolean appendStatistics) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(prefix+"-"+nodeType.toLowerCase()+".csv");
    printNetKitNodes(graph,pw,nodeType,saveAttributes,appendStatistics);
    pw.close();
  }
  
  public static void printNetKitEdges(final Graph graph, final PrintWriter pw, final String edgeName) {
    for(final Edge edge : graph.getEdges(edgeName)) {
      pw.println(edge.getSource().getName()+","+edge.getDest().getName()+","+edge.getWeight());
      edge.toString();
    }
  }

  public static void saveNetKitEdges(final Graph graph, final String prefix, final String edgeName) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(prefix+"-"+edgeName.toLowerCase()+".rn");
    printNetKitEdges(graph,pw,edgeName);
    pw.close(); 
  }

  public static void saveGraph(final Graph graph, final String outputStem, final boolean saveAttributes, final boolean appendStatistics) {
    saveSchema(graph,outputStem, saveAttributes, appendStatistics);
    for(final String nodeType : graph.getNodeTypes())
      saveNetKitNodes(graph,outputStem,nodeType,saveAttributes,appendStatistics);
    for(final String edgeName : graph.getEdgeTypeNames())
      saveNetKitEdges(graph,outputStem,edgeName);
  }
  
  public static void saveGraph(final Graph graph, final String outputStem) {
    saveGraph(graph, outputStem, true, false);
  }
  
  public static Graph readGraph(final File schemafile) {
    return SchemaReader.readSchema(schemafile);
  }
}
