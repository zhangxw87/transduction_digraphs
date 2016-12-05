package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class Betweenness extends ReverseScoringFunction {
  @Override
  public String toString() { return "betweenness"; }
  
  @Override
  public double score(Cluster c, Node n) { return gm.getWeightedBetweennessCentrality(n); }
}
