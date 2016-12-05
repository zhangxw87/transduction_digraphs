/**
 * Classification.java
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
 * $Id: Classification.java,v 1.9 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 12:21:57 AM
 */
package netkit.classifiers;

import netkit.graph.*;
import netkit.util.ArrayUtil;
import netkit.util.NetKitEnv;

import java.util.Iterator;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public final class Classification implements Iterable<Node>
{
    private final Logger logger = NetKitEnv.getLogger(this);

    private int[] classification;
    private int size;
    private double[] priors = null;
    private int majorityClass = -1;
    private final AttributeCategorical attribute;
    private final String nodeType;
    private final Graph graph;

    private class NIT implements Iterator<Node> {
        int idx=-1;
        Node[] nodes;
        public NIT() {
            nodes = graph.getNodes(nodeType);
            advance();
        }

        private void advance() {
            for(++idx ; idx < classification.length && classification[idx] == -1; idx++) ;
        }
        public boolean hasNext() {
            return idx < classification.length;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Node next() {
            if(!hasNext())
                throw new NoSuchElementException();
            Node node = nodes[idx];
            advance();
            return node;
        }
    }

    private Classification(Classification c) {
      graph = c.graph;
      nodeType = c.nodeType;
      attribute = c.attribute;
      classification = c.classification.clone();
      size = c.size;
      priors = ((c.priors==null) ? null : c.priors.clone());
      majorityClass = c.majorityClass;
    }
    
    public Classification(Estimate e) {
        this(e.getGraph(),e.getNodeType(),e.getAttribute());
        clear();
	    for (Node node : e)
		    classification[node.getIndex()] = e.getClassification(node);
    }

    public Classification(Graph graph, String nodeType, AttributeCategorical attribute) {
        if (graph == null || nodeType == null || attribute == null)
            throw new IllegalArgumentException("null parameters not allowed!");
        this.graph = graph;
        this.nodeType = nodeType;
        this.attribute = attribute;
        int numEntities = graph.numNodes(nodeType);
        logger.config("Initializing Classification(" + graph + "," + nodeType + "," + attribute + ")");
        this.classification = new int[numEntities];
        clear();
        int clsIdx = graph.getAttributes(nodeType).getAttributeIndex(attribute.getName());
        for(Node node : graph.getNodes(nodeType))
            if(!node.isMissing(clsIdx))
            {
                set(node, node.getValue(clsIdx));
                size++;
            }
    }

    public Classification asBinaryClassification(String label) {
        int cIdx = attribute.getValue(label);
        if(cIdx == -1)
            throw new RuntimeException("Class("+label+") in truth - no such class found! - classes: "+attribute.toString());
        FixedTokenSet tokens = new FixedTokenSet(new String[]{label,"not"+label});
        AttributeCategorical bMap = new AttributeFixedCategorical(attribute.getName(),tokens);
        Classification bC = new Classification(graph,nodeType,bMap);
        bC.clear();
	    for (int i=0; i<classification.length; i++)
        {
            if(classification[i] == -1)
                continue;
            int cls = ((classification[i] == cIdx) ? 0 : 1);
            bC.classification[i] = cls;
        }
        bC.size = size;
        return bC;
    }

    private void checkType(Node node) {
        if(!node.getType().equals(nodeType))
            throw new IllegalArgumentException("node["+node+"] is of wrong type.  Expected type '"+nodeType+"'");
    }

    private void add(Node node, int clsValue) {
        checkType(node);
        if(classification[node.getIndex()] == -1)
            size++;
        classification[node.getIndex()] = clsValue;
    }

    public void setUnknown(Node node) {
        checkType(node);
        classification[node.getIndex()] = -1;
        size--;
    }

    public Classification clone() {
      return new Classification(this);
    }
    
    public boolean isUnknown(Node node) {
        checkType(node);
        return (classification[node.getIndex()] == -1);
    }

    public void clear() {
        Arrays.fill(classification,-1);
        size = 0;
    }

    public void set(Node node, int clsValue) {
        if (clsValue < 0)
            setUnknown(node);
        else
            add(node, clsValue);
    }

    public void set(Node node, double clsValue) {
        set(node, (Double.isNaN(clsValue)?-1:(int)clsValue));
    }

    public int getClassValue(Node node) {
        checkType(node);
        return classification[node.getIndex()];
    }

    public int size() {
        return size;
    }

    public AttributeCategorical getAttribute() {
    	return attribute;
    }

    public Graph getGraph() {
        return graph;
    }

    public String getNodeType() {
        return nodeType;
    }

    public Iterator<Node> iterator() {
        return new NIT();
    }

    public double getBaseError() {
        return 1-getBaseAccuracy();
    }
    public double getBaseAccuracy() {
        getMajorityClass();
        return priors[majorityClass];
    }
    public int getMajorityClass() {
        getClassDistribution();
        return majorityClass;
    }
    public double[] getClassDistribution() {
        if (priors == null)
        {
            majorityClass = -1;
            priors = new double[attribute.size()];
            for (int i = 0; i < attribute.size(); i++)
                priors[i] = 0;
            int n = 0;
            for (int i = 0; i < classification.length; i++)
            {
                if (classification[i] != -1)
                {
                    priors[classification[i]]++;
                    n++;
                }
            }
            if (n > 0)
            {
                double Z = (double) n;
                for (int i = 0; i < attribute.size(); i++)
                    priors[i] /= Z;
            }
        }
        if (majorityClass == -1)
        {
            majorityClass = 0;
            for (int i = 1; i < priors.length; i++)
                if (priors[i] > priors[majorityClass]) majorityClass = i;
        }
        return priors;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Classification["+nodeType+","+attribute.getName()+"]={").append(ArrayUtil.asString(attribute.getTokens())).append("}:\n");
        sb.append("   Distrib: ").append(ArrayUtil.asString(getClassDistribution())).append('\n');
        for(int i=0;i<classification.length;i++)
        {
            if(classification[i]==-1) continue;
            sb.append("   Node[").append(i).append("]=").append(classification[i]).append('\n');
        }
        return sb.toString();
    }
}
