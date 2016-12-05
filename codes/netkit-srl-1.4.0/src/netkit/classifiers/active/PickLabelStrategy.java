/**
 * PickLabelStrategy.java
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package netkit.classifiers.active;

import netkit.classifiers.DataSplit;
import netkit.classifiers.Estimate;
import netkit.graph.Node;
import netkit.util.Configurable;
import netkit.classifiers.NetworkLearner;

public interface PickLabelStrategy extends Configurable {
  public class LabelNode implements Comparable<LabelNode> {
    public final Node node;
    public double score;
    public LabelNode(final Node node, final double score) {
      this.node = node;
      this.score = score;
    }
    
    @Override
    public int compareTo(LabelNode ln) {
      return Double.compare(score, ln.score);
    }
    
    public String toString() {
      return node.getName()+":"+score;
    }
  }
  
  /**
   * Get the list of nodes to get labels for... without changing the internal state of the
   * active labeler.
   * @param currentSplit Current datasplit
   * @param currentPredictions Current predictions of the classifier
   * @param numPicks how many nodes should it peek at
   * @return An array of Node objects that should receive labels.  <code>null</code> is returned if done.
   * @throws UnsupportedOperationException if this is not supported
   */
  public LabelNode[] peek(DataSplit currentSplit, Estimate currentPredictions, int numPeek);
  
  /**
   * Get the rank of the given node if the strategy were to pick the node.
   * If more than one node has the same score, then average their ranks (hence a double is returned)
   * @param currentSplit Current datasplit
   * @param currentPredictions Current predictions of the classifier
   * @param node the node whose rank is requested
   * @return the rank of the given node or Double.NaN if the node is not in the rankings.  If more than one node has the same score, then average their ranks (hence a double is returned)
   * @throws UnsupportedOperationException if this is not supported
   */
  public double getRank(DataSplit currentSplit, Estimate currentPredictions, Node node);

  /**
   * Get the list of nodes to get labels for.
   * @param currentSplit Current datasplit
   * @param currentPredictions Current predictions of the classifier
   * @param maxPicks how many nodes should it pick at maximum (this iteration)
   * @return An array of Node objects that should receive labels.  <code>null</code> is returned if done.
   */
  public LabelNode[] getNodesToLabel(DataSplit currentSplit, Estimate currentPredictions, int maxPicks);

  /**
   * Initialize the label strategy by providing a reference to the NetworkLeaner
   * object that calls the strategy, thereby giving it access to all information
   * it is likely to need.   This should re-initialize the strategy if
   * it has any cached values from prior calls.
   * @param nl The NetworkLearner object that will be calling this strategy.
   * @param split The initial train/test split that this will be used on 
   */
  public void initialize(NetworkLearner nl, DataSplit split);
  
  public String getShortName();
  public String getName();
  public String getDescription();
}
