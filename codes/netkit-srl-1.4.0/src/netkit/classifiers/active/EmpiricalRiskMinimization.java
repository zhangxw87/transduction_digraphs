/**
 * EmpiricalRiskMinimization.java
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
import netkit.graph.Node;
import netkit.util.Configuration;
import netkit.util.VectorMath;

import java.util.*;

public class EmpiricalRiskMinimization extends PickLabelStrategyImp {
    
  @Override
  public Configuration getDefaultConfiguration() {
    Configuration config = super.getDefaultConfiguration();
    return config;
  }

  @Override
  public void configure(Configuration config) {
    super.configure(config);
  }

  /**
   * Compute the empirical risk for a specific set of predictions using
   * the standard empirical risk formulation:
   * <blockquote>
   * <code>risk(predictions) = sum over x in testset: argmin_i [ 1-f(x,i) ],</code>
   * </blockquote>
   * where <code>f(x,i)</code> is the probability that <code>x</code> belongs to class <code>i</code>.  
   * 
   * @param predictions
   * @return the risk of these computations
   */
  public static double computeEmpiricalRisk(Estimate predictions) {
    double risk = 0;
    for(Node n : predictions)
    {
      double[] p = predictions.getEstimate(n); 
      double max = VectorMath.getMaxValue(p);
      for(double v : p)
      {
        risk += (1.0D - Math.max(max,(1-v)));
      }
    }
    return risk;
  }

  /**
   * Get the next nodes to label based on the empirical risk minimization principle.
   */
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    DataSplit split = getSplit();
    if(split == null)
      throw new IllegalStateException(getName()+" has not yet been initialized!");

    NetworkLearner nl = getNetworkLearner();
    
    int attributeIndex = split.getView().getAttributeIndex();
    
    Node[] candidateNodes = split.getTestSet();
    Node[] trainset = new Node[split.getTrainSetSize()+1];
    Node[] testset = new Node[split.getTestSetSize()-1];
    
    System.arraycopy(split.getTrainSet(), 0, trainset, 1, split.getTrainSetSize());
    System.arraycopy(candidateNodes, 1, testset, 0, testset.length);
    
    List<LabelNode> nodes = new ArrayList<LabelNode>(predictions.size());
    
    Node trainNode = null;
    java.util.logging.Level lvl = nl.logger.getLevel();
    if(!logger.isLoggable(java.util.logging.Level.FINEST))
      nl.logger.setLevel(java.util.logging.Level.OFF);
    logger.info(getName()+": picking top "+maxPicks+" nodes from "+candidateNodes.length+" candidates, over "+split.getClassDistribution().length+" classes");
    for(int i=0;i<candidateNodes.length;i++)
    {
      if(trainNode != null)
        testset[i-1] = trainNode;
      trainNode = candidateNodes[i];
      
      double[] pred = predictions.getEstimate(trainNode);
      if(pred==null)
        continue;
      
      trainset[0] = trainNode;

      DataSplit ermsplit = new DataSplit(split.getView(), testset, trainset);
      
      double val = trainNode.getValue(attributeIndex);
      double risk = 0;
      for(int p=0;p<pred.length;p++)
      {
        trainNode.setValue(attributeIndex, p);
        Estimate e = nl.runInference(ermsplit);
        risk += pred[p] * computeEmpiricalRisk(e);
      }
      trainNode.setValue(attributeIndex, val);
      
      nodes.add(new LabelNode(trainNode,risk));
      System.out.println("erm adding node="+trainNode.getName()+" risk="+risk);

    }
    nl.logger.setLevel(lvl);

    Collections.sort(nodes);
    logger.fine(getName()+" Risks in range "+nodes.get(0).score+" to "+nodes.get(nodes.size()-1).score);
    logger.info(getName()+" Top picked node="+nodes.get(0).node.getName()+":"+nodes.get(0).score);
    if(nodes.size()>maxPicks)
      nodes = nodes.subList(0, maxPicks);
    
    return nodes.toArray(new LabelNode[0]);
  }

  @Override
  public String getDescription() {    
    return "";
  }

  @Override
  public String getName() {
    return "EmpiricalRiskMinimization";
  }

  @Override
  public String getShortName() {
    return "EmpiricalRiskMinimization";
  }
}
