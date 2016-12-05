/**
 * NetworkWeka.java
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

/**
 * $Id: NetworkWeka.java,v 1.9 2007/04/11 21:58:03 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 10:58:51 AM
 */
package netkit.classifiers.relational;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.FastVector;
import weka.core.Instances;
import netkit.util.Factory;
import netkit.util.ArrayUtil;
import netkit.util.Configuration;
import netkit.util.NetKitEnv;
import netkit.graph.*;
import netkit.classifiers.DataSplit;
import netkit.classifiers.aggregators.Aggregator;

/**
 * Weka wrapper that uses a specified weka classifier to do its predictions.  This includes
 * converting all attributes into the weka format.  The classifier uses a Factory to read the
 * <code>weka.properties</code> file to look up a named weka classifier.
 * <p>
 * The classifier itself uses two properties (specified in the <code>lclassifier.properties</code> file):
 * <ul>
 * <li>classifier: the weka classifier (name should be defined in the <code>weka.properties</code> file.
 * <li>options: optional string that specifies the commandline options to pass to the weka classifier.
 * </ul>
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 *
 * @see netkit.classifiers.NetworkLearning.LC_PREFIX
 */
public final class NetworkWeka extends NetworkClassifierImp {
    // The factory from which to get a Weka classifier.  This factory reads configuration information
    // from weka.properties
    private static Factory<Classifier> classifiers = new Factory<Classifier>("weka");

    // The Weka classifier that is being wrapped
    private Classifier classifier;

    // The Weka learner as a string name
    private String learner;

    // Weka Instances object for test instances
    private Instances testInstances = null;

    // Wka Instance object for testing --- this is the only instance in the testInstances object
    private Instance testInstance = null;

    /**
     * Configure this classifier by getting the Weka classifier object using the <code>classifier</code>
     * and <code>options</code> properties in addition to anything used by the superclass.  The classifier
     * property defines the weka classifier (this name should resolve to a real weka class name in the
     * <code>weka.properties</code> file).  The options property defines the options string to pass to
     * the weka classifier.
     *
     * @param config The configuration object used to configure this classifier
     */
    public void configure(Configuration config) {
        learner = config.get("classifier");
        if(learner == null)
            throw new IllegalArgumentException("No weka classifier specified.  Make sure a 'classifier' property is specified.");
        classifier = classifiers.get(learner);
        if(classifier == null)
            throw new IllegalArgumentException("Invalid weka classifier name of '"+learner+"' - must be one of "+ArrayUtil.asString(classifiers.getValidNames()));

        super.configure(config);

        logger.config("   "+this.getClass().getName()+"["+getName()+"] configure: weka-classifier["+learner+"]="+classifier);

        if(config.containsKey("options"))
        {
            try
            {
                classifier.setOptions(config.get("options").split(" "));
            }
            catch(Exception ex)
            {
                throw new RuntimeException("Failed to initialize "+this.getClass().getName(),ex);
            }
        }
    }

    /**
     * This classifier needs to include the class attribute at all times because the Weka
     * classifier needs it.
     *
     * @return true
     */
    protected boolean includeClassAttribute() {
        return true;
    }

    /**
     * @return &quot;NetworkWeka(LEARNER)&quot;
     */
    public String getShortName() {
        return "NetworkWeka("+learner+")";
    }

    /**
     * @return &quot;NetworkWeka(LEARNER)[LEARNER-class]&quot;
     */
    public String getName() {
        return "NetworkWeka("+learner+")["+classifier.getClass()+"]";
    }

    /**
     * @return &quot;A Wrapper for a weka network-classifier&quot;
     */
    public String getDescription() {
        return "A Wrapper for a weka network-classifier.";
    }

    /**
     * Induce the weka classifier by creating a training Instances object according
     * to the schema of the nodes to be classified.
     *
     * @param graph Graph whose nodes are to be estimated
     * @param split The split between training and test.  Used to get the nodetype and class attribute.
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        Node[] trainingSet = split.getTrainSet();
        if(trainingSet == null || trainingSet.length == 0)
            return;

        Attributes attribs = trainingSet[0].getAttributes();
        FastVector attInfo = new FastVector(tmpVector.length);
        logger.finer("Setting up WEKA attributes");
        if(useIntrinsic)
        {
            for(Attribute attrib : attribs)
            {
                // do not include the KEY attribute
                if(attrib == attribs.getKey())
                    continue;

                switch(attrib.getType())
                {
                case CATEGORICAL:
                    String[] tokens = ((AttributeCategorical)attrib).getTokens();
                    FastVector values = new FastVector(tokens.length);
                    for(String token : tokens)
                        values.addElement(token);
                    attInfo.addElement(new weka.core.Attribute(attrib.getName(),values));
                    logger.finer("Adding WEKA attribute "+attrib.getName()+":Categorical");
                    break;

                default:
                    attInfo.addElement(new weka.core.Attribute(attrib.getName()));
                    logger.finer("Adding WEKA attribute "+attrib.getName()+":Numerical");
                    break;
                }
            }
        }
        else
        {
            String[] tokens = attribute.getTokens();
            FastVector values = new FastVector(tokens.length);
            for(String token : tokens)
                values.addElement(token);
            attInfo.addElement(new weka.core.Attribute(attribute.getName(),values));
            logger.finer("Adding WEKA attribute "+attribute.getName()+":Categorical");
        }

        for(Aggregator agg : aggregators)
        {
            Attribute attrib = agg.getAttribute();
            switch(agg.getType())
            {
            case CATEGORICAL:
                String[] tokens = ((AttributeCategorical)attrib).getTokens();
                FastVector values = new FastVector(tokens.length);
                for(String token : tokens)
                    values.addElement(token);
                attInfo.addElement(new weka.core.Attribute(agg.getName(),values));
                logger.finer("Adding WEKA attribute "+agg.getName()+":Categorical");
                break;

            default:
                attInfo.addElement(new weka.core.Attribute(agg.getName()));
                logger.finer("Adding WEKA attribute "+agg.getName()+":Numerical");
                break;
            }
        }

        Instances train = new Instances("train",attInfo,split.getTrainSetSize());
        train.setClassIndex(vectorClsIdx);

        for(Node node : split.getTrainSet())
        {
            double[] v = new double[attInfo.size()];
            makeVector(node,v);
            train.add(new Instance(1,v));
        }
        try
        {
            classifier.buildClassifier(train);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to build classifier "+classifier.getClass().getName(),e);
        }
        testInstance = new Instance(1,tmpVector);
        testInstances = new Instances("test",attInfo,1);
        testInstances.setClassIndex(vectorClsIdx);
        testInstances.add(testInstance);
        testInstance = testInstances.firstInstance();
    }

    /**
     * Predict class labels for the given node.
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return true
     */
    public boolean doEstimate(Node node, double[] result) {
        makeVector(node,tmpVector);
        try
        {
            double[] dist = classifier.distributionForInstance(testInstance);
            System.arraycopy(dist,0,result,0,dist.length);
        }
        catch(Exception e)
        {
            throw new RuntimeException("failed to estimate distribution for given node",e);
        }
        return true;
    }
    
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getShortName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append(classifier.toString());

    	return sb.toString();
    }
}

