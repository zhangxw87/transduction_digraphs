/**
 * Estimate.java
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
 * $Id: Estimate.java,v 1.7 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 12:10:33 AM
 */
package netkit.classifiers;

import netkit.util.ArrayUtil;
import netkit.util.VectorMath;
import netkit.util.NetKitEnv;
import netkit.graph.Node;
import netkit.graph.AttributeCategorical;
import netkit.graph.Graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public final class Estimate implements Iterable<Node>
{
    private final Logger logger = NetKitEnv.getLogger(this);

    private double[][] estimates;
    private int size;
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
            for(++idx ; idx < estimates.length && estimates[idx] == null; idx++) ;
        }

        public boolean hasNext() {
            return idx < estimates.length;
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

    public Estimate(Estimate e) {
        graph = e.graph;
        nodeType = e.nodeType;
        attribute = e.attribute;
        estimates = new double[e.estimates.length][];
        e.copyInto(this);
    }
    public Estimate(Graph graph, String nodeType, AttributeCategorical attribute) {
        if(graph == null || nodeType == null || attribute == null)
            throw new IllegalArgumentException("null parameters not allowed!");
        this.size = 0;
        this.graph = graph;
        this.nodeType = nodeType;
        this.attribute = attribute;
        int numEntities = graph.numNodes(nodeType);
        logger.config("Initializing Estimate(" + graph + "," + nodeType + "," + attribute + ")");
        this.estimates = new double[numEntities][];
        clear();
    }

    public Estimate(Classification labels) {
        this(labels.getGraph(), labels.getNodeType(),labels.getAttribute());
        double[] priors = labels.getClassDistribution();
	    for (Node node : labels)
        {
            if(labels.isUnknown(node))
                add(node,priors);
            else
                add(node,labels.getClassValue(node));
        }
    }

    private void checkType(Node node) {
        if(!node.getType().equals(nodeType))
            throw new IllegalArgumentException("node["+node+"] is of wrong type.  Expected type '"+nodeType+"'");
    }
    private void checkDistribution(double[] dist) {
        if(dist != null && dist.length != attribute.size())
            throw new IllegalArgumentException("distribution array is the wrong size("+((dist==null)?-1:dist.length)+") - expected "+attribute.size());
    }
    private void checkValue(int clsVal) {
        if(clsVal >= attribute.size() || clsVal < 0)
            throw new IllegalArgumentException("class value("+clsVal+") is invalid.  It must be in the range[0:"+attribute.size()+"]");
    }

    private void add(Node node, double[] estimate) {
        checkType(node);
        checkDistribution(estimate);
        if(estimate == null)
            estimates[node.getIndex()] = null;
        else if(estimates[node.getIndex()] == null)
        {
            size++;
            estimates[node.getIndex()] = estimate.clone();
        }
        else
            System.arraycopy(estimate,0,estimates[node.getIndex()],0,estimate.length);
    }
    private void add(Node node, int clsVal) {
        checkType(node);
        checkValue(clsVal);
        if(estimates[node.getIndex()] == null)
        {
            size++;
            estimates[node.getIndex()] = new double[attribute.size()];
        }
        else
        {
            Arrays.fill(estimates[node.getIndex()],0);
            estimates[node.getIndex()][clsVal] = 1;
        }
    }

    public void estimate(Node node, double[] estimate) {
        add(node,estimate);
    }

    public void classify(Node node, int clsVal) {
        add(node, clsVal);
    }

    public void clear() {
        Arrays.fill(estimates,null);
    }

    public double getScore(Node node, int clsVal) {
        double[] e = getEstimate(node);
        if(e == null || e.length <= clsVal)
            return Double.NaN;
        return e[clsVal];
    }
    public double[] getEstimate(Node node) {
        checkType(node);
        return estimates[node.getIndex()];
    }
    public double[] getEstimate(Node node, double[] defaultValue) {
        double[] e = getEstimate(node);
        return ((e == null) ? defaultValue : e);
    }

    public int sampleEstimateIdx(Node node) {
        double[] e = getEstimate(node);
        return ((e == null) ? -1 : VectorMath.sampleIdx(e));
    }

    public void normalize(Node node) {
        double[] e = getEstimate(node);
        if (e != null) VectorMath.normalize(e);
    }

    public int getClassification(Node node) {
        double[] e = getEstimate(node);
        return ((e == null) ? -1 : VectorMath.getMaxIdx(e));
    }

    public int getClassificationIdx(Node node, int defaultValue) {
        int clsIdx = getClassification(node);
        return ((clsIdx == -1) ? defaultValue : clsIdx);
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

    public Classification asClassification() {
        return new Classification(this);
    }

    public void copyInto(Estimate result) {
        if (result == null)
            throw new IllegalArgumentException("copying Estimate with empty estimates!");
        if (result.graph != graph || result.attribute != attribute || result.nodeType != nodeType)
            throw new IllegalArgumentException("copying Estimate into an estimate with a different graph/attribute/nodeType");
        if (result.estimates == null || result.estimates.length < estimates.length)
        {
            result.estimates = new double[estimates.length][];
            Arrays.fill(result.estimates,null);
            result.size = 0;
        }

        for (int i=0; i<estimates.length; i++)
        {
            if (estimates[i] == null)
                result.estimates[i] = null;
            else
                result.estimates[i] = estimates[i].clone();
        }
        result.size = size;
    }

    /**
     * Apply class mass normalization.
     * @see Zhu, X., Ghahramani, Z., & Lafferty, J. (2003). &quot;Semi-supervised learning using Gaussian fields and harmonic functions,&quot; The 20th International Conference on Machine Learning (ICML), 2003.
     * @param known
     */
    public void applyCMN(DataSplit split) {
      double[] cmn = split.getClassDistribution();
      VectorMath.multiply(cmn,split.getTrainSetSize());
      // slight Laplacian correction
      for(int i=0;i<cmn.length;i++)
        cmn[i]++;
      
      double[] d   = new double[cmn.length];
      double[] q   = new double[cmn.length];
      Arrays.fill(q,0.0D);
      Arrays.fill(d,0.0D);
      for(double[] pred : estimates)
      {
        if(pred == null)
          continue;
        VectorMath.add(q,pred);
        d[VectorMath.getMaxIdx(pred)]++;
      }
      VectorMath.normalize(d);
      
      for(int c=0;c<q.length;c++)
        cmn[c] /= q[c];
      
      logger.fine("applyCMN: preCMN distrib="+ArrayUtil.asString(d));
      logger.fine("applyCMN: cmn="+ArrayUtil.asString(cmn));
      Arrays.fill(d,0.0D);

      for(double[] pred : estimates)
      {
        if(pred == null)
          continue;
        for(int c=0;c<pred.length;c++)
          pred[c] *= cmn[c];
        VectorMath.normalize(pred);

        d[VectorMath.getMaxIdx(pred)]++;
      }
      VectorMath.normalize(d);
      logger.fine("applyCMN: afterCMN distrib="+ArrayUtil.asString(d));
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("Estimate(");
        sb.append(nodeType).append(',').append(attribute.toString()).append("):\n");
        for (int i=0; i<estimates.length; i++)
        {
            if (estimates[i] != null)
            {
                sb.append("  node-").append(i);
                sb.append('=');
                sb.append(ArrayUtil.asString(estimates[i]));
                sb.append(NetKitEnv.newline);
            }
        }
        return sb.toString();
    }
}

