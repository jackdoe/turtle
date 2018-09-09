package bz.turtle.readable;

import bz.turtle.readable.input.Doc;
import bz.turtle.readable.input.Feature;
import bz.turtle.readable.input.Namespace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Reades Vowpal Wabbit --readable_model file and creates a weights array containing the weight per
 * bucket then using same hashing of vw finds the correct bucket for the input features and computes
 * the inner product.
 *
 * <p>check out https://gist.github.com/luoq/b4c374b5cbabe3ae76ffacdac22750af and
 * https://github.com/JohnLangford/vowpal_wabbit/wiki/Feature-Hashing-and-Extraction for more
 * information
 *
 * <p>Example:
 *
 * <pre>
 * execute: echo "1 |ns a b c:4" | vw --readable_model /tmp/readable_model.txt
 *
 *
 *
 * ReadableModel m = new ReadableModel(new File("/tmp/readable_model.txt"));
 * float[] p = m.predict(new Doc(new Namespace("ns", new Feature("a"), new Feature("c",3)));
 * System.out.println(Arrays.toString(p));
 *
 * </pre>
 */
public class ReadableModel {

  private static final boolean DEBUG = false;
  private static final int intercept = 11650396;
  private final int FNV_prime = 16777619;

  /**
   * This is the actual model of size 2**bits if you build something with vw -b 18 it will be of
   * size 262144
   */
  public float[] weights;

  private int bits;

  private int oaa = 1;
  private int mask = 0;
  private int multiClassBits = 0;
  private int seed = 0;
  private boolean hashAll = false;

  private float minLabel = 0;
  private float maxLabel = 0;

  // -q ab
  // -q ac
  private Map<Character, Set<Character>> quadratic = new HashMap<>();
  private boolean quadraticAnyToAny = false;

  // XXX: incomplete
  private void extractOptions(String o, BiConsumer<String, String> cb) {
    o = o.trim();
    if (o.isEmpty())
      return;
    String[] op = o.split("\\s+");
    for (int i = 0; i < op.length; i += 2) {
      cb.accept(op[i], op[i + 1]);
    }
  }

  private String getSecondValue(String s) {
    return s.split(":")[1];
  }
  /**
   * @param file the vw --readable_model file.txt
   * @throws IOException if file reading fails
   *     <p>The contents of the file looks like this
   *     <pre>
   *   Version 8.6.1
   *   Id
   *   Min label:0
   *   Max label:3
   *   bits:18
   *   lda:0
   *   1 ngram:2
   *   0 skip:
   *   options: --hash_seed 0 --link identity
   *   Checksum: 3984224786
   *   :0
   *   116060:0.532933
   *   155256:0.192113
   *   213375:0.390151
   *   218329:0.158008
   *   250698:0.192113
   *   259670:0.343652
   * </pre>
   *     <b>155256:0.192113</b> is hash bucket:weight, we use the same hashing algorithm as vw to
   *     find the features in the model
   */
  public void loadReadableModel(File file) throws IOException {
    bits = 0;
    boolean inHeader = true;
    multiClassBits = 0;
    BufferedReader br = new BufferedReader(new FileReader(file));
    // TODO: more robust parsing
    try {
      String line;
      while ((line = br.readLine()) != null) {
        if (inHeader) {
          if (line.equals(":0")) {
            inHeader = false;
          }
          if (line.contains("bits:")) {
            bits = Integer.parseInt(getSecondValue(line));
            weights = new float[(1 << bits)];
          }
          if (line.contains("Min label")) {
            minLabel = Float.parseFloat(getSecondValue(line));
          }
          if (line.contains("Max label")) {
            maxLabel = Float.parseFloat(getSecondValue(line));
          }
          if (line.contains("options")) {
            extractOptions(
                line.split(":", 2)[1],
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
                      // TODO: the way we do permutation differs from the way we do permutations
                      // TODO: the results will differ
                      /*

                      from vw
                      echo '1 |aa x:1 y:2 ' | vw -f x.bin -a -q :: 2>&1 | grep Constant | tr "\t" "\n" | sort
                      aa^x*aa^x:113732:1:0@0
                      aa^x*aa^y:189809:2:0@0
                      aa^x:63954:1:0@0
                      aa^y*aa^y:125762:4:0@0
                      aa^y:237799:2:0@0

                      and we generate
                      aa^x:63954:1:0.000000
                      aa^y:237799:1:0.000000
                      aa^x*aa^x:113732:1:0.000000
                      aa^x*aa^y:189809:1:0.000000
                      aa^y*aa^x:176759:1:0.000000
                      aa^y*aa^y:125762:1:0.000000
                       */
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
          String[] v = line.split(":");
          int bucket = Integer.parseInt(v[0]);
          float w = Float.parseFloat(v[1]);
          weights[bucket] = w;
        }
      }
    } finally {
      br.close();
    }

    mask = (1 << bits) - 1;
  }

  /**
   * @param root file or directory to read from
   * @throws IOException if reading fails
   *     <p>If you pass a directory as input it will look for 3 files
   *     <ul>
   *       <li>readable_model.txt
   *       <li>test.txt
   *       <li>predictions.txt
   *     </ul>
   *     if test.txt and predictions.txt exists it will automatically run makeSureItWorks()
   *     <p>If you pass a file it will just load the model
   */
  public ReadableModel(File root) throws IOException {
    if (root.isDirectory()) {
      File model = Paths.get(root.toString(), "readable_model.txt").toFile();
      File test = Paths.get(root.toString(), "test.txt").toFile();
      File predictions = Paths.get(root.toString(), "predictions.txt").toFile();
      loadReadableModel(model);

      if (test.exists() && predictions.exists()) {
        makeSureItWorks(test, predictions);
      }
    } else {
      loadReadableModel(root);
    }
  }

  /**
   * @param testFile file with one example per line
   * @param predFile file output from vw -t -i model --predictions -r
   * @throws IOException if file reading fails
   *     <p>This function reads the test file and the predictio file and compares if it can make the
   *     same predictions as vw line for line
   */
  public void makeSureItWorks(File testFile, File predFile) throws IOException {
    /* to make sure we predict the same values as VW */

    BufferedReader brTest = new BufferedReader(new FileReader(testFile));
    BufferedReader brPred = new BufferedReader(new FileReader(predFile));
    int lineNum = 0;
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
        float[] ourPrediction = predict(doc);

        // ran with --probabilities for -oaa
        if (predLine.contains(":")) {
          String[] perKlass = predLine.split(" ");
          for (int i = 0; i < perKlass.length; i++) {
            String[] kv = perKlass[i].split(":");
            int index = Integer.parseInt(kv[0]) - 1;
            float pred = Float.parseFloat(kv[1]);

            if (Math.abs(pred - ourPrediction[index]) > 0.01) {
              throw new IllegalStateException(
                  String.format(
                      "line: %d index %d, prediction: %f, ourPrediction: %f \noaa %s,\npred line: %s\ntest line: %s",
                      lineNum,
                      index,
                      pred,
                      ourPrediction[index],
                      Arrays.toString(ourPrediction),
                      predLine,
                      testLine));
            }
          }
        } else {
          float pred = Float.parseFloat(predLine);

          if (Math.abs(pred - ourPrediction[0]) > 0.01) {
            throw new IllegalStateException(
                String.format(
                    "line: %d index %d, prediction: %f, ourPrediction: %f \noaa %s,\npred line: %s\ntest line: %s",
                    lineNum,
                    0,
                    pred,
                    ourPrediction[0],
                    Arrays.toString(ourPrediction),
                    predLine,
                    testLine));
          }
        }
        lineNum++;
      }
    } finally {
      brPred.close();
      brTest.close();
    }
  }

  private int getBucket(int featureHash, int klass) {
    return ((featureHash << multiClassBits) | klass) & mask;
  }

  /**
   * @param mmNamespaceHash the namespace hash VWMurmur.hash(namespace, seed) where seed is usually
   *     0 unless you pass --hash_seed to vw
   * @param featureName the feature name
   * @return the hash of the feature according to vw
   *     <p>check out
   *     https://github.com/JohnLangford/vowpal_wabbit/blob/579c34d2d2fd151b419bea54d9921fc7f3f55bbc/vowpalwabbit/parse_primitives.cc#L48
   */
  public int hashOf(int mmNamespaceHash, String featureName) {
    int featureHash = 0;
    if (hashAll) {
      featureHash = VWMurmur.hash(featureName, mmNamespaceHash);
    } else {
      try {
        featureHash = Integer.parseInt(featureName) + mmNamespaceHash;
      } catch (NumberFormatException ex) {
        featureHash = VWMurmur.hash(featureName, mmNamespaceHash);
      }
    }
    return featureHash;
  }

  /**
   * @param input Document to evaluate
   * @return prediction per class
   */
  public float[] predict(Doc input) {
    if (DEBUG) {
      System.out.println("-----------");
    }

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
                  if (DEBUG) {
                    System.out.println(
                        String.format(
                            "%s^%s:%d:1:%f", n.namespace, f.name, bucket, weights[bucket]));
                  }
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

      if (quadraticAnyToAny) {
        input.namespaces.forEach(
            ans -> {
              input.namespaces.forEach(
                  bns -> {
                    ans.features.forEach(
                        a -> {
                          bns.features.forEach(
                              b -> {
                                int fnv = ((a._computed_hash * FNV_prime) ^ b._computed_hash);
                                for (int klass = 0; klass < oaa; klass++) {
                                  int bucket = getBucket(fnv, klass);

                                  if (DEBUG) {
                                    System.out.println(
                                        String.format(
                                            "%s^%s*%s^%s:%d:1:%f",
                                            ans.namespace,
                                            a.name,
                                            bns.namespace,
                                            b.name,
                                            bucket,
                                            weights[bucket]));
                                  }

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
                                        if (DEBUG) {
                                          System.out.println(
                                              String.format(
                                                  "%s^%s*%s^%s:%d:1:%f",
                                                  ans.namespace,
                                                  a.name,
                                                  bns.namespace,
                                                  b.name,
                                                  bucket,
                                                  weights[bucket]));
                                        }

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
        if (DEBUG) {
          System.out.println(String.format("%s:%d:1:%f", "Constant", bucket, weights[bucket]));
        }

        out[klass] += weights[bucket];
      }
    }

    // TODO: clip if requested as per https://github.com/jackdoe/turtle/issues/1
    return clip(out);
  }

  protected float[] clip(float[] raw_out){
    for (int klass = 0; klass < oaa; klass++){
      raw_out[klass] = clip(raw_out[klass]);
    }
    return raw_out;
  }

  protected float clip(float raw_out){
     return Math.max(Math.min(raw_out, this.maxLabel), this.minLabel);
  }
}
