/**
 * ReadPrior.java
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

import netkit.graph.AttributeCategorical;
import netkit.graph.AttributeExpandableCategorical;
import netkit.util.NetKitEnv;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.io.BufferedReader;

/**
 * This class reads in class priors from a file such that a user can specify the priors
 * rather than have the classifier use the priors that are estimated from training examples.
 * The prior file should be of the format:
 * <PRE>
 * value,prior-score
 * ...
 * </PRE>
 * Any line that starts with '#' is ignored (treated as a comment).
 * The 'value' in the line is the likelihood that the class attribute takes on that value.
 * The scores are not checked to see if they add to 1.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 *
 * @see netkit.graph.AttributeCategorical
 */
public final class ReadPrior {
    private static final Logger logger = NetKitEnv.getLogger(ReadPrior.class.getName());


    /**
     * Reads in a file using the file format mentioned in the header.  It returns the vector
     * of scores for the values in the passed-in AttriuteCategorical--the value read in the file
     * are matched with the index into the categorical attribute list of known values.  The values in the
     * attribute are expanded if the attribute does not already know about a given value (and if
     * the attribute is of type AttributeExpandableCategorical).
     *
     * @param f The file to read from
     * @param attribute The attribute whose prior likelihood scores are being read
     * @return A vector of doubles representing the read in prior likelihood scores of observing each value in the attribute
     *
     * @see netkit.graph.AttributeExpandableCategorical
     */
    public static double[] readPrior(java.io.File f, AttributeCategorical attribute) {
      try
      {
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
        logger.fine("Reading prior from '"+f+"'");
        return readPrior(br, attribute);
      }
      catch(java.io.IOException ioe)
      {
          // General I/O
          throw new RuntimeException("Failed to open ValueMap file "+f.getName(),ioe);
      }
    }
      
      /**
     * Reads in a file using the file format mentioned in the header.  It returns the vector
     * of scores for the values in the passed-in AttriuteCategorical--the value read in the file
     * are matched with the index into the categorical attribute list of known values.  The values in the
     * attribute are expanded if the attribute does not already know about a given value (and if
     * the attribute is of type AttributeExpandableCategorical).
     *
     * @param f The file to read from
     * @param attribute The attribute whose prior likelihood scores are being read
     * @return A vector of doubles representing the read in prior likelihood scores of observing each value in the attribute
     *
     * @see netkit.graph.AttributeExpandableCategorical
     */
    public static double[] readPrior(BufferedReader br, AttributeCategorical attribute) {
        ArrayList<Double> prior = new ArrayList<Double>(attribute.size());
        for(int i=0;i<attribute.size();i++) prior.add(0.0);
        Pattern splitter = Pattern.compile(",");
        String s=null;
        try
        {
            for(s=br.readLine();s!=null;s=br.readLine())
            {
                // a comment line to be ignored
                if(s.startsWith("#"))
                    continue;

                // split along ',' --- this should only be 2 values: token-value and score
                // anything else is ignored.
                String[] v = splitter.split(s);
                String vName = v[0];

                // If this throws an exception, then that exception is caught below
                double score = Double.parseDouble(v[1]);

                // See if we can find the value in the list of known values for this attribute
                int vIdx = -1;
                try
                {
                    // This will throw an exception if the value is not found and if the attribute
                    // is not expandable.  We just log the exception here and throw our own exception
                    // below
                    vIdx = attribute.getValue(vName);
                }
                catch(Exception e)
                {
                    logger.log(Level.WARNING, "   failed to get '"+vName+": ",e);
                }
                if(vIdx == -1)
                {
                    // If the attribute is expandable, then add the value to the attribute
                    // otherwise throw an exception
                    if(attribute instanceof AttributeExpandableCategorical)
                    {
                        ((AttributeExpandableCategorical)attribute).addToken(vName);
                        vIdx = attribute.getValue(vName);
                        prior.add(0.0);
                    }
                    else
                    {
                        throw new RuntimeException("token "+vName+" was not found in categorical attribute and the categorical is not expandable!");
                    }
                }
                logger.finer("Entity["+vName+":"+vIdx+"] [number of classes: "+attribute.size()+"]");

                // Set the prior score
                prior.set(vIdx,score);
                logger.fine("  Added class '"+vName+"' with prior "+score);
            }
        }
        catch(java.io.IOException ioe)
        {
            // General I/O
            throw new RuntimeException("I/O error in ValueMap",ioe);
        }
       catch(NumberFormatException nfe)
        {
            // This is reached if the Double.parseDouble failed above
            throw new RuntimeException("Parsing error for input ["+s+"] when reading ValueMap",nfe);
        }

        // Convert the arraylist into a proper double array
        double[] result = new double[prior.size()];
        for(int i=0;i<prior.size();i++)
            result[i] = prior.get(i);
        return result;
    }
}
