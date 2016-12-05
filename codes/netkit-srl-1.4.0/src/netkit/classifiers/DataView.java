/**
 * DataView.java
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

import netkit.graph.*;
import netkit.util.VectorMath;
import netkit.util.ArrayUtil;
import netkit.util.NetKitEnv;
import netkit.util.ArrayIterator;

import java.util.*;
import java.util.logging.Logger;

public final class DataView implements Iterable<Node> {
    private final Logger logger = NetKitEnv.getLogger(this);

    private final Graph graph;
    private final String nodeType;
    private final AttributeCategorical attrib;
    private final Node[] nodes;

    private boolean graphHasMissingClassValues = false;
    private boolean replacement;
    private boolean stratified;
    private boolean pruneZeroKnowledge;
    private boolean pruneSingletons;
    private final Random pick;
    public final int clsIdx;
    private Classification truth;
    private Classification known = null;
    private double[] prior = null;
    
    private DataSampler sampler;

    private DataView(DataView view) {
        graph = view.graph;
        nodeType = view.nodeType;
        attrib = view.attrib;
        nodes = view.nodes;
        graphHasMissingClassValues = view.graphHasMissingClassValues;
        replacement = view.replacement;
        stratified = view.stratified;
        pruneZeroKnowledge = view.pruneZeroKnowledge;
        pruneSingletons = view.pruneSingletons;
        pick = view.pick;
        clsIdx = view.clsIdx;
        truth = view.truth;
        known = view.known;
        prior = view.prior;
        sampler = view.sampler.clone();
    }
    public DataView(final Graph g, final String nodeType, final AttributeCategorical attrib) {
        this(g, nodeType, attrib, VectorMath.getSeed(), false, true, true);
    }
    public DataView(final Graph g, final String nodeType, final AttributeCategorical attrib, final long seed) {
        this(g, nodeType, attrib, seed, false, true, true);
    }
    public DataView(final Graph g, final String nodeType, final AttributeCategorical attrib, final long seed, final boolean replacement, final boolean stratified, final boolean pruneZeroKnowledge) {
        this(g, nodeType, attrib, seed, replacement, stratified, pruneZeroKnowledge, true, true);
    }
    public DataView(final Graph g, final String nodeType, final AttributeCategorical attrib, final long seed, final boolean replacement, final boolean stratified, final boolean pruneZeroKnowledge, final boolean pruneSingletons, final boolean sampleUnknown) {
        this.graph = g;
        this.nodeType = nodeType;
        this.attrib = attrib;
        this.replacement = replacement;
        this.pruneSingletons = pruneSingletons;
        this.stratified = stratified;
        this.pruneZeroKnowledge = pruneZeroKnowledge;
        this.pick = new Random(seed);
        this.clsIdx = g.getAttributes(nodeType).getAttributeIndex(attrib.getName());
        
        Node[] candidateNodes = g.getNodes(nodeType);
        List<Node> acceptedNodes = null;
        if(pruneSingletons)
          acceptedNodes = new ArrayList<Node>(candidateNodes.length);
        for(Node n : candidateNodes)
        {
            if(pruneSingletons)
            {
              if(n.numEdges() == 0)
                continue;
              acceptedNodes.add(n);
            }
            
            if(n.isMissing(clsIdx))
            {
                graphHasMissingClassValues = true;
                break;
            }
        }
        nodes = ((pruneSingletons) ? acceptedNodes.toArray(new Node[0]) : candidateNodes);
        this.truth = new Classification(g, nodeType, attrib);
        sampler = new DataSampler(nodes, clsIdx, seed, replacement, stratified, sampleUnknown);
    }

    public DataView clone() {
        return new DataView(this);
    }
    
    public boolean graphHasMissingClassValues() {
        return graphHasMissingClassValues;
    }

    public Iterator<Node> iterator()
    {
        return new ArrayIterator<Node>(nodes);
    }

    protected int showNeighbors(final Node n, final int depth) {
        int num=0;

        if(depth>0)
        {
            Edge[] eArray = n.getEdgesToNeighbor(n.getType());
            for (Edge e : eArray)
            {
                Node nbr = e.getDest();
                if(nbr.isMissing(clsIdx) && !truth.isUnknown(nbr))
                {
                    num++;
                    showClassValue(nbr);
                }
                num += showNeighbors(nbr,depth-1);
            }
        }
        return num;
    }

    public int size() {
        return nodes.length;
    }
    public double[] getPrior() {
        return prior;
    }
    public void setPrior(double[] prior) {
        if(prior != null && prior.length != attrib.size())
            throw new IllegalArgumentException("passed-in prior is wrong size("+prior.length+") --- it should be "+attrib.size());
        this.prior = prior.clone();
    }
    public double[] getClassDistribution() {
        return (graphHasMissingClassValues ? null : truth.getClassDistribution() );
    }
    public boolean doReplacement() {
        return replacement;
    }
    public boolean doStratified() {
        return stratified;
    }
    public boolean doPruneZeroKnowledge() {
        return pruneZeroKnowledge;
    }
    public boolean doPruneSingletons() {
        return pruneSingletons;
    }
    public DataSplit[] crossValidate(final int numSplits) {
        logger.finer("crossValidate("+numSplits+") [stratified="+stratified+"]");
        
        Node[][][] samples = sampler.crossValidate(numSplits);
        DataSplit[] result = new DataSplit[samples.length];
        for(int i=0;i<result.length;i++)
        {
            Node[] train = samples[i][1];
            Node[] test = samples[i][0];
            result[i] = new DataSplit(this, test, train);
        }
        return result;
    }

    public Node[] sample(final double ratio) {
        logger.finer("sample(ratio="+ratio+")");
        return sample((int)(ratio*nodes.length));
    }
    public Node[] sample(final int size) {
        logger.finer("sample(size="+size+")");
        return sampler.sample(size);
    }

    public DataSplit getSplit(final Classification known) {
        if(known == null)
            throw new IllegalArgumentException("getSplit(known) --- known cannot be null!");
        logger.finer("getSplit(classification)");
        ArrayList<Node> trainN = new ArrayList<Node>(nodes.length);
        ArrayList<Node> testN = new ArrayList<Node>(nodes.length);
        for(Node node : nodes)
        {
            if(known.isUnknown(node))
                testN.add(node);
            else
                trainN.add(node);
        }
        return new DataSplit(this, testN.toArray(new Node[0]), trainN.toArray(new Node[0]));
    }
    public DataSplit getSplit(final Classification known, final Classification test) {
        if(test == null)
            return getSplit(known);
        logger.finer("getSplit(known, test)");
        ArrayList<Node> trainN = new ArrayList<Node>(nodes.length);
        ArrayList<Node> testN = new ArrayList<Node>(nodes.length);
        if(known == null)
        {
            for(Node node : nodes)
            {
                if(test.isUnknown(node))
                    trainN.add(node);
                else
                    testN.add(node);
            }
        }
        else
        {
            for(Node node : nodes)
            {
                if(!known.isUnknown(node))
                    trainN.add(node);
                if(!test.isUnknown((node)))
                    testN.add(node);
            }
        }
        return new DataSplit(this, testN.toArray(new Node[0]), trainN.toArray(new Node[0]));
    }
    public DataSplit getSplit(final NodeFilter trainFilter) {
        logger.finer("getSplit(trainingNodeFilter)");
        ArrayList<Node> train = new ArrayList<Node>(nodes.length);
        ArrayList<Node> test = new ArrayList<Node>(nodes.length);
        for(Node node : nodes)
        {
            if(trainFilter.accept(node))
                train.add(node);
            else
                test.add(node);
        }
        return new DataSplit(this, test.toArray(new Node[0]), train.toArray(new Node[0]));
    }
    public DataSplit getSplit(final int trainSize) {
        logger.finer("getSplit(trainSize="+trainSize+")");
        if(trainSize < 0 || (!replacement && trainSize > nodes.length))
            throw new IllegalArgumentException("trainSize("+trainSize+") must lie in the range [0:"+nodes.length+"]");
        return getSplit(trainSize,nodes.length-trainSize);
    }
    public DataSplit getSplit(final int trainSize, final int testSize) {
        logger.finer("getSplit(trainSize="+trainSize+",testSize="+testSize+")");
        if(trainSize < 0 || testSize < 0 || (!replacement && (trainSize+testSize > nodes.length)))
            throw new IllegalArgumentException("trainSize("+trainSize+") and testSize("+testSize+") are invalid.  They sum to too much or are negative.");
        Node[][] samples = sampler.sample(trainSize,testSize);
        return new DataSplit(this, samples[1], samples[0]);
    }
    public DataSplit getSplit(final double trainRatio) {
        logger.finer("getSplit(trainRatio="+trainRatio+")");
        if(trainRatio < 0 || (trainRatio > 1 && !replacement))
            throw new IllegalArgumentException("trainRatio("+trainRatio+") must lie in the range [0:1]");
        return getSplit((int)(nodes.length*trainRatio+0.5));
    }
    public DataSplit getSplit(final double trainRatio, final double testRatio) {
        logger.finer("getSplit(trainRatio="+trainRatio+",testRatio="+testRatio+")");
        if(trainRatio < 0 || testRatio < 0 || (!replacement && (trainRatio > 1 || testRatio > 1)))
            throw new IllegalArgumentException("trainRatio("+trainRatio+") and testRatio("+testRatio+") must both lie in the range [0:1]");
        if(!replacement && testRatio + trainRatio > 1)
            throw new IllegalArgumentException("trainRatio("+trainRatio+") and testRatio("+testRatio+") must sum to at most 1");

        int trainSize = (int)(nodes.length*trainRatio+0.5);
        int testSize  = (int)(nodes.length*testRatio+0.5);
        if(!replacement && trainSize+testSize > nodes.length)
            trainSize--;
        return getSplit(trainSize, testSize);
    }

    public DataSplit[] getSplits(final int numSplits, final double trainRatio, final double testRatio) {
        logger.finer("getSplits(numSplits="+numSplits+",trainRatio="+trainRatio+",testRatio="+testRatio+")");
        int train = (int)(0.5+trainRatio*(double)nodes.length);
        int test = (int)(0.5+testRatio*(double)nodes.length);
        if(train+test > nodes.length)
            test = nodes.length-train;
        return getSplits(numSplits,train,test);
    }
    public DataSplit[] getSplits(final int numSplits, final int trainSize, final int testSize) {
        logger.finer("getSplits(numSplits="+numSplits+",trainSize="+trainSize+",testSize="+testSize+")");
        DataSplit[] result = new DataSplit[numSplits];
        for(int i=0;i<numSplits;i++)
            result[i] = getSplit(trainSize,testSize);
        return result;
    }
    public DataSplit[] getSplits(final int numSplits, final double trainRatio) {
        logger.fine("getSplits(numSplits="+numSplits+",trainRatio="+trainRatio+")");
        if(trainRatio <= 0)
          throw new IllegalArgumentException("trainRatio("+trainRatio+") must be positive!");

        int trainSize = -1;
        
        if(trainRatio >= 1)
          trainSize = (int)trainRatio;
        else
          trainSize = (int)(0.5+trainRatio*(double)nodes.length);

        return getSplits(numSplits, trainSize);
    }
    public DataSplit[] getSplits(final int numSplits, final int trainSize) {
        logger.finer("getSplits(numSplits="+numSplits+",trainSize="+trainSize+")");
        return getSplits(numSplits, trainSize, nodes.length-trainSize);
    }

    public void setClassification(final Classification known) {
        this.known = known;
        for(Node node : nodes)
        {
            if(known.isUnknown(node))
                hideClassValue(node);
            else
                node.setValue(clsIdx, known.getClassValue(node));
        }
    }
    public void resetTruth() {
        for(Node node : graph.getNodes(nodeType))
            node.setValue(clsIdx, getTrueClassValue(node));
    }
    public Classification getTruth() {
        return truth;
    }
    public void setTruth(final Classification truth) {
        if(this.truth==null)
            this.truth = new Classification(truth.getGraph(), nodeType, attrib);
        for(Node n : truth)
            this.truth.set(n,truth.getClassValue(n));
    }
    public int getTrueClassValue(final Node node) {
        return truth.getClassValue(node);
    }
    public void hideClassValue(final Node node) {
        node.setValue(clsIdx,Double.NaN);
    }
    public void showClassValue(final Node node) {
        if(known != null && !known.isUnknown(node))
            node.setValue(clsIdx,(double)known.getClassValue(node));
        else
            node.setValue(clsIdx,((truth.isUnknown(node))?Double.NaN : (double)truth.getClassValue(node)));
    }

    public String getNodeType() {
        return nodeType;
    }
    public int getAttributeIndex() {
        return clsIdx;
    }
    public AttributeCategorical getAttribute() {
        return attrib;
    }
    public Graph getGraph() {
        return graph;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DataView");
        sb.append('[').append(nodeType).append(',').append(attrib.getName()).append("]\n");
        sb.append("    num-nodes:          ").append(size()).append('\n');
        sb.append("    attribute:          ").append(attrib).append('\n');
        sb.append("    distribution:       ").append(ArrayUtil.asString(getClassDistribution())).append('\n');
        sb.append("    replacement:        ").append(replacement).append('\n');
        sb.append("    stratified:         ").append(stratified).append('\n');
        sb.append("    pruneZeroKnowledge: ").append(pruneZeroKnowledge).append('\n');
        sb.append("    pruneSingletons:    ").append(pruneSingletons).append('\n');
        return sb.toString();
    }
}
