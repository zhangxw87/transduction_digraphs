import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.classifiers.*;
import netkit.classifiers.io.ReadPrior;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class RunNetKit {
  private static class PredictionHolder implements Comparable<PredictionHolder> {
    final double s;
    final Node n;
    public PredictionHolder(Node n, double s) {
      this.n = n;
      this.s = s;
    }
    public int compareTo(PredictionHolder ph) {
      return (int)(s-ph.s);
    }
  }
  
  public static void main(String[] args)
  {
    if(args.length != 1)
    {
      System.err.println("Usage: RunNetKit <schema-file>");
      System.exit(0);
    }
    
    final String newLine = System.getProperty("line.separator");
    final String prior = "nonterrorist,0.01"+newLine+"terrorist,0.99"+newLine;

    final File f = new File(args[0]);
    System.out.println("=========================================");
    System.out.println("Reading graph from file "+args[0]);
    final Graph g = netkit.graph.io.SchemaReader.readSchema(f);

    NetworkLearning nl = new NetworkLearning(new String[]{"-numit","10"});
    nl.setGraph(g);
    nl.setRandomSeed(123L);
    nl.setPrior(ReadPrior.readPrior(new BufferedReader(new StringReader(prior)), nl.getAttribute()));
    nl.setupExperiment();
    
    NetworkLearner learner = nl.getLearner();
    DataSplit[] splits = nl.getSplits();
    
    Estimate predictions = learner.runInference(splits[0]);
    
    int val = nl.getAttribute().getValue("terrorist");

    // Get the top K nodes (top 10% most likely to be 'terrorist'
    ArrayList<PredictionHolder> list = new ArrayList<PredictionHolder>();
    
    for (Node node : predictions)
    {
      double s = predictions.getScore(node, val);
      if(Double.isNaN(s))
          continue;
      list.add(new PredictionHolder(node,s));
    }
    PredictionHolder[] ph = list.toArray(new PredictionHolder[list.size()]);
    Arrays.sort(ph);
    
    // Let's figure out the top 10% of all initially unknown
    int p = (int)(0.1* (double)(g.numNodes()-splits[0].getTestSetSize()));
    System.out.println("Getting "+p+" nodes");
    for(int i=ph.length-1;p>0&&i>=0;p--,i--)
      System.out.println(ph[i].n.getName()+":"+ph[i].s);
  }
}
