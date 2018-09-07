package bz.turtle.readable.input;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Doc implements Serializable {
  public List<Namespace> namespaces;

  // we cant know that from the model data
  public boolean hasIntercept = true;

  public Doc() {
    namespaces = new ArrayList<>();
  }

  public Doc(Namespace... nss) {
    this.namespaces = Arrays.asList(nss);
  }
}
