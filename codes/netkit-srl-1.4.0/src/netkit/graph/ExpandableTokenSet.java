/**
 * ExpandableTokenSet.java
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

/** This class keeps track of valid tokens for a CATEGORICAL attribute
 * type.  This container is mutable in the sense that you can add more
 * tokens at any time.  However existing tokens cannot be changed or
 * removed.
 * @see AttributeExpandableCategorical
 *
 * @author Kaveh R. Ghazi
 */
public class ExpandableTokenSet extends TokenSet
{
    // Override with public access.
    public final void add(String token)
    {
	super.add(token);
    }
}
