/**
 * GraphMetrics.java
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

package netkit.util;

import netkit.graph.*;
import netkit.util.ModularityClusterer.Cluster;
import netkit.classifiers.Classification;

import java.util.*;
import java.util.logging.Logger;

public class GraphMetrics {
	protected static Logger logger = NetKitEnv.getLogger(GraphMetrics.class);

	// calculate graph metrics:
	//   # nodes
	//   # edges (weighted + non)
	//   min,max,median,mean-degree (weighted + non)
	//   min,max,median,mean edge-weight
	//   cluster-coeff global
	//   cluster-coeff local
	//   assortative coeff  (Newman, 2003 "Mixing patterns in networks")
	//   number components
	//   max size component
	//   number singletons
	//   mean vertex-vertex distance (?)
	//   number triangles in network
	//   number paths of length 2 in the network

	/**
	 * This is a wrapper class for the COLT DoubleMatrix2D object such
	 * that NetKit does not require the colt library in its classpath.
	 * Otherwise, any instantiation of GraphMetrics would require that the
	 * COLT library is in the classpath regardless of whether the adjacency
	 * matrix is needed.
	 */
	public final class AdjacencyMatrix {
		public final cern.colt.matrix.DoubleMatrix2D coltMatrix;

		/**
		 * Create a wrapper for the (possibly unweighted) adjacency matrix of this graph in the COLT sparseMatrix2D format.
		 */
		public AdjacencyMatrix(boolean unweighted)
		{
			coltMatrix = buildAdjacencyMatrix(unweighted);
		}

		private cern.colt.matrix.DoubleMatrix2D buildAdjacencyMatrix(boolean unweighted) {
			cern.colt.matrix.impl.SparseDoubleMatrix2D matrix = new cern.colt.matrix.impl.SparseDoubleMatrix2D(numNodes,numNodes);

			for(int nt1=0;nt1<nodeTypes.length;nt1++)
			{
				Node[] nodes = graph.getNodes(nodeTypes[nt1]);
				int offset1 = nodetypeOffsets[nt1];
				for(Node node : nodes)
				{
					int idx1 = node.getIndex()+offset1;
					for(int nt2=0;nt2<nodeTypes.length;nt2++)
					{
						int offset2 = nodetypeOffsets[nt2];
						for(Edge nbrEdge : node.getEdgesToNeighbor(nodeTypes[nt2]))
						{
							Node nbr = nbrEdge.getDest();
							int idx2 = nbr.getIndex()+offset2;
							if(unweighted)
								matrix.setQuick(idx1,idx2,1);
							else
								matrix.setQuick(idx1,idx2,nbrEdge.getWeight() + matrix.getQuick(idx1,idx2));
						}
					}
				}
			}

			return matrix;
		}
	}

	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int MEAN = 2;

	public final Graph graph;
	public final String nodeType;
	public final int numNodes;
	public final int numEdges;

	private final Object lock = new Object();

	private ModularityClusterer mc = null;

	private boolean calcCentrality = false;
	private double centralityProgress = 0;
	private boolean calcCluster = false;
	private double clusterProgress = 0;
	private boolean calcComponent = false;
	private double componentProgress = 0;

	private double density = Double.NaN;
	private double numWeightedEdges = -1;
	private double[] edgeStat = null;
	private double[] degreeStat = null;
	private double[] weightedDegreeStat = null;
	private HistogramDiscrete degreeDist = null;

	private double efficiency = Double.NaN;
	private Map<String,Integer> nodetypeToIndex = null;
	private String[] nodeTypes = null;
	private int[] nodetypeOffsets = null;

        private ApproximateCentralities approx = null;
        private double alphaCentralityAlpha = Double.NaN;
        private double alphaCentralityDelta = Double.NaN;
        private double pagerankAlpha = Double.NaN;
        private double pagerankDelta = Double.NaN;

	//private double[] informationCentrality = null;
	//private double[] weightedInformationCentrality = null;
	private double[] closenessCentrality = null;
	private double[] weightedClosenessCentrality = null;
	private double[] betweennessCentrality = null;
	private double[] weightedBetweennessCentrality = null;
	private double[][] weightedPairwisedistances = null;
	private double[][] pairwisedistances = null;
	private double[] graphCentralityPerNode = null;
	private double[] weightedGraphCentralityPerNode = null;
	private double meanDist = Double.NaN;
	private double weightedMeanDist = Double.NaN;
	private double characteristicPathLength = Double.NaN;
	private double weightedCharacteristicPathLength = Double.NaN;
	private double graphCentrality = Double.NaN;
	private double weightedGraphCentrality = Double.NaN;
	private double maxDist = -1;
	private double weightedMaxDist = -1;

	private int numtriangles = -1;
	private int path2 = -1;
	private double localClusterCoeff = -1;
	private double globalClusterCoeff = -1;
	private int numComponents = -1;
	private Map<String,int[]> component = null; // component[nodetype]->[node-index] = which component does it belong to
	private int[] componentSize = null;
	private int[] clusternum = null; // clusternum[globalnodeindex] = which cluster does it belong to
	private Cluster[] clusters = null;
	private int maxComponentSize = -1;
	private int maxComponentIdx = -1;
	private int numSingletons = -1;

	/**
	 * Compute metrics over all nodes in the graph
	 * @param g
	 */
	public GraphMetrics(Graph g) {
		graph = g;
		nodeType = null;
		numNodes = graph.numNodes();
		numEdges = graph.numEdges();
		nodetypeToIndex = new HashMap<String,Integer>();
		nodeTypes = graph.getNodeTypes();
		nodetypeOffsets = new int[nodeTypes.length];
		int offset = 0;
		for(int i=0;i<nodeTypes.length;i++)
		{
			nodetypeToIndex.put(nodeTypes[i],i);
			nodetypeOffsets[i] = offset;
			offset += graph.numNodes(nodeTypes[i]);
		}
	}

	/**
	 * Compute certain metrics only over nodes of the given node type
	 * @param g
	 * @param nodeType
	 */
	public GraphMetrics(Graph g, String nodeType) {
		graph = g;
		this.nodeType = nodeType;
		numNodes = graph.numNodes(nodeType);
		int nE = 0;
		for(String et : graph.getEdgeTypeNames(nodeType,nodeType))
			nE += graph.numEdges(et);
		numEdges = nE;
		nodeTypes = new String[]{nodeType};
		nodetypeOffsets = new int[]{0};
		nodetypeToIndex = new HashMap<String,Integer>();
		nodetypeToIndex.put(nodeType,0);
	}

	public ModularityClusterer getClusterer() {
		if(mc == null)
			mc = new ModularityClusterer(graph);
		return mc;
	}

	private void doCluster() {
		if(clusternum != null)
			return;
		final ModularityClusterer mc = getClusterer();
		if(mc.percentDone()!=1.0D) {
			try {
				mc.startClustering();
			} catch(Exception ex) {}
		}

		clusternum = new int[graph.numNodes()];
		clusters = new Cluster[mc.getNumConnectedClusters()];
		int i=0;
		for(Cluster c : mc.getConnectedClusters())
		{     
			clusters[i] = c;
			for(Node n : c)
				clusternum[getNodeIndex(n)] = i;
			i++;
		}
	}

	/**
	 * @param node The node whose cluster index is sought after
	 * @return the cluster index of the given node as clustered by ModularityClusterer
	 */
	public int getCluster(Node node) {
		doCluster();
		return clusternum[getNodeIndex(node)];
	}

	/**
	 * @return the number of clusters found by ModularityClusterer
	 */
	public int getNumClusters() {
		doCluster();
		return clusters.length;
	}

	/**
	 * @return the specified cluster as found by ModularityClusterer
	 */
	public Cluster getCluster(int idx) {
		doCluster();
		return (idx<0 || idx>=clusters.length) ? null : clusters[idx];
	}


	private void cleanClusterStat() {
		path2 = -1;
		numtriangles = -1;
		globalClusterCoeff = (double)numtriangles / (double)path2;
		localClusterCoeff = 0;
	}

	public double calcClusterProgress() { return clusterProgress; }
	public boolean calcClusterActive() { return calcCluster; }
	public void stopCalcClusterStat() { calcCluster=false; }

	public void calculateClusterStat() {
		if(numtriangles >= 0)
			return;

		synchronized(lock) {
			if(calcCluster)
				throw new IllegalArgumentException("This graph metric is already computing cluster stats!");
			calcCluster = true;
		}
		path2 = 0;
		numtriangles = 0;
		int[] triple = new int[numNodes];
		int[] center = new int[numNodes];
		Arrays.fill(triple, 0);
		Arrays.fill(center, 0);

		clusterProgress = 0;
		double progressDelta = 1.0D/(double)((numNodes + graph.numEdges()*graph.numEdges()));

		for (final Node node1 : ((nodeType==null) ? graph.getNodes() : graph.getNodes(nodeType)))
		{
			for(final Edge e1 : ((nodeType == null) ? node1.getEdges() : node1.getEdgesToNeighbor(nodeType)))
			{
				clusterProgress += progressDelta;
				Node node2 = e1.getDest();
				int idx = getNodeIndex(node2);
				for(final Edge e2 : ((nodeType == null) ? node2.getEdges() : node2.getEdgesToNeighbor(nodeType)))
				{
					if(!calcCluster) {
						cleanClusterStat();
						return;
					}
					final Node node3 = e2.getDest();
					if (node1 == node3)
						continue;
					path2++;
					center[idx]++;
					if(node1.getEdgesToNeighbor(node3).length == 0)
						continue;
					if(node3.getEdgesToNeighbor(node1).length == 0)
						continue;
					triple[idx]++;
					numtriangles++;
				}
			}
		}

		globalClusterCoeff = (double)numtriangles / (double)path2;
		localClusterCoeff = 0;
		for (int i = 0; i < triple.length; i++)
		{
			clusterProgress += progressDelta;
			if(!calcCluster) {
				cleanClusterStat();
				return;
			}

			if (center[i] == 0 || triple[i] == 0)
				continue;
			localClusterCoeff += (double) triple[i] / (double) center[i];
		}
		localClusterCoeff /= triple.length;

		clusterProgress = 1;
		calcCluster = false;
	}

	/**
	 * @return The local clustering coefficient (computed as average over all triangles)
	 */
	public double getLocalClusterCoeff() {
		if (numtriangles < 0) calculateClusterStat();
		return localClusterCoeff;
	}

	/**
	 * @return The global clustering coefficient
	 */
	public double getGlobalClusterCoeff() {
		if (numtriangles < 0) calculateClusterStat();
		return globalClusterCoeff;
	}

	/**
	 * @return The number of triangles in the graph
	 */
	public int getNumtriangles() {
		if (numtriangles < 0) calculateClusterStat();
		return numtriangles;
	}

	/**
	 * @return The number of paths of length two that are not triangles
	 */
	public int getNumPath2() {
		if (numtriangles < 0) calculateClusterStat();
		return path2;
	}

	public void calculateEdgeStat() {
		String[] ets = ((nodeType == null) ? graph.getEdgeTypeNames() : graph.getEdgeTypeNames(nodeType,nodeType));
		edgeStat = new double[3];
		numWeightedEdges = 0;
		int nE = 0;
		double minE = Double.MAX_VALUE;
		double maxE = Double.MIN_VALUE;
		for(String et : ets)
		{
			for(Edge e : graph.getEdges(graph.getEdgeType(et)))
			{
				numWeightedEdges += e.getWeight();
				nE++;
				if(e.getWeight() < minE) minE = e.getWeight();
				if(e.getWeight() > maxE) maxE = e.getWeight();
			}
		}
		edgeStat[MIN] = minE;
		edgeStat[MAX] = maxE;
		edgeStat[MEAN] = numWeightedEdges / (double) nE;
	}

	/**
	 * @return The min edge weight
	 */
	 public double getMinEdgeWeight() {
		 if (edgeStat == null) calculateEdgeStat();
		 return edgeStat[MIN];
	 }

	 /**
	  * @return The max edge weight
	  */
	 public double getMaxEdgeWeight() {
		 if (edgeStat == null) calculateEdgeStat();
		 return edgeStat[MAX];
	 }

	 /**
	  * @return The average edge weight
	  */
	 public double getMeanEdgeWeight() {
		 if (edgeStat == null) calculateEdgeStat();
		 return edgeStat[MEAN];
	 }

	 /**
	  * @return The sum of all edges using their edge weight
	  */
	 public double getTotalEdgeWeights() {
		 if (edgeStat == null) calculateEdgeStat();
		 return numWeightedEdges;
	 }

	 /**
	  * @return The number of edges in the graph
	  */
	 public double getNumEdges() {
		 return numEdges;
	 }


	 /**
	  * @return The number of nodes in the graph
	  */
	 public double getNumNodes() {
		 return numNodes;
	 }

	 public void calculateDegreeStat() {
		 degreeStat = new double[]{0,0,0};
		 degreeStat[MIN] = Integer.MAX_VALUE;
		 degreeStat[MAX] = Integer.MIN_VALUE;

		 weightedDegreeStat = new double[]{0,0,0};
		 weightedDegreeStat[MIN] = Double.MAX_VALUE;
		 weightedDegreeStat[MAX] = Double.MIN_VALUE;

		 for (Node n : ((nodeType == null) ? graph.getNodes() : graph.getNodes(nodeType)))
		 {
			 int ud = ( (nodeType==null) ? n.getUnweightedDegree() : n.getUnweightedDegree(nodeType) );
			 double wd = ( (nodeType==null) ? n.getWeightedDegree() : n.getWeightedDegree(nodeType) );

			 degreeStat[MEAN] += ud;
			 if(ud < degreeStat[MIN]) degreeStat[MIN] = ud;
			 if(ud > degreeStat[MAX]) degreeStat[MAX] = ud;

			 weightedDegreeStat[MEAN] += wd;
			 if(wd < weightedDegreeStat[MIN]) weightedDegreeStat[MIN] = wd;
			 if(wd > weightedDegreeStat[MAX]) weightedDegreeStat[MAX] = wd;
		 }
		 degreeStat[MEAN] /= (double) numNodes;
		 weightedDegreeStat[MEAN] /= (double) numNodes;
	 }

	 /**
	  * Get the (possibly unweighted) adjacency matrix of this graph in the COLT sparseMatrix2D format.
	  * @return the (possibly unweighted) adjacency matrix of this graph in the COLT sparseMatrix2D format.
	  */
	 public AdjacencyMatrix getAdjacencyMatrix(boolean unweighted) {
		 return new AdjacencyMatrix(unweighted);
	 }

	 /**
	  * Return the histogram of (unweighted) edge degrees for nodes in the graph.
	  * @return the histogram of (unweighted) edge degrees for nodes in the graph.
	  */
	 public HistogramDiscrete getDegreeDistribution() {
		 if(degreeDist == null)
		 {
			 double[] values = new double[numNodes];
			 if(nodeType == null)
			 {
				 Node[] nodes = graph.getNodes();
				 for(int i=0;i<nodes.length;i++)
					 values[i] = nodes[i].getUnweightedDegree();
			 }
			 else
			 {
				 Node[] nodes = graph.getNodes(nodeType);
				 for(int i=0;i<nodes.length;i++)
					 values[i] = nodes[i].getUnweightedDegree(nodeType);
			 }
			 degreeDist = new HistogramDiscrete(values, null);
		 }
		 return degreeDist;
	 }

	 /**
	  * @return The min node degree when calculated ignoring edge weights
	  */
	 public double getMinDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return degreeStat[MIN];
	 }

	 /**
	  * @return The max node degree when calculated ignoring edge weights
	  */
	 public double getMaxDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return degreeStat[MAX];
	 }

	 /**
	  * @return The average node degree when calculated ignoring edge weights
	  */
	 public double getMeanDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return degreeStat[MEAN];
	 }

	 /**
	  * @return The minimum node degree when calculated using the weights of the edges
	  */
	 public double getMinWeightedDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return weightedDegreeStat[MIN];
	 }

	 /**
	  * @return The max node degree when calculated using the weights of the edges
	  */
	 public double getMaxWeightedDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return weightedDegreeStat[MAX];
	 }

	 /**
	  * @return The average node degree when calculated using the weights of the edges
	  */
	 public double getMeanWeightedDegree() {
		 if (degreeStat == null) calculateDegreeStat();
		 return weightedDegreeStat[MEAN];
	 }

	 private void cleanComponentStat() {
		 component = null;
		 maxComponentSize = 0;
		 numComponents = 0;
		 numSingletons = 0;
		 maxComponentSize = 0;
		 maxComponentIdx = 0;
		 componentSize = null;
	 }

	 public double calcComponentProgress() { return componentProgress; }
	 public boolean calcComponentActive() { return calcComponent; }
	 public void stopCalcComponentStat() { calcComponent=false; }

	 public void calculateComponentStat() {
		 if(component != null)
			 return;

		 synchronized(lock) {
			 if(calcComponent)
				 throw new IllegalStateException("This graph metrics is already computing component stats!");
			 calcComponent = true;
		 }
		 component = new HashMap<String,int[]>(nodeTypes.length);
		 Map<String,int[]> tag = new HashMap<String,int[]>(nodeTypes.length);
		 int[] tComponentSize = new int[numNodes];
		 Node[] front = new Node[numNodes];


		 for(String nt : nodeTypes)
		 {
			 int[] c = new int[graph.numNodes(nt)];
			 Arrays.fill(c, -1);
			 component.put(nt,c);
			 int[] t = new int[numNodes];
			 Arrays.fill(t, 0);
			 tag.put(nt,t);
		 }
		 maxComponentSize = 0;
		 numComponents = 0;
		 numSingletons = 0;
		 componentProgress = 0;
		 double progressDelta = 1.0D/(double)( (nodeType == null) ? graph.getNodes().length : graph.getNodes(nodeType).length );

		 for (Node node : ( (nodeType == null) ? graph.getNodes() : graph.getNodes(nodeType) ) )
		 {
			 componentProgress+=progressDelta;
			 if (component.get(node.getType())[node.getIndex()] >= 0)
				 continue;
			 front[0] = node;
			 int idx = 1;
			 int size = 0;
			 while (idx > 0)
			 {
				 if(!calcComponent) {
					 cleanComponentStat();
					 return;
				 }
				 --idx;
				 Node frontNode = front[idx];
				 int[] c = component.get(frontNode.getType());
				 if (c[frontNode.getIndex()] >= 0)
					 continue;
				 size++;
				 c[frontNode.getIndex()] = numComponents;
				 for (Edge e : ( (nodeType==null) ? frontNode.getEdges() : frontNode.getEdgesToNeighbor(nodeType)))
				 {
					 Node neighbor = e.getDest();
					 if (tag.get(neighbor.getType())[neighbor.getIndex()] > 0)
						 continue;
					 tag.get(neighbor.getType())[neighbor.getIndex()] = 1;
					 front[idx] = neighbor;
					 idx++;
				 }
			 }
			 if (size == 1)
				 numSingletons++;
			 if (size > maxComponentSize)
			 {
				 maxComponentSize = size;
				 maxComponentIdx = numComponents;
			 }
			 tComponentSize[numComponents] = size;
			 numComponents++;
		 }
		 componentSize = new int[numComponents];
		 System.arraycopy(tComponentSize, 0, componentSize, 0, numComponents);
		 componentProgress = 1;
		 calcComponent = false;
	 }

	 /**
	  * @param node The node whose component index is sought after
	  * @return The component index of the specified node
	  */
	 public int getComponent(Node node) {
		 if (component == null) calculateComponentStat();
		 int[] c = component.get(node.getType());
		 return ( (c==null) ? -1 : c[node.getIndex()]);
	 }

	 /**
	  * @return the number of connected components in the graph (includes singletons)
	  */
	 public int getNumComponents() {
		 if (component == null) calculateComponentStat();
		 return numComponents;
	 }

	 /**
	  * @return the number of singleton nodes in the graph
	  */
	 public int getNumSingletons() {
		 if (component == null) calculateComponentStat();
		 return numSingletons;
	 }

	 /**
	  * @return the size (number of nodes) of the largest connected component
	  */
	 public int getMaxComponentSize() {
		 if (component == null) calculateComponentStat();
		 return maxComponentSize;
	 }

	 /**
	  * @param componentNum The component whose size to get
	  * @return the number of nodes belonging to the specified connected component
	  */
	 public int getComponentSize(int componentNum) {
		 if (component == null) calculateComponentStat();
		 return componentSize[componentNum];
	 }

	 /**
	  * @return index into largest connected component in the graph
	  */
	 public int getMaxComponentIdx() {
		 if (component == null) calculateComponentStat();
		 return maxComponentIdx;
	 }

	 public int getNodeIndex(Node n) {
		 int offset = ( (nodeTypes.length==1) ? 0 : nodetypeOffsets[nodetypeToIndex.get(n.getType())] );
		 return offset + n.getIndex(); 
	 }

	 private void cleanCentralityStat() {
		 meanDist = Double.NaN;
		 maxDist = 0;
		 weightedMaxDist = 0;
		 weightedMeanDist = 0;
		 graphCentrality = 0;
		 weightedGraphCentrality = 0;
		 characteristicPathLength = 0;
		 weightedCharacteristicPathLength = 0;
		 betweennessCentrality = null;
		 pairwisedistances = null;
		 closenessCentrality = null;
		 graphCentralityPerNode = null;
		 weightedBetweennessCentrality = null;
		 weightedPairwisedistances = null;
		 weightedClosenessCentrality = null;
		 weightedGraphCentralityPerNode = null;
	 }

	 public double calcCentralityProgress() { return centralityProgress; }
	 public boolean calcCentralityActive() { return calcCentrality; }
	 public void stopCalcCentralityStat() { calcCentrality=false; }

	 /**
	  * This calculates the all-pairs closest distances
	  */
	  public void calculateCentralityStat() {
		  // This follows the pseudocode from:
		  // Ulrik Brandes, "A Faster Algorithm for Betweenness Centrality," 2001.
		  // 
		  // Updated psedo code + distance-weighted computations from:
		  // Ulrik Brandes, 
		  // "On variants of shortest-path betweenness centrality and their generic computation,"
		  // Social Networks, Vol. 30, No. 2. (May 2008), pp. 136-145.

		  if(!Double.isNaN(meanDist))
			  return;

		  synchronized(lock) {
			  if(calcCentrality)
				  throw new IllegalStateException("GraphMetrics is already calculating centrality stats!");
			  calcCentrality = true;
		  }

		  centralityProgress = 0;

		  betweennessCentrality = new double[numNodes];
		  Arrays.fill(betweennessCentrality,0.0);
		  pairwisedistances = new double[numNodes][numNodes];
		  closenessCentrality = new double[numNodes];
		  graphCentralityPerNode = new double[numNodes];

		  weightedBetweennessCentrality = new double[numNodes];
		  Arrays.fill(weightedBetweennessCentrality,0.0);
		  weightedPairwisedistances = new double[numNodes][numNodes];
		  weightedClosenessCentrality = new double[numNodes];
		  weightedGraphCentralityPerNode = new double[numNodes];

		  maxDist = 0;
		  weightedMaxDist = 0;

		  double[] nsp   = new double[numNodes]; // number shortest paths
		  double[] wnsp   = new double[numNodes]; // number shortest paths - weighted
		  double[] dist;
		  double[] wdist;
		  double[] delta = new double[numNodes];
		  double[] wdelta = new double[numNodes];
		  double[] avgSP = new double[numNodes];
		  double[] wavgSP = new double[numNodes];
		  int numPair    = 0;
		  int nodePair   = 0;
		  double totDist    = 0;
		  double nodeDist   = 0;
		  double wTotDist    = 0;
		  double wNodeDist   = 0;

		  boolean DEBUG = false;
		  Node[] revIdx = null;
		  if(DEBUG)
		  {
			  revIdx  = new Node[numNodes]; // This is for debugging
			  Arrays.fill(revIdx,null);
		  }

		  Queue<Node> Q = new LinkedList<Node>();
		  Stack<Node> S = new Stack<Node>();
		  List<List<Node>> pred = new ArrayList<List<Node>>();
		  List<List<Edge>> wpred = new ArrayList<List<Edge>>();

		  double progressDelta = 1.0D/(numNodes + numNodes*numNodes);

		  // N steps
		  for(int i=0;i<numNodes;i++)
		  {
			  centralityProgress += progressDelta;
			  pred.add(new ArrayList<Node>());
			  wpred.add(new ArrayList<Edge>());
		  }

		  if(!calcCentrality)
		  {
			  cleanCentralityStat();
			  return;
		  }

		  // for each node (N) do shortest-path (N*logN) roughly
		  for(int i=0;i<nodeTypes.length;i++)
		  {
			  int offsetS = nodetypeOffsets[i];

			  // For each node, do shortest-path problem
			  for(Node s : graph.getNodes(nodeTypes[i]))
			  {
				  if(!calcCentrality)
				  {
					  cleanCentralityStat();
					  return;
				  }

				  int idxS = s.getIndex() + offsetS;

				  if(DEBUG)
				  {
					  if(revIdx[idxS] == null)
						  revIdx[idxS] = s;
					  else if(revIdx[idxS] != s)
						  System.err.println("Node("+s.getName()+"): idx["+idxS+"] already taken by: "+revIdx[idxS].getName());

					  System.out.println("vertexStat: Analyzing Node("+s.getName()+"):");
				  }

				  // BEGIN: Initialization
				  dist = pairwisedistances[s.getIndex()];
				  wdist = weightedPairwisedistances[s.getIndex()];

				  Arrays.fill(dist,-1);
				  Arrays.fill(wdist,-1);

				  // clear pred for all nodes
				  for(List<Node> w : pred)
					  w.clear();

				  // clear pred for all nodes
				  for(List<Edge> w : wpred)
					  w.clear();

				  // nsp = sigma in pseudo code
				  Arrays.fill(nsp,0.0);
				  Arrays.fill(wnsp,0.0);
				  nsp[idxS] = 1;
				  wnsp[idxS] = 1;

				  dist[idxS] = 0;
				  wdist[idxS] = 0;

				  S.clear();
				  Q.clear();
				  Q.add(s);
				  // END: Initialization

				  while(!Q.isEmpty())
				  {
					  centralityProgress += progressDelta;

					  if(!calcCentrality)
					  {
						  cleanCentralityStat();
						  return;
					  }

					  Node v = Q.remove();
					  S.push(v);
					  int idxV = getNodeIndex(v);

					  if(DEBUG)
					  {
						  if(revIdx[idxV] == null)
							  revIdx[idxV] = v;
						  else if(revIdx[idxV] != v)
							  System.err.println("Node("+v.getName()+"): idx["+idxV+"] already taken by: "+revIdx[idxV].getName());
					  }

					  for(int j=0;j<nodeTypes.length;j++)
					  {
						  int offsetW = nodetypeOffsets[j];

						  // go through all edges from w 
						  for(Edge e : v.getEdgesToNeighbor(nodeTypes[j]))
						  {
							  if(!calcCentrality)
							  {
								  cleanCentralityStat();
								  return;
							  }

							  Node w = e.getDest();

							  int idxW = w.getIndex()+offsetW;

							  if(DEBUG)
							  {
								  if(revIdx[idxW] == null)
									  revIdx[idxW] = w;
								  else if(revIdx[idxW] != w)
									  System.err.println("Node("+w.getName()+"): idx["+idxW+"] already taken by: "+revIdx[idxW].getName());
							  }

							  double sp = dist[idxV] + 1;
							  double wsp = wdist[idxV] + 1.0/e.getWeight();

							  // path discovery: w found for the first time?
							  if(dist[idxW] < 0)
							  {
								  dist[idxW] = sp;
								  wdist[idxW] = wsp;
								  Q.add(w);
							  }

							  // path counting: shortest path to w via v?
							  if(dist[idxW] == sp )
							  {
								  nsp[idxW] += nsp[idxV];
								  pred.get(idxW).add(v);
							  }

							  // weighted path counting: shortest path to w via v?
							  if(wdist[idxW] == wsp )
							  {
								  wnsp[idxW] += wnsp[idxV];
								  wpred.get(idxW).add(e);
							  }
						  }
					  }
				  }

				  // accumulation phase - back-propagation of dependencies
				  Arrays.fill(delta, 0.0);
				  Arrays.fill(wdelta, 0.0);
				  while(!S.isEmpty())
				  {
					  if(!calcCentrality)
					  {
						  cleanCentralityStat();
						  return;
					  }

					  Node w = S.pop();
					  int idxW = getNodeIndex(w);

					  // handle unweighted case
					  for(Node v : pred.get(idxW))
					  {
						  int idxV = getNodeIndex(v);
						  delta[idxV] += (nsp[idxV]/nsp[idxW]) * (1.0 + delta[idxW]);
					  }

					  // handle weighted case
					  for(Edge e : wpred.get(idxW))
					  {
						  int idxV = getNodeIndex(e.getSource());
						  wdelta[idxV] += (wnsp[idxV]/wnsp[idxW]) * (e.getWeight() + delta[idxW]);
					  }

					  if(w != s)
					  {
						  betweennessCentrality[idxW] += delta[idxW];
						  weightedBetweennessCentrality[idxW] += wdelta[idxW];
					  }
				  }

				  if(DEBUG)
				  {                
					  System.out.print("Node("+s.getName()+") dist: ");
					  for(int k=0;k<dist.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+dist[k]);
					  }
					  System.out.println();
					  System.out.print("Node("+s.getName()+") wdist: ");
					  for(int k=0;k<wdist.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+wdist[k]);
					  }
					  System.out.println();

					  System.out.print("Node("+s.getName()+") NSP: ");
					  for(int k=0;k<nsp.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+nsp[k]);
					  }
					  System.out.println();
					  System.out.print("Node("+s.getName()+") wNSP: ");
					  for(int k=0;k<wnsp.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+wnsp[k]);
					  }
					  System.out.println();

					  System.out.print("Node("+s.getName()+") delta: ");
					  for(int k=0;k<delta.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+delta[k]);
					  }
					  System.out.println();
					  System.out.print("Node("+s.getName()+") wdelta: ");
					  for(int k=0;k<wdelta.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+wdelta[k]);
					  }
					  System.out.println();

					  System.out.print("betweenness: ");
					  for(int k=0;k<betweennessCentrality.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+betweennessCentrality[k]);
					  }
					  System.out.println();
					  System.out.print("weightedbetweenness: ");
					  for(int k=0;k<weightedBetweennessCentrality.length;k++)
					  {
						  if(revIdx[k] == null)
							  continue;
						  System.out.print(" "+revIdx[k].getName()+":"+weightedBetweennessCentrality[k]);
					  }
					  System.out.println();
				  } // DEBUG END

				  if(!calcCentrality)
				  {
					  cleanCentralityStat();
					  return;
				  }

				  nodeDist = 0;
				  nodePair = 0;
				  wNodeDist = 0;
				  for(double d : dist)
				  {
					  if(d<=0)
						  continue;
					  if(d>maxDist)
						  maxDist = d;
					  nodeDist+=d;
					  nodePair++;
				  }
				  for(double d : wdist)
				  {
					  if(d<=0)
						  continue;
					  if(d>weightedMaxDist)
						  weightedMaxDist = d;
					  wNodeDist+=d;
				  }

				  totDist += nodeDist;
				  wTotDist += wNodeDist;
				  numPair += nodePair;

				  avgSP[idxS]  = (double)nodeDist/(double)nodePair;
				  wavgSP[idxS] = (double)wNodeDist/(double)nodePair;
				  closenessCentrality[idxS] = 1/(double)nodeDist;               
				  weightedClosenessCentrality[idxS] = 1/(double)wNodeDist;               
				  graphCentralityPerNode[idxS] = 1.0/VectorMath.getMaxValue(dist);
				  weightedGraphCentralityPerNode[idxS] = 1.0/VectorMath.getMaxValue(wdist);
			  }
		  }

		  meanDist = (double)totDist/(double)numPair;
		  weightedMeanDist = (double)wTotDist/(double)numPair;

		  graphCentrality = 0;
		  weightedGraphCentrality = 0;
		  double max = betweennessCentrality[VectorMath.getMaxIdx(betweennessCentrality)];
		  for(double cb : betweennessCentrality)
		  {
			  graphCentrality += (max - cb);
		  }
		  graphCentrality /= (double)(numNodes-1);

		  double wmax = weightedBetweennessCentrality[VectorMath.getMaxIdx(weightedBetweennessCentrality)];
		  for(double cb : betweennessCentrality)
		  {
			  weightedGraphCentrality += (wmax - cb);
		  }
		  weightedGraphCentrality /= (double)(numNodes-1);

		  Arrays.sort(avgSP);
		  Arrays.sort(wavgSP);
		  int midPt = numNodes/2;
		  if((numNodes%2) == 0)
		  {
			  characteristicPathLength = (avgSP[midPt]+avgSP[midPt+1])/2.0;
			  weightedCharacteristicPathLength = (wavgSP[midPt]+wavgSP[midPt+1])/2.0;
		  }
		  else
		  {
			  characteristicPathLength = wavgSP[midPt];
			  weightedCharacteristicPathLength = wavgSP[midPt];
		  }
		  if(DEBUG)
		  {
			  System.out.println("pairwise:");
			  System.out.println(ArrayUtil.asString(pairwisedistances));
			  System.out.println("weightedpairwise:");
			  System.out.println(ArrayUtil.asString(weightedPairwisedistances));
		  }

		  centralityProgress = 1.0;
		  calcCentrality = false;
	  }


	  /**
	   * 
	   */
	  public double getDist(Node src, Node dst) {
		  calculateCentralityStat();
		  int srcIndex = getNodeIndex(src);
		  int dstIndex = getNodeIndex(dst);
		  return pairwisedistances[srcIndex][dstIndex]; 
	  }

	  /**
	   * 
	   */
	  public double getWeightedDist(Node src, Node dst) {
		  calculateCentralityStat();
		  int srcIndex = getNodeIndex(src);
		  int dstIndex = getNodeIndex(dst);
		  return weightedPairwisedistances[srcIndex][dstIndex]; 
	  }

	  /**
	   * @return average weighted path-length between nodes
	   */
	  public double getMeanDist() {
		  calculateCentralityStat();
		  return meanDist;
	  }

	  /**
	   * @return average weighted path-length between nodes
	   */
	  public double getWeightedMeanDist() {
		  calculateCentralityStat();
		  return weightedMeanDist;
	  }

	  /**
	   * @return weighted diameter of graph
	   */
	  public double getMaxDist() {
		  calculateCentralityStat();
		  return maxDist;
	  }

	  /**
	   * @return weighted diameter of graph
	   */
	  public double getWeightedMaxDist() {
		  calculateCentralityStat();
		  return weightedMaxDist;
	  }

	  /**
	   * Utility method which can be used by any sub-class
	   * @param matrix The assortativity matrix
	   * @return assortativity
	   */
	  public static double computeAssortativityFromMatrix(double[][] matrix) {
		  int numC = matrix.length;
		  double[] totC = new double[numC];
		  double[] totR = new double[numC];

		  Arrays.fill(totC, 0);
		  Arrays.fill(totR, 0);
		  for (int r = 0; r < numC; r++)
		  {
			  for (int c = 0; c < numC; c++)
			  {
				  totC[c] += matrix[r][c];
				  totR[r] += matrix[r][c];
			  }
		  }

		  double diag = 0;
		  double ab = 0;
		  for (int c = 0; c < numC; c++)
		  {
			  diag += matrix[c][c];
			  ab += totC[c] * totR[c];
		  }

		  return (ab==1 ? 1 : (diag - ab) / (1 - ab) );
	  }


	  public static double[] calculateEdgeBasedAssortativityCoeff(Classification known) {
		  return calculateEdgeBasedAssortativityCoeff(known, null);
	  }

	  public static double[] calculateNodeBasedAssortativityCoeff(Classification known) {
		  return calculateNodeBasedAssortativityCoeff(known, null);
	  }

	  private static int countNeighbors(Node node, Classification known, EdgeType et, double[][] val) {
		  if(et != null && !et.getDestType().equals(node.getType()))
			  return 0;
		  
		  Arrays.fill(val[0],0);
		  Arrays.fill(val[1],0);
		  Edge[] edges = (et == null) ? node.getEdgesToNeighbor(known.getNodeType()) : node.getEdgesByType(et.getName());
		  int d=0;
		  for (Edge e : edges)
		  {
			  if (known.isUnknown(e.getDest()))
				  continue;
			  int c2 = known.getClassValue(e.getDest());
			  val[1][c2] += e.getWeight();
			  val[0][c2]++;
			  d++;
		  }
		  if(d>0) {
			  VectorMath.normalize(val[0]);
			  VectorMath.normalize(val[1]);
		  }
		  return d;
	  }
	  
	  public static double[] calculateNodeBasedAssortativityCoeff(Classification known, EdgeType et) {
		  if(known.size()==0)
			  return new double[]{Double.NaN, Double.NaN};

		  if(et != null && !et.getDestType().equals(known.getNodeType()))
			  return new double[]{Double.NaN, Double.NaN};
		  
		  int numC = known.getAttribute().size();
		  double[][] count = new double[2][numC];
		  double[][][] index = new double[2][numC][numC];

		  for (int t = 0; t < index.length; t++)
			  for (int i = 0; i < index[t].length; i++)
				  Arrays.fill(index[t][i], 0);
		  double num = 0;
		  for (Node node : known)
		  {
			  if (known.isUnknown(node))
				  continue;
			  int c1 = known.getClassValue(node);
			  if(countNeighbors(node, known, et, count)>0) {
				  num++;
				  VectorMath.add(index[0][c1],count[0]);
				  VectorMath.add(index[1][c1],count[1]);
			  }
		  }
		  num *= numC; // normalizing constant to ensure that matrix elements sum to 1
		  double[] nodeAssort = new double[index.length];
		  for(int p=0;p<index.length;p++) {
			  for(int c1=0;c1<numC;c1++)
				  VectorMath.divide(index[p][c1],num);
			  nodeAssort[p] = computeAssortativityFromMatrix(index[p]);
		  }
		  return nodeAssort;
	  }

	  public static double[] calculateEdgeBasedAssortativityCoeff(Classification known, EdgeType et) {
		  if(known.size()==0)
			  return new double[]{Double.NaN, Double.NaN};

		  if(et != null && !et.getDestType().equals(known.getNodeType()))
			  return new double[]{Double.NaN, Double.NaN};
		  
		  final int numC = known.getAttribute().size();

		  double[][][] index = new double[2][numC][numC];
		  for (int t = 0; t < index.length; t++)
			  for (int i = 0; i < index[t].length; i++)
				  Arrays.fill(index[t][i], 0);

		  double[] numE = new double[]{0, 0};
		  for (Node node : known)
		  {
			  if (known.isUnknown(node))
				  continue;
			  int c1 = known.getClassValue(node);
			  Edge[] edges = (et == null) ? node.getEdgesToNeighbor(known.getNodeType()) : node.getEdgesByType(et.getName());
			  for (Edge e : edges)
			  {
				  if (known.isUnknown(e.getDest()))
					  continue;
				  int c2 = known.getClassValue(e.getDest());
				  
				  index[1][c1][c2] += e.getWeight();
				  numE[1] += e.getWeight();
				  index[0][c1][c2]++;
				  numE[0]++;
			  }
		  }
		  double[] edgeAssort = new double[index.length];
		  for(int p=0;p<index.length;p++) {
			  for(double[] row : index[p])
				  for(int i=0;i<row.length;i++)
					  row[i] /= numE[p];
			  edgeAssort[p] = computeAssortativityFromMatrix(index[p]);
		  }

		  return edgeAssort;
	  }

	  public double[] calculateEdgeBasedAssortativityCoeff(String nodeType, AttributeCategorical attribute) {
		  return calculateEdgeBasedAssortativityCoeff(new Classification(graph, nodeType, attribute));
	  }

	  public double[] calculateNodeBasedAssortativityCoeff(String nodeType, AttributeCategorical attribute) {
		  return calculateNodeBasedAssortativityCoeff(new Classification(graph, nodeType, attribute));
	  }

	  public double[] calculateEdgeBasedAssortativityCoeff(String nodeType, AttributeCategorical attribute, EdgeType et) {
		  return calculateEdgeBasedAssortativityCoeff(new Classification(graph, nodeType, attribute), et);
	  }

	  public double[] calculateNodeBasedAssortativityCoeff(String nodeType, AttributeCategorical attribute, EdgeType et) {
		  return calculateNodeBasedAssortativityCoeff(new Classification(graph, nodeType, attribute), et);
	  }

	  /**
	   * Not implemented yet.
	   * @return
	   */
	  public double getEfficiency() {
		  if(Double.isNaN(efficiency))
		  {
		  }
		  return efficiency;
	  }

	  /**
	   * Return the density of the graph as defined by: |E|/(|N|*(|N)-1)), where |E| is the number
	   * of edges and |N| is the number of nodes in the graph.
	   * 
	   * @return the density of the graph
	   */
	  public double getGraphDensity() {
		  if(Double.isNaN(density) && numNodes > 1)
			  density = (double)numEdges/(double)(numNodes*(numNodes-1));
		  return density;
	  }

	  /**
	   * Get the closeness centrality for the given node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the closeness centrality for the given node.
	   */
	  public double getClosenessCentrality(Node n) {
		  calculateCentralityStat();
		  return closenessCentrality[getNodeIndex(n)];
	  }

	  /**
	   * Get the closeness centrality for the given node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the closeness centrality for the given node.
	   */
	  public double getWeightedClosenessCentrality(Node n) {
		  calculateCentralityStat();
		  return weightedClosenessCentrality[getNodeIndex(n)];
	  }

	  /**
	   * Not implemented yet.
	   * @return
	   */
	  public double getInformationCentrality(Node n) {
		  return Double.NaN;
	  }

	  /**
	   * Not implemented yet.
	   * @return
	   */
	  public double getWeightedInformationCentrality(Node n) {
		  return Double.NaN;
	  }

	  /**
	   * Get the betweenness centrality for the given node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the betweenness centrality for the given node.
	   */
	  public double getBetweennessCentrality(Node n) {
		  calculateCentralityStat();
		  return betweennessCentrality[getNodeIndex(n)];
	  }

	  /**
	   * Get the betweenness centrality for the given node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the betweenness centrality for the given node.
	   */
	  public double getWeightedBetweennessCentrality(Node n) {
		  calculateCentralityStat();
		  return weightedBetweennessCentrality[getNodeIndex(n)];
	  }

	  /**
	   * Get the graph centrality.
	   * For details on this, see Linton C. Freeman, &quot;A Set of Measures of Centrality Based on Betweenness,&quot Sociometry, Vol 40, No 1, pg. 35-41, 1977.
	   * @return the graph centrality.
	   */
	  public double getGraphCentrality() {
		  calculateCentralityStat();
		  return graphCentrality;
	  }

	  /**
	   * Get the graph centrality.
	   * For details on this, see Linton C. Freeman, &quot;A Set of Measures of Centrality Based on Betweenness,&quot Sociometry, Vol 40, No 1, pg. 35-41, 1977.
	   * @return the graph centrality.
	   */
	  public double getWeightedGraphCentrality() {
		  calculateCentralityStat();
		  return weightedGraphCentrality;
	  }

	  /**
	   * Get the graph centrality for a specific node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the graph centrality for a specific node.
	   */
	  public double getGraphCentrality(Node n) {
		  calculateCentralityStat();
		  return graphCentralityPerNode[getNodeIndex(n)];
	  }

	  /**
	   * Get the graph centrality for a specific node.
	   * For details on this, see Ulrik Brandes, &quot;A Faster Algorithm for Betweenness Centrality,&quot (2001).
	   * @return the graph centrality for a specific node.
	   */
	  public double getWeightedGraphCentrality(Node n) {
		  calculateCentralityStat();
		  return weightedGraphCentralityPerNode[getNodeIndex(n)];
	  }

	  /**
	   * @return median of all average shortest-path-lengths of all nodes
	   */
	  public double getCharacteristicPathLength() {
		  calculateCentralityStat();
		  return characteristicPathLength;
	  }

	  /**
	   * @return median of all average shortest-path-lengths of all nodes
	   */
	  public double getWeightedCharacteristicPathLength() {
		  calculateCentralityStat();
		  return weightedCharacteristicPathLength;
	  }

    /**
     * @return the alpha used when computing approximate alpha centrality
     */
    public double getAlphaCentralityAlpha() {
	return alphaCentralityAlpha;
    }

    /**
     * set the alpha used when computing approximate alpha centrality
     */
    public void setAlphaCentralityAlpha(final double alpha) {
	alphaCentralityAlpha = alpha;
    }

    /**
     * @return the delta used when computing approximate alpha centrality
     */
    public double getAlphaCentralityDelta() {
	return alphaCentralityDelta;
    }

    /**
     * set the delta used when computing approximate alpha centrality
     */
    public void setAlphaCentralityDelta(final double delta) {
	alphaCentralityDelta = delta;
    }

    /**
     * @return the alpha used when computing approximate pagerank centrality
     */
    public double getPagerankAlpha() {
	return pagerankAlpha;
    }

    /**
     * set the alpha used when computing approximate pagerank centrality
     */
    public void setPagerankAlpha(final double alpha) {
	pagerankAlpha = alpha;
    }

    /**
     * @return the delta used when computing approximate pagerank centrality
     */
    public double getPagerankDelta() {
	return pagerankDelta;
    }

    /**
     * set the delta used when computing approximate pagerank centrality
     */
    public void setPagerankDelta(final double delta) {
	pagerankDelta = delta;
    }

    public ApproximateCentralities getApproximateCentralities() {
	if(approx == null)
	    approx = new ApproximateCentralities(this);
	return approx;
    }
}
