package org.tinyfix.latency.collectors;

public interface LatencyCollector extends AutoCloseable {


    /**
     * @param buffer buffer containing token
     * @param offset token offset
     * @param length token length
     * @param latency latency in microseconds
     */
    void recordLatency(byte[] buffer, int offset, int length, long latency);

    /** Notifies about an error when outbound packet cannot be matched with inbound packet (possible cause is double order or short history of ticks)*/
    void missingInboundSignal(byte[] buffer, int offset, int length);

    void close();
}
