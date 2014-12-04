package org.tinyfix.latency.protocols.timebase;

import org.jnetpcap.packet.JPacket;
import org.tinyfix.latency.protocols.CorrelationIdExtractor;
import org.tinyfix.latency.protocols.CorrelationIdListener;

/** Quick-and-Dirty way of extracting BestBidOfferMessage.bidQuoteId and .askQuoteID from Deltix TimeBase packets */
public class TimeBaseQuoteIdExtractor<T> implements CorrelationIdExtractor<T> {

    private static final int CHAR_SEQUENCE_COUNTER_LENGTH = 20;
    private static final int MIN_NUMBER_OF_LEADING_ZEROS = CHAR_SEQUENCE_COUNTER_LENGTH/2;
    private static final int SIGNATURE_PREFIX = 7;
    private static final int MIN_PACKET_LENGTH = 28;
    private final byte [] signature;
    private final byte [] correlationIdBuffer;
    private final CorrelationIdListener listener;

    public TimeBaseQuoteIdExtractor(CorrelationIdListener listener) {
        this.listener = listener;
        signature = new byte [SIGNATURE_PREFIX + MIN_NUMBER_OF_LEADING_ZEROS];
        correlationIdBuffer = new byte [SIGNATURE_PREFIX + CHAR_SEQUENCE_COUNTER_LENGTH];

        int i=0;

        // exchange code (always null)
        signature [i++] = 0;
        signature [i++] = 0;
        signature [i++] = 0;
        signature [i++] = 0;

        signature [i++] = (byte) 0xB0;

        // UTF8 string length (two bytes)
        signature [i++] = 0;
        signature [i++] = (byte) CHAR_SEQUENCE_COUNTER_LENGTH;

        while (i < signature.length) {
            signature [i++] = '0'; // zero-padded prefix
        }
    }


    @Override
    public void parse(JPacket packet, int start, int len, T cookie) {
        assert len > 0;
        if (len < MIN_PACKET_LENGTH) {
            return;
        }

        int j = 0;
        final int cnt = len + start;
        for (int i = start; i < cnt; i++) {

            final byte b = packet.getByte(i);

            if (j < signature.length) {
                if (b == signature[j])
                    j++;
                else
                    j = 0;
            } else {
                 j++;
            }
            if (j > 0) {
                correlationIdBuffer[j - 1] = b;

                if (j == correlationIdBuffer.length) {
                    listener.onCorrelationId(packet, correlationIdBuffer, SIGNATURE_PREFIX, CHAR_SEQUENCE_COUNTER_LENGTH);
                    j = 0;
                }
            }

        }
    }

    public String toString() {
        return "TimeBase Protocol (HACK)";
    }
}
