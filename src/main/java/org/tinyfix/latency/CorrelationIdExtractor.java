package org.tinyfix.latency;

import org.jnetpcap.packet.JPacket;

/** Extracts correlation ID from JPacket */
interface CorrelationIdExtractor<T> {
    void parse(JPacket packet, int start, int len, T cookie);

    /**
     * Callback method to report correlation ID
     * @param packet current JPacket (e.g. to lookup timestamps)
     * @param buffer buffer containing correlation ID
     * @param offset offset in the buffer
     * @param length length of correlation ID (in bytes)
     */
    void tokenFound(JPacket packet, byte[] buffer, int offset, int length);

}
