/**
 * EdgeTest.java
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
 * Edge Tester.
 *
 * @author Kaveh R. Ghazi
 * @since <pre>12/06/2004</pre>
 * @version 1.0
 */
public class EdgeTest extends TestCase
{
    private Edge ema;
    
    public EdgeTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
	final EdgeType etMA = new EdgeType("etMA", "Movie", "Actor");
	final Attributes actorType = new Attributes("Actor");
	actorType.add(new AttributeKey("Name"));
	final Attributes movieType = new Attributes("Movie");
	movieType.add(new AttributeKey("Name"));
	final Node movie1 = new Node("movie1", movieType, 0);
	final Node actor1 = new Node("actor1", actorType, 2);
	ema = new Edge(etMA, movie1, actor1, 1.0);

	try { // Invalid EdgeType constaints
	    new Edge(etMA, movie1, movie1, 1.0);
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Invalid EdgeType constaints
	    new Edge(etMA, actor1, actor1, 1.0);
	    fail();
	}
	catch (RuntimeException success) { }
	
	try { // Negative weight
	    new Edge(etMA, movie1, actor1, -1.0);
	    fail();
	}
	catch (IllegalArgumentException success) { }
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
	ema = null;
    }

    public void testAddWeight() throws Exception
    {
	assertEquals(1.0, ema.getWeight());
	ema.addWeight(3.5);
	assertEquals(4.5, ema.getWeight());

	try { // Invalid weight
	    ema.addWeight(-2.0);
	    fail();
	}
	catch (IllegalArgumentException success) { }

	assertEquals(4.5, ema.getWeight());
    }

    public void testGetWeight() throws Exception
    {
	assertEquals(1.0, ema.getWeight());
    }

    public void testGetSource() throws Exception
    {
	assertEquals("movie1", ema.getSource().getName());
    }

    public void testGetDest() throws Exception
    {
	assertEquals("actor1", ema.getDest().getName());
    }

    public void testGetEdgeType() throws Exception
    {
	assertEquals("etMA", ema.getEdgeType().getName());
    }

    public void testHashCode() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testEquals() throws Exception
    {
        //TODO: Test goes here...
    }

    public void testToString() throws Exception
    {
	assertEquals("[Edge(movie1,actor1,1.0)]", ema.toString());
    }

    public static Test suite()
    {
        return new TestSuite(EdgeTest.class);
    }
}
