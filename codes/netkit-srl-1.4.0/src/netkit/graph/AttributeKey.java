/**
 * AttributeKey.java
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

/** This class handles attributes that are of type KEY.  KEY types
 * have values that must be unique among all entities in the same
 * attribute container.
 * @see Attributes
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public final class AttributeKey extends AttributeExpandableCategorical
{
    /** The constructor must be provided the name for this attribute.
     * @param name a String representing the name of this attribute.
     */
    public AttributeKey(String name)
    {
	super(name, new ExpandableTokenSet());
    }

    /** Parses the supplied string token for insertion into this
     * attribute and converts the token into a double value; 
     * @return the token converted into an double.
     * @throws RuntimeException if the token is "?".
     * @throws RuntimeException if the token is a duplicate.
     */
    public double parseAndInsert(String token)
    {
	if (token.equals("?"))
	    throw new RuntimeException("Cannot have unknown key token");
	else
	    tokenSet.add(token);
	return tokenSet.getValue(token);
    }
}
