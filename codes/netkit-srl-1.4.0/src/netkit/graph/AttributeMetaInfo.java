/**
 * AttributeMetaInfo.java
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

package netkit.graph;

/** This class is used to persist certain information across an entire
 * graph related to a particular Attribute.
 * 
 * @see Attribute
 * @see Graph
 * @see Node
 * 
 * @author Kaveh R. Ghazi
 */

public class AttributeMetaInfo extends AbstractAttributeMetaInfo
{
    /** Construct an object of this type.
     * @param attrib the Attribute for this meta info object.
     * @param attributes the Attributes container that attrib lives in.
     * @param graph the Graph object that the attributes lives in.
     */
    public AttributeMetaInfo(Attribute attrib, Attributes attributes, Graph graph)
    {
	super(attrib, attributes, graph);
    }
}
