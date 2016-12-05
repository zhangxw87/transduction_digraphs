/**
 * AttributeContinuousTest.java
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
 * AttributeContinuous Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class AttributeContinuousTest extends AttributeAbstract
{
    private AttributeContinuous attr;

    public AttributeContinuousTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	attr = new AttributeContinuous("attr");
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
	assertEquals(Type.CONTINUOUS, attr.getType());
    }
    
    public void testToString() throws Exception
    {
	assertEquals("(attr->CONTINUOUS)", attr.toString());
    }
    
    public void testParseAndInsert() throws Exception
    {
	assertEquals(Double.NaN, attr.parseAndInsert("?"));
	assertEquals(1.5, attr.parseAndInsert("1.5"));
	assertEquals(-2.5, attr.parseAndInsert("-2.5"));

	try { // Invalid double
	    attr.parseAndInsert("blah");
	    fail();
	}
	catch (NumberFormatException success) { }
    }

    public void testFormatForOutput() throws Exception
    {
	assertEquals(attr.formatForOutput(Double.NaN), "?");
	assertEquals(attr.formatForOutput(1.5), "1.5");
	assertEquals(attr.formatForOutput(-2.5), "-2.5");
    }
    
    public static Test suite()
    {
        return new TestSuite(AttributeContinuousTest.class);
    }
}
