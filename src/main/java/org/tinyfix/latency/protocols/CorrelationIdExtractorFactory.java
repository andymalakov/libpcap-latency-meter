package org.tinyfix.latency.protocols;

public interface CorrelationIdExtractorFactory<T> {
    /**
     * @param protocolKey protocol key specified in command line (e.g. "fix:11" or "timebase")
     * @return CorrelationIdExtractor for given key or <code>null</code>
     */
    CorrelationIdExtractor<T> create(String protocolKey, CorrelationIdListener listener);
}
