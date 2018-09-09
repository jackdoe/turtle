package bz.turtle.readable;

import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;
import bz.turtle.readable.input.PredictionRequest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.io.File;

public class BenchmarkRunner {
  static File tdir =
      new File(BenchmarkRunner.class.getClassLoader().getResource("testq").getFile());
  static ReadableModel m;

  static {
    try {
      m = new ReadableModel(tdir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public float run() {
    float[] pred =
        m.predict(
            new PredictionRequest(
                new Namespace("a", new Feature("x"), new Feature("z")),
                new Namespace("b", new Feature("x1"), new Feature("z1"))));

    return pred[0];
  }
}
