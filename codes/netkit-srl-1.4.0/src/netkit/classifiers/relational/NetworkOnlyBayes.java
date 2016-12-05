/**
 * NetworkOnlyBayes.java
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

import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.Edge;
import netkit.classifiers.DataSplit;
import netkit.util.ArrayUtil;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;
import netkit.util.Configuration;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;

/**
 * Network-only Bayes Classifier induces a naive Bayes model based on labels of neighbors of a node
 * and uses a Markov random field formulation when one or more neighbors have estimated labels.
 *
 * <B>References:</B>
 * <UL>
 * <LI>Soumen Chakrabarti, Byron Dom and Piotr Indyk (1998)
 *      Enhanced Hypertext Categorization Using Hyperlinks,
 *      SIGMOD, 1998
 * <LI> Sofus A. Macskassy, Foster Provost (2007).
 *      Classification in Networked Data: A toolkit and a univariate case study.
 *      Journal of Machine Learning, 8(May):935-983, 2007.
 * </UL>
 *
 * This replicates the network-only bayes as described in
 * the univariate reference paper.  This classifier applies
 * Markov Random Field techniques for dealing with uncertainty
 * in neighborhood (and local) attribute values, and can
 * therefore be used with Relaxation Labeling.   This is not
 * the case for the Weka bayes classifier, where you would
 * need the (pseudo-)certainty that is given by Iterative
 * classification and Gibbs sampling.
 *
 * NOTE: This currently takes no parameters and builds a model
 * only on the neighborhood class labels.
 * This is equivalent to these parameters:
 * <BLOCKQUOTE><CODE>
 *        <name>.aggregation=ClassOnly<BR>
 *        <name>.aggregators=??? (there is no current aggregation technique for uncertainy)<BR>
 *        <name>.useintrinsic=false
 * </CODE></BLOCKQUOTE>
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class NetworkOnlyBayes extends NetworkClassifierImp {
    private static final double EPSILON = 0.000001; // ad hoc low value

    // prior[i] = P(i)
    private double[] prior;

    // count[j][i] = how many times is class j a neighbor of class i === P(i|j)
    private double[][] count;

    // knownProbl[j][i] = the probability estimates based on neighbors whose class is known
    private double[][] knownProb = null;

    // Randomly sub-samplededges
    private Edge[] sampledEdges = null;

    // The random object to sub-sample edges
    private Random edgePicker = null;

    /**
     * @return &quot;NetworkOnlyBayes&quot;
     */
    public String getShortName() {
	    return "NetworkOnlyBayes";
    }

    /**
     * @return &quot;Network-Only Bayes NetworkClassifier with MRF internals (NetworkOnlyBayes)&quot;
     */
    public String getName() {
	    return "Network-Only Bayes NetworkClassifier with MRF internals (NetworkOnlyBayes)";
    }

    /**
     * @return &quot;See reference: [Macskassy, Provost] 'Simple Models' - nBC&quot;
     */
    public String getDescription() {
	    return "See reference: [Macskassy, Provost] 'Simple Models' - nBC";
    }

    /**
     * Create a default configuration for this classifier.  It sets the 'sampleneighbors'
     * property to -1.   It otherwise uses the configuration from the super class.
     *
     * @return a default configuration with 'sampleneighbors' set to -1.
     *
     * @see NetworkClassifierImp#getDefaultConfiguration()
     */
    public Configuration getDefaultConfiguration() {
        Configuration conf = super.getDefaultConfiguration();
        conf.set("sampleneighbors",-1);
        return conf;
    }

    /**
     * Configures this classifier.  does the super class configuration and then
     * uses 'sampleneighbors' sets the maximum number of neighbor edges to use.
     * Random sampling will be used if a node has more than this number of edges.
     *
     * @param config The configuration object to use.
     *
     * @see NetworkClassifierImp#configure(netkit.util.Configuration)
     */
    public void configure(Configuration config) {
        super.configure(config);
        int numS = config.getInt("sampleneighbors",-1);
        if(numS>0)
        {
            sampledEdges = new Edge[numS];
            edgePicker = new Random();
        }
    }

    /**
     * Resets internal variables.
     * @see netkit.classifiers.ClassifierImp#reset()
     */
    public void reset() {
        super.reset();
        knownProb = null;
    }

    /**
     * Induce the model by computing the counts for Prob(classIdx | neighborClassIdx)
     *
     * @param graph The graph to induce the model over
     * @param split The datasplit telling the classifier which nodes have known labels and which do not
     *
     * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
     */
    public void induceModel(Graph graph, DataSplit split) {
        super.induceModel(graph,split);

        knownProb = new double[graph.numNodes()][];
        Arrays.fill(knownProb, null);

        count = new double[attribute.size()][];
        this.prior = new double[attribute.size()];
        Arrays.fill(this.prior,1); // laplace smoothing ... 1/C

        for(int i=0;i<attribute.size();i++)
        {
            count[i] = new double[attribute.size()];
            Arrays.fill(count[i],1);  // laplace smoothing ... 1/C
        }

        // generate a vector of neighbors per class
        logger.finer("NoBayes:induceModel - Counting");
        for(Node node : split.getTrainSet())
        {
            if(node.isMissing(clsIdx))
                continue;
            int clsVal = (int)node.getValue(clsIdx);
            this.prior[clsVal]++;
            Edge[] edges = node.getEdgesToNeighbor(node.getType());
	        for (Edge e : edges)
            {
                if(e.getDest().isMissing(clsIdx))
                    continue;
                int oIdx = (int)e.getDest().getValue(clsIdx);
                // count[oIdx][cIdx]++;  // count[oIdx][cIdx] = Prob(cIdx | oIdx)
                count[oIdx][clsVal] += e.getWeight();
            }
        }
        // let's normalize the prior
        VectorMath.normalize(this.prior);

       // let's normalize each count
        for(int i=0;i<count.length;i++)
            VectorMath.normalize(count[i]);

        if(logger.isLoggable(Level.FINER))
            logger.finer(ArrayUtil.asString(count));
    }

    // Get the neighbors for a node -- sample if more than what is set by 'numsamples'
    private Edge[] getNeighbors(Node node) {
        Edge[] edges = node.getEdges();
        if(sampledEdges != null && sampledEdges.length < edges.length)
        {
            int num = edges.length;
            for(int i=0;i<sampledEdges.length;i++)
            {
                int idx = edgePicker.nextInt(num);
                sampledEdges[i] = edges[idx];
                num--;
                edges[idx] = edges[num];
            }
            edges = sampledEdges;
        }
        return edges;
    }

    // Get class estimates based on nodes whose neighbors have known labels.  Cache this so that we do
    // not need to recompute
    private void applyKnownNeighborEstimates(Node node, double[] estimation) {
        Edge[] edges = getNeighbors(node);
        double[] known = knownProb[node.getIndex()];
        if(known == null || sampledEdges != null)
        {
            known = new double[estimation.length];
            knownProb[node.getIndex()] = known;
            System.arraycopy(this.prior,0,known,0,this.prior.length);
	        for (Edge e : edges)
            {
                if(e.getDest().isMissing(clsIdx))
                    continue;
                int nIdx = (int)e.getDest().getValue(clsIdx);
                for(int cA=0;cA<known.length;cA++)
                {
                    double pa = known[cA];
                    known[cA]*=Math.pow(count[nIdx][cA],e.getWeight());
                    if(Double.isNaN(known[cA])||Double.isInfinite(known[cA]))
                    {
                        final StringBuilder cV = new StringBuilder();
                        cV.append(count[0][cA]);
                        for(int i=1;i<known.length;i++) cV.append(',').append(count[i][cA]);
                        throw new RuntimeException("class["+cA+"] - estimate is NaN/Infinite? - eV="+ArrayUtil.asString(known)+" prevScore="+pa+" weight="+e.getWeight()+" cV=["+cV+"]");
                    }
                }
            }
        }
        System.arraycopy(known,0,estimation,0,estimation.length);
    }

    // Apply VITERBI to compute class estimates based on neighbors whose labels are not known (but estimated)
    private void finalizeEstimate(Node node,double[] estimation) {
        Edge[] edges = getNeighbors(node);

        // Do the viterbi algorithm...
        // P(c_a|N_e) =  P(c_a)
        //     for(e_n in N_e)
        //          tmp = w(e->e_n)*P(c_a|N_e)*sum(c in C) [ P(label(e_n)=c) * P(c_a|label(e_n)) ]
        //          P(c_a|N_e) *= tmp

        StringBuilder nV = null;
        if(logger.isLoggable(Level.FINEST)) nV = new StringBuilder();
	    for (final Edge e : edges)
        {
            if(!e.getDest().isMissing(clsIdx))
                continue;

            if(logger.isLoggable(Level.FINEST)) nV.append(e.getDest().getIndex()).append('(').append(e.getDest().getName()).append(")=");
            double[] d = super.prior.getEstimate(e.getDest());
            if(d == null)
            {
                if(logger.isLoggable(Level.FINEST)) nV.append("null ");
                continue;
            }
            if(logger.isLoggable(Level.FINEST))
            {
                nV.append('<').append(d[0]);
                for(int i=1;i<d.length;i++)
                    nV.append(',').append(d[i]);
                nV.append("> ");
            }

            int idx = VectorMath.getMaxIdx(d);
            if(d[idx] == 1.0)
            {
                // neighbor is classified - apply straight bayes
                for(int cA=0;cA<estimation.length;cA++)
                {
                    double pa = estimation[cA];
                    estimation[cA]*=Math.pow(count[idx][cA],e.getWeight());
                    if(Double.isNaN(estimation[cA])||Double.isInfinite(estimation[cA]))
                    {
                        final StringBuilder cV = new StringBuilder();
                        cV.append(count[0][cA]);
                        for(int i=1;i<estimation.length;i++) cV.append(',').append(count[i][cA]);
                        throw new RuntimeException("class["+cA+"] - estimate is NaN/Infinite? - eV="+ArrayUtil.asString(estimation)+" prevScore="+pa+" weight="+e.getWeight()+" dV("+e.getDest().getIndex()+")="+ArrayUtil.asString(d)+" cV=["+cV+"]");
                    }
                }
            }
            else
            {
                // neighbor is estimated --- do viterbi
                for(int cA=0;cA<estimation.length;cA++)
                {
                    double sum = 0;
                    for(int cB=0;cB<estimation.length;cB++)
                    {
                        sum += ((d[cB]<EPSILON)?EPSILON : d[cB])*Math.pow(count[cB][cA],e.getWeight());
                    }
                    double pa = estimation[cA];
                    estimation[cA] *= sum;
                    if(Double.isNaN(estimation[cA])||Double.isInfinite(estimation[cA]))
                    {
                        final StringBuilder cV = new StringBuilder();
                        cV.append(count[0][cA]);
                        for(int i=1;i<estimation.length;i++) cV.append(',').append(count[i][cA]);
                        throw new RuntimeException("class["+cA+"] - estimate is NaN/Infinite? - eV="+ArrayUtil.asString(estimation)+" prevScore="+pa+" weight="+e.getWeight()+" sum="+sum+" dV("+e.getDest().getIndex()+")="+ArrayUtil.asString(d)+" cV=["+cV+"]");
                    }
                }
            }
        }
        if(logger.isLoggable(Level.FINEST)) logger.finest("NoBayes estimate node-"+node.getIndex()+"("+node.getName()+") raw="+ArrayUtil.asString(estimation));
        VectorMath.normalize(estimation);
        if(logger.isLoggable(Level.FINEST)) logger.finest(" normalized="+ArrayUtil.asString(estimation)+" ) neighbors="+nV);
        for(int cA=0;cA<estimation.length;cA++)
        {
            if(Double.isNaN(estimation[cA])||Double.isInfinite(estimation[cA]))
            {
                final StringBuilder eV = new StringBuilder();
                eV.append(estimation[0]);
                for(int i=1;i<estimation.length;i++) eV.append(',').append(estimation[i]);
                final StringBuilder cV = new StringBuilder();
                cV.append(count[0][cA]);
                for(int i=1;i<estimation.length;i++) cV.append(',').append(count[i][cA]);
                throw new RuntimeException("class["+cA+"] - estimate is NaN/Infinite? - eV=["+eV+"] cV=["+cV+"]");
            }
        }
    }

    /**
     * compute class estimates.
     * @param node The node to estimate class probabilities for
     * @param estimation the double array containing the probability estimates that the node belongs to each
     *               of the possible class labels.
     * @return true
     */
    public boolean doEstimate(Node node, double[] estimation) {
        applyKnownNeighborEstimates(node, estimation);
        finalizeEstimate(node, estimation);
        return true;
    }
    
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("-------------------------------------").append(NetKitEnv.newline);
    	sb.append("[[ CONDITIONALS ]]").append(NetKitEnv.newline);
        
    	return sb.toString();
    }
}

