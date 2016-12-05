/**
 * Attributes.java
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

/** This class is a container for Attribute classes.  It keeps an
 * ordered list of Attribute classes as individually typed fields.
 * This container knows which field is the KEY Attribute (if a key
 * exists.)  Each Attribute within this container must have a unique
 * field name.  Each Attributes container must have a unique container
 * name also.  The name of this container class serves as a "type" for
 * Nodes.
 * @see Attribute
 * @see AttributeKey
 * @see Node
 * @see netkit.graph.io.SchemaReader
 * 
 * @author Kaveh R. Ghazi
 */
public final class Attributes implements Iterable<Attribute>
{
    // This is the name of this container of attributes or "type".
    private final String name;
    // We use LinkedHashMap to preserve the order in which attributes
    // were added to this container.
    private final Map<String, Attribute> attrMap
	= new LinkedHashMap<String, Attribute>();
    // If this container has a key, store it here.
    private AttributeKey key;
    private int keyIndex = -1;

    /** This constructor builds an Attributes container object using
     * the supplied name.
     * @param name a string to uniquely name this container.
     */
    public Attributes(String name)
    {
	this.name = name;
    }
    
    /** Adds an Attribute to this container; the name of the Attribute
     * must be a unique field name within this container.  Note you
     * shouldn't call this method directly after this container has
     * been added to a Graph because Nodes will not carry the same
     * number of values as their Attributes container requires.
     * @param a the attribute to be added.
     * @throws RuntimeException if the attribute name is already present.
     * @throws RuntimeException if an attempt is made to add a second key.
     * @see Graph#addAttribute(String,Attribute)
     */
    public void add(Attribute a)
    {
	final String name = a.getName();
	if (this.contains(name))
	    throw new RuntimeException("Already got field <" + name + ">");
	if (a instanceof AttributeKey)
	    if (key == null)
	    {
		key = (AttributeKey)a;
		keyIndex = attrMap.size();
	    }
	    else
		throw new RuntimeException("Got second key attribute <" + name +
					   ">, already had key <" + key.getName() + ">");
	attrMap.put(name,a);
    }
    
    /** Get the name of this object.
     * @return the name of this object.
     */
    public String getName()
    {
	return name;
    }
    
    /** Get the index of an attribute within this container; indexes
     * honor the presence of a key Attribute within this container.
     * This method is reflexive with {@link #getAttribute(int)}.
     * @param attribute the attribute name to lookup.
     * @return the index of an attribute within this container.
     * @throws RuntimeException if the attribute isn't found.
     */
    public int getAttributeIndex(String attribute)
    {
	int i = 0;
	for (final Attribute a : this)
	{
	    if (a.getName().equals(attribute))
		return i;
	    i++;
	}
	throw new RuntimeException("Couldn't find index for attribute <" + attribute + ">");
    }

    /** Removes the Attribute at the specified index.  One cannot
     * remove a KEY field if it exists.  Note you shouldn't call this
     * method directly after this container has been added to a Graph
     * because Nodes will not carry the same number of values as their
     * Attributes container requires.
     * @param index an integer index specifying which Attribute to
     * remove.
     * @throws RuntimeException if index is out of bounds or if index
     * references the KEY field.
     * @see Graph#removeAttribute(String,int)
     */
    public void remove(int index)
    {
	// Make sure the index is not out of bounds.
	if (index < 0 || index >= attributeCount())
	    throw new RuntimeException("Invalid index <"+index+">");
	// Make sure we're not removing the KEY Attrbute.
	if (index == getKeyIndex())
	    throw new RuntimeException("Can't remove KEY field");
	remove (getAttribute(index).getName());
    }

    /** Removes the Attribute specified by the supplied field name.
     * One cannot remove a KEY field if it exists.  Note you shouldn't
     * call this method directly after this container has been added
     * to a Graph because Nodes will not carry the same number of
     * values as their Attributes container requires.
     * @param fieldName a String representing the name of the
     * Attribute field to be removed.
     * @throws RuntimeException if fieldName references the KEY field
     * or if the Attribute referenced by fieldName does not exist in
     * this container.
     * @see Graph#removeAttribute(String,int)
     */
    public void remove(String fieldName)
    {
	// Make sure we're not removing the KEY Attribute.
	if (key != null && key.getName().equals(fieldName))
	    throw new RuntimeException("Can't remove KEY field");
	// Figure out what index we're removing.
	final int index = getAttributeIndex(fieldName);
	// Now remove the Attribute.
	if (attrMap.remove(fieldName) == null)
	    throw new RuntimeException("Can't find Attribute field <"+fieldName+">");
	// Adjust the key index if necessary.
	if (index < getKeyIndex())
	    keyIndex--;
    }
    
    /** Get an Iterator over the Attribute fields; iteration is
     * performed in the order that each Attribute was added to this
     * container.  The Iterator is read-only.
     * @return an Iterator on the Attribute fields in this container.
     */
    public Iterator<Attribute> iterator()
    {
    	return Collections.unmodifiableCollection(attrMap.values()).iterator();
    }

    /** Check if the specified field name is already present among the
     * attributes in this container.
     * @param fieldName the field name to look for in this container.
     * @return true if the field name exists in this container.
     */
    public boolean contains(String fieldName)
    {
	return attrMap.containsKey(fieldName);
    }

    /** Get the Attribute with a particular name.
     * @param attribute the attribute name to look for in the container.
     * @return the Attribute with the matching attribute name or null
     * if it doesn't exist.
     */
    public Attribute getAttribute(String attribute)
    {
	return attrMap.get(attribute);
    }

    /** Get the Attribute at a particular index; indexes honor the
     * presence of a key Attribute within this container.  This method
     * is reflexive with {@link #getAttributeIndex(String)}.
     * @param attrIndex the attribute index to look for in the
     * container.
     * @return the Attribute at the matching index name or null
     * if it doesn't exist.
     */
    public Attribute getAttribute(int attrIndex)
    {
        int i = 0;
        for (final Attribute a : this)
            if (i++ == attrIndex)
                return a;
        return null;
    }

    /** Get the key Attribute from this container.
     * @return the key Attribute from this container or null if none
     * exists.
     */
    public AttributeKey getKey()
    {
	return key;
    }

    /** Get the key index from this container.
     * @return the key index from this container.
     */
    public int getKeyIndex()
    {
	return keyIndex;
    }

    /** Get the number of Attribute fields in this container including
     * the key Attribute (if one exists).
     * @return the number of attribute fields in this container.
     */
    public int attributeCount()
    {
	return attrMap.size();
    }
    
    /** Returns a hash code value for this object.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
	return name.hashCode();
    }
    
    /** Indicates whether some other object is "equal to" this one;
     * note this method assumes that each instance has a unique name
     * and uses the name for equality purposes.  The constraint on
     * name uniqueness is enforced by the Graph class which holds
     * Attributes.
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the argument; {@code false} otherwise.
     * @see Graph#addAttributes(Attributes)
     */
    public boolean equals(Object o)
    {
	if (this == o)
	    return true;
        if (o instanceof Attributes)
	    return name.equals(((Attributes)o).getName());
	return false;
    }
    
    /** Returns a String representation for this object.
     * @return a String representation for this object.
     */
    public String toString()
    {
	final StringBuilder sb = new StringBuilder('('+name+")[");
	for (final Attribute a : this)
	    sb.append(a);
	return sb.append(']').toString();
    }
}
