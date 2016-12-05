/**
 * ExpandableTokenSetTest.java
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
 * ExpandableTokenSet Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class ExpandableTokenSetTest extends TokenSetAbstract
{
    private ExpandableTokenSet ets;

    public ExpandableTokenSetTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	ets = new ExpandableTokenSet();
	ets.add("Low");
	ets.add("Medium");
	ets.add("High");
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	ets = null;
    }

    public void testAdd() throws Exception
    {
	// The setup() method already tested valid additions.
	try { // Duplicate token
	    ets.add("Low");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Duplicate token
	    ets.add("Medium");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Duplicate token
	    ets.add("High");
	    fail();
	}
	catch (RuntimeException success) { }
	
	ets.add("blah");
	
	assertTrue(java.util.Arrays.equals(new String[] {"Low","Medium","High","blah"},
					   ets.getTokens()));
    }

    public void testContains() throws Exception
    {
	assertTrue(ets.contains("Low"));
	assertTrue(ets.contains("Medium"));
	assertTrue(ets.contains("High"));
	assertFalse(ets.contains("foo"));
	assertFalse(ets.contains("bar"));
	assertFalse(ets.contains("baz"));
    }

    public void testGetTokens() throws Exception
    {
	assertTrue(java.util.Arrays.equals(new String[] {"Low","Medium","High" },
					   ets.getTokens()));
    }

    public void testGetToken() throws Exception
    {
	assertEquals("Low", ets.getToken(0));
	assertEquals("Medium", ets.getToken(1));
	assertEquals("High", ets.getToken(2));

	try { // Invalid index
	    ets.getToken(-1);
	    fail();
	}
	catch (RuntimeException success) { }

	try { // Invalid index
	    ets.getToken(3);
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public void testSize() throws Exception
    {
	assertEquals(3, ets.size());
    }

    public void testGetValue() throws Exception
    {
	assertEquals(0, ets.getValue("Low"));
	assertEquals(1, ets.getValue("Medium"));
	assertEquals(2, ets.getValue("High"));

	try { // Invalid token
	    ets.getValue("foo");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    ets.getValue("bar");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    ets.getValue("baz");
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public static Test suite()
    {
        return new TestSuite(ExpandableTokenSetTest.class);
    }
}
