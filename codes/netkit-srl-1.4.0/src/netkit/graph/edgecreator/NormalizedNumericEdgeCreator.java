/**
 * BaseNumericEdgeCreator.java
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

import netkit.graph.Graph;
import netkit.graph.Node;

public class NormalizedNumericEdgeCreator extends BaseNumericEdgeCreator {
  private double min = Double.MAX_VALUE;
  private double max = Double.MIN_VALUE;
  private double range = Double.NaN;
  private double mean = Double.NaN;
                   
  @Override
  public void initialize(final Graph graph, final String nodeType, final int attributeIndex, final double attributeValue, final int maxEdges) {
    super.initialize(graph, nodeType, attributeIndex, attributeValue, maxEdges);
    mean = 0;
    double count = 0;
    for(Node n : graph.getNodes(nodeType)) {
      if(n.isMissing(attributeIndex))
        continue;
      count++;
      double v = n.getValue(attributeIndex);
      if(v > max) max = v;
      if(v < min) min = v;
      mean += v;
    }
    if(count > 0) {
      range = max-min;
      if(range == 0) range = 1.0D;
      mean /= count;
    }
    logger.info("initialize["+getEdgeType()+"] - min="+min+" max="+max+"  range="+range+"   mean="+mean);
  }

  @Override
  public String getName() {
    return "normalizedNumericEdgeCreator";
  }

  @Override
  protected double getValue(Node n) {
    return (n.isMissing(attributeIndex) ? Double.NaN : (n.getValue(attributeIndex)-min)/range);
  }
  
  @Override
  protected double getWeightFast(Node src, Node dest) {
    double w1 = getValue(src);
    double w2 = getValue(dest);
    double diff = Math.abs(w1-w2)/range;
    return 1.0D-Math.min(1,diff);
  }
  
  @Override
  public double getWeight(Node src, Node dest) {
    if(attributeIndex == -1)
      throw new IllegalStateException("EdgeCreator["+getName()+"] has not yet been initialized!");
    return getWeightFast(src,dest);
  }
}
