package netkit.graph.edgecreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import netkit.graph.*;
import netkit.util.GraphMetrics;
import netkit.util.NetKitEnv;
import netkit.util.VectorMath;
import netkit.classifiers.Classification;
import netkit.classifiers.DataSplit;

public abstract class EdgeCreatorImp implements EdgeCreator {
  protected Logger logger = NetKitEnv.getLogger(this);

  /**
   * Utility class for sub-classes where necessary
   */
  protected final class NbrEntry implements Comparable<NbrEntry> {
    public final Node source;
    public final Node dest;
    public final double score;
    public NbrEntry (final Node src, final Node dest, final double score) {
      this.source = src;
      this.dest = dest;
      this.score = score;
    }
    public int compareTo(NbrEntry e) {
      return -(int)Math.signum(score - e.score);
    }
    public Edge asEdge() {
      return new Edge(edgetype,source,dest,score);
    }
    public Edge asEdge(Node src) {
      return new Edge(edgetype,src,dest,score);
    }
  }
  
  protected Edge[] edges = null;
  protected Graph graph = null;
  protected String nodeType=null;
  protected int attributeIndex=-1;
  protected int maxEdges=-1;
  protected double attributeValue=Double.NaN;
  protected EdgeType edgetype=null;
  protected GraphMetrics gm=null;
  protected Attribute attrib=null;
  protected DataSplit split=null;
  protected double trueAssortativity=Double.NaN;
  protected double trainAssortativity=Double.NaN;
  
  @Override
  public void initialize(final Graph graph, final String nodeType, final int attributeIndex, final double attributeValue, final int maxEdges) {
    if(graph == null)
      throw new IllegalArgumentException("incoming value of 'graph' is null!");
    if(maxEdges == 0)
      throw new IllegalArgumentException("incoming value of 'maxEdges' = 0!");
    if(this.graph != null)
      throw new IllegalStateException("This creator has already been initialized");
    
    this.graph = graph;
    this.attributeIndex = attributeIndex;
    this.gm = graph.getMetrics();
    this.nodeType = nodeType;
    this.attributeValue = attributeValue;

    this.maxEdges = maxEdges;
    final Attributes as = graph.getAttributes(nodeType);
    attrib = as.getAttribute(attributeIndex);

    if(isByAttribute() && !canHandle(attrib))
        throw new IllegalArgumentException(getName()+" cannot handle attribute "+attrib.getName());
    final String connect = (isByAttribute() ? attrib.getName() : nodeType);
    String eName = getName()+"-"+connect;
    
    if(isByAttributeValue()) {
      String sNm = null;
      if(attrib.getType() == Type.CATEGORICAL) {
        AttributeCategorical ac = (AttributeCategorical)attrib;
        sNm = ac.getToken((int)attributeValue);
      } else {
        sNm = Double.toString(attributeValue);
      }
      eName = eName+"["+sNm+"]";
    } 
    edgetype = new EdgeType(eName,nodeType,nodeType);

    logger.info("Initialized("+getName()+") nodeType="+nodeType+" edgeType="+eName+" attrib="+attrib.getName()+" maxEdges="+maxEdges+" attributeValue="+attributeValue);
  }
  
  @Override
  public void buildModel(DataSplit split) {
    if(graph == null)
      throw new IllegalStateException("EdgeCreator has not yet been built!");
    if(split.getView().getGraph()!=graph)
      throw new IllegalArgumentException("split object has a different graph object than this edge creator!");
    this.split = split;
    trainAssortativity = Double.NaN;
  }
  
  @Override
  public final EdgeType getEdgeType() {
    return edgetype;
  }

  @Override
  public final int getAttributeIndex() {
    return attributeIndex;
  }
  
  @Override
  public final int getMaxEdges() {
    return maxEdges;
  }

  
  @Override
  /**
   * Return true: by default, an edgecreator is by attribute (i.e., it creates an edge by
   * an attribute).
   */
  public boolean isByAttribute() {
    return true;
  }

  @Override
  /**
   * Return false: by default, an edgecreator is by attribute as a whole.
   */
  public boolean isByAttributeValue() {
    return false;
  }

  @Override
  /**
   * Return false: by default, an edgecreator is by attribute as a whole and cannot handle attribute values.
   */
  public boolean canHandleAttributeValue(final Attribute attribute) {
    return false;
  }

  @Override
  public final double getAttributeValue() {
    return attributeValue;
  }
  
  /**
   * Utility method whch can be used by any sub-class.  Adds 
   * @param matrix An assortativity matrix
   * @param cliqueClassDistrib The number of nodes for each class in the incoming clique
   */
  protected final void addCliqueToAssortMatrix(final double[][] matrix, final double[] cliqueClassDistrib) {
    for(int r=0;r<cliqueClassDistrib.length;r++) {
      if(cliqueClassDistrib[r]==0)
        continue;
      matrix[r][r] += cliqueClassDistrib[r]*(cliqueClassDistrib[r]-1);
      for(int c=r+1;c<cliqueClassDistrib.length;c++) {
        double v = cliqueClassDistrib[r]*cliqueClassDistrib[c];
        matrix[r][c] += v;
        matrix[c][r] += v;
      }
    }
  }
    

  /**
   * Get a classification object which contains all the nodes to be used to calculate
   * assortativity
   * 
   * @param split The data train/test split to use 
   * @param useTrueAssort If true, use all objects, otherwise use only nods from training set (given in DataSplit object during initialize) 
   * @throws IllegalArgumentException if creator has not yet been initialized
   */
  public final Classification getLabeledNodes(final DataSplit split, final boolean useTrueAssort) {
    if(graph == null)
      throw new IllegalArgumentException("EdgeCreator has not yet been initialized!");
    Classification labels = null;
    if(useTrueAssort) {
        labels = split.getView().getTruth();
    }
    else
    {
      labels = new Classification(graph, nodeType, split.getView().getAttribute());
      final int clsIdx = split.getView().clsIdx;
      labels.clear();
      for (Node node : split.getTrainSet())
        if (!node.isMissing(clsIdx))
          labels.set(node, node.getValue(clsIdx));
    }
    return labels;
  }
  
  /**
   * Get the edges to the <i>K</i> nearest nodes (highest weight using this edge creator),
   * where <i>max-k</i> was provided during initialization.  This goes through all nodes
   * in the graph, so sub-classes should override to provide a more optimized version.
   * This list should include all nearest neighbors, not only those in the 'training set'.
   * It will look for a cutoff in weights less than <i>k</i>, but if the first <i>k</i>
   * weights are all the same then it will look forward to the first cutoff.  This may
   * mean that all nodes are neighbors if all weights are the same.
   * 
   * @param node The node 
   * @return
   */
  public Edge[] getEdgesToNearestNeighbors(final Node node) {
    final List<NbrEntry> nbrList = new ArrayList<NbrEntry>();
    for (Node dest : graph.getNodes(nodeType)) {
      final double w = getWeight(node, dest);
      if(Double.isNaN(w) || w == 0)
        continue;
      nbrList.add(new NbrEntry(node,dest,w));
    }
    
    // now, find the cutoff less than K
    int currI = nbrList.size();
    if(maxEdges > 0 && currI > maxEdges) {
      Collections.sort(nbrList);
      final double currW = nbrList.get(maxEdges).score;
      currI = maxEdges-1;
      while(currI >= 0 && nbrList.get(currI).score==currW)
        currI--;
      
      // if we could not find a cutoff (all weights are the same), look forward
      if(currI==-1) {
        // if all weights are the same, 
        if(nbrList.get(nbrList.size()-1).score==currW)
          currI=nbrList.size();
        else {
          currI = maxEdges+1;
          while(currI < nbrList.size() && nbrList.get(currI).score==currW)
            currI++;
        }
      }
    }

    Edge[] edges = new Edge[currI];
    for(int i=0;i<currI;i++)
      edges[i] = nbrList.get(i).asEdge();
    return edges;
  }
  
 
  // this relies on how edges are built: in order/grouped by source-node
  // so that as we go through the edges, we will see all neighbor nodes
  // of a given source node grouped together in the array
  public double[][] getAssortativityMatrix(final boolean useTrueAssort) {
    buildEdges();
    final GraphMetrics metric = split.getView().getGraph().getMetrics();
    Classification known = this.getLabeledNodes(split, useTrueAssort);
    int numC = split.getView().getAttribute().size();

    double[] nbrcount = new double[numC];
    double[][] matrix = new double[numC][numC];
    for(double[] row : matrix)
      Arrays.fill(row,0);
    
    // we sort the edges by source node
    Arrays.sort(edges, new Comparator<Edge>() {
      @Override
      public int compare(Edge e1, Edge e2) {
        return metric.getNodeIndex(e1.getSource()) - metric.getNodeIndex(e2.getSource());
      } });

    double tot=0;
    for(int i=0;i<edges.length;i++) {
      Node src = edges[i].getSource();  
      if(known.isUnknown(src))
        continue;

      // first, we'll add up all the neighbors for this source
      Arrays.fill(nbrcount, 0);
      int d=0;
      while(i<edges.length && edges[i].getSource() == src) {
        if(!known.isUnknown(edges[i].getDest())) {
          int n = known.getClassValue(edges[i].getDest());
          nbrcount[n] += edges[i].getWeight();
          if(Double.isNaN(nbrcount[n]))
            logger.info("NaN weight on edge: "+edges[i]);
          d++;
        }
        i++;
      }
      // second, if node had any neighbors,
      // normalize the neighbor vector and add it to the matrix
      if(d>0) {
        int c = known.getClassValue(src);
        tot++;
        VectorMath.normalize(nbrcount);
        VectorMath.add(matrix[c], nbrcount);
      }
    }
    
    if(tot>0) {
      // finally, normalize the matrix so it sums to 1
      for(int i=0;i<numC;i++) {
        VectorMath.divide(matrix[i], tot);
      }
    }
    return matrix;
  }
  
  @Override
  public final double getAssortativity(final boolean useTrueAssort) {
    if(useTrueAssort) {
      if(!Double.isNaN(trueAssortativity))
        return trueAssortativity;
    } else if(!Double.isNaN(trainAssortativity))
        return trainAssortativity;

    if(split == null)
      throw new IllegalArgumentException("EdgeCreator has not yet built a model!");
    final double[][] matrix = getAssortativityMatrix(useTrueAssort);
    double assortativity = GraphMetrics.computeAssortativityFromMatrix(matrix);
    if(useTrueAssort)
      trueAssortativity = assortativity;
    else
      trainAssortativity = assortativity;
    
    return assortativity;
  }
  
  @Override
  public final Edge[] createEdges() {
    if(edges == null)
      buildEdges();
    return edges.clone();
  }
  
  /**
   * build the edges using the current edge creation model.   If you want to use
   * the default buildAssortativityMatrix method and if you override this method, then
   * you <i>must</i> add edges such that they are grouped by source node (i.e., all
   * edges with the same source node appear sequentially in the edges array).   This
   * assumption is leverage by the default getAssortativityMatrix method for efficiency.
   */ 
  protected void buildEdges() {
    if(edges != null)
      return;
    if(split == null)
      throw new IllegalArgumentException("EdgeCreator has not yet built a model!");

    final ArrayList<Edge> edgelist = new ArrayList<Edge>();
    boolean reverse = edgetype.getDestType().equals(edgetype.getSourceType());
    for(Node node : graph.getNodes(nodeType)) {
      for(Edge edge : getEdgesToNearestNeighbors(node)) {
        edgelist.add(edge);

        // TODO: Fix double creation of edges
        if(reverse)
          edgelist.add(new Edge(edgetype,edge.getDest(),edge.getSource(),edge.getWeight()));
      }
    }

    edges = edgelist.toArray(new Edge[0]);
  }
}
