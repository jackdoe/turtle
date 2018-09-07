package bz.turtle.readable.input;

import java.io.Serializable;

public class Feature implements Serializable {
  public String name;
  public float value = 1f;

  public Feature() {}

  public Feature(String n) {
    this(n, 1);
  }

  public Feature(String n, float v) {
    this.name = n;
    this.value = v;
  }
}
