package bz.turtle.readable;

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
    for (int nsA = 0; nsA < 5; nsA++) {
      for (int nsB = 0; nsB < 5; nsB++) {
        String ns =
            Character.toString(alphabet.charAt(nsA))
                + "_"
                + Character.toString(alphabet.charAt(nsB));
        sb.append("|");
        sb.append(ns);
        sb.append(" ");
        //        for (int i = 1; i < 5; i++) {
        //          sb.append("num_");
        //          sb.append((char) (r.nextInt(26) + 'a'));
        //          sb.append(":");
        //          sb.append(i);
        //          sb.append(" ");
        //        }

        for (int i = 0; i < 3; i++) {
          sb.append("cat_");
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

  public String runOrExit(String cmd) throws Exception {

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
    return "";
  }

  @Test
  public void hashOf() throws Exception {
    File tempDir = Files.createTempDirectory("foobar").toFile();
    File data = Paths.get(tempDir.toString(), "test.txt").toFile();
    File pred = Paths.get(tempDir.toString(), "predictions.txt").toFile();

    System.out.println(tempDir);
    File model = Paths.get(tempDir.toString(), "readable_model.txt").toFile();
    File modelBin = Paths.get(tempDir.toString(), "model.bin").toFile();

    BufferedWriter writer = new BufferedWriter(new FileWriter(data));

    for (int i = 0; i < 10; i++) {
      for (int klass = 1; klass < 10; klass++) {
        writer.write(createExample(klass));
      }
    }
    writer.close();

    {
      runOrExit(String.format("vw -d %s --readable_model %s -f %s", data, model, modelBin));
      runOrExit(String.format("vw -d %s -t -i %s -p %s", data, modelBin, pred));
      ReadableModel m = new ReadableModel(tempDir);
      m.makeSureItWorks(data, pred);
    }

    {
      runOrExit(
          String.format("vw -d %s --readable_model %s -f %s --oaa 10", data, model, modelBin));
      runOrExit(String.format("vw -d %s -t -i %s -p %s", data, modelBin, pred));
      ReadableModel m = new ReadableModel(tempDir);
      m.makeSureItWorks(data, pred);
    }

    {
      runOrExit(String.format("vw -d %s --readable_model %s -f %s -q ab", data, model, modelBin));
      runOrExit(String.format("vw -d %s -t -i %s -p %s", data, modelBin, pred));
      ReadableModel m = new ReadableModel(tempDir);
      m.makeSureItWorks(data, pred);
    }

    {
      runOrExit(
          String.format(
              "vw -d %s --readable_model %s -f %s -q ab -q ac -q cb -q cd", data, model, modelBin));
      runOrExit(String.format("vw -d %s -t -i %s -p %s", data, modelBin, pred));
      ReadableModel m = new ReadableModel(tempDir);
      m.makeSureItWorks(data, pred);
    }

    {
      runOrExit(
          String.format(
              "vw -d %s --readable_model %s -f %s -q ab -q :: --leave_duplicate_interactions",
              data, model, modelBin));
      runOrExit(
          String.format(
              "vw -d %s -t -i %s -p %s --leave_duplicate_interactions", data, modelBin, pred));
      ReadableModel m = new ReadableModel(tempDir);
      m.makeSureItWorks(data, pred);
    }

    cleanup(tempDir);
  }
}
