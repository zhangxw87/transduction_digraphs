/**
 * GreedyTruth.java
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

import netkit.classifiers.*;
import netkit.classifiers.relational.*;
import netkit.graph.Node;
import netkit.util.Configuration;

import java.util.*;

/**
 * Picks next labels by getting the best jump in accuracy, knowing what the truth is.  This
 * is used as the gold standard that noone should be able to beat.  Right now, this assumes
 * that we are using the harmonic function as the relational learner, because we can relatively
 * efficiently compute the true accuracies.
 * 
 * @author sofmac
 *
 */
public class GreedyTruth extends PickLabelStrategyImp {
    
  private Harmonic harmonic = null;
  private Classification truth = null;
  
  @Override
  public Configuration getDefaultConfiguration() {
    Configuration config = super.getDefaultConfiguration();
    return config;
  }

  @Override
  public void configure(Configuration config) {
    super.configure(config);
  }

  @Override
  public LabelNode[] peek(DataSplit split, Estimate predictions, int maxPicks) { 
    return pickNodes(predictions,maxPicks);
  }
  
  @Override
  public double getRank(DataSplit split, Estimate predictions, Node node) { 
    List<LabelNode> result = new ArrayList<LabelNode>(predictions.size());
    LabelNode target = null;
    for(Node n : predictions)
    {
      double[] riskAcc = harmonic.getERM(n, truth);
      if(Double.isNaN(riskAcc[1]))
        continue;
      LabelNode lbl = new LabelNode(n,1.0D-riskAcc[1]);
      if(n==node)
        target = lbl;
      result.add(lbl);
    }
    return getAverageRank(result, target);
  }
  
  /**
   * Get the next nodes to label based on the empirical risk minimization principle.
   */
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {    
    List<LabelNode> result = new ArrayList<LabelNode>(predictions.size());
   
    logger.info(getName()+": picking top "+maxPicks+" nodes from "+predictions.size()+" candidates, over "+getSplit().getClassDistribution().length+" classes");
     
    for(Node n : predictions)
    {
      double[] riskAcc = harmonic.getERM(n, truth);
      if(Double.isNaN(riskAcc[1]))
        continue;
      result.add(new LabelNode(n,1.0D-riskAcc[1]));
    }
    
    Collections.sort(result);
    if(result.size()>0)
    {
      logger.fine(getName()+" Estimated errors in range "+result.get(0).score+" to "+result.get(result.size()-1).score);
      logger.info(getName()+" Top picked node="+result.get(0).node.getName()+":"+result.get(0).score);
    }
    if(result.size()>maxPicks)
      result = result.subList(0, maxPicks);
    
    int iteration = this.getIterationNum();
    if(result.size()>0)
    {
      logger.fine("[iteration-"+iteration+"] getNodesToLabel returning "+result.size()+" nodes.  MaxNode="+result.get(0).node.getName()+":"+result.get(0).score);
      return result.toArray(new LabelNode[0]);
    }
    logger.fine("[iteration-"+iteration+"] getNodesToLabel returning null");
    return null;
  }  

  @Override
  public void initialize(NetworkLearner nl, DataSplit split) {
    super.initialize(nl, split);
    NetworkClassifier nc = nl.getNetworkClassifier();
    truth = split.getView().getTruth();
    if(truth == null)
      throw new IllegalArgumentException("Truth is not known!");
    if(!(nc instanceof Harmonic))
      throw new IllegalArgumentException("NetworkClassifier must be the Harmonic function!");
    harmonic = (Harmonic)nc;
  }

  @Override
  public String getDescription() {    
    return "";
  }

  @Override
  public String getName() {
    return "GreedyTruth";
  }

  @Override
  public String getShortName() {
    return "GreedyTruth";
  }
}
