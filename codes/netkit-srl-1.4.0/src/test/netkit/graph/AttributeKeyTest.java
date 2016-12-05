/**
 * AttributeKeyTest.java
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
 * AttributeKey Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class AttributeKeyTest extends AttributeCategoricalAbstract
{
    private AttributeKey attr;

    public AttributeKeyTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	attr = new AttributeKey("attr");
	attr.parseAndInsert("key1");
	attr.parseAndInsert("key2");
	attr.parseAndInsert("key3");
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
	assertTrue(java.util.Arrays.equals(new String[] {"key1","key2","key3" },
					   attr.getTokens()));
    }

    public void testGetToken() throws Exception
    {
	assertEquals("key1", attr.getToken(0));
	assertEquals("key2", attr.getToken(1));
	assertEquals("key3", attr.getToken(2));

	try { // Invalid token index
	    attr.getToken(-1);
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Invalid token index
	    attr.getToken(3);
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public void testSize() throws Exception
    {
	assertEquals(3, attr.size());
    }

    public void testGetValue() throws Exception
    {
	assertEquals(0, attr.getValue("key1"));
	assertEquals(1, attr.getValue("key2"));
	assertEquals(2, attr.getValue("key3"));

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

    public void testAddToken() throws Exception
    {
	try { // Duplicate Key
	    attr.addToken("key1");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Duplicate Key
	    attr.addToken("key2");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Duplicate Key
	    attr.addToken("key3");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Unknown Key
	    attr.addToken("?");
	    fail();
	}
	catch (RuntimeException success) { }

	attr.addToken("blah");

	assertTrue(java.util.Arrays.equals(new String[] {"key1","key2","key3","blah"},
					   attr.getTokens()));
    }

    public void testParseAndInsert() throws Exception
    {
	try { // Duplicate Key
	    attr.parseAndInsert("key1");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Duplicate Key
	    attr.parseAndInsert("key2");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Duplicate Key
	    attr.parseAndInsert("key3");
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Unknown Key
	    attr.parseAndInsert("?");
	    fail();
	}
	catch (RuntimeException success) { }

	attr.parseAndInsert("blah");

	assertTrue(java.util.Arrays.equals(new String[] {"key1","key2","key3","blah"},
					   attr.getTokens()));
    }

    public void testFormatForOutput() throws Exception
    {
	assertEquals(attr.formatForOutput(Double.NaN), "?");
	assertEquals(attr.formatForOutput(0.0), "key1");
	assertEquals(attr.formatForOutput(1.0), "key2");
	assertEquals(attr.formatForOutput(2.0), "key3");
	try { // Invalid token
	    attr.formatForOutput(3.0);
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public static Test suite()
    {
        return new TestSuite(AttributeKeyTest.class);
    }
}
