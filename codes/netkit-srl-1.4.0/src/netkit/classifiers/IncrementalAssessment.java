package netkit.classifiers;

import netkit.graph.Node;

public interface IncrementalAssessment {
  /**
   * What is the empirical risk if this node is labeled (after the
   * initial model has been induced)?
   *  
   * @param node
   * @return the risk
   */
  public double getEmpiricalRisk(Node node);

  /**
   * What would be the new accuracy if this node is labeled (after the
   * initial model has been induced)?
   *  
   * @param node
   * @return the risk
   */
  public double getIncrementalAccuracy(Node n, Classification truth);
}
