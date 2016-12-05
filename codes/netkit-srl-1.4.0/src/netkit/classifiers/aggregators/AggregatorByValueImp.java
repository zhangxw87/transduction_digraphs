/**
 * AggregatorByValueImp.java
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

import netkit.graph.Attribute;
import netkit.graph.AttributeCategorical;
import netkit.graph.EdgeType;
import netkit.graph.Type;

/**
 * This should be the parent class for any AggregatorByValue class.
 * It sets up the basic structure for an AggregatorByValue and defines most of the needed API.
 * The only thing needed to be implemented by any subclass is the getValue(Node, Estimate)
 * method.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public abstract class AggregatorByValueImp
        extends AggregatorImp
        implements AggregatorByValue
{
    protected final double attributeValue; // The specific value of the attribute being aggregated (e.g., the double value of 'aquavit')
    private final String token;

    /**
     * Specifies the core parameters needed to instantiate an AggregatorByValue class.  This calls
     * the standard aggregator constructor with all these parameters, which helps set the name and other
     * internal variables.  The name of the 'virtual' attribute is set in the AggregatorImp constructor.
     *
     * @param aggName The prefix name of this aggregator attribute
     * @param edgeType The name of the relation to follow to find neighbor nodes to aggregate over
     * @param attribute The attribute of neighboring nodes that should be aggregated on
     * @param type The attribute type of this aggregator
     * @param value The value to aggregate on (e.g., the double representing 'aquavit')
     *
     * @see netkit.classifiers.aggregators.AggregatorImp
     */
    public AggregatorByValueImp(final String aggName, final EdgeType edgeType, final Attribute attribute, final Type type, double value) {
        super(aggName,edgeType,attribute,type,value);
        this.attributeValue = value;
        if(attribute instanceof AttributeCategorical)
        {
            this.token = ((AttributeCategorical)attribute).getToken((int)value);
        }
        else
        {
            this.token = String.valueOf(value);
        }
    }

    /**
     * @return the value passed in as the 'value' in the Constructor
     */
    public final double getAttributeValue() {
        return attributeValue;
    }
    
    public final String toString() {
        return super.toString()+"(value:"+token+")";
    }
}
