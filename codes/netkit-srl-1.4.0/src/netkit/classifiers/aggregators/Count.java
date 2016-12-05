/**
 * Count.java
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
 * The Count aggregator counts the number of times a specific value of a given
 * attribute is observed in the neighborhood of a node in the graph.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class Count extends AggregatorByValueImp {
    private final int intValue;
    public Count(EdgeType edgeType, Attribute attribute, double value) {
        super("count",edgeType, attribute, Type.CONTINUOUS, value);
        if(!((attribute instanceof AttributeCategorical) || (attribute instanceof AttributeKey) || (attribute instanceof AttributeDiscrete)))
            throw new IllegalArgumentException("["+getName()+"] Unknown attribute: "+attribute);
        if(Double.isNaN(value))
            throw new IllegalArgumentException("["+getName()+"] No value specified!");
        intValue = (int)attributeValue;
    }

    public double getValue(Node n, Estimate prior) {
        double[] counts = null;
        double value = 0;
        SharedNodeInfo info = getNodeInfo(n);
        switch(attribute.getType())
        {
        case CATEGORICAL:
            counts = info.countNeighbors(n, prior);
            value = counts[intValue];
            break;

        case DISCRETE:
            HistogramDiscrete hist = info.getHistogram(n);
            value = ( (hist==null) ? 0 : hist.getCount(intValue) );
            break;
        }
        return value;
    }
}
