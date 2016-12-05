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
package netkit.classifiers.relational;

import netkit.graph.Node;
import netkit.graph.Graph;
import netkit.classifiers.Classifier;
import netkit.classifiers.DataSplit;
import netkit.classifiers.Estimate;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;

/**
 * a classifier that multiplies the predictions of one or more classifiers and returns a normalized
 * distribution as its own estimate.  See the superclass for configuration details.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class MetaMultiplicative extends NetworkMetaClassifier {
    // temporary vector to multiply predictions from each classifier into
    private double[] tmpVector;

    // a place to cache the estimates of the nonrelational classifiers since they will not change over time
    private Estimate localEstimate = null;

    /**
     * @return 'MetaMultiplicative'
     */
    public String getShortName() {
        return "MetaMultiplicative";
    }

    /**
     * @return 'MetaMultiplicative Network Classifier(classifiernames)'
     * @see NetworkMetaClassifier#getClassifierNames()
     */
    public String getName() {
        return "MetaMultiplicative Network Classifier("+getClassifierNames()+")";
    }

    /**
     * @return 'Does a bayesian combination (multiplies probabilities) of the classifiers to be used'
     */
    public String getDescription() {
        return "Does a bayesian combination (multiplies probabilities) of the classifiers to be used";
    }

    /**
     * Induce the model.  This calls the superclass.
     *
     * @param graph
     * @param split
     *
     * @see netkit.classifiers.relational.NetworkMetaClassifier#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        tmpVector = new double[classPrior.length];
        localEstimate = new Estimate(graph, split.getView().getNodeType(), split.getView().getAttribute());
    }

    /**
     * Get the estimates from each of the underlying classifiers, multiply their respective predictions
     * together and return a normalized distribution.
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return the label estimate distribution of this classifier
     */
    protected boolean doEstimate(Node node, double[] result) {
        double[] e = localEstimate.getEstimate(node);

        // See if the local classifiers have already made a prediction on this
        // node.  If so, reuse their cached response, otherwise get their predictions
        // and cache them.  We cache the results because it may be timeconsuming for
        // some classifiers to generate their predictions.
        if(e == null)
        {
            System.arraycopy(classPrior,0,result,0,classPrior.length);
            for(Classifier lc : lclassifiers)
            {
                lc.estimate(node, tmpVector);
                for(int i=0;i<classPrior.length;i++)
                    result[i] *= tmpVector[i];
            }
            localEstimate.estimate(node, result);
        }
        else
        {
            System.arraycopy(e,0,result,0,e.length);
        }

        for(NetworkClassifier nc : rclassifiers)
        {
            nc.estimate(node, this.prior, tmpVector, false);
            for(int i=0;i<classPrior.length;i++)
                result[i] *= tmpVector[i];
        }
        VectorMath.normalize(result);
        return true;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("Local classifiers:");
    	for(Classifier lc : lclassifiers) {
    		sb.append(lc.toString());
    	}
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("Relational classifiers:");
    	for(Classifier rc : rclassifiers) {
    		sb.append(rc.toString());
    	}
    	return sb.toString();
    }
}
