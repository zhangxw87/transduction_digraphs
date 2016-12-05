/**
 * Aggregator.java
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

import netkit.graph.EdgeType;
import netkit.graph.Type;
import netkit.graph.Node;
import netkit.graph.Attribute;
import netkit.classifiers.Estimate;

/**
 * The Aggregator interface provides for dynamic attribute fields
 * within Attributes containers.  Aggregators may be added in a Graph
 * on a specific node type and act like additional attribute fields
 * within that node type.  The value returned by an Aggregator may be
 * static (fixed for the lifetime of the Aggregator-Node combination
 * and therefore possibly cached) or dynamically calculated on each
 * fetch.  That is up to the implementor of the interface object.
 * Within a Node, getting a named or indexed value first searches the
 * fixed attributes then looks in the aggregators for a matching value
 * to return.  They attempt to behave just like any other attribute
 * field.
 *
 * @see netkit.graph.Attribute
 * @see netkit.graph.Attributes
 * @see netkit.graph.Node
 * 
 * @author Kaveh R. Ghazi
 */
public interface Aggregator
{
    /** Gets the name of the field represented by this object.
     * @return the name of the field represented by this object.
     */
    public String getName();

    /** Gets the Type of the value stored in this object.
     * @return the Type of the value stored in this object.
     */
    public Type getType();

    /** Gets the attribute that is being aggregated over.
     * @return the Type of the value stored in this object.
     */
    public Attribute getAttribute();

    /** Gets the edge that is used for aggregation.
     * @return the edge that is used for aggregation.
     */
    public EdgeType getEdgeType();

    /** Gets the value stored in this object for the supplied Node.
     * @param n a Node for which to calculate and/or supply a value.
     * @return the value stored in this object for the supplied Node.
     */
    public double getValue(Node n);

    /** Gets the value stored in this object for the supplied Node.
     * @param n a Node for which to calculate and/or supply a value.
     * @param prior current priors for unknown neighbor values.
     * @return the value stored in this object for the supplied Node.
     */
    public double getValue(Node n, Estimate prior);
}
