/**
 * NodeFilter.java
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

/** A filter for Nodes.  Objects implementing this interface can be
 * used to filter Nodes in or out from a collection.
 * @see Node
 * 
 * @author Kaveh R. Ghazi
 */
public interface NodeFilter
{
    /** Tests whether or not the specified Node should be included.
     * @return true if the Node should be included.
     */
    public boolean accept(Node node);
}
