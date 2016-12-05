/**
 * DataSampler.java
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

package netkit.classifiers;

import netkit.graph.*;
import netkit.util.VectorMath;
import netkit.util.ArrayUtil;
import netkit.util.NetKitEnv;

import java.util.*;
import java.util.logging.Logger;

public final class DataSampler {
  private final Logger logger = NetKitEnv.getLogger(this);

  private boolean DEBUG = false;
  
  private Node[] nodes = null;
  private double[] distrib;
  private double[] currState; 
  private int[] currIndex;
  private Node[][] nodesByClass = null;

  private final Random pick;
  private int numMissingValues;
  private boolean sampleUnknown;
  private int size;
  private boolean replacement;
  private boolean stratified;
  private Classification truth;

  public final int clsIdx;

  private DataSampler(DataSampler sampler) {
    nodes = sampler.nodes;
    distrib = sampler.distrib;
    currState = sampler.currState.clone();
    currIndex = sampler.currIndex.clone();
    if(sampler.nodesByClass != null)
    {
      nodesByClass = sampler.nodesByClass.clone();
      for(int i=0;i<nodesByClass.length;i++)
        nodesByClass[i] = sampler.nodesByClass[i].clone();
    }
    replacement = sampler.replacement;
    stratified = sampler.stratified;
    pick = sampler.pick;
    clsIdx = sampler.clsIdx;
    numMissingValues = sampler.numMissingValues;
    truth = sampler.truth;
    size = sampler.size;
    sampleUnknown = sampler.sampleUnknown;
    DEBUG = sampler.DEBUG;
  }
  public DataSampler(final Node[] nodes, final int attribIdx) {
    this(nodes, attribIdx, VectorMath.getSeed());
  }
  public DataSampler(final Node[] nodes, final int attribIdx, final long seed) {
    this(nodes, attribIdx, seed, false, true, false);
  }
  public DataSampler(final Node[] nodes, final int attribIdx, final long seed, final boolean replacement, final boolean stratified, final boolean sampleUnknown) {
    this.nodes = nodes;
    size = nodes.length;
    this.replacement = replacement;
    this.stratified = stratified;
    this.sampleUnknown = sampleUnknown;  
    this.DEBUG = logger.isLoggable(java.util.logging.Level.FINEST);
    pick = new Random(seed);

    clsIdx = attribIdx;
    double max=-1;
    for(Node n : nodes)
      if(n.getValue(clsIdx)>max)
        max = n.getValue(clsIdx);
    int attribSize = (int)max;
    double[] prior = new double[attribSize+1];
    int[] cls   = new int[nodes.length];

    Arrays.fill(prior,0);
    numMissingValues = 0;
    for(int i=0;i<nodes.length;i++)
    {
      Node n = nodes[i];
      if(n.isMissing(clsIdx))
      {
        numMissingValues++;
        cls[i] = prior.length;
      }
      else
      {
        cls[i] = (int)n.getValue(clsIdx);
        prior[cls[i]]++;
      }
    }

    int extra = ((numMissingValues>0 && sampleUnknown)?1:0);
    distrib   = new double[prior.length+extra];
    currState = new double[prior.length+extra];
    currIndex = new int[prior.length+extra];
    System.arraycopy(prior, 0, distrib, 0, prior.length);
    if(numMissingValues>0 && sampleUnknown)
      distrib[prior.length] = numMissingValues;

    Arrays.fill(currState,0);
    Arrays.fill(currIndex,0);

    if(stratified)
    {
      nodesByClass = new Node[distrib.length][];
      for(int i=0;i<nodesByClass.length;i++)
        nodesByClass[i] = new Node[(int)distrib[i]];
      for(int i=0;i<nodes.length;i++)
      {
        if(cls[i] >= nodesByClass.length)
          continue;
        nodesByClass[cls[i]][currIndex[cls[i]]] = nodes[i];
        currIndex[cls[i]]++;
      } 
      Arrays.fill(currIndex,0);
      for(int i=0;i<nodesByClass.length;i++)
        VectorMath.randomize(nodesByClass[i]);
      this.nodes = null;
    }
    else
    {
      VectorMath.randomize(nodes);
    }

    VectorMath.normalize(distrib);

    if(!sampleUnknown)
    {
      if(numMissingValues>0)
      {
        Node[] newNodes = new Node[size-numMissingValues];
        int i=0;
        for(Node n : nodes)
          if(!n.isMissing(clsIdx))
            newNodes[i++] = n;
        this.nodes = newNodes;
      }
      size -= numMissingValues;      
      numMissingValues = 0;
    }
  }

  public DataSampler clone() {
    return new DataSampler(this);
  }    
  public int numMissingValues() {
    return numMissingValues;
  }
  public int size() {
    return size;
  }
  public double[] getDistribution() {
    return distrib.clone();
  }
  public boolean doReplacement() {
    return replacement;
  }
  public boolean sampleUnknown() {
    return sampleUnknown;
  }
  public boolean doStratified() {
    return stratified;
  }

  private void randomize() {
    if(nodesByClass != null)
    {
      for(int i=0;i<nodesByClass.length;i++)
        VectorMath.randomize(nodesByClass[i]);
    }
    if(nodes != null)
      VectorMath.randomize(nodes);
    Arrays.fill(currIndex, 0);
  }

  private void sampleWithReplacement(Node[] result, int[] numSamples) {
    for(int c=0,i=0;c<numSamples.length;c++)
      for(int j=0;j<numSamples[c];j++,i++)
        result[i] = nodesByClass[c][pick.nextInt(nodesByClass[c].length)]; 
  }
  
  private void sampleWithReplacement(Node[] result) {
    for(int i=0;i<result.length;i++)
      result[i] = nodes[pick.nextInt(nodes.length)]; 
  }
  
  private int sampleNoReplacement(Node[] src, int srcStart, Node[] dst, int dstStart, int numSamples) {
    if(DEBUG) System.out.println("    sampleNoReplacement(srcSize="+src.length+",srcStart="+srcStart+",dstSize="+dst.length+",dstStart="+dstStart+",numSamples="+numSamples+")");
    
    int start = srcStart;
    int end = start + numSamples;
    if(end>src.length)
    {
      if(DEBUG)
      {
        System.out.print("RESET:     initial array:");
        for(int i=0;i<src.length;i++)
        {
          if(i==start)
            System.out.print(" |");
          System.out.print(" "+src[i].getName());
        }
        System.out.println();
      }
      VectorMath.randomize(src,start);
      for(int i=0,j=start;j<src.length;j++,i++)
      {
        final Node tmp = src[i];
        src[i] = src[j];
        src[j] = tmp;
      }
      start = 0;
      end = numSamples;

      if(DEBUG)
      {
        System.out.print("RESET:     reset array:  ");
        for(int i=0;i<src.length;i++)
        {
          if(i==end)
            System.out.print(" |");
          System.out.print(" "+src[i].getName());
        }
        System.out.println();
      }
    }
    System.arraycopy(src, start, dst, dstStart, numSamples);
    
    return end;
  }
  
  private void sampleNoReplacement(Node[] result, int[] numSamples) {
    if(DEBUG)
    { 
      System.out.println("START: sampleNoReplacement(resultSize="+result.length+",numSamples="+ArrayUtil.asString(numSamples)+"):"+
          " currState="+ArrayUtil.asString(currState)+
          " currIndex="+ArrayUtil.asString(currIndex));
    }

    for(int c=0,dstIdx=0;c<nodesByClass.length;c++)
    {
      currIndex[c] = sampleNoReplacement(nodesByClass[c],currIndex[c], result, dstIdx, numSamples[c]);
      dstIdx+=numSamples[c];
    }

    if(DEBUG)
    { 
      System.out.println("STOP:  sampleNoReplacement(resultSize="+result.length+",numSamples="+ArrayUtil.asString(numSamples)+"):"+
          " currState="+ArrayUtil.asString(currState)+
          " currIndex="+ArrayUtil.asString(currIndex));
    }
  }

  private void sampleNoReplacement(Node[] result) {
    if(DEBUG) System.out.println("START: sampleNoReplacement(resultSize="+result.length+" currIndex="+currIndex[0]+")");

    currIndex[0] = sampleNoReplacement(nodes,currIndex[0], result, 0, result.length);
    
    if(DEBUG) System.out.println("STOP:  sampleNoReplacement(resultSize="+result.length+" currIndex="+currIndex[0]+")");
  }
  
  private int[] getNumberStratifiedSamples(int size) {
    if(size < distrib.length)
      logger.warning("getNumberStratifiedSamples(size="+size+"): not big enough size for stratitified sampling as there are "+distrib.length+" classes to sample from.");
    if(DEBUG)
    { 
      System.out.println("START getNumberStratifiedSamples("+size+"):"+
          " distrib="+ArrayUtil.asString(distrib)+
          " currState="+ArrayUtil.asString(currState)+
          " currIndex="+ArrayUtil.asString(currIndex));
    }

    double[] numPicks = distrib.clone();
    VectorMath.multiply(numPicks, size);
    VectorMath.add(currState, numPicks);

    int[] numSamples = new int[currState.length];
    Arrays.fill(numSamples,1);
    for(int i=0;i<numSamples.length;i++)
      currState[i]--;

    int tot=0;
    for(int i=0;i<currState.length;i++)
    {
      // enforce that we do not remove any instances
      if(currState[i]>0)
      {
        numSamples[i] += (int)currState[i];
        currState[i] -= (int)currState[i];
      }
      tot += numSamples[i];
    }
    
    // if we somehow got too many --- could happen if we are not getting
    // enough samples to cover all classes, then let's remove samples
    while(tot > size)
    {
      int idx = VectorMath.getMinIdx(currState);
      numSamples[idx]--;
      currState[idx]++;
      
      // enforce that if there is room then have at least one
      // instance from each class.
      if(size >= currState.length && numSamples[idx]<=0)
      {
        int idx2 = VectorMath.getMinIdx(currState);
         
        // find the next item that has more than one element
        while(numSamples[idx2]<2)
        {
          int idx3=0;
          double min=Double.POSITIVE_INFINITY;
          for(int i=0;i<currState.length;i++)
          {
            if(currState[i]>currState[idx2] && currState[i] < min)
            {
              min = currState[i];
              idx3 = i;
            }
          }
          idx2=idx3;
        }
        numSamples[idx]++;
        currState[idx]--;
        numSamples[idx2]--;
        currState[idx2]++;
      }
      tot--;
    }

    // if we somehow got too few --- then let's add samples
    while(tot<size)
    {
      int idx = VectorMath.getMaxIdx(currState);
      numSamples[idx]++;
      currState[idx]--;
      tot++;
    }
    
    if(DEBUG)
    { 
      System.out.println("STOP  getNumberStratifiedSamples("+size+"):"+
          " distrib="+ArrayUtil.asString(distrib)+
          " currState="+ArrayUtil.asString(currState)+
          " currIndex="+ArrayUtil.asString(currIndex)+
          " --> samples="+ArrayUtil.asString(numSamples));
    }

    return numSamples;
  }
  
  /**
   * Fill the array with sampled data
   * @param result
   */
  public void sample(Node[] result) {
    if(result==null || result.length==0)
      return;
    if(result.length > size && !replacement)
      throw new IllegalArgumentException("sample(Node[]) for a total of "+result.length+" nodes and no replacement cannot be done.  Total size="+size);
    if(DEBUG) System.out.println("sample(resultSize="+result.length+")");

    if(stratified)
    {
      int[] numSamples = (stratified ? getNumberStratifiedSamples(result.length) : null);
      if(replacement)
        sampleWithReplacement(result,numSamples);
      else
        sampleNoReplacement(result,numSamples);
    }
    else
    {
      if(replacement)
        sampleWithReplacement(result);
      else
        sampleNoReplacement(result);
    }
    VectorMath.randomize(result);
  }

  /**
   * Sample the given number of nodes and return a new array filled with the samples.
   * @param size
   * @return
   */
  public Node[] sample(int size) {
    Node[] result = new Node[size];
    sample(result);
    return result;
  }


  /**
   * Sample sets of the given size from the underlying distribution of nodes
   * and return them in a a list of lists.
   * This ensures that there is no overlap in nodes between the two arrays unless you are sampling with replacement).
   * <p>
   * <b>NOTE:</b> there is no guarantee of non-overlap if you are sampling with replacement.
   * 
   * @param sizes The sizes of the lists to sample
   * @return a list of lists of the given sizes.
   */
  public Node[][] sample(int ...sizes) {
    int sum = VectorMath.sum(sizes);
    if(sum > size && !replacement)
      throw new IllegalArgumentException("sample"+ArrayUtil.asString(sizes)+"="+sum+" with no replacement cannot be done.  Total size="+size);
    Node[][] result = new Node[sizes.length][];
    for(int i=0;i<sizes.length;i++)
      result[i] = new Node[sizes[i]];
    sample(result);
    return result;
  }

  /**
   * Fill the given list of arrays with sample nodes, ensuring that there is no overlap
   * between nodes sampled in each of the sub-arrays (unless you are sampling with replacement).
   * <p>
   * <b>NOTE:</b> there is no guarantee of non-overlap if you are sampling with replacement.

   * @param result
   */
  public void sample(Node[] ...result) {
    if(result == null)
      return;
    int numsample = 0;
    for(Node[] list : result)
      numsample += ( (list==null)? 0 : list.length);    
    if(numsample > size && !replacement)
      throw new IllegalArgumentException("sample(Node[] ...) for a total of "+numsample+" nodes and no replacement cannot be done.  Total size="+size);
    for(Node[] list : result)
      sample(list);    
  }

  /**
   * Create full cross-validation node sets in the form <code>result[numsplit][0][...]</code>
   * is the training set for split <code>numsplit</code> and <code>result[numsplit][1][...]</code>
   * is the test set for split <code>numsplit</code>.
   * <p>
   * <b>NOTE:</b> there is no guarantee of non-overlap if you are sampling with replacement.
   *  
   * @param numSplits
   * @return
   */
  public Node[][][] crossValidate(int numSplits) {
    if(numSplits > size)
    {
      logger.warning("crossValidate("+numSplits+") cannot be done.  Total size="+size+" resetting splits to "+size+".");
      numSplits = size;
    }
    
    double trainRatio = (double)size/(double)numSplits + .0001;
    double trainStart = 0;
    double trainEnd = trainRatio;
    
    randomize();
    Node[][][] result = new Node[numSplits][2][];
    for(Node[][] cv : result)
    {
      int trainIndex = (int)trainStart;
      int trainSize = (int)trainEnd - trainIndex;
      int testSize = size-trainSize;
      cv[0] = new Node[trainSize];
      cv[1] = new Node[testSize];
      
      int i=0;
      int j=0;
      for(;i<trainIndex;i++)
        cv[1][i] = nodes[i];
      for(;j<trainSize;i++,j++)
        cv[0][j] = nodes[i];
      j=trainIndex;
      for(;i<size;i++,j++)
        cv[1][j] = nodes[i];
      
      trainStart = trainEnd;
      trainEnd += trainRatio;
    }
    return result;     
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("DataSampler\n");
    sb.append("    num-nodes:          ").append(size()).append('\n');
    sb.append("    distribution:       ").append(ArrayUtil.asString(distrib)).append('\n');
    sb.append("    replacement:        ").append(replacement).append('\n');
    sb.append("    stratified:         ").append(stratified).append('\n');
    return sb.toString();
  }
  
  public static Graph buildGraph()
  {
    Attributes as = new Attributes("testme");
    AttributeKey key = new AttributeKey("key");
    String[] clstokens = new String[]{"P","N","I"};
    AttributeCategorical cls = new AttributeFixedCategorical("cls", new FixedTokenSet(clstokens));
    
    as.add(key);
    as.add(cls);
    
    double[] keys = new double[15];
    Node[] nodes = new Node[15];
    double[] clsvals = new double[]{Double.NaN,Double.NaN,0,0,0,0,0,0,1,1,1,1,2,2,2};

    Graph g = new Graph();
    g.addAttributes(as);
    for(int i=0;i<keys.length;i++)
    {
      nodes[i] = g.addNode("instance-"+i,as);
      nodes[i].setValue(0, nodes[i].getValue(0));
      nodes[i].setValue(1, clsvals[i]);
    }
    
    return g;
  }
  
  private static DataSampler getSampler(String[] args, Graph g, List<String[]> actions) {
    String usage="usage: DataSampler [debug] [replacement] [seed:K] [unknown] [stratified] [crossvalidate:numsplits]* [sample:size]*";

    long seed = 123;
    boolean replacement = false;
    boolean stratified = false;
    boolean unknown = false;
    boolean DEBUG = false;
    for(String opt : args)
    {
      String a = opt.toLowerCase();
      if(a.equals("replacement"))
        replacement = true;
      else if(a.equals("debug"))
        DEBUG = true;
      else if(a.equals("stratified"))
        stratified = true;
      else if(a.equals("unknown"))
        unknown = true;
      else if(a.startsWith("seed"))
      {
        String[] s = a.split(":");
        if(s.length!=2)
        {
          System.out.println(usage);
          System.out.println("Invalid option: "+opt);
          System.exit(0);
        }
        seed = Long.parseLong(s[1]);
      }
      else if(a.startsWith("crossvalidate") || a.startsWith("sample"))
      {
        String[] action = a.toLowerCase().split(":");
        if(action.length!=2)
        {
          System.out.println(usage);
          System.out.println("Invalid option: "+opt);
          System.exit(0);
        }
        actions.add(action);
      }
      else if(a.toLowerCase().startsWith("sample"))
      {
        String[] action = a.toLowerCase().split(":");
        actions.add(action);
      }
      else
      {
        System.out.println(usage);
        System.exit(0);
      }
    }
    
    System.out.println("DataSampler tester.");
    System.out.println("debug="+DEBUG);
    System.out.println("replacement="+replacement);
    System.out.println("stratified="+stratified);
    System.out.println("unknown="+unknown);
    
    DataSampler sampler = new DataSampler(g.getNodes(), 1, seed, replacement, stratified, unknown);
    sampler.DEBUG = DEBUG;
    return sampler;
  }
  
  public static void main(String[] args) {
    Graph g = buildGraph();
    Node[] nodes = g.getNodes();
    Arrays.sort(nodes,new Comparator<Node>(){public int compare(Node s1, Node s2){return s1.getIndex()-s2.getIndex();}});
    System.out.println("nodes created:");
    for(Node n : nodes)
      System.out.println("   "+n.getName()+":"+n.getValue(1));

    List<String[]> actions = new ArrayList<String[]>();  
    
    DataSampler s = getSampler(args,g,actions);
    System.out.println("datasampler nodes [size="+s.size()+"]:");
    if(s.stratified)
    {
      for(int c=0;c<s.nodesByClass.length;c++)
      {
        System.out.println("  class-"+c+":");
        for(Node n : s.nodesByClass[c])
          System.out.println("   "+n.getName()+":"+n.getValue(1));
      }      
    }
    else
    {
      for(Node n : s.nodes)
        System.out.println("   "+n.getName()+":"+n.getValue(1));
    }
    
    Map<Node,Integer> samples = new HashMap<Node,Integer>();
    for(String[] action : actions)
    {
      Map<Node,Integer> samplesCV = new HashMap<Node,Integer>();
      System.out.println("Action: "+action[0]+" "+action[1]);
      int val = Integer.parseInt(action[1]);
      if(action[0].equals("crossvalidate"))
      {
        Node[][][] result = s.crossValidate(val);
        for(int i=0;i<result.length;i++)
        {
          System.out.println("  split-"+i+":");
          System.out.print("    train:");
          for(Node n : result[i][0])
          {
            samples.put(n, (samples.containsKey(n) ? (samples.get(n)+1) : 1));
            samplesCV.put(n, (samplesCV.containsKey(n) ? (samplesCV.get(n)+1) : 1));
            System.out.print(" "+n.getName());
          }
          System.out.println();
          System.out.print("    test: ");
          for(Node n : result[i][1])
          {
            samples.put(n, (samples.containsKey(n) ? (samples.get(n)+1) : 1));
            samplesCV.put(n, (samplesCV.containsKey(n) ? (samplesCV.get(n)+1) : 1));
            System.out.print(" "+n.getName()+":"+(int)n.getValue(1));
          }
          System.out.println();
          System.out.println("cross-validated samples:");
          for(Node n : nodes)
          {
            System.out.println("   "+n.getName()+":"+n.getValue(1)+" -> "+samplesCV.get(n));
          }
        }
      }
      else if(action[0].equals("sample"))
      {
        Node[] result = s.sample(val);
        System.out.print("   result:");
        for(Node n : result)
        {
          samples.put(n, (samples.containsKey(n) ? (samples.get(n)+1) : 1));
          System.out.print(" "+n.getName()+":"+n.getValue(1));
        }
        System.out.println();
      }
    }
    System.out.println("Total samples:");
    for(Node n : nodes)
    {
      System.out.println("   "+n.getName()+":"+n.getValue(1)+" -> "+samples.get(n));
    }
  }
}
