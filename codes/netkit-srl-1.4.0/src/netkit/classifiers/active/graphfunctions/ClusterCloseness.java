package netkit.classifiers.active.graphfunctions;

import netkit.graph.Node;
import netkit.util.ModularityClusterer.Cluster;

public class ClusterCloseness extends ScoringFunction {
  @Override
  public boolean clusterBased() { return true; }
  
  @Override
  public String toString() { return "clusterCloseness"; }

  @Override
  public double score(Cluster c, Node n)
  { 
    double dist = 0;
    double num = c.getSize()-1;
    for(Node n2 : c)
    {
      if(n==n2)
        continue;
      double d = gm.getDist(n, n2);
      dist += d;
    }
    return ( num==0 ? Double.MAX_VALUE : dist / num );
  }
}
