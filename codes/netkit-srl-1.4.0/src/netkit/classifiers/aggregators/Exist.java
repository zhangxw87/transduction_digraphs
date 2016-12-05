/**
 * Exist.java
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
import netkit.classifiers.Estimate;

/**
 * The Exist aggregator returns whether a specific value of a given
 * attribute is observed in the neighborhood of a node in the graph.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class Exist extends AggregatorByValueImp {
    private final Count count;
    public Exist(EdgeType edgeType, Attribute attribute, double value) {
        super("exist",edgeType, attribute, Type.CONTINUOUS, value);
        this.count = new Count(edgeType, attribute, value);
    }

    public double getValue(Node n, Estimate prior) {
        double value = count.getValue(n,prior);
        if(!Double.isNaN(value))
        {
            value = ((value>0) ? 1.0 : 0.0);
        }
        return value;
    }
}
