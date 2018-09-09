package bz.turtle.readable;

public class Counter {
  public float sum = 0;
  public int count = 0;
  public int second = 0;

  @Override
  public String toString() {
    return String.format("%d@%f/%d=%f", second, sum, count, get());
  }

  public void add(int s, float f) {
    if (s != second) clear();
    second = s;
    sum += f;
    count++;
  }

  public float get() {
    if (count == 0) return 0;
    return sum / count;
  }

  public void clear() {
    sum = 0;
    count = 0;
  }
}
