/**
 * ReadEstimate.java
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

package netkit.classifiers.io;

import netkit.classifiers.Estimate;
import netkit.graph.Graph;
import netkit.graph.AttributeCategorical;

import java.io.File;

/**
 * This interface defines the methods needed to read a set of predictions from a file.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */

public interface ReadEstimate {
    /**
     * Read from a given file an estimate for nodes in
     * the given graph.  Return these estimates in an Estimate
     * object.
     *
     * @param graph The graph whose nodes are two be estimated
     * @param nodeType  The nodeType which the estimated object refers to
     * @param attribute  The categorial values to map estimation-names against.
     * @param input The file from which to read estimations
     *
     * @return An Estimate object for the estimations read in.
     *         Nodes whose estimates were not in the given file have a 'null' estimate.
     **/
    public Estimate readEstimate(Graph graph,
                                 String nodeType,
                                 AttributeCategorical attribute,
                                 File input);

    /**
     * Read from a given file estimates for nodes in
     * the given graph.  Fill these into the given Estimate
     * object.
     *
     * @param graph  The graph whose nodes are two be estimated
     * @param estimates The Estimate object to fill in with the
     *               read in estimates.
     * @param input The file from which to read classifications
     **/
    public void readEstimate(Graph graph,
                             Estimate estimates,
                             File input);
}
