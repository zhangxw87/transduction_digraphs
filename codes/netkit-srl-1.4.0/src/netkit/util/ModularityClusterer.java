package netkit.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import netkit.graph.Edge;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.graph.io.SchemaReader;

import com.fetch.common.structures.ExposedLinkedList;
import com.fetch.common.structures.IndexedMaxHeap;
import com.fetch.common.structures.TinyIdentityMap;


/**
 * 
 * Modularity-based clusterer, extended to work with weights, multiple edges,
 * and directed edges.
 * 
 * Self-loops are ignored.  All edges are directed.  For bidirectional edges,
 * use two opposite edges with equal weights.
 * 
 * @see A. Clauset, M. Newman and C. Moore, "Finding community structure in
 *      very large networks." 
 * 
 * @author Evan Gamble (egamble@fetch.com)
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class ModularityClusterer {
  private static final boolean DEBUG = false;
  private static final double initPct = 0.095;
  private static final double clusterPct = 0.9;
  private static final double summaryPct = 0.005;
  
  private final Logger logger = NetKitEnv.getLogger(this);
  
  // max heap containing the max ClusterEdge from each cluster
  private IndexedMaxHeap<ClusterEdge> majorMaxHeap=null;

  // clusters with no edges connecting to other clusters
  private Set<Cluster> isolatedClusters=null;
  private List<Set<Node>> isolatedClusterNodeSets=null;

  // clusters with edges connecting to other clusters
  private Set<Cluster> connectedClusters=null;
  private List<Set<Node>> connectedClusterNodeSets=null;

  private int nextClusterId = 0;

  private int numConnectedClusters = 0;
  private int numIsolatedClusters = 0;
  private int numClusterNodes = 0;
  private int numSingletons = 0;
  
  private double pctDone = 0D;
  
  private final Graph graph;
  
  private boolean stop = false;
  private boolean active = false;
  private final Object lock = new Object();

  public ModularityClusterer(Graph g) {
    this.graph = g;
  }
  
  public void startClustering() {
    synchronized(lock) {
      if(active)
        throw new IllegalStateException("ModularityClusterer is already active!");
      active = true;
    }
    
    if(DEBUG) System.out.println("ModularityClustering startClustering [pctDone="+pctDone+"]");
    pctDone = 0;
    majorMaxHeap=null;
    isolatedClusters=null;
    isolatedClusterNodeSets=null;
    connectedClusters=null;
    connectedClusterNodeSets=null;
    nextClusterId = 0;
    numConnectedClusters = 0;
    numIsolatedClusters = 0;
    numClusterNodes = 0;
    numSingletons = 0;
    stop = false;

    cluster();
    if(!stop)
      summarize();
    
    if(!stop)
      pctDone = 1.0D;
    
    active = false;
    if(stop)
      if(DEBUG) System.out.println("ModularityClusterer stopped at "+pctDone+"%");
  }
  
  public Set<Cluster> getIsolatedClusters() { return isolatedClusters; }
  public List<Set<Node>> getIsolatedClusterNodeSets() { return isolatedClusterNodeSets; }
  public Set<Cluster> getConnectedClusters() { return connectedClusters; }
  public List<Set<Node>> getConnectedClusterNodeSets() { return connectedClusterNodeSets; }
  public int getNumConnectedClusters() { return numConnectedClusters; }
  public int getNumIsolatedClusters() { return numIsolatedClusters; }
  public int getNumClusterNodes() { return numClusterNodes; }
  public int getNumSingletons() { return numSingletons; }
  public Graph getGraph() { return graph; }
  public double percentDone() { return pctDone; }
  public boolean isActive() { return active; }
  public void stop() { stop = true; }

  private void cluster() {
    // pctDone is initPct after initialize()
    double Q = initialize();
    double max_dQ = 0.0;
    
    if(stop)
      return;
    
    double initial_dQ = majorMaxHeap.max().dQ;
    //double prior_dQ = initial_dQ;
    int numIt = 0;
    double maxIt = graph.numNodes();
    if(DEBUG) System.out.println("["+(DateFormat.getDateTimeInstance().format(new Date()))+"] ModularityClustering starting primary clustering loop [Q="+Q+" init_dQ="+initial_dQ+"]");
    while (!majorMaxHeap.isEmpty()) {
      // Sofus added for threading purposes
      if(stop) {
        return;
      }
      
      ClusterEdge max = majorMaxHeap.max();
      max_dQ = max.dQ;
      ++numIt;
      
      // this is a crude way of estimating percent done:
      pctDone = initPct + clusterPct * ((double)numIt/maxIt);
      if(DEBUG) if((((numIt>1000)? numIt % 1000 : numIt % 100)==0)) System.out.println("["+(DateFormat.getDateTimeInstance().format(new Date()))+"] ModularityClustering iteration "+numIt+" - dQ="+max_dQ+" pctDone="+pctDone);
      
      // this is one very bad way of estimating percent done
//      if(numIt==500) initial_dQ = max_dQ;
//      double delta = (numIt > 500 ? (initial_dQ-max_dQ)/initial_dQ : (double)numIt/(double)10000);
//      pctDone = initPct + clusterPct * delta;
//      // pctDone = initPct + clusterPct * (1D-(prior_dQ-max_dQ)/prior_dQ);
//      if(DEBUG && (((numIt>1000)? numIt % 1000 : numIt % 100)==0)) System.out.println("["+(DateFormat.getDateTimeInstance().format(new Date()))+"] ModularityClustering iteration "+numIt+" - dQ="+max_dQ+" pctDone="+pctDone+" delta="+delta);

      if (max_dQ <= 0.0) break;
      Q += max_dQ;
      new Cluster();      
    }
    
    // we're initPct+clusterPct done after clustering ... last is for summary
    pctDone = initPct+clusterPct;
  }
  
  private void summarize()
  {
    connectedClusters = new HashSet<Cluster>();

    if(DEBUG) System.out.println("ModularityClustering starting primary summarize loop");
    
    for (ClusterEdge edge : majorMaxHeap) {
      // Sofus added for threading purposes
      if(stop) {
        return;
      }
      
      Cluster c1 = edge.e1.cluster;
      Cluster c2 = edge.e2.cluster;
      connectedClusters.add(c1);
      connectedClusters.add(c2);
    }

    numConnectedClusters = 0;
    numIsolatedClusters = 0;
    numClusterNodes = 0;
    numSingletons = 0;

    connectedClusterNodeSets = new ArrayList<Set<Node>>();

    // we are going to add the last 1% to pctDone here
    double delta = summaryPct/(double)graph.numNodes();
    for (Cluster c : connectedClusters) {
      // Sofus added for threading purposes
      if(stop) {
        return;
      }

      addNodeCluster(connectedClusterNodeSets, c);
      pctDone += c.size*delta;
      numConnectedClusters++;
    }
    
    isolatedClusterNodeSets = new ArrayList<Set<Node>>();

    for (Cluster c : isolatedClusters) {
      // Sofus added for threading purposes
      if(stop) {
        return;
      }

      addNodeCluster(isolatedClusterNodeSets, c);
      pctDone += c.size*delta;
      numIsolatedClusters++;
    }

    majorMaxHeap = null;

    pctDone = 1D;
  }

  private void addNodeCluster(List<Set<Node>> clusters, Cluster c) {
    Set<Node> nodeCluster = new HashSet<Node>();

    buildNodeCluster(nodeCluster, c);
    int size = c.size;

    clusters.add(nodeCluster);

    numClusterNodes += size;
    if (size == 1) numSingletons++;
  }

  // use this alternative buildNodeCluster if deep recursion is a problem
  public static void buildNodeCluster(Set<Node> nodeCluster, Cluster c) {
    Queue<Cluster> queue = new LinkedList<Cluster>();
    queue.add(c);

    Cluster next;
    while ((next = queue.poll()) != null) {
      if (next.node == null) {
        queue.add(next.child1);
        queue.add(next.child2);
      } else {
        nodeCluster.add(next.node);
      }
    }
  }


  private double initialize() {
    double Q = 0.0;
    
    logger.fine("initialize() Get number of nodes");

    Node[] nodes = graph.getNodes();
    int numNodes = nodes.length;

    Edge[] edges = graph.getEdges();

    Map<Node, Cluster> mapNodeCluster = new HashMap<Node, Cluster>(numNodes);

    if(DEBUG) System.out.println("ModularityClustering initialize starting first loop");

    // we have a total of 3*N + E + N*E steps, which add up to initPct at the end
    double delta = initPct/(double)(numNodes*3D + (double)edges.length + (double)edges.length*(double)numNodes);
    
    logger.fine("initialize() Add one leaf node per node");
    // first N steps (total = N)
    for (Node n : nodes) {
      pctDone += delta;

      // Sofus added for threading purposes
      if(stop) {
        return -1;
      }

      Cluster c = new Cluster(n); // initially each node is in its own singleton cluster
      mapNodeCluster.put(n, c); // mapping each node to its own cluster
    }


    double m = 0.0;

    if(DEBUG) System.out.println("ModularityClustering initialize starting second loop - "+percentDone()+"% done");
    logger.fine("initialize() count up edge weights");
    // E steps (total = N +E)
    for (Edge e : edges) {
      pctDone += delta;

      // Sofus added for threading purposes
      if(stop) {
        return -1;
      }

      Node n1 = e.getSource();
      Node n2 = e.getDest();

      // self-loops are not allowed
      if (n1 == n2) continue;

      Cluster c1 = mapNodeCluster.get(n1);
      Cluster c2 = mapNodeCluster.get(n2);
      
      double weight = e.getWeight();
      c1.weight_out += weight;
      c2.weight_in += weight;
      m += weight;
    }
    
    double mInv = 1.0 / m;

    logger.fine("initialize() set initial Q values");
    
    if(DEBUG) System.out.println("ModularityClustering initialize starting third loop - "+percentDone()+"% done");
    // N steps (total = 2*N + E)
    for (Cluster c : mapNodeCluster.values()) {
      pctDone += delta;

      // Sofus added for threading purposes
      if(stop) {
        return -1;
      }

      c.a_in = c.weight_in / m;
      c.a_out = c.weight_out / m;
      Q -= c.a_in * c.a_out;
    }
    
    if(DEBUG) System.out.println("ModularityClustering initialize starting fourth loop - "+percentDone()+"% done");
    logger.fine("initialize() initialize cluster edges");
    // NE steps (total = 2N + NE + E)
    int numIt = 0;
    double ititDelta = (2D*numNodes*delta)/((double)edges.length-1D);
    // double itDelta = delta*(double)numNodes;
    double itDelta =0;

    for (Edge e : edges) {
      itDelta += ititDelta;
      pctDone += itDelta;
      if(DEBUG) if((++numIt % 5000) == 0) System.out.println("ModularityClustering fourth loop  it="+numIt+" - "+percentDone()+"% done");

      // Sofus added for threading purposes
      if(stop) {
        return -1;
      }

      Node n1 = e.getSource();
      Node n2 = e.getDest();

      // self-loops are not allowed
      if (n1 == n2) continue;

      Cluster c1 = mapNodeCluster.get(n1);
      Cluster c2 = mapNodeCluster.get(n2);

      double weight = e.getWeight();

      boolean isDuplicateEdge = false;
      for (ClusterEdgeEnd end1 : c1.edgeEnds) {
        // Sofus added for threading purposes
        if(stop) {
          return -1;
        }

        if (end1.getOppositeCluster() == c2) {
          end1.weight_out += weight;
          end1.dQ_out = end1.weight_out * mInv - c1.a_out * c2.a_in;
          
          ClusterEdgeEnd end2 = end1.getOppositeEdgeEnd();
          end1.edge.dQ = end1.dQ_out + end2.dQ_out;
          
          isDuplicateEdge = true;
          break;
        }
      }
      if (isDuplicateEdge) continue; // no further initialization for duplicate edges

      ClusterEdgeEnd end1 = new ClusterEdgeEnd(c1, 1);
      ClusterEdgeEnd end2 = new ClusterEdgeEnd(c2, 2);

      end1.weight_out = weight;
      end1.dQ_out = weight * mInv - c1.a_out * c2.a_in;
      end2.dQ_out =               - c2.a_out * c1.a_in;
      ClusterEdge ce = new ClusterEdge(end1, end2, end1.dQ_out + end2.dQ_out);
      
      end1.edge = ce;
      end2.edge = ce;

      c1.insertEnd(end1);
      c2.insertEnd(end2);
    }

    logger.fine("initialize() add clusters to maxHeap");
   
    majorMaxHeap = new IndexedMaxHeap<ClusterEdge>(edgeComparator, numNodes);
    isolatedClusters = new HashSet<Cluster>();

    if(DEBUG) System.out.println("ModularityClustering initialize starting fifth loop - "+percentDone()+"% done");
    // last N steps (total = 3*N + E + N*E)
    for (Cluster c : mapNodeCluster.values()) {
      pctDone += delta;

      // Sofus added for threading purposes
      if(stop) {
        return -1;
      }

      ClusterEdge max = c.minorMaxHeap.max();
      if (max == null) {
        isolatedClusters.add(c);
        continue;
      }
      majorMaxHeap.insert(max);
    }
    
    logger.fine("initialize() return Q");
    if(DEBUG) System.out.println("ModularityClustering initialize done - Q = "+Q+" pctDone="+pctDone);

    pctDone = initPct;
    return Q;
  }

  private static Comparator<ClusterEdgeEnd> edgeEndComparator = new Comparator<ClusterEdgeEnd>() {
    public int compare(ClusterEdgeEnd e1, ClusterEdgeEnd e2) {
      return e1.getOppositeCluster().id - e2.getOppositeCluster().id;
    }
  };

  private static Comparator<ClusterEdge> edgeComparator = new Comparator<ClusterEdge>() {
    public int compare(ClusterEdge e1, ClusterEdge e2) {
      return Double.compare(e1.dQ, e2.dQ);
    }    
  };


  public class Cluster implements Iterable<Node> {
    
    private class ClusterNodeIterator implements Iterator<Node> {
      private Queue<Cluster> queue = new LinkedList<Cluster>();
      private Cluster next = null;
      ClusterNodeIterator(Cluster c)
      {
        next = c;
      }

      @Override
      public boolean hasNext() {
        return (next!=null);
      }

      @Override
      public Node next() {
        Node node = null;
        for(node = next.getNode(); node==null; node = next.getNode())
        {
          queue.add(next.getChildCluster1());
          queue.add(next.getChildCluster2());
          next = queue.poll();
        }
        next = queue.poll();        
        return node;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

    
    private final int id = nextClusterId++;

    // number of nodes in the cluster
    private final int size;

    // node if this cluster is a leaf in the dendrogram
    private final Node node;

    // children of this cluster in the dendrogram
    private final Cluster child1;
    private final Cluster child2;

    // weights are only used during initialization
    private double weight_in = 0.0; // weight of all edges coming in to this (singleton) cluster, including duplicate edges but not self-loops
    private double weight_out = 0.0; // weight of all edges going out of this (singleton) cluster, including duplicate edges but not self-loops
    
    private double a_in;
    private double a_out;
    
    private ExposedLinkedList<ClusterEdgeEnd> edgeEnds = new ExposedLinkedList<ClusterEdgeEnd>(edgeEndComparator);
    private IndexedMaxHeap<ClusterEdge> minorMaxHeap;

    // this constructor creates a singleton cluster (a leaf in the dendrogram)
    private Cluster(Node node) {
      this.node = node;
      child1 = child2 = null;
      size = 1;

      // Because netkit edges are directed, node.numEdges() counts only the outgoing edges.
      // If reverse edges are not present, node.numEdges() may not provide enough capacity, but IndexedMaxHeap expands dynamically.
      // The capacity unnecessarily includes self-loops.
      minorMaxHeap = new IndexedMaxHeap<ClusterEdge>(edgeComparator, node.numEdges());
    }

    
    // this constructor creates a new cluster by merging the two clusters from the max cluster edge in majorMaxHeap
    private Cluster() {
      node = null;
      ClusterEdge majorMax = majorMaxHeap.removeMax();

      child1 = majorMax.e1.cluster;
      child2 = majorMax.e2.cluster;

      size = child1.size + child2.size;

      a_in = child1.a_in + child2.a_in;
      a_out = child1.a_out + child2.a_out;

      minorMaxHeap = new IndexedMaxHeap<ClusterEdge>(edgeComparator, child1.minorMaxHeap.size() + child2.minorMaxHeap.size());
      
      mergeEdges(majorMax);

      child1.clear();
      child2.clear();

      if (minorMaxHeap.isEmpty()) {
        isolatedClusters.add(this);
      } else {
        majorMaxHeap.insert(minorMaxHeap.max());
      }
    }
    
    public Iterator<Node> iterator() {
      return new ClusterNodeIterator(this);
    }
    
    public Node getNode() {
      return node;
    }

    public int getSize() {
      return size;
    }

    public Cluster getChildCluster1() {
      return child1;
    }

    public Cluster getChildCluster2() {
      return child2;
    }

    public int getId() {
      return id;
    }

    private void mergeEdges(ClusterEdge excludeEdge) {
      ClusterEdgeEnd edgeEnd1 = skipExcludeEdge(child1.edgeEnds.first, excludeEdge);
      ClusterEdgeEnd edgeEnd2 = skipExcludeEdge(child2.edgeEnds.first, excludeEdge);

      while (edgeEnd1 != null || edgeEnd2 != null) {
        int comparison;

        // Sofus added for threading purposes
        if(stop)
          return;

        if (edgeEnd1 == null) {
          comparison = +1;
        } else if (edgeEnd2 == null) {
          comparison = -1;
        } else {
          comparison = edgeEndComparator.compare(edgeEnd1, edgeEnd2);
        }

        Cluster opposite;
        double dQ;

        if (comparison < 0) {
          opposite = edgeEnd1.getOppositeCluster();

          majorMaxHeap.remove(edgeEnd1.edge);

          opposite.removeEndUpdateMax(edgeEnd1.getOppositeEdgeEnd());

          dQ = edgeEnd1.edge.dQ - opposite.a_in * child2.a_out - opposite.a_out * child2.a_in;
          newEdge(opposite, dQ);

          edgeEnd1 = skipExcludeEdge(edgeEnd1.next, excludeEdge);

        } else if (comparison > 0) {
          opposite = edgeEnd2.getOppositeCluster();

          majorMaxHeap.remove(edgeEnd2.edge);

          opposite.removeEndUpdateMax(edgeEnd2.getOppositeEdgeEnd());

          dQ = edgeEnd2.edge.dQ - opposite.a_in * child1.a_out - opposite.a_out * child1.a_in;
          newEdge(opposite, dQ);

          edgeEnd2 = skipExcludeEdge(edgeEnd2.next, excludeEdge);

        } else { // comparison == 0
          opposite = edgeEnd1.getOppositeCluster();

          majorMaxHeap.remove(edgeEnd1.edge);
          majorMaxHeap.remove(edgeEnd2.edge);

          opposite.removeEndUpdateMax(edgeEnd1.getOppositeEdgeEnd());
          opposite.removeEndUpdateMax(edgeEnd2.getOppositeEdgeEnd());

          dQ = edgeEnd1.edge.dQ + edgeEnd2.edge.dQ;
          newEdge(opposite, dQ);

          edgeEnd1 = skipExcludeEdge(edgeEnd1.next, excludeEdge);
          edgeEnd2 = skipExcludeEdge(edgeEnd2.next, excludeEdge);
        }
      }
    }

    private void newEdge(Cluster opposite, double dQ) {
      ClusterEdgeEnd e1 = new ClusterEdgeEnd(this, 1);
      ClusterEdgeEnd e2 = new ClusterEdgeEnd(opposite, 2);

      ClusterEdge edge = new ClusterEdge(e1, e2, dQ);

      e1.edge = edge;
      e2.edge = edge;

      appendEnd(e1);
      opposite.appendEndUpdateMax(e2);  // we can append to the opposite edge list (more efficient than insert) because this cluster is new and therefore has the highest id
    }

    private ClusterEdgeEnd skipExcludeEdge(ClusterEdgeEnd edgeEnd, ClusterEdge excludeEdge) {
      if (edgeEnd == null)
        return null;
      if (edgeEnd.edge == excludeEdge) {
        edgeEnd = edgeEnd.next;
      }
      return edgeEnd;
    }

    // to free space when this cluster object is only needed for the dendrogram
    private void clear() {
      edgeEnds = null;
      minorMaxHeap = null;
    }

    private void appendEnd(ClusterEdgeEnd edgeEnd) {
      edgeEnds.append(edgeEnd);
      minorMaxHeap.insert(edgeEnd.edge);
      // the max heap will be updated later
    }

    private void insertEnd(ClusterEdgeEnd edgeEnd) {
      edgeEnds.insert(edgeEnd);
      minorMaxHeap.insert(edgeEnd.edge);
      // the max heap will be updated later
    }

    private void appendEndUpdateMax(ClusterEdgeEnd edgeEnd) {
      ClusterEdge oldMinorMax = minorMaxHeap.max();
      appendEnd(edgeEnd);
      ClusterEdge newMinorMax = minorMaxHeap.max();

      ClusterEdge edge = edgeEnd.edge;

      if (newMinorMax == edge) {
        if (oldMinorMax != null) {
          Cluster opposite = oldMinorMax.e1.cluster == this ? oldMinorMax.e2.cluster : oldMinorMax.e1.cluster;

          if (opposite.minorMaxHeap.max() != oldMinorMax) {
            majorMaxHeap.remove(oldMinorMax);
          }
        }

        majorMaxHeap.insert(newMinorMax);
      }
    }

    private void removeEndUpdateMax(ClusterEdgeEnd edgeEnd) {
      ClusterEdge edge = edgeEnd.edge;

      boolean isMinorMax = (edge == minorMaxHeap.max());

      edgeEnds.remove(edgeEnd);
      minorMaxHeap.remove(edge);

      if (isMinorMax) {
        majorMaxHeap.remove(edge);
        majorMaxHeap.insert(minorMaxHeap.max());
      }
    }
    
    public String toString() {
      if (node == null) {
        return "c" + Integer.toString(id);
      } else {
        return node.getName();
      }
    }
  }


  private class ClusterEdge implements IndexedMaxHeap.Element<ClusterEdge> {
    private ClusterEdgeEnd e1;
    private ClusterEdgeEnd e2;

    private double dQ;


    ClusterEdge(ClusterEdgeEnd e1, ClusterEdgeEnd e2, double dQ) {
      this.e1 = e1;
      this.e2 = e2;
      this.dQ = dQ;
    }

    ClusterEdgeEnd getEdgeEnd(int end) {
      return (end == 1 ? e1 : e2);
    }

    private Map<IndexedMaxHeap<ClusterEdge>, Integer> heapMap = new TinyIdentityMap<IndexedMaxHeap<ClusterEdge>, Integer>(3);

    public void setHeapIndex(IndexedMaxHeap<ClusterEdge> heap, Integer index) {
      if (index == null) {
        heapMap.remove(heap);
      } else {
        heapMap.put(heap, index);
      }
    }

    public Integer getHeapIndex(IndexedMaxHeap<ClusterEdge> heap) {
      return (Integer)heapMap.get(heap);
    }

    public String toString() {
      return "(" + e1.cluster + ", " + e2.cluster + ")";
    }
  }


  private class ClusterEdgeEnd implements ExposedLinkedList.Element<ClusterEdgeEnd> {
    private Cluster cluster;
    private ClusterEdge edge;
    private int endIndex;
    
    private double weight_out = 0; // weight of edge(s) directed away from this end, used only during initialization

    private double dQ_out = 0; // dQ for the direction away from this end

    ClusterEdgeEnd(Cluster cluster, int endIndex) {
      this.cluster = cluster;
      this.endIndex = endIndex;
    }

    ClusterEdgeEnd getOppositeEdgeEnd() {
      return edge.getEdgeEnd(endIndex == 1 ? 2 : 1);
    }

    Cluster getOppositeCluster() {
      return getOppositeEdgeEnd().cluster;
    }


    private ClusterEdgeEnd next = null;
    private ClusterEdgeEnd prev = null;

    public ClusterEdgeEnd next() {
      return next;
    }

    public ClusterEdgeEnd prev() {
      return prev;
    }

    public void setNext(ClusterEdgeEnd edgeEnd) {
      next = edgeEnd;
    }

    public void setPrev(ClusterEdgeEnd edgeEnd) {
      prev = edgeEnd;
    }


    public String toString() {
      return ((Integer)getOppositeCluster().id).toString() + " p" + edge.heapMap.get(cluster.minorMaxHeap);
    }
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    if(args.length != 1)
    {
      System.out.println("usage: ModularityClusterer netkit-arff-file");
      System.out.println();
      System.out.println("This will read the graph, cluster it, and print out the clusters");
    }
    
    File arffFile = new File(args[0]);
    
    Graph graph = SchemaReader.readSchema(arffFile);
    
    ModularityClusterer mc = new ModularityClusterer(graph);
    mc.startClustering();
    
    for(Cluster c : mc.connectedClusters)
    {
      System.out.println("-------------------------------------");
      System.out.println("Cluster-"+c.id+" ["+c.size+" nodes]");
      System.out.println("-------------------------------------");
      for(Node n : c)
        System.out.println(n.getName());
      System.out.println();
    }
    
    for(Cluster c : mc.isolatedClusters)
    {
      System.out.println("-------------------------------------");
      System.out.println("Cluster-"+c.id+" ["+c.size+" nodes]");
      System.out.println("-------------------------------------");
      for(Node n : c)
        System.out.println(n.getName());
      System.out.println();
    }
  }
}