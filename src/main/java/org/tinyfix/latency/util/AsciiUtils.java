package org.tinyfix.latency.util;

import org.jnetpcap.nio.JBuffer;

import java.io.UnsupportedEncodingException;

public class AsciiUtils {

    public static final String US_ASCII = "US-ASCII";

    public static byte [] getBytes(String asciiText) {
        try {
            return asciiText.getBytes(US_ASCII);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Expecting ASCII string", e);
        }
    }

    public static boolean equals(JBuffer src, byte[] dst, int srcOffset, int length) {
        for (int i=0; i < length; i++)
            if (src.getByte(i + srcOffset) != dst[i])
                return false;

        return true;
    }

//    public static String getString(JPacket packet, int tagValueStart, int tagValueLen) {
//        try {
//            return new String (packet.getByteArray(tagValueStart, tagValueLen), US_ASCII);
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException("Expecting ASCII byte sequence", e);
//        }
//    }

}