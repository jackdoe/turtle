package bz.turtle.readable.input;

import java.io.Serializable;

/**
 * We compute the hash value only once
 * do not reuse between namespaces because the hash is dependent on the namespace hash
 * */
public class Feature implements Serializable {
  /** feature name */
  public String name;

  /** feature value */
  public float value = 1f;

  /** used so we dont recompute the hash value of the feature*/
  public transient int computedHashValue;

  /** used so we dont recompute the hash value of the feature*/
  public transient boolean hashIsComputed = false;

  public static Feature fromString(String featureString) {
    String[] parts = featureString.split(":");
    String name = parts[0];
    float value = 1f;
    if (parts.length > 1) {
      value = Float.parseFloat(parts[1]);
    }
    return new Feature(name, value);
  }

  public Feature() {}

  public Feature(String n) {
    this(n, 1);
  }

  @Override
  public String toString() {
    return String.format("%s:%f", name, value);
  }

  public Feature(String n, float v) {
    this.name = n;
    this.value = v;
  }
}
