package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.classifiers.active.GraphCentralityLabeling;
import netkit.classifiers.relational.Harmonic;
import netkit.util.ModularityClusterer.Cluster;

public class ERMRank extends ScoringFunction {
  private Harmonic harmonic;

  @Override
  public String toString() { return "ERMRank"; }

  @Override
  public void initialize(GraphCentralityLabeling labeler) {
    super.initialize(labeler);
    harmonic = labeler.getHarmonicFunction();
    if(harmonic==null)
      throw new IllegalArgumentException("You cannot use ERMRank without using the Harmonic Function!");
  }

  @Override
  public double score(Cluster c, Node n) {
    return harmonic.getERM(n);
  }
  @Override
  public double update(Cluster c, double currentScore, Node n, Node[] newPicks) {    
    return score(c,n);
  }
  @Override
  public boolean updateable() { return true; }
}
