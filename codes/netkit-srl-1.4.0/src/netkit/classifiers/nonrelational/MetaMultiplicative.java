/**
 * MetaMultiplicative.java
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
import netkit.graph.Graph;
import netkit.classifiers.Classifier;
import netkit.classifiers.DataSplit;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;

/**
 * a classifier that multiplies the predictions of one or more classifiers and returns a normalized
 * distribution as its own estimate.  See the superclass for configuration details.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class MetaMultiplicative extends LocalMetaClassifier {
    // The array to store the multiplicative results
    private double[] tmpVector;

    /**
     * @return 'MetaMultiplicative'
     */
    public String getShortName() {
        return "MetaMultiplicative";
    }

    /**
     * @return 'MetaMultiplicative[COMMA-SEPARATED-LIST-OF-CLASSIFIERS]'
     */
    public String getName() {
        return "MetaMultiplicative["+getClassifierNames()+"]";
    }

    /**
     * @return a short description of this class functionality
     */
    public String getDescription() {
        return "Does a bayesian combination (multiplies probabilities) of the classifiers to be used";
    }
    /**
     * Induce the model.  This calls the superclass.
     *
     * @param graph Graph whose nodes are to be estimated
     * @param split The split between training and test.  Used to get the nodetype and class attribute.
     *
     * @see netkit.classifiers.nonrelational.LocalMetaClassifier#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        tmpVector = new double[classPrior.length];
    }

    /**
     * Get the estimates from each of the underlying classifiers, multiply their respective predictions
     * together and return a normalized distribution.
     *
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return the label estimate distribution of this classifier
     */
    public boolean estimate(Node node, double[] result) {
        System.arraycopy(classPrior,0,result,0,classPrior.length);
        for(Classifier lc : lclassifiers)
        {
            lc.estimate(node, tmpVector);
            for(int i=0;i<classPrior.length;i++)
                result[i] *= tmpVector[i];
        }
        VectorMath.normalize(result);
        return true;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	for(Classifier lc : lclassifiers) {
    		sb.append(lc.toString());
    	}
    	return sb.toString();
    }
}
