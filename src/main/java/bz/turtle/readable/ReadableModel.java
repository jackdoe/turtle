package bz.turtle.readable;

import bz.turtle.readable.input.Doc;
import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ReadableModel {
  public static final int intercept = 11650396;

  public float[] weights;
  public int bits;

  public int maxLabels = 1;
  public int mask;
  public int multiClassBits;
  public int seed = 0;

  @Override
  public String toString() {
    return String.format("bits: %d, weights: %s", bits, Arrays.toString(weights));
  }

  // XXX: incomplete
  public Map<String, String> extractOptions(String o) {
    o = o.trim();
    String[] op = o.split("\\s+");
    Map<String, String> out = new HashMap<>();
    for (int i = 0; i < op.length; i += 2) {
      out.put(op[i], op[i + 1]);
    }
    return out;
  }

  Map<String, String> options;

  /*
  Version 8.6.1
  Id
  Min label:0
  Max label:3
  bits:18
  lda:0
  1 ngram:2
  0 skip:
  options: --hash_seed 0 --link identity
  Checksum: 3984224786
  :0
  116060:0.532933
  155256:0.192113
  213375:0.390151
  218329:0.158008
  250698:0.192113
  259670:0.343652
  */
  public void readLineByLine(File f, BiConsumer<Integer, String> cb) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(f));
    try {
      String x;
      int lineNum = 0;
      while ((x = br.readLine()) != null) {
        cb.accept(lineNum, x);
        lineNum++;
      }
    } finally {
      br.close();
    }
  }

  public void loadReadableModel(File f) throws Exception {
    bits = 0;
    multiClassBits = 0;
    // TODO: more robust parsing
    readLineByLine(
        f,
        (lineNum, x) -> {
          if (lineNum < 11) {
            if (x.contains("bits:")) {
              bits = Integer.parseInt(x.split(":")[1]);
              weights = new float[(1 << bits) - 1];
            }
            if (x.contains("options")) {
              options = extractOptions(x.split(":")[1]);
              if (options.containsKey("--oaa")) {
                maxLabels = Integer.parseInt(options.get("--oaa"));

                multiClassBits = 0;
                int ml = maxLabels;
                while (ml > 0) {
                  multiClassBits++;
                  ml >>= 1;
                }
              }
              if (options.containsKey("--hash_seed")) {
                seed = Integer.parseInt(options.get("--hash_seed"));
              }

              // TODO: ngrams, skips
            }
          } else {
            String[] v = x.split(":");
            int bucket = Integer.parseInt(v[0]);
            float w = Float.parseFloat(v[1]);
            weights[bucket] = w;
          }
        });
    mask = (1 << bits) - 1;
  }

  public ReadableModel(File root) throws Exception {
    if (!root.isDirectory()) {
      throw new IllegalArgumentException(
          String.format(
              "usage: %s/readable_model.txt, %s/test.txt, %s.predictions.txt", root, root, root));
    }
    File model = Paths.get(root.toString(), "readable_model.txt").toFile();
    File test = Paths.get(root.toString(), "test.txt").toFile();
    File predictions = Paths.get(root.toString(), "predictions.txt").toFile();
    loadReadableModel(model);

    if (test.exists() && predictions.exists()) {
      makeSureItWorks(test, predictions);
    }
  }

  public void makeSureItWorks(File testFile, File predFile) throws Exception {
    /* to make sure we predict the same values as VW */

    BufferedReader brTest = new BufferedReader(new FileReader(testFile));
    BufferedReader brPred = new BufferedReader(new FileReader(predFile));
    // perl -e 'for (1..1000) { my $r = int(rand(2)) == 1 ? 1 : -1; print "$r |f a b c\n"}'
    try {
      String testLine;
      String predLine;

      while ((testLine = brTest.readLine()) != null && ((predLine = brPred.readLine()) != null)) {
        String[] test = testLine.split("\\s+");
        Doc doc = new Doc();
        boolean hasNamespace = false;
        for (int i = 0; i < test.length; i++) {
          // label |ns f:value f f f \ns
          if (test[i].startsWith("|")) {
            hasNamespace = true;
            String ns = test[i].replaceFirst("\\|", "");
            doc.namespaces.add(new Namespace(ns));
          } else if (hasNamespace) {

            float weight = 1;
            String feature = test[i];
            if (test[i].contains(":")) {
              String[] s = test[i].split(":");
              feature = s[0];
              weight = Float.parseFloat(s[1]);
            }
            doc.namespaces
                .get(doc.namespaces.size() - 1)
                .features
                .add(new Feature(feature, weight));
          }
        }

        float pred = Float.parseFloat(predLine);
        if (pred - predict(doc)[0] > 0.01) {
          throw new IllegalStateException(
              String.format(
                  "prediction: %f, expected: %f, test line: %s pred line: %s",
                  predict(doc)[0], pred, testLine, predLine));
        }
      }
    } finally {
      brPred.close();
      brTest.close();
    }
  }

  public int getBucket(int featureHash, int klass) {
    return ((featureHash << multiClassBits) | klass) & mask;
  }

  public float[] predict(Doc input) {
    final float[] out = new float[maxLabels];
    // TODO: ngrams skips
    // TODO: -q --cubic hash calculation
    input.namespaces.forEach(
        n -> {
          int namespaceHash = VWMurmur.hash(n.namespace, seed);
          n.features.forEach(
              f -> {
                int featureHash = VWMurmur.hash(f.name, namespaceHash);

                for (int klass = 0; klass < maxLabels; klass++) {
                  int bucket = getBucket(featureHash, klass);
                  out[klass] += f.value * weights[bucket];
                }
              });
        });

    if (input.hasIntercept) {
      for (int klass = 0; klass < maxLabels; klass++) {
        int bucket = getBucket(intercept, klass);
        out[klass] += weights[bucket];
      }
    }
    return out;
  }
}
