package bz.turtle.readable.input;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Namespace implements Serializable {
  public String namespace;
  public List<Feature> features;

  public Namespace() {
    this("");
  }

  public Namespace(String ns) {
    this.namespace = ns;
    features = new ArrayList<>();
  }

  public Namespace(String ns, Feature... features) {
    this.namespace = ns;
    this.features = Arrays.asList(features);
  }
}
