package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class WeightedCloseness extends ScoringFunction {
  @Override
  public String toString() { return "weightedCloseness"; }

  @Override
  public double score(Cluster c, Node n) { return gm.getWeightedClosenessCentrality(n); }
}
