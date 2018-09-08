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

  @Test
  public void predictQuadraticNumeric() throws Exception {
    File tdir = new File(this.getClass().getClassLoader().getResource("testqnum").getFile());
    ReadableModel m = new ReadableModel(tdir);
    // echo "1 |a numa:2 cat1 cat2 |b numb:4 cat3 cat4" | vw -t -i model.bin
    assertEquals(
        m.predict(
                new Doc(
                    new Namespace("a", Feature.fromString("numa:2"), Feature.fromString("cat1"), Feature.fromString("cat2")),
                    new Namespace("b", Feature.fromString("numb:4"), Feature.fromString("cat3"), Feature.fromString("cat4"))))[0],
        0.864206,
        0.001);
  }

}
