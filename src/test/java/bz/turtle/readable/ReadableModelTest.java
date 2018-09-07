package bz.turtle.readable;

import bz.turtle.readable.input.Doc;
import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ReadableModelTest {

  @Test
  public void predict() throws Exception {
    File tdir = new File(this.getClass().getClassLoader().getResource("test").getFile());
    ReadableModel m = new ReadableModel(tdir);
    assertEquals(
        m.predict(
                new Doc(
                    new Namespace(
                        "f",
                        new Feature("a"),
                        new Feature("b"),
                        new Feature("c"),
                        new Feature("odd=-1"))))[0],
        -1,
        0.01);
  }
}
