package netkit.graph.io;

import java.io.PrintWriter;

import netkit.graph.Edge;
import netkit.graph.Graph;
import netkit.graph.Node;
import netkit.util.NetKitEnv;

public class DotGraph {
  private static final String[] dotColors = new String[]{
    "black","red1","blue1","green1","gray","red2","blue2","green2","red3","blue3","green3","red4","blue4","green4"
  };
  private static final String[] dotShapes = new String[]{
    "ellipse","polygon","box","hexagon","doubleoctagon","trapezium","invhouse"
  };

  public static void saveGraph(final Graph graph, final PrintWriter pw) {
    String[] edgeNames = graph.getEdgeTypeNames();
    String[] nodeTypes = graph.getNodeTypes();
    pw.println("graph G {");
    for(int i=0;i<nodeTypes.length;i++) {
      final String shape = dotShapes[i%dotShapes.length];
      pw.println("  {node [shape="+shape+"]");
      pw.print("    ");
      for(Node node : graph.getNodes(nodeTypes[i])) {
        pw.print(" ");
        pw.print('"'+node.getName()+'"');
      }
      pw.println();
      pw.println("  }");
    }
    for(int i=0;i<edgeNames.length;i++) {
      final String color = dotColors[i%dotColors.length];
      pw.println("  edge [color="+color+"]");
      for(Edge edge : graph.getEdges(edgeNames[i])) {
        final Node src = edge.getSource();
        final Node dest = edge.getDest();
        final String directed = ((graph.getEdge(edgeNames[i],src,dest) == null) ? "->" : "--");       
        pw.println("  \""+src.getName()+"\" "+directed+" \""+dest.getName()+"\"");
      }
    }
    pw.println("}");
  }
  
  public static void saveGraph(final Graph graph, final String file) {
    final PrintWriter pw = NetKitEnv.getPrintWriter(file);
    saveGraph(graph,pw);
    pw.close();
  }
}
