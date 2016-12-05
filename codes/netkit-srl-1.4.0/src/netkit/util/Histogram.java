/**
 * Histogram.java
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

package netkit.util;

import netkit.graph.*;

import java.util.*;

/** This abstract class represents a histogram on Node field values.
 * The histogram keeps track of unique values and a running of how
 * many times each value appeared.
 * @see netkit.graph.Attribute
 * @see netkit.graph.Node
 * 
 * @author Kaveh R. Ghazi
 */
public abstract class Histogram
{
    // This map contains value->count entries.
    protected final Map<Integer,Double> cMap = new HashMap<Integer,Double>();
    // This keeps track of how many total values we kept after
    // flushing for minOccurance.
    private final double totalCount;
    // This keeps track of the attribute field type for the values in
    // this histogram.
    private final Attribute attribute;

    // This Comparator compares Map.Entry objects from the cMap by
    // comparing their values.
    private static final Comparator<Map.Entry<Integer,Double>> compValues
	= new Comparator<Map.Entry<Integer,Double>>()
    {
	public final int compare(Map.Entry<Integer,Double> o1,
				 Map.Entry<Integer,Double> o2)
	{ return o1.getValue().compareTo(o2.getValue()); }
    };

    // This Comparator compares Map.Entry objects from the cMap by
    // comparing their keys.
    private static final Comparator<Map.Entry<Integer,Double>> compKeys
            = new Comparator<Map.Entry<Integer,Double>>()
    {
	public final int compare(Map.Entry<Integer,Double> o1,
				 Map.Entry<Integer,Double> o2)
	{ return o1.getKey().compareTo(o2.getKey()); }
    };

    // A helper method used by constructors to initialize the count
    // map and return the totalCount after flushing for minOccurance.
    private double initialize(double[] values, int minOccurance)
    {
	if (minOccurance < 1)
	    throw new RuntimeException("Got minOccurance of " + minOccurance);

	// Build the count map with tallies for all values.
	cMap.clear();
	for (final double value : values)
	{
	    final Double count = cMap.get((int)value);
	    if (count == null)
		cMap.put((int)value, 1.0);
	    else
		cMap.put((int)value, count+1.0);
	}

	// Remove any elements whose count is less than minOccurance.
	for (final Iterator<Double> c = cMap.values().iterator(); c.hasNext(); )
	    if (c.next() < minOccurance)
		c.remove();

	// Initialize the total count, the return value of this method
	// should be set to the tc instance variable.
	double tc = 0;
	for (final double i : cMap.values())
	    tc += i;
	if (tc <= 0)
	    throw new RuntimeException("Total count must be greater than zero");
	return tc;
    }
    
    // A helper method used by the constructor taking a Node array as
    // input.  This method produces a double array with corresponding
    // values inserted from the Nodes.
    private static double[] getAllValues(Node[] nodes, String fieldName)
    {
	final double[] values = new double[nodes.length];

	for (int i=0; i<nodes.length; i++)
	    values[i] = nodes[i].getValue(fieldName);
	return values;
    }

    // A helper method used by the constructor taking an Edge array as
    // input.  This method produces a double array with corresponding
    // values inserted from the destination Nodes and weighted by the
    // edge weights
    private double initialize(Edge[] edges, String fieldName, int minOccurance)
    {
        if (minOccurance < 1)
            throw new RuntimeException("Got minOccurance of " + minOccurance);

        // Build the count map with tallies for all values.
        cMap.clear();
        for (final Edge edge : edges)
        {
            final int value = (int)edge.getDest().getValue(fieldName);
            final Double count = cMap.get(value);
            if (count == null)
            cMap.put((int)value, edge.getWeight());
            else
            cMap.put((int)value, count+edge.getWeight());
        }

        // Remove any elements whose count is less than minOccurance.
        for (final Iterator<Double> c = cMap.values().iterator(); c.hasNext(); )
            if (c.next() < minOccurance)
            c.remove();

        // Initialize the total count, the return value of this method
        // should be set to the tc instance variable.
        double tc = 0;
        for (final double i : cMap.values())
            tc += i;
        if (tc <= 0)
            throw new RuntimeException("Histogram("+attribute.getName()+","+fieldName+"): count must be greater than zero (numedges="+edges.length+")");
        return tc;
    }

    /** This constructor creates a histogram object given an array of
     * values and an attribute type.  It also checks that values
     * appear at least minOccurance times before including them.
     * @param values the array of double values for this object.
     * @param attribute the attribute describing the field type.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    protected Histogram(double[] values, Attribute attribute, int minOccurance)
    {
	this.attribute = attribute;
	totalCount = initialize(values, minOccurance);
    }
    
    /** This constructor creates a histogram object given an array of
     * nodes and an attribute from which to get the values.  It also
     * checks that values appear at least minOccurance times before
     * including them.
     * @param nodes the array of Nodes from which to get values.
     * @param attribute the attribute describing the field in the Node
     * to get values from.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    protected Histogram(Node[] nodes, Attribute attribute, int minOccurance)
    {
	this.attribute = attribute;
	final double[] values = getAllValues(nodes, attribute.getName());
	totalCount = initialize(values, minOccurance);
    }

    /** This constructor creates a histogram object given an array of
     * edges and an attribute from which to get the values.  Each edge
     * counts the destination-node's attribute edge-weight times.  It also
     * checks that values appear at least minOccurance times before
     * including them.
     * @param edges the array of Edges from which to get weight + attribute values.
     * @param attribute the attribute describing the field in the Node
     * to get values from.
     * @param minOccurance the minimum number of times a value must
     * occur before being kept in this histogram.
     * @throws RuntimeException if minOccurance is less than 1.
     */
    protected Histogram(Edge[] edges, Attribute attribute, int minOccurance)
    {
	this.attribute = attribute;
	totalCount = initialize(edges, attribute.getName(), minOccurance);
    }

    /** Gets the "mode" for this set of values.  The mode is the most
     * frequent value, i.e. the entry with the highest count in cMap.
     * If multiple entries tie for highest count, the one returned is
     * unspecified.
     * @return the mode for this set of values.
     */
    public int getMode()
    {
	return Collections.max(cMap.entrySet(), compValues).getKey();
    }

    /** Gets the number of times a particular value appears in this
     * histogram.
     * @param value the value to lookup in the histogram.
     * @return the number of times a particular value appears in this
     * histogram.
     */
    public double getCount(int value)
    {
	Double count = cMap.get(value);
        return ( (count==null) ? 0 : count.doubleValue() );
    }

    /** Gets the cumulative number of times all values appear in this
     * histogram.
     * @return the cumulative number of times all values appear in
     * this histogram.
     */
    public double getTotalCount()
    {
	return totalCount;
    }

    /** Gets the distribution of values of this histogram, in no
     * particular order.
     * @return the distribution of values of this histogram.
     */
    public double[] getDistribution()
    {
	final double[] result = new double[cMap.size()];

	int i=0;
	for (final Double v : cMap.values())
	    result[i++] = v;
	return result;
    }

    /** Gets the set of value->count pairs in this histogram.
     * @return the set of value->count pairs in this histogram.  The
     * returned set is not modifiable.
     */
    public Set<Map.Entry<Integer,Double>> getSet()
    {
	return Collections.unmodifiableSet(cMap.entrySet());
    }

    // Helper method for the main driver.
    private static void output(HistogramDiscrete h)
    {
	System.err.print("Discrete values are: ");
	final SortedSet<Map.Entry<Integer,Double>> set
	    = new TreeSet<Map.Entry<Integer,Double>>(compKeys);
	set.addAll(h.getSet());
	for (final Map.Entry<Integer,Double> me : set)
	    System.err.printf(" (%d->%f)", me.getKey(), me.getValue());
	System.err.println();
	System.err.printf("Mode=%d count=%f totalCount=%f\n", h.getMode(),
			  h.getCount(h.getMode()), h.getTotalCount());
	System.err.print("Distribution is: ");
	for (final double i : h.getDistribution())
	    System.err.printf(" %f", i);
	System.err.println();
	System.err.printf("Max=%d, min=%d, mean=%f, median=%f\n\n",
			  h.getMaxValue(), h.getMinValue(),
			  h.getMeanValue(), h.getMedianValue());
    }
    
    // Helper method for the main driver.
    private static void output(HistogramCategorical h)
    {
	System.err.print("Categorical values are: ");
	final SortedSet<Map.Entry<Integer,Double>> set
	    = new TreeSet<Map.Entry<Integer,Double>>(compKeys);
	set.addAll(h.getSet());
	for (final Map.Entry<Integer,Double> me : set)
	    System.err.printf(" (%s(%d)->%f)",
			      h.getAttribute().getToken(me.getKey()),
			      me.getKey(), me.getValue());
	System.err.println();
	System.err.printf("Mode=%s(%d) count=%f totalCount=%f\n",
			  h.getAttribute().getToken(h.getMode()),
			  h.getMode(), h.getCount(h.getMode()),
			  h.getTotalCount());
	System.err.print("Distribution is: ");
	for (final double i : h.getDistribution())
	    System.err.printf(" %f", i);
	System.err.println();
	System.err.println();
    }
    
    /** This is a main driver to test the Histogram hierarchy classes.
     */
    public static final void main(String args[])
    {
	final AttributeDiscrete ad = new AttributeDiscrete("DiceRoll");
	final FixedTokenSet fts =
	    new FixedTokenSet(new String[] {"Raw","Rare","MediumRare",
					    "Medium","MediumWell","Well"});
	final AttributeFixedCategorical ac =
	    new AttributeFixedCategorical("SteakCooking", fts);
	final AttributeKey ak = new AttributeKey("keys");
	final double[] valuesD = new double[100];
	final double[] valuesS = new double[100];
	final double[] valuesK = new double[15];
	final Random random = new Random(System.currentTimeMillis());

	for (int i=0; i<valuesD.length; i++)
	    valuesD[i] = random.nextDouble()*6 + 1.0 + random.nextDouble()*6 + 1.0;
	for (int i=0; i<valuesS.length; i++)
	    valuesS[i] = random.nextDouble()*(double)(fts.size());
	for (int i=0; i<valuesK.length; i++)
	    valuesK[i] = ak.parseAndInsert("token"+i);

	final HistogramDiscrete hd = new HistogramDiscrete(valuesD, ad);
	final HistogramDiscrete hd2 = new HistogramDiscrete(valuesD, ad, 8);
	final HistogramCategorical hc = new HistogramCategorical(valuesS, ac);
	final HistogramCategorical hc2 = new HistogramCategorical(valuesS, ac, 16);
	final HistogramCategorical hk = new HistogramCategorical(valuesK, ak);

	output(hd);
	output(hd2);

	output(hc);
	output(hc2);

	output(hk);
    }
}
