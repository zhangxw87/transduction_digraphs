/**
 * InferenceMethod.java
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

/**
 * $Id: InferenceMethod.java,v 1.7 2007/03/26 23:45:06 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:00:41 AM
 */
package netkit.inference;

import netkit.graph.Attributes;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.io.PajekGraph;
import netkit.classifiers.Classification;
import netkit.classifiers.Estimate;
import netkit.classifiers.io.PrintEstimateWriter;
import netkit.classifiers.relational.NetworkClassifier;
import netkit.util.Configurable;
import netkit.util.Configuration;
import netkit.util.NetKitEnv;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public abstract class InferenceMethod implements Configurable
{
  public final Logger logger = NetKitEnv.getLogger(this);

  protected Node[] unknown;
  protected double[] tmpPredict;
  protected Estimate initialPrior;
  protected Estimate currPrior;
  protected double[][] idMatrix = null;
  protected int numIterations=0;
  private String outPredict = null;
  private Node[] eval = null;
  private PrintEstimateWriter pe = null;
  private String pajekFile = null;
  private PrintWriter pajekPW = null;
  private Classification truth = null;
  private boolean append = false;
  private boolean showItAcc = false;
  private String header = null;

  private transient Set<InferenceMethodListener> listeners = new HashSet<InferenceMethodListener>();
  private boolean notify=true;

  public abstract String getShortName();
  public abstract String getName();
  public abstract String getDescription();
  protected abstract boolean iterate(NetworkClassifier networkClassifier);

  public Configuration getDefaultConfiguration() {
    Configuration dCfg = new Configuration();
    dCfg.set("numit",100);
    return dCfg;
  }
  public void configure(Configuration config) {
    try
    {
      numIterations = config.getInt("numit",100);
    }
    catch(NumberFormatException nfe)
    {
      throw new RuntimeException("Failed to initialize "+this.getClass().getName(),nfe);
    }
    logger.config("   "+this.getClass().getName()+" configure: numit="+numIterations);
  }
  public void reset(Iterator<Node> unknowns) {
    if(initialPrior == null)
      throw new IllegalStateException("No initial priors defined!");

    tmpPredict = new double[initialPrior.getAttribute().size()];

    // generate NxN ID matrix
    idMatrix = new double[tmpPredict.length][];
    for(int i=0;i<idMatrix.length;i++)
    {
      idMatrix[i] = new double[tmpPredict.length];
      Arrays.fill(idMatrix[i],0);
      idMatrix[i][i] = 1.0;
    }

    // generate the array of unknown nodes to estimate
    ArrayList<Node> al = new ArrayList<Node>();
    while(unknowns.hasNext())
      al.add(unknowns.next());
    unknown = al.toArray(new Node[0]);

    // generate the current prior using the given initial priors
    currPrior = new Estimate(initialPrior.getGraph(),initialPrior.getNodeType(),initialPrior.getAttribute());
    for (Node n : unknown)
      currPrior.estimate(n,initialPrior.getEstimate(n));
  }

  public void setShowIterationAccuracies(boolean showItAcc) {
    this.showItAcc = showItAcc;
  }

  /**
   * What is the accuracy on the training data, if we do a leave-one-out
   * estimation, keeping current predictions for the test set.  This is
   * a pseudo-estimation of how well we are doing in the current iteration.
   * 
   * @return
   */
  public double getCurrentTrainingLOOAccuracy(NetworkClassifier nc) {
    if(unknown == null || truth == null) return 0;

    Graph g = truth.getGraph();
    String nodeType = truth.getNodeType();
    Attributes as = g.getAttributes(nodeType);
    int index = as.getAttributeIndex(truth.getAttribute().getName());
    double numC = 0;
    double numN =0;
    for(Node n : truth)
    {
      if(n.isMissing(index))
        continue;
      numN++;
      if(truth.getClassValue(n) == nc.classify(n, currPrior, false))
        numC++;

    }
    return numC/numN;
  }

  public double getCurrentAccuracy() {
    if(unknown == null || truth == null) return 0;

    Estimate e = getCurrentEstimate();
    double numC = 0;
    for (Node n : unknown)
      if(truth.getClassValue(n) == e.getClassification(n))
        numC++;
    return numC/(double)unknown.length;
  }
  public Estimate getCurrentEstimate() {
    return currPrior;
  }

  public final void setTruth(Classification truth) {
    this.truth = truth;
  }
  public final void savePredictions(String outPredict,
      PrintEstimateWriter pe,
      boolean append,
      Node[] eval,
      String header) {
    this.outPredict = outPredict;
    this.pe         = pe;
    this.append     = append;
    this.eval       = ((eval==null)? unknown : eval);
    this.header     = header;
  }

  public final void savePredictionsInPajek(String pajekFile) {
    this.pajekFile = pajekFile;
    logger.info("["+getClass().getName()+"] Will save inferences to Pajek time network using prefix "+pajekFile);
  }

  private void printStatistics(int numIteration, NetworkClassifier nc) {
    if(showItAcc) if(numIterations<250 || ((numIteration+1)%10) == 0) logger.info(new Date()+" ["+getClass().getName()+"] iteration-"+numIteration+" accuracy="+getCurrentAccuracy()+" trainingLOO="+getCurrentTrainingLOOAccuracy(nc));
    if(outPredict != null)
    {
      logger.info(new Date()+" ["+getClass().getName()+"] iteration-"+numIteration+" accuracy="+getCurrentAccuracy());
      try
      {
        PrintWriter pw = new PrintWriter(new FileWriter(outPredict+"."+(numIteration+1)+".predict",append),true);
        pw.println(header);
        Classification outTruth;
        if (truth != null)
          outTruth = truth;
        else
        {
          outTruth = new Classification(currPrior.getGraph(), currPrior.getNodeType(), currPrior.getAttribute());
          outTruth.clear();
          for(Node node : eval)
            outTruth.set(node, truth.getClassValue(node));
        }
        pe.setOutput(pw);
        for (Node node : eval)
        {
          pe.println(node, getCurrentEstimate(), outTruth);
        }
        pw.close();
      }
      catch(IOException ioe)
      {
        logger.log(Level.WARNING,"Error writing to '"+outPredict+"."+(numIteration+1)+".predict'",ioe);
      }
    }
    if(pajekFile != null && pajekPW == null)
      pajekPW = NetKitEnv.getPrintWriter(pajekFile);
    
    if(pajekPW != null) {
      if(numIteration == -1) {
        PajekGraph.saveGraph(currPrior.getGraph(), pajekPW, truth, currPrior, null);
      } else {
        PajekGraph.saveGraph(currPrior.getGraph(), pajekPW, truth, currPrior, null);
      }
    }
  }
  public final Estimate estimate(NetworkClassifier networkClassifier, Iterator<Node> unknowns) {
    reset(unknowns);
    logger.fine("["+getClass().getName()+"] initial accuracy="+getCurrentAccuracy());
    printStatistics(-1,networkClassifier);
    for(int i=0;i<numIterations;i++)
    {
      networkClassifier.initializeRun(currPrior,unknown);
      boolean predictOK = iterate(networkClassifier);
      printStatistics(i,networkClassifier);
      if(!predictOK)
      {
        logger.info(getName()+" converged after "+i+" iterations (max="+numIterations+")");
        i=numIterations;
      }
    }
    if(pajekPW != null) {
      pajekPW.println();
      pajekPW.close();
      pajekPW = null;
    }
    return getCurrentEstimate();
  }
  public final void estimate(NetworkClassifier networkClassifier, Iterator<Node> unknowns, Estimate result) {
    Estimate e = estimate(networkClassifier,unknowns);
    for(Node node : result)
      result.estimate(node,e.getEstimate(node));
  }

  public final Classification classify(NetworkClassifier networkClassifier, Iterator<Node> unknowns) {
    return new Classification(estimate(networkClassifier,unknowns));
  }
  public final void classify(NetworkClassifier networkClassifier, Iterator<Node> unknowns, Classification result) {
    Estimate e = estimate(networkClassifier,unknowns);
    for (Node node : result)
      result.set(node,e.getClassification(node));
  }

  public final void setInitialPrior(Estimate prior) {
    this.initialPrior = prior;
  }
  public final Estimate getInitialPrior() {
    return initialPrior;
  }

  public final void setNumIterations(int numIterations) {
    this.numIterations = numIterations;
  }
  public final int getNumIterations() {
    return numIterations;
  }

  public final void addListener(InferenceMethodListener cl) {
    listeners.add(cl);
  }
  public final void removeListener(InferenceMethodListener cl) {
    listeners.remove(cl);
  }
  public final void clearListeners() {
    listeners.clear();
  }
  public final boolean getNofifyListeners() {
    return notify;
  }
  public final void setNofityListeners(boolean notify) {
    this.notify = notify;
  }

  public final void notifyListeners(Estimate e, int[] unknown) {
    if(!notify)
      return;
    for (InferenceMethodListener iml : listeners)
      iml.estimate(e,unknown);
  }
  public final void notifyListeners(Classification c, int[] unknown) {
    if(!notify)
      return;
    for (InferenceMethodListener iml : listeners)
      iml.classify(c,unknown);
  }
  public final void notifyListeners(Graph g, int[] unknown) {
    if(!notify)
      return;
    for (InferenceMethodListener iml : listeners)
      iml.iterate(g,unknown);
  }
}
