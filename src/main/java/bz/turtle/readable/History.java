package bz.turtle.readable;

public class History {
  public Counter[] perSecond;
  public String tag;
  public static int now = 0;
  static Thread timer;

  static {
    timer =
        new Thread(
            () -> {
              while (true) {
                now = (int) (System.currentTimeMillis() / 1000);
                try {
                  Thread.sleep(1000);
                } catch (Exception e) {
                  break;
                }
              }
            });
    timer.start();
  }

  public History(String tag) {
    this.tag = tag;
    perSecond = new Counter[60];
    for (int i = 0; i < perSecond.length; i++) perSecond[i] = new Counter();
  }

  public void add(float f) {
    int bucket = now % 60;
    perSecond[bucket].add(now, f);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < perSecond.length; i++) {
      if (perSecond[i].get() != 0) {
        sb.append("[");
        sb.append(perSecond[i].toString());
        sb.append("]");
      }
    }
    return sb.toString();
  }
}
