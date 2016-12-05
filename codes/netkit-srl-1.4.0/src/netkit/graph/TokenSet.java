/**
 * TokenSet.java
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

import java.util.*;

/** This abstract class is the parent of the token set hierarchy.
 * These containers keep track of valid tokens for a CATEGORICAL
 * attribute type.
 * @see AttributeCategorical
 * 
 * @author Kaveh R. Ghazi
 */
public abstract class TokenSet
{
    // This Map keeps track of tokens and the order they appeared in.
    // We keep the order as the value of each key for fast lookup.
    // Note, token order starts at zero.
    protected final Map<String,Integer> tokenMap
	= new HashMap<String,Integer>();
    // The ArrayList is necessary to preserve insertion order and for fast
    // lookup of indexed elements.  The set is used for determination
    // of whether a token already exists in this container.
    protected final ArrayList<String> tokenList = new ArrayList<String>();

    /** Adds the supplied token to this set; duplicates are not allowed.
     * @param token a String token to add to this container.
     * @throws RuntimeException if the token already exists in this
     * container.
     */
    protected void add(String token)
    {
	if (tokenMap.put(token, tokenMap.size()) != null)
	    throw new RuntimeException("TokenSet already contains <"+token+">");
	tokenList.add(token);
    }

    /** Determines if a token is valid for this container.
     * @param token a String representation of a value for this Set.
     * @return true if the token is valid for this Set, i.e. it exists
     * in this container.
     */
    public boolean contains(String token)
    {
	return tokenMap.containsKey(token);
    }
    
    /** Get a String array of valid tokens.
     * @return a String array representing the valid tokens in this
     * container.
     */
    public String[] getTokens()
    {
	return tokenList.toArray(new String[tokenList.size()]);
    }
    
    /** Gets the i'th element from the array of valid tokens.
     * @param i the index to get the token from.
     * @return the i'th token element at the supplied index.
     * @throws IndexOutOfBoundsException if the supplied index is not
     * within the range of valid token indexes.
     */
    public String getToken(int i)
    {
	return tokenList.get(i);
    }
    
    /** Gets the size of the categorical token list.
     * @return the size of the categorical token list.
     */
    public int size()
    {
	return tokenList.size();
    }
        
    /** Gets the integer value associated with the supplied token.
     * @param token a String representing the token to be converted
     * into a numeric value.
     * @return an integer value unique for each valid token.  Tokens
     * are numbered sequentially in the order they were added to this
     * container.
     * @throws RuntimeException if the supplied token isn't found.
     */
    public int getValue(String token)
    {
	return tokenMap.get(token);
    }
}
