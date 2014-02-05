package org.tinyfix.latency.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

public class TestHelper {

    // From DataExchangeUtils:

    public  static void  	writeLong (byte [] bytes, int offset, long l) {
        b (bytes, offset, l >>> 56);
        b (bytes, offset + 1, l >>> 48);
        b (bytes, offset + 2, l >>> 40);
        b (bytes, offset + 3, l >>> 32);
        b (bytes, offset + 4, l >>> 24);
        b (bytes, offset + 5, l >>> 16);
        b (bytes, offset + 6, l >>> 8);
        b (bytes, offset + 7, l);
    }

    private static void		b (byte [] bytes, int offset, long byt) {
        bytes [offset] = (byte) (byt & 0xFF);
    }

    public static long      getBigEndianLong (byte [] bytes) {
        long result =
                (0xFF & bytes[7])         +
                        ((0xFF & bytes[6])  <<  8) +
                        ((0xFF & bytes[5])  << 16) +
                        ((0xFFL & bytes[4]) << 24) +
                        ((0xFFL & bytes[3]) << 32) +
                        ((0xFFL & bytes[2]) << 40) +
                        ((0xFFL & bytes[1]) << 48) +
                        ((0xFFL & bytes[0]) << 56);

        return result;
    }

    /**
     * dump byte [] as StringBuilder  "0x00, ..., 0xFF"
     */
    public  static StringBuilder dump(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("null bytes");
        return dump (bytes, 0, bytes.length);
    }

    public static StringBuilder dump(byte[] bytes, int offset, int length) {
        if (bytes == null) throw new IllegalArgumentException("null bytes");

        StringBuilder sbuf = new StringBuilder();
        int i, cnt = offset + length;
        for (i = offset; i < cnt; i++) {
            if (i > offset)
                sbuf.append(", ");
            sbuf.append("(byte)0x" + Integer.toHexString(((int) bytes[i]) & 0xFF).toUpperCase());
        }
        return sbuf;
    }

    private static Random rnd = new Random(13123);

    public static void storeNoise(ByteArrayOutputStream baos, int len) throws IOException {
        byte [] noise = new byte[len];
        rnd.nextBytes(noise);
        baos.write(noise);
    }

}
