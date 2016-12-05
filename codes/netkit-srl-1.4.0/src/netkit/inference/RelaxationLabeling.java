/**
 * RelaxationLabeling.java
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
 * $Id: RelaxationLabeling.java,v 1.5 2007/03/26 23:45:07 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:02:26 AM
 */
package netkit.inference;

import netkit.classifiers.Estimate;
import netkit.classifiers.relational.NetworkClassifier;
import netkit.util.ArrayUtil;
import netkit.util.Configuration;
import netkit.util.VectorMath;
import netkit.util.NetKitEnv;
import netkit.graph.Node;

import java.util.Iterator;
import java.util.logging.Level;

public class RelaxationLabeling extends InferenceMethod {
    private Estimate tmpEstimate=null;
    private double decay=0.99;
    private double beta0=1;
    private double beta=1;

    public Configuration getDefaultConfiguration() {
        Configuration dCfg = super.getDefaultConfiguration();
        dCfg.set("numit",99);
        dCfg.set("beta",1.0);
        dCfg.set("decay",0.99);
        return dCfg;
    }
    public void configure(Configuration config) {
        super.configure(config);
        try
        {
            beta0 = config.getDouble("beta",1);
            decay = config.getDouble("decay",0.99);
        }
        catch(NumberFormatException nfe)
        {
            throw new RuntimeException("Failed to configure "+this.getClass().getName(),nfe);
        }
        if(beta0<0) beta0 = 0;
        if(beta0>1) beta0 = 1;
        beta = beta0;
        if(decay<0||decay>1) decay = beta;
        logger.config("   "+this.getClass().getName()+" configure: beta="+beta);
        logger.config("   "+this.getClass().getName()+" configure: decay="+decay);
    }

    public void reset(Iterator<Node> unknowns) {
        super.reset(unknowns);
        tmpEstimate = new Estimate(currPrior);
        beta = beta0;
    }

    public String getShortName() {
	    return "RelaxationLabeling";
    }
    public String getName() {
	    return "RelaxationLabeling";
    }
    public String getDescription() {
	    return "Classifies unknowns by estimating all unknowns at the same time, iterating until some stopping criterion is met";
    }

    public final void setBeta0(double beta0) {
    	this.beta0 = beta;
    }
    public final double getBeta0() {
    	return beta0;
    }

    public final void setBeta(double beta) {
    	this.beta = beta;
    }
    public final double getBeta() {
    	return beta;
    }

    public final void setDecay(double decay) {
    	this.decay = decay;
    }
    public final double getDecay() {
    	return decay;
    }

    public boolean iterate(NetworkClassifier networkClassifier) {
	for (Node n : unknown)
        {
            double[] oldEstimate = currPrior.getEstimate(n);
            if(networkClassifier.estimate(n, currPrior, tmpEstimate, false))
            {
                double[] result = tmpEstimate.getEstimate(n);
                if(result != null && beta<1)
                {
                    VectorMath.merge(beta,oldEstimate,result,result);
                }
                if(logger.isLoggable(Level.FINEST))
                {
                    logger.finest("BeliefProp-node-"+n.getIndex()+"="+ArrayUtil.asString(result));
                    logger.finest("   tmpEstimate=["+tmpEstimate.getEstimate(n)+"]="+ArrayUtil.asString(tmpEstimate.getEstimate(n))+")");
                }
            }
        }
        beta *= decay;

        // Now let's swap, so that currPrior is the latest estimate
        Estimate e = currPrior;
        currPrior = tmpEstimate;
        tmpEstimate = e;
        if(logger.isLoggable(Level.FINEST))
            logger.finest("BeliefProp-iterate: currPrior estimates"+NetKitEnv.newline+currPrior.toString());
        return true;
    }
}
