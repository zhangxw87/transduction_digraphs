/**
 * DataViewTest.java
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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import netkit.graph.*;
import netkit.util.VectorMath;

import java.util.Arrays;
import java.util.HashSet;

/**
 * DataView Tester.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 * @since <pre>12/11/2004</pre>
 * @version 1.0
 */
public class DataViewTest extends TestCase
{
    private Graph g;
    private static final String nodeType = "DataViewTest";
    private static final String attrName = "ClassName";
    private AttributeCategorical ca;

    public DataViewTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        g = new Graph();
        Attributes a = new Attributes(nodeType);
        ca = new AttributeFixedCategorical(attrName, new FixedTokenSet(new String[]{"A","B","C","D"}));
        a.add(new AttributeKey("key"));
        a.add(ca);
        g.addAttributes(a);
        int size = 0;
        for(String s : ca.getTokens())
        {
            int value = ca.getValue(s);
            size += 20;
            for(int i=1;i<=size;i++)
            {
                Node n = g.addNode(s+i,a);
                n.setValue(attrName,value);
            }
        }
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testShowNeighbors() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSize() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetPrior() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSetPrior() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetClassDistribution() throws Exception
    {
        //TODO: Test goes here...
    }

    private DataView getDataView(boolean replacement, boolean stratified) {
        return new DataView(g, nodeType, ca, System.currentTimeMillis(), replacement, stratified, false, false);
    }
    private void testCrossValidate(boolean replacement, boolean stratified) {
        System.err.println("testCrossValidate("+replacement+","+stratified+")");
        DataView v = getDataView(replacement, stratified);
        double[] tdist = v.getClassDistribution();
        HashSet<Node> train = new HashSet<Node>(v.size());
        HashSet<Node> ltrain = new HashSet<Node>(v.size());
        HashSet<Node> seen = new HashSet<Node>(v.size());
        for(int ncv=5;ncv<=20;ncv+=5)
        {
            double[][] prior = new double[ncv][ca.size()];
            for(double[] row : prior)
                Arrays.fill(row,0);
            int[] numSeen = new int[v.size()];
            Arrays.fill(numSeen,0);
            System.err.println("   testCrossValidate numCV="+ncv);
            for(int i=0;i<100;i++)
            {
                DataSplit[] cv = v.crossValidate(ncv);
                assertEquals(ncv,cv.length);

                train.clear();
                for(int idx=0;idx<cv.length;idx++)
                {
                    assertEquals(cv[idx].getTestSetSize()+cv[idx].getTrainSetSize(),v.size());
                    seen.clear();
                    ltrain.clear();
                    double[] cd = cv[idx].getClassDistribution();
                    VectorMath.add(prior[idx], cd);
                    for(Node n : cv[idx].getTrainSet())
                    {
                        assertTrue("Node "+n+" seen twice in training data.",(seen.add(n) || replacement));
                        train.add(n);
                        ltrain.add(n);
                        numSeen[n.getIndex()]++;
                    }
                    for(Node n : cv[idx].getTestSet())
                    {
                        assertEquals("Node "+n+" seen in training and test",false,ltrain.contains(n));
                        assertTrue("Node "+n+" seen twice",(seen.add(n) || replacement));
                    }
                    if(!replacement)
                        assertEquals(v.size(), seen.size());
                }
                if(!replacement)
                    assertEquals(v.size(), train.size());
            }
            for(double[] row : prior)
            {
                VectorMath.normalize(row);
                if(stratified)
                    assertTrue(VectorMath.equals(row,tdist,0.05));
                else
                    assertTrue(VectorMath.equals(row,tdist,0.1));
            }
        }
    }
    public void testCrossValidate() throws Exception
    {
        testCrossValidate(false, false);
        testCrossValidate(false, true);
        testCrossValidate(true, true);
        testCrossValidate(true, false);
    }

    public void testSample() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSample1() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSample2() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSample3() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplit() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplit1() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplit2() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplit3() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplit4() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSampleList() throws Exception
    {
        DataView v = getDataView(false, true);
        Node[] all = v.getSampleList(0,v.size());
        int size = v.size();
        assertEquals(all.length,size);

        Node[] l1 = v.getSampleList(0,size/2);
        Node[] l2 = v.getSampleList(size/2,size);

        HashSet<Node> l1nodes = new HashSet<Node>(v.size());
        HashSet<Node> seen = new HashSet<Node>(v.size());
        for(Node n : l1)
        {
            assertFalse(l1nodes.contains(n));
            l1nodes.add(n);
            seen.add(n);
        }
        for(Node n : l2)
        {
            assertFalse(seen.contains(n));
            seen.add(n);
        }
        assertEquals(size,seen.size());

        l1 = v.getSampleList(size/3,2*size/3);
        l2 = v.getSampleList(2*size/3,size/3);

        l1nodes.clear();
        seen.clear();
        for(Node n : l1)
        {
            assertFalse(l1nodes.contains(n));
            l1nodes.add(n);
            seen.add(n);
        }
        for(Node n : l2)
        {
            assertFalse(seen.contains(n));
            seen.add(n);
        }
        assertEquals(size,seen.size());
    }

    private void testGetSplits(boolean replacement, boolean stratified) {
        System.err.println("testGetSplits("+replacement+","+stratified+")");

        DataView v = getDataView(replacement, stratified);
        double[] tdist = v.getClassDistribution();
        HashSet<Node> train = new HashSet<Node>(v.size());
        HashSet<Node> ltrain = new HashSet<Node>(v.size());
        HashSet<Node> seen = new HashSet<Node>(v.size());
        for(int nsplit=5;nsplit<=20;nsplit+=5)
        {
            double[][] prior = new double[nsplit][ca.size()];
            for(double[] row : prior)
                Arrays.fill(row,0);
            for(double r=0.1;r<0.99;r+=0.1)
            {
                System.err.println("    testGetSplits numSplits="+nsplit+" r="+r);
                for(int i=0;i<100;i++)
                {
                    DataSplit[] cv = v.getSplits(nsplit, r);
                    assertEquals(nsplit, cv.length);
                    train.clear();
                    for(int idx=0;idx<cv.length;idx++)
                    {
                        double[] cd = cv[idx].getClassDistribution();
                        assertEquals(v.size(), cv[idx].getTrainSetSize()+cv[idx].getTestSetSize());

                        VectorMath.add(prior[idx], cd);
                        seen.clear();
                        ltrain.clear();
                        for(Node n : cv[idx].getTrainSet())
                        {
                            assertFalse("node "+n+" seen twice in training set",!seen.add(n) && !replacement);
                            train.add(n);
                            ltrain.add(n);
                        }
                        for(Node n : cv[idx].getTestSet())
                        {
                            assertFalse(ltrain.contains(n));
                            assertFalse("node "+n+" seen in train + test",!seen.add(n) && !replacement);
                        }
                        if(!replacement)
                            assertEquals(v.size(), seen.size());
                    }
                }
                for(double[] row : prior)
                {
                    VectorMath.normalize(row);
                    if(stratified)
                        assertTrue(VectorMath.equals(row,tdist,0.05));
                    else
                        assertTrue(VectorMath.equals(row,tdist,0.1));
                }
            }
        }
    }
    public void testGetSplits() throws Exception
    {
        testGetSplits(true,true);
        testGetSplits(true,false);
        //testGetSplits(false,true);
        testGetSplits(false,false);
    }

    public void testGetSplits1() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplits2() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetSplits3() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testSetClassification() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testResetTruth() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testHasTruth() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetTruth() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetTrueClassValue() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testHideClassValue() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testShowClassValue() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetNodeType() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetAttributeIndex() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetAttribute() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testGetGraph() throws Exception
    {
        //TODO: Test goes here...
    }

    public static Test suite()
    {
        return new TestSuite(DataViewTest.class);
    }
}
