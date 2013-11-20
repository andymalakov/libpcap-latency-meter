package org.tinyfix.latency.collectors;

public interface LatencyCollector extends AutoCloseable {


    /**
     * @param buffer buffer containing correlation ID
     * @param offset correlation ID offset
     * @param length correlation ID length
     * @param inboundTimestamp Timestamp of inbound signal (microseconds) (time of day since local midnight?)
     * @param outboundTimestamp Timestamp of inbound signal (microseconds) (time of day since local midnight?)
     */
    void recordLatency(byte[] buffer, int offset, int length, long inboundTimestamp, long outboundTimestamp);

    /** Notifies about an error when outbound packet cannot be matched with inbound packet (possible cause is double order or short history of ticks)*/
    void missingInboundSignal(byte[] buffer, int offset, int length);

    void close();
}
