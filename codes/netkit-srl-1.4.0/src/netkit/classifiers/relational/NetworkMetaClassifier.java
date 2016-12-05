/**
 * NetworkMetaClassifier.java
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

import netkit.classifiers.NetworkLearning;
import netkit.classifiers.Classifier;
 import netkit.classifiers.DataSplit;
import netkit.util.Configuration;
import netkit.util.ArrayUtil;
import netkit.graph.Graph;

import java.util.ArrayList;

/**
 * Abstract class for combining multiple relational and non-relational classifiers.  It configures itsel
 * with the list of (non-)relational classifiers to use and induces them individually.  How to combine them
 * is left to subclasses.  It uses the
 * <code>Networklearning.LC_PREFIX</code> and
 * <code>Networklearning.RC_PREFIX</code> properties from the configuration details
 * specified in the <code>lclassifier.properties</code> and <code>rclassifier.properties</code>
 * files (in addition to any properties used by the superclass).
 * <P>
 * <B>Properties</B>
 * <UL>
 * <LI>NetworkLearning.LC_PREFIX: what non-relational classifiers to use.  default=naivebayes (see <code>lclassifier.properties</code>)
 * <LI>NetworkLearning.RC_PREFIX: what relational classifiers to use.  default=naivebayes (see <code>rclassifier.properties</code>)
 * </UL>
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 *
 * @see NetworkLearning#LC_PREFIX
 * @see NetworkLearning#RC_PREFIX
 */
public abstract class NetworkMetaClassifier extends NetworkClassifierImp {
    // The comma-separated list of classifiers that are used in this classifier
    private String classifierNames = "";

    /**
     * The list of relational classifiers to use
     */
    protected ArrayList<NetworkClassifier> rclassifiers = null;

    /**
     * The list of non-relational classifiers to use
     */
    protected ArrayList<Classifier> lclassifiers = null;


    /**
     * Get the detault configuration of using a naive Bayes classifier both as the single non-relational and the
     * single relational classifier..  This is in addition to any defaults set by the superclass.
     *
     * @return a Configuration object
     *
     * @see NetworkClassifierImp#getDefaultConfiguration()
     */
    public Configuration getDefaultConfiguration() {
        Configuration dCfg = super.getDefaultConfiguration();
        dCfg.set(NetworkLearning.LC_PREFIX,"naivebayes");
        dCfg.set(NetworkLearning.RC_PREFIX,"naivebayes");
        return dCfg;
    }


    /**
     * Configure this classifier by getting the Weka classifier object using the <code>classifier</code>
     * and <code>options</code> properties in addition to anything used by the superclass.  The classifier
     * property defines the weka classifier (this name should resolve to a real weka class name in the
     * <code>weka.properties</code> file).  The options property defines the options string to pass to
     * the weka classifier.
     * <p>
     * By default, this will use a naive Bayes classifier both as the single non-relational and the
     * single relational classifier.
     *
     * @param config The configuration object used to configure this classifier
     *
     * @see NetworkClassifierImp#configure(netkit.util.Configuration)
     */
    public void configure(Configuration config) {
        super.configure(config);

        String rcs = config.get(NetworkLearning.RC_PREFIX,"naivebayes");
        String[] nrcs = rcs.split(",");
        rclassifiers = new ArrayList<NetworkClassifier>(nrcs.length);
        logger.config("  configure: "+NetworkLearning.RC_PREFIX+"="+rcs+" "+ArrayUtil.asString(nrcs));
        for(String s : nrcs)
        {
            NetworkClassifier nc = NetworkLearning.rclassifiers.get(s.trim(),config);
            if(nc == null)
                throw new IllegalArgumentException("Invalid network classifier '"+s+"' - does not exist.   Valid names are: "+ArrayUtil.asString(NetworkLearning.rclassifiers.getValidNames()));
            logger.info("  added "+nc.getName());
            rclassifiers.add(nc);
        }

        String lcs = config.get(NetworkLearning.LC_PREFIX,"naivebayes");
        String[] nlcs = lcs.split(",");
        lclassifiers = new ArrayList<Classifier>(nlcs.length);
        logger.config("  configure: "+NetworkLearning.LC_PREFIX+"="+lcs+" "+ArrayUtil.asString(nlcs));
        for(String s : nlcs)
        {
            Classifier lc = NetworkLearning.lclassifiers.get(s.trim(),config);
            if(lc == null)
                throw new IllegalArgumentException("Invalid local classifier '"+s+"' - does not exist.   Valid names are: "+ArrayUtil.asString(NetworkLearning.lclassifiers.getValidNames()));
            logger.fine("  added "+lc.getName());
            lclassifiers.add(lc);
        }

        classifierNames = "RC["+rcs+"] LC["+lcs+"]";
    }

    /**
     * This separately induces all the non-relational and relational classifiers in addition to any
     * setup the super-class needs to do.
     *
     * @param graph The graph over which a model is induced
     * @param split The datasplit that specifies which nodes have their class labels known
     *
     * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        for(NetworkClassifier nc : rclassifiers)
        {
            logger.fine("Inducing "+nc.getName());
            nc.induceModel(graph, split);
        }
        for(Classifier lc : lclassifiers)
        {
            logger.fine("Inducing "+lc.getName());
            lc.induceModel(graph, split);
        }
    }

    /**
     * Returns the list of classifier names in the format: &quot;RC[relational_classifiers] LC[nonrelational_classifiers]&quot;
     * where the list of classifiers is comma-separated and appear exactly is it was in the configuration object
     * that was used to configure this classifier.
     *
     * @return the list of classifier names from the configuration object when configure was called
     * @see netkit.classifiers.relational.NetworkMetaClassifier#configure(netkit.util.Configuration)
     */
    protected String getClassifierNames() {
        return classifierNames;
    }
}
