/**
 * ERMHybrid.java
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
 */
package netkit.classifiers.active;

import netkit.classifiers.*;
import netkit.util.*;
import netkit.graph.Node;
import netkit.classifiers.active.graphfunctions.*;
import netkit.classifiers.relational.Harmonic;
import netkit.classifiers.relational.NetworkClassifier;

import java.util.*;
import java.text.*;

/**
 * This class duses multiple active learning strategies to pick the next candidate(s).
 * Currently, the strategies are hardcoded.  In particular, it picks the top 2.5% from
 * the following strategies:
 * <ul>
 * <li>uncertainty
 * <!--
 * <li>graphcentrality using weighted closeness (global)
 * <li>graphcentrality using closeness (global)
 * -->
 * <li>graphcentrality using weighted betweenness (global)
 * <!--
 * <li>graphcentrality using betweenness (global)
 * <li>graphcentrality using clustering + closeness
 * -->
 * <li>graphcentrality using clustering + weighted closeness
 * <!--
 * <li>graphcentrality using clustering + closeness [size rank]
 * -->
 * <li>graphcentrality using clustering + weighted closeness [size rank]
 * <!--
 * <li>graphcentrality using clustering + closeness [labeldist rank]
 * <li>graphcentrality using clustering + weighted closeness [labeldist rank]
 * <li>graphcentrality using clustering + closeness [erm rank]
 * <li>graphcentrality using clustering + weighted closeness [erm rank]
 * -->
 * </ul>
 * It then uses ERM to rank those candidates.  It currently assumes that it uses the Harmonic
 * function.
 * 
 * @author sofmac
 *
 */
public class ERMHybrid extends PickLabelStrategyImp { 
  
  private static NumberFormat nf;
  static { 
    nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(5);
  }
  private List<PickLabelStrategyImp> strategies = null;
  private double topK = 0.025;
  private double nodesPerCluster = 5;
  private Harmonic harmonic = null;
  private boolean majorityVote = false;
  private boolean cluster = true;
  
  @Override
  public void configure(Configuration config) {
    super.configure(config);
    topK = config.getDouble("topk",0.025);
    nodesPerCluster = config.getDouble("nodespercluster",topK);
    cluster = config.getBoolean("cluster",true);
    majorityVote = config.getBoolean("vote");
    strategies = new ArrayList<PickLabelStrategyImp>();
    strategies.add(new UncertaintyLabeling());
    strategies.add(GraphCentralityLabeling.getInstance(WeightedBetweenness.class.getName(), null, nodesPerCluster));
    
    if(cluster)
      strategies.add(GraphCentralityLabeling.getInstance(ClusterWeightedCloseness.class.getName(), ClusterSizeRank.class.getName(), nodesPerCluster));
  }

  @Override
  public Configuration getDefaultConfiguration() {
    Configuration def = super.getDefaultConfiguration();
    def.set("topk", 0.025);
    def.set("vote", false);
    return def;
  }

  
  private void initializeOthers(int itNum)  {
    for(PickLabelStrategyImp ps : strategies)
    {
      ps.initialize(getNetworkLearner(), getSplit());
      ps.iteration = itNum;
    }

  }


  @Override
  public void initialize(NetworkLearner nl, DataSplit split) {
    super.initialize(nl, split);
    NetworkClassifier nc = nl.getNetworkClassifier();
    if(!(nc instanceof Harmonic))
      throw new IllegalArgumentException("NetworkClassifier must be the Harmonic function!");
    harmonic = (Harmonic)nc;
  }

  /**
   * Maxpicks are ignored.  We ever only look at the top-1 pick from all strategies and only
   * follow the top pick from the 
   */
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    NetKitEnv.logTime("ERMHybrid picking "+maxPicks+"nodes: initialize strategies");
    int iterationNum = getIterationNum();
    initializeOthers(iterationNum);

    logger.info(getName()+": picking top "+maxPicks+" nodes from "+predictions.size()+" candidates, over "+getSplit().getClassDistribution().length+" classes");

    int numToGet = (int)(topK+0.9999999999999999);
    if(topK<1)
      numToGet = (int)(topK * predictions.size())+1;
      
    Map<Node,List<PickLabelStrategy>> nodes = new HashMap<Node,List<PickLabelStrategy>>();
    int maxVote = 0;
    for(PickLabelStrategy strat : strategies)
    {
      NetKitEnv.logTime("ERMHybrid picking "+maxPicks+" nodes: getting top picks from "+strat.getName());
      LabelNode[] picks = strat.getNodesToLabel(getSplit(), predictions, numToGet);
      if(picks == null)
      {
        logger.warning("[iteration-"+iterationNum+"] no "+strat.getName()+" picks!");
        continue;
      }
      NetKitEnv.logTime("ERMHybrid got "+picks.length+" picks from "+strat.getName());
      for(LabelNode n : picks)
      {
        List<PickLabelStrategy> list = nodes.get(n.node);
        if(list==null)
        {
          list = new ArrayList<PickLabelStrategy>();
          nodes.put(n.node,list);
        }
        list.add(strat);
        maxVote = Math.max(maxVote,list.size());
      }
    }
    logger.info(getName()+": got "+nodes.size()+" candidate nodes from the other strategies");
    
    if(majorityVote)
    {
      Map<Node,List<PickLabelStrategy>> allNodes = nodes;
      nodes = new HashMap<Node,List<PickLabelStrategy>>();
      for(Node n : allNodes.keySet())
      {
        List<PickLabelStrategy> list = allNodes.get(n);
        if(list.size() == maxVote)
          nodes.put(n,list);
      }
      logger.info(getName()+": pruned down to "+nodes.size()+" candidate nodes (from "+allNodes.size()+")");
    }
    List<LabelNode> result = new ArrayList<LabelNode>(nodes.size());
    if(nodes.size()==1)
    {
      for(Node n : nodes.keySet())
       result.add(new LabelNode(n,1.0));
    }
    else if(nodes.size()>1)
    {
      NetKitEnv.logTime("ERMHybrid computing ERM scores for "+nodes.size()+" nodes");
      for(Node n : nodes.keySet())
      {
        double score = harmonic.getERM(n);
        result.add(new LabelNode(n,score));
  
        List<PickLabelStrategy> list = nodes.get(n);
        StringBuffer sb = new StringBuffer(getName()).append(": Adding node ").append(n.getName()).append(":").append(score).append(" from ").append(list.size()).append(" strategies:");
        for(PickLabelStrategy strat : list)
          sb.append(strat.getName()).append(" ");
        logger.fine(sb.toString());
      }
      Collections.sort(result);
      if(result.size() > maxPicks)
        result = result.subList(0, maxPicks);
    }
    
    logger.info(getName()+": returning "+result.size()+" nodes.");   
    if(result.size()>0)
    {
      PickLabelStrategy[] strats = nodes.get(result.get(0).node).toArray(new PickLabelStrategy[0]);
      logger.info(getName()+": top node="+result.get(0).node.getName()+" contributed by "+strats.length+" strategies: "+ArrayUtil.asString(strats));
    }
    NetKitEnv.logTime("ERMHybrid returning "+result.size()+" nodes");
    return result.toArray(new LabelNode[0]);
  }

  @Override
  public String getDescription() {    
    return "The ERM hybrid labeler use multiple strategies to pick candidate nodes, "+
           "then ERM to rank those nodes.";
  }

  @Override
  public String getName() {
    return "ERMHybrid";
  }

  @Override
  public String getShortName() {
    return "ERMHybrid";
  }

}
