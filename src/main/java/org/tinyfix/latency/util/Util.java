package org.tinyfix.latency.util;


import java.nio.ByteBuffer;

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

    public static void dump(ByteBuffer buffer) {
        System.out.println();
        int mark = buffer.position();

        int ii = 0;
        while (buffer.hasRemaining()) {
            if ((ii & 15) == 0)
                System.out.printf ("%04X: ", ii);

            System.out.printf ("%02X ", buffer.get());

            if ((ii & 15) == 15)
                System.out.println ();
            ii++;
        }

        buffer.position(mark);
        System.out.println();
    }
}
