/**
 * AttributeCategorical.java
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
 * CATEGORICAL types can have values from a fixed set of tokens
 * specified by the tokenSet.
 * @see Attributes
 * @see TokenSet
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public abstract class AttributeCategorical extends Attribute
{
    // This is the container for the valid tokens in this attribute.
    protected final TokenSet tokenSet;

    /** The constructor must be provided the name of this attribute
     * and the set of valid categorical token values.  Subclasses
     * should provide a public constructor that overrides this one.
     * @param name a String representing the name of this attribute.
     * @param tokenSet a TokenSet representing the valid tokens of
     * this attribute.
     */
    protected AttributeCategorical(String name, TokenSet tokenSet)
    {
	super(name,Type.CATEGORICAL);
	this.tokenSet = tokenSet;
    }

    /** Gets the tokens valid for this categorical attribute.
     * @return an array of String containing the tokens valid for this
     * categorical attribute.
     */
    public final String[] getTokens()
    {
	return tokenSet.getTokens();
    }
    
    /** Gets the i'th token from the list of valid tokens for this
     * categorical attribute; the index is a zero-based array lookup.
     * @param i the index into the token array to lookup
     * @return a String with the i'th valid token.
     * @throws ArrayIndexOutOfBoundsException if the parameter is
     * outside the bounds of the array containing the valid tokens.
     */
    public final String getToken(int i)
    {
	return tokenSet.getToken(i);
    }
    
    /** Gets the size of the categorical token list.
     * @return the size of the categorical token list.
     */
    public final int size()
    {
	return tokenSet.size();
    }
        
    /** Gets the numerical value for a particular token; the token
     * must be in the set of valid tokens for this attribute.
     * @param token the String token to lookup in the token set.
     * @return the integer index of the token.
     * @throws RuntimeException if the token isn't found.
     */
    public final int getValue(String token)
    {
	return tokenSet.getValue(token);
    }

    public String formatForOutput(double value)
    {
	if (Double.isNaN(value))
	    return "?";
	else
	    return "\""+getToken((int)value)+"\"";
    }
}
