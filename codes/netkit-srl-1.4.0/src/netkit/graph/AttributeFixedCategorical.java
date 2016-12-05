/**
 * AttributeFixedCategorical.java
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

/** This class handles attributes that are of type CATEGORICAL.
 * CATEGORICAL types can have values from a set of tokens specified by
 * the tokenSet.  The set of valid tokens is fixed at the time this
 * container is created.  This class is immutable.
 * @see Attributes
 * @see FixedTokenSet
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public final class AttributeFixedCategorical extends AttributeCategorical
{
    /** The constructor must be provided the name of this attribute
     * and the set of valid categorical token values.
     * @param name a String representing the name of this attribute.
     * @param tokenSet a set representing the valid categorical token
     * values for this attribute.
     */
    public AttributeFixedCategorical(String name, FixedTokenSet tokenSet)
    {
	super(name, tokenSet);
    }

    /** Parses the supplied string token for insertion into this
     * attribute and converts the token into a double value; if the
     * token is "?", that results in NaN.
     * @return the token converted into an double.
     * @throws RuntimeException if the token is not already in the set
     * of valid tokens.
     */
    public double parseAndInsert(String token)
    {
	if (token.equals("?"))
	    return Double.NaN;
	else if (tokenSet.contains(token))
	    return tokenSet.getValue(token);
	else
	    throw new RuntimeException("Parsed invalid token <"
				       + token + ">");
    }
}
