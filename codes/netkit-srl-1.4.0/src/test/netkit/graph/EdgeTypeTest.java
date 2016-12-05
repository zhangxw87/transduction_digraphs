/**
 * EdgeTypeTest.java
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
 * EdgeType Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class EdgeTypeTest extends TestCase
{
    private EdgeType et1, et2, et3;

    public EdgeTypeTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	et1 = new EdgeType("MAtype", "Movie", "Actor");
	et2 = new EdgeType("MStype", "Movie", "Studio");
	et3 = new EdgeType("MAtype", "Movie2", "Actor2");
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	et1 = et2 = et3 = null;
    }

    public void testGetName() throws Exception
    {
	assertEquals("MAtype", et1.getName());
	assertEquals("MStype", et2.getName());
	assertEquals("MAtype", et3.getName());
    }

    public void testGetSourceType() throws Exception
    {
	assertEquals("Movie", et1.getSourceType());
	assertEquals("Movie", et2.getSourceType());
	assertEquals("Movie2", et3.getSourceType());
    }

    public void testGetDestType() throws Exception
    {
	assertEquals("Actor", et1.getDestType());
	assertEquals("Studio", et2.getDestType());
	assertEquals("Actor2", et3.getDestType());
    }

    public void testHashCode() throws Exception
    {
	assertEquals(et1.getName().hashCode(), et1.hashCode());
	assertEquals(et2.getName().hashCode(), et2.hashCode());
	assertEquals(et3.getName().hashCode(), et3.hashCode());
    }

    public void testEquals() throws Exception
    {
	assertTrue(et1.equals(et1));
	assertFalse(et1.equals(et2));
	assertTrue(et1.equals(et3));

	assertFalse(et2.equals(et1));
	assertTrue(et2.equals(et2));
	assertFalse(et2.equals(et3));

	assertTrue(et3.equals(et1));
	assertFalse(et3.equals(et2));
	assertTrue(et3.equals(et3));
    }

    public void testToString() throws Exception
    {
	assertEquals("[EdgeType(name=<MAtype>,sourceType=<Movie>,destType=<Actor>)]", et1.toString());
	assertEquals("[EdgeType(name=<MStype>,sourceType=<Movie>,destType=<Studio>)]", et2.toString());
	assertEquals("[EdgeType(name=<MAtype>,sourceType=<Movie2>,destType=<Actor2>)]", et3.toString());
    }

    public static Test suite()
    {
        return new TestSuite(EdgeTypeTest.class);
    }
}
