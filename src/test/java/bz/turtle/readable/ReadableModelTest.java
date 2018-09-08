package bz.turtle.readable;

import bz.turtle.readable.input.Doc;
import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ReadableModelTest {

  @Test
  public void predictBasic() throws Exception {
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

  @Test
  public void predictQuadratic() throws Exception {
    File tdir = new File(this.getClass().getClassLoader().getResource("testq").getFile());
    ReadableModel m = new ReadableModel(tdir);
    // echo "1 |a x z |b x1 z1" | vw -t -i model.bin
    assertEquals(
        m.predict(
                new Doc(
                    new Namespace("a", new Feature("x"), new Feature("z")),
                    new Namespace("b", new Feature("x1"), new Feature("z1"))))[0],
        -0.0657,
        0.01);
  }
}
