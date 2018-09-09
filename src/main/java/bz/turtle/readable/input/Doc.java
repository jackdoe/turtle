package bz.turtle.readable.input;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Document to predict on Example
 *
 * <pre>
 *      model.predict(
 *                 new Doc(
 *                     new Namespace(
 *                         "a",
 *                         new Feature("x"),
 *                         new Feature("y",0.5),
 *                         new Feature("z",0.3))));
 *
 * </pre>
 */
public class Doc implements Serializable {
  public List<Namespace> namespaces;

  /**
   * there is no information in the model file if --noconstant parameter was used
   * in case you are using it you <b>not</b> to pass hasIntercept=false
   * */
  public boolean hasIntercept = true;

  public Doc() {
    namespaces = new ArrayList<>();
  }

  public Doc(Namespace... nss) {
    this.namespaces = Arrays.asList(nss);
  }
}
