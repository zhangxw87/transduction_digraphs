/**
 * PrintEstimateWriter.java
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
import netkit.graph.AttributeCategorical;
import netkit.util.NetKitEnv;
import netkit.classifiers.Estimate;
import netkit.classifiers.Classification;

import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * This class prints label estimates in a user-defined output format.  The output format
 * is like a printf statement in c.  This format string includes constant information as
 * well as variable information such as value scores and attribute token strings.
 * <P>
 * The format string is broken up into a list of print segments, where each print
 * segment is one of:
 * <UL>
 * <LI><b>Constant</b>: a constant string.
 * <LI><b>Node ID</b> [%ID]: the node ID (Key) of the node whose estimate is being written.
 * <LI><b>Class</b> [%CLASS or %LABEL]: the true label of the node whose estimate is being written.  'UNKNOWN' is the true label is not known
 * <LI><b>Score</b> [%token-name]: the estimated score/prediction of a specific token-name (such as 'red' if the class attribute can take on the value 'red').
 * <LI><b>Estimate</b> [%ESTIMATE!token-name]: a combination of a Class and Score as 'token-name:score'
 * <LI><b>Prediction</b> [%PREDICTION]: a combination of a predictionclass and predictionscore as 'token-name:score'
 * <LI><b>PredictionClass</b> [%PREDICTLABEL]: the predicted class for a node.
 * <LI><b>PredictionScore</b> [%PREDICTSCORE]: the score of the predicted class for a node.
 * </UL>
 * For example, a valid format string could be:
 * <BLOCKQUOTE>
 * Node %ID is predicted to be %PREDICTLABEL with a score of %PREDICTSCORE.  Full estimates are: %ESTIMATE!red %ESTIMATE!blue %ESTIMATE!green
 * </BLOCKQUOTE>
 * If the label estimates for node 'Ball903' are 0.2 for red, 0.7 for green and 0.1 for blue, then the resulting output string would be (variable segments boldfaced for clarity):
 * <BLOCKQUOTE>
 * Node <B>Ball903</B> is predicted to be <B>green</B> with a score of <B>0.7</B>.  Full estimates are: <B>red:0.2 blue:0.1 green:0.7</B>
 * </BLOCKQUOTE>
 * The default output format (if none are provided) is equivalent to:
 * <BLOCKQUOTE>
 * %ID %ESTIMATE!class1 ... %ESTIMATE!classK
 * </BLOCKQUOTE>
 * Where class1 through classK are the possible labels for the class attribute, in the order that they were specified
 * in the schema file or the order in which they were observed in the data
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public class PrintEstimateWriter
{
    private final Logger logger = NetKitEnv.getLogger(this);

    // The name of the label and index into the score array of the predicted label for the node
    // whose information is currently being printed
    private String predNm = null;
    private int    predIdx = -1;

    // The source format string, the output printwriter and the resulting list of printsegments
    private String format = null;
    private PrintWriter pw;
    private List<PrintSegment> segments = new ArrayList<PrintSegment>();

    public PrintEstimateWriter(PrintWriter pw, String format) {
        setOutput(pw);
        setOutputFormat(format);
    }
    public PrintEstimateWriter(PrintWriter pw) {
        this(pw,null);
    }
    public PrintEstimateWriter(PrintStream ps, String format) {
        this(new PrintWriter(ps,true),format);
    }
    public PrintEstimateWriter(PrintStream ps) {
        this(new PrintWriter(ps,true));
    }

    // defines the PrintSegment interface, which is just one print method that all
    // PrintSegments must implement
    private interface PrintSegment {
        public void print(PrintWriter pw, Node node, Estimate e, Classification known);
    }
    private class PrintConstant implements PrintSegment {
        private String c;
        public PrintConstant(String s) {
            this.c = s.intern();
        }
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            logger.finer("PrintConstant() - print("+c+")");
            pw.print(c);
        }
    }
    private class PrintNodeID implements PrintSegment {
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            logger.finer("PrintNodeID() - print("+node.getName()+")");
            pw.print(node.getName());
        }
    }
    private class PrintClass implements PrintSegment {
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            int lbl = ( (known == null) ? -1 : known.getClassValue(node));
            String cls = ((lbl==-1) ? "UNKNOWN" : e.getAttribute().getToken(lbl));
            pw.print(cls);
            logger.finer("PrintClass() - print("+cls+")");
        }
    }
    private class PrintScore implements PrintSegment {
        private String vName;
        public PrintScore(String name) {
            this.vName = name.intern();
        }
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            double[] p = e.getEstimate(node);
            double s = -1;
            int vIdx = -1;
            if(p!=null)
            {
                vIdx = e.getAttribute().getValue(vName);
                s = ( (vIdx<0||vIdx>=p.length) ? -1 : p[vIdx]);
            }
            pw.print(s);
            logger.finer("PrintScore() - print["+vName+","+vIdx+"]("+s+")");
        }
    }
    private class PrintEstimate implements PrintSegment {
        private String vName;
        public PrintEstimate(String name) {
            this.vName = name.intern();
        }
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            if(vName.equals(e.getAttribute().getToken(predIdx)))
                return;

            double[] p = e.getEstimate(node);
            double s = -1;
            int vIdx = -1;
            if(p!=null)
            {
                try
                {
                    vIdx = e.getAttribute().getValue(vName);
                }
                catch(NullPointerException npe)
                {
                    logger.severe("Class name "+vName+" is not known");
                }
                s = ( (vIdx<0||vIdx>=p.length) ? -1 : p[vIdx]);
            }
            pw.print(vName);
            pw.print(":");
            pw.print(s);
        }
    }
    private class PrintPrediction implements PrintSegment {
        PrintPredictionClass pc = new PrintPredictionClass();
        PrintPredictionScore ps = new PrintPredictionScore();
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            pc.print(pw,node,e,known);
            pw.print(":");
            ps.print(pw,node,e,known);
            logger.finer("PrintPrediction() - print()");
        }
    }
    private class PrintPredictionClass implements PrintSegment {
        public void print(PrintWriter pw, Node node, Estimate e, Classification known) {
            pw.print(predNm);
            logger.finer("PrintPredictionClass() - print["+predNm+","+predIdx+"]");
        }
    }
    private class PrintPredictionScore implements PrintSegment {
        public void print(PrintWriter pw, Node node, Estimate e, Classification known)
        {
            double[] p = e.getEstimate(node);
            double s = ( (predIdx<0) ? -1 : p[predIdx]);
            pw.print(s);
            logger.finer("PrintPredictionScore() - print["+e.getAttribute().getToken(predIdx)+","+predIdx+"]("+s+")");
        }
    }

    /**
     * Reset the output to the given outputstream.
     * @param os New place to send output
     */
    public void setOutput(OutputStream os) {
        setOutput(new PrintWriter(os));
    }
    /**
     * Reset the output to the given printwriter.
     * @param pw New place to send output
     */
    public void setOutput(PrintWriter pw) {
        this.pw = pw;
    }

    /**
     * Print an estimate line of the given node using the given output format and the given current estimates.
     *
     * @param node The node whose estimate to print
     * @param e The current estimates
     * @see netkit.classifiers.io.PrintEstimateWriter#print(netkit.graph.Node, netkit.classifiers.Estimate, netkit.classifiers.Classification)
     */
    public void println(Node node, Estimate e) {
        println(node,e,null);
    }
    /**
     * Print an estimate line of the given node using the given output format and the given current estimates and true labels.
     *
     * @param node The node whose estimate to print
     * @param e The current estimates
     * @param known The true labels
     * @see netkit.classifiers.io.PrintEstimateWriter#print(netkit.graph.Node, netkit.classifiers.Estimate, netkit.classifiers.Classification)
     */
    public void println(Node node, Estimate e, Classification known) {
        print(node,e,known);
        pw.println();
    }
    /**
     * Print an estimate  of the given node using the given output format and the given current estimates.
     *
     * @param node The node whose estimate to print
     * @param e The current estimates
     * @see netkit.classifiers.io.PrintEstimateWriter#print(netkit.graph.Node, netkit.classifiers.Estimate, netkit.classifiers.Classification)
     */
    public void print(Node node, Estimate e) {
        print(node,e,null);
    }
    /**
     * Print an estimate of the given node using the given output format and the given current estimates and true labels.
     * This, in effect, loops through all the inferred PrintSegments and calls each of them to output the
     * node estimate in the user-defined output format.
     * <P>
     * If not output format has been specified, then output in the following format:
     * <BLOCKQUOTE>
     * %ID %ESTIMATE!class1 ... %ESTIMATE!classK
     * </BLOCKQUOTE>
     * Where class1 through classK are the possible labels for the class attribute, in the order that they were specified
     * in the schema file or the order in which they were observed in the data
     *
     * @param node The node whose estimate to print
     * @param e The current estimates
     * @param known The true labels
     */
    public void print(Node node, Estimate e, Classification known) {
        predIdx = e.getClassification(node);
        predNm  = e.getAttribute().getToken(predIdx);
        if(segments.size()>0)
        {
            for(PrintSegment segment: segments)
                segment.print(pw,node,e,known);
        }
        else
        {
            pw.print(node.getName());
            double[] p = e.getEstimate(node);
            AttributeCategorical attr = e.getAttribute();
            if(p!=null)
            {
                for(int c=0;c<p.length;c++)
                {
                    pw.print(" ");
                    pw.print(attr.getToken(c));
                    pw.print(":");
                    pw.print(p[c]);
                }
            }
            else
            {
                for(String s : attr.getTokens())
                {
                    pw.print(" ");
                    pw.print(s);
                    pw.print(":-1");
                }
            }
        }
    }

    /**
     * The equivalent of a print, where the output has been set to a string to be returned.
     * This temporarily resets the output to a StringWriter and prints to there, then resets
     * the output back to the original stream.
     *
     * @param node The node whose estimate needs to be written
     * @param e The current estimates
     * @param known The known true labels
     * @return The output of the print in a string format.
     * @see netkit.classifiers.io.PrintEstimateWriter#print(netkit.graph.Node, netkit.classifiers.Estimate, netkit.classifiers.Classification)
     */
    public String toString(Node node, Estimate e, Classification known) {
        PrintWriter ow = this.pw;
        StringWriter sw = new StringWriter(80);
        setOutput(new PrintWriter(sw));
        print(node,e,known);
        sw.flush();
        setOutput(ow);
        return sw.toString();
    }
    /**
     * The equivalent of a print, where the output has been set to a string to be returned.
     * This temporarily resets the output to a StringWriter and prints to there, then resets
     * the output back to the original stream.
     *
     * @param node The node whose estimate needs to be written
     * @param e The current estimates
     * @return The output of the print in a string format.
     * @see netkit.classifiers.io.PrintEstimateWriter#toString(netkit.graph.Node, netkit.classifiers.Estimate, netkit.classifiers.Classification)
     */
    public String toString(Node node, Estimate e) {
        return toString(node,e,null);
    }

    /**
     * Adds a PrintSegment to the output based on the current offset into
     * the output format string.  It is assumed that the very next 'token'
     * in the string will resolve into one of the known segment types above.
     *
     * @param format The source output format string
     * @param offset The current offset
     * @return The new offset in the format string after handling the current segment
     */
    private int addSegment(String format,int offset) {
        int i=0;

        // Find the end of the current 'token'.  It looks for the next nonletter and nondigit
        for(i=offset;i<format.length();i++)
        {
            if(!Character.isLetterOrDigit(format.charAt(i)) ||
                    format.charAt(i) == '%')
                break;
        }
        logger.finer("getSegment("+format+") - extract-variable substring("+offset+","+i+")='"+format.substring(offset,i)+"'");

        // Extract that token into a separate string
        String s = format.substring(offset,i);
        PrintSegment ps = null;

        // Identify the type of print segment we are dealing with
        if(s.equalsIgnoreCase("ID"))
        {
            ps = new PrintNodeID();
            logger.finer("   Adding PrintNodeID()");
        }
        else if(s.equalsIgnoreCase("class") ||
                s.equalsIgnoreCase("label"))
        {
            ps = new PrintClass();
            logger.finer("   Adding PrintClass()");
        }
        else if(s.equalsIgnoreCase("predictlabel"))
        {
            ps = new PrintPredictionClass();
            logger.finer("   Adding PrintPredictionClass()");
        }
        else if(s.equalsIgnoreCase("predictscore"))
        {
            ps = new PrintPredictionScore();
            logger.finer("   Adding PrintPredictionScore()");
        }
        else if(s.equalsIgnoreCase("prediction"))
        {
            ps = new PrintPrediction();
            logger.finer("   Adding PrintPrediction()");
        }
        else if(s.equalsIgnoreCase("estimate"))
        {
            // The format of the 'ESTIMATE' segment requires an additional '!<token>'
            if(format.charAt(i)!='!')
            {
                logger.warning("invalid segment["+format+"] - Should start '%ESTIMATE!...'");
            }
            else
            {
                offset = i+1;
                for(i=offset;i<format.length();i++)
                {
                    if(!Character.isLetterOrDigit(format.charAt(i)) || format.charAt(i) == '%')
                        break;
                }
                String cls = format.substring(offset,i);
                if(cls.length()>0)
                {
                    ps = new PrintEstimate(cls);
                    logger.finer("   Adding PrintEstimate("+cls+")");
                }
            }
        }
        // The default print segment is a 'Score'
        else
        {
            ps = new PrintScore(s);
            logger.finer("   Adding PrintScore("+s+")");
        }

        segments.add(ps);
        return i;
    }
    /**
     * Reset the output format to the given format string.
     * @param f The new output format string
     */
    public void setOutputFormat(String f) {
        segments.clear();
        if(f==null)
        {
            format = null;
            return;
        }
        format = f.intern();
        int prev=0;
        int prevS=0;
        for(int next=format.indexOf('%',prev);next != -1;next=format.indexOf('%',prev))
        {
            prev = next+1;
            logger.finer("setOutputformat("+format+") - segment("+prev+"("+prevS+"),"+next+")");
            if((next+1)<format.length() && format.charAt(next+1)=='%')
                continue;
            if(prevS<next)
            {
                segments.add(new PrintConstant(format.substring(prevS,next)));
                logger.finer("   Adding PrintConstant("+format.substring(prevS,next)+")");
            }
            prevS = addSegment(format,next+1);
        }
        if(prevS < format.length())
        {
            segments.add(new PrintConstant(format.substring(prevS)));
            logger.finer("   Adding PrintNodeID()");
        }
    }
    public String getOutputFormat() {
        return format;
    }
}
