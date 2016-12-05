/**
 * NetworkLearner.java
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

package netkit.classifiers;

import netkit.util.*;
import netkit.classifiers.relational.NetworkClassifier;
import netkit.classifiers.io.PrintEstimateWriter;
import netkit.inference.InferenceMethod;
import netkit.classifiers.active.PickLabelStrategy;
import netkit.classifiers.active.PickLabelStrategy.LabelNode;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.AttributeCategorical;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.*;

public class NetworkLearner {
    public final Logger logger = NetKitEnv.getLogger(this);

    private final NetworkClassifier nc;
    private final Classifier lc ;
    private final InferenceMethod ic;
    private final boolean applyCMN;

    private DataView view = null;
    private Graph g = null;
    private DataSplit split = null;
    private boolean[] inTraining = null;

    public NetworkLearner(Classifier lc, NetworkClassifier nc, InferenceMethod ic, boolean applyCMN) {
        this.nc = nc;
        this.lc = lc;
        this.ic = ic;
        this.applyCMN = applyCMN;
        if(nc == null || lc == null || ic == null)
          throw new IllegalArgumentException("Could not instantiate the NetworkLearner LC="+lc+" RC="+nc+" CI="+ic);
    }

    public NetworkClassifier getNetworkClassifier() {
        return nc;
    }

    public Classifier getLocalClassifier() {
        return lc;
    }

    public InferenceMethod getInferenceMethod() {
        return ic;
    }
    
    private String distributionAsString(String prefix, int[] dist, AttributeCategorical attribute) {
        StringBuilder sb = new StringBuilder(prefix);
        for(int i=0;i<dist.length;i++)
            sb.append(' ').append(attribute.getToken(i)).append(':').append(dist[i]);
        return sb.toString();
    }

    private void setup(DataSplit split, boolean learnWithTruth, int depth) {
        this.split = split;
        view = split.getView();
        g = view.getGraph();

        AttributeCategorical attribute = view.getAttribute();
        int clsIdx = view.getAttributeIndex();

        split.applyLabels(0);

        int [] dist = new int[attribute.size()];
        Arrays.fill(dist,0);
        for(Node node : split.getTrainSet())
            dist[(int)node.getValue(clsIdx)]++;
        logger.fine(distributionAsString("        Known Distribution:",dist,attribute));
        if(split.hasTruth())
        {
            Arrays.fill(dist,0);
            int unk=0;
            for(Node node : split.getTrainSet())
            {
                if(view.getTrueClassValue(node) == -1)
                    unk++;
                else
                    dist[view.getTrueClassValue(node)]++;
            }
            logger.fine(distributionAsString("        True Train Distribution["+unk+" unknown]:",dist,attribute));
            Arrays.fill(dist,0);
            unk=0;
            for(Node node : split.getTestSet())
            {
                if(view.getTrueClassValue(node) == -1)
                    unk++;
                else
                    dist[view.getTrueClassValue(node)]++;
            }
            logger.fine(distributionAsString("        True Test Distribution["+unk+" unknown]:",dist,attribute));
        }

        logger.fine("Inducing local model [" + lc.getName() + "]");
        lc.induceModel(g, split);
        logger.finer("Induced local model [" + lc.getName() + "]");
        logger.info("Induced local classifier:"+NetKitEnv.newline+lc.toString());
        if (learnWithTruth)
        {
            logger.fine("Labeling graph using truth");
            logger.finer("Inducing relational model [" + nc.getName() + "] using truth");
            view.resetTruth();
        }
        else
        {
            logger.finer("Labeling graph using known nodes with depth=" + depth);
            int numShown = split.applyLabels(depth);
            logger.finer("Labeled " + numShown + " nodes (" + split.getTrainSetSize() + " training nodes)");
            logger.finer("Inducing relational model [" + nc.getName() + "] with depth " + depth);
        }
        
        logger.fine("Inducing relational model [" + nc.getName() + "]");
        nc.induceModel(g, split);
        logger.finer("Induced relational model [" + nc.getName() + "]");
        logger.info("Induced relational classifier:"+NetKitEnv.newline+nc.toString());

        split.applyLabels(0);

        logger.fine("Set initial priors");
        ic.setInitialPrior(generateInitialPriors(split, lc));
        ic.setTruth(view.getTruth());
                
        inTraining = new boolean[g.numNodes(view.getNodeType())];
        Arrays.fill(inTraining, false);
        for(Node n : split.getTrainSet())
          inTraining[n.getIndex()] = true;
    }

    private Estimate generateInitialPriors(DataSplit split, Classifier localLearner) {
        logger.finer("Creating priors using local learner ["+localLearner.getClass().getName()+"]");
        Graph g = split.getView().getGraph();
        Estimate initialPrior = new Estimate(g, split.getView().getNodeType(), split.getView().getAttribute());
        for(Node node : split.getUnknownSet())
            localLearner.estimate(node,initialPrior);
        logger.finest("Local estimates:");
        logger.finest(initialPrior.toString());
        logger.finest("Verifying priors");

        // for verification
        if(!split.getView().graphHasMissingClassValues())
        {
            ConfusionMatrix cm = new ConfusionMatrix(initialPrior, split.getView().getTruth());
            logger.fine("Prior accuracy="+cm.getAccuracy());
            for(int i=0;i<view.getAttribute().size();i++)
            {
                ROC roc = new ROC(initialPrior,split.getView().getTruth(),i);
                logger.fine("initial-prior AUC-"+split.getView().getAttribute().getToken(i)+ ": "+roc.getAUC());
            }
        }
        return initialPrior;
    }
    
    public Graph getGraph() {
      return g;
    }

    public DataSplit getSplit() {
      return split;
    }
    
    public boolean inTrainingSet(Node n) {
      return (inTraining != null && n.getType().equals(view.getNodeType()) && inTraining[n.getIndex()]);
    }
    
    private Estimate getTestSetPredictions(Estimate predictions) {
      Estimate e = null;
      if(split.getTestSet() == split.getUnknownSet())
          e = predictions;
      else
      {
          e = new Estimate(view.getGraph(), view.getNodeType(), view.getAttribute());
          for(Node node : split.getTestSet())
              e.estimate(node,predictions.getEstimate(node));
      }
      return e;
    }
    
    /**
     * See fully parameterized method for details.  This method calls the more
     * fully qualified method, with the following parameters:
     * <blockquote>
     * <code>runActiveLearner(ps,split,seedSize,true)</code>
     * </blockquote>
     * It computes <code>seedSize</code> to be the number of classes possible, thereby
     * in effect setting the initial seeds to be one instance of each class randomly
     * chosen from the training set from the given DataSplit.
     * 
     * @param ps Strategy for picking labels
     * @param split Split from which to pick seeds + test set
     * @see #runActiveLearner(PickLabelStrategy, DataSplit, boolean, int, boolean)
     * @return Final estimates after running active learning
     */
    public Estimate runActiveLearner(PickLabelStrategy ps, DataSplit split) {
      return runActiveLearner(ps,split,1,split.getTestSetSize(),false,0); 
    }
    
    /**
     * Run active learning using the given parameters.
     * <p>
     * The methodology works as follows:
     * <ol>
     * <li>Initialize the strategy object with a reference to this object 
     * <li>Repeat until <code>labelSet</code> is as large as maximum size given or the strategy does not pick more nodes.
     *     <ol>
     *     <li>Learn a model using the <code>labelSet</code>.  Use complete truth (<code>learnWithTruth</code>) or truth up to <code>depth</code> from nodes in the <code>labelSet</code>
     *     <li>Use learned model to predict labels from all nodes that are not in the <code>labelSet</code>.
     *     <li>Use strategy to pick nodes that should be labeled next.  If <code>pickFromTrainingOnly</code> is true, then only pick from nodes that are in the DataSplit training set, otherwise allow the strategy to pick any node in the graph that are not part of the DataSplit test set.
     *     <li>Get true labels from the picked nodes and add these to <code>labelSet</code>.
     *     </ol>
     *     Return the final predictions for the test set only 
     * </ol>
     * 
     * @param ps Strategy for picking labels
     * @param split Split from which to pick seeds + test set
     * @param picksPerIteration How many instances should be picked each iteration
     * @param maxPicks How many instances should be picked total
     * @param learnWithTruth Use complete truth when learning classifiers
     * @param depth Get true labels to this depth when learning a relational classifier
     * @return Final estimates after running active learning
     */
    public Estimate runActiveLearner(PickLabelStrategy ps, DataSplit split, int picksPerIteration, int maxPicks, boolean learnWithTruth, int depth) {     
      NetKitEnv.logTime("starting initialize active learning");
      DataView view = split.getView();
      if(!split.hasTruth())
          throw new IllegalStateException("Cannot do active learning without truth!");
      logger.info("run active learning using strategy "+ps.getName()+" picksPerIteration="+picksPerIteration+" maxPicks="+maxPicks+" learnWithTruth="+learnWithTruth+" depth="+depth);

      Classification truth = view.getTruth();

      Level ncLevel = null;
      Level lcLevel = null;
      Level icLevel = null;
      if(!logger.isLoggable(Level.FINEST))
      {
        ncLevel = nc.getLogger().getLevel();
        nc.getLogger().setLevel(Level.OFF);

        lcLevel = lc.getLogger().getLevel();
        lc.getLogger().setLevel(Level.OFF);

        icLevel = ic.logger.getLevel();
        ic.logger.setLevel(Level.OFF);
      }

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw,true);
      pw.println("initial labels:");
      AttributeCategorical attrib = split.getView().getAttribute();
      for(Node n : split.getTrainSet())
      {
        String cls = truth.isUnknown(n) ? "NA" : attrib.getToken(truth.getClassValue(n)); 
        pw.println("   Node-"+n.getIndex()+": "+n.getName()+" "+cls);
      }
      logger.fine(sw.toString());
      pw.close();
      
      List<Node> labelSet = new ArrayList<Node>(view.size());
      List<Node> testSet = new ArrayList<Node>(split.getTestSetSize());
      for(Node n : split.getTrainSet())
        labelSet.add(n);
      
      for(Node n : split.getTestSet())
        testSet.add(n);
      
      boolean newLabels = false;
      Estimate predictions = null;
      int it = 0;
      logger.fine("Active Learning Starting");
      ps.initialize(this, split);
      maxPicks = Math.min(maxPicks, testSet.size());
      int numPicks = Math.min(picksPerIteration, maxPicks);
      
      NetKitEnv.logTime("finished initialize active learning");

      do
      { 
        it++;
        NetKitEnv.resetTime();
        NetKitEnv.logTime("starting active learning iteration "+it);
        DataSplit alSplit = new DataSplit(view, testSet.toArray(new Node[0]), labelSet.toArray(new Node[0]));

        logger.info("Active Learning Iteration-"+it+": START (strategy="+ps.getShortName()+", labelSet="+labelSet.size()+", trainSet="+alSplit.getTrainSetSize()+", testSet="+alSplit.getTestSetSize()+", unknownSet="+alSplit.getUnknownSetSize()+", numbernodes="+view.size()+")");
        
        predictions = getEstimate(alSplit, false, learnWithTruth, depth);
        logger.fine("Active Learning Iteration-"+it+": got predictions for "+predictions.size()+" nodes.");
        
        Estimate testSetPred = getTestSetPredictions(predictions);
        ConfusionMatrix cm = new ConfusionMatrix(testSetPred,truth);

        sw = new StringWriter();
        pw = new PrintWriter(sw,true);
        pw.println("Active Learning ConfusionMatrix-" + it + ":");
        cm.printMatrix(pw);
        logger.finer(sw.toString());
        pw.close();

        double acc = cm.getAccuracy();
        logger.info("Active Learning Accuracy-" + it + ": " + acc);
        
        for(int i=0;i<view.getAttribute().size();i++)
        {
          ROC roc = new ROC(testSetPred,truth,i);
          logger.fine("Active Learning AUC-"+view.getAttribute().getToken(i)+"-"+ it + ": "+roc.getAUC());
        }

        newLabels = false;
        LabelNode[] nodes = ps.getNodesToLabel(alSplit, predictions,numPicks);
        if(nodes != null && nodes.length > 0)
        {        
          logger.fine("   Active Learning Strategy ["+ps.getShortName()+"] picked "+nodes.length+" nodes to label");
          for(LabelNode ln : nodes)
          {
            Node n = ln.node;
            if(truth.isUnknown(n))
            {
              logger.warning("Active Learning Iteration-"+it+": Wanted to label node "+n.getName()+", but we have no truth for that label");
              continue;
            }

            logger.info("Active Learning Iteration-"+it+": Labeling node "+n.getName());

            newLabels = true;
            labelSet.add(n);
            testSet.remove(n);
            maxPicks--;
          }
          numPicks = Math.min(numPicks, maxPicks);
        }
      } while(numPicks > 0 && newLabels);

      NetKitEnv.logTime("finished active learning runs");

      int nl = labelSet.size() - split.getTrainSetSize();
      
      if(!logger.isLoggable(Level.FINEST))
      {
        nc.getLogger().setLevel(ncLevel);
        lc.getLogger().setLevel(lcLevel);
        ic.logger.setLevel(icLevel);
      }

      logger.info("Active Learning Ended after "+it+" iterations.  "+nl+" nodes were labeled.  Final labelSet="+labelSet.size());

      return getTestSetPredictions(predictions);
    }

    public Estimate runLeaveOneOut(DataSplit split) {
        return runLeaveOneOut(split, false, 0);
    }
    public Estimate runLeaveOneOut(DataSplit split, boolean learnWithTruth, int depth) {
        Node[] train = split.getTrainSet();
        if(train == null || train.length == 0)
            throw new IllegalStateException("Cannot run leave-one-out with no training examples specified!");
        Node[] looTrain = Arrays.copyOfRange(train,1,train.length);
        Node[] looTest = new Node[]{train[0]};
        
        Estimate predictions = new Estimate(view.getTruth());
        
        logger.info("Doing leave-one-out estimations ("+view.getGraph().numNodes()+" instances)");
        for(int i=0;i<train.length;i++)
        {
            DataSplit looSplit = new DataSplit(view, looTest, looTrain);
            Estimate p = runInference(looSplit, false, learnWithTruth, depth);
            predictions.estimate(looTest[0], p.getEstimate(looTest[0]));
            looTrain[i] = looTest[0];
            looTest[0] = train[i+1];
        }
        return predictions;
    }

    public void saveIterationPredictions(String stem, PrintEstimateWriter pe, boolean append, Node[] eval, String header) {
      if(nc == null || lc == null || ic == null)
          throw new IllegalArgumentException("Could not instantiate the NetworkLearner LC="+lc+" RC="+nc+" CI="+ic);
      if(stem == null)
          stem = lc.getShortName()+nc.getShortName()+ic.getShortName();
      ic.savePredictions(stem,pe,true,eval,header);
    }

    public void saveIterationsInPajek(String pajekFile) {
      if(nc == null || lc == null || ic == null)
          throw new IllegalArgumentException("Could not instantiate the NetworkLearner LC="+lc+" RC="+nc+" CI="+ic);
      ic.savePredictionsInPajek(pajekFile);
    }

    public Estimate runInference(DataSplit split) {
        return runInference(split, false, false, 0);
    }
    public Estimate runInference(DataSplit split, boolean showItAcc, boolean learnWithTruth, int depth) {
        return getTestSetPredictions(getEstimate(split,showItAcc,learnWithTruth,depth));
    }
    
    private Estimate getEstimate(DataSplit split, boolean showItAcc, boolean learnWithTruth, int depth) {
      if (showItAcc)
        ic.setShowIterationAccuracies(true);
      setup(split, learnWithTruth, depth);
    
      logger.info("Run inferenceMethod [" + ic.getClass().getName() + "] on " + split.getUnknownSetSize() + " nodes");
      Estimate predictions = ic.estimate(nc, new ArrayIterator<Node>(split.getUnknownSet()));
      if(applyCMN)
        predictions.applyCMN(split);
      return predictions;
    }
}
