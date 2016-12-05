/**
 * UniformPrior.java
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
import netkit.util.NetKitEnv;
import netkit.classifiers.DataSplit;
import netkit.classifiers.ClassifierImp;

import java.util.Arrays;

/**
 * Dummy classifier that always predicts that all classes were equally likely.  Useful
 * for simple baseline experiments and to gain insight into other parts of NetKit such as
 * the relational classifiers and inference methods.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class UniformPrior extends ClassifierImp
{
    private double uprior = 0;

    /**
     * @return 'UniformPriorPredictor'
     */
    public String getShortName() {
        return "UniformPriorPredictor";
    }
    /**
     * @return 'UniformPriorPredictor'
     */
    public String getName() {
        return "UniformPriorPredictor";
    }
    /**
     * @return 'Always returns a uniform prior (all classes are equally likely).'
     */
    public String getDescription() {
        return "Always returns a uniform prior (all classes are equally likely).";
    }

    /**
     * Makes a uniform prediction array---all classes are equally likely
     * @param graph
     * @param split
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        uprior = 1.0/classPrior.length;
    }

    /**
     * Fills the result array with all the same values---each class is equally likely.
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.  These are set to <code>1/n</code>, where <code>n</code>
     *               is the number of classes.
     * @return true
     */
    public boolean estimate(Node node, double[] result) {
        Arrays.fill(result,uprior);
        return true;
    }
    
    public String toString() {
        double prior = 1.0/classPrior.length;

    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("-------------------------------------").append(NetKitEnv.newline);
    	for(int i=0;i<classPrior.length;i++) {
    		sb.append(attribute.getToken(i)).append(": ").append(prior).append(NetKitEnv.newline);
    	}
    	return sb.toString();
    }
}
