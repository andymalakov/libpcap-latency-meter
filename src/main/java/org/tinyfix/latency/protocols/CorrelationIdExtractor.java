package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;

/** Extracts correlation ID from JPacket */
public interface CorrelationIdExtractor<T> {

    /**
     * @param packet packet to be parsed
     * @param start payload start offset
     * @param len payload length
     * @param cookie optional cookie
     */
    void parse(JPacket packet, int start, int len, T cookie);

}
