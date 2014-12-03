package org.tinyfix.latency.protocols.timebase;

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;
import org.tinyfix.latency.util.LongFormatter;
import org.tinyfix.latency.util.Util;

/** Extracts Correlation IDs from TCP packets that use TimeBase Playback data feed format */
public class TBPlayerCorrelationIdExtractor<T> implements CorrelationIdExtractor<T> {

    private static final int MIN_PACKET_LENGTH = 11;
    static byte [] MAGIC = {
            (byte) 0xC0, // Ma-
            (byte) 0xDE, // -
            (byte) 0x07, // gic
    };

    private final CorrelationIdListener listener;

    private static final int SIZE_OF_LONG = 8;

    private final byte [] correlationIdBinary = new byte [SIZE_OF_LONG];
    private final byte [] correlationIdFormatted = new byte [LongFormatter.WIDTH];

    public TBPlayerCorrelationIdExtractor(CorrelationIdListener listener) {
        this.listener = listener;
    }

    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        assert len > 0;
        if (len < MIN_PACKET_LENGTH) {
            return;
        }

        int magicIndex = 0;
        final int cnt = len + start;
        for (int i = start; i < cnt; ) {
            if (magicIndex < MAGIC.length) {
                final byte b = packet.getByte(i);

                if (b == MAGIC[magicIndex]) {
                    magicIndex ++;
                } else {
                    magicIndex = (MAGIC[0] == b) ? 1 : 0;
                }
                i++;

            } else {
                // Found Magic, let's parse correlation ID
                if (i + SIZE_OF_LONG <= cnt) {
                    synchronized (correlationIdBinary) {
                        // copy LONG into buffer
                        for (int j = 0; j < SIZE_OF_LONG; j++) {
                            correlationIdBinary[j] = packet.getByte(i + j);
                        }
                        // parse LONG
                        long correlationId = Util.readLong(correlationIdBinary, 0);
                        // format as decimal number
                        LongFormatter.format(correlationId, correlationIdFormatted, 0);
                        int offset = trimLeadingSpace(correlationIdFormatted);
                        // notify
                        listener.onCorrelationId(packet, correlationIdFormatted, offset, correlationIdFormatted.length - offset);
                    }
                } else {
                    // packet truncated?
                }

                magicIndex = 0; // search for the next magic
                i += SIZE_OF_LONG;
            }
        }
    }

    private static int trimLeadingSpace(byte [] correlationIdFormatted) {
        int offset = 0;
        while (correlationIdFormatted[offset] == ' ')
            offset++;
        return offset;
    }


}
