import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.util.*;
import cern.colt.matrix.DoubleMatrix2D;

import java.io.File;
import java.util.*;

public class Wrapper {
  public static void main(String[] args)
  {
    for(String s : args)
    {
      File f = new File(s);
      System.out.println("=========================================");
      System.out.println("Reading graph from file "+s);
      Graph g = netkit.graph.io.SchemaReader.readSchema(f);
      GraphMetrics metrics = g.getMetrics();

      double numNodes = metrics.getNumNodes();
      int path2 = metrics.getNumPath2();
      int triangle = metrics.getNumtriangles();
      int singletons = metrics.getNumSingletons();
      double numEdges = metrics.getNumEdges();
      double numComponents = metrics.getNumComponents();
      double avgDegree = metrics.getMeanDegree();
      double clusterCoeff = metrics.getGlobalClusterCoeff();
      double localClusterCoeff = metrics.getLocalClusterCoeff();
      double graphCentrality = metrics.getGraphCentrality();
      double density = metrics.getGraphDensity();
      double path = metrics.getCharacteristicPathLength();
    
      System.out.println("# nodes: "+numNodes);
      System.out.println("# edges: "+numEdges);
      System.out.println("# components: "+numComponents);
      System.out.println("# path2: "+path2);
      System.out.println("# triangles: "+triangle);
      System.out.println("# singletons: "+singletons);
      System.out.println("avg degree: "+avgDegree);
      System.out.println("global cluster coeff: "+clusterCoeff);
      System.out.println("local cluster coeff: "+localClusterCoeff);
      System.out.println("graph centrality: "+graphCentrality);
      System.out.println("weighted graph centrality: "+metrics.getWeightedGraphCentrality());
      System.out.println("graph density: "+density);
      System.out.println("characteristic path length: "+path);
      System.out.println("weighted characteristic path length: "+metrics.getWeightedCharacteristicPathLength());
      System.out.println("=========================================");

      HistogramDiscrete degreeDistribution = metrics.getDegreeDistribution();
      
      System.out.println("Degree distribution:");
      
      int maxDegree = degreeDistribution.getMaxValue();
      int minDegree = degreeDistribution.getMinValue();
      double meanDegree = degreeDistribution.getMeanValue();
      double medianDegree = degreeDistribution.getMedianValue();
      System.out.println("Min: "+minDegree);
      System.out.println("Max: "+maxDegree);
      System.out.println("Median: "+medianDegree);
      System.out.println("Mean: "+meanDegree);
      for(int i=minDegree;i<=maxDegree;i++)
        System.out.println("  Degree("+i+"): "+degreeDistribution.getCount(i));
      
      System.out.println("=========================================");
      DoubleMatrix2D adjacency = metrics.getAdjacencyMatrix(true).coltMatrix;
      System.out.println("AdjacencyMatrix:");
      for(int row=0;row<adjacency.rows();row++)
      {
        for(int col=0;col<adjacency.columns();col++)
        {
          System.out.print(" "+(int)adjacency.getQuick(row,col));
        }
        System.out.println();
      }
      
      System.out.println("=========================================");
      ModularityClusterer mod = new ModularityClusterer(g);
      mod.startClustering();
      
      System.out.println("Clustering found "+mod.getNumConnectedClusters()+" clusters");
      int i=0;
      for(Set<Node> nodes : mod.getConnectedClusterNodeSets())
      {
        i++;
        System.out.print("Cluster-"+i+":");
        for(Node n : nodes)
        {
          System.out.print(" "+n.getName());
        }
        System.out.println();
      }

      System.out.println("=========================================");

      Node[] nodes = g.getNodes();
      System.out.println("Node, Betweenness, Closeness, graphCentrality, degreeCentrality");
      for(Node n : nodes)
      {
        double bc = metrics.getBetweennessCentrality(n);
        double cc = metrics.getClosenessCentrality(n);
        double gc = metrics.getGraphCentrality(n);
        double dc = n.getUnweightedDegree();
        
        System.out.print(n.getName());
        System.out.print(",");
        System.out.print(bc);
        System.out.print(",");
        System.out.print(cc);
        System.out.print(",");
        System.out.print(gc);
        System.out.print(",");
        System.out.print(dc);
        System.out.println();
      }

      System.out.println("=========================================");

      System.out.println("Node, WeightedBetweenness, WeightedCloseness, WeightedGraphCentrality, WeightedDegreeCentrality");
      for(Node n : nodes)
      {
        double bc = metrics.getWeightedBetweennessCentrality(n);
        double cc = metrics.getWeightedClosenessCentrality(n);
        double gc = metrics.getWeightedGraphCentrality(n);
        double dc = n.getWeightedDegree();
        
        System.out.print(n.getName());
        System.out.print(",");
        System.out.print(bc);
        System.out.print(",");
        System.out.print(cc);
        System.out.print(",");
        System.out.print(gc);
        System.out.print(",");
        System.out.print(dc);
        System.out.println();
      }
}
  }
}
