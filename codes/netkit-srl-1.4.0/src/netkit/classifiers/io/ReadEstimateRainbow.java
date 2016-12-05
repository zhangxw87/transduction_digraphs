/**
 * ReadEstimateRainbow.java
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
package netkit.classifiers.io;

import netkit.graph.Node;
import netkit.graph.Graph;
import netkit.graph.AttributeCategorical;
import netkit.classifiers.Estimate;
import netkit.util.NetKitEnv;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Reads in estimates of entities in a graph from a file where the format of each line is:
 * <BLOCKQUOTE>
 * nodeID trueclass class:score ... class:score
 * </BLOCKQUOTE>
 * Lines starting with '#' are ignored.
 * This line format is equivalent to what you would see in the output from the Rainbow
 * text classification package developed by Andrew McCallum.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */

public final class ReadEstimateRainbow implements ReadEstimate
{
    private final Logger logger = NetKitEnv.getLogger(this);

    private static final Pattern fieldSplitter = Pattern.compile(" ");
    private static final Pattern scoreSplitter = Pattern.compile(":");

    /**
     * Create a new Estimate object and call the more general readEstimate method.
     * @param graph
     * @param nodeType
     * @param attribute
     * @param input
     * @return A new Estimate object filled in with Estimates from the input file
     */
    public Estimate readEstimate(Graph graph, String nodeType, AttributeCategorical attribute, File input) {
        Estimate estimates = new Estimate(graph, nodeType, attribute);
        readEstimate(graph,estimates,input);
        return estimates;
    }

    /**
     * Reads in a set of estimates from the given file, assuming that each line
     * is of the form:
     * <BLOCKQUOTE>
     * nodeID trueclass class:score ... class:score
     * </BLOCKQUOTE>
     * where all classes in the underlying categorical attribute will be given a score.
     * <P>
     * <STRONG>ASSUMPTION:</STRONG> The underlying categorical attribute has already
     * been initialized with the valid names for the attributes.   Otherwise exceptions
     * will be thrown!
     * @param graph
     * @param estimates
     * @param input
     */
    public void readEstimate(Graph graph, Estimate estimates, File input)
    {
        AttributeCategorical attribute = estimates.getAttribute();
        String nodeType = estimates.getNodeType();
        int numFields = attribute.size() + 2; // add 1 each for nodeID and trueClass

        int line = 0;
        try
        {
            logger.fine("ReadEstimateRainbow reading from '"+input.getName()+"'");
            logger.finer("Filling up estimates:");
            logger.finer(estimates.toString());

            BufferedReader br = new BufferedReader(new FileReader(input));
            for(String s=br.readLine();s!=null;s=br.readLine(),line++)
            {
                if(s.startsWith("#"))
                    continue;
                String[] vals = fieldSplitter.split(s);
		        if (vals.length != numFields)
                    throw new RuntimeException(input.getName()+":"+line
					       +" - Invalid-lineformat (got "+vals.length
					       +" fields, expected "+numFields+") line: ["+s+"]");
		        String nodeName = vals[0].intern();
                logger.finest("Getting node "+nodeName);
                Node n = graph.getNode(nodeName, nodeType);
                if(n == null)
                {
                    logger.warning("WARNING! Entity["+nodeName+"] not found in graph!");
                    continue;
                }
                double[] scores = new double[attribute.size()];
                Arrays.fill(scores,0);
                for(int i=2;i<numFields;i++)
                {
                    String[] scorePair = scoreSplitter.split(vals[i]);
                    if(scorePair.length != 2)
                        throw new RuntimeException(input.getName()+":"+line
                               +" - Invalid-fieldformat (got "+scorePair.length
                               +" fields, expected 2) field["+i+"]: ["+vals[i]+"]");
                    double score = Double.parseDouble(scorePair[1]);

                    logger.finest("Getting class "+scorePair[0]);
                    int idx = -1;
                    try
                    {
                        idx = attribute.getValue(scorePair[0]);
                    }
                    catch(Exception e)
                    {
                        logger.log(Level.WARNING, "   failed to get '"+scorePair[0]+": ",e);
                    }
                    scores[idx] = score;
                }
                estimates.estimate(n,scores);
            }
        }
        catch(Throwable ioe)
        {
            throw new RuntimeException(input.getName()+":"+line+" - Error reading file?",ioe);
        }
    }
}
