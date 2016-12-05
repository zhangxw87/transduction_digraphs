/**
 * DataSplit.java
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
 * $Id: DataSplit.java,v 1.9 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 9:55:41 AM
 */
package netkit.classifiers;

import netkit.graph.*;
import netkit.util.GraphMetrics;
import netkit.util.VectorMath;
import netkit.util.NetKitEnv;

import java.util.*;
import java.util.logging.Logger;

public class DataSplit {
    private final Logger logger = NetKitEnv.getLogger(this);
    private final DataView view;
    private Node[] train;
    private Node[] test;
    private Node[] unknown;
    private double[] distrib;
    private boolean hasTruth = true;

    public DataSplit(DataView view, Node[] test, Node[] train) {
        this.view = view;
        if(train == null) train = new Node[0];
        if(test == null) test = new Node[0];

        distrib = new double[view.getAttribute().size()];
        Arrays.fill(distrib,0);

        int idx = view.getAttributeIndex();
        if(train.length > 0)
        {
            for(Node n : train)
            {
                if(!n.isMissing(idx))
                    distrib[(int)n.getValue(idx)]++;
            }
            VectorMath.normalize(distrib);
        }
        else
        {
            if(test.length == 0)
                throw new IllegalArgumentException("Both train and test are empty!");
        }

        boolean[] pruned = new boolean[view.getGraph().numNodes(view.getNodeType())];
        Arrays.fill(pruned,false);
        if (test.length > 0 && view.doPruneZeroKnowledge())
        {
            int numPruned = 0;
            ArrayList<Node> al = new ArrayList<Node>(test.length);

            GraphMetrics gm = view.getGraph().getMetrics();
            int[] componentWithTest = new int[gm.getNumComponents()];
            Arrays.fill(componentWithTest, 0);
            for(Node n : train)
            {
                componentWithTest[gm.getComponent(n)]++;
                pruned[n.getIndex()] = true;
            }

            for(Node n : test)
            {
                if (pruned[n.getIndex()])
                {
                  logger.warning("Node "+n.getName()+" Appears in both test AND training!?");
                }
                if (componentWithTest[gm.getComponent(n)] == 0)
                {
                    numPruned++;
                    pruned[n.getIndex()] = true;
                }
                else
                    al.add(n);
            }
            
            test = al.toArray(new Node[0]);
            logger.fine("Pruned " + numPruned + " test nodes in zeroknowledge components (new size="+test.length+")");
        }
        this.train = train;
        this.test = test;

        int num = view.size();
        if(train.length + test.length == num)
            this.unknown = test;
        else
        {
            ArrayList<Node> al = new ArrayList<Node>(num-train.length);
            for(Node n : view)
                view.hideClassValue(n);
            for(Node n : train)
                view.showClassValue(n);
            for(Node n : view)
                if(n.isMissing(idx) && !pruned[n.getIndex()])
                    al.add(n);
            this.unknown = al.toArray(new Node[0]);
        }
        
        hasTruth = true;
        if(this.test != null)
        {
           Classification truth = view.getTruth();
           for(Node n : this.test)
           {
               if(truth.isUnknown(n))
               {
                   hasTruth = false;
                   break;
               }
           }
        }
    }

    public int applyLabels(int depth)  {
        for (Node n : unknown)
            view.hideClassValue(n);
        for (Node n : train)
            view.showClassValue(n);
        int num = train.length;
        for (Node n : train)
            num += view.showNeighbors(n,depth);
        return num;
    }

    public DataView getView() {
        return view;
    }

    public double[] getClassDistribution() {
        return distrib;
    }

    public double[] getPrior() {
        double[] prior = view.getPrior();
        return ( (prior == null) ? distrib : prior );
    }
    
    public boolean hasTruth() {
        return hasTruth;
    }

    public Node[] getTrainSet() {
        return train;
    }
    public Node[] getTestSet() {
        return test;
    }
    public Node[] getUnknownSet() {
        return unknown;
    }
    public int getTrainSetSize() {
        return ( train.length );
    }
    public int getTestSetSize() {
        return ( test.length );
    }
    public int getUnknownSetSize() {
        return ( unknown.length );
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DataSplit");
        sb.append("    view:           ").append(view).append('\n');
        sb.append("    trainSetSize:   ").append(train.length).append('\n');
        sb.append("    testSetSize:    ").append(test.length).append('\n');
        sb.append("    unknownSetSize: ").append(unknown.length).append('\n');
        return sb.toString();
    }

}
