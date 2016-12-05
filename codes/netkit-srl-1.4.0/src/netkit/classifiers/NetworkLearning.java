/**
 * NetworkLearning.java
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

import netkit.Netkit;
import netkit.util.*;
import netkit.classifiers.io.ReadClassification;
import netkit.classifiers.io.PrintEstimateWriter;
import netkit.classifiers.io.ReadClassificationGeneric;
import netkit.classifiers.io.ReadPrior;
import netkit.classifiers.aggregators.SharedNodeInfo;
import netkit.classifiers.relational.NetworkClassifier;
import netkit.classifiers.active.PickLabelStrategy;
import netkit.graph.*;
import netkit.graph.edgecreator.*;
import netkit.graph.io.NetkitGraph;
import netkit.graph.io.PajekGraph;
import netkit.graph.io.SchemaReader;
import netkit.inference.InferenceMethod;

import java.util.*;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.StringWriter;

public class NetworkLearning implements Configurable {
  private static final boolean DEBUG = false;
  private Logger logger = NetKitEnv.getLogger(this);

  private static final String ParamDB = "__dbfile";
  private static final String ParamEdge = "__edgefile";
  private static final String ParamNT = "__nodetype";
  private static final String ParamAT = "__attribute";

  private PrintEstimateWriter pe = null;
  private PrintWriter outPredict = null;
  private Configuration params = null;
  private Configuration conf = null;

  private Graph graph = null;
  private String nodeType = null;
  private double[] classPrior = null;
  private String outFormat = null;
  private NetworkLearner learner = null;
  private DataSplit[] splits = null;
  private EdgeCreator[] ecs = null;
  private Classification test = null;
  private Classification truth = null;
  private Classification known = null;
  private DataView dataView = null;
  private AttributeCategorical attribute = null;
  private long rndSeed = -1;

  public static final String RC_PREFIX = "rclassifier";
  public static final String LC_PREFIX = "lclassifier";
  public static final String IM_PREFIX = "inferencemethod";
  public static final String AL_PREFIX = "activelearning";
  public static final String EC_PREFIX = "edgecreator";

  public static final Factory<NetworkClassifier> rclassifiers = new Factory<NetworkClassifier>(RC_PREFIX);
  public static final Factory<Classifier> lclassifiers = new Factory<Classifier>(LC_PREFIX);
  public static final Factory<InferenceMethod> imethods = new Factory<InferenceMethod>(IM_PREFIX);
  public static final Factory<PickLabelStrategy> alstrategies = new Factory<PickLabelStrategy>(AL_PREFIX);
  public static final Factory<EdgeCreator> edgecreators = new Factory<EdgeCreator>(EC_PREFIX);

  private Configuration getDefaultParameters() {
    Configuration defParam = new Configuration();
    defParam.set(RC_PREFIX, "wvrn");
    defParam.set(LC_PREFIX, "classprior");
    defParam.set(IM_PREFIX, "relaxlabel");
    return defParam;
  }

  public Configuration getDefaultConfiguration() {
    Configuration defConf = new Configuration();
    defConf.set("wvrn.class", "netkit.classifiers.relational.WeightedVoteRelationalNeighbor");
    defConf.set("classprior.class", "netkit.classifiers.nonrelational.ClassPrior");
    defConf.set("relaxlabel.class", "netkit.inference.RelaxationLabeling");
    defConf.set("relaxlabel.numit", "99");
    defConf.set("relaxlabel.beta", "1.00");
    defConf.set("relaxlabel.decay", "0.99");
    return defConf;
  }
  
  public void configure(Configuration conf) {
    this.conf = new Configuration();
    this.conf.setParent(conf);
  }

  public NetworkLearning(String[] args) {
    configure(getDefaultConfiguration());
    params = new Configuration(Configuration.getConfiguration(NetKitEnv.getBundle("NetKit"), "netkit"));
    params.setParent(getDefaultParameters());
    setOptions(args);
  }

  private void verifyDataSamples(DataSplit[] splits) {
    int[] verifySample = new int[splits[0].getView().getAttribute().size()];
    Graph g = splits[0].getView().getGraph();
    for (int ki = 0; ki < splits.length; ki++) {
      Arrays.fill(verifySample, 0);
      int numS = 0;
      int clsIdx = splits[ki].getView().getAttributeIndex();
      for (Node node : splits[ki].getTrainSet()) {
        verifySample[(int) node.getValue(clsIdx)]++;
        numS++;
      }
      StringBuilder sb = new StringBuilder("   Verify sampling:");
      sb.append(NetKitEnv.newline);
      for (int i : verifySample)
        sb.append("      class-" + i + ": " + i + " samples").append(NetKitEnv.newline);
      sb.append("   Sampled a total of " + numS + " samples (out of " + g.numNodes() + ") test-size=" + (g.numNodes() - numS));
      sb.append(NetKitEnv.newline);
      sb.append("     Verify: known has " + numS + " known nodes");
      sb.append(NetKitEnv.newline);
      logger.info(sb.toString());
    }
  }

  public void setNodeType(String nt) {
    nodeType = nt;
  }

  public String getNodeType() {
    if (nodeType == null) {
      Graph g = getGraph();
      if (g == null) {
        logger.severe("no graph specified");
        return null;
      }
      nodeType = params.get(ParamNT, g.getNodeTypes()[0]);
    }
    return nodeType;
  }

  public void setRandomSeed(long seed) {
    rndSeed = seed;
  }

  public long getRandomSeed() {
    if (rndSeed == -1)
      rndSeed = params.getLong("seed", System.currentTimeMillis());
    return rndSeed;
  }

  public void setDataView(DataView view) {
    dataView = view;
  }

  public DataView getDataView() {
    if (dataView != null)
      return dataView;

    Graph graph = getGraph();
    if (graph == null) {
      logger.severe("no graph specified!");
      return null;
    }

    String nodeType = getNodeType();
    if (nodeType == null) {
      logger.severe("no nodetype specified!");
      return null;
    }

    AttributeCategorical attribute = getAttribute();
    if (attribute == null) {
      logger.severe("nodetype '" + nodeType
          + "' does not exist in given schema.");
      return null;
    }

    boolean pruneZeroKnowledge = params.getBoolean("prunezeroknowledge");
    boolean pruneSingletons = params.getBoolean("prunesingletons");
    boolean sampleWithReplacement = params.getBoolean("replacement", false);
    boolean stratified = params.getBoolean("stratified", false);
    boolean sampleUnknown = params.getBoolean("sampleunknown", false);
    
    if(params.containsKey("sample") && params.getDouble("sample") <= 0.0)
    {
      stratified = true;
      params.set("stratified", true);
      params.set("sample", (double)attribute.size());
      logger.info("active learning is on and sample<=0.  Turning on stratified sampling and setting sample="+(double)attribute.size());
    }

    dataView = new DataView(graph, nodeType, attribute, getRandomSeed(),
        sampleWithReplacement, stratified, pruneZeroKnowledge, pruneSingletons, sampleUnknown);
    return dataView;
  }

  public void setAttribute(AttributeCategorical a) {
    attribute = a;
  }

  public AttributeCategorical getAttribute() {
    if (attribute != null)
      return attribute;

    Graph g = getGraph();
    if (g == null) {
      logger.severe("no graph specified!");
      return null;
    }

    String nodeType = getNodeType();
    if (nodeType == null) {
      logger.severe("no nodetype specified!");
      return null;
    }

    Attributes attribs = g.getAttributes(nodeType);
    if (attribs == null) {
      logger.severe("nodetype '" + nodeType
          + "' does not exist in given schema.");
      return null;
    }

    Attribute a;
    if (!params.containsKey(ParamAT)) {
      a = attribs.getAttribute(attribs.attributeCount() - 1);
      logger.info("attribute not set... use last attribute in '" + nodeType + "'");
    } else {
      a = attribs.getAttribute(params.get(ParamAT));
      if (a == null)
        logger.severe("attribute '" + params.get(ParamAT) + "' does not exist in " + nodeType + " table.");
    }
    if (a.getType() != Type.CATEGORICAL) {
      logger.severe("ERROR: attribute '" + a.getName() + "' in table " + nodeType + " is not categorical.");
      return null;
    }

    attribute = (AttributeCategorical) a;
    if (DEBUG) {
      int clsIdx = g.getAttributes(nodeType).getAttributeIndex(
          attribute.getName());
      System.out.println("Classification Attribute: " + attribute.getName() + " [" + attribute.toString() + "] index=" + clsIdx);
      System.out.println("   Classification nodes:");
      for (Node n : g.getNodes(nodeType))
        System.out.println("   " + n.toString() + " cls=" + n.getValue(clsIdx));
    }

    return attribute;
  }

  private void setOutput() {
    outPredict = null;
    if (params.containsKey("output")) {
      try {
        outPredict = new PrintWriter(new FileWriter(params.get("output") + ".predict"), true);
      } catch (IOException ioe) {
        logger.severe("Error writing to file '" + params.get("output") + "': " + ioe.getMessage());
      }
    }
    if (outPredict == null)
      outPredict = new PrintWriter(System.out, true);
  }

  public double[] getClassPrior() {
    if (classPrior == null && params.containsKey("vprior"))
      classPrior = ReadPrior.readPrior(new File(params.get("vprior")), getAttribute());
    return classPrior;
  }

  public void setPrior(double[] prior) {
    classPrior = prior;
  }

  public String getOutputFormat() {
    if (outFormat == null) {
      DataView view = getDataView();
      if (view == null) {
        logger.severe("no data view specified");
        return null;
      }

      setOutputFormat(view);
    }
    return outFormat;
  }

  public void setOuputFormat(String f) {
    outFormat = f;
    logger.info("Using output format='" + outFormat + "'");
    pe = new PrintEstimateWriter(outPredict);
    pe.setOutputFormat(outFormat);
  }

  private void setOutputFormat(DataView view) {
    outFormat = params.get("format");
    if (outFormat == null) {
      final StringBuilder sb = new StringBuilder("%ID");
      if (view.graphHasMissingClassValues())
        sb.append(" %CLASS");
      sb.append(" %PREDICTION");
      for (int i = 0; i < view.getAttribute().size(); i++)
        sb.append(" %ESTIMATE!").append(view.getAttribute().getToken(i));
      outFormat = sb.toString();
    }
    logger.info("Using output format='" + outFormat + "'");
    pe = new PrintEstimateWriter(outPredict);
    pe.setOutputFormat(outFormat);
  }

  private Classification getClassification(String param, DataView view) {
    logger.info("Reading labels from: "+params.get(param,"--NA--"));
    if (!params.containsKey(param))
      return null;

    ReadClassification lReader = new ReadClassificationGeneric();
    File file = new File(params.get(param));
    logger.info("Read " + param + " entities from '" + file.getName() + "'");
    Classification c = lReader.readClassification(view.getGraph(), view.getNodeType(), view.getAttribute(), file);
    if (param.equals("known") && params.containsKey("binary"))
      c = c.asBinaryClassification(params.get("binary"));
    if (DEBUG) {
      System.out.println(param + " classification:");
      System.out.println(c.toString());
    }
    return c;
  }

  public Classification getTest() {
    if (test == null && params.containsKey("test")) {
      DataView view = getDataView();
      if (view == null) {
        logger.severe("no data view specified");
        return null;
      }
      test = getClassification("test", view);
    }
    return test;
  }

  public void setTest(Classification t) {
    test = t;
  }

  public Classification getTruth() {
    if (truth == null) {
      DataView view = getDataView();
      if (view == null) {
        logger.severe("no data view specified");
        return null;
      }
      if(params.containsKey("truth")) {
        truth = getClassification("truth", view);
      } else {
        truth = view.getTruth();
      }
    }
    return truth;
  }

  public void setTruth(Classification t) {
    truth = t;
  }

  public Classification getKnown() {
    if (known == null && params.containsKey("known"))
      known = getClassification("known", getDataView());
    return known;
  }

  public void setKnown(Classification k) {
    known = k;
  }

  public void setGraph(final Graph g) {
    graph = g;
  }

  public Graph getGraph() {
    if (graph != null)
      return graph;

    if (!params.containsKey(ParamDB)) {
    	logger.severe("no schema file specified");
    	return null;
    }
    File schema = new File(params.get(ParamDB));

    if (params.getBoolean("gda")) {
      graph = SchemaReader.readGDASchema(schema,
          new File(params.get(ParamEdge)));
      logger.info("Read GDA graph from '" + schema.getName() + "' and '" + params.get(ParamEdge) + "'");
    } else if (schema.getName().toLowerCase().endsWith(".net")) {
      graph = PajekGraph.readGraph(schema);
      logger.info("Read pajek net-graph from '" + schema.getName() + "'");
    } else {
      graph = NetkitGraph.readGraph(schema);
      logger.info("Read graph from '" + schema.getName() + "'");
    }
    StringBuffer sb = new StringBuffer();
    sb.append("   ").append(graph.numNodes()).append(" nodes").append(NetKitEnv.newline);
    if (DEBUG) {
      for (Node n : graph.getNodes())
        sb.append("   ").append(n.toString()).append(NetKitEnv.newline);
    }
    logger.info(sb.toString());
    sb = new StringBuffer();
    sb.append("   ").append(graph.numEdges()).append(" edges").append(NetKitEnv.newline);
    if (DEBUG) {
      for (Edge e : graph.getEdges())
        sb.append("   ").append(e.toString()).append(NetKitEnv.newline);
    }
    logger.info(sb.toString());
    return graph;
  }

  public void setLearner(NetworkLearner l) {
    learner = l;
  }

  public NetworkLearner getLearner() {
    if (learner == null) {
      Classifier lc = lclassifiers.get(params.get(NetworkLearning.LC_PREFIX), conf);
      NetworkClassifier nc = rclassifiers.get(params.get(NetworkLearning.RC_PREFIX), conf);
      InferenceMethod ic = imethods.get(params.get(NetworkLearning.IM_PREFIX), conf);
      learner = new NetworkLearner(lc, nc, ic, params.getBoolean("applycmn"));
    }
    return learner;
  }

  public void setSplits(DataSplit[] s) {
    splits = s;
  }

  public DataSplit[] getSplits() {
    if (splits != null)
      return splits;

    dataView = getDataView();
    if (dataView == null) {
      logger.severe("no data view specified");
      return new DataSplit[] {};
    }

    splits = null;
    if (params.containsKey("known") || params.containsKey("test")) {
      Classification known = getKnown();
      Classification test = getTest();

      if (known != null)
        dataView.setClassification(known);

      if (test != null)
        dataView.setTruth(test);

      splits = new DataSplit[] { dataView.getSplit(known, test) };
    } else if (dataView.graphHasMissingClassValues()) {
      splits = new DataSplit[] { dataView.getSplit(new NodeFilter() {
        public boolean accept(Node n) {
          return !n.isMissing(dataView.clsIdx);
        }
      }) };
    } else {
      int runs = params.getInt("runs", 10);
      double sample = params.getDouble("sample");
      if (params.containsKey(AL_PREFIX) && sample <= 0)
      {
        sample = dataView.getAttribute().size();
        params.set("stratified", true);
        params.set("sample",sample);
        logger.info("active learning is on and sample<=0.  Turning on stratified sampling and setting sample="+sample);
      }
      
      if (sample <= 0) {
        if (runs == 1)
          usage("ERROR: Cannot have sample <= 0 and runs <= 1!");
        else
          splits = dataView.crossValidate(runs);
      } else {
        splits = dataView.getSplits(runs, sample);
      }
    }
    if (DEBUG)
      verifyDataSamples(splits);
    return splits;
  }

  public EdgeCreator[] getEdgeCreators() {
    if(ecs != null || !params.containsKey(EC_PREFIX))
        return ecs;

    Attributes as = graph.getAttributes(nodeType);
    Attribute clsAttrib = getAttribute();
    int clsIdx = as.getAttributeIndex(clsAttrib.getName());

    int maxEdges = params.getInt("maxedges", -1);
    int maxInstEdges = params.getInt("maxinstanceedges", maxEdges);

    List<EdgeCreator> eclist = new ArrayList<EdgeCreator>();
    String[] creators = params.get(EC_PREFIX).split(",");
    for (int i = 0; i < creators.length; i++) {
      EdgeCreator ec = edgecreators.get(creators[i]);
      int maxEdgesForEC = (ec.isByAttribute() ? maxEdges : maxInstEdges);
      
      if(!ec.isByAttribute())
      {
        eclist.add(ec);
        ec.initialize(graph, nodeType, 0, Double.NaN, maxEdgesForEC);
      }
      else
      {
        final boolean byCatValue  = params.getBoolean("onlycatvalues") || !params.getBoolean("onlycatattrib");
        final boolean byCatAttrib = params.getBoolean("onlycatattrib") || !params.getBoolean("onlycatvalues");
        
        for (int k = 0; k < as.attributeCount(); k++) {
          if (k == clsIdx || k == as.getKeyIndex()) {
            continue;
          }
          
          Attribute a = as.getAttribute(k);

          // TODO: Should we handle non-categoricals as well?
          if(byCatValue && ec.canHandleAttributeValue(a) && a.getType() == Type.CATEGORICAL) {
            AttributeCategorical ac = (AttributeCategorical)a;
              
            // don't do by-attribute-value if we have too many unique attribute values
            // TODO: Make this threshold a parameter
            if(ac.size() > graph.numNodes(nodeType)*0.25)
              continue;
              
            for(int av=0;av<ac.size();av++) {
              EdgeCreator ecK = edgecreators.get(creators[i]);
              eclist.add(ecK);
              ecK.initialize(graph, nodeType, k, av, maxEdgesForEC);
            }
          }
          // TODO: Should we do both by-attribute _and_ by-attribute-value
          // if the edgecreator can handle both?  That is not clear.
          if (byCatAttrib && ec.canHandle(a)) {
            EdgeCreator ecK = edgecreators.get(creators[i]);
            eclist.add(ecK);
            ecK.initialize(graph, nodeType, k, Double.NaN, maxEdgesForEC);
          }
        }
      }
    }
    ecs = eclist.toArray(new EdgeCreator[0]);
    return ecs;
  }
  
  public void augmentGraph(final DataSplit split, final GraphView gv, final EdgeCreator[] ecs) {
    if(ecs==null || ecs.length==0)
      return;
    
    final Graph graph = gv.getGraph();
    Classification labels = null;
    final boolean useTrueAssort = params.getBoolean("usetrueassort",false);

    Map<EdgeCreator, Double> weightsByEC = new HashMap<EdgeCreator, Double>();
    Map<EdgeType, Double> weightsByEdgeType = new HashMap<EdgeType, Double>();
    int maxECS = params.getInt("numet", ecs.length);
    int minECS = params.getInt("minet", 0);
    if(minECS > ecs.length) minECS = ecs.length;

    if(maxECS < minECS) maxECS = minECS;
    if(maxECS > ecs.length) maxECS = ecs.length;
      
    double minAS = params.getDouble("minassort", 0.05);

    double[] assortET = new double[ecs.length];
    Arrays.fill(assortET,-2.0D);
    boolean weightByAssort = params.getBoolean("weightedgesbyassort", false);
    boolean mergeEdges = params.getBoolean("mergeedges", false);

    int numPass = 0;
    for (int idx = 0; idx < ecs.length; idx++) {
      final EdgeCreator ec = ecs[idx];
      logger.info("Getting edges and calculate assortativity for "+ec.getEdgeType());
      ec.buildModel(split);
      final double assort = ec.getAssortativity(useTrueAssort);
      assortET[idx] = assort;
      if(assort>=minAS) numPass++;
    }
    
    // if we have too many or too few edge creators who pass, let's modify
    if(numPass < minECS || numPass > maxECS) {
      double[] sortedAssort = assortET.clone();
      Arrays.sort(sortedAssort);
      
      if(numPass > maxECS) { // too many, so reset minAS to only include top maxECS
        minAS = sortedAssort[sortedAssort.length - maxECS];
      } else { // too few, so lower minAS
        minAS = sortedAssort[sortedAssort.length - minECS];
        if(minAS <= 0.001)
          minAS = 0.001;
      }
    }

    // now let's add all creators whose assortativity is high enough
    for (int idx = 0; idx < ecs.length; idx++) {
      final EdgeCreator ec = ecs[idx];
      final double assort = assortET[idx];

      if (assort >= minAS) {
        logger.info("Using edge=" + ec.getEdgeType() + " with assortativity=" + assort);
        weightsByEC.put(ec, assortET[idx]);
      } else {
        logger.info("Pruning edge=" + ec.getEdgeType() + " with assortativity=" + assort);
      }
    }
    logger.info("Done computing assortativities.");

    if (weightByAssort) {
      for (EdgeCreator ec : weightsByEC.keySet())
        weightsByEdgeType.put(ec.getEdgeType(), weightsByEC.get(ec));
      
      for (EdgeType et : graph.getEdgeTypes()) {
        double[] assort = GraphMetrics.calculateNodeBasedAssortativityCoeff(labels, et);
        
        if (assort[1] > minAS) {
          weightsByEdgeType.put(et, assort[1]);
          logger.info("Using edge=" + et + " with assortativity=" + assort[1]);
        } else {
          weightsByEdgeType.put(et, 0.0D);
          logger.info("Pruning edge=" + et + " with assortativity=" + assort[1]);
        }
      }
    }

    EdgeCreator[] ecPruned = weightsByEC.keySet().toArray(new EdgeCreator[0]);

    if (ecPruned.length > 0) {
      logger.info("About to add new edges to graph.");
      gv.enhanceGraphWithAttributeEdges(nodeType, ecPruned, weightsByEdgeType, mergeEdges);

      if(params.containsKey("savegraph"))
      {
        NetkitGraph.saveGraph(graph, params.get("savegraph"));
      }
      logger.info("Done adding new edges to graph.");
    }
  }

  public Estimate runInference(DataSplit split) {
    int depth = params.getInt("depth", 0);
    boolean learnWithTruth = params.getBoolean("learnwithtruth");

    EdgeCreator[] ecs = getEdgeCreators();

    GraphView gv = null;
    if(ecs != null) {
      gv = new GraphView(split.getView().getGraph());
      augmentGraph(split,gv,ecs);
    }

    NetworkLearner learner = getLearner();
    Estimate predictions = null;
    if (params.getBoolean("loo")) {
      predictions = learner.runLeaveOneOut(split, learnWithTruth, depth);
    } else if (params.containsKey(AL_PREFIX)) {
      PickLabelStrategy ps = alstrategies.get(params.get(AL_PREFIX));
      int maxPicks = params.getInt("maxpicks",split.getTestSetSize());      
      int picksPerIteration = params.getInt("numpicks",1);      
      predictions = learner.runActiveLearner(ps, split, picksPerIteration, maxPicks, learnWithTruth, depth);
    } else {
      boolean showItAcc = params.getBoolean("showitacc");
      predictions = learner.runInference(split, showItAcc, learnWithTruth, depth);
    }

    if (gv != null)
    {
      logger.info("About to reset graph.");
      gv.reset();
      logger.info("Done with graph reset.");
    }

    return predictions;
  }

  public void runInference() {
    DataView view = getDataView();
    NetworkLearner learner = getLearner();

    Classification outTruth = view.getTruth();
    AttributeCategorical attribute = view.getAttribute();
    String nodeType = view.getNodeType();
    Graph g = view.getGraph();

    DataSplit[] splits = getSplits();

    double[][] auc = new double[view.getAttribute().size()][splits.length];
    double[] acc = new double[splits.length];
    double[] assortN = new double[splits.length];
    double[] assortW = new double[splits.length];
    double[] nAssortN = new double[splits.length];
    double[] nAssortW = new double[splits.length];

    GraphMetrics gm = g.getMetrics();
    double[] truthAssort = gm.calculateEdgeBasedAssortativityCoeff(nodeType, attribute);
    double[] truthNodeAssort = gm.calculateNodeBasedAssortativityCoeff(nodeType, attribute);
    ConfusionMatrix cfFinal = new ConfusionMatrix(attribute);

    boolean hasTruth = false;

    for (int ki = 0; ki < splits.length; ki++) {
      logger.info("Running validation run " + (ki + 1) + " of " + splits.length);
      if (params.getBoolean("saveitpredict"))
        learner.saveIterationPredictions(params.get("output"), pe, true, splits[ki].getTestSet(), "#" + ki);

      if(params.containsKey("savepajek") &&
          (params.getBoolean("saveitpredict") || params.getBoolean("showitacc")))
          learner.saveIterationsInPajek(params.get("savepajek")+"-"+ki+"-time.paj");
      
      Estimate predictions = runInference(splits[ki]);

      if(params.containsKey("savepajek"))
        PajekGraph.saveGraph(g, params.get("savepajek")+"-"+ki+".net", truth, predictions, null); 
      
      logger.info("Outputting predictions for run " + (ki + 1) + " of " + splits.length);
      outPredict.println("#" + ki);
      pe.setOutput(outPredict);
      for (Node node : predictions)
        pe.println(node, predictions, outTruth);
      logger.info("Done!");
      outPredict.flush();
      
      if (params.getBoolean("showassort")) {
        double[] a = gm.calculateEdgeBasedAssortativityCoeff(view.getNodeType(), attribute);
        System.out.println("EdgeBasedAssortCoeff-Normal-" + (ki + 1) + ": " + a[0]);
        System.out.println("EdgeBasedAssortCoeff-Weighted-" + (ki + 1) + ": " + a[1]);
        for (String et : g.getEdgeTypeNames()) {
          a = gm.calculateEdgeBasedAssortativityCoeff(view.getNodeType(),
              attribute, g.getEdgeType(et));
          System.out.println("EdgeBasedAssortCoeff-Normal-" + (ki + 1)
              + "[edge=" + et + "]: " + a[0]);
          System.out.println("EdgeBasedAssortCoeff-Weighted-" + (ki + 1)
              + "[edge=" + et + "]: " + a[1]);
        }
        assortN[ki] = a[0];
        assortW[ki] = a[1];

        a = gm.calculateNodeBasedAssortativityCoeff(view.getNodeType(),
            attribute);
        System.out.println("NodeBasedAssortCoeff-Normal-" + (ki + 1) + ": "
            + a[0]);
        System.out.println("NodeBasedAssortCoeff-Weighted-" + (ki + 1) + ": "
            + a[1]);
        for (String et : g.getEdgeTypeNames()) {
          a = gm.calculateNodeBasedAssortativityCoeff(view.getNodeType(),
              attribute, g.getEdgeType(et));
          System.out.println("NodeBasedAssortCoeff-Normal-" + (ki + 1)
              + "[edge=" + et + "]: " + a[0]);
          System.out.println("NodeBasedAssortCoeff-Weighted-" + (ki + 1)
              + "[edge=" + et + "]: " + a[1]);
        }
        nAssortN[ki] = a[0];
        nAssortW[ki] = a[1];
      }

      if (splits[ki].hasTruth()) {
        hasTruth = true;
        ConfusionMatrix cf = new ConfusionMatrix(predictions, view.getTruth());
        cfFinal.add(cf);
        StringWriter sw = new StringWriter();
        sw.append("ConfusionMatrix-" + (ki + 1) + ":").append(NetKitEnv.newline);
        cf.printMatrix(new PrintWriter(sw, true));
        logger.info(sw.toString());
        acc[ki] = cf.getAccuracy();
        logger.info("Accuracy-" + (ki + 1) + ": " + acc[ki]);
      }

      if (splits[ki].hasTruth()
          && (params.getBoolean("saveroc") || params.getBoolean("showauc"))) {
        for (int i = 0; i < view.getAttribute().size(); i++) {
          ROC roc = new ROC(predictions, view.getTruth(), i);
          auc[i][ki] = roc.getAUC();
          StringBuilder sb = new StringBuilder();
          sb.append("AUC-" + view.getAttribute().getToken(i) + "-"
              + (ki + 1) + ": " + roc.getAUC());
          if (params.getBoolean("saveroc")) {
            File f = new File(params.get("output", "roc") + "-"
                + view.getAttribute().getToken(i) + "-" + ki + ".roc");
            roc.save(f);
            sb.append(" (save as " + f.getName() + ")");
          }
          sb.append(NetKitEnv.newline);
          if(params.getBoolean("showauc"))
            System.out.print(sb.toString());
          else
            logger.info(sb.toString());
        }
      }
      view.resetTruth();
    }

    double avgAN = StatUtil.getMean(assortN);
    double stddevAN = StatUtil.getStdDev(assortN, avgAN);
    double avgAW = StatUtil.getMean(assortW);
    double stddevAW = StatUtil.getStdDev(assortW, avgAW);

    double avgNAN = StatUtil.getMean(nAssortN);
    double stddevNAN = StatUtil.getStdDev(nAssortN, avgNAN);
    double avgNAW = StatUtil.getMean(nAssortW);
    double stddevNAW = StatUtil.getStdDev(nAssortW, avgNAW);

    if (!hasTruth) {
      if (params.getBoolean("showassort")) {
        System.out.println("EdgeBasedAssortCoeff-Normal-Final: " + avgAN + " ("
            + stddevAN + ")");
        System.out.println("EdgeBasedAssortCoeff-Weighted-Final: " + avgAW
            + " (" + stddevAW + ")");
        System.out.println("NodeBasedAssortCoeff-Normal-Final: " + avgNAN
            + " (" + stddevNAN + ")");
        System.out.println("NodeBasedAssortCoeff-Weighted-Final: " + avgNAW
            + " (" + stddevNAW + ")");
      }
    } else {
      if (params.getBoolean("showassort")) {
        double sigAN = ((stddevAN <= 0) ? 0 : StatUtil.getSignificance(
            splits.length, Math.abs((avgAN - truthAssort[0]) / stddevAN)));
        System.out.println("EdgeBasedAssortCoeff-Normal-Final: " + avgAN + " ("
            + stddevAN + ") truth=" + truthAssort[0] + " sigTest=" + sigAN);

        double sigAW = ((stddevAW <= 0) ? 0 : StatUtil.getSignificance(
            splits.length, Math.abs((avgAW - truthAssort[1]) / stddevAW)));
        System.out.println("EdgeBasedAssortCoeff-Weighted-Final: " + avgAW
            + " (" + stddevAW + ") truth=" + truthAssort[1] + " sigTest="
            + sigAW);

        double sigNAN = ((stddevNAN <= 0) ? 0 : StatUtil.getSignificance(
            splits.length, Math.abs((avgNAN - truthNodeAssort[0]) / stddevNAN)));
        System.out.println("NodeBasedAssortCoeff-Normal-Final: " + avgNAN
            + " (" + stddevNAN + ") truth=" + truthNodeAssort[0] + " sigTest="
            + sigNAN);

        double sigNAW = ((stddevNAW <= 0) ? 0 : StatUtil.getSignificance(
            splits.length, Math.abs((avgNAW - truthNodeAssort[1]) / stddevNAW)));
        System.out.println("NodeBasedAssortCoeff-Weighted-Final: " + avgNAW
            + " (" + stddevNAW + ") truth=" + truthNodeAssort[1] + " sigTest="
            + sigNAW);
      }
      double baseAcc = view.getTruth().getBaseAccuracy();
      StringWriter sw = new StringWriter();
      sw.append("ConfusionMatrix-Final:").append(NetKitEnv.newline);
      cfFinal.printMatrix(new PrintWriter(sw, true));
      logger.info(sw.toString());

      double avgAcc = StatUtil.getMean(acc);
      double stddevAcc = StatUtil.getStdDev(acc, avgAcc);
      double sigAcc = ((stddevAcc <= 0) ? 0 : StatUtil.getSignificance(
          splits.length, Math.abs((avgAcc - baseAcc) / stddevAcc)));
      logger.info("Accuracy-Final: " + avgAcc + " (" + stddevAcc
          + ") base=" + baseAcc + " sigTest=" + sigAcc);

      if (params.getBoolean("showauc") || params.getBoolean("saveroc")) {
        for (int i = 0; i < view.getAttribute().size(); i++) {
          double avgAUC = StatUtil.getMean(auc[i]);
          double stddevAUC = StatUtil.getStdDev(auc[i], avgAUC);
          String msg = "AUC-" + view.getAttribute().getToken(i)
              + "-Final: " + avgAUC + " (" + stddevAUC + ")";
          if(params.getBoolean("showauc"))
            System.out.println(msg);
          else
            logger.info(msg);
        }
      }
    }
  }
  
  private int setDouble(String[] argv, int idx, String option, double min) {
    if (argv.length < idx + 2)
      usage("No value given for parameter " + option + "!");

    try {
      params.set(option, Double.parseDouble(argv[++idx]));
    } catch (NumberFormatException nfe) {
      nfe.printStackTrace();
    }
    if (!Double.isNaN(min))
    {
      if (params.getDouble(option) < min)
        usage("Invalid value for " + option + "(" + params.getDouble(option) + ")! (must be >= "+min+"))!");
    }
    logger.info("Will use " + option + "=" + params.getDouble(option));
    return idx;
  }

  private int setInt(String[] argv, int idx, String option, int min) {
    if (argv.length < idx + 2)
      usage("No value given for parameter " + option + "!");

    try {
      params.set(option, Integer.parseInt(argv[++idx]));
    } catch (NumberFormatException nfe) {
      nfe.printStackTrace();
    }
    if (min > Integer.MIN_VALUE)
    {
      if (params.getInt(option) < min)
        usage("Invalid value for " + option + "(" + params.getInt(option) + ")! (must be >= "+min+"))!");
    }
    logger.info("Will use " + option + "=" + params.getInt(option));
    return idx;
  }

  private int setLong(String[] argv, int idx, String option, long min) {
    if (argv.length < idx + 2)
      usage("No value given for parameter " + option + "!");

    try {
      params.set(option, Long.parseLong(argv[++idx]));
    } catch (NumberFormatException nfe) {
      nfe.printStackTrace();
    }
    if (min > Long.MIN_VALUE)
    {
      if (params.getLong(option) < min)
        usage("Invalid value for " + option + "(" + params.getLong(option) + ")! (must be >= "+min+"))!");
    }
    logger.info("Will use " + option + "=" + params.getLong(option));
    return idx;
  }
  
  private void setOptions(String[] argv) {
    if (argv.length == 0)
      usage(null);

    int idx = 0;
    if(argv[idx].equalsIgnoreCase(Netkit.learning))
      idx++;
    while (idx < argv.length && argv[idx].startsWith("-")) {
      String p = argv[idx].toLowerCase().substring(1);
      if (p.startsWith("h")) {
        usage(null);
      } else if (p.equals("log")) {
        final String filename = argv[++idx];
        NetKitEnv.setLogfile(filename);
        logger.info("Set log output to "+filename);
      } else if (p.equals("gda")
          || p.equals("saveitpredict")
          || p.equals("saveroc")
          || p.equals("showauc")
          || p.equals("showitacc")
          || p.equals("showassort")
          || p.equals("usetrueassort")
          || p.equals("mergeedges")
          || p.equals("applycmn")
          || p.equals("onlycatvalues")
          || p.equals("onlycatattrib")
          || p.equals("loo")
          || p.equals("stratified")
          || p.equals("replacement")
          || p.equals("sampleunknown")          
          || p.equals("weightedgesbyassort")
          || p.equals("maxcomponent")
          || p.equals("prunesingletons")
          || p.equals("prunezeroknowledge")
          || p.equals("edgeweightlog")
          || p.equals("learnwithtruth")) {
        params.set(p, true);
        if (p.equals("loo"))
          logger.info("Will perform leave-one-out estimations.");
        if (p.equals("maxcomponent"))
          logger.info("Will use only largest connected component.");
        if (p.equals("applycmn"))
          logger.info("Will apply class mass normalization.");
        if (p.equals("prunesingletons"))
          logger.info("Will remove singleton nodes.");
        if (p.equals("onlycatvalues")) {
          if (params.getBoolean("onlycatattrib")) {
            usage("Invalid option: " + p + " cannot be used with '-onlyCatAttrib'");
          }
          logger.info("Will create categorical relations only on a per-value basis.");
        }
        if (p.equals("onlycatattrib")) {
          if (params.getBoolean("onlycatvalues")) {
            usage("Invalid option: " + p + " cannot be used with '-onlyCatValues'");
          }
          logger.info("Will create categorical relations for an attribute as a whole.");
        }
        if (p.equals("sampleunknown"))
          logger.info("Will sample from nodes whose labels are unknown (no ground truth).");
        if (p.equals("sampleunknown"))
          logger.info("Will sample from nodes whose labels are unknown (no ground truth).");
        if (p.equals("sampleunknown"))
          logger.info("Will sample from nodes whose labels are unknown (no ground truth).");
        if (p.equals("learnwithtruth"))
          logger.info("Will allow classifiers full access to truth during learning.");
        if (p.equals("gda"))
          logger.info("Will read input as GDA format.");
        if (p.equals("saveitpredict"))
          logger.info("Will save predictions after each iteration.");
        if (p.equals("weightedgesbyassort"))
          logger.info("Will reweight edges by their assortativity.");
        if (p.equals("mergeedges"))
          logger.info("Will merge new edges into one generic edge type.");
        if (p.equals("usetrueassort"))
          logger.info("Will use truth-based assortativity for newly created edges.");
        if (p.equals("saveroc"))
          logger.info("Will save ROC curves.");
        if (p.equals("showauc"))
          logger.info("Will calculate AUCs.");
        if (p.equals("showassort"))
          logger.info("Will calculate assortativity.");
        if( p.equals("stratified"))
          logger.info("Will do stratified sampling");
        if( p.equals("replacement"))
          logger.info("Will sample with replacement");
        if (p.equals("showitacc"))
          logger.info("Will show accuracy for each inference iteration.");
      } else if (p.equals("sample")) {
        idx = setDouble(argv,idx,p,0.0D);
      } else if (p.equals("depth")) {
        idx = setInt(argv,idx,p,0);
      } else if (p.equals("seedsize")) {
        idx = setInt(argv,idx,p,0);
      } else if (p.equals("seed")) {
        idx = setLong(argv,idx,p,0);
      } else if (p.equals("maxpicks")) {
        idx = setInt(argv,idx,p,1);
      } else if (p.equals("numpicks")) {
        idx = setInt(argv,idx,p,1);
      } else if (p.equals("numet")) {
        idx = setInt(argv,idx,p,1);
      } else if (p.equals("minet")) {
        idx = setInt(argv,idx,p,1);
      } else if (p.equals("maxedges")) {
        idx = setInt(argv,idx,p,Integer.MIN_VALUE);
        if (params.getInt(p) == 0)
          usage("Invalid value for " + p + "(" + params.getInt(p) + ")! (must be <>0)!");
      } else if (p.equals("maxinstanceedges")) {
        idx = setInt(argv,idx,p,Integer.MIN_VALUE);
        if (params.getInt(p) == 0)
          usage("Invalid value for " + p + "(" + params.getInt(p) + ")! (must be <>0)!");
      } else if (p.equals("minassort")) {
        idx = setDouble(argv,idx,p,Double.NaN);
        if (params.getDouble(p) < -1 && params.getDouble(p) > 1)
          usage("Invalid value for " + p + "(" + params.getDouble(p) + ")! (must be in range [-1:1] inclusive)!");
      }
      else if (p.equals("runs")) {
       idx = setInt(argv,idx,p,1);
      } else if (p.equals("vprior")
          || p.equals("known")
          || p.equals("test")
          || p.equals("truth")
          || p.equals("classifier")
          || p.equals(RC_PREFIX)
          || p.equals(LC_PREFIX)
          || p.equals(AL_PREFIX)
          || p.equals(EC_PREFIX)
          || p.equals("output")
          || p.equals("savepajek")
          || p.equals("binary")
          || p.equals("strategy")
          || p.equals("savegraph")
          || p.equals(IM_PREFIX)
          || p.equals("format")) {
        if (argv.length < idx + 2)
          usage("No value given for parameter " + p);
        params.set(p, argv[++idx]);
        if (p.equals("vprior"))
          logger.info("Will read value priors from " + argv[idx]);
        if (p.equals("known"))
          logger.info("Will read known labels from " + argv[idx]);
        if (p.equals("test"))
          logger.info("Will read test labels from " + argv[idx]);
        if (p.equals("truth"))
          logger.info("Will read true labels from " + argv[idx]);
        if (p.equals("output"))
          logger.info("Will send output to " + argv[idx]);
        if (p.equals("format"))
          logger.info("Will use output format " + argv[idx]);
        if (p.equals("binary"))
          logger.info("Will perform binary classification on " + argv[idx] + " not" + argv[idx]);
        if (p.equals("savegraph"))
          logger.info("Will save graphs to files using prefix " + argv[idx]);
        if (p.equals("savepajek"))
          logger.info("Will save Pajek networks to files using prefix " + argv[idx]);
        if (p.equals(RC_PREFIX))
          logger.info("Will use relational classifier " + argv[idx]);
        if (p.equals(LC_PREFIX))
          logger.info("Will use local classifier " + argv[idx]);
        if (p.equals(IM_PREFIX))
          logger.info("Will use inference method " + argv[idx]);
        if (p.equals(AL_PREFIX))
          logger.info("Will do active learning with strategy " + argv[idx]);
        if (p.equals(EC_PREFIX))
          logger.info("Will add attribute-based edges, using the creation-strategies: " + argv[idx]);
      } else if (p.equals("attribute")) {
        String[] val = argv[++idx].split(":");
        if (val.length != 2)
          usage("Invalid parameter(" + argv[idx] + ") given for " + p);
        params.set(ParamNT, val[0]);
        params.set(ParamAT, val[1]);
        logger.info("Will build model on '" + argv[idx] + "' (" + val[0] + "," + val[1] + ")");
      } else {
        conf.set(p, argv[++idx]);
        logger.info("Set " + p + "=" + argv[idx]);
      }
      idx++;
    }

    if (idx < argv.length) {
      params.set(ParamDB, argv[idx]);
      logger.info("Using schema file=" + argv[idx]);
    }
    if (params.getBoolean("gda") && idx + 1 < argv.length) {
      params.set(ParamEdge, argv[idx + 1]);
      logger.info("Using GDA edge file=" + argv[idx + 1]);
    }
  }

  public void setupExperiment() {
    rndSeed = getRandomSeed();
    VectorMath.setSeed(rndSeed);

    setOutput();
    getGraph();
    getAttribute();
    logger.info("Classify(NodeType:Attribute): " + nodeType + ":" + attribute.getName());

    getDataView();
    getTruth();
    getKnown();
    getTest();
    getOutputFormat();
    if (getClassPrior() != null)
      dataView.setPrior(classPrior);

    SharedNodeInfo.initialize(graph);

    if (DEBUG) {
      System.out.println("True Classification:");
      System.out.println(dataView.getTruth().toString());
    }

    if (truth != null)
      dataView.setTruth(truth);
  }
  
  public void run() {
    NetKitEnv.logTime("starting netkit");
    setupExperiment();
    NetKitEnv.logTime("after setupExperiment");

    if (params.getBoolean("showassort")) {
      double[] a = GraphMetrics.calculateEdgeBasedAssortativityCoeff(dataView.getTruth());
      System.out.println("EdgeBasedAssortCoeff-Normal-Truth: " + a[0]);
      System.out.println("EdgeBasedAssortCoeff-Weighted-Truth: " + a[1]);
      for (String et : graph.getEdgeTypeNames()) {
        a = GraphMetrics.calculateEdgeBasedAssortativityCoeff(dataView.getTruth(), graph
            .getEdgeType(et));
        System.out.println("EdgeBasedAssortCoeff-Normal-Truth[edge=" + et
            + "]: " + a[0]);
        System.out.println("EdgeBasedAssortCoeff-Weighted-Truth[edge=" + et
            + "]: " + a[1]);
      }

      a = GraphMetrics.calculateNodeBasedAssortativityCoeff(dataView.getTruth());
      System.out.println("NodeBasedAssortCoeff-Normal-Truth: " + a[0]);
      System.out.println("NodeBasedAssortCoeff-Weighted-Truth: " + a[1]);
      for (String et : graph.getEdgeTypeNames()) {
        a = GraphMetrics.calculateNodeBasedAssortativityCoeff(dataView.getTruth(), graph
            .getEdgeType(et));
        System.out.println("NodeBasedAssortCoeff-Normal-Truth[edge=" + et
            + "]: " + a[0]);
        System.out.println("NodeBasedAssortCoeff-Weighted-Truth[edge=" + et
            + "]: " + a[1]);
      }
    }

    NetKitEnv.logTime("after initial assortativity computations");
    runInference();
    NetKitEnv.logTime("after complete run");
    outPredict.close();
  }

  public static String[] getCommandLines() {
    String opt = Netkit.learning;
    return new String[]{
           "usage: Netkit ["+opt+"] [-h] [OPTIONS] <schema-file|pajek-net-file>",
           "usage: Netkit ["+opt+"] [-h] [OPTIONS] -gda <class-file> <edge-file>"};
  }
  
  public static void usage(String msg) {
    for(String cmd : getCommandLines())
      System.out.println(cmd);
    
    if (msg == null) {
      System.out.println();
      System.out.println("GENERAL OPTIONS");
      System.out.println("  -h                This help screen");
      System.out.println("  -log <filename>   Where to send logging information.");
      System.out.println("                      In logging.properties:");
      System.out.println("                        handlers property must be set to java.util.logging.FileHandler.");
      System.out.println("                      default out: see (java.util.logging.FileHandler.pattern) in logging.properties");
      System.out.println("  -<key> <value>    Overrides a property in NetKit.properties.");
      System.out.println("                       e.g.: -numit 10");
      System.out.println("                    overrides all inferencemethod.X.numit properties");
      System.out.println("                    the key matches the last element in the property keys");

      System.out.println();
      System.out.println("LEARNING OPTIONS");
      System.out.println("  -applyCMN         Will apply class-mass normalization on predictions");
      System.out.println("  -"+RC_PREFIX+"      Which relational classifier to run (cannot be used with -learner)");
      System.out.println("                      " + ArrayUtil.asString(rclassifiers.getValidNames()));
      System.out.println("                      default: wvrn");
      System.out.println("  -"+LC_PREFIX+"      Which local classifier to run (cannot be used with -learner)");
      System.out.println("                      " + ArrayUtil.asString(lclassifiers.getValidNames()));
      System.out.println("                      default: classprior");
      System.out.println("  -"+IM_PREFIX+"  Which collective inference method to use (cannot be used with -learner)");
      System.out.println("                      " + ArrayUtil.asString(imethods.getValidNames()));
      System.out.println("                      default: relaxlabel");
      System.out.println("  -attribute <NT>:<A> Which attribute to classify, where");
      System.out.println("                    <NT> is the @RELATION table in schema file");
      System.out.println("                    <A> is the name of the attribute in that table");
      System.out.println("                    default: last attribute of first table in schema");
      
      System.out.println();
      System.out.println("TRAIN/TEST OPTIONS");
      System.out.println("  -seed #           Use this random seed for sampling (when -known not given)");
      System.out.println("  -sample #         Sample from the graph for each run (cannot be used with -known");
      System.out.println("                      If value <= 0 && active learning is on, turn on stratified sampling and sample the number of classes");
      System.out.println("                      If value >= 1, sample this many absolute instances");
      System.out.println("                      If value > 0 and < 1, sample this ratio");
      System.out.println("                      If value <= 0, perform cross-validation");
      System.out.println("                      default: 0");
      System.out.println("  -pruneZeroKnowledge For each test/train split, remove all nodes in the test");
      System.out.println("                        set which belong to disconnected components which have");
      System.out.println("                        no known nodes.");
      System.out.println("  -pruneSingletons  Remove all singleton nodes before test/training begings.");
      System.out.println("  -vprior <file>    Read priors from this file");
      System.out.println("                      Line format is:");
      System.out.println("                         <value>,<prior>");
      System.out.println("                      <node-ID> must match a node-ID from the graph");
      System.out.println("                      the scores will be normalized to 1.");
      System.out.println("  -stratified       Will do stratified sampling (rather than random)");
      System.out.println("  -replacement      Will sample with replacement.");
      System.out.println("  -sampleUnknown    Will sample from nodes whose labels are unknown (no ground truth).");
      System.out.println("  -runs #           Perform this many runs (cannot be used with -known).");
      System.out.println("                      Perform cross-validation runs if -sample is 0.");
      System.out.println("                      default: 10 (unless -known is specified)");
      System.out.println("  -learnWithTruth   Allow the classifiers full access when learning the model");
      System.out.println("  -loo              Perform leave-one-out estimations.");
      System.out.println("  -truth <file>     A file consisting of true labels");
      System.out.println("                      Line format is:");
      System.out.println("                        <node-ID>,<label>");
      System.out.println("  -known <file>     A file consisting of known labels");
      System.out.println("                      Line format is:");
      System.out.println("                        <node-ID>,<label>");
      System.out.println("                      This tell NetKit to use these as the training set and everything");
      System.out.println("                      else as the test set.");
      System.out.println("  -test <file>     A file consisting of test IDs and labels):");
      System.out.println("                      Line format either is:");
      System.out.println("                        <node-ID>,<label>");
      System.out.println("                      where unknown labels are specified as ?");
      System.out.println("                      This tell NetKit to use these as the test set.");
      System.out.println("  -depth #          When training, make neighbor class labels visible up to depth #");
      System.out.println("                      away from each training instance.");
      System.out.println("                      default: 0 (i.e., do not explicitly make neighbors visible,");
      System.out.println("                                  unless they are in the training estimate as well)");

      System.out.println();
      System.out.println("OUTPUT OPTIONS");
      System.out.println("  -showItAcc        Show, for each inference iteration, its accuracy.");
      System.out.println("  -showAUC          Show AUCs, one per class.");
      System.out.println("  -saveROC          Save ROC curves after final iteration, one per class.");
      System.out.println("  -showAssort       Show assortativity coefficients.");
      System.out.println("  -output <file>    Where to send output estimates.");
      System.out.println("                      See '-format' for output format");
      System.out.println("                      Default: screen");
      System.out.println("  -saveGraph <prefix>  When using -edgecreator, this option will save the created graphs into");
      System.out.println("                      files starting with the given prefix.  It will reuse the csv file and");
      System.out.println("                      create new .rn and .arff files (prefix+###.rn and prefix+###.arff)");
      System.out.println("  -saveItPredict    Save the predictions after each iteration.");
      System.out.println("                    This is storage intensive, especially for Gibbs Sampling.");
      System.out.println("  -savePajek <prefix> In addition to normal prediction output, save the predictions");
      System.out.println("                    as a Pajek graph to <prefix>-[run#].net.   If '-showItAcc' or '-saveItPredict'");
      System.out.println("                    are turned on, save the inferences as a time graph in pajek format under");
      System.out.println("                    the name <prefix>-[run#]-time.net");
      System.out.println("  -format 'format'  Output result lines using this format");
      System.out.println("                    Variables:");
      System.out.println("                        %ID = node node");
      System.out.println("                        %CLASS = known value from 'truth' file");
      System.out.println("                        %<value-name> = score for node-node attribute = value");
      System.out.println("                             This is case sensitive!");
      System.out.println("                             must match value names in 'prior', 'known' or 'truth' files");
      System.out.println("                      Example: '%ID Good:%Good Bad:%Bad'");
      System.out.println("                      Default: '%ID [%CLASS] <value1-name>:%<value> <value2-name>:%<value> ... <valueK-name>:<value>'");
      System.out.println("                               --- %CLASS is added if -truth is given");
      
      System.out.println();
      System.out.println("EDGE-CREATION/GRAPH-AUGMENTING OPTIONS");
      System.out.println("  -"+EC_PREFIX+"     Add attribute-based edges.  Valid values (comma-separated for multiple values):");
      System.out.println("                      " + ArrayUtil.asString(edgecreators.getValidNames()));
      System.out.println("                      default: none");
      System.out.println("  -weightEdgesByAssort  Reweight edges by their assortativity.");
      System.out.println("  -onlyCatValues    For categorical attributes, only create relations on a per-value basis.");
      System.out.println("                       Cannot be used with 'onlyCatAttrib'");
      System.out.println("  -onlyCatAttrib    For categorical attributes, only create relations for all values.");
      System.out.println("                       Cannot be used with 'onlyCatValues'");
      System.out.println("  -mergeEdges       Combine all new created edges into one generic edge type.");
      System.out.println("  -useTrueAssort    Use truth-based assortativity when weighting newly created edges.");
      System.out.println("  -numET #          Maximum number of edge creators to use.");
      System.out.println("                      default: use all of them");
      System.out.println("  -minET #          Use at least this many new types of edges (as long as assortativity>0).");
      System.out.println("                      default: 0");
      System.out.println("                      THIS OVERRIDES '-maxEdges' and '-minAssort', '-maxInstanceEdges' as needed");
      System.out.println("  -minAssort #      Minimum assortativity for a newly created edge type");
      System.out.println("                      default: 0.05");
      System.out.println("  -maxEdges #      Maximum node degree for new edges (per edge type)");
      System.out.println("                      A value < 0 means no limit.");
      System.out.println("                      default: -1");
      System.out.println("  -maxInstanceEdges #  Maximum node degree for new edges (per edge type) for");
      System.out.println("                      edge creators that are based on instance similarity rather");
      System.out.println("                      than on an attribute.");
      System.out.println("                      A value < 0 means no limit.");
      System.out.println("                      default: whatever the value of -maxEdges is.");
      
      System.out.println();
      System.out.println("ACTIVE LEARNING OPTIONS");
      System.out.println("  -"+AL_PREFIX+"      Select which active learning strategy to use.  Valid values:");
      System.out.println("                      " + ArrayUtil.asString(alstrategies.getValidNames()));
      System.out.println("                      default: no active learning");
    } else {
      System.out.println(msg);
    }
    System.exit(0);
  }
  
  public static void run(String[] argv) {
    NetworkLearning netkit = new NetworkLearning(argv);
    if (netkit.params.get(ParamDB) == null)
      usage("No graph specified!");
    netkit.run();
  }

  /**
   * @param argv
   * @deprecated You should use Netkit.main to access NetworkLearning from now on
   */
  public static void main(String[] argv) {
    Netkit.main(argv);
  }
}
