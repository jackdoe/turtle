package bz.turtle.readable.input;

import java.io.Serializable;

public class Feature implements Serializable {
  public String name;
  public float value = 1f;
  public transient int _computed_hash;

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
