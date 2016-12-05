/**
 * NullPrior.java
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
package netkit.classifiers.nonrelational;

import netkit.graph.Node;
import netkit.util.NetKitEnv;
import netkit.classifiers.ClassifierImp;

/**
 * Predict nothing.  Useful to initialize predictions in some circumstances (such as for the
 * Iterative Classification Algorithm).
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class NullPrior extends ClassifierImp
{
    /**
     * @return 'NullPrior'
     */
    public String getShortName() {
        return "NullPrior";
    }
    /**
     * @return 'NullPrior'
     */
    public String getName() {
        return "NullPrior";
    }
    /**
     * @return a short description of this class.
     */
    public String getDescription() {
        return "Always returns an empty, or null, prediction.  This is generally used by Iterative Classification to initialize the network.";
    }

    /**
     * @param node The node for which to make predictions
     * @param result The array to fill with the predictions---this is filled with the class marginals from the training set
     * @return false (meaning that it did not really make a prediction)
     */
    public boolean estimate(Node node, double[] result) {
        System.arraycopy(classPrior,0,result,0,classPrior.length);
        return false;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("Returns a null/empty prediction").append(NetKitEnv.newline);
    	return sb.toString();
    }
}
