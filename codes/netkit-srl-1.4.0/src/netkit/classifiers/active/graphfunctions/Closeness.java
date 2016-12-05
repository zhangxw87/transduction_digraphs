package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class Closeness extends ScoringFunction {
  @Override
  public String toString() { return "closeness"; }

  @Override
  public double score(Cluster c, Node n) { return gm.getClosenessCentrality(n); }
}
