/**
 * IterativeClassification.java
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
 **/

/**
 * $Id$
 **/

/**
 * $Id: IterativeClassification.java,v 1.4 2007/03/26 23:45:07 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:04:12 AM
 */
package netkit.inference;

import netkit.util.Configuration;
import netkit.util.VectorMath;
import netkit.graph.Node;
import netkit.classifiers.Estimate;
import netkit.classifiers.relational.NetworkClassifier;

import java.util.Iterator;

public class IterativeClassification extends InferenceMethod
{
  private Estimate tmpEstimate = null;

  public String getShortName() {
    return "Iterative";
  }
  public String getName() {
    return "IterativeClassification";
  }
  public String getDescription() {
    return "Classifies unknowns by the most confident first, then the less confident (making use of the updated classifications)";
  }

  public Configuration getDefaultConfiguration() {
    Configuration dCfg = super.getDefaultConfiguration();
    dCfg.set("numit",1000);
    return dCfg;
  }
  public void reset(Iterator<Node> unknowns) {
    super.reset(unknowns);
    tmpEstimate = new Estimate(currPrior.getGraph(), currPrior.getNodeType(),currPrior.getAttribute());
  }

  public boolean iterate(NetworkClassifier networkClassifier) {
    boolean changed = false;
    for (Node n : unknown)
    {
      int oIdx = tmpEstimate.getClassification(n);
      if(networkClassifier.estimate(n,tmpEstimate,tmpPredict,true))
      {
        currPrior.estimate(n,tmpPredict);
        int pIdx = VectorMath.getMaxIdx(tmpPredict);
        tmpEstimate.classify(n,pIdx);
        if(oIdx != pIdx)
          changed = true;
      }
      else
      {
        changed = (oIdx != -1);
      }
    }
    return changed;
  }
}
