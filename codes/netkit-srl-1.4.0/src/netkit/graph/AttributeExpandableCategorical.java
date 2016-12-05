/**
 * AttributeExpandableCategorical.java
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
 * the tokenSet.  The set of valid tokens is mutable during the
 * lifetime of this container in that more tokens can be added.
 * However existing tokens cannot be changed or removed.
 * @see Attributes
 * @see ExpandableTokenSet
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public class AttributeExpandableCategorical extends AttributeCategorical
{
    /** The constructor must be provided the name of this attribute
     * and an ExpandableTokenSet to keep track of tokens.
     * @param name a String representing the name of this attribute.
     * @param tokenSet an ExpandableTokenSet representing the valid
     * tokens of this attribute.
     */
    protected AttributeExpandableCategorical(String name, ExpandableTokenSet tokenSet)
    {
	super(name, tokenSet);
    }

    /** The constructor must be provided the name of this attribute.
     * @param name a String representing the name of this attribute.
     */
    public AttributeExpandableCategorical(String name)
    {
	this(name, new ExpandableTokenSet());
    }

    /** Adds the supplied token parameter to the set of valid tokens
     * for this attribute.
     * @param token a String to add to the list of valid tokens.
     * @throws RuntimeException if the token token already exists in
     * the set.
     */
    public void addToken(String token)
    {
	parseAndInsert(token);
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
	else if (!tokenSet.contains(token))
	    tokenSet.add(token);
	return tokenSet.getValue(token);
    }
}
