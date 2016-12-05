/**
 * LocalMetaClassifier.java
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

import netkit.util.Configuration;
import netkit.util.ArrayUtil;
import netkit.classifiers.ClassifierImp;
import netkit.classifiers.Classifier;
import netkit.classifiers.NetworkLearning;
import netkit.classifiers.DataSplit;
import netkit.graph.Graph;

import java.util.ArrayList;

/**
 * Abstract class for combining multiple classifiers.  It configures itself with the list of
 * classifiers to use and induces them individually.  How to combine them is left to subclasses.
 * It uses the <code>Networklearning.LC_PREFIX</code> property from the configuration details
 * specified in the <code>lclassifier.properties</code> file (in addition to any properties
 * used by the superclass).
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public abstract class LocalMetaClassifier extends ClassifierImp {
    // The comma-separated list of classifiers that are used in this classifier
    private String classifierNames = "";

    /**
     * The list of classifiers to combine
     */
    protected ArrayList<Classifier> lclassifiers = null;

    /**
     * Default configuration uses only the naive Bayes classifier in addition to any
     * defaults from the superclass
     * @return a default Configuration object
     */
    public Configuration getDefaultConfiguration() {
        Configuration conf = super.getDefaultConfiguration();
        conf.set(NetworkLearning.LC_PREFIX,"naivebayes");
        return conf;
    }

    /**
     * Configures the classifier by getting the list of classifiers to use (comma-separated
     * list in the <code>NetworkLearning.LC_PREFIX</code> property.
     *
     * @param config The configuration object to use to configure this classifier.
     *
     * @see netkit.classifiers.NetworkLearning.LC_PREFIX
     */
    public void configure(Configuration config) {
        super.configure(config);
        classifierNames = config.get(NetworkLearning.LC_PREFIX,"naivebayes");
        String[] nlcs = classifierNames.split(",");

        lclassifiers = new ArrayList<Classifier>(nlcs.length);
        logger.config("  configure: "+NetworkLearning.LC_PREFIX+"="+classifierNames+" "+ArrayUtil.asString(nlcs));
        for(String s : nlcs)
        {
            Classifier lc = NetworkLearning.lclassifiers.get(s.trim(),config);
            if(lc == null)
                throw new IllegalArgumentException("Invalid local classifier '"+s+"' - does not exist.   Valid names are: "+ArrayUtil.asString(NetworkLearning.lclassifiers.getValidNames()));
            logger.fine("  added "+lc.getName());
            lclassifiers.add(lc);
        }
    }

    /**
     * This induces each of the local classifiers individually.  If learning need
     * be done to learn how to combine them, then that needs to be done by subclasses.
     *
     * @param graph Graph whose nodes are to be estimated
     * @param split The split between training and test.  Used to get the nodetype and class attribute.
     */
    public void induceModel(Graph graph, DataSplit split) {
        for(Classifier lc : lclassifiers)
        {
            logger.info("inducing "+lc.getName());
            lc.induceModel(graph,split);
        }
    }

    /**
     * @return the list of classifier names from the configuration object when configure was called
     * @see netkit.classifiers.nonrelational.LocalMetaClassifier#configure(netkit.util.Configuration)
     */
    protected String getClassifierNames() {
        return classifierNames;
    }
}
