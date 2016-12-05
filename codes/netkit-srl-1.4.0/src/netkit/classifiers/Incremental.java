package netkit.classifiers;

import netkit.graph.Node;

public interface Incremental {
  /**
   * After the model has been induced, use this method to add
   * a new training label.
   * 
   * @param node The node whose label is added
   * @param label The label of the node
   */
  public void addTrainingLabel(Node node, int label);
  
}
