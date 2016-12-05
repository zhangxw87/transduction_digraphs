/**
 * GraphCentralityLabeling.java
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
import netkit.classifiers.active.graphfunctions.*;
import netkit.classifiers.relational.Harmonic;
import netkit.graph.*;
import netkit.util.*;
import netkit.util.ModularityClusterer.Cluster;

import java.util.*;
import java.util.logging.Level;

/**
 * Graph Centrality Labeling for Active Learning iteratively picks central nodes in a 
 * graph that are in clusters that have no known labels.   It uses a modularity based
 * clustering algorithm to iteratively cluster a graph into smaller and smaller
 * clusters until it finds a cluster that has no currently known labels.  It then 
 * picks the most central node in that cluster. 
 * <p>
 * Various centrality metric strategies are possible (using the <code>metric</code> configuration variable):
 * <ul>
 * <ul>
 * <li><code>metric</code> --- name of the class (full class name or stem if in the <code>netkit.classifiers.active.graphfunctions</code> package)
 * <li><code>useClustering=[true|false]</code> -- cluster graph and find central nodes in each cluster? 
 * </ul>
 * <p>
 * If clustering is turned on, then various cluster-ranking functions are available (after
 * using the centrality metric to identify the node in the cluster).  The cluster rank
 * function is specified using the <code>clusterrank</code> configuration variable.  By
 * default, the rank is the same as the centrality metric.
 * <ul>
 * <li><code>clusterRank</code> --- name of the class (full class name or stem if in the <code>netkit.classifiers.active.graphfunctions</code> package)
 * <li><code>nodesPerCluster</code> --- keep the topk list in each cluster and return those as well (first topk from first cluster, then iteratively through the next clusters).  default topk=1
 * </ul>  
 * 
 * @author sofmac
 * 
 * @see netkit.graph.ModularityClusterer
 *
 */
public class GraphCentralityLabeling extends PickLabelStrategyImp {

  private GraphMetrics gm = null;
  private Harmonic harmonic = null;
  private String name = "GraphCentralityLabeling";
  private ModularityClusterer mod = null;
  private Graph graph = null;
  private List<CandidateNode> candidateList = null;
  private List<CandidateNode> candidateReuse = null;
  private int bfsLevel = 1;
  private List<Cluster> bfsClusters = null;
  private Classification labels = null;
  private Classification truth = null;
  private boolean cluster = false;
  private ScoringFunction sf = null; // scoring function to identify/rank nodes
  private ScoringFunction cr = null; // cluster ranking function to rank clusters
  private double nodesPerCluster = 1;

  private class CandidateNode extends LabelNode {
    public final Cluster cluster;
    public LabelNode[] topknodes;
    private int num=0;
    private int level=0;

    CandidateNode(Cluster c, int level, LabelNode[] n) {
      super(n[0].node,((c==null)?n[n.length-1].score:cr.score(c,n[n.length-1].node)));
      this.level=level;
      topknodes = n.clone();
      num=topknodes.length;
      cluster = c;
    }

    public Node pickNode() {
      if(num<1)
        return null;
      num--;
      return topknodes[num].node;
    }

    public boolean hasNode() {
      return (num>0);
    }

    public void update(Node[] newPicks) {
      if(num==0)
        return;
      if(num<topknodes.length)
      {
        topknodes = Arrays.copyOfRange(topknodes, 0, num);
        num = topknodes.length;
      }
      if(sf.updateable())
      {
        for(LabelNode n : topknodes)
          n.score = sf.update(cluster, n.score, n.node, newPicks);
        Arrays.sort(topknodes,sf);
      }
      if(cluster == null || cr == sf)
        score = topknodes[num-1].score;
      else if(cr.updateable() && cr != sf)
        score = cr.update(cluster, score, topknodes[num-1].node, newPicks);
    }

    @Override
    public int compareTo(LabelNode node) {
      return cr.compare(this.score, node.score);
    }

    public String toString() {
      return node.getName()+":"+score;
    }
  }
  
  private ScoringFunction getScoringFunction(String clsName) {
    try
    {
      if(!clsName.contains("."))
        clsName = "netkit.classifiers.active.graphfunctions."+clsName;
      @SuppressWarnings("unchecked")
      Class<ScoringFunction> cls = (Class<ScoringFunction>)Class.forName(clsName);
      ScoringFunction instance = cls.newInstance();
      return instance;
    }
    catch(Exception ex)
    {
        logger.log(Level.WARNING,"Failed to instantiate("+name+")["+ex+"]",ex);
        throw new RuntimeException(ex.getMessage(),ex);
    }
  }

  public static GraphCentralityLabeling getInstance(String metric, String rank, double nodesPerCluster){
    GraphCentralityLabeling strat = new GraphCentralityLabeling();
    Configuration conf = new Configuration();
    conf.set("metric", metric);
    conf.set("nodespercluster", nodesPerCluster);
    if(rank!=null)
    {
      conf.set("cluster", true);
      if(rank!=null && !rank.equals(metric))
        conf.set("clusterrank", rank);
    }
    strat.configure(conf);
    return strat;
  }
  
  @Override
  public Configuration getDefaultConfiguration() {
    Configuration def = super.getDefaultConfiguration();
    def.set("metric",WeightedBetweenness.class.getName());
    def.set("cluster",false);
    def.set("nodesPerCluster",1);
    return def;
  }

  @Override
  public void configure(Configuration config) {
    super.configure(config);
    nodesPerCluster = config.getDouble("nodespercluster",1);
    cluster = config.getBoolean("cluster",false);
    String metric = config.get("metric",WeightedBetweenness.class.getName());
    String rank = config.get("clusterrank",null);
        
    sf = getScoringFunction(metric);
    cr = ((rank == null || rank.equals(metric)) ? sf : getScoringFunction(rank));    
    name = "GraphCentralityLabeling-metric_"+sf+"-cluster_"+cluster+"-rank_"+cr;
    
    if(sf.clusterBased() && !cluster)
    {
      cluster = true;
      logger.warning(getName()+": scoringfunction is clusterBased but clustering is off.   clustering turned on.");
    }

    if(rank != null && !cluster)
    {
      logger.warning(getName()+": clusterRank is given but clustering is off.   clustering turned on.");  
      cluster = true;
    }

    logger.config(getName()+": metric="+metric);
    logger.config(getName()+": cluster="+cluster);
    logger.config(getName()+": nodespercluster="+nodesPerCluster);
    logger.config(getName()+": clusterrank="+rank);
  }
  
  public GraphMetrics getMetrics() { return gm; }
  public Classification getLabels() { return labels; }
  public Harmonic getHarmonicFunction() { return harmonic; }

  @Override
  public void initialize(NetworkLearner nl, DataSplit split) {
    super.initialize(nl,split);
    graph = split.getView().getGraph();
    gm = graph.getMetrics();
    truth = split.getView().getTruth();
    labels = truth.clone();
    labels.clear();
    for(Node n : split.getTrainSet())
      labels.set(n, truth.getClassValue(n));

    //printStat("init - state [1]");
    
    if((nl.getNetworkClassifier() instanceof Harmonic))
      harmonic = (Harmonic)nl.getNetworkClassifier();
    
    sf.initialize(this);
    if(cr != null && cr != sf)
      cr.initialize(this);

    candidateList = new ArrayList<CandidateNode>(split.getTestSetSize());
    candidateReuse = new ArrayList<CandidateNode>();
    bfsClusters = new ArrayList<Cluster>();

    //printStat("init - state [2]");

    if(cluster)
    {
      mod = new ModularityClusterer(graph);
      mod.startClustering();
      //printStat("init - state [3a]");
      bfsLevel=1;

      if(!(cr instanceof ERMRank))
      {
        for(Cluster c : mod.getIsolatedClusters())
          addCluster(c,0);
        for(Cluster c : mod.getConnectedClusters())
          addCluster(c,0);
      }
      //printStat("init - state [3b]");
    }
    else
    {
      for(Node n : split.getTestSet())
      {
        LabelNode ln = new LabelNode(n,sf.score(null, n));
        candidateList.add(new CandidateNode(null, -1, new LabelNode[]{ln}));
      }
      //printStat("init - state [3c]");
    }
    Collections.sort(candidateList);
      
    logger.fine(getName()+"::intitialize() - Created candidateList of "+candidateList.size()+" nodes.  MaxCentrality="+(candidateList.isEmpty()?Double.NaN:candidateList.get(candidateList.size()-1).score));
    if(logger.isLoggable(java.util.logging.Level.FINER))
    {
      int rank = 0;
      for(CandidateNode cn : candidateList)
      {
        rank++;
        double r = 1;
        List<LabelNode> ln = new ArrayList<LabelNode>();
        if(cn.cluster == null)
        {
          ln.add(cn);
        }
        else
        {
          if(logger.isLoggable(java.util.logging.Level.FINEST))
          {
            for(Node n : cn.cluster)
              ln.add(new LabelNode(n,sf.score(cn.cluster, n)));
            Collections.sort(ln);
          }
          else
          {
            for(LabelNode n : cn.topknodes)
              ln.add(n);
          }
        }
        for(LabelNode lbln : ln)
        {
          if(ln.size()>1)
          {
            logger.finer(getName()+" Rank-"+(rank+(r/100))+": "+lbln);
            r++;
          }
          else
            logger.finer(getName()+" Rank-"+rank+": "+lbln);
          
        }
      }
    }
    logger.finer(getName()+" Done with initialize.");
  }

  @Override
  public String getDescription() {    
    return "Graph Centrality Labeling for Active Learning iteratively picks central nodes in a graph that are in clusters that have no known labels.  It uses a modularity based clustering algorithm to iteratively cluster a graph into smaller and smaller clusters until it finds a cluster that has no currently known labels.  It then picks the most central node in that cluster.";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getShortName() {
    return name;
  }

  private void buildERMcandidates() {
    if(!cluster || !(cr instanceof ERMRank))
      return;

    if(!candidateList.isEmpty())
      return;
    
    for(Cluster c : mod.getIsolatedClusters())
      addCluster(c,0);
    for(Cluster c : mod.getConnectedClusters())
      addCluster(c,0);
    logger.fine(getName()+"::peek() - ermclustering created candidateHeap of "+candidateList.size()+" nodes.  MaxCentrality="+((candidateList.size()>0)?candidateList.get(candidateList.size()-1).score: Double.NaN));
    if(logger.isLoggable(java.util.logging.Level.FINEST))
    {
      int rank = 0;
      for(CandidateNode cn : candidateList)
      {
        rank++;
        logger.finest(" Rank-"+rank+": "+cn.node.getName()+":"+cn.score);
      }
    }
  }
  
  private boolean isZeroKnowledgeCluster(Cluster c) {
    for(Node n : c)
      if(!labels.isUnknown(n))
        return false;
    return true;
  }

  private CandidateNode getClusterCentralityCandidate(Cluster c, int level) {
    // find central node in cluster
    if(nodesPerCluster==1)
    {
      Node minN = null;
      double minC = Double.NaN;
      for(Node src : c)
      {
        // we want the 'biggest' candidate based on the scoring function  
        double dist = sf.score(c, src);
        if(Double.isNaN(minC) || sf.compare(dist,minC)<0)
        {
          minN = src;
          minC = dist;
        }
      }
      if(minN==null)
      {
        logger.warning(getName()+" getClusterCentralityCandidate() - found no central cluster node?!");
        return null;
      }

      // now that we've found the node, use the cluster ranker to rank the node
      LabelNode ln = new LabelNode(minN,minC);
      return new CandidateNode(c,level,new LabelNode[]{ln});
    }
    else
    {
      int numToGet = (int)(nodesPerCluster+0.9999999999999999);
      if(nodesPerCluster<1)
        numToGet = (int)(nodesPerCluster * getSplit().getTestSetSize())+1;

      List<CandidateNode> nodes = new ArrayList<CandidateNode>(c.getSize()); 
      for(Node src : c)
      {
        // we want the smallest candidate based on the scoring function  
        LabelNode ln = new LabelNode(src, sf.score(c, src));
        nodes.add(new CandidateNode(c,level,new LabelNode[]{ln}));
      }
      Collections.sort(nodes);
      if(nodes.size() > numToGet)
        nodes = nodes.subList(0,numToGet);
      LabelNode[] list = nodes.toArray(new LabelNode[0]);
      return new CandidateNode(c,level,list);      
    }
  }

  private void addCluster(Cluster c, int level) {
    if(c==null)
      return;

    if(level >= bfsLevel)
    {
      logger.finer(getName()+" addCluster(): adding cluster to bfsClusters at level="+level);
      bfsClusters.add(c);
    }
    else if(isZeroKnowledgeCluster(c))
    {
      CandidateNode cc = getClusterCentralityCandidate(c,level); 
      logger.finest(getName()+" addCluster(): got candidate node at level="+level+" cc="+cc);
      if(cc!=null)
      {
        logger.finer(getName()+"::addCluster() --- Adding cluster with maxNode="+cc.node.getName()+" centrality="+cc.score+" size="+cc.cluster.getSize());
        candidateReuse.add(cc);
      }
    }
    else if(c.getNode()==null)
    {
      logger.finer(getName()+" addCluster(): adding child clusters at level="+level);
      addCluster(c.getChildCluster1(),level+1);
      addCluster(c.getChildCluster2(),level+1);
    }
  }

  private CandidateNode pickNextNode() {
    if(candidateList.isEmpty())
    {
      List<CandidateNode> tmp = candidateList;
      candidateList = candidateReuse;
      candidateReuse = tmp;
      Collections.sort(candidateList);
    }
    
    while(candidateList.isEmpty() && !bfsClusters.isEmpty())
    {
      logger.finest(getName()+" pickNextNode() - adding new clusters from bfsClusters (level="+bfsLevel+")");
      bfsLevel++;
      
      List<Cluster> set = bfsClusters;
      bfsClusters = new ArrayList<Cluster>();
      for(Cluster c : set)
        addCluster(c, bfsLevel-1);
      
      List<CandidateNode> tmp = candidateList;
      candidateList = candidateReuse;
      candidateReuse = tmp;
      Collections.sort(candidateList);
    }
    
    if(candidateList.isEmpty())
    {
      logger.finer(getName()+" pickNextNode(): candidateList is empty");
      return null;
    }
    
    CandidateNode cc = candidateList.remove(candidateList.size()-1);
    if(cc==null)
    {
      logger.severe(getName()+" pickNextNode(): candidateList returned null?!");
      return null;
    }

    logger.finest(getName()+" pickNextNode() - cc="+cc+" hasNode="+cc.hasNode());
    Node node = cc.pickNode();
    if(node == null)
    {
      logger.warning(getName()+" pickNextNode(): candidate node has no actual node available?");
      return null;
    }
    
    logger.finer(getName()+" pickNextNode() - node="+node.getName()+" cc="+cc+" hasNode="+cc.hasNode());
    if(cc.hasNode())
      candidateReuse.add(cc);
    else if(cc.cluster != null)
    {
      if(cc.cluster.getNode()==null)
      {
        addCluster(cc.cluster.getChildCluster1(),cc.level+1);
        addCluster(cc.cluster.getChildCluster2(),cc.level+1);
      }
    }
      
    labels.set(node, truth.getClassValue(node));
    logger.fine(getName()+" pickNextNode() --- Adding node="+node.getName()+" centrality="+cc.score+" size="+((cc.cluster==null) ? -1 : cc.cluster.getSize())); 

    return cc;
  }

  protected void printStat(String title)
  {
    System.out.println(title);
    System.out.println(" gm = "+gm);
    System.out.println(" harmonic = "+harmonic);
    System.out.println(" mod = "+mod);
    System.out.println(" graph = "+graph);
    System.out.println(" bsfClusters = "+bfsClusters);
    System.out.println(" bfsLevel = "+bfsLevel);
    System.out.println(" candidateList = "+candidateList);
    System.out.println(" candidateReuse = "+candidateReuse);
    System.out.println(" labels = "+labels);
    System.out.println(" truth = "+truth);
    System.out.println(" scoringfunction = "+sf);
    System.out.println(" clusterrank = "+cr);
    System.out.println(" nodesPerCluster = "+nodesPerCluster);
    System.out.println(" useClustering = "+cluster);
    System.out.println(" sf = "+sf);
    System.out.println(" cr = "+cr);

  }
  
  @Override
  public double getRank(DataSplit split, Estimate predictions, Node node) {
    logger.fine(getName()+" getRank("+node.getName()+")");
    
    buildERMcandidates();

    // This is somewhat of a hack for clustering... what if the node is not amongst the central
    // nodes in the current clusters?  The answer does not seem clear, so we do something
    // slightly different:
    // 1) find the cluster rank where the node is in
    // 2) find the rank using the centrality metric
    // return a double of the value KK.NN, where KK=cluster rank and NN is the fractional
    // representation of the node's rank in that cluster (up to 99).
    // e.g., if the node is in the third ranked cluster and is the fifth element to be picked
    // in that cluster, then the rank would be 3.05

    if(cluster && nodesPerCluster != 1)
    {
      int r=0;
      CandidateNode found = null;
      for(CandidateNode cn : candidateList)
      {
        logger.fine(getName()+" getRank("+node.getName()+") - checking cluster "+(r+1)+" (out of "+candidateList.size()+")");
        for(Node n : cn.cluster)
        {
          logger.finest(getName()+" getRank("+node.getName()+") - checking node "+n.getName()+" (out of "+cn.cluster.getSize()+")");
          if(n==node)
          {
            found = cn;
            logger.fine(getName()+" getRank("+node.getName()+") - node found in cluster "+(r+1));
            break;
          }
        }
        r++;
        if(found!=null)
          break;
      }
      if(found==null)
        return Double.NaN;
      List<LabelNode> ln = new ArrayList<LabelNode>();
      LabelNode target = null;
      for(Node n : found.cluster)
      {
        // we want the smallest candidate based on the scoring function  
        double dist = sf.score(found.cluster, n);
        LabelNode lbl = new LabelNode(n,dist);
        if(node == n)
          target = lbl;
        ln.add(lbl);
      }
      
      double rank = getAverageRank(ln,target);
      if(rank>=100) rank=99.9;
      return r+(rank/100.0D);
    }
    
    int r = 0;
    for(CandidateNode cn : candidateList)
    {
      r++;
      Node n = cn.node;
      logger.finer(getName()+" getRank("+node.getName()+") - checking node "+n.getName()+" (rank="+r+") match="+(n==node));
      if(n==node)
        return r;
    }
    logger.finer(getName()+" getRank("+node.getName()+") - node not found");
    return Double.NaN;
  }

  @Override
  public LabelNode[] peek(DataSplit split, Estimate predictions, int maxPicks) {
    if(maxPicks==0)
    {
      logger.warning(getName()+" peek(): asked to return 0 picks?!");
      return new LabelNode[0];
    }
    
    List<CandidateNode> oldCList = new ArrayList<CandidateNode>(candidateList);
    List<CandidateNode> oldRList = new ArrayList<CandidateNode>(candidateReuse);
    List<Cluster> oldCluster = new ArrayList<Cluster>(bfsClusters);
    int oldLevel = bfsLevel;
    
    if(graph == null)
      throw new IllegalStateException(getName()+" peek(): has not yet been initialized!");

    buildERMcandidates();

    // printStat("iteration-"+this.getIterationNum()+" peek - state [1]");

    // save current state
    Map<CandidateNode,Integer> cnums = new HashMap<CandidateNode,Integer>();
    for(CandidateNode cn : candidateList)
      cnums.put(cn, cn.num);
    for(CandidateNode cn : candidateReuse)
      cnums.put(cn, cn.num);

    //printStat("iteration-"+this.getIterationNum()+" peek - state [2]");

    List<LabelNode> resultList = new ArrayList<LabelNode>();
    for(int i=0;i<maxPicks;i++)
    {
      LabelNode ln = pickNextNode();
      if(ln == null)
        break;
      logger.finest(getName()+" peek() - adding node: "+ln);
      resultList.add(ln);
    }
    Collections.sort(resultList);

    //printStat("iteration-"+this.getIterationNum()+" peek - state [3]");

    int r=0;
    for(LabelNode ln : resultList)
    {
      r++;
      logger.finest("  Rank-"+r+": "+ln.node.getName()+" "+ln.score);
    }
    //printStat("iteration-"+this.getIterationNum()+" peek - state [4]");

    logger.fine(getName()+" peek() returning "+resultList.size()+" nodes");
    
    // reset state
    candidateList = oldCList;
    candidateReuse = oldRList;
    bfsClusters = oldCluster;
    bfsLevel = oldLevel;
    for(CandidateNode cn : candidateList)
      cn.num = cnums.get(cn);
    for(CandidateNode cn : candidateReuse)
      cn.num = cnums.get(cn);
    
    return resultList.toArray(new LabelNode[0]);
  }

  private void update(List<Node> newPicks)
  {
    if(sf.updateable() || (cluster && cr.updateable()))
    {
      for(CandidateNode cn : candidateList)
        cn.update(newPicks.toArray(new Node[0]));
      Collections.sort(candidateList);
    }
  }

  /**
   * Picks the next nodes as the ones with the highest closeness centrality (normalized
   * by cluster size) in a cluster that has no known labels. 
   * As it chooses new labels, it will split those clusters into two clusters: one
   * with a labeled example and one with no labeled examples.
   */
  @Override
  protected LabelNode[] pickNodes(Estimate predictions, int maxPicks) {
    if(graph == null)
      throw new IllegalStateException(getName()+" has not yet been initialized!");

    //printStat("iteration-"+this.getIterationNum()+" pickNodes - state [1]");
    buildERMcandidates();

    //printStat("iteration-"+this.getIterationNum()+" pickNodes - state [2]");

    List<LabelNode> picks = new ArrayList<LabelNode>();
    List<Node> newPicks = new ArrayList<Node>();
    while(picks.size()<maxPicks)
    {
      CandidateNode pick = pickNextNode();
      if(pick==null)
        break;
      picks.add(new LabelNode(pick.node,pick.score));
      newPicks.add(pick.node);
    }
    update(newPicks);
    logger.fine(getName()+" pickNodes() returning "+picks.size()+" nodes");

    int r=0;
    for(LabelNode ln : picks)
    {
      r++;
      logger.finest("  Rank-"+r+": "+ln.node.getName()+" "+ln.score);
    }
    return picks.toArray(new LabelNode[0]);
  }
  
}
