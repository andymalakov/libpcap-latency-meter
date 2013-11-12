package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.util.AsciiUtils;

/** Scans JPacket and extracts value of given FIX tag */
abstract class FixMessageTagExtractor<T> implements CorrelationIdExtractor<T> {
    private static final byte SOH = 1;
    private static final byte[] BEGIN_STRING_PREFIX = AsciiUtils.getBytes("8=FIX.");
    private static final int BEGIN_STRING_PREFIX_LEN = BEGIN_STRING_PREFIX.length;
    private static final int FULL_BEGIN_STRING_LENGTH = "8=FIX.X.X".length();

    private final int maxTokenLength;
    private final byte[] tokenPrefix;
    private final int tokenPrefixLen;
    private final byte[] tokenValueBuffer;

    /**
     * @param tokenTagNum FIX tag that this parser will look for
     * @param maxTokenLength maximum size of tag value (in bytes)
     */
    FixMessageTagExtractor(int tokenTagNum, int maxTokenLength) {
        System.out.println("Scanning FIX tag " + tokenTagNum);
        this.tokenPrefix = AsciiUtils.getBytes("\001" + Integer.toString(tokenTagNum) + "=");  //  |11=
        this.maxTokenLength = maxTokenLength;
        this.tokenValueBuffer = new byte[maxTokenLength];
        this.tokenPrefixLen = tokenPrefix.length;
    }

    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        //TODO: Support the case when FIX messages are split between several TCP packets (MTU < FIX message size)

        //TODO: Skip ACK messages?
        assert len > 0;
        if (len < FULL_BEGIN_STRING_LENGTH && !AsciiUtils.equals(packet, BEGIN_STRING_PREFIX, start, BEGIN_STRING_PREFIX_LEN)) {
            //throw new ParseException("TCP ACK instead of FIX? " + packet); //return; // not a FIX packet
            return;
        }

        int j = 0;
        int currentTokenStart = 0;
        final int cnt = len + start - 1;
        for (int i = start + FULL_BEGIN_STRING_LENGTH; i < cnt; i++) {
            final byte b = packet.getByte(i);
            if (currentTokenStart != 0) {
                if (b == SOH) {
                    tokenFound(packet, tokenValueBuffer, 0, i - currentTokenStart);
                    currentTokenStart = 0;
                    j = 0;
                } else {
                    if (j == maxTokenLength)
                        throw new ParseException("Token value length exceed maximum allowed (" + maxTokenLength + ')');
                    tokenValueBuffer[j++] = b;
                }
            } else {
                if (b == tokenPrefix[j]) {
                    if (++j == tokenPrefixLen) {
                        currentTokenStart = i + 1;
                        j = 0;
                    }
                } else {
                    j = 0;
                }
            }
        }
    }
}
