package bz.turtle.readable;

public class PredictionStats {
  /** count of missing features */
  public Counter missingFeatures = new Counter();
  /** amount of features looked up */
  public Counter featuresLookedUp = new Counter();
  /** sum of predictions of all classess (unnormalized unclipped) */
  public Counter predictions = new Counter();
}
