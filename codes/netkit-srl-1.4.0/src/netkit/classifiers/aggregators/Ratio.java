/**
 * Ratio.java
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
package netkit.classifiers.aggregators;

import netkit.graph.*;
import netkit.util.HistogramDiscrete;
import netkit.classifiers.Estimate;

/**
 * The Ratio aggregator counts the ratio of times a specific value of a given
 * attribute is observed in the neighborhood of a node in the graph.  Ratio is
 * defined as how often the value is observed divided by all the observed values.
 * In other words, if 'red' is observed in three neighbors and if colors other
 * than red are observed in seven neighbors, then the ratio of 'red' is 0.3.
 * Neighbors for which no value is observed are not counted.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class Ratio extends AggregatorByValueImp {
    private final Count count;
    public Ratio(EdgeType edgeType, Attribute attribute, double value) {
        super("ratio",edgeType, attribute, Type.CONTINUOUS, value);
        this.count = new Count(edgeType, attribute, value);
    }

    public double getValue(Node n, Estimate prior) {
        double value = count.getValue(n,prior);
        if(!Double.isNaN(value) && value > 0)
        {
            SharedNodeInfo info = getNodeInfo(n);
            switch(attribute.getType())
            {
            case CATEGORICAL:
                    value /= info.getSum(n,prior);
                    break;

            case DISCRETE:
                    HistogramDiscrete hist = info.getHistogram(n);
                    value /= hist.getTotalCount();
                    break;
            }
        }
        return value;
    }
}
