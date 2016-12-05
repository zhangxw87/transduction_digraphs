/**
 * WeightedVoteRelationalNeighbor.java
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

import netkit.graph.Edge;
import netkit.graph.Node;
import netkit.graph.Graph;
import netkit.util.Configuration;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;
import netkit.util.LaplaceCorrection;
import netkit.classifiers.DataSplit;
import netkit.classifiers.Estimate;

import java.util.Arrays;

/**
 * weighted-vote Relational Neighbor Classifier (wvRN).
 *
 * <B>Reference:</B>
 * <UL>
 * <LI> Sofus A. Macskassy, Foster Provost (2007).
 *      Classification in Networked Data: A toolkit and a univariate case study.
 *      Journal of Machine Learning, 8(May):935-983, 2007.
 * </UL>
 *
 * NOTE: This builds a model only on the neighborhood class labels.
 * Parameters:
 * <BLOCKQUOTE><CODE>
 *        <name>.laplace=None<BR>
 *        <name>.laplaceone=false<BR>
 *        <name>.lfactor=1.0<BR>
 *        <name>.rfactor=0.0
 * </CODE></BLOCKQUOTE>
 * 
 * Equivalent hard-coded parameters:
 * <BLOCKQUOTE><CODE>
 *        <name>.aggregation=ClassOnly<BR>
 *        <name>.aggregators=count
 * </CODE></BLOCKQUOTE>
 *
 * @author Sofus A. Macskassy
 */
public final class WeightedVoteRelationalNeighbor extends NetworkClassifierImp
{
    // whether to use laplace correction during the first iteration only
    private boolean laplaceonce = false;

    // what kind of laplace correction to use
    private LaplaceCorrection laplace = LaplaceCorrection.None;

    // how much to use neighbors ratio of weight to this node versus this node's ratio of weight to neighbor
    private double rfactor = 0.0D;

    // laplace correction factor
    private double lfactor = 1.0D;

    // how many collective inference iterations have been going on
    private int numInit = 0;

    // an external prior/estimate
    private Estimate external = null;

    // cached reverse edges
    private double[][] weights = null;

    // init... ?
    private double[] init;

    /**
     * @return return &quot;wvRN&quot;
     */
    @Override
    public String getShortName() {
	    return "wvRN";
    }
    /**
     * @return return &quot;Weighted Vote Relational Neighbor NetworkClassifier (wvRN)&quot;
     */
    @Override
    public String getName() {
	    return "Weighted Vote Relational Neighbor NetworkClassifier (wvRN)";
    }
    /**
     * @return return &quot;See reference: [Macskassy, Provost] 'Classification in Networked Data'&quot;
     */
    @Override
    public String getDescription() {
	    return "See reference: [Macskassy, Provost] 'Classification in Networked Data'";
    }

    
    /**
     * Creates and returns a default configuration, which only includes the laplaceonce, laplace and lfactor
     * properties (the only ones used in this classifier as nothing else is not configurable).  By default,
     * there is no laplace correction.
     *
     * @return configation object
     */
    @Override
    public Configuration getDefaultConfiguration() {
        Configuration c = new Configuration();
        c.set("laplaceonce",false);
        c.set("laplace",LaplaceCorrection.None.toString());
        c.set("lfactor",1.0D);
        c.set("rfactor",0.0D);
        return c;
    }
    /**
     * Configures the classifier with respect to laplace correction: whether to have it (and what kind) and
     * whether tu use it only on the first iteration of collective inferencing.
     * @param conf Configuration object to use to configure the classifier
     */
    @Override
    public void configure(Configuration conf) {
        useIntrinsic = false;
        aggregation = Aggregation.ClassOnly;
        aggTypes = new String[]{"count"};
        laplaceonce = conf.getBoolean("laplaceonce");
        laplace = LaplaceCorrection.valueOf(conf.get("laplace",LaplaceCorrection.None.toString()));
        lfactor = conf.getDouble("lfactor",1.0D);
        rfactor = conf.getDouble("rfactor",0.0D);
        logger.config("laplaceonce="+laplaceonce);
        logger.config("laplace="+laplace);
        logger.config("lfactor="+lfactor);
        logger.config("rfactor="+rfactor);
        numInit = 0;
    }

    /**
     * This initializes wvRN for the next collective inference iteration by setting up the
     * laplace correction, if needed, for the current iteration. If the laplace correction is
     * external, then it caches the class priors on the first iteration as they are the external
     * priors.  If we should only use laplace correction the first time, then laplace correction
     * is set to None after the first iteration
     *
     * @param currPrior The current priors for all nodes in the graph
     * @param unknowns The list of nodes whose labels are unknown
     */
    @Override
    public void initializeRun(Estimate currPrior, Node[] unknowns) {
        numInit++;
        if(numInit > 1)
        {
            laplace = LaplaceCorrection.None;
            if(laplaceonce)
                Arrays.fill(init, 0.0D);
        }
        else if(laplace == LaplaceCorrection.External)
            external = new Estimate(currPrior);
    }

    /**
     * wvRN has no model, so this only initializes what needs to be done for laplace correction
     * (in addition to whatever the superclass does).
     *
     * @param graph The graph to induce a model over
     * @param split The data split identifying which nodes have known and unknown labels
     *
     * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    @Override
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph, split);
        init = new double[classPrior.length];

        switch(laplace)
        {
            case None:
                Arrays.fill(init, 0.0D);
                break;
            case ClassPrior:
                System.arraycopy(classPrior, 0, init, 0, classPrior.length);
                break;
            case External:
                break;
            case Smoothed:
                Arrays.fill(init, 1.0D/(double)(classPrior.length));
                break;
        }
        if(laplace != LaplaceCorrection.None && lfactor != 1.0D)
            VectorMath.multiply(init,lfactor);
        if(rfactor>0)
          weights = new double[graph.numNodes(split.getView().getNodeType())][];
    }

    /**
     *
     * @param node
     * @param estimation
     * @return true
     */
    @Override
    public boolean doEstimate(Node node, double[] estimation) {
        if(laplace == LaplaceCorrection.External && external != null)
        {
            System.arraycopy(external.getEstimate(node,init),0,estimation,0,estimation.length);
            if(lfactor != 1.0D)
                VectorMath.multiply(estimation,lfactor);
        }
        else
        {
            // no need to multiply this by lfactor, as that was already taken care of
            // in the induceModel method
            System.arraycopy(init,0,estimation,0,estimation.length);
        }
        
        Edge[] edges = node.getEdgesToNeighbor(node.getType());
        
        /* -- this is not currently used.  It is an early stage of using destination node's weights
        double[] rweights = null;
        if(rfactor>0 && weights[node.getIndex()] == null) {
          rweights = new double[edges.length];
          weights[node.getIndex()] = rweights;
          double ntot = node.getWeightedDegree();

          int eidx = 0;
          for(Edge e : edges) {
            Node dest = e.getDest();
            double totw = dest.getWeightedDegree();
            double rw = 0;
            for(Edge re : dest.getEdgesToNeighbor(node))
              rw += re.getWeight();
            rweights[eidx] = rfactor*rw/totw + (1.0D-rfactor)*e.getWeight()/ntot;
          }
        }
        */
        int eidx=0;
        for(Edge e : edges) {
          double rw = (rfactor==0.0D ? e.getWeight() : weights[node.getIndex()][eidx]);
          eidx++;

          Node dest = e.getDest();
          if(!dest.isMissing(clsIdx)) {
            int value = (int)dest.getValue(clsIdx);
            estimation[value]+=rw;
          } else if(prior != null) {
            double[] d = prior.getEstimate(dest);
            if(d!=null)
              for(int v=0;v<d.length;v++)
                estimation[v] += rw*d[v];
          }
        }
        
        VectorMath.normalize(estimation);
        return true;
    }

    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getShortName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("[[ Model: Weighted-vote of neighbors ]]").append(NetKitEnv.newline);
    	return sb.toString();
    }
}

