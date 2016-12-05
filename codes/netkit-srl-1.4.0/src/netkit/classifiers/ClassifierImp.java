/**
 * ClassifierImp.java
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

package netkit.classifiers;

import netkit.graph.Attribute;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.AttributeCategorical;
import netkit.graph.Attributes;
import netkit.util.Configuration;
import netkit.util.VectorMath;
import netkit.util.NetKitEnv;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * $Id: ClassifierImp.java,v 1.6 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 * <p/>
 * User: smacskassy
 * Date: Dec 2, 2004
 * Time: 9:16:44 PM
 */
public abstract class ClassifierImp implements Classifier {
    public final Logger logger = NetKitEnv.getLogger(this);

    private transient Set<ClassifierListener> listeners = new HashSet<ClassifierListener>();
    private boolean notify=true;
    protected double[] tmpVector = null;

    protected AttributeCategorical attribute;
    protected String nodeType;
    protected Graph graph;
    protected int clsIdx = -1;
    protected int vectorClsIdx = -1;
    protected int keyIndex = -1;
    protected int right = -1;
    protected boolean useIntrinsic = true;

    protected double[] classPrior = null;

    public void reset() {}

    public Logger getLogger() { return logger; }
    
    public Configuration getDefaultConfiguration() {
        return new Configuration();
    }
    public void configure(Configuration config) {
        logger.finest("configure: "+config);
        useIntrinsic = true;
    }

    public final double[] estimate(Node node) {
        if(estimate(node,tmpVector))
        {
            notifyListeners(node,tmpVector);
            return tmpVector.clone();
        }
        return null;
    }
    public final boolean estimate(Node node, Estimate result) {
        boolean predicted = estimate(node,tmpVector);
        result.estimate(node, (predicted ? tmpVector : null ));
        return predicted;
    }

    public final int classify(Node node)  {
        if(!estimate(node,tmpVector))
            return -1;
        int result = VectorMath.getMaxIdx(tmpVector);
        notifyListeners(node,result);
        return result;
    }
    public final boolean classify(Node node, Classification result) {
        int res = classify(node);
        if(res == -1)
            result.setUnknown(node);
        else
            result.set(node, res);
        return (res != -1);
    }

    public void induceModel(Graph graph, DataSplit split) {
        reset();
        nodeType    = split.getView().getNodeType();
        attribute   = split.getView().getAttribute();
        clsIdx      = split.getView().getAttributeIndex();
        this.graph  = graph;
        classPrior  = split.getPrior();

        Attributes  attribs = graph.getAttributes(nodeType);
        int numAttrib = attribs.attributeCount();
        // we do not want to include key values in the tmpVector
        keyIndex    = attribs.getKeyIndex();
        vectorClsIdx = clsIdx;
        if(keyIndex!=-1)
        {
            numAttrib--;
            right = numAttrib-keyIndex;
            if(vectorClsIdx>keyIndex)
                vectorClsIdx--;
        }
        tmpVector   = new double[attribute.size()];
        logger.finest("WEKA Class index = "+vectorClsIdx);
        if(!useIntrinsic)
            vectorClsIdx = 0;
    }
    
    protected String[] getAttributeNames() {
    	ArrayList<String> attribs = new ArrayList<String>();
        if(useIntrinsic)
        {
        	Attributes a = graph.getAttributes(nodeType);
        	for(int i=0; i<a.attributeCount();i++) {
        		if(i==keyIndex)
        			continue;
        		Attribute attr = a.getAttribute(i);
        		attribs.add(attr.getName()+"["+attr.getType()+"]");
        	}
        }
        return attribs.toArray(new String[0]);
    }

    protected void makeVector(Node node, double[] vector) {
        if(useIntrinsic)
        {
            double[] nv = node.getValues();
            if(keyIndex==-1)
                System.arraycopy(nv,0,vector,0,nv.length);
            else
            {
                // do not include the key in this vector
                System.arraycopy(nv,0,vector,0,keyIndex);
                System.arraycopy(nv,keyIndex+1,vector,keyIndex,right);
            }
        }
    }



    public final void addListener(ClassifierListener cl) {
    	listeners.add(cl);
    }
    public final void clearListeners() {
    	listeners.clear();
    }
    public final void removeListener(ClassifierListener cl) {
    	listeners.remove(cl);
    }

    public final boolean getNofifyListeners() {
    	return notify;
    }
    public final void setNofityListeners(boolean notify) {
    	this.notify = notify;
    }
    public final void notifyListeners(Node node, double[] estimate) {
        if(!notify)
            return;
	for (ClassifierListener cl : listeners)
            cl.estimate(node,estimate);
    }
    public final void notifyListeners(Node node, int classification) {
        if(!notify)
            return;
	for (ClassifierListener cl : listeners)
            cl.classify(node,classification);
    }
}
