/**
 * ClassDistribRelNeighbor.java
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

import netkit.util.*;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.classifiers.DataSplit;

import java.util.Arrays;

/**
 * The Class Distributional Relational Neighbor (ClassDistributRelNeighbor) classifier
 * works by creating a 'prototypical' class vector for each class of node and then
 * estimating a label for a new node by calculating how near that new node is to each
 * of these 'class reference vectors'.
 * <P>
 * <B>Properties:</B>
 * <UL>
 * <LI>distance: the distance function to use.  default=cosine.  See <code>distance.properties</code>
 * <LI>useintrinsic: whether to use intrinsic attributes.  default=false
 * <LI>aggregation: what attributes to aggregate on.  default=classOnly
 * <LI>aggregators: what aggregators to use.  default=ratio
 * </UL>
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class ClassDistribRelNeighbor extends NetworkClassifierImp
{
    // factory to get a distance function object
    private static Factory<DistanceMeasure> distances = new Factory<DistanceMeasure>("distance");

    // the learned class-conditional distribution vectors
    private double[][] classVectors = null;

    // the distance metric to use
    private DistanceMeasure dist = null;

    /**
     * Get the detault configuration of using a cosine distance function, and aggregating
     * only on the class attribute using the ratio aggregator.  This is in addition to any
     * defaults set by the superclass.
     *
     * @return a Configuration object
     *
     * @see NetworkClassifierImp#getDefaultConfiguration()
     */
    public Configuration getDefaultConfiguration() {
        Configuration dCfg = super.getDefaultConfiguration();
        dCfg.set("distance","cosine");
        dCfg.set("useintrinsic","false");
        dCfg.set("aggregation", Aggregation.ClassOnly.toString());
        dCfg.set("aggregators","ratio");
        return dCfg;
    }

    /**
     * Configure this classifier object.  All but the distance function is taken care of by
     * the super class.  The distance function is gotten by using a Factory class on the
     * <code>distance.properties</code> file, so the name of the distance function should
     * appear in that file.
     *
     * @param config The Configuration object used to configure this classifier.
     *
     * @see NetworkClassifierImp#configure(netkit.util.Configuration)
     */
    public void configure(Configuration config) {
        super.configure(config);
        try
        {
            dist = distances.get(config.get("distance","cosine"));
        }
        catch(NumberFormatException nfe)
        {
            throw new RuntimeException("Failed to initialize "+this.getClass().getName(),nfe);
        }
        logger.config(this.getClass().getName()+" configure: distance="+dist.getClass().getName());
    }

    /**
     * @return 'classDistribRN'
     */
    public String getShortName() {
	    return "classDistribRN";
    }
    /**
     * @return 'Class-Distributional Relational Neighbor NetworkClassifier (classDistribRN)'
     */
    public String getName() {
	    return "Class-Distributional Relational Neighbor NetworkClassifier (classDistribRN)";
    }

    /**
     * @return 'No description yet'
     */
    public String getDescription() {
	    return "No description yet";
    }

    /**
     * Induce the cdRN model by finding the 'prototypical' neighborhood for each class
     * of nodes.  By default, this neighborhood consists only of the neighboring class
     * labels, but this can easily include any intrinsic or aggregate attributes.
     *
     * @param graph Graph whose nodes are to be estimated
     * @param split The split between training and test.  Used to get the nodetype and class attribute.
     *
     * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph,split);

        classVectors = new double[attribute.size()][tmpVector.length];
        for(int i=0;i<attribute.size();i++)
            Arrays.fill(classVectors[i],0);

        // generate a vector of neighbors per class
        int[]    clsCount = new int[attribute.size()];
        Arrays.fill(clsCount,0);
        for(Node node : split.getTrainSet())
        {
            if(node.isMissing(clsIdx))
                continue;
            int clsVal = (int)node.getValue(clsIdx);
            makeVector(node,tmpVector);
            if(!Double.isNaN(tmpVector[0])) // is isNaN, then there were no known neighbors
                VectorMath.add(classVectors[clsVal],tmpVector);
            clsCount[clsVal]++;
        }

        // Now divide each classVector by the number of times that class
        // was found to get the 'prototypical' or average class vector.
        for(int i=0;i<attribute.size();i++)
        {
            if(clsCount[i] == 0)
                classVectors[i] = null;
            else
                VectorMath.divide(classVectors[i],clsCount[i]);
            logger.fine(getShortName()+" - RV["+attribute.getToken(i)+"]="+ArrayUtil.asString(classVectors[i]));
        }
    }

    /**
     * Estimate how near this node's neighborhood is to each of the class vectors using
     * a user-specified distance function (cosine by default) and normalize to produce
     * a pseudo distribution.
     *
     * @param node The node to estimate class probabilities for
     * @param result the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return true
     *
     * @see NetworkClassifierImp#makeVector(netkit.graph.Node, double[])
     */
    public boolean doEstimate(Node node, double[] result) {
        makeVector(node,tmpVector);
        if(Double.isNaN(tmpVector[0]) || VectorMath.sum(tmpVector) == 0)
        {
            logger.warning("cdRN-node-"+node.getIndex()+"("+node+") - empty cvCount - using classPrior");
            System.arraycopy(classPrior,0,result,0,classPrior.length);
        }
        else
        {
            for(int c=0;c<classVectors.length;c++)
            {
                if(classVectors[c] == null)
                    result[c] = 0;
                else
                {
                	for(int i=0;i<tmpVector.length;i++) {
                    	if(Double.isNaN(tmpVector[i]) || Double.isInfinite(tmpVector[i]))
                    		tmpVector[i] = 0;
                    }
                    result[c] = dist.distance(classVectors[c],tmpVector);
                    if(Double.isNaN(result[c]) || Double.isInfinite(result[c]))
                    {
                        logger.warning("class["+c+"] - estimate is NaN/infinite? - clsV="+ArrayUtil.asString(classVectors[c])+" cmpV="+ArrayUtil.asString(tmpVector)+"]");
                        result[c] = 0.0;
                    }
                }
            }
            VectorMath.normalize(result);
        }
        logger.finer(" estimate="+ArrayUtil.asString(result));
        return true;
    }
    
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("-------------------------------------").append(NetKitEnv.newline);
    	sb.append("Distance function: ").append(dist.getClass().getName()).append(NetKitEnv.newline);
    	String[] attrNames = getAttributeNames();
        for(int c=0;c<classVectors.length;c++)
        {
        	sb.append("---------------------------------").append(NetKitEnv.newline);
        	sb.append("Vector[").append(attribute.getToken(c)).append("]:").append(NetKitEnv.newline);
        	sb.append("---------------------------------").append(NetKitEnv.newline);
            if(classVectors[c] == null) {
            	sb.append("Null").append(NetKitEnv.newline);
            } else if(classVectors[c].length!=attrNames.length) {
            	sb.append("Bad meta data ("+attrNames.length+" attribute names, but "+classVectors[c].length+" weights).").append(NetKitEnv.newline);
            } else {
            	for(int i=0;i<classVectors[c].length;i++) {
            		sb.append(attrNames[i]).append(":").append(classVectors[c][i]).append(NetKitEnv.newline);
            	}
            }
        }
        
    	return sb.toString();
    }
}

