package bz.turtle.readable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;

public class Fuzz {
  public void cleanup(File path) throws IOException {
    Path pathToBeDeleted = Paths.get(path.toURI());

    Files.walk(pathToBeDeleted)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  public static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

  Random r = new Random();

  public String createExample(int klass) {
    StringBuilder sb = new StringBuilder();
    sb.append(klass);
    sb.append(" ");
    for (int nsA = 0; nsA < 15; nsA++) {
      for (int nsB = 0; nsB < 2; nsB++) {
        String ns =
            Character.toString(alphabet.charAt(nsA)) + Character.toString(alphabet.charAt(nsB));
        sb.append("|");
        sb.append(ns);
        sb.append(" ");
        for (int i = 1; i < 10; i++) {
          sb.append("n");
          sb.append((char) (r.nextInt(26) + 'a'));
          sb.append(":");
          sb.append(i);
          sb.append(" ");
        }

        for (int i = 0; i < 10; i++) {
          sb.append("c");
          sb.append((char) (r.nextInt(26) + 'a'));
          sb.append("=");
          sb.append(i);
          sb.append(" ");
        }
      }
    }
    sb.append("\n");
    return sb.toString();
  }

  public void runOrExit(String cmd) throws Exception {

    Process p = Runtime.getRuntime().exec(cmd);

    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

    BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

    System.out.println(cmd);
    String s = null;
    while ((s = stdInput.readLine()) != null) {
      System.out.println(s);
    }

    while ((s = stdError.readLine()) != null) {
      System.out.println(s);
    }

    p.waitFor();

    if (p.exitValue() != 0) {
      throw new Exception("expected exit code 0, got " + p.exitValue());
    }
  }

  public void runVW(String optionsTrain, String optionsTest) throws Exception {
    runOrExit(
        String.format(
            "vw -d %s --min_prediction -10000 --max_prediction 10000 --readable_model %s -f %s %s",
            data, model, modelBin, optionsTrain));
    runOrExit(String.format("vw -d %s -t -i %s -r %s %s", data, modelBin, pred, optionsTest));
    ReadableModel m = new ReadableModel(tempDir);
    m.makeSureItWorks(data, pred);
  }

  @Test
  public void testMany() throws Exception {
    runVW("--oaa 10", "");

    runVW("", "");
    runVW("-q ab", "");
    runVW("-q ab -q cd -q ac -q bc", "");

    // XXX: accumulates error with many namespaces
    //    runVW("-q ::", "");
  }

  File tempDir, data, pred, model, modelBin;

  @Before
  public void setUp() throws Exception {
    tempDir = Files.createTempDirectory("foobar").toFile();
    data = Paths.get(tempDir.toString(), "test.txt").toFile();
    pred = Paths.get(tempDir.toString(), "predictions.txt").toFile();
    System.out.println(tempDir);
    model = Paths.get(tempDir.toString(), "readable_model.txt").toFile();
    modelBin = Paths.get(tempDir.toString(), "model.bin").toFile();
    BufferedWriter writer = new BufferedWriter(new FileWriter(data));

    for (int i = 0; i < 100; i++) {
      for (int klass = 1; klass < 10; klass++) {
        writer.write(createExample(klass));
      }
    }
    writer.close();
  }

  @After
  public void tearDown() throws Exception {
    cleanup(tempDir);
  }
}
