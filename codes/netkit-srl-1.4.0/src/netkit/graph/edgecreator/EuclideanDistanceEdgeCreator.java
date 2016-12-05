/**
 * EuclideanDistanceEdgeCreator.java
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

import java.util.*;

public class EuclideanDistanceEdgeCreator extends EdgeCreatorImp {
  private interface DistanceFunction {
    public EdgeCreator getEdgeCreator();
    public double getDistance(Node src, Node dest);
  }
  private class CategoricalDistance implements DistanceFunction {
    final BaseCategoricalEdgeCreator ec;
    public CategoricalDistance(BaseCategoricalEdgeCreator ec) {
      this.ec = ec;
    }
    @Override
    public EdgeCreator getEdgeCreator() { return ec; }
    @Override
    public double getDistance(Node src, Node dest) {
      return 1.0D - ec.getWeight(src, dest);
    }
  }
  private class NumericDistance implements DistanceFunction {
    final NormalizedNumericEdgeCreator ec;
    public NumericDistance(NormalizedNumericEdgeCreator ec) {
      this.ec = ec;
    }
    @Override
    public EdgeCreator getEdgeCreator() { return ec; }
    @Override
    public double getDistance(Node src, Node dest) {
     double w1 = ec.getValue(src);
     double w2 = ec.getValue(dest);
     return Math.abs(w1-w2);
    }
  }

  private DistanceFunction[] distances = null;

  @Override
  public String getName() {
    return "euclideanDistanceEdgeCreator";
  }

  @Override
  /**
   * return false because this edge creator uses the whole instance
   */
  public boolean isByAttribute() { return false; }

  @Override
  /**
   * return false because this edge creator uses the whole instance
   */
  public boolean canHandle(Attribute attribute) { return false; }

  private void buildDistanceFunctions() {
    if(distances != null)
      return;
    List<DistanceFunction> ecl = new ArrayList<DistanceFunction>();
    Attributes as = graph.getAttributes(nodeType);
    int idx = split.getView().clsIdx;
    for(int i=0;i<as.attributeCount();i++)
    {
      if(i==as.getKeyIndex() || i==idx)
        continue;
      Attribute a = as.getAttribute(i);
      DistanceFunction df = null;
      EdgeCreator ec = null;
      if(a.getType() == Type.CATEGORICAL)
      {
        ec = new BaseCategoricalEdgeCreator();
        df = new CategoricalDistance((BaseCategoricalEdgeCreator)ec);
      }
      else
      {
        ec = new NormalizedNumericEdgeCreator();
        df = new NumericDistance((NormalizedNumericEdgeCreator)ec);
      }
      ec.initialize(graph, nodeType, i, attributeValue, maxEdges);
      ecl.add(df);
    }
    distances = ecl.toArray(new DistanceFunction[0]);
  }

  @Override
  public void buildModel(final DataSplit split) {
    super.buildModel(split);
    buildDistanceFunctions();
    for(DistanceFunction df : distances) {
      EdgeCreator ec = df.getEdgeCreator();
      ec.buildModel(split);
    }
  }

  @Override
  public double getWeight(Node src, Node dest) {
    double distance = 0;
    double len = 0;
    for(DistanceFunction df : distances)
    {
      double diff = df.getDistance(src, dest);
      if(Double.isNaN(diff))
        continue;
      distance += diff*diff;
      len++;
    }
    distance = (len == 0 ? Double.NaN : 1.0D/(1.0D+Math.sqrt(distance/len)));
    return distance;
  }
}
