package org.tinyfix.latency.protocols;

import org.jnetpcap.packet.JPacket;

/** Extracts correlation ID from JPacket */
public interface CorrelationIdExtractor<T> {

    void parse(JPacket packet, int start, int len, T cookie);

}
