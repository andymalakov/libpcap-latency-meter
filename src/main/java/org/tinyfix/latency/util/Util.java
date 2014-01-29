package org.tinyfix.latency.util;


public class Util {

    public static long  	readLong (byte [] bytes, int offset) {
        return (
                ((long) bytes [offset]) << 56 |
                        lb (bytes, offset + 1) << 48 |
                        lb (bytes, offset + 2) << 40 |
                        lb (bytes, offset + 3) << 32 |
                        lb (bytes, offset + 4) << 24 |
                        readByte (bytes, offset + 5) << 16 |
                        readByte (bytes, offset + 6) << 8 |
                        readByte (bytes, offset + 7)
        );
    }

    private static long		lb (byte [] bytes, int offset) {
        return (((long) bytes [offset]) & 0xFF);
    }

    public static int		readByte (byte [] bytes, int offset) {
        return (((int) bytes [offset]) & 0xFF);
    }
}
