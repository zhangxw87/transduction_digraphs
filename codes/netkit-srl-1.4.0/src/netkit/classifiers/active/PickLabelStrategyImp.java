/**
 * PickLabelStrategyImp.java
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

import java.util.Collections;
import java.util.logging.Logger;

import netkit.classifiers.*;
import netkit.graph.Node;
import netkit.util.Configuration;
import netkit.util.NetKitEnv;

public abstract class PickLabelStrategyImp implements PickLabelStrategy {
  protected final Logger logger = NetKitEnv.getLogger(this);
 
  protected int iteration = 0;
  private NetworkLearner nl = null;
  private DataSplit split = null;
    
  /**
   * Get the list of nodes to get labels for.
   * 
   * @param currentPredictions Current predictions of the classifier
   * @param maxPicks how many nodes should it pick at maximum (this iteration)
   * 
   * @return An array of Node objects that should receive labels.  <code>null</code> is returned if done.
   */
  protected abstract LabelNode[] pickNodes(Estimate predictions, int maxPicks); 
  
  @Override
  public void configure(Configuration config) {}

  @Override
  public Configuration getDefaultConfiguration() {
    return new Configuration();
  }

  @Override
  public void initialize(NetworkLearner nl, DataSplit split) {
    this.nl = nl;
    this.split = split;
    iteration = 0;
  }
  
  public final DataSplit getSplit() {
    return split;
  }
  
  public final int getIterationNum() {
    return iteration;
  }
  
  public final NetworkLearner getNetworkLearner() {
    return nl;
  }
  
  @Override
  public LabelNode[] peek(DataSplit currentSplit, Estimate currentPredictions, int numPeek)
  {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public double getRank(DataSplit currentSplit, Estimate currentPredictions, Node node)
  {
    throw new UnsupportedOperationException();
  }

  protected double getAverageRank(java.util.List<? extends LabelNode> list, LabelNode target) {
    if(target == null)
      return Double.NaN;
    Collections.sort(list);
    return getAverageRank(list, list.indexOf(target));
  }

  protected double getAverageRank(java.util.List<? extends LabelNode> sortedlist, int targetIndex) {
    if(targetIndex == -1)
      return Double.NaN;
    LabelNode target = sortedlist.get(targetIndex);
    double score = target.score;
    int sIdx = sortedlist.indexOf(target);
    int eIdx = sIdx;
    while(sIdx>0&&sortedlist.get(sIdx-1).score==score)
      sIdx--;
    while(eIdx<sortedlist.size()-1&&sortedlist.get(eIdx+1).score==score)
      eIdx++;
    return 1+((double)(eIdx-sIdx)/2.0D);
  }
  
  /**
   * Get the list of nodes to get labels for.
   * @param currentSplit Current predictions of the classifier
   * @param currentPredictions Current predictions of the classifier
   * @param maxPicks how many nodes should it pick at maximum (this iteration)
   * @return An array of Node objects that should receive labels.  <code>null</code> is returned if done.
   */
  @Override
 public final LabelNode[] getNodesToLabel(DataSplit currentSplit, Estimate currentPredictions, int maxPicks) {
    this.split = currentSplit;
    LabelNode[] nodes = pickNodes(currentPredictions, maxPicks);
    iteration++;
    LabelNode[] result = nodes;
    if(nodes != null && nodes.length > maxPicks)
    {
      result = new LabelNode[Math.min(nodes.length, maxPicks)];
      for(int i=0;i<result.length;i++)
        result[i] = nodes[i];
    }
    if(result != null && result.length>0)
    {
      logger.fine("[iteration-"+iteration+"] getNodesToLabel returning "+result.length+" nodes.  MaxNode="+nodes[0].node.getName()+":"+nodes[0].score);
      return result;
    }
    logger.fine("[iteration-"+iteration+"] getNodesToLabel returning null");
    return null;
  }
  
  public String toString() { return getName(); }
}
