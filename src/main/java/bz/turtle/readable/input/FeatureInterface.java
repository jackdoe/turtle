package bz.turtle.readable.input;

import java.nio.ByteBuffer;

public interface FeatureInterface {
  boolean hasIntegerName();

  int getIntegerName();

  StringBuilder getStringName();

  ByteBuffer getBytes();

  float getValue();

  void rename(StringBuilder name);

  void rename(int name);

  void rename(double name);

  void rename(float name);

  void rename(long name);

  int getComputedHash();

  void setComputedHash(int h);

  void resetIsHashComputed();

  boolean isHashComputed();
}
