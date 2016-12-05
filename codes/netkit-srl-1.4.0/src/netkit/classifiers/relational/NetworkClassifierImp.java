/**
 * NetworkClassifierImp.java
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
import netkit.classifiers.aggregators.Aggregator;
import netkit.classifiers.aggregators.AggregatorFactory;
import netkit.graph.*;
import netkit.util.Configuration;
import netkit.util.VectorMath;
import netkit.util.ArrayUtil;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Core implementation of the NetworkClassifier (and Classifier) interface.  All methods that
 * are generic have been implemented (although they can be overridden to be customized as
 * necessary).  It extends the core implementation of the basic Classifier interface
 * (ClassifierImp) and implements only the methods that are specifically relational in
 * nature.
 *
 * <p>
 * The only four methods that any subclass <i>need</i> to implement are:
 * <ul>
 * <li><code>public boolean doEstimate(Node n, double[] result)</code>   <br>
 *     This estimates the likelihood of the given node belonging to each class.  Returns
 *     true if it estimated these or false if it abstains.
 *  <li><code>public String getName()</code><br>
 *     Returns the full name of this classifier.  e.g.: Class-Distributional Relational Classifier (cdRN)
 *  <li><code>public String getShortName()</code>
 *     Returns the short name of the classifier. e.g.: cdRN
  *  <li><code>public String getDescription()</code>  <br>
 *     Returns a description of what this classifier does.
 * </ul>
 *
 * <p>
 * In addition, the classifier ought to override the
 * <code>public void induceModel(Graph graph, DataSplit split)</code>
 * method, which does the actual learning.
 *
 * <p>
 * You may also want to override the <code>getDefaultConfiguration()</code> and
 * <code>configure(Configuration config)</code> methods if the classifier can be
 * configured in any special way (e.g., if it can take some parameters).
 *
 * <p>
 * Finally, if the classifier needs to do some book-keeping at every run of the
 * collective inference method, the you should override
 * <code>initializeRun(Estimate currPrior, Node[] unknowns)</code>
 *
 * <P>
 * <B>Properties:</B>
 * <UL>
 * <LI>useintrinsic: whether to use intrinsic attributes.  default=true
 * <LI>aggregation: what attributes to aggregate on.  default=classOnly
 * <LI>aggregators: what aggregators to use.  default=mode,ratio,mean
 * </UL>
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 *
 * @see NetworkClassifierImp#doEstimate(netkit.graph.Node, double[])
 * @see NetworkClassifierImp#getName()
 * @see NetworkClassifierImp#getShortName()
 * @see NetworkClassifierImp#getDescription()
 * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
 * @see NetworkClassifierImp#getDefaultConfiguration()
 * @see NetworkClassifierImp#configure(netkit.util.Configuration)
 * @see NetworkClassifierImp#initializeRun(netkit.classifiers.Estimate, netkit.graph.Node[])
 */
public abstract class NetworkClassifierImp
        extends ClassifierImp
        implements NetworkClassifier
{
    /**
     * Get the aggregator factory, which will be used to get the aggregators needed for
     * the classifier.
     */
    protected static AggregatorFactory aggFactory = AggregatorFactory.getInstance();

    /**
     * This keeps track of the priors for the unknown nodes.
     */
    protected Estimate prior;

    /**
     * Temporary place to put estimates. Should only be used within a method and should not
     * be expected to keep its values beyond that.
     */
    private double[] tmpResult;

    /**
     * The possible ways the relational classifier can handle aggregation.
     * <ul>
     * <li>None: No aggregation.  Just like a non-relational classifier.
     * <li>All: Aggregate everything
     * <li>ClassOnly: Aggregate only the class variable
     * <li>ExcludeClass: Aggregate everything but the class variable
     * </ul>
     */
    protected static enum Aggregation { None, All, ClassOnly, ExcludeClass };

    /**
     * What kind of aggregation should the classifier do.  Default is to
     * aggregate on everything.
     */
    protected Aggregation aggregation = Aggregation.ClassOnly;

    /**
     * This array contains the list of aggregators that this classifier will use.  These
     * are gotten from the classifier configuration.
     */
    protected String[] aggTypes = null;

    /**
     * This list contains all the aggregators for an input graph.  It will contain
     * all the aggregators that can be instantiated for all the attributes that should
     * be aggregated.
     */
    protected List<Aggregator> aggregators = new ArrayList<Aggregator>();
    /**
     * This list contains the 'dynamic' aggregators... those whose values will change
     * if the class estimates change.  These are the only ones that need to be updated
     * across iterations of the collective inference method.
     */
    protected List<Aggregator> dynamicAggregators = new ArrayList<Aggregator>();

    /**
     * This is the final estimation method that will be called and the only estimation
     * method that sub-classes should implement.
     *
     * @param node The node whose class label needs to be estimated
     * @param result The array to be filled with estimations for the class label
     * @return true if the classifier estimated the class label.  false if the classifier abstains.
     */
    protected abstract boolean doEstimate(Node node, double[] result);

    /**
     * Method to tell this object whether to include the class attribute when
     * creating the internal instance representation for relational learning.  If
     * the configuration says not to use intrinsic variables (the non-relational
     * variables), then the class attribute is also removed.  However, classifiers
     * that use WEKA underneath need to have the class attribute used.  This method
     * tells this object to create the internal data representation that includes
     * the class attribute whether the intrinsics are on or off.
     *
     * @return This always returns false.  Subclasses that always require a
     * class attribute to be included should override to return true.
     */
    protected boolean includeClassAttribute() { return false; }

    /**
     * Default configuration for relational learners.  This sets aggregation to be
     * only for the class attribute, to use intrinsic variables and to use the mode,
     * ratio and mean aggregators.
     * <P>
     * These are also the configuration objects that are set in this instance.  If a
     * classifier needs more, then it should override this (remembering to call
     * <code>super.getDefaultConfiguration()<code> if needed) to set other default
     * configuration options.
     */
    public Configuration getDefaultConfiguration() {
        Configuration conf = super.getDefaultConfiguration();
        conf.set("agregation", Aggregation.ClassOnly.toString());
        conf.set("useintrinsic",true);
        conf.set("aggregators","mode,ratio,mean");
        return conf;
    }

    /**
     * Configure the classifier.  This takes care of the type of aggregation
     * is done, whether to use intrinsic variables, and what aggregator functions
     * to use.
     *
     * @param config The Configuration object used to configure this classifier.
     *
     * @see NetworkClassifierImp#aggregation
     */
    public void configure(Configuration config) {
        super.configure(config);
        aggregation  = Aggregation.valueOf(config.get("aggregation",Aggregation.ClassOnly.toString()));
        useIntrinsic = config.getBoolean("useintrinsic",true);
        aggTypes     = config.get("aggregators","mode,ratio,mean").split(",");

        logger.config(" configure: aggregation="+aggregation);
        logger.config(" configure: useintrinsic="+useIntrinsic);
        logger.config(" configure: aggregators="+ArrayUtil.asString(aggTypes));
    }

    /**
     * This generates all the aggregator instances needed to create all
     * the aggregated values for all the attributes as directed by the configuration.
     * It populates the <code>aggregators</code> List with these aggregators, which
     * will then be used to convert an instance into a 'learning instance' by adding
     * these aggregated values.
     *
     * @see NetworkClassifierImp#aggregators
     * @see NetworkClassifierImp#dynamicAggregators
     */
    protected void generateAggregators() {
        switch(aggregation)
        {
        case None:
            // In this case, no aggregators are needed.
            break;
        case ExcludeClass:
        case All:
            EdgeType[] edgeTypes = graph.getEdgeTypes(nodeType);
            logger.fine("generateAggregators nodeType="+nodeType+" edgeTypes="+ArrayUtil.asString(edgeTypes));

            // Go through all edge types
            for(EdgeType edgeType : edgeTypes)
            {
                String destType = edgeType.getDestType();
                logger.finer("generateAggregators nodeType="+nodeType+" aggregating on edge="+edgeType.getName()+" node="+destType);

                // Go through all attributes for the node type at the other end of the current edge
                // This ensures that we aggregate through all edges and all attributes.  This
                // also means that we can aggregate on attributes more than once if multiple edge
                // types have the same destination node type.
                Attributes attribs = graph.getAttributes(destType);
                for(Attribute attrib : attribs)
                {
                    logger.finer("generateAggregators nodeType="+nodeType+" edgeType="+edgeType+" attribute="+attrib+")");
                    // skip if the classifier aggregation excludes class labels and if the current attribute
                    // is the class label
                    if(aggregation == Aggregation.ExcludeClass && attribute == attrib)
                        continue;
                    int idx = attribs.getAttributeIndex(attrib.getName());

                    // We now have a specific attribute to aggregate on
                    // so loop through all aggregators that this classifier should
                    // use and apply the ones that are relevant to the given attribute type
                    for(String aggType : aggTypes)
                    {
                        // ignore if this aggregator cannot handle the current attribute
                        if(!aggFactory.canAggregate(aggType, attrib))
                            continue;

                        logger.finer("generateAggregators nodeType="+nodeType+" edgeType="+edgeType+" attrib="+attrib+" aggregator="+aggType);

                        // If the aggregator is by value (i.e., it aggregates on a particular value of the attribute)
                        // then handle it this way
                        if(aggFactory.isByValue(aggType))
                        {
                            switch(attrib.getType())
                            {
                            case CATEGORICAL:
                                    // If the attribute is categorical, create an aggregator for each
                                    // observed or known value of the categorical attribute
                                    logger.finest(" START adding categoricals by value");
                                    for(int i=0;i<((AttributeCategorical)attrib).size();i++)
                                    {
                                        Aggregator agg = aggFactory.get(aggType, edgeType, attrib, i);
                                        logger.finest("     --- aggregator("+((AttributeCategorical)attrib).getToken(i)+": "+agg);
                                        aggregators.add(agg);
                                    }
                                    logger.finest(" DONE adding categoricals by value");
                                    break;
                            case DISCRETE:
                                    // If the attribute is discrete, then go through the graph and
                                    // find all values of that attribute (whether the node has a known
                                    // class label or not).
                                    Set<Double> seen = new HashSet<Double>(graph.numNodes(destType));
                                    for(Node n : graph.getNodes(destType))
                                    {
                                        double value = n.getValue(idx);
                                        if(Double.isNaN(value))
                                            continue;
                                        if(seen.contains(value))
                                            continue;
                                        seen.add(value);
                                        aggregators.add(aggFactory.get(aggType, edgeType, attrib, value));
                                    }
                                    break;
                            }
                        }
                        // The aggregator is not by value
                        else
                        {
                            Aggregator agg = aggFactory.get(aggType, edgeType, attrib);
                            logger.finest("  ADDING categorical aggregator: "+agg);
                            aggregators.add(agg);
                        }
                    }
                }
            }
            break;
        case ClassOnly:
            // In this case, we are only concerned about edge types start and
            // end with the node type that contain the class attribute
            // Loop through all those edge types...
            for(EdgeType et : graph.getEdgeTypes(nodeType,nodeType))
            {
                logger.finer("generateAggregators nodeType="+nodeType+" aggregating on edge="+et);
                // Go through all aggregators that are specified in the configuration
                for(String s : aggTypes)
                {
                    // skip aggregators that cannot aggregate on the class attribute
                    if(!aggFactory.canAggregate(s, attribute))
                        continue;
                    logger.finest("generateAggregators nodeType="+nodeType+" edgeType="+et+" aggregator="+s);
                    if(aggFactory.isByValue(s))
                    {
                        // class attributes can only be categoricals
                        logger.finest(" START adding categoricals by value");
                        for(int i=0;i<attribute.size();i++)
                        {
                            Aggregator agg = aggFactory.get(s, et, attribute, i);
                            logger.finest("     --- aggregator("+attribute.getToken(i)+": "+agg);
                            aggregators.add(agg);
                        }
                    }
                    else
                    {
                        Aggregator agg = aggFactory.get(s, et, attribute);
                        logger.finest("  ADDING categorical aggregator: "+agg);
                        aggregators.add(agg);
                    }
                }
            }
            break;
        }

        // Finally, go through all the newly created aggregators, identyfing the one's
        // that aggregate on the class attribute.  Add these to the 'dynamicAggregators' list
        for(Aggregator agg : aggregators)
        {
            if(agg.getAttribute() == attribute)
                dynamicAggregators.add(agg);
        }
    }

    /**
     * This method induces a new prediction model.  Any subclass should remember
     * to call <code>super.induceModel(graph,split)</code> to ensure that all internal
     * variables have been set.
     * <p>
     * This method sets up crucial information needed for internal use.  Of general interest,
     * it resets the aggregators list to the new list of aggregators (by calling
     * <code>generateAggregators()</code> and sets certain internal variables such as
     * tmpVector and vectorClsIdx.
     *
     * @param graph The graph over which to induce a model
     * @param split The datasplit which informs us which nodes have known class labels and which do not
     *
     * @see NetworkClassifierImp#generateAggregators()
     * @see NetworkClassifierImp#aggregators
     * @see netkit.classifiers.ClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     * @see netkit.classifiers.ClassifierImp#tmpVector
     * @see netkit.classifiers.ClassifierImp#vectorClsIdx
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        aggregators.clear();
        Node[] trainingSet = split.getTrainSet();
        if(trainingSet == null || trainingSet.length == 0)
            return;
        generateAggregators();

        int numAttrib = aggregators.size();
        if(useIntrinsic)
        {
            numAttrib += graph.getAttributes(nodeType).attributeCount();
            // we do not want to include key values in the tmpVector
            if(keyIndex!=-1)
                numAttrib--;
        }
        else if(includeClassAttribute())
        {
            vectorClsIdx = 0;
            numAttrib++;
        }
        else
        {
            vectorClsIdx = -1;
        }

        tmpResult = tmpVector;
        tmpVector = new double[numAttrib];
    }
    
    
    protected String[] getAttributeNames() {
    	ArrayList<String> attribs = new ArrayList<String>();
    	for(String s : super.getAttributeNames())
    		attribs.add(s);

        if(!useIntrinsic && includeClassAttribute())
        {
        	attribs.add(attribute.getName()+"["+attribute.getType()+"]");
        }

        for(Aggregator a : aggregators)
        {
        	attribs.add(a.getName()+"["+a.getType()+"]");
        }
        return attribs.toArray(new String[0]);
    }

    
    protected void makeVector(Node node, double[] vector) {
        super.makeVector(node,vector);
        int offset = 0;
        if(useIntrinsic)
        {
            double[] nv = node.getValues();
            offset = nv.length;
            if(keyIndex!=-1)
                offset--;
        }
        else if(includeClassAttribute())
        {
            vector[offset] = node.getValue(clsIdx);
            offset++;
        }

        for(Aggregator a : aggregators)
        {
            vector[offset] = a.getValue(node,prior);
            offset++;
        }
    }

    /**
     * This is called prior to predicting labels for the unknown labels in the graph,
     * in case the classifier needs to initialize itself.  This is called at the beginning
     * of every iteration of a collective inference run.  This method does nothing and
     * should be overridden if a particular classifier does need to do something specific.
     *
     * @param currPrior The current 'priors' or estimates of the unknown lables
     * @param unknowns The list of nodes which are to be predicted in the upcoming run.
     */
    public void initializeRun(Estimate currPrior, Node[] unknowns) { }

    /**
     * Classify a given node into one of the given classes.  It may use the class estimations
     * of other nodes and may update the prior of the given node.
     * <p>
     * This method is final and calls the
     * <code>estimate(Node,Estimate,double[],boolean)</code> method with <code>false</code>
     * as the boolean as this takes care of that boolean if neccessary (by updating the
     * prior object).  It also notifies any listeners that the label of this node has
     * been predicted.
     *
     * @param node The node to classify.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the predicted classification.
     * @return The index of the class that this node is classified as.  It returns -1 if it abstains.
     *
     * @see NetworkClassifierImp#estimate(netkit.graph.Node, netkit.classifiers.Estimate, double[], boolean)
     * @see netkit.classifiers.ClassifierImp#notifyListeners(netkit.graph.Node, int)
     */
    public final int classify(Node node, Estimate prior, boolean updatePrior) {
        if(!estimate(node,prior,tmpResult,false))
        {
            if(updatePrior)
                prior.estimate(node,null);
            return -1;
        }
        int result = VectorMath.getMaxIdx(tmpResult);
        if(updatePrior)
            prior.classify(node,result);
        notifyListeners(node,result);
        return result;
    }

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     * <p>
     * This method is final and sets the prior object before it calls
     * <code>estimate(Node,double[])</code> to do the actual estimate.
     * It takes care of the prior if necessary (by updating the prior object)
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param result The array that is filled in with class estimates
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     *
     * @see NetworkClassifierImp#estimate(netkit.graph.Node, double[])
     * @see NetworkClassifierImp#prior
     */
    public final boolean estimate(Node node, Estimate prior, double[] result, boolean updatePrior) {
        this.prior = prior;
        boolean e = estimate(node,result);
        if(updatePrior)
            prior.estimate(node,( e ? result : null) );
        return e;
    }

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     * <p>
     * This method is final and calls the
     * <code>estimate(Node,Estimate,double[],boolean)</code> method using a temporary
     * double array, which is returned if the classifier does not abstain.  If it abstains
     * (the called estimate method returns false), then this method returns null.
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return An array that is filled in with class estimates.  This is <code>null</code> is the classifier abstains
     *
     * @see NetworkClassifierImp#estimate(netkit.graph.Node, netkit.classifiers.Estimate, double[], boolean)
     */
    public final double[] estimate(Node node, Estimate prior, boolean updatePrior) {
        double[] result = new double[attribute.size()];
        boolean e = estimate(node,prior,result,updatePrior);
        return (e ? result : null);
    }

    /**
     * Estimate the probabilities that a given node into belongs to any given class
     * It may use the class estimations of other nodes and may update the prior of
     * the given node.
     * <p>
     * This method is final and calls the
     * <code>estimate(Node,Estimate,double[],boolean)</code> method.
     *
     * @param node The node to estimate.
     * @param prior The current class estimates of all initially unknown nodes.
     * @param result The Estimate object that is updated with class estimates.
     * @param updatePrior Whether the classifier should update the prior of the node that it classifies.  If true, then the
     *                    prior object is updated with the new estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     *
     * @see NetworkClassifierImp#estimate(netkit.graph.Node, netkit.classifiers.Estimate, double[], boolean)
     */
    public final boolean estimate(Node node, Estimate prior, Estimate result, boolean updatePrior) {
        boolean predicted = estimate(node,prior,tmpResult,updatePrior);
        result.estimate(node,( predicted ? tmpResult : null) );
        return predicted;
    }

    /**
     * Estimate the probabilities that a given node into belongs to any given class.
     * <p>
     * This method is final and calls the
     * <code>doEstimate(Node,Estimate,double[],boolean)</code> method.  It also notifies any listeners
     * that the label for this node has been estimated.
     *
     * @param node The node to estimate.
     * @param result The Estimate object that is updated with class estimates.
     * @return Whether the classifier abstained or inferred class probabilities
     *
     * @see NetworkClassifierImp#doEstimate(netkit.graph.Node, double[])
     * @see netkit.classifiers.ClassifierImp#notifyListeners(netkit.graph.Node, double[])
     */
    public final boolean estimate(Node node, double[] result) {
        boolean e = doEstimate(node, result);
        notifyListeners(node,result);
        return e;
    }
}
