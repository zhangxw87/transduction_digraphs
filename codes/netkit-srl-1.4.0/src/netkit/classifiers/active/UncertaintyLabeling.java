/**
 * UncertaintyLabeling.java
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

import java.util.*;

public class UncertaintyLabeling extends PickLabelStrategyImp {
  
  private double minThreshold = 0.2;

  @Override
  public Configuration getDefaultConfiguration() {
    Configuration config = super.getDefaultConfiguration();
    config.set("minthreshold", 0.2);
    return config;
  }
  
  @Override
  public void configure(Configuration config) {
    super.configure(config);
    minThreshold = config.getDouble("minthreshold",0.2);
    logger.config(getName()+" configuration: minThreshold="+minThreshold);
  }

  @Override
  public double getRank(DataSplit split, Estimate predictions, Node node) {
    List<LabelNode> result = new ArrayList<LabelNode>(predictions.size());
    LabelNode target = null;
    for(final Node n : predictions)
    {
      final double[] pred = predictions.getEstimate(n);   
      Arrays.sort(pred);
      final double diff = pred[pred.length-1]-pred[pred.length-2];
      if(diff > minThreshold)
        continue;
      LabelNode lbl = new LabelNode(n,diff);
      if(n==node)
        target = lbl;
      result.add(lbl);
    }
    return getAverageRank(result,target);
  }

  @Override
  public LabelNode[] peek(DataSplit split, Estimate predictions, int maxPicks) {
    return pickNodes(predictions, maxPicks);
  }

  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    List<LabelNode> nodes = new ArrayList<LabelNode>(predictions.size());
    for(final Node n : predictions)
    {
      final double[] pred = predictions.getEstimate(n);   
      Arrays.sort(pred);
      final double diff = pred[pred.length-1]-pred[pred.length-2];
      if(diff > minThreshold)
        continue;
      nodes.add(new LabelNode(n,diff));      
    }
    
    Collections.sort(nodes);
    if(nodes.size()>maxPicks)
      nodes = nodes.subList(0, maxPicks);
    
    logger.fine(getName()+" pickNodes() returning "+nodes.size()+" picks (max="+maxPicks+" out of "+predictions.size()+")");
    return nodes.toArray(new LabelNode[0]);
  }

  @Override
  public String getDescription() {    
    return "The UncertaintyLabeling active learning strategy picks "+
           "the nodes whose predictions are the most uncertain (i.e., "+
           "the difference in scores between the top two predictions of" +
           "the class label).";
  }

  @Override
  public String getName() {
    return "UncertaintyLabeling";
  }

  @Override
  public String getShortName() {
    return "UncertaintyLabeling";
  }

}
