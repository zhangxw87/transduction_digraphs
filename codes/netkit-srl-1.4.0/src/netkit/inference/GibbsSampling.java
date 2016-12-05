/**
 * GibbsSampling.java
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
 * $Id: GibbsSampling.java,v 1.6 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:03:14 AM
 */
package netkit.inference;

import netkit.util.Configuration;
import netkit.util.VectorMath;
import netkit.util.NetKitEnv;
import netkit.graph.Node;
import netkit.classifiers.relational.NetworkClassifier;
import netkit.classifiers.Estimate;

import java.util.Iterator;
import java.util.Arrays;
import java.util.logging.Logger;

public class GibbsSampling extends InferenceMethod
{
    private final Logger logger = NetKitEnv.getLogger(this);

    protected int[][] chains;

    private Estimate tmpEstimate = null;
    private int burnin = 0;
    private int numChains = 0;
    private int gIterations = 0;
    private int iteration = 0;
    double[][] counts = null;

    public String getShortName() {
	    return "Gibbs";
    }
    public String getName() {
	    return "GibbsSampling";
    }
    public String getDescription() {
	    return "Gibbs Sampling!";
    }

    public Configuration getDefaultConfiguration() {
        Configuration dCfg = super.getDefaultConfiguration();
        dCfg.set("numchains",10);
        dCfg.set("burnin",200);
        dCfg.set("numit",2000);
        return dCfg;
    }
    public void configure(Configuration config) {
        super.configure(config);
        try
        {
            burnin        = config.getInt("burnin",200);
            gIterations   = config.getInt("numit",2000);
            numChains     = config.getInt("numchains",10);
        }
        catch(NumberFormatException nfe)
        {
            throw new RuntimeException("Failed to initialize "+this.getClass().getName(),nfe);
        }
        if(gIterations < 1) gIterations = 2000;
        if(burnin < 1) burnin = 200;
        if(numChains < 1) numChains = 10;
        numIterations = gIterations+burnin;
        logger.config("   "+this.getClass().getName()+" configure: burnin="+burnin);
        logger.config("   "+this.getClass().getName()+" configure: gibbsIterations="+gIterations);
        logger.config("   "+this.getClass().getName()+" configure: numchains="+numChains);

        chains = new int[numChains][];
    }
    public void reset(Iterator<Node> unknowns) {
        super.reset(unknowns);
        chains[0] = new int[unknown.length];
        for(int j=0;j<unknown.length;j++)
            chains[0][j] = j;
        for(int i=1;i<numChains;i++)
        {
            chains[i] = new int[unknown.length];
            System.arraycopy(chains[0],0,chains[i],0,unknown.length);
        }
        for(int i=0;i<numChains;i++)
            VectorMath.randomize(chains[i]);

        tmpEstimate = new Estimate(currPrior.getGraph(), currPrior.getNodeType(), currPrior.getAttribute());

    	for (Node n : unknown)
        {
            int pIdx = currPrior.sampleEstimateIdx(n);
            tmpEstimate.estimate(n,idMatrix[pIdx]);
        }

        iteration = 0;
        if(counts == null || counts.length<unknown.length)
            counts = new double[unknown.length][];
        if(counts[0] == null || counts[0].length < tmpPredict.length)
        {
            for(int n=0;n<unknown.length;n++)
                counts[n] = new double[tmpPredict.length];
        }
        for(int n=0;n<unknown.length;n++)
            Arrays.fill(counts[n],0);
    }

    public Estimate getCurrentEstimate() {
        for(int i=0;i<unknown.length;i++)
        {
            currPrior.estimate(unknown[i],counts[i]);
            currPrior.normalize(unknown[i]);
        }
        return super.getCurrentEstimate();
    }

    public boolean iterate(NetworkClassifier networkClassifier) {
        iteration++;
        if(iteration==burnin)
        {
            for(int i=0;i<counts.length;i++)
                Arrays.fill(counts[i],0);
        }

        for(int[] nodes : chains)
        {
            for(int idx : nodes)
            {
                Node node = unknown[idx];
                if(networkClassifier.estimate(node,tmpEstimate,tmpPredict,true))
                {
                    int pIdx = tmpEstimate.sampleEstimateIdx(node);
                    if(pIdx != -1)
                    {
                        tmpEstimate.estimate(node,idMatrix[pIdx]);
                        counts[idx][pIdx]++;
                    }
                }
            }
        }
        return true;
    }
}
