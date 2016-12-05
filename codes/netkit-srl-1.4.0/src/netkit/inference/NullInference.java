/**
 * NullInference.java
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
 * $Id: NullInference.java,v 1.3 2004/12/12 04:32:51 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:04:56 AM
 */
package netkit.inference;

import java.util.Iterator;

import netkit.classifiers.relational.NetworkClassifier;
import netkit.graph.Node;
import netkit.util.Configuration;

public class NullInference extends InferenceMethod
{
    boolean called = false;

    @Override
    public String getShortName() {
	    return "NullInference";
    }

    @Override
    public String getName() {
	    return "NullInference";
    }

    @Override
    public String getDescription() {
	    return "";
    }

    @Override
    public Configuration getDefaultConfiguration() {
      Configuration dCfg = super.getDefaultConfiguration();
      dCfg.set("numit",1);
      return dCfg;
    }

    @Override
    public boolean iterate(NetworkClassifier networkClassifier) {
      if(called)
        return false;
      
      called = true;
      for (Node n : unknown)
        networkClassifier.estimate(n, initialPrior, currPrior, true);

      return true;
    }
    
    @Override
    public void reset(Iterator<Node> unknowns) {
      super.reset(unknowns);
      called = false;
    }
}
