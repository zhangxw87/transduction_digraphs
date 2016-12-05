/**
 * Classifier.java
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

package netkit.classifiers;

import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.util.Configurable;

/**
 * $Id: Classifier.java,v 1.4 2004/12/12 17:43:40 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 * <p/>
 * User: smacskassy
 * Date: Dec 2, 2004
 * Time: 9:16:44 PM
 */
public interface Classifier extends Configurable {
    public void reset();

    public void induceModel(Graph graph, DataSplit split);

    public double[] estimate(Node node);
    
    /**
     * Estimate the probabilities that a given node into belongs to any given class.
     *
     * @param node The node to estimate.
     * @param result The Estimate object that is updated with class estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     */
    public boolean estimate(Node node, double[] result);
    public boolean estimate(Node node, Estimate result);
    public int classify(Node node);
    public boolean classify(Node node, Classification result);

    public java.util.logging.Logger getLogger();
    
    public String getShortName();
    public String getName();
    public String getDescription();

    public void addListener(ClassifierListener cl);
    public void clearListeners();
    public void removeListener(ClassifierListener cl);

    public boolean getNofifyListeners();
    public void setNofityListeners(boolean notify);
    public void notifyListeners(Node node, double[] estimate);
    public void notifyListeners(Node node, int classification);
}
