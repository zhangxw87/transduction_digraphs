package netkit.classifiers.active.graphfunctions;

import java.util.Comparator;

import netkit.classifiers.active.PickLabelStrategy.LabelNode;
import netkit.classifiers.active.GraphCentralityLabeling;
import netkit.classifiers.Classification;
import netkit.util.GraphMetrics;
import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public abstract class ScoringFunction implements Comparator<LabelNode> {
  protected GraphCentralityLabeling labeler;
  protected GraphMetrics gm;
  protected Classification labels;
  
  public void initialize(GraphCentralityLabeling graphLabeler)
  {
    labeler = graphLabeler;
    gm = graphLabeler.getMetrics();
    labels = graphLabeler.getLabels();
  }
  
  public abstract String toString();
  
  /**
   * @ return the 'score' of the given node.
   */
  public abstract double score(Cluster c, Node n);
  
  /**
   * Return the new score of a node given its old score and a newly labeled node.
   * By default a scoring function is not updateable and will just return the
   * current score.
   * @return the current score.
   */
  public double update(Cluster c, double currentScore, Node n, Node[] newPicks) { return currentScore; }
  /**
   * returns whether the score of a node will change if more nodes are labeled.
   * The default is that a scoring function is not updateable.
   * @return false.
   */
  public boolean updateable() { return false; }
  
  /**
   * Is this scoring function cluster based (does it need clustering).
   * @return false (default)
   */
  public boolean clusterBased(){ return false; }
  
  /**
   * Standard comparator function.  Calls the compare on doubles. 
   */
  public final int compare(LabelNode n1, LabelNode n2) {
    return compare(n1.score,n2.score);
  }

  /**
   * Standard comparator function.  Return reverse natural order by default to ensure
   * that the 'best' score (=smallest) is at the end of the list (first to be picked). 
   */
  public int compare(double d1, double d2) {
    return Double.compare(d2,d1);
  }

  /**
   * What is the best score (first to be picked)
   */
  public double bestScore() { return Double.NEGATIVE_INFINITY; }

  /**
   * What is the best score (last to be picked)
   */
  public double worstScore() { return Double.POSITIVE_INFINITY; }
}
