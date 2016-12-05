/**
 * RandomLabeling.java
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
import netkit.util.*;

public class RandomLabeling extends PickLabelStrategyImp {
  
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    LabelNode[] candidates = new LabelNode[predictions.size()];
    if(predictions.size() == 0)
      return null;

    int i=0;
    for(final Node n : predictions)
    {
      candidates[i++] = new LabelNode(n,0);
    } 
    
    LabelNode[] nodes = new LabelNode[maxPicks];
    for(i=0;i<maxPicks;i++)
    {
      int p = VectorMath.pickRandom.nextInt(candidates.length);
      if(candidates[p] == null)
      {
        i--;
      }
      else
      {
        nodes[i] = candidates[p];
        candidates[p] = null;
      }
    }
      
    return nodes;
  }

  @Override
  public String getDescription() {    
    return "The RandomLabeling active learning strategy picks "+
           "nodes at random.";
  }

  @Override
  public String getName() {
    return "RandomLabeling";
  }

  @Override
  public String getShortName() {
    return "RandomLabeling";
  }

}
