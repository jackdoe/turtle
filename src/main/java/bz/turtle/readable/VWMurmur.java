package bz.turtle.readable;

import java.nio.ByteBuffer;

/**
 * copy paste reimplementation from explore/hash.h in JohnLangford/vowpal_wabbit since i couldnt
 * find published murmur for java that returns the same hash as VW so i had to copy it
 */
public class VWMurmur {
  private static int rotl32(int x, int r) {
    return (x << r) | (x >>> (32L - r));
  }

  private static int fmix(int h) {
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }

  public static int hash(StringBuilder s, int seed) {
    ByteBuffer d = ByteBuffer.wrap(s.toString().getBytes());
    return hash(d, seed);
  }

  public static int hash(ByteBuffer data, int seed) {
    int len = data.limit();
    int nblocks = len / 4;
    int h1 = seed;
    int c1 = 0xcc9e2d51;
    int c2 = 0x1b873593;

    int i = 0;
    while (i <= len - 4) {
      int k1 = (data.get(i) | data.get(i + 1) << 8 | data.get(i + 2) << 16 | data.get(i + 3) << 24);

      k1 *= c1;
      k1 = rotl32(k1, 15);
      k1 *= c2;

      h1 ^= k1;
      h1 = rotl32(h1, 13);
      h1 = h1 * 5 + 0xe6546b64;

      i += 4;
    }

    int k1 = 0;
    int end = (nblocks * 4);
    switch (len & 3) {
      case 3:
        k1 ^= (int) data.get(end + 2) << 16;
      case 2:
        k1 ^= (int) data.get(end + 1) << 8;
      case 1:
        k1 ^= (int) data.get(end);

        k1 *= c1;
        k1 = rotl32(k1, 15);

        k1 *= c2;
        h1 ^= k1;
    }
    h1 ^= len;
    return fmix(h1);
  }
}
