/**
 * Max.java
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
 * The Max aggregator returns the maximum value observed for a continuous
 * attribute in the neighborhood of a node in the graph.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class Max extends AggregatorImp {
    public Max(EdgeType edgeType, Attribute attribute) {
        super("max",edgeType, attribute, Type.CONTINUOUS);
        if(!((attribute instanceof AttributeDiscrete) || (attribute instanceof AttributeContinuous)))
            throw new IllegalArgumentException("Max["+getName()+"] aggregator can only be used with numerical attributes.");
    }

    public double getValue(Node n, Estimate prior) {
        return getNodeInfo(n).getMax(n);
    }
}
