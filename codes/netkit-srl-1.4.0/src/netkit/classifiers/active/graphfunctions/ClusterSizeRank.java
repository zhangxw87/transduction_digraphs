package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class ClusterSizeRank extends WeightedBetweenness {
  @Override
  public boolean clusterBased() { return true; }
  
  @Override
  public String toString() { return "clusterSizeRank"; }

  @Override
  public double score(Cluster c, Node n)
  { 
    return c.getSize()+super.score(c,n);
  }
}
