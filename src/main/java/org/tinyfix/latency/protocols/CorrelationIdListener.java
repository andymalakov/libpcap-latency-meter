package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;

public interface CorrelationIdListener {

    /**
     * Callback method to report correlation ID found in packet body
     * @param packet current JPacket (e.g. to lookup timestamps)
     * @param buffer buffer containing correlation ID
     * @param offset offset in the buffer
     * @param length length of correlation ID (in bytes)
     */
    void onCorrelationId(JPacket packet, byte[] buffer, int offset, int length);

}
