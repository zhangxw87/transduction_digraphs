package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class WeightedBetweenness extends ReverseScoringFunction {
  @Override
  public String toString() { return "weightedBetweenness"; }
  
  @Override
  public double score(Cluster c, Node n) { return gm.getBetweennessCentrality(n); }
}
