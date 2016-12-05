/**
 * FixedTokenSet.java
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
 * type.  A token is valid if it exists in this container and all
 * tokens must be provided at the time of construction.  This class is
 * immutable.
 * @see AttributeFixedCategorical
 * 
 * @author Kaveh R. Ghazi
 */
public final class FixedTokenSet extends TokenSet
{
    /** This constructor takes a String array of valid tokens; the
     * tokens supplied to the constructor are the only valid ones for
     * the lifetime of this token set.
     * @param tarray an array of String representing the valid tokens.
     * @throws RuntimeException if any of the tokens is duplicated.
     */
    public FixedTokenSet(String tarray[])
    {
	for (final String token : tarray)
	    add(token);
    }
}
