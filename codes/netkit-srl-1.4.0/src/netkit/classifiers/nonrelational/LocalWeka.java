/**
 * LocalWeka.java
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

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.FastVector;
import weka.core.Instances;
import netkit.util.Factory;
import netkit.util.ArrayUtil;
import netkit.util.Configuration;
import netkit.util.NetKitEnv;
import netkit.graph.*;
import netkit.classifiers.ClassifierImp;
import netkit.classifiers.DataSplit;

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
public final class LocalWeka extends ClassifierImp {
    // Factory that creates Weka classifier object.  Uses the 'weka.properties' configuration file
    private static Factory<Classifier> classifiers = new Factory<Classifier>("weka");

    // The weka classifier object
    private Classifier classifier;

    // The name to look up in 'weka.properties' to find the right weka classifier
    private String learner;

    // Test instance wrapper object---convert a NetKit node into a weka Instance object
    private Instance testInstance = null;

    // Training instances wrapper object---convert a set of NetKit nodes into a weka Instances object
    private Instances testInstances = null;

    // The vector of doubles that is used by the testInstance object---this is what needs to be filled
    private double[] cVector = null;

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
        this.learner = config.get("classifier");
        if(learner == null)
            throw new IllegalArgumentException("No weka classifier specified.  Make sure a 'classifier' property is specified.");
        this.classifier = classifiers.get(learner);
        if(classifier == null)
            throw new IllegalArgumentException("Invalid weka classifier name of '"+learner+"' - must be one of "+ArrayUtil.asString(classifiers.getValidNames()));

        super.configure(config);

        logger.config("   "+this.getClass().getName()+" configure: weka-classifier["+learner+"]="+classifier);

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
        /**
         * For later use... eventually we may want to read in a Weka model from a file:
         *
         * read in weka-model from file:
         *            InputStream is = new FileInputStream(objectInputFileName);
          if (objectInputFileName.endsWith(".gz")) {
            is = new GZIPInputStream(is);
          }
	     objectInputStream = new ObjectInputStream(is);
         if (objectInputFileName.length() != 0) {

           // Load classifier from file
           classifier = (Classifier) objectInputStream.readObject();
           objectInputStream.close();
         }
         */
    }

    /**
     * @return 'LocalWeka(WEKA-LEARNER-NAME)'
     */
    public String getShortName() {
        return "LocalWeka("+learner+")";
    }

    /**
     * @return 'LocalWeka(WEKA-LEARNER-NAME)[WEKA-FULLY-SPECIFIED-CLASSNAME]'
     */
    public String getName() {
        return "LocalWeka("+learner+")["+classifier.getClass()+"]";
    }

    /**
     * @return 'A Wrapper for a weka classifier using only intrinsic attributes'
     */
    public String getDescription() {
        return "A Wrapper for a weka classifier using only intrinsic attributes";
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

        // return if no training is to be done.
        if(trainingSet == null || trainingSet.length == 0)
            return;

        // Create a FastVector of the possible values of the class attribute
        FastVector clsValues = new FastVector(attribute.size());
        for(String token : attribute.getTokens())
            clsValues.addElement(token);

        // Create the array that defines the attributes.  We do not include the 'key' attribute
        Attributes attribs = trainingSet[0].getAttributes();
        FastVector attInfo = new FastVector(attribs.attributeCount()-1);
        for(Attribute attrib : attribs)
        {
            // do not include the KEY attribute
            if(attrib == attribs.getKey())
                continue;

            if(attrib.getType()==Type.CATEGORICAL)
            {
                String[] tokens = ((AttributeCategorical)attrib).getTokens();
                FastVector values = new FastVector(tokens.length);
                for(String token : tokens)
                    values.addElement(token);
                attInfo.addElement(new weka.core.Attribute(attrib.getName(),values));
            }
            else
                attInfo.addElement(new weka.core.Attribute(attrib.getName()));
        }

        // Create the training Instances object + set the class attribute index
        Instances train = new Instances("train",attInfo,split.getTrainSetSize());
        train.setClassIndex(vectorClsIdx);

        // Create the training instance objects
        for(Node node : split.getTrainSet())
        {
            double[] v = new double[attInfo.size()];
            makeVector(node,v);
            train.add(new Instance(1,v));
        }

        // Finally induce the weka classifier
        try
        {
            classifier.buildClassifier(train);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to build classifier "+classifier.getClass().getName(),e);
        }

        // Now set up the test environment.  It is a test Instances object containing
        // only a single test instance.  We also keep a reference to the double array
        // that represents the attribute values.
        cVector = new double[attInfo.size()];
        testInstance = new Instance(1,cVector);
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
    public boolean estimate(Node node, double[] result) {
        // create the attribute vector
        makeVector(node,cVector);
        try
        {
            // get the prediction from the weka classifier and copy over the answer to the results array
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
    	sb.append(getShortName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append(classifier.toString());

    	return sb.toString();
    }
}
