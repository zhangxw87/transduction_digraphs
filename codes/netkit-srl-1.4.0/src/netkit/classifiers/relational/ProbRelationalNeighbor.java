/**
 * ProbRelationalNeighbor.java
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

import netkit.graph.Node;
import netkit.graph.Edge;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;
import netkit.util.ArrayUtil;
import netkit.util.Configuration;

/**
 * This is a probablistic version of wbRN and it estimates nodes by using a Bayesian
 * combination of the neighbors edges.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 *
 * @see netkit.classifiers.relational.WeightedVoteRelationalNeighbor
 */
public final class ProbRelationalNeighbor extends NetworkClassifierImp {
    private static final double EPSILON = 0.000001; // ad hoc low value

    /**
     * @return &quot;pRN&quot;
     */
    public String getShortName() {
	    return "pRN";
    }
    /**
     * @return &quot;Probablistic Relational Neighbor NetworkClassifier (pRN)&quot;
     */
    public String getName() {
	    return "Probablistic Relational Neighbor NetworkClassifier (pRN)";
    }
    public String getDescription() {
	    return "See reference: [Macskassy, Provost] 'Classification in Networked Data'";
    }

    /**
     * @return an empty configuration
     */
    public Configuration getDefaultConfiguration() {
        return new Configuration();
    }
    /**
     * This does not use the configuration.  It configures pRN to the only
     * way it works: no intrinsics, no aggregation (because it uses its own
     * aggregation rather than what the super class does), and it uses the
     * 'ratio' aggregator although that is also ignored here.
     *
     * @param conf ignored
     */
    public void configure(Configuration conf) {
        useIntrinsic = false;
        aggregation = Aggregation.None;
        aggTypes = new String[]{"ratio"};
    }

    /**
     * Estimate the label of this node by using a naive Bayesian combination
     * of the neighbor nodes.
     *
     * @param node The node whose label to estimate
     * @param estimation Array to put the result of the estimate
     * @return true
     */
    public boolean doEstimate(Node node, double[] estimation) {
        Edge[] edges = node.getEdgesToNeighbor(node.getType());
        java.util.Arrays.fill(estimation,1);
	    for (Edge e : edges)
        {
            double clsVal = e.getDest().getValue(clsIdx);
            if(!Double.isNaN(clsVal))
            {
                double mulN = Math.pow(EPSILON,e.getWeight());
                for(int i=0;i<estimation.length;i++)
                    if(i != clsVal)
                        estimation[i] *= mulN;
            }
            else
            {
                double[] nEstimate = prior.getEstimate(e.getDest());
                if(nEstimate == null)
                    continue;
                for(int c=0;c<nEstimate.length;c++)
                {
                    double pos = nEstimate[c];
                    if(pos < EPSILON) pos = EPSILON;
                    double neg = 1-pos;
                    if(neg < EPSILON) neg = EPSILON;
                    estimation[c] *= Math.pow(pos,e.getWeight());
                    double mulN = Math.pow(neg,e.getWeight());
                    for(int i=0;i<estimation.length;i++)
                    {
                        if(i != c)
                            estimation[i] *= mulN;
                    }
                }
            }
        }
        VectorMath.normalize(estimation);
        logger.finest("  pRN-node-"+node.getIndex()+"="+ArrayUtil.asString(estimation));
        return true;
    }

    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getShortName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("----------------------------------------").append(NetKitEnv.newline);
    	sb.append("[[ Bad Model -- Learning still not implemented ]]").append(NetKitEnv.newline);
    	return sb.toString();
    }
}

