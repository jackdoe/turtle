package bz.turtle.readable.input;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

class Util {
    static int parseInt(CharSequence s)
            throws NumberFormatException {
        /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
         */

        int radix = 10;
        if (s == null) {
            throw new NumberFormatException("null");
        }

        int result = 0;
        boolean negative = false;
        int i = 0, len = s.length();
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+')
                    throw new NumberFormatException("For input string: \"" + s + "\"");

                if (len == 1) // Cannot have lone "+" or "-"
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                if (result < multmin) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                result *= radix;
                if (result < limit + digit) {
                    throw new NumberFormatException("For input string: \"" + s + "\"");
                }
                result -= digit;
            }
        } else {
            throw new NumberFormatException("For input string: \"" + s + "\"");
        }
        return negative ? result : -result;
    }

    static ByteBuffer encode(CharsetEncoder ce, CharBuffer in, ByteBuffer out)
            throws CharacterCodingException {
        int n = (int) (in.remaining() * ce.averageBytesPerChar());

        if ((n == 0) && (in.remaining() == 0))
            return out;
        ce.reset();
        for (; ; ) {
            CoderResult cr = in.hasRemaining()
                    ? ce.encode(in, out, true)
                    : CoderResult.UNDERFLOW;
            if (cr.isUnderflow())
                cr = ce.flush(out);

            if (cr.isUnderflow())
                break;
            if (cr.isOverflow()) {
                n = 2 * n + 1;    // Ensure progress; n might be 0!
                ByteBuffer o = ByteBuffer.allocate(n);
                out.flip();
                o.put(out);
                out = o;
                continue;
            }
            cr.throwException();
        }
        out.flip();
        return out;
    }
}
