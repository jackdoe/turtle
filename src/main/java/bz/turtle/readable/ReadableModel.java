package bz.turtle.readable;

import bz.turtle.readable.input.Doc;
import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class ReadableModel {
  // https://gist.github.com/luoq/b4c374b5cbabe3ae76ffacdac22750af
  // https://github.com/JohnLangford/vowpal_wabbit/wiki/Feature-Hashing-and-Extraction
  public static final int intercept = 11650396;
  static final int FNV_prime = 16777619;

  public float[] weights;
  public int bits;

  public int oaa = 1;
  public int mask = 0;
  public int multiClassBits = 0;
  public int seed = 0;
  public boolean hashAll = false;

  public float minLabel = 0;
  public float maxLabel = 0;

  // -q ab
  // -q ac
  public Map<Character, Set<Character>> quadratic = new HashMap<>();
  public boolean quadraticAnyToAny = false;

  @Override
  public String toString() {
    return String.format("bits: %d, weights: %s", bits, Arrays.toString(weights));
  }

  // XXX: incomplete
  private void extractOptions(String o, BiConsumer<String, String> cb) {
    o = o.trim();
    String[] op = o.split("\\s+");
    for (int i = 0; i < op.length; i += 2) {
      cb.accept(op[i], op[i + 1]);
    }
  }

  private String getSecondValue(String s) {
    return s.split(":")[1];
  }
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
              bits = Integer.parseInt(getSecondValue(x));
              weights = new float[(1 << bits)];
            }
            if (x.contains("Min label")) {
              minLabel = Float.parseFloat(getSecondValue(x));
            }
            if (x.contains("Max label")) {
              maxLabel = Float.parseFloat(getSecondValue(x));
            }
            if (x.contains("options")) {
              extractOptions(
                  x.split(":", 2)[1],
                  (key, value) -> {
                    if (key.equals("--oaa")) {
                      oaa = Integer.parseInt(value);

                      multiClassBits = 0;
                      int ml = oaa;
                      while (ml > 0) {
                        multiClassBits++;
                        ml >>= 1;
                      }
                    }

                    if (key.equals("--hash_seed")) {
                      seed = Integer.parseInt(value);
                    }
                    if (key.equals("--hash")) {
                      if (value.equals("all")) {
                        hashAll = true;
                      }
                    }
                    if (key.equals("--quadratic")) {
                      if (value.equals("::")) {
                        quadraticAnyToAny = true;
                      } else {
                        quadratic
                            .computeIfAbsent(value.charAt(0), k -> new HashSet<>())
                            .add(value.charAt(1));
                      }
                    }
                    // TODO: --cubic
                    // TODO: ngrams, skips
                    // TODO: lda
                  });
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

  // https://github.com/JohnLangford/vowpal_wabbit/blob/579c34d2d2fd151b419bea54d9921fc7f3f55bbc/vowpalwabbit/parse_primitives.cc#L48
  public int hashOf(int nsHash, String f) {
    int featureHash = 0;
    if (hashAll) {
      featureHash = VWMurmur.hash(f, nsHash);
    } else {
      try {
        featureHash = Integer.parseInt(f) + nsHash;
      } catch (NumberFormatException ex) {
        featureHash = VWMurmur.hash(f, nsHash);
      }
    }
    return featureHash;
  }

  public float[] predict(Doc input) {
    final float[] out = new float[oaa];
    // TODO: ngrams skips
    // TODO: --cubic hash calculation
    input.namespaces.forEach(
        n -> {
          int namespaceHash = n.namespace.length() == 0 ? 0 : VWMurmur.hash(n.namespace, seed);
          n._computed_hash = namespaceHash;
          n.features.forEach(
              f -> {
                int featureHash = hashOf(namespaceHash, f.name);
                f._computed_hash = featureHash;
                for (int klass = 0; klass < oaa; klass++) {
                  int bucket = getBucket(featureHash, klass);
                  out[klass] += f.value * weights[bucket];
                }
              });
        });

    if (quadratic.size() > 0 || quadraticAnyToAny) {

      // foreach namespace nsA
      //    foreach interacting namespaces nsB
      //       foreach nsA.features a
      //         foreach nsB.feature b
      //            bucket = ((a._computed_hash * FNV_prime) ^ b._computed_hash);

      if (!quadraticAnyToAny) {
        input.namespaces.forEach(
            ans -> {
              input.namespaces.forEach(
                  bns -> {
                    if (ans.namespace.equals(bns.namespace)) return;
                    ans.features.forEach(
                        a -> {
                          bns.features.forEach(
                              b -> {
                                int fnv = ((a._computed_hash * FNV_prime) ^ b._computed_hash);
                                for (int klass = 0; klass < oaa; klass++) {
                                  int bucket = getBucket(fnv, klass);
                                  out[klass] += a.value * b.value * weights[bucket];
                                }
                              });
                        });
                  });
            });
      } else {
        Map<Character, List<Namespace>> prebuild = new HashMap<>();
        // build char -> list of namespaces map so we can work with multiple interactions -q ab -q
        // ac
        input.namespaces.forEach(
            n -> {
              if (n.namespace.length() == 0) return;
              prebuild.computeIfAbsent(n.namespace.charAt(0), k -> new ArrayList<>()).add(n);
            });
        input.namespaces.forEach(
            ans -> {
              Set<Character> interactStartingWith = quadratic.get(ans.namespace.charAt(0));
              if (interactStartingWith == null) return;
              interactStartingWith.forEach(
                  inter -> {
                    List<Namespace> interactions = prebuild.get(inter);
                    if (interactions == null) return;
                    interactions.forEach(
                        bns -> {
                          ans.features.forEach(
                              a -> {
                                bns.features.forEach(
                                    b -> {
                                      int fnv = ((a._computed_hash * FNV_prime) ^ b._computed_hash);
                                      for (int klass = 0; klass < oaa; klass++) {
                                        int bucket = getBucket(fnv, klass);
                                        // TODO: check how is that computed for numerical features
                                        out[klass] += a.value * b.value * weights[bucket];
                                      }
                                    });
                              });
                        });
                  });
            });
      }
    }

    if (input.hasIntercept) {
      for (int klass = 0; klass < oaa; klass++) {
        int bucket = getBucket(intercept, klass);
        out[klass] += weights[bucket];
      }
    }
    return out;
  }
}
