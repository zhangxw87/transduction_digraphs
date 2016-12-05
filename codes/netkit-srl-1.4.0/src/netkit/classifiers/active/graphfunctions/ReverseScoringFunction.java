package netkit.classifiers.active.graphfunctions;

public abstract class ReverseScoringFunction extends ScoringFunction {
  /**
   * Reverse of default comparator function.  Return  natural order by default to ensure
   * that the 'best' score (=smallest) is at the end of the list (first to be picked). 
   */
  public int compare(double d1, double d2) {
    return Double.compare(d1, d2);
  }

  /**
   * What is the best score (first to be picked)
   */
  public double bestScore() { return Double.POSITIVE_INFINITY; }

  /**
   * What is the best score (last to be picked)
   */
  public double worstScore() { return Double.NEGATIVE_INFINITY; }
}
