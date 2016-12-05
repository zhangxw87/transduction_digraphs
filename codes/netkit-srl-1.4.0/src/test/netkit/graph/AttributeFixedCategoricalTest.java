/**
 * AttributeFixedCategoricalTest.java
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

/**
 * AttributeFixedCategorical Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class AttributeFixedCategoricalTest extends AttributeCategoricalAbstract
{
    private AttributeFixedCategorical attr;

    public AttributeFixedCategoricalTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	final FixedTokenSet fts = new FixedTokenSet(new String[] {"Low","Medium","High" });
	attr = new AttributeFixedCategorical("attr", fts);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	attr = null;
    }

    public void testGetName() throws Exception
    {
	assertEquals("attr", attr.getName());
    }
    
    public void testGetType() throws Exception
    {
	assertEquals(Type.CATEGORICAL, attr.getType());
    }
    
    public void testToString() throws Exception
    {
	assertEquals("(attr->CATEGORICAL)", attr.toString());
    }
    
    public void testGetTokens() throws Exception
    {
	assertTrue(java.util.Arrays.equals(new String[] {"Low","Medium","High" },
					   attr.getTokens()));
    }

    public void testGetToken() throws Exception
    {
	assertEquals("Low", attr.getToken(0));
	assertEquals("Medium", attr.getToken(1));
	assertEquals("High", attr.getToken(2));

	try { // Invalid token index
	    attr.getToken(-1);
	    fail();
	}
	catch (IndexOutOfBoundsException success) { }

	try { // Invalid token index
	    attr.getToken(3);
	    fail();
	}
	catch (IndexOutOfBoundsException success) { }
    }

    public void testSize() throws Exception
    {
	assertEquals(3, attr.size());
    }

    public void testGetValue() throws Exception
    {
	assertEquals(0, attr.getValue("Low"));
	assertEquals(1, attr.getValue("Medium"));
	assertEquals(2, attr.getValue("High"));

	try { // Invalid token
	    attr.getValue("foo");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    attr.getValue("bar");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    attr.getValue("baz");
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public void testParseAndInsert() throws Exception
    {
	assertEquals(Double.NaN, attr.parseAndInsert("?"));
	assertEquals(0.0, attr.parseAndInsert("Low"));
	assertEquals(1.0, attr.parseAndInsert("Medium"));
	assertEquals(2.0, attr.parseAndInsert("High"));

	try { // New token into fixed set
	    attr.parseAndInsert("foo");
	    fail();
	}
	catch (RuntimeException success) { }

	assertTrue(java.util.Arrays.equals(new String[] {"Low","Medium","High" },
					   attr.getTokens()));
    }

    public void testFormatForOutput() throws Exception
    {
	assertEquals(attr.formatForOutput(Double.NaN), "?");
	assertEquals(attr.formatForOutput(0.0), "Low");
	assertEquals(attr.formatForOutput(1.0), "Medium");
	assertEquals(attr.formatForOutput(2.0), "High");
	try { // Invalid token
	    attr.formatForOutput(3.0);
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public static Test suite()
    {
        return new TestSuite(AttributeFixedCategoricalTest.class);
    }
}
