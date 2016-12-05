/**
 * BayesCategoricalEdgeCreator.java
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
 */package netkit.graph.edgecreator;

import java.util.Arrays;

import netkit.graph.*;
import netkit.classifiers.DataSplit;
import netkit.util.VectorMath;

public class BayesCategoricalEdgeCreator extends EdgeCreatorImp {
  private double[][] prob = null;
  private double[] prior = null;

  @Override
  public String getName() {
    return "BayesCategoricalEdgeCreator";
  }

  @Override
  public boolean canHandle(Attribute attribute) {
    return (attribute.getType() == Type.CATEGORICAL);
  }

  @Override
  public double getWeight(Node src, Node dest) {
    int attributeIndex = getAttributeIndex();
    if(attributeIndex == -1)
      throw new IllegalStateException("EdgeCreator["+getName()+"] has not yet been initialized!");

    if(src.isMissing(attributeIndex) || dest.isMissing(attributeIndex))
      return Double.NaN;
    
    double val = 0;
    for(int i=0;i<prior.length;i++)
    {
      double prob1 = prob[(int)src.getValue(attributeIndex)][i];
      double prob2 = prob[(int)dest.getValue(attributeIndex)][i];
      val += prior[i]*prior[i]*prob1*prob2;
    }
    
    return val;
  }

  private void computeStatistics(final DataSplit split) {
    Attributes as = split.getView().getGraph().getAttributes(nodeType);
    int idx = split.getView().getAttributeIndex();
    int attributeIndex = getAttributeIndex();

    AttributeCategorical acls = (AttributeCategorical)as.getAttribute(idx);
    AttributeCategorical aval = (AttributeCategorical)as.getAttribute(attributeIndex);
    String[] tokens = acls.getTokens();
    String[] values = aval.getTokens();
    
    prob = new double[values.length][tokens.length];   
    prior = new double[tokens.length];
    for(double[] vc : prob)
      Arrays.fill(vc,1); // simple Laplace smoothing
    Arrays.fill(prior,1); // simple Laplace smoothing

    for(Node n : split.getTrainSet())
    {
      if(n.isMissing(idx) || n.isMissing(attributeIndex))
        continue;
      
      int cls = (int)n.getValue(idx);
      int val = (int)n.getValue(attributeIndex);
      prob[val][cls]++;
      prior[cls]++;
    }
    for(double[] vc : prob)
      VectorMath.normalize(vc);
    VectorMath.normalize(prior);
  }
  
  @Override
  public void buildModel(final DataSplit split) {
    edges=null;
    super.buildModel(split);
    computeStatistics(split);
  }
}
