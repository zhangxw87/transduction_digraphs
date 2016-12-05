package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

/**
 * For label closeness, we want to pick the largest closeness first, so that
 * means we reverse normal sorting order
 */
public class LabelWeightedClosenessRank extends ReverseScoringFunction {
  @Override
  public boolean clusterBased() { return true; }
  
  @Override
  public String toString() { return "labelWeightedClosenessRank"; }

  @Override
  public double score(Cluster c, Node n) {
    double dist = 0;
    for(Node t : labels)
      dist += gm.getWeightedDist(n,t); 
    return dist/(double)labels.size();
  }
  @Override
  public double update(Cluster c, double currentScore, Node n, Node[] newPicks) {    
    currentScore *= (double)(labels.size()-newPicks.length);
    for(Node newPick : newPicks)
      currentScore += gm.getWeightedDist(n,newPick);
    return currentScore/(double)labels.size();
  }
  @Override
  public boolean updateable() { return true; }
}
