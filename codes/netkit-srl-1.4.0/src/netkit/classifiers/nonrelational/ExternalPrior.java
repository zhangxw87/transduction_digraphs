/**
 * ExternalPrior.java
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
import netkit.classifiers.ClassifierImp;
import netkit.classifiers.Estimate;
import netkit.classifiers.DataSplit;
import netkit.classifiers.io.ReadEstimate;
import netkit.util.Configuration;
import netkit.util.Factory;
import netkit.util.ArrayUtil;
import netkit.util.NetKitEnv;

import java.io.File;
import java.util.logging.Level;

/**
 * This classifier reads in estimates from a user-specified file.  It uses a ReadEstimate object
 * to read estimates from a file.  It uses a Factory to get a ReadEstimate object.  This factory
 * is initialized from the <code>readestimate.properties</code> file.
 * <p>
 * The classifier itself has two properties (read from <code>lclassifier.properties</code> or
 * specified from the commandline):
 * <ul>
 * <li>reader: the name of the ReadEstimate object.  Default='rainbow'.
 * <li>priorfile: the name of the file to read.  Default=null.
 * </ul>
 * These are in addition to any properties used by the superclass.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class ExternalPrior extends ClassifierImp
{
    // The factory from which to get a ReadEstimate object
    private static final Factory<ReadEstimate> factory = new Factory<ReadEstimate>("readestimate");

    // The file to read estimates from
    private File priorfile = null;

    // The instantiated ReadEstimate object gotten from the factory
    private ReadEstimate reader = null;

    // The Estimate object instantiated by reading the input file
    private Estimate externalprior = null;

    /**
     * @return 'ExternalPrior'
     */
    public String getShortName() {
        return "ExternalPrior";
    }
    /**
     * @return 'ExternalPrior'
     */
    public String getName() {
        return "ExternalPrior";
    }
    /**
     * @return A short description
     */
    public String getDescription() {
        return "Static classifier that returns predictions read from a file.";
    }

    /**
     * Sets a default configuration where the reader is of type 'rainbow', which should
     * resolve to the ReadEstimateRainbow class in the 'readestimate.properties' file.  This
     * is in addition to any defaults set by the superclass.
     *
     * @return a default configuration object.
     * @see netkit.classifiers.io.ReadEstimateRainbow
     */
    public Configuration getDefaultConfiguration() {
        Configuration conf = super.getDefaultConfiguration();
        conf.set("reader","rainbow");
        return conf;
    }

    /**
     * Configure this classifier using the passed-in configuration.
     *
     * @param config The configuration object to use to configure this classifier
     */
    public void configure(Configuration config) {
        super.configure(config);
        String readName = config.get("reader","rainbow");
        reader = factory.get(readName);
        if(reader == null)
            throw new IllegalArgumentException("Invalid reader '"+readName+"' - must be one of "+ArrayUtil.asString(factory.getValidNames()));
        String filename = config.get("priorfile",null);
        if(filename == null)
            throw new IllegalArgumentException("No priorfile name given");
        priorfile = new File(filename);
        if(!priorfile.exists())
            throw new IllegalArgumentException("priorfile("+filename+") does not exist");
    }

    /**
     * Inducing this model simply means to read the estimates from the input file.
     * @param graph Graph whose nodes are to be estimated
     * @param split The split between training and test.  Used to get the nodetype and class attribute.
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        externalprior = new Estimate(graph, split.getView().getNodeType(), split.getView().getAttribute());
        logger.info(getName()+" reading prior from '"+priorfile+"'");
        reader.readEstimate(graph, externalprior, priorfile);
        if(logger.isLoggable(Level.FINE)) logger.fine(externalprior.toString());
    }

    /**
     * Estimate class probabilities for the given node--returns the read in estimates.
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return true
     */
    public boolean estimate(Node node, double[] result) {
        double[] e = externalprior.getEstimate(node, classPrior);
        logger.finest(getName()+" estimate["+node.getName()+","+node.getIndex()+"] = "+ArrayUtil.asString(e));
        System.arraycopy(e,0,result,0,e.length);
        return true;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("[[ Priors for each nodes ]]").append(NetKitEnv.newline);
    	return sb.toString();
    }
}
