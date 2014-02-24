package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.common.CaptureSettings;
import org.tinyfix.latency.util.AsciiUtils;

/** Handles messages of FIX protocol. Scans JPacket and extracts value of given FIX tag */
class FixMessageTagExtractor<T> implements CorrelationIdExtractor<T> {
    private static final byte SOH = 1;
    private static final byte[] BEGIN_STRING_PREFIX = AsciiUtils.getBytes("8=FIX.");
    private static final int BEGIN_STRING_PREFIX_LEN = BEGIN_STRING_PREFIX.length;
    private static final int FULL_BEGIN_STRING_LENGTH = "8=FIX.X.X".length();

    private final byte[] correlationPrefix; // contains FIX tag prefix, starting with SOH separator, e.g. "|299="
    private final int correlationPrefixLength;
    private final byte[] correlationIdBuffer;

    private final CorrelationIdListener listener;

    /**
     * @param tagNum FIX tag that this parser will look for
     */
    FixMessageTagExtractor(int tagNum, CorrelationIdListener listener) {
        this.correlationPrefix = AsciiUtils.getBytes("\001" + Integer.toString(tagNum) + "=");
        this.correlationIdBuffer = new byte[CaptureSettings.MAX_CORRELATION_ID_LENGTH];
        this.correlationPrefixLength = correlationPrefix.length;
        this.listener = listener;
    }

    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        //TODO: Support the case when FIX messages are split between several TCP packets (MTU < FIX message size)

        try {

            assert len > 0;
            if (len < FULL_BEGIN_STRING_LENGTH || !AsciiUtils.equals(packet, BEGIN_STRING_PREFIX, start, BEGIN_STRING_PREFIX_LEN)) {
                return;
            }

            int j = 0;
            int currentIdIndex = 0;
            final int cnt = len + start - 1;
            for (int i = start + FULL_BEGIN_STRING_LENGTH; i < cnt; i++) {
                final byte b = packet.getByte(i);
                if (currentIdIndex != 0) {
                    if (b == SOH) {
                        listener.onCorrelationId(packet, correlationIdBuffer, 0, i - currentIdIndex);
                        currentIdIndex = 0;
                        j = 0;
                    } else {
                        if (j == CaptureSettings.MAX_CORRELATION_ID_LENGTH)
                            throw new ParseException("Correlation ID value length exceed maximum allowed (" + CaptureSettings.MAX_CORRELATION_ID_LENGTH + ')');
                        correlationIdBuffer[j++] = b;
                    }
                } else {
                    if (b == correlationPrefix[j]) {
                        if (++j == correlationPrefixLength) {
                            currentIdIndex = i + 1;
                            j = 0;
                        }
                    } else {
                        j = 0;
                    }
                }
            }
        } catch (Throwable e) {
            System.err.println("Error parsing packet #" + packet.getFrameNumber() + ": " + e.getMessage());
            //e.printStackTrace();
        }

    }
}
