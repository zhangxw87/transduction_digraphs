/**
 * ReadClassificationGeneric.java
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
import netkit.graph.AttributeExpandableCategorical;
import netkit.classifiers.Classification;
import netkit.util.NetKitEnv;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Reads in true labels of entities in a graph from a file where the format of each line is:
 * <BLOCKQUOTE>
 * nodeID,class
 * </BLOCKQUOTE>
 * Lines starting with '#' are ignored
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class ReadClassificationGeneric implements ReadClassification
{
    private final Logger logger = NetKitEnv.getLogger(this);
    private static final Pattern pattern = Pattern.compile(",");

    /**
     * Creates a new Classification object based on the graph, nodeType and attribute and then calls
     * the generic readClassification method.
     * @param graph
     * @param nodeType
     * @param attribute
     * @param input
     * @return a filled-in Classification object
     * @see netkit.classifiers.io.ReadClassificationGeneric#readClassification(netkit.graph.Graph, netkit.classifiers.Classification, java.io.File)
     */
    public Classification readClassification(Graph graph, String nodeType, AttributeCategorical attribute, File input) {
        Classification labels = new Classification(graph, nodeType, attribute);
        labels.clear();
        readClassification(graph,labels,input);
        return labels;
    }

    /**
     * Reads in a set of classifications from the given file, assuming that each line
     * is of the form 'nodeID,class'. (Lines starting with '#' are ignored)
     * <P>
     * <STRONG>ASSUMPTION:</STRONG> The underlying categorical attribute has already
     * been initialized with the valid names for the attributes.   Otherwise exceptions
     * will be thrown!
     * @param graph
     * @param labels The Classification object to fill in
     * @param input
     */
    public void readClassification(Graph graph, Classification labels, File input) {
        AttributeCategorical attribute = labels.getAttribute();
        String nodeType = labels.getNodeType();

        int line = 1;
        try
        {
            logger.fine("ReadClassificationGeneric reading from '"+input.getName()+"'");
            logger.finer("Filling up classification:");
            logger.finer(labels.toString());

            BufferedReader br = new BufferedReader(new FileReader(input));
            for(String s=br.readLine();s!=null;s=br.readLine(),line++)
            {
                if(s.startsWith("#"))
                    continue;

                String[] tokens = pattern.split(s);
                if (tokens.length != 2)
                    throw new RuntimeException(input.getName()+":"+line
					       +" - Invalid-lineformat (got "+tokens.length
					       +" fields, expected 2) line: ["+s+"]");
		        String nodeName = tokens[0].intern();
                logger.finer("Getting node "+nodeName);
                Node n = graph.getNode(nodeName, nodeType);
                if(n == null)
                {
                    logger.finer("WARNING! Entity["+nodeName+"] not found in graph!");
                    continue;
                }
                String vName = tokens[1].intern();
                logger.finer("Getting class "+vName);
                int vIdx = -1;
                try
                {
                    vIdx = attribute.getValue(vName);
                }
                catch(Exception e)
                {
                    logger.log(Level.WARNING,"label '"+vName+"' is not known?",e);
                }
                if(vIdx == -1)
                {
                    if(attribute instanceof AttributeExpandableCategorical)
                    {
                        ((AttributeExpandableCategorical)attribute).addToken(vName);
                        vIdx = attribute.getValue(vName);
                    }
                    else
                    {
                        throw new RuntimeException("token "+vName+" was not found in categorical attribute and the categorical is not expandable!");
                    }
                }
                logger.finer("Entity["+n.getName()+":"+n.getIndex()+"] [Class: "+vName+":"+vIdx+"] [number of classes: "+attribute.size()+"]");
                labels.set(n,vIdx);
            }
        }
        catch(Throwable ioe)
        {
            throw new RuntimeException(input.getName()+":"+line+" - Error reading file?",ioe);
        }
    }
}
