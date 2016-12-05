/**
 * GaussianNumericEdgeCreator.java
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
package netkit.graph.edgecreator;

import netkit.graph.*;
import netkit.classifiers.DataSplit;
import java.util.Arrays;
import netkit.util.VectorMath;
import netkit.util.StatUtil;

public class GaussianNumericEdgeCreator extends BaseNumericEdgeCreator {
  private double[] mean = null;
  private double[] stddev = null;
  private int[] count = null;
  private double[] prior = null;

  @Override
  public String getName() {
    return "gaussianNumericEdgeCreator";
  }

  @Override
  protected double getWeightFast(Node src, Node dest) {
    double val = 0;
    for(int i=0;i<mean.length;i++)
    {
      // TODO: THIS SHOULD NEVER HAPPEN --- BUT IT DOES?!
      if(count[i] == 0)
        continue;
      double prob1 = 2.0D*(1.0D - StatUtil.getSignificance(count[i],Math.abs(src.getValue(attributeIndex)-mean[i])/stddev[i]));
      double prob2 = 2.0D*(1.0D - StatUtil.getSignificance(count[i],Math.abs(dest.getValue(attributeIndex)-mean[i])/stddev[i]));
      val += prior[i]*prior[i]*prob1*prob2;
    }
    
    return val;
  }

  private void computeStatistics(final DataSplit split) {
    Attributes as = graph.getAttributes(nodeType);
    int idx = split.getView().getAttributeIndex();
    AttributeCategorical acls = (AttributeCategorical)as.getAttribute(idx);
    String[] tokens = acls.getTokens();
    
    mean = new double[tokens.length];
    stddev = new double[tokens.length];
    prior = new double[tokens.length];
    Arrays.fill(prior,0);
    Arrays.fill(mean,0);
    Arrays.fill(prior,0);

    for(Node n : split.getTrainSet())
    {
      if(n.isMissing(idx) || n.isMissing(attributeIndex))
        continue;
      
      int cls = (int)n.getValue(idx);
      double val = n.getValue(attributeIndex);
      mean[cls] += val;
      prior[cls]++;
    }
    
    for(int i=0;i<mean.length;i++)
      mean[i] /= prior[i];
    
    for(Node n : split.getTrainSet())
    {
      if(n.isMissing(idx) || n.isMissing(attributeIndex))
        continue;

      int cls = (int)n.getValue(idx);
      double val = n.getValue(attributeIndex);
      double diff = val-mean[cls];
      
      stddev[cls] += diff*diff;
    }
    
    // get unbiased variance
    for(int i=0;i<stddev.length;i++)
    {
      stddev[i] /= (prior[i]-1);
      stddev[i] = Math.sqrt(stddev[i]);
    }
    
    count = new int[prior.length];
    for(int i=0;i<prior.length;i++)
      count[i] = (int)prior[i];
    VectorMath.normalize(prior);
  }

  @Override
  public void buildModel(final DataSplit split) {
    edges=null;
    super.buildModel(split);
    computeStatistics(split);
  }
}
