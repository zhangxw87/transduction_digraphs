/**
 * AttributesTest.java
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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import netkit.classifiers.Estimate;

/**
 * Attributes Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class AttributesTest extends TestCase
{
    private Attributes attrs;
    
    public AttributesTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	attrs = new Attributes("myAttributes");
	attrs.add(new AttributeDiscrete("attrField0"));
	attrs.add(new AttributeKey("keyField"));
	attrs.add(new AttributeContinuous("attrField1"));
	final FixedTokenSet fts = new FixedTokenSet(new String[] {"Low","Medium","High" });
	attrs.add(new AttributeFixedCategorical("attrField2", fts));
	attrs.add(new AttributeExpandableCategorical("attrField3"));
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	attrs = null;
    }

    public void testAdd() throws Exception
    {
	// The setup method has already tested successful additions.

	try { // Duplicate field
	    attrs.add(new AttributeDiscrete("attrField1"));
	    fail();
	}
        catch (RuntimeException success) { }

	try { // Second KEY
	    attrs.add(new AttributeKey("attrField10"));
	    fail();
	}
	catch (RuntimeException success) { }

	assertEquals(5, attrs.attributeCount());
   }

    public void testGetName() throws Exception
    {
	assertEquals("myAttributes", attrs.getName());
    }

    public void testGetAttributeIndex() throws Exception
    {
	assertEquals(0, attrs.getAttributeIndex("attrField0"));
	assertEquals(1, attrs.getAttributeIndex("keyField"));
	assertEquals(2, attrs.getAttributeIndex("attrField1"));
	assertEquals(3, attrs.getAttributeIndex("attrField2"));
	assertEquals(4, attrs.getAttributeIndex("attrField3"));
	
	try { // Field name doesn't exist
	    attrs.getAttributeIndex("foo");
	    fail();
	}
        catch (RuntimeException success) { }
    }

    public void testRemove() throws Exception
    {
	int keyIndex = attrs.getKeyIndex();
	
	try { // Invalid index
	    attrs.remove(-1);
	    fail();
	}
        catch (RuntimeException success) { }
	try { // Invalid index
	    attrs.remove(attrs.attributeCount());
	    fail();
	}
        catch (RuntimeException success) { }
	try { // KEY index
	    attrs.remove(keyIndex);
	    fail();
	}
        catch (RuntimeException success) { }

	assertEquals(attrs.attributeCount(), 5);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"attrField0");
	assertEquals(attrs.getAttribute(1).getName(),"keyField");
	assertEquals(attrs.getAttribute(2).getName(),"attrField1");
	assertEquals(attrs.getAttribute(3).getName(),"attrField2");
	assertEquals(attrs.getAttribute(4).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("attrField0"), 0);
	assertEquals(attrs.getAttributeIndex("keyField"), 1);
	assertEquals(attrs.getAttributeIndex("attrField1"), 2);
	assertEquals(attrs.getAttributeIndex("attrField2"), 3);
	assertEquals(attrs.getAttributeIndex("attrField3"), 4);

	attrs.remove(3);
	assertEquals(attrs.attributeCount(), 4);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"attrField0");
	assertEquals(attrs.getAttribute(1).getName(),"keyField");
	assertEquals(attrs.getAttribute(2).getName(),"attrField1");
	assertEquals(attrs.getAttribute(3).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("attrField0"), 0);
	assertEquals(attrs.getAttributeIndex("keyField"), 1);
	assertEquals(attrs.getAttributeIndex("attrField1"), 2);
	assertEquals(attrs.getAttributeIndex("attrField3"), 3);
	
	attrs.remove(0); keyIndex--;
	assertEquals(attrs.attributeCount(), 3);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttribute(2).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	assertEquals(attrs.getAttributeIndex("attrField3"), 2);
	
	try { // KEY index
	    attrs.remove(keyIndex);
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 3);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttribute(2).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	assertEquals(attrs.getAttributeIndex("attrField3"), 2);

	attrs.remove(2);
	assertEquals(attrs.attributeCount(), 2);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	
	try { // Invalid index
	    attrs.remove(attrs.attributeCount());
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 2);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);

	attrs.remove(1);
	assertEquals(attrs.attributeCount(), 1);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);

	try { // KEY index
	    attrs.remove(keyIndex);
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 1);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
    }
    
    public void testRemove1() throws Exception
    {
	int keyIndex = attrs.getKeyIndex();
	
	try { // Invalid field
	    attrs.remove("blah");
	    fail();
	}
        catch (RuntimeException success) { }
	try { // KEY field
	    attrs.remove("keyField");
	    fail();
	}
        catch (RuntimeException success) { }

	assertEquals(attrs.attributeCount(), 5);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"attrField0");
	assertEquals(attrs.getAttribute(1).getName(),"keyField");
	assertEquals(attrs.getAttribute(2).getName(),"attrField1");
	assertEquals(attrs.getAttribute(3).getName(),"attrField2");
	assertEquals(attrs.getAttribute(4).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("attrField0"), 0);
	assertEquals(attrs.getAttributeIndex("keyField"), 1);
	assertEquals(attrs.getAttributeIndex("attrField1"), 2);
	assertEquals(attrs.getAttributeIndex("attrField2"), 3);
	assertEquals(attrs.getAttributeIndex("attrField3"), 4);

	attrs.remove("attrField2");
	assertEquals(attrs.attributeCount(), 4);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"attrField0");
	assertEquals(attrs.getAttribute(1).getName(),"keyField");
	assertEquals(attrs.getAttribute(2).getName(),"attrField1");
	assertEquals(attrs.getAttribute(3).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("attrField0"), 0);
	assertEquals(attrs.getAttributeIndex("keyField"), 1);
	assertEquals(attrs.getAttributeIndex("attrField1"), 2);
	assertEquals(attrs.getAttributeIndex("attrField3"), 3);
	
	attrs.remove("attrField0"); keyIndex--;
	assertEquals(attrs.attributeCount(), 3);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttribute(2).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	assertEquals(attrs.getAttributeIndex("attrField3"), 2);
	
	try { // KEY field
	    attrs.remove("keyField");
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 3);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttribute(2).getName(),"attrField3");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	assertEquals(attrs.getAttributeIndex("attrField3"), 2);

	attrs.remove("attrField3");
	assertEquals(attrs.attributeCount(), 2);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);
	
	try { // Invalid field
	    attrs.remove("blahblah");
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 2);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttribute(1).getName(),"attrField1");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
	assertEquals(attrs.getAttributeIndex("attrField1"), 1);

	attrs.remove("attrField1");
	assertEquals(attrs.attributeCount(), 1);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);

	try { // KEY field
	    attrs.remove("keyField");
	    fail();
	}
        catch (RuntimeException success) { }
	assertEquals(attrs.attributeCount(), 1);
	assertNull(attrs.getAttribute(attrs.attributeCount()));
	assertEquals(attrs.getKeyIndex(), keyIndex);
	assertEquals(attrs.getAttribute(0).getName(),"keyField");
	assertEquals(attrs.getAttributeIndex("keyField"), 0);
    }
    
    public void testIterator() throws Exception
    {
	final java.util.Iterator<Attribute> it = attrs.iterator();
	assertTrue(it.hasNext());
	assertEquals("attrField0", it.next().getName());

	try { // read-only iterator
	    it.remove();
	    fail();
        }
        catch (UnsupportedOperationException success) { }

	assertEquals("keyField", it.next().getName());
	assertEquals("attrField1", it.next().getName());
	assertEquals("attrField2", it.next().getName());
	assertEquals("attrField3", it.next().getName());
	assertFalse(it.hasNext());
    }

    public void testContains() throws Exception
    {
	assertTrue(attrs.contains("attrField0"));
	assertTrue(attrs.contains("keyField"));
	assertTrue(attrs.contains("attrField1"));
	assertTrue(attrs.contains("attrField2"));
	assertTrue(attrs.contains("attrField3"));
	assertFalse(attrs.contains("attrField10"));
    }

    public void testGetAttribute() throws Exception
    {
	assertEquals("attrField0", attrs.getAttribute("attrField0").getName());
	assertEquals("keyField", attrs.getAttribute("keyField").getName());
	assertEquals("attrField1", attrs.getAttribute("attrField1").getName());
	assertEquals("attrField2", attrs.getAttribute("attrField2").getName());
	assertEquals("attrField3", attrs.getAttribute("attrField3").getName());
	assertNull(attrs.getAttribute("attrField10"));

	assertEquals("attrField0", attrs.getAttribute(0).getName());
	assertEquals("keyField", attrs.getAttribute(1).getName());
	assertEquals("attrField1", attrs.getAttribute(2).getName());
	assertEquals("attrField2", attrs.getAttribute(3).getName());
	assertEquals("attrField3", attrs.getAttribute(4).getName());
	assertNull(attrs.getAttribute(5));
	assertNull(attrs.getAttribute(10));
    }

    public void testGetKey() throws Exception
    {
	assertEquals("keyField", attrs.getKey().getName());
    }

    public void testGetKeyIndex() throws Exception
    {
	assertEquals(1, attrs.getKeyIndex());
    }

    public void testAttributeCount() throws Exception
    {
	assertEquals(5, attrs.attributeCount());
    }

    public void testHashCode() throws Exception
    {
	assertEquals(attrs.getName().hashCode(), attrs.hashCode());
    }

    public void testEquals() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testToString() throws Exception
    {
        //TODO: Test goes here...
    }

    public static Test suite()
    {
        return new TestSuite(AttributesTest.class);
    }
}
