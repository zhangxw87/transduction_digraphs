/**
 * CosineDistanceEdgeCreator.java
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

import netkit.classifiers.DataSplit;
import netkit.graph.*;
import netkit.util.VectorMath;
import java.util.Arrays;

public class CosineDistanceEdgeCreator extends EdgeCreatorImp {
  private double[] srcInstance = null;
  private double[] destInstance = null;
  private double[] count = null;
  private double[] mean = null;
  private int[] attrDim = null;

  @Override
  public String getName() {
    return "cosineDistanceEdgeCreator";
  }
  
  @Override
  public double getWeight(Node src, Node dest) {
    fillInstance(src,srcInstance,mean);
    fillInstance(dest,destInstance,mean);
    VectorMath.normalize(srcInstance);
    VectorMath.normalize(destInstance);
    
    // cosine-distance = dot product
    // convert from distance to weight: weight = 1 - distance (since distance lies in [0:1] range)
    return 1.0 - VectorMath.dotproduct(srcInstance, destInstance);
  }

  @Override
  /**
   * This edge creator uses the whole instance
   */
  public boolean isByAttribute() { return false; }
  

  @Override
  public boolean canHandle(Attribute attribute) {
    return true;
  }
 
  private void fillInstance(Node n, double[] instance, double[] defvals) {
    double[] vals = n.getValues();
    Arrays.fill(instance,0.0D);
    for(int i=0,j=0;i<vals.length;i++)
    {
      if(attrDim[i]==0)
        continue; 
      if(attrDim[i]==1)
      {
        if(n.isMissing(i))
          instance[j] = defvals[j];
        else
        {
          instance[j] = vals[i];
          count[j]++;
        }
        j++;
      }
      else
      {
        if(n.isMissing(i))
        {
          for(int k=0;k<attrDim[i];k++,j++)
            instance[j] = defvals[j];         
        }
        else
        {
          int iVal = j+(int)vals[i];
          instance[iVal] = 1;
          for(int k=0;k<attrDim[i];k++,j++)
            count[j]++;
        }
      }
    }
  }
  
  private void computeStatistics(final DataSplit split) {
    Attributes as = graph.getAttributes(nodeType);
    int idx = split.getView().getAttributeIndex();

    /**
     * Find dimension:
     * expand categorical attributes to one dimension per value
     */
    attrDim = new int[as.attributeCount()];
    Arrays.fill(attrDim,0);
    for(int i=0;i<as.attributeCount();i++)
    {
      if(i==as.getKeyIndex() || i==idx)
        continue;
      Attribute a = as.getAttribute(i);
      if(a.getType() == Type.CATEGORICAL)
        attrDim[i] = ((AttributeCategorical)a).getTokens().length;
      else
        attrDim[i] = 1;
    }
    int dim = VectorMath.sum(attrDim);
    
    srcInstance = new double[dim];
    destInstance = new double[dim];    
    count = new double[dim];
    mean = new double[dim];
    
    double[] zeroes = new double[dim];
    Arrays.fill(mean, 0.0D);
    Arrays.fill(zeroes, 0.0D);
    Arrays.fill(count,0.0D);
    
    /*
     * Now, compute means
     */
    for(Node n : split.getTrainSet())
    {
      if(n.isMissing(idx))
        return;

      fillInstance(n,srcInstance,zeroes);
      VectorMath.add(mean,srcInstance);
    }
    
    for(int i=0;i<count.length;i++)
    {
      if(count[i]==0)
        continue;
      mean[i] /= count[i];
    }
  }
  
  
  @Override
  public void buildModel(final DataSplit split) {
    edges=null;
    super.buildModel(split);
    computeStatistics(split);
  }

}
