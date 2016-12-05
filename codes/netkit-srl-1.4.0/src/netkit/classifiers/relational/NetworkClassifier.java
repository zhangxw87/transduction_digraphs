/**
 * NetworkClassifier.java
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

import netkit.classifiers.*;
import netkit.graph.Node;

/**
 * Interface for a relational classifier in addition to those of a nonrelational classifier.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public interface NetworkClassifier extends Classifier
{
    /**
     * This is called prior to predicting labels for the unknown labels in the graph,
     * in case the classifier needs to initialize itself.  This is called at the beginning
     * of every iteration of a collective inference run.
     *
     * @param currPrior The current 'priors' or estimates of the unknown lables
     * @param unknowns The list of nodes which are to be predicted in the upcoming run.
     */
    public void initializeRun(Estimate currPrior, Node[] unknowns);

    /**
     * Classify a given node into one of the given classes.  It may use the class estimations
     * of other nodes and may update the prior of the given node.
     *
     * @param node The node to classify.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the predicted classification.
     * @return The index of the class that this node is classified as.  It returns -1 if it abstains.
     */
    public int classify(Node node, Estimate prior, boolean updatePrior);

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param result The array that is filled in with class estimates
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     */
    public boolean estimate(Node node, Estimate prior, double[] result, boolean updatePrior);

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return An array that is filled in with class estimates.  This is <code>null</code> is the classifier abstains
     */
    public double[] estimate(Node node, Estimate prior, boolean updatePrior);

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param result The Estimate object that is updated with class estimates.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     */
    public boolean estimate(Node node, Estimate prior, Estimate result, boolean updatePrior);
}
