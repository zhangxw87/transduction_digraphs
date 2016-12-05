/**
 * FixedTokenSetTest.java
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
 * FixedTokenSet Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class FixedTokenSetTest extends TokenSetAbstract
{
    private FixedTokenSet fts;

    public FixedTokenSetTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	try { // Duplicate token
	    fts = new FixedTokenSet(new String[] {"foo","foo"});
	    fail();
	}
	catch (RuntimeException success) { }

	fts = new FixedTokenSet(new String[] {"Low","Medium","High" });
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	fts = null;
    }

    public void testContains() throws Exception
    {
	assertTrue(fts.contains("Low"));
	assertTrue(fts.contains("Medium"));
	assertTrue(fts.contains("High"));
	assertFalse(fts.contains("foo"));
	assertFalse(fts.contains("bar"));
	assertFalse(fts.contains("baz"));
    }

    public void testGetTokens() throws Exception
    {
	assertTrue(java.util.Arrays.equals(new String[] {"Low","Medium","High" },
					   fts.getTokens()));
    }

    public void testGetToken() throws Exception
    {
	assertEquals("Low", fts.getToken(0));
	assertEquals("Medium", fts.getToken(1));
	assertEquals("High", fts.getToken(2));

	try { // Invalid index
	    fts.getToken(-1);
	    fail();
	}
	catch (IndexOutOfBoundsException success) { }

	try { // Invalid index
	    fts.getToken(3);
	    fail();
	}
	catch (IndexOutOfBoundsException success) { }
    }

    public void testSize() throws Exception
    {
	assertEquals(3, fts.size());
    }

    public void testGetValue() throws Exception
    {
	assertEquals(0, fts.getValue("Low"));
	assertEquals(1, fts.getValue("Medium"));
	assertEquals(2, fts.getValue("High"));

	try { // Invalid token
	    fts.getValue("foo");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    fts.getValue("bar");
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid token
	    fts.getValue("baz");
	    fail();
	}
	catch (RuntimeException success) { }
    }

    public static Test suite()
    {
        return new TestSuite(FixedTokenSetTest.class);
    }
}
