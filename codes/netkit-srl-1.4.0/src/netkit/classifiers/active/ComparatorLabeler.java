/**
 * ComparatorLabeler.java
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
import netkit.classifiers.active.graphfunctions.*;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 * This class does a comparison between multiple active learning strategies.  Currently,
 * the comparisons are hard-coded to compare against greedy truth.  It then compares against:
 * <ul>
 * <li>ERM
 * <li>uncertainty
 * <li>graphcentrality using weighted closeness (global)
 * <li>graphcentrality using closeness (global)
 * <li>graphcentrality using weighted betweenness (global)
 * <li>graphcentrality using betweenness (global)
 * <li>graphcentrality using clustering + closeness
 * <li>graphcentrality using clustering + closeness [size rank]
 * <li>graphcentrality using clustering + closeness [labeldist rank]
 * <li>graphcentrality using clustering + closeness [erm rank]
 * <li>graphcentrality using clustering + weighted closeness
 * <li>graphcentrality using clustering + weighted closeness [size rank]
 * <li>graphcentrality using clustering + weighted closeness [labeldist rank]
 * <li>graphcentrality using clustering + weighted closeness [erm rank]
 * </ul>
 * Comparison shows:
 * <ol>
 * <li>The rank of the greedy truth node in each of the other strategies (when applicable)
 * <li>The rank of the ERM top node in each of the other strategies (when applicable)
 * <li>The greedy truth rank for the top node in each of the other strategies
 * <li>The ERM rank for the top node in each of the other strategies
 * </ol>
 * 
 * 
 * @author sofmac
 *
 */
public class ComparatorLabeler extends PickLabelStrategyImp { 
  
  private static NumberFormat nf;
  static { 
    nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(5);
  }
  private List<PickLabelStrategyImp> strategies = null;
  GreedyTruth greedy = null;
  EmpiricalRiskMinimizationHarmonic erm = null;
  private double topK = 1;
  private double nodesPerCluster = 1;
  
  @Override
  public void configure(Configuration config) {
    super.configure(config);
    greedy = new GreedyTruth();
    erm = new EmpiricalRiskMinimizationHarmonic();
    topK = config.getDouble("topk",1);
    nodesPerCluster = config.getDouble("nodespercluster",topK);
    strategies = new ArrayList<PickLabelStrategyImp>();
    strategies.add(new UncertaintyLabeling());
    strategies.add(GraphCentralityLabeling.getInstance(Betweenness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(Betweenness.class.getName(), Betweenness.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(Closeness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(WeightedBetweenness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(WeightedBetweenness.class.getName(), WeightedBetweenness.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(WeightedCloseness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterCloseness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterCloseness.class.getName(), LabelWeightedClosenessRank.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterCloseness.class.getName(), ClusterSizeRank.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterCloseness.class.getName(), ERMRank.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterWeightedCloseness.class.getName(), null, nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterWeightedCloseness.class.getName(), LabelWeightedClosenessRank.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterWeightedCloseness.class.getName(), ClusterSizeRank.class.getName(), nodesPerCluster));
    strategies.add(GraphCentralityLabeling.getInstance(ClusterWeightedCloseness.class.getName(), ERMRank.class.getName(), nodesPerCluster));
  }

  private void initializeOthers(int itNum)  {
    erm.initialize(getNetworkLearner(), getSplit());
    erm.iteration = itNum;
    for(PickLabelStrategyImp ps : strategies)
    {
      logger.fine("comparator (iteration-"+itNum+") about to initialize "+ps.getName());
      ps.initialize(getNetworkLearner(), getSplit());
      ps.iteration = itNum;
    }

  }


  @Override
  public void initialize(NetworkLearner nl, DataSplit split) {
    super.initialize(nl, split);
    greedy.initialize(nl, split);
  }

  /**
   * Maxpicks are ignored.  We ever only look at the top-1 pick from all strategies and only
   * follow the top pick from the 
   */
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    int iterationNum = getIterationNum();
    initializeOthers(iterationNum);

    logger.info(getName()+": picking top "+maxPicks+" nodes from "+predictions.size()+" candidates, over "+getSplit().getClassDistribution().length+" classes");

    LabelNode[] mainPicks = greedy.getNodesToLabel(getSplit(), predictions, 1);
    if(mainPicks == null)
    {
      logger.warning("[iteration-"+iterationNum+"] no greedy picks!");
      return null;
    }
    
    LabelNode[] ermPicks = erm.getNodesToLabel(getSplit(), predictions, 1);
    if(ermPicks == null)
    {
      logger.warning("[iteration-"+iterationNum+"] no ERM picks!");
    }

    StringWriter greedySW = new StringWriter();
    PrintWriter greedyPW = new PrintWriter(greedySW,true);
    StringWriter stratSW = new StringWriter();
    PrintWriter stratPW = new PrintWriter(stratSW,true);
    StringWriter ermSW = new StringWriter();
    PrintWriter ermPW = new PrintWriter(ermSW,true);

    double maxERM = 1;
    double maxGreedy = 1;
    double ermRank = ((ermPicks != null) ? erm.getRank(getSplit(), predictions, mainPicks[0].node) : Double.NaN);
    double greedyRank = ((ermPicks != null) ? greedy.getRank(getSplit(), predictions, ermPicks[0].node) : Double.NaN);
    maxERM = Math.max(maxERM, ermRank);
    maxGreedy = Math.max(maxGreedy, greedyRank);
    ermPW.println("[iteration-"+iterationNum+"] ERM top-1 node="+ermPicks[0]);
    ermPW.println("[iteration-"+iterationNum+"] Rank(ERM) of greedy-top1="+ermRank);
    greedyPW.println("[iteration-"+iterationNum+"] greedy top-1 node="+mainPicks[0]);
    greedyPW.println("[iteration-"+iterationNum+"] Rank(greedy) of ERM-top1="+greedyRank);
    
    StringWriter csvSW = new StringWriter();
    PrintWriter csvPW = new PrintWriter(csvSW,true);
    csvPW.println("CSV Table for iteration-"+iterationNum);
    csvPW.println(",,absolute,percent,absolute,percent,absolute,percent,absolute,percent");
    csvPW.println(",,greedy rank,greedy rank,strat rank,strat rank,erm rank,erm rank,strat rank,strat rank");
    csvPW.println("iteration,name,of strat top-1,of strat top-1,of greedy top-1,of greedy top-1,of strat top-1,of strat top-1,of erm top-1,of erm top-1");
    csvPW.print(iterationNum+",greedy,");
    csvPW.print(",,");
    csvPW.print(",,");
    csvPW.print(nf.format(ermRank)+","+nf.format(100*ermRank/predictions.size())+",");
    csvPW.print(nf.format(greedyRank)+","+nf.format(100*greedyRank/predictions.size()));
    csvPW.println();
    csvPW.print(iterationNum+",erm,");
    csvPW.print(nf.format(greedyRank)+","+nf.format(100*greedyRank/predictions.size())+",");
    csvPW.print(nf.format(ermRank)+","+nf.format(100*ermRank/predictions.size())+",");
    csvPW.print(",,");
    csvPW.print(",");
    csvPW.println();

    stratPW.println("[iteration-"+iterationNum+"] strategies:");
    for(PickLabelStrategy strat : strategies)
    {
      LabelNode[] picks = strat.peek(getSplit(), predictions, 1);
      if(picks == null || picks.length == 0)
      {
        logger.warning("[iteration-"+iterationNum+"] no "+strat.getName()+" picks!");
        continue;
      }
      double stratErmRank = ( (ermPicks != null) ? strat.getRank(getSplit(), predictions, ermPicks[0].node) : Double.NaN );
      double stratGreedyRank = strat.getRank(getSplit(), predictions, mainPicks[0].node);
      greedyRank = greedy.getRank(getSplit(), predictions, picks[0].node);
      ermRank = erm.getRank(getSplit(), predictions, picks[0].node);

      maxERM = (Double.isNaN(stratErmRank) ? maxERM : Math.max(maxERM, stratErmRank));
      maxGreedy = (Double.isNaN(stratGreedyRank) ? maxGreedy : Math.max(maxGreedy, stratGreedyRank));
      
      csvPW.print(iterationNum+","+strat.getName()+",");
      csvPW.print(nf.format(greedyRank)+","+nf.format(100*greedyRank/predictions.size())+",");
      csvPW.print(nf.format(stratGreedyRank)+","+nf.format(100*stratGreedyRank/predictions.size())+",");
      csvPW.print(nf.format(ermRank)+","+nf.format(100*ermRank/predictions.size())+",");
      csvPW.print(nf.format(stratErmRank)+","+nf.format(100*stratErmRank/predictions.size()));
      csvPW.println();

      int numPeek = (int)(Math.max(stratErmRank,stratGreedyRank))+1;
      LabelNode[] peek = (numPeek>0 ? strat.peek(getSplit(), predictions, numPeek) : null);
      stratPW.print("[iteration-"+iterationNum+"] "+strat.getName()+" peek-"+numPeek+" nodes:");
      if(peek != null)
        for(LabelNode ln : peek)
          stratPW.print(" "+ln);
      stratPW.println();
      stratPW.println("[iteration-"+iterationNum+"] Rank("+strat.getName()+") of greedy-top1="+stratGreedyRank);
      greedyPW.println("[iteration-"+iterationNum+"] Rank(greedy) of "+strat.getName()+"-top1="+greedyRank);

      stratPW.println("[iteration-"+iterationNum+"] Rank("+strat.getName()+") of ERM-top1="+stratErmRank);
      ermPW.println("[iteration-"+iterationNum+"] Rank(ERM) of "+strat.getName()+"-top1="+ermRank);
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw,true);
    int numPeek = (int)(maxGreedy)+1;
    LabelNode[] peek = (numPeek>0 ? greedy.peek(getSplit(), predictions, numPeek) : null);
    pw.print("[iteration-"+iterationNum+"] greedy peek-"+numPeek+" nodes:");
    if(peek != null)
      for(LabelNode ln : peek)
        pw.print(" "+ln);
    pw.println();
    logger.info(sw.toString()+greedySW.toString());

    if(ermPicks != null)
    {
      sw = new StringWriter();
      pw = new PrintWriter(sw,true);
      numPeek = (int)(maxERM)+1;
      peek = (numPeek>0 ? erm.peek(getSplit(), predictions, numPeek) : null);
      pw.print("[iteration-"+iterationNum+"] erm peek-"+numPeek+" nodes:");
      if(peek != null)
        for(LabelNode ln : peek)
          pw.print(" "+ln);
      pw.println();
    }
    logger.info(sw.toString()+ermSW.toString());    
    logger.info(stratSW.toString());  
    logger.info(csvSW.toString());
    
    return mainPicks;
  }

  @Override
  public String getDescription() {    
    return "The comparator labeler compares multiple strategies, "+
           "following one main strategy and reporting on how close "+
           "the other strategies are to it over time.";
  }

  @Override
  public String getName() {
    return "ComparatorLabeler";
  }

  @Override
  public String getShortName() {
    return "ComparatorLabeler";
  }

}
