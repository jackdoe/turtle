package bz.turtle.readable.input;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * We compute the hash value only once do not reuse between namespaces because the hash is dependent
 * on the namespace hash
 */
public class Feature implements FeatureInterface {
   /**
   * used so we dont recompute the hash value of the feature
   */
  public transient int computedHashValue;
  public transient boolean hashIsComputed = false;
  private int nameInt;
  private float value = 1f;
  private boolean isStringNameComputed = false;
  private boolean isBytesNameComputed = false;
  private StringBuilder name;
  private boolean hasIntegerName;
  private int bufferSize = 10240;
  private CharBuffer charBuffer = CharBuffer.wrap(new char[bufferSize]);
  private ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[bufferSize]);

  public Feature() {
  }

  public Feature(StringBuilder name) {
    this(name, 1);
  }

  public Feature(String name) {
    this(new StringBuilder(name), 1);
  }

  public Feature(int name) {
    this(name, 1);
  }

  public Feature(CharSequence name, float value) {
    this.name = new StringBuilder();
    this.setName(name);
    this.setValue(value);
  }

  public Feature(String name, float value) {
    this(new StringBuilder(name), value);
  }

  public Feature(int name, float v) {
    this.name = null;
    this.isStringNameComputed = false;
    if (name < 0) {
      rename(name);
    } else {
      this.setNameInt(name);
    }
    this.setValue(v);
  }

  private static boolean isPositiveInteger(CharSequence s) {
    return isPositiveInteger(s, 10);
  }

  private static boolean isPositiveInteger(CharSequence s, int radix) {
    if (s.length() == 0) return false;
    for (int i = 0; i < s.length(); i++) {
      if (i == 0 && s.charAt(i) == '-') {
        return false;
      }
      if (Character.digit(s.charAt(i), radix) < 0) return false;
    }
    return true;
  }

  public static Feature fromString(String featureString) {
    String[] parts = featureString.split(":");
    String name = parts[0];
    float value = 1f;
    if (parts.length > 1) {
      value = Float.parseFloat(parts[1]);
    }
    return new Feature(name, value);
  }

  public boolean hasIntegerName() {
    return hasIntegerName;
  }

  @Override
  public String toString() {
    return String.format("%s[%d]:%f", getStringName(), this.getIntegerName(), getValue());
  }

  /**
   * change the feature name, which also resets the computed hash
   *
   * @param name - the new feature name
   */
  public void rename(int name) {
    if (name < 0) {
      this.setName(name);
    } else {
      this.setNameInt(name);
      this.isStringNameComputed = false;
      this.isBytesNameComputed = false;
    }
    resetIsHashComputed();
  }

  public void rename(CharSequence name) {
    this.setName(name);
    resetIsHashComputed();
  }

  public void rename(double name) {
    this.setName(name);
    resetIsHashComputed();
  }

  public void rename(float name) {
    this.setName(name);
    resetIsHashComputed();
  }

  public void rename(long name) {
    this.setName(name);
    resetIsHashComputed();
  }

  /**
   * --hash strings vs --hash all, in case the feature value is integer, there is no need to
   * convert it to string
   */
  public int getIntegerName() {
    return nameInt;
  }

  private void setNameInt(int nameInt) {
    this.nameInt = nameInt;
    this.hasIntegerName = true;
  }

  /**
   * feature value
   */
  public float getValue() {
    return value;
  }

  public void setValue(float value) {
    this.value = value;
  }

  /**
   * @return the string name, recomputed from nameInt if needed @see getIntegerName
   */
  public String getStringName() {
    if (!isStringNameComputed) {
      this.name.setLength(0);
      this.name.append(this.nameInt);
      this.isStringNameComputed = true;
    }
    return name.toString();
  }

  public ByteBuffer getBytes() {
    if (!isBytesNameComputed) {
      this.setByteBuffer();
    }
    return this.byteBuffer;
  }

  private void setName(CharSequence name) {
    if (isPositiveInteger(name)) {
      try {
        setNameInt(Util.parseInt(name));
      } catch (Exception e) {
      }
    }

    this.isStringNameComputed = true;
    this.name.setLength(0);
    this.name.append(name);
    this.setByteBuffer();
  }

  private void setName(int name) {
    if (name > 0) {
      try {
        setNameInt(name);
      } catch (Exception e) {
      }
    }

    this.isStringNameComputed = true;
    this.name.setLength(0);
    this.name.append(name);
    this.setByteBuffer();
  }

  private void setName(double name) {
    this.isStringNameComputed = true;
    this.name.setLength(0);
    this.name.append(name);
    this.setByteBuffer();
  }

  private void setName(long name) {
    this.isStringNameComputed = true;
    this.name.setLength(0);
    this.name.append(name);
    this.setByteBuffer();
  }

  private void setName(float name) {
    this.isStringNameComputed = true;
    this.name.setLength(0);
    this.name.append(name);
    this.setByteBuffer();
  }

  private void setByteBuffer() {
    // Not using .clear() because java hates it
    this.charBuffer.position(0);
    this.charBuffer.limit(this.charBuffer.capacity());

    this.byteBuffer.position(0);
    this.byteBuffer.limit(this.byteBuffer.capacity());

    CharsetEncoder ce = LocalCharsetEncoder.get();

    for (int i = 0; i < this.name.length(); i++) {
      this.charBuffer.put(this.name.charAt(i));
    }
    // Not using .flip() because java hates it
    this.charBuffer.limit(this.charBuffer.position());
    this.charBuffer.position(0);
    try {
      Util.encode(ce, this.charBuffer, this.byteBuffer);
    } catch (CharacterCodingException e ) {
    }
    this.isBytesNameComputed = true;
  }

  public int getComputedHash() {
    return computedHashValue;
  }

  public void setComputedHash(int n) {
    this.computedHashValue = n;
    this.hashIsComputed = true;
  }

  public boolean isHashComputed() {
    return hashIsComputed;
  }

  public void resetIsHashComputed() {
    hashIsComputed = false;
  }

  private static final class LocalCharsetEncoder {
    private static final ThreadLocal<CharsetEncoder> localEncoder = ThreadLocal.withInitial(
            () -> StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE));

    static CharsetEncoder get() {
      return localEncoder.get().reset();
    }
  }
}
