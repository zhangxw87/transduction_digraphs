/**
 * AttributeDiscrete.java
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

/** This class handles attributes that are of type DISCRETE.  DISCRETE
 * types can have integer numerical values and are represented
 * internally as java double types.  This class is immutable.
 * @see Attributes
 * @see netkit.graph.io.SchemaReader
 *
 * @author Kaveh R. Ghazi
 */
public final class AttributeDiscrete extends Attribute
{
    /** The constructor must be provided the name for this attribute.
     * @param name a String representing the name of this attribute.
     */
    public AttributeDiscrete(String name)
    {
	super(name,Type.DISCRETE);
    }

    /** Parses the supplied string token for insertion into this
     * attribute and converts the token into a double value; a "?"
     * token results in NaN.
     * @return the token converted into an double.
     * @throws NumberFormatException if the token does not contain a
     * parsable integer.
     */
    public double parseAndInsert(String token)
    {
	if (token.equals("?"))
	    return Double.NaN;
	else
	    return Integer.parseInt(token);
    }

    public String formatForOutput(double value)
    {
	if (Double.isNaN(value))
	    return "?";
	else
	    return Integer.toString((int)value);
    }
}
