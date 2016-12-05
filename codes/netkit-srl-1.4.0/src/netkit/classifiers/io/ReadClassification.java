/**
 * ReadClassification.java
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

import netkit.classifiers.Classification;
import netkit.graph.Graph;
import netkit.graph.AttributeCategorical;

import java.io.File;

/**
 * This interface defines the methods needed to read a set of true classifications from a file.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public interface ReadClassification {
    /**
     * Read from a given file a estimate of classifications for nodes in
     * the given graph.  Return these classifications in a Classification
     * object.
     *
     * @param graph The graph whose nodes are two be classified
     * @param nodeType  The nodeType which the classification object refers to
     * @param attribute  The categorial values to map classification-names against.
     * @param input The file from which to read classifications
     *
     * @return A Classification object for the classifications read in.
     *         Nodes whose classifications were not in the given file are
     *         classified as index '-1'
     **/
    public Classification readClassification(Graph graph,
                                             String nodeType,
                                             AttributeCategorical attribute,
                                             File input);

    /**
     * Read from a given file a estimate of classifications for nodes in
     * the given graph.  Fill these into the given Classification
     * object.
     *
     * @param graph  The graph whose nodes are two be classified
     * @param labels The classification object to fill in with the
     *               read in labels.   The calling function should
     *               lock the valuemap if no new labels are to be
     *               accepted.
     * @param input The file from which to read classifications
     **/
    public void readClassification(Graph graph,
				   Classification labels,
				   File input);
}
